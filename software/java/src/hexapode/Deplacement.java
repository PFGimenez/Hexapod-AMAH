package hexapode;

import serial.Serial;
import test.Markov;
import util.Config;
import util.Sleep;
import hexapode.capteurs.Capteur;
import hexapode.enums.Direction;
import hexapode.enums.EnumPatte;
import hexapode.enums.EtatPatte;
import hexapode.enums.Marche;
import hexapode.enums.Mode;
import hexapode.enums.Profil;
import hexapode.exceptions.BordureException;
import hexapode.exceptions.EnnemiException;
import hexapode.exceptions.GoToException;

/**
 * Classe des déplacements bas niveau. S'occupe des permutations de pattes pour
 * tourner, ainsi que la symétrie.
 * Utilisé par Hexapode (haut niveau) ou les tests.
 * @author pf
 *
 */

public class Deplacement
{
    private Serial serie;
    
    private boolean maj_position;

    public Vec2 position; // position à laquelle l'hexapode se croit
    // Utilisé afin de pouvoir changer la direction

    private static final int[] pattes_rotation = { 3, 0, 1, 4, 5, 2, 4, 3, 0,
            5, 2, 1, 5, 4, 3, 2, 1, 0, 2, 5, 4, 1, 0, 3, 1, 2, 5, 0, 3, 4 };

    public boolean capteur_actif = false;
    private long date_debut = -1;
    private static final String[][][] marche = { {
            { new String("101010"), new String("010101") },
            { new String("100000"), new String("100001"), new String("100101"),
                    new String("101101"), new String("000000") },
            { new String("001000"), new String("001010"), new String("001011"),
                    new String("011011"), new String("000000") },
            { new String("101010"), new String("111111"), new String("000000"),
                    new String("010101"), new String("111111"),
                    new String("000000") } } };
    private Marche marche_actuelle = Marche.BASIQUE;
    private Markov[] marcheApprise = new Markov[Mode.values().length];
    private Mode mode = Mode.BIPHASE;
    private String pas_actuel = "000000";
    private Patte[][] pattes;
    private int direction = 0;
    private int pas = 0; // indice pour la marche
    private Capteur capteur;

    public Deplacement(Serial serie, boolean maj_position)
    {
        this.maj_position = maj_position;
        this.serie = serie;
        pattes = new Patte[6][6];

        // Pattes pour la direction 0
        for (int i = 0; i < 6; i++)
            pattes[0][i] = new Patte(serie, i);

        // La couleur modifie les directions.
        // On applique une symétrie verticale (la direction 1 devient la 5)
        
        if(capteur.getInverser())
            for (int i = 1; i < 6; i++)
                for (int j = 0; j < 6; j++)
                    pattes[6 - i][j] = pattes[0][pattes_rotation[(i - 1) * 6 + j]];
        else
            for (int i = 1; i < 6; i++)
                for (int j = 0; j < 6; j++)
                    pattes[i][j] = pattes[0][pattes_rotation[(i - 1) * 6 + j]];

        setDirection(Direction.HAUT);

        for(Mode m: Mode.values())
            marcheApprise[m.ordinal()] = Markov.getLearnedMarkov(m);

        arret();
        desasserv();
        // position = new Vec2(1300,1800);
        position = new Vec2(0, 500);
        
    }

    /**
     * Lève une patte et la tourne vers la droite (du point de vue de la patte).
     * @param patte
     */
    public void lever_droite(EnumPatte patte)
    {
        pattes[direction][patte.ordinal()].lever_droite();
    }

    /**
     * Lève une patte et la tourne vers la gauche (du point de vue de la patte).
     * @param patte
     */
    public void lever_gauche(EnumPatte patte)
    {
        pattes[direction][patte.ordinal()].lever_gauche();
    }

    /**
     * Lève une patte.
     * @param patte
     */
    public void lever(EnumPatte patte)
    {
        pattes[direction][patte.ordinal()].lever();
    }


    /**
     * Fait un pas, en ignorant les bordures. Utilisé, par exemple, pour poser
     * les fresques.
     * @param ignore
     * @throws EnnemiException
     */
    public void avancer_elementaire_pres_bord(EnumPatte[] ignore) throws EnnemiException
    {
        try
        {
            avancer_elementaire(ignore);
        } catch (BordureException e)
        {
        }       
    }

