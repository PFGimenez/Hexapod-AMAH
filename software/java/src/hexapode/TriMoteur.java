package hexapode;

import hexapode.exceptions.GoToException;
import serial.Serial;
import serial.SerialException;

	/**
	 * Classe des moteurs. Visibilité en "friendly".
	 * @author pf
	 *
	 */

class TriMoteur {

	private Serial serie;
	private int id;
	private boolean desasservi;
	private int curAngles[];
	
    private static final int[] angle_min = {900, 500, 1000};  // sécurité des moteurs
    private static final int[] angle_max = {1500, 2000, 2000}; // sécurité des moteurs
    
	/**
	 * Construit trois moteurs, désasservis.
	 * @param serie
	 * @param id
	 * @param etat
	 * @param angle_min
	 * @param angle_max
	 */
	public TriMoteur(Serial serie, int id)
	{
		this.serie = serie;
		this.id = id;
		desasserv();
		curAngles = new int[3];
	}

    /**
     * Deplacement groupé des trois moteurs
     * Lève une exception si un angle ne satisfait pas les bornes.
     * @param angles
     * @param temps de déplacement en ms
     * @throws GoToException
     */
    public void goto_etat(int[] angles, int temps) throws GoToException
	{	
	    for(int i = 0; i < 2; i++)
	        if(angles[i] < angle_min[i] || angles[i] > angle_max[i])
	            throw new GoToException(i);
        if(serie != null)
            try {
                String ordre = new String();
                // S500 pour régler la vitesse maximale
                for(int i = 0; i < 3; i++)
                {
                    ordre += "#"+Integer.toString(id+i)+"P"+Integer.toString(angles[i])+" ";
                    curAngles[i] = angles[i];
                }
                if(!desasservi) // c'est la datasheet du SSC-32 qui le dit.
                    ordre += "T"+Integer.toString(temps);
                serie.communiquer(ordre);
            } catch (SerialException e1) {
                e1.printStackTrace();
            }
        desasservi = false;
     }

	/**
	 * Désasservit les moteurs
	 */
	public void desasserv()
	{
        if(serie != null)
    		try {
                for(int i = 0; i < 3; i++)
                    serie.communiquer("#"+Integer.toString(id+i)+"L");
    		} catch (SerialException e1) {
    			e1.printStackTrace();
    		}		
        desasservi = true;
	}
	
	public int[] getAngles()
	{
		return curAngles;
	}
	
}
