package demo2;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Razred koji predstavlja demonstracijski program za rad sa razredom
 * {@link Logger} i općenito paketom <b>java.util.logging</b>
 * 
 *@see java.util.logging
 *@see Logger
 *  
 *  @author Davor Češljaš
 */
public class Logiranje {

	/** Konstantan primjerak razreda {@link Logger} */
	private static final Logger LOG = Logger.getLogger("demo2");

	/**
	 * Metoda od koje započinje izvođenje ovog programa.
	 * 
	 * @param args
	 *            argumenti naredbenog redka. Ovdje se ne koriste.
	 */
	public static void main(String[] args) {

		Level[] levels = new Level[] { Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER,
				Level.FINEST };
		for (Level l : levels) {
			LOG.log(l, "Ovo je poruka " + l + " razine.");
		}
	}
}