    /**
     * Avance de "Patte.avancee" millimètres dans la direction actuelle
     * Ignore certaines pattes, qui resteront dans leur position initiale.
     * @param ignore
     * @throws EnnemiException 
     * @throws BordureException 
     */
    public void avancer_elementaire(EnumPatte[] ignore) throws EnnemiException, BordureException
    {
        String prochain_pas = getProchainPas();

        // On ignore certaines pattes dans le mouvement
        if(ignore != null)
            for(EnumPatte patte: ignore)
                prochain_pas = prochain_pas.substring(0,patte.ordinal()) + "?" + prochain_pas.substring(patte.ordinal()+1);

        goto_etat(prochain_pas);
        if(position.x > 1500-Config.ecart_bordure || position.x < -1500+Config.ecart_bordure || position.y > 2000-Config.ecart_bordure || position.y < Config.ecart_bordure)
            throw new BordureException();
    }

    /**
     * L'hexapode fait une action, biphasée ou triphasée.
     * 
     * @param e
     * @throws EnnemiException
     */
    public void goto_etat(String e) throws EnnemiException
    {
        if (mode == Mode.BIPHASE)
            goto_etat_biphase(e);
        else if (mode == Mode.TRIPHASE)
            goto_etat_triphase(e);
    }

    /**
     * L'hexapode fait l'action donnée par une chaîne binaire. Note: on peut
     * ignorer une patte en mettant un autre caractère.
     * 
     * @param e
     * @throws EnnemiException
     */
    private void goto_etat_biphase(String e) throws EnnemiException
    {
        verif_avant_mouvement();

        boolean mouvement = false, avance = false;

        // on sépare les deux for pour lever/baisser. Ainsi, on lève toutes les
        // pattes intéressées, puis on les abaisse en même temps
        // On ramène en arrière et on lève
        try
        {
            for (int i = 0; i < 6; i++)
                if (e.charAt(i) == '1'
                        && pattes[direction][i].getEtat() != EtatPatte.AVANT)
                {
                    mouvement = true;
                    pattes[direction][i].goto_etat(i, EtatPatte.DEBOUT);
                }
                else if (e.charAt(i) == '0'
                        && pattes[direction][i].getEtat() != EtatPatte.ARRIERE)
                {
                    avance = true; // si on ramène une patte en arrière, alors
                                   // c'est que l'hexapode avance
                    mouvement = true;
                    pattes[direction][i].goto_etat(i, EtatPatte.POUSSE);
                }

            // On continue le mouvement que s'il y a un mouvement à continuer
            if (mouvement)
            {
                Sleep.sleep();

                for (int i = 0; i < 6; i++)
                    // on baisse
                    if (pattes[direction][i].getEtat() == EtatPatte.DEBOUT)
                        pattes[direction][i].goto_etat(i, EtatPatte.AVANT,
                                Sleep.temps_defaut / 4);
                    else if (pattes[direction][i].getEtat() == EtatPatte.POUSSE)
                        pattes[direction][i].goto_etat(i, EtatPatte.ARRIERE,
                                Sleep.temps_defaut / 4);

                Sleep.sleep(Sleep.temps_defaut / 4);

                if (avance)
                    maj_position();

                System.out.println(position);

            }
        } catch (GoToException exception)
        {
            exception.printStackTrace();
        }

    }

