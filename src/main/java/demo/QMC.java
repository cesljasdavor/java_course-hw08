package demo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hr.fer.zemris.bf.model.Node;
import hr.fer.zemris.bf.parser.Parser;
import hr.fer.zemris.bf.parser.ParserException;
import hr.fer.zemris.bf.qmc.Minimizer;
import hr.fer.zemris.bf.utils.Util;
import hr.fer.zemris.bf.utils.VariablesGetter;

/**
 * Razred koji predstavlja program koji minimizira predane booleove funkcije.
 * Program od korisnika zatražuje unos izraza te provofi minimizaciju. Primjer
 * izvođenja programa (zadebljano je korisnikov unos):
 * 
 * <pre>
 * <i>
 *> <b> f(A,B,C,D) = [4,5,6,7,8,9,11] | [2,3,12,15]</b>
 *1. NOT A AND B OR A AND NOT B AND NOT C OR A AND NOT B AND D
 *2. NOT A AND B OR A AND NOT B AND NOT C OR C AND D
 *3. NOT A AND B OR A AND NOT C AND NOT D OR A AND NOT B AND D
 *> <b>f(A,B,C,D) = NOT A AND NOT B AND (NOT C OR D) OR A AND C | NOT A AND B AND NOT D</b>
 *1. A AND C OR NOT A AND NOT B AND NOT C OR NOT A AND NOT B AND D
 *2. A AND C OR NOT A AND NOT B AND NOT C OR NOT B AND C AND D
 *3. A AND C OR NOT A AND NOT C AND NOT D OR NOT A AND NOT B AND D
 * <i>
 * </pre>
 * 
 * Razred za parsiranje izraza koristi primjerke razreda {@link Parser} , a za
 * parsiranje uglatih zagrada svoj interni parser. Korisniku se daje na volju
 * kako će unijeti svoju booleovu funkciju (moguća je i kombinacije predhodno
 * pokazana dva načina). Ukoliko je korisnik unio pogrešnu definiciju booleave
 * funkcije program će ispisati poruku, ali će nastaviti s radom. Program
 * završava s radom unosom {@value #EXIT}.
 * 
 * <pre>
 * <b>Napomena: Ime funkcija i ime varijabli mora započinjati slovom
 * te nakon toga imati opcionalni broj brojki ili slova.</b>
 * </pre>
 * 
 * @see Minimizer
 * @see Parser
 * 
 * @author Davor Češljaš
 */
public class QMC {

	/**
	 * Konstanta koja predstavlja znakovni niz koji korisnik treba upisati kako
	 * bi program završio.
	 */
	private static final String EXIT = "exit";

	/**
	 * Konstanta koja predstavlja znak koji se ispisuje prilikom svakog upita da
	 * korisnik upiše funkciju koju je potrebno minimizirati
	 */
	private static final String PROMPT = "> ";

	/** Konstanta koja predstavlja niz znakova "=" */
	private static final String EQUALS = "=";

	/** Konstanta koja predstavlja niz znakova "\\|" */
	private static final String PIPE = "\\|";

	/** Konstanta koja predstavlja niz znakova "\\(" */
	private static final String OPEN_BRACKET = "\\(";

	/** Konstanta koja predstavlja niz znakova "[" */
	private static final String OPEN_SQUARE_BRACKET = "[";

	/** Konstanta koja predstavlja niz znakova "," */
	private static final String COMMA = ",";

	/**
	 * Metoda od koje započinje izvođenje programa.
	 *
	 * @param args
	 *            Argumenti naredbenog redka. Ovdje se ne koriste.
	 */
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.print(PROMPT);
			String line = sc.nextLine().trim();
			if (line.equalsIgnoreCase(EXIT)) {
				break;
			}
			if (line.isEmpty()) {
				continue;
			}

			String[] splitted = line.split(EQUALS);
			if (splitted.length != 2) {
				System.out.println("Pogreška: funkcija nije ispravno zadana.");
				continue;
			}

