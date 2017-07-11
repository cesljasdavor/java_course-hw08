package hr.fer.zemris.bf.qmc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Razred predstavlja demonstracijski program koji demonstrira rad s razredom
 * {@link Minimizer}. 
 * 
 * @author Davor Češljaš
 */
public class Main {

	/**
	 * Metoda od koje započinje izvođenje programa.
	 *
	 * @param args
	 *            argumenti naredbenog redka. Ovdje se ne koriste
	 */
	public static void main(String[] args) {
		Set<Integer> minterms = new HashSet<>(Arrays.asList(4,5,6,7,8,9,11));
		Set<Integer> dontcares = new HashSet<>(Arrays.asList(2,3,12,15));
		Minimizer minimizer = new Minimizer(minterms, dontcares, Arrays.asList("A", "B", "C", "D"));
		minimizer.getMinimalFormsAsString().forEach(System.out::println);
	}
}