    /**
     * L'hexapode fait l'action donnée par une chaîne ternaire. Note: on peut
     * ignorer une patte en mettant un autre caractère.
     * 0: POUSSE
     * 1: DEBOUT
     * 2: AVANT
     * 
     * @param e
     * @throws EnnemiException
     */
    private void goto_etat_triphase(String e) throws EnnemiException
    {
        verif_avant_mouvement();

        boolean mouvement = false, avance = false;

        // on sépare les deux for pour lever/baisser. Ainsi, on lève toutes les
        // pattes intéressées, puis on les abaisse en même temps
        // On ramène en arrière et on lève
        try
        {
            for (int i = 0; i < 6; i++)
                if (e.charAt(i) == '0'
                        && pattes[direction][i].getEtat() != EtatPatte.ARRIERE)
                {
                    mouvement = true;
                    avance = true; // si on ramène une patte en arrière, alors
                                   // c'est que l'hexapode avance
                    pattes[direction][i].goto_etat(i, EtatPatte.POUSSE);
                }
                else if (e.charAt(i) == '1'
                        && pattes[direction][i].getEtat() != EtatPatte.DEBOUT)
                {
                    mouvement = true;
                    pattes[direction][i].goto_etat(i, EtatPatte.DEBOUT);
                }
                else if (e.charAt(i) == '2'
                        && pattes[direction][i].getEtat() != EtatPatte.AVANT)
                {
                    mouvement = true;
                    pattes[direction][i].goto_etat(i, EtatPatte.AVANT);
                }

            // On continue le mouvement que s'il y a un mouvement à continuer
            if (mouvement)
            {
                Sleep.sleep();

                for (int i = 0; i < 6; i++)
                    // on relève
                    if (pattes[direction][i].getEtat() == EtatPatte.POUSSE)
                        pattes[direction][i].goto_etat(i, EtatPatte.ARRIERE,
                                Sleep.temps_defaut / 4);

                if (avance)
                {
                    Sleep.sleep(Sleep.temps_defaut / 4);
                    maj_position();
                }

            }
        } catch (GoToException exception)
        {
            exception.printStackTrace();
        }

    }

    public String getProchainPas()
    {
        if (marche_actuelle == Marche.MARKOV)
            pas_actuel = marcheApprise[mode.ordinal()].nextValidation(pas_actuel);
        else
        {
            pas++;
            pas %= marche[mode.ordinal()][marche_actuelle.indice].length;
            pas_actuel = marche[mode.ordinal()][marche_actuelle.indice][pas];
        }
        return pas_actuel;
    }

    /**
     * Modifie le mode (biphasé, triphasé)
     * 
     * @param mode
     */
    public void setMode(Mode mode)
    {
        if(this.mode != mode)
        {
            this.mode = mode;
            pas_actuel = "000000";
        }
    }

    /**
     * Modifie la marche, c'est-à-dire l'enchaînement d'états.
     * 
     * @param marche
     */
    public void setMarche(Marche marche)
    {
        if(marche_actuelle != marche)
        {
            marche_actuelle = marche;
            pas = 0;
        }
    }

    /**
     * Désasservit l'hexapode
     */
    public void desasserv()
    {
        System.out.println("Desasservissement de l'hexapode");
        for (int i = 0; i < 6; i++)
            pattes[direction][i].desasserv();
    }

