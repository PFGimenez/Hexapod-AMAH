package hexapode.markov;

/**
 * Etat d'une patte
 * @author pf
 *
 */
public class EtatPatte {

	public EtatMoteur[] em = new EtatMoteur[3];

	/**
	 * Constructeur classique
	 * @param em
	 */
	public EtatPatte(EtatMoteur[] em)
	{
		this.em = em;
	}

	public EtatPatte(int angle0, int angle1, int angle2)
	{
		em[0] = new EtatMoteur(angle0);
		em[1] = new EtatMoteur(angle1);
		em[2] = new EtatMoteur(angle2);
	}
	
	public EtatPatte(boolean leve)
	{
		em[0] = new EtatMoteur(1500);
		if(leve)
			em[1] = new EtatMoteur(2000);
		else
			em[1] = new EtatMoteur(1200);
		em[2] = new EtatMoteur(1200);
	}
	
	public boolean isLeve()
	{
		return equals(new EtatPatte(true));
	}
	
	public boolean equals(EtatPatte e)
	{
		return e.em[0].angle == em[0].angle
				&& e.em[1].angle == em[1].angle
				&& e.em[2].angle == em[2].angle;
	}

	/**
	 * Constructeur aléatoire
	 */
	public EtatPatte()
	{
		for(int i = 0; i < 3; i++)
			em[i] = new EtatMoteur();
	}

	public void change_moteur(int nbMoteur, int angle)
	{
		em[nbMoteur].angle = angle;
	}
}
