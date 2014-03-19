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

	/**
	 * Constructeur aléatoire
	 */
	public EtatPatte()
	{
		for(int i = 0; i < 3; i++)
			em[i] = new EtatMoteur();
	}

	/**
	 * Constructeur user-friendly
	 * @param angle0
	 * @param angle1
	 * @param angle2
	 */
	public EtatPatte(int angle0, int angle1, int angle2)
	{
		em[0] = new EtatMoteur(angle0);
		em[1] = new EtatMoteur(angle1);
		em[2] = new EtatMoteur(angle2);
	}
	
	/**
	 * Constructeur de patte debout ou baissée
	 * @param leve
	 */
	public EtatPatte(boolean leve)
	{
		em[0] = new EtatMoteur(1500);
		if(leve)
			em[1] = new EtatMoteur(2000);
		else
			em[1] = new EtatMoteur(1200);
		em[2] = new EtatMoteur(1200);
	}

	public EtatPatte(EtatPatteTest2 etat)
	{
		if(etat == EtatPatteTest2.LEVE || etat == EtatPatteTest2.BAISSE)
			em[0] = new EtatMoteur(1500);
		else if(etat == EtatPatteTest2.ARRIERE)
			em[0] = new EtatMoteur(1200);
		else if(etat == EtatPatteTest2.AVANT)
			em[0] = new EtatMoteur(1800);
		if(etat == EtatPatteTest2.LEVE)
			em[1] = new EtatMoteur(2000);
		else
			em[1] = new EtatMoteur(1200);
		if(etat == EtatPatteTest2.LEVE)
			em[2] = new EtatMoteur(1800);
		else
			em[2] = new EtatMoteur(1200);
	}

	/**
	 * La patte est-elle levée?
	 * @return true si c'est le cas, false sinon
	 */
	public boolean isLeve()
	{
		return equals(new EtatPatte(true));
	}

	/**
	 * Méthode equals
	 */
	@Override
	public boolean equals(Object e)
	{
		return e instanceof EtatPatte
				&& ((EtatPatte) e).em[0].angle == em[0].angle
				&& ((EtatPatte) e).em[1].angle == em[1].angle
				&& ((EtatPatte) e).em[2].angle == em[2].angle;
	}

}