    /**
     * Met l'hexapode en position "normale" (toute patte baissée).
     */
    public void arret()
    {
        try
        {
            for (int i = 0; i < 3; i++)
                pattes[direction][2 * i].lever();
            Sleep.sleep();
            for (int i = 0; i < 3; i++)
                pattes[direction][2 * i].goto_etat(2 * i, EtatPatte.POSE);
            Sleep.sleep();
            for (int i = 0; i < 3; i++)
                pattes[direction][2 * i + 1].lever();
            Sleep.sleep();
            for (int i = 0; i < 3; i++)
                pattes[direction][2 * i + 1].goto_etat(2 * i + 1,
                        EtatPatte.POSE);
            Sleep.sleep();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Vérifications avant de bouger. On vérifie: - qu'il n'y a pas d'ennemi -
     * que le temps n'est pas écoulé
     * 
     * @throws EnnemiException
     */
    private void verif_avant_mouvement() throws EnnemiException
    {
        int attente = 0;

        while (detecter_ennemi())
        {
            System.out.println("Ennemi détecté! Attente.");
            Sleep.sleep(Config.attente_avant_evitement / 5);
            attente++;
            if (attente == 5)
                throw new EnnemiException();
        }

        if (date_debut != -1 && System.currentTimeMillis() - date_debut > 90000)
            fin_match();
    }

    /**
     * Méthode exécutée au bout de 90s
     */
    private void fin_match()
    {
        arret();
        desasserv();
        serie.close();
    }

    /**
     * Surcouche user-friendly de setDirection
     * 
     * @param dir
     */
    public void setDirection(Direction dir)
    {
        setDirection(dir.ordinal());
    }

    /**
     * "Tourne" vers la gauche (si différence < 0) ou vers la droite (si
     * différence > 0)
     * 
     * @param difference
     *            , entre -5 et +5
     */
    public void setDirectionRelatif(int difference)
    {
        setDirection(direction + difference);
    }

    /**
     * Modifie la direction. Prend du temps, mais c'est nécessaire si on ne veut
     * pas des mouvements parasites qui font bouger un peu le robot.
     * 
     * @param direction
     *            , entre -6 et 6
     */
    public void setDirection(int direction)
    {
        // Afin d'avoir direction entre 0 et 5
        direction += 6;
        direction %= 6;
        if (direction != this.direction)
        {
            capteur.tourner(direction);
            this.direction = direction;
            try
            {
                EtatPatte[] sauv = new EtatPatte[6];
                for (int i = 0; i < 6; i++)
                    sauv[i] = pattes[direction][i].getEtat();

                arret();

                for (int i = 0; i < 3; i++)
                    if (sauv[2 * i] != EtatPatte.OTHER)
                        pattes[direction][2 * i].goto_etat(2 * i, sauv[2 * i]);
                Sleep.sleep();
                for (int i = 0; i < 3; i++)
                    if (sauv[2 * i + 1] != EtatPatte.OTHER)
                        pattes[direction][2 * i + 1].goto_etat(2 * i + 1,
                                sauv[2 * i + 1]);
                Sleep.sleep();

            } catch (GoToException e)
            {
                e.printStackTrace();
            }
            System.out.println("Nouvelle direction: " + this.direction);
        }
    }

    /**
     * Lève l'hexapode (en piétinant)
     */
    public void stand_up()
    {
        try
        {
            System.out.println("L'hexapode se lève");
            // On piétine pour monter
            for (int i = 0; i < 9; i++)
            {
                pietine(i % 3, 1500, 1800 - 25 * i, 1600 + 25 * i);
                pietine(5 - i % 3, 1500, 1800 - 25 * i, 1600 + 25 * i);
            }
            // On baisse toutes les pattes
            for (int i = 0; i < 6; i++)
                pattes[direction][i].baisser();
            Sleep.sleep(500);
            // On abaisse un peu l'hexapode
            for (int i = 0; i < 6; i++)
                pattes[direction][i].goto_etat(1500, 1800, 1800);
        } catch (GoToException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Utilisé pour piétiner
     * 
     * @param num
     * @param angle0
     * @param angle1
     * @param angle2
     * @throws GoToException
     */
    private void pietine(int num, int angle0, int angle1, int angle2)
            throws GoToException
    {
        pattes[direction][num].goto_etat(angle0, angle1, angle2 - 400);
        Sleep.sleep(100);
        pattes[direction][num].goto_etat(angle0, angle1, angle2);
        Sleep.sleep(100);
    }

    /**
     * Méthode qui répond à la question "y a-t-il un ennemi devant moi?"
     * 
     * @return true s'il y a un ennemi devant false sinon
     */
    private boolean detecter_ennemi()
    {
        if (!capteur_actif)
            return false;
        return capteur.mesure() < Config.distance_detection;
    }

    /**
     * Met à jour la position. On n'a pas d'odométrie, on estime juste.
     * Peut-être désactivé afin de ne pas avoir de BordureException
     * Par contre, si c'est désactivé, va_au_point aura un comportement faussé.
     */
    private void maj_position()
    {
        if(maj_position)
        {
            position.add(new Vec2((int) Math.round(Patte.avancee_effective
                    * Math.cos(Math.PI / 2 - direction * Math.PI / 3)), (int) Math
                    .round(Patte.avancee_effective
                            * Math.sin(Math.PI / 2 - direction * Math.PI / 3))));
            // System.out.println(position);
        }
    }

    public void wait_jumper()
    {
        while(!capteur.jumper())
            Sleep.sleep(100);
        date_debut = System.currentTimeMillis();
        capteur_actif = true;
        position = new Vec2(1300, 1800); // TODO ajuster
    }

    /**
     * Modifie les constantes (lever les pattes plus haut,
     * faire de plus grands pas, ...)
     */
    public void setProfil(Profil profil)
    {
        Patte.profil_actuel = profil;
    }

}