			try {
				minimize(splitted);
			} catch (RuntimeException e) {
				System.out.println("Pogreška: " + e.getMessage());
			}
		}
		sc.close();
	}

	/**
	 * Pomoćna metoda unutar koje se vrši parsiranje predanog izraza ili brojeva
	 * minterma i minimizacija tog izraza.
	 *
	 * @param splitted
	 *            parametar koji predstavlja korisnikov unos rastavljen metodom
	 *            {@link String#split(String)} po {@value #EQUALS}
	 */
	private static void minimize(String[] splitted) {
		List<String> variables = extractFunction(splitted[0].trim());

		String[] mintermsAndDontCares = splitted[1].trim().split(PIPE);
		if (mintermsAndDontCares.length < 1 || mintermsAndDontCares.length > 2) {
			throw new IllegalArgumentException("Naveli ste pogrešan broj znakova '|' u Vašem unosu!");
		}

		Set<Integer> minterms = extractIndexes(mintermsAndDontCares[0].trim(), variables);
		Minimizer minimizer = null;
		if (mintermsAndDontCares.length == 1) {
			minimizer = new Minimizer(minterms, new LinkedHashSet<>(), variables);
		} else {
			Set<Integer> dontCares = extractIndexes(mintermsAndDontCares[1].trim(), variables);
			minimizer = new Minimizer(minterms, dontCares, variables);
		}

		List<String> minimalForms = minimizer.getMinimalFormsAsString();
		for (int i = 0, len = minimalForms.size(); i < len; i++) {
			System.out.printf("%d. %s%n", (i + 1), minimalForms.get(i));
		}
	}

	/**
	 * Pomoćna metoda koja parsira funkciju i njezine argumenta. Funkcija (i
	 * pripadni argumenti) moraju biti oblika: <i>ime_funkcije(varijabla1,
	 * varijabla2, ... , varijablaN)</i>. Ime funkcija i ime varijabli mora
	 * započinjati slovom te nakon toga imati opcionalni broj brojki ili slova.
	 * Za ekstrahiranje varijabli koristi se metoda
	 * {@link #extractVariables(String, Pattern)}.
	 *
	 * @param function
	 *            primjerka razreda {@link String} koji se pokušava parsirati u
	 *            ime funkcije i argumente te funkcije
	 * @return {@link List} primjeraka razreda {@link String} koji predstavlja
	 *         ime varijabli
	 */
	private static List<String> extractFunction(String function) {
		Pattern functionPattern = Pattern.compile("^[^\\(]+\\([^\\(\\)]*\\)$");
		Matcher matcher = functionPattern.matcher(function.trim());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Pogrešno ste unijeli naziv funkcije i varijable!");
		}

		String[] splitted = function.split(OPEN_BRACKET);

		Pattern varAndFuncPattern = Pattern.compile("\\p{Alpha}\\p{Alnum}*");
		Matcher functionMatcher = varAndFuncPattern.matcher(splitted[0].trim());
		if (!functionMatcher.matches()) {
			throw new IllegalArgumentException(
					"Naziv funkcije počinje sa slovom, nakon čega smije doći brojke ili slova!");
		}

		String variables = splitted[1].trim();
		variables = variables.substring(0, variables.length() - 1);
		return extractVariables(variables, varAndFuncPattern);
	}

	/**
	 * Pomoćna metoda koja ekstrahira varijable te ih pohranjuje u obliku
	 * primjerka razreda {@link String} unutar {@link List}e.
	 *
	 * @param variablesString
	 *            dio korisnikova unosa iz kojeg se vade varijable
	 * @param pattern
	 *            regex koji je potrebno zadovoljiti da bi varijabla bila
	 *            spremljena u listu
	 * @return {@link List} primjeraka razreda {@link String} koji predstavlja
	 *         popis varijabli
	 */
	private static List<String> extractVariables(String variablesString, Pattern pattern) {
		List<String> variables = new ArrayList<>();

		for (String variable : variablesString.split(COMMA)) {
			variable = variable.trim();
			Matcher matcher = pattern.matcher(variable);
			if (!matcher.matches()) {
				throw new IllegalArgumentException(
						"Varijabla '" + variable + "' ne počinje sa slovom ili se ne sastoji samo od slova i brojeva!");
			}
			variables.add(variable.toUpperCase());
		}

		return variables;
	}

	/**
	 * Pomoćna metoda koja iz predanog parametra <b>indexesString</b> parsira
	 * skup {@link Set} svih minterma ili don't careova koje je korisnik zadao.
	 * Ukoliko je korisnik zadao izraz kao booleovu funkciju poziva se metoda
	 * {@link #parseExpression(String, List)}, a ukoliko je korisnik predao
	 * popis minterma unutar uglatih zagrada poziva se metoda
	 * {@link #parseInputInsideBrackets(String)}
	 * 
	 * @param indexesString
	 *            korisnikov unos za minterme odnosno don't careove
	 * @param passedVariables
	 *            varijable čiji je popis parsiran s lijeve strane znaka
	 *            {@value #EQUALS}
	 * @return skup {@link Set} svih minterma ili don't careova koje je korisnik
	 *         zadao.
	 */
	private static Set<Integer> extractIndexes(String indexesString, List<String> passedVariables) {
		if (indexesString.startsWith(OPEN_SQUARE_BRACKET)) {
			Pattern pattern = Pattern.compile("\\[[^\\[\\]]+\\]");
			Matcher matcher = pattern.matcher(indexesString);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("Pogrešan format unosa. Unos mora biti između '[' i ']'");
			}
			// ispitano je počinje li i završava li s [ i ]
			return parseInputInsideBrackets(indexesString.substring(1, indexesString.length() - 1));
		}
		return parseExpression(indexesString, passedVariables);
	}

	/**
	 * Pomoćna metoda koja korisnikov unos predan kao booleava funkcija parsira
	 * pomoću primjerka razreda {@link Parser}. Potom, ukoliko je parsiranje
	 * uspjelo, metoda dohvaća sve varijable koje su spominjane unutar samog
	 * izraza i uspoređuje ih s varijablama čiji je popis parsiran s lijeve
	 * strane znaka {@value #EQUALS}. Ukoliko je sve regularno metoda vraća
	 * rezultat od {@link Util#toSumOfMinterms(List, Node)}
	 *
	 * @param expressionString
	 *            booleava funkcija koja se parsira unutar primjerka razreda
	 *            {@link String}
	 * @param passedVariables
	 *            varijable čiji je popis parsiran s lijeve strane znaka
	 *            {@value #EQUALS}
	 * @return Ukoliko je sve regularno metoda vraća rezultat od
	 *         {@link Util#toSumOfMinterms(List, Node)}
	 * @throws ParserException
	 *             ukoliko parsiranje predanog izraza nije moguće
	 * @throws IllegalArgumentException
	 *             ukoliko unutar izraza postoje varijable koje nisu unutar
	 *             <b>passedVariables</b> ili je unutar izraza jednostavno veći
	 *             broj varijabli nego unutar <b>passedVariables</b>
	 */
	private static Set<Integer> parseExpression(String expressionString, List<String> passedVariables) {
		Parser parser = new Parser(expressionString);
		Node expression = parser.getExpression();
		VariablesGetter variablesGetter = new VariablesGetter();
		expression.accept(variablesGetter);
		List<String> variables = variablesGetter.getVariables();
		
		if (!passedVariables.containsAll(variables) || variables.size() > passedVariables.size()) {
			throw new IllegalArgumentException("Predani izraz sastoji se od pogrešnih varijabli!");
		}

		return Util.toSumOfMinterms(variables, expression);
	}

	/**
	 * Pomoćna metoda koja predani parametar <b>input</b> parsira kao listu
	 * primjeraka razreda {@link Integer} koji su odvojeni zarezom.
	 *
	 * @param lista
	 *            potencijalnih primjeraka razreda {@link Integer} koja se
	 *            parsira
	 * @return Skup {@link Set} primjeraka razreda {@link Integer} koji je
	 *         parsiran iz <b>input</b>
	 * @throws NumberFormatException
	 *             ukoliko neki član liste nije moguće parsirati u primjerak
	 *             razreda {@link Integer} preko metode
	 *             {@link Integer#parseInt(String)}
	 * 
	 * @see Integer#parseInt(String)
	 */
	private static Set<Integer> parseInputInsideBrackets(String input) {
		Set<Integer> indexes = new LinkedHashSet<>();

		for (String indexString : input.split(COMMA)) {
			indexString = indexString.trim();
			indexes.add(Integer.parseInt(indexString));
		}

		return indexes;
	}
}
