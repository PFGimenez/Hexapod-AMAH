package hexapode.markov;

/**
 * Classe abstraite � h�riter pour coder les tests.
 * @author Stud
 *
 */

public abstract class Test {
	
	protected int nbIteration;				//Nombre de tests � executer
	protected double consecutiveLearnTime;	//Temps de test entre chaque pause en seconde
	protected double pauseTime;				//Temps d'arr�t par pause en seconde
	
	public void Test(int nbIteration, double consecutiveLearnTime, double pauseTime)
	{
		this.nbIteration = nbIteration;
		this.consecutiveLearnTime = consecutiveLearnTime;
		this.pauseTime = pauseTime;
	}

	public abstract void onStart();
	public abstract void onExit();
	public abstract void proceedTest();
	
	public int getNbIteration()
	{
		return nbIteration;
	}
	
	public double getConsecutiveLearnTime()
	{
		return consecutiveLearnTime;
	}

	public double getPauseTime()
	{
		return pauseTime;
	}
	
}
