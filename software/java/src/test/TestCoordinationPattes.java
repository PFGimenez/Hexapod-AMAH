package test;

import hexapode.GoToException;
import hexapode.Hexapode;
import hexapode.markov.Markov;

public class TestCoordinationPattes extends Test {

	public TestCoordinationPattes(Hexapode hexapode, int nbIteration, double consecutiveLearnTime, double pauseTime, boolean restartMarkov, boolean validation) 
	{
		super(hexapode, nbIteration, consecutiveLearnTime, pauseTime, restartMarkov, validation);
	}

	@Override
	public void onStart() {
		etat_actuel = etat_suivant;
	}
	
	@Override
	public void onExit()
	{
		super.onExit();
		markov.updateMatrix(note, etat_actuel, etat_suivant);
	}
	

	@Override
	public void onBreak() {
		hexapode.desasserv();
	}

	@Override
	public void proceedTest() {
		//On r�cup�re l'�tat suivant � tester
		String nEtatSuivant = markov.next();
		etat_suivant = new String(nEtatSuivant);
		
		//On demande � l'hexapode de se mettre en position
		try {
			hexapode.goto_etat((String.valueOf(nEtatSuivant)));
		} catch (GoToException e) {
			e.printStackTrace();
		}
		
		//On calcule la note en fonction de la transition 
		calcNote();
	}

	@Override
	public void validTest() {
		// TODO Auto-generated method stub

	}

	@Override
	public void terminate() {
		super.terminate();	// sauvegarde
		hexapode.desasserv();
	}

	@Override
	public void init() 
	{
		markov = new Markov(2);
	}
	
	private void calcNote()
	{
		note = 0;
		int nbRetourArriere = 0;
		char[] cEtatSuivant = etat_suivant.toCharArray();
		char[] cEtatActuel = etat_actuel.toCharArray();
		for(int i = 0; i < 6; i++)
		{
			if(cEtatActuel[i] == '0')
			{
				if(cEtatSuivant[i] == '1')
				{
					note += 5;
				}
				else
				{
					nbRetourArriere++;
				}
			}
			else
			{
				if(cEtatSuivant[i] == '1')
				{
					note += 5;
				}
			}
		}
		
		if(nbRetourArriere >= 3)
		{
			note += nbRetourArriere * 10;
		}
		else
		{
			note -=  -40;
		}
	}

}
