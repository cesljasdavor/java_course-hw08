package hr.fer.zemris.bf.qmc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import hr.fer.zemris.bf.model.BinaryOperatorNode;
import hr.fer.zemris.bf.model.ConstantNode;
import hr.fer.zemris.bf.model.Node;
import hr.fer.zemris.bf.utils.NodeAsExpressionPrinter;

/**
 * Razred predstavlja minimizator booleaivih funkcija. Primjerci ovog razreda
 * funkcije minimiziraju metodom Quine-McCluskey s Pyne-McCluskey pristupom.
 * <a href =
 * "https://en.wikipedia.org/wiki/Quine%E2%80%93McCluskey_algorithm">metodom
 * Quine-McCluskey s Pyne-McCluskey pristupom.</a> Razred predstavlja
 * implementaciju navedenog minimizacijskog postupka za jednu Booleovu funkciju.
 * Razred prilikom minimizacije koristi primjerke razreda {@link Mask}. Razred
 * nudi jedan konstruktor: {@link #Minimizer(Set, Set, List)}. Razreda također
 * nudi 2 različite metode za dohvat rezultata minimizacije:
 * <ul>
 * <li>{@link #getMinimalFormsAsExpressions()}</li>
 * <li>{@link #getMinimalFormsAsString()}</li>
 * </ul>
 * 
 * @see Mask
 *
 * @author Davor Češljaš
 */
public class Minimizer {

	/**
	 * Konstanta koja predstavlja primjerak razreda {@link Logger} koji se
	 * koristi za ispis postupka minimizacije
	 */
	private static final Logger LOG = Logger.getLogger("hr.fer.zemris.bf.qmc");

	/**
	 * Konstanta koja se koristi prilikom formatiranog ispisa, a predstavlja
	 * duplu crtu
	 */
	private static final String DOUBLE_LINE = "=================================";

	/**
	 * Konstanta koja se koristi prilikom formatiranog ispisa, a predstavlja
	 * crtu
	 */
	private static final String SINGLE_LINE = "-------------------------------";

	/**
	 * Konstanta koja se koristi prilikom formatiranog ispisa, a predstavlja niz
	 * znakova "Stupac tablice:"
	 */
	private static final String COLUMN_STRING = "Stupac tablice:";

	/**
	 * Konstanta koja se koristi prilikom formatiranog ispisa, a predstavlja niz
	 * znakova "Pronašao primarni implikant: "
	 */
	private static final String PRIMARY_IMPLICANT_FOUND = "Pronašao primarni implikant: ";

	/** Konstanta koja predstavlja prazan niz znakova */
	private static final String EMPTY = "";

	/**
	 * Konstanta koja predstavlja prazan niz znakova "OR"
	 */
	private static final String OR = "OR";

	/**
	 * Konstanta koja predstavlja prazan niz znakova "AND"
	 */
	private static final String AND = "AND";

	/** Članska varijabla koja predstavlja brojčani {@link Set} minterma */
	private Set<Integer> mintermSet;

	/** Članska varijabla koja predstavlja brojčani {@link Set} don't careova */
	private Set<Integer> dontCareSet;

	/**
	 * Članska varijabla koja predstavlja {@link List} primjeraka razreda
	 * {@link String} koji predstavljaju nazive varijabli
	 */
	private List<String> variables;

	/**
	 * Članska varijabla koja predstavlja {@link List} {@link Set}ova
	 * *primjeraka razreda {@link Mask}. Svaki {@link Set} predstavlja jedno
	 * rješenje minimizacije
	 */
	private List<Set<Mask>> minimalForms;

	/**
	 * Konstruktor koji inicijalizira primjerke ovog razreda. Unutar
	 * konstrukotra vrši se detaljna provjera predanih argumenata
	 * <b>mintermSet</b>, <b>dontCareSet</b> te <b>variables</b>. Ukoliko se
	 * <b>mintermSet</b> i <b>dontCareSet</b> preklapaju prekida se postupak.
	 * Ukoliko je bilo koji od {@link Set}ova <code>null</code> ili je predana
	 * {@link List}a <code>null</code> baca se {@link IllegalArgumentException}.
	 * Ukoliko je predana funkcija tautologija ili kontradikcija prekida se
	 * postupak uz loggiranje prigodne poruke.
	 *
	 * @param mintermSet
	 *            brojčani {@link Set} minterma koji se minimizira
	 * @param dontCareSet
	 *            brojčani {@link Set} don't careova pomoću kojeg se minimizira
	 * @param variables
	 *            {@link List} primjeraka razreda {@link String} koji
	 *            predstavlja nazive varijabli unutar booleove funkcije
	 */
	public Minimizer(Set<Integer> mintermSet, Set<Integer> dontCareSet, List<String> variables) {
		if (!checkArguments(mintermSet, dontCareSet, variables)) {
			return;
		}
		checkNonOverlapping(mintermSet, dontCareSet);

		this.mintermSet = mintermSet;
		this.dontCareSet = dontCareSet;
		this.variables = variables;

		minimize();
	}

	/**
	 * Metoda koja dohvaća sve minimalne oblike kao {@link List} primjeraka
	 * razreda {@link Node}. Svaki primjerak tog razreda predstavlja upravo
	 * jedan minimalni oblik predane booleove funkcije.
	 *
	 * @return sve minimalne oblike kao {@link List} primjeraka razreda
	 *         {@link Node}.
	 * @see Node
	 *
	 */
	public List<Node> getMinimalFormsAsExpressions() {
		if (minimalForms == null) {
			throw new UnsupportedOperationException("Minimizacija nije uspjela!");
		}
		List<Node> minimalNodes = new ArrayList<>();
		
		for (Set<Mask> minimalForm : minimalForms) {
			//kontradikcija (ako je ovo će biti prva iteracija)
			if(minimalForm.isEmpty()) {
				minimalNodes.add(new ConstantNode(false));
				return minimalNodes;
			}
			Node minimalNode = combineWithOr(minimalForm);
			minimalNodes.add(minimalNode);
		}
		return minimalNodes;
	}

	/**
	 * Pomoćna metoda koja gradi čvor predstavljen sučeljem {@link Node} koji
	 * predstavlja sumu produkata jedne minimalne forme pohranjene unutar
	 * {@link Set}a <b>minimalForms</b>
	 *
	 * @param minimalForm
	 *            sumu produkata jedne minimalne forme
	 * @return čvor predstavljen sučeljem {@link Node} koji predstavlja sumu
	 *         produkata jedne minimalne forme
	 */
	private Node combineWithOr(Set<Mask> minimalForm) {
		List<Node> children = new ArrayList<>();
		for (Mask mask : minimalForm) {
			Node child = combineWithAnd(mask);
			children.add(child);
		}
		if (children.size() == 1) {
			return children.get(0);
		}
		return new BinaryOperatorNode(OR, children, Boolean::logicalOr);
	}

	/**
	 * Pomoćna metoda koja iz predanog primjerka razreda {@link Mask}
	 * <b>mask</b> stvara čvor predstavljen sučeljem {@link Node}. Taj čvor
	 * predstavlja produkt svih čvorova dobivenih pozivom
	 * {@link Mask#toVariableNodes(List)} nad predanim primjerkom razreda
	 * {@link Mask}.
	 *
	 * @param mask
	 *            primarni implikant
	 * @return produkt svih čvorova dobivenih pozivom
	 *         {@link Mask#toVariableNodes(List)} nad predanim primjerkom
	 *         razreda {@link Mask}.
	 */
	private Node combineWithAnd(Mask mask) {
		List<Node> children = mask.toVariableNodes(variables);
		if (children.size() == 1) {
			return children.get(0);
		}
		return new BinaryOperatorNode(AND, children, Boolean::logicalAnd);
	}

	/**
	 * Metoda koja dohvaća sve minimalne oblike kao {@link List} primjeraka
	 * razreda {@link String}. Svaki primjerak tog razreda predstavlja jedan
	 * minimalni oblik predane booleove funkcije. Metoda interno poziva metodu
	 * {@link #getMinimalFormsAsExpressions()}
	 *
	 * @return sve minimalne oblike kao {@link List} primjeraka razreda
	 *         {@link String}.
	 */
	public List<String> getMinimalFormsAsString() {
		List<String> expressions = new ArrayList<>();

		List<Node> minimalNodes = getMinimalFormsAsExpressions();
		for (Node minimalNode : minimalNodes) {
			NodeAsExpressionPrinter printer = new NodeAsExpressionPrinter();
			minimalNode.accept(printer);
			expressions.add(printer.getExpressionAsString());
		}

		return expressions;
	}

	/**
	 * Pomoćna metoda koja provjerava ispravnost predanih argumenata
	 * konstruktoru. Ukoliko se <b>mintermSet</b> i <b>dontCareSet</b>
	 * preklapaju vraća se <code>false</code>. Ukoliko je bilo koji od
	 * {@link Set}ova <code>null</code> ili je predana {@link List}a
	 * <code>null</code> baca se {@link IllegalArgumentException}. Ukoliko je
	 * predana funkcija tautologija ili kontradikcija vraća se
	 * <code>false</code> uz loggiranje prigodne poruke.
	 *
	 * @param mintermSet
	 *            brojčani {@link Set} minterma koji se minimizira
	 * @param dontCareSet
	 *            brojčani {@link Set} don't careova pomoću kojeg se minimizira
	 * @param variables
	 *            {@link List} primjeraka razreda {@link String} koji
	 *            predstavlja nazive varijabli unutar booleove funkcije
	 * @return <code>true</code> ako i samo ako su svi predani argumenti
	 *         ispravni, <code>false</code> inače
	 * 
	 * @throws IllegalArgumentException
	 *             Ukoliko je bilo koji od {@link Set}ova <code>null</code> ili
	 *             je predana {@link List}a <code>null</code>
	 */
	private boolean checkArguments(Set<Integer> mintermSet, Set<Integer> dontCareSet, List<String> variables) {
		if (mintermSet == null) {
			throw new IllegalArgumentException("Skup minterma ne smije biti null");
		}
//		if (mintermSet.isEmpty()) {
//			if (LOG.isLoggable(Level.SEVERE)) {
//				LOG.log(Level.SEVERE, "Funkcija je kontradikcija!");
//			}
//			return false;
//		}
		if (variables == null) {
			throw new IllegalArgumentException("Lista varijabli ne smije biti null!");
		}
		if (variables.isEmpty()) {
			if (LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Predali ste prazan skup varijabli!");
			}
			return false;
		}
//		if (mintermSet.size() == Math.pow(2, variables.size())) {
//			if (LOG.isLoggable(Level.SEVERE)) {
//				LOG.log(Level.SEVERE, "Funkcija je tautologija!");
//			}
//			return false;
//		}
		if (dontCareSet == null) {
			throw new IllegalArgumentException("Don't care skup ne smije biti null");
		}
		return true;
	}

	/**
	 * Pomoćna metoda koja provjerava postoji li preklapanja unutar
	 * {@link Set}ova <b>mintermSet</b> i <b>dontCareSet</b>. Ukoliko postoji
	 * metoda baca {@link IllegalArgumentException}
	 *
	 * @param mintermSet
	 *            brojčani {@link Set} minterma koji se minimizira
	 * @param dontCareSet
	 *            brojčani {@link Set} don't careova pomoću kojeg se minimizira
	 * 
	 * @throws IllegalArgumentException
	 *             ukoliko postoji preklapanje između {@link Set}ova
	 */
	private void checkNonOverlapping(Set<Integer> mintermSet, Set<Integer> dontCareSet) {
		if (mintermSet.removeAll(dontCareSet)) {
			throw new IllegalArgumentException("Skup mintermi ima preklapanja sa skupom don't careova");
		}
	}

	/**
	 * Pomoćna metoda od koje kreće postupak minimizacije metodom metodom
	 * Quine-McCluskey s Pyne-McCluskey pristupom.
	 * 
	 */
	private void minimize() {
		Set<Mask> primCover = findPrimaryImplicants();
		minimalForms = chooseMinimalCover(primCover);
	}

	/**
	 * Pomoćna metoda koja traži sve primarne implikante te ih vraća kao
	 * {@link Set} primjeraka razreda {@link Mask}.
	 *
	 * @return {@link Set} primjeraka razreda {@link Mask} koji predstavlja sve
	 *         primarne implikante
	 */
	private Set<Mask> findPrimaryImplicants() {
		List<Set<Mask>> column = createFirstColumn();
		List<Set<Mask>> nextColumn = createColumn(column.size() - 1);
		Set<Mask> primaryImplicants = new LinkedHashSet<>();
		while (!column.isEmpty()) {
			boolean nextColumnModified = false;
			for (int i = 0, len = column.size() - 1; i < len; i++) {
				for (Mask lowerMask : column.get(i)) {
					for (Mask greaterMask : column.get(i + 1)) {
						nextColumnModified |= combineMasks(lowerMask, greaterMask, nextColumn, i);
					}
				}
			}
	
			logCurrentColumn(column, Level.FINER);
			extractPossiblePrimaryImplicants(column, primaryImplicants);
			if (!nextColumnModified) {
				break;
			}
			column = nextColumn;
			nextColumn = createColumn(column.size() - 1);
		}
		if (primaryImplicants.isEmpty()) {
			LOG.log(Level.FINE, "Nisam pronašao niti jedan primarni implikant!");
		} else {
			logImplicants(primaryImplicants, "Svi primarni implikanti:", Level.FINE);
		}
		return primaryImplicants;
	}

	/**
	 * Pomoćna metoda koja iz {@link #mintermSet} i {@link #dontCareSet} gradi
	 * prvi stupac minimizacije metodom Quine-McCluskey s Pyne-McCluskey
	 * pristupom.
	 * 
	 * @return {@link List} {@link Set}ova primjeraka razreda {@link Mask}, gdje
	 *         svaki {@link Set} predstavalja broj jedninica [0,
	 *         2^broj_varijabli_u_{@link #variables} -1]
	 */
	private List<Set<Mask>> createFirstColumn() {
		int numberOfVariables = variables.size();
		List<Set<Mask>> firstColumnList = createColumn(numberOfVariables + 1);
	
		appendFirstColumnListElements(firstColumnList, mintermSet, numberOfVariables, false);
		appendFirstColumnListElements(firstColumnList, dontCareSet, numberOfVariables, true);
		return firstColumnList;
	}

	/**
	 * Pomoćna metoda koja asistira u gradnji prvog redka minimizacije metodom
	 * Quine-McClusky. Metoda služi kako se kod ne bi duplicirao za dodavnje
	 * minterma i don't careova u prvi redak
	 *
	 * @param firstColumnList
	 *            {@link List} {@link Set} primjeraka razreda {@link Mask} koji
	 *            predstavlja prvi redak
	 * @param set
	 *            ili {@link Set} minterma ili {@link Set} don't careova
	 * @param numberOfVariables
	 *            broj varijabli koji se predaje konstruktoru razreda
	 *            {@link Mask#Mask(int, int, boolean)}
	 * @param dontCare
	 *            zastavica koja ukazuje radi li metoda sa {@link Set}om
	 *            minterma (<code>false</code>) ili {@link Set}om don't careova
	 *            (<code>true</code>)
	 */
	private void appendFirstColumnListElements(List<Set<Mask>> firstColumnList, Set<Integer> set, int numberOfVariables,
			boolean dontCare) {
		for (Integer minterm : set) {
			// ako se maska pokuša stvoriti sa indeksom koji nije unutar
			// [0, 2^numberOfVariables -1]
			// baca se IllegalArgumentException
			Mask mask = new Mask(minterm, numberOfVariables, dontCare);
			firstColumnList.get(mask.countOfOnes()).add(mask);
		}
	}

	/**
	 * Pomoćna metoda koja stvara jedan stupac tablice minimizacije metodom
	 * Quine-McClusky, veličine <b>size</b>. {@link List}a je veličine upravo
	 * <b>size</b> jer smo predhodno kombinirali sa <b>size + 1 </b> redaka
	 *
	 * @param size
	 *            broj redaka unutar tablice. Bitno je naznačiti da redak ovdje
	 *            predstavlja skup razreda {@link Mask} nastali kombinacijom dva
	 *            redka u predhodnoj iteraciji
	 * @return novi stupac tablice minimizacije metodom Quine-McClusky
	 */
	private List<Set<Mask>> createColumn(int size) {
		List<Set<Mask>> columnList = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			columnList.add(new LinkedHashSet<>());
		}
		return columnList;
	}

	/**
	 * Pomoćna metoda koja pokušava kombinirati dva primjerka razreda
	 * {@link Mask} metodom {@link Mask#combineWith(Mask)}. Ukoliko uspije novi
	 * primjerak razreda {@link Mask} dodaje se u <b>nextColumn</b> te se vraća
	 * vrijednost <code>true</code>, a <code>false</code> u suprotnom.
	 *
	 * @param lowerMask
	 *            primjerak razreda {@link Mask} nižeg redka
	 * @param greaterMask
	 *            primjerak razreda {@link Mask} višeg redka
	 * @param nextColumn
	 *            {@link List} {@link Set}ova primjeraka razreda {@link Mask}
	 *            koji gradimo
	 * @param index
	 *            redak u <b>nextColumn</b> u koji se umeće nova vrijednost
	 *            ukoliko kombiniranje uspije
	 * @return <code>true</code> ukoliko je kombiniranje uspjelo,
	 *         <code>false</code> inače
	 * 
	 * @see Mask#combineWith(Mask)
	 */
	private boolean combineMasks(Mask lowerMask, Mask greaterMask, List<Set<Mask>> nextColumn, int index) {
		Optional<Mask> optional = lowerMask.combineWith(greaterMask);
		if (!optional.isPresent()) {
			return false;
		}
	
		nextColumn.get(index).add(optional.get());
		lowerMask.setCombined(true);
		greaterMask.setCombined(true);
		return true;
	}

	/**
	 * Pomoćna metoda koja preko konstante {@link #LOG} vrši loggiranje
	 * <b>currentColumn</b> ukoliko je omogućeno loggiranje razine <b>level</b>
	 *
	 * @param currentColumn
	 *            {@link List} {@link Set}ova primjeraka razreda {@link Mask}
	 *            koji predstavlja trenutni stupac koji obrađujemo unutar
	 *            minimizacije
	 * @param level
	 *            razina loggiranja
	 */
	private void logCurrentColumn(List<Set<Mask>> currentColumn, Level level) {
		if (!LOG.isLoggable(level)) {
			return;
		}
		LOG.log(level, COLUMN_STRING);
		LOG.log(level, DOUBLE_LINE);
		for (int i = 0, len = currentColumn.size(); i < len; i++) {
			Set<Mask> row = currentColumn.get(i);
			if (row.isEmpty()) {
				continue;
			}
	
			for (Mask mask : row) {
				LOG.log(level, mask.toString());
			}
			LOG.log(level, i == len - 1 ? EMPTY : SINGLE_LINE);
		}
	}

	/**
	 * Pomoćna metoda koja ekstrahira primarne implikante i sprema ih u
	 * <b>primaryImplicants</b> {@link Set}
	 *
	 * @param column
	 *            {@link List} {@link Set}ova primjeraka razreda {@link Mask}
	 *            koji predstavlja trenutni stupac koji obrađujemo unutar
	 *            minimizacije
	 * @param primaryImplicants
	 *            {@link Set} svih primarnih implikanata, modeliranih razredom
	 *            {@link Mask}
	 */
	private void extractPossiblePrimaryImplicants(List<Set<Mask>> column, Set<Mask> primaryImplicants) {
		boolean modified = false;
		for (Set<Mask> row : column) {
			for (Mask mask : row) {
				if (!mask.isCombined() && !mask.isDontCare()) {
					modified = true;
					primaryImplicants.add(mask);
					LOG.log(Level.FINEST, () -> PRIMARY_IMPLICANT_FOUND + mask.toString());
				}
			}
		}
		// prazan zapis zbog formata ispisa
		if (modified) {
			LOG.log(Level.FINEST, EMPTY);
		}
	}

	/**
	 * Pomoćna metoda koja preko konstante {@link #LOG} vrši loggiranje
	 * <b>implicants</b> ukoliko je omogućeno loggiranje razine <b>level</b>
	 *
	 * @param implicants
	 *            {@link Set} implikanta modeliranih razredom {@link Mask} koji
	 *            se loggira
	 * @param level
	 *            razina loggiranja
	 */
	private void logImplicants(Set<Mask> implicants, String message, Level level) {
		if (!LOG.isLoggable(level)) {
			return;
		}
	
		LOG.log(level, EMPTY);
		LOG.log(level, message);
		implicants.forEach(primImpl -> LOG.log(level, primImpl.toString()));
	}

	/**
	 * Metoda koja pronalazi minimalnu pokrivenost Pyne-McCluskey pristupom.
	 * Metoda prima izlaz metode {@link #findPrimaryImplicants()} te iz njega
	 * gradi (ukoliko već svi mintermi nisu pokriveni) p-funkciju. Zatim iz
	 * zadane p-funkcije preko metode {@link #findMinimalSet(List)} vraća
	 * {@link List} {@link Set}ova primjeraka razreda {@link BitSet} koji
	 * predstavlja pozicije minterma unutar p-funkcije koji s najmanjim
	 * kardinalitetom pokrivaju sve minterme. Potom se čitava lista pretvara u
	 * {@link List} {@link Set}ova primjeraka razreda {@link Mask} s kojim lakše
	 * radimo kada su sve operacije gotove
	 *
	 * @param primCover
	 *            predstavlja {@link Set} primjeraka razreda {@link Mask} koji
	 *            predstavljaju primarne implikante minimizacije
	 * @return {@link List} {@link Set}ova primjeraka razreda {@link Mask}
	 *         unutar koje svaki {@link Set} predstavlja jednu minimalnu formu
	 *         funkcije koja se minimizira
	 */
	private List<Set<Mask>> chooseMinimalCover(Set<Mask> primCover) {
		// Izgradi polja implikanata i minterma (rub tablice):
		Mask[] implicants = primCover.toArray(new Mask[primCover.size()]);
		Integer[] minterms = mintermSet.toArray(new Integer[mintermSet.size()]);

		// Mapiraj minterm u stupac u kojem se nalazi:
		Map<Integer, Integer> mintermToColumnMap = new HashMap<>();
		for (int i = 0; i < minterms.length; i++) {
			Integer index = minterms[i];
			mintermToColumnMap.put(index, i);
		}

		// Napravi praznu tablicu pokrivenosti:
		boolean[][] table = buildCoverTable(implicants, minterms, mintermToColumnMap);

		// Donji redak tablice: koje sam minterme pokrio?
		boolean[] coveredMinterms = new boolean[minterms.length];

		// Pronađi primarne implikante...
		Set<Mask> importantSet = selectImportantPrimaryImplicants(implicants, mintermToColumnMap, table,
				coveredMinterms);
		logImplicants(importantSet, "Bitni primarni implikanti su:", Level.FINE);
		// ako su pokriveni svi mintermi nemoj dalje ispitivati. Ostale metode
		// su skupe
		if (isAllCovered(importantSet)) {
			List<Set<Mask>> minimalForms = new ArrayList<>();
			minimalForms.add(importantSet);
			return minimalForms;
		}

		// Izgradi funkciju pokrivenosti:
		List<Set<BitSet>> pFunction = buildPFunction(table, coveredMinterms);
		logObject(pFunction, "p funkcija je:", Level.FINER);

		// Pronađi minimalne dopune:
		Set<BitSet> minset = findMinimalSet(pFunction);

		// Izgradi minimalne zapise funkcije:
		List<Set<Mask>> minimalForms = new ArrayList<>();
		for (BitSet bs : minset) {
			Set<Mask> set = new LinkedHashSet<>(importantSet);
			bs.stream().forEach(i -> set.add(implicants[i]));
			minimalForms.add(set);
		}
		logMinimalForms(minimalForms, Level.FINE);
		return minimalForms;
	}

	/**
	 * Pomoćna metoda moja gradi tablicu pokrivenosti iz polja <b>implicants</b>
	 * i <b>minterms</b> koristeći {@link Map} indeksa minterma u polju
	 * <b>minterms</b>
	 *
	 * @param implicants
	 *            predstavlja polje primjeraka razreda {@link Mask} koji
	 *            predstavljaju primarne implikante. Ujednio se koristi za
	 *            izgradnju redaka tablice pokrivenosti
	 * @param minterms
	 *            predstavlja polje primjeraka razreda {@link Integer} koji
	 *            predstavljaju minterme. Ujedino koristi se za izgradnju
	 *            stupaca tablice pokrivenosti
	 * @param mintermToColumnMap
	 *            {@link Map}a koja mapira minterm u stupac u kojem se nalazi
	 * 
	 * @return izgrađenu tablicu pokrivenosti
	 */
	private boolean[][] buildCoverTable(Mask[] implicants, Integer[] minterms,
			Map<Integer, Integer> mintermToColumnMap) {
		boolean[][] coverTable = new boolean[implicants.length][minterms.length];
		for (int i = 0; i < implicants.length; i++) {
			Set<Integer> indexes = implicants[i].getIndexes();
			for (Integer minterm : minterms) {
				coverTable[i][mintermToColumnMap.get(minterm)] = indexes.contains(minterm);
			}
		}
	
		return coverTable;
	}

	/**
	 * Pomoćna metoda koja se koristi za pronalazak bitnih primarnih implikanta
	 * predane booleove funkcije.
	 *
	 * @param implicants
	 *            polje primjeraka razreda {@link Mask} koji predstavljaju
	 *            primarne implikante
	 * @param mintermToColumnMap
	 *            {@link Map}a koja mapira minterm u stupac u kojem se nalazi
	 * @param table
	 *            tablicu pokrivenosti
	 * @param coveredMinterms
	 *            pomoćna tablica koja sadrži minterme koji su pokriveni.
	 * @return {@link Set} primjeraka razreda {@link Mask} koji predstavljaju
	 *         bitne primarne implikant
	 */
	private Set<Mask> selectImportantPrimaryImplicants(Mask[] implicants, Map<Integer, Integer> mintermToColumnMap,
			boolean[][] table, boolean[] coveredMinterms) {
		Set<Mask> importantPrimaryImplicants = new LinkedHashSet<>();
		for (int j = 0, len = mintermToColumnMap.size(); j < len; j++) {
			boolean onlyOneCovers = false;
			Mask primaryImplicant = null;
			for (int i = 0; i < implicants.length; i++) {
				if (table[i][j]) {
					if (onlyOneCovers) {
						onlyOneCovers = false;
						break;
					}
	
					onlyOneCovers = true;
					primaryImplicant = implicants[i];
				}
			}
	
			if (onlyOneCovers) {
				importantPrimaryImplicants.add(primaryImplicant);
				coverMinterms(primaryImplicant, mintermToColumnMap, coveredMinterms);
			}
		}
	
		return importantPrimaryImplicants;
	}

	/**
	 * Pomoćna metoda koja se koristi za oznađavanje svih mintermi koji se
	 * dobiju pozivom metode {@link Mask#getIndexes()} unutar tablice
	 * <b>coveredMinterms</b>. Za traženje indeksa svakog od minterma koristi se
	 * predana {@link Map}a <b>mintermToColumnMap</b>.
	 *
	 * @param primaryImplicant
	 *            primjerak razreda {@link Mask} koji predstavlja jedan bitni
	 *            primarni implikant
	 * @param mintermToColumnMap
	 *            {@link Map}a koja mapira minterm u stupac u kojem se nalazi
	 * @param coveredMinterms
	 *            pomoćna tablica koja sadrži minterme koji su pokriveni.
	 */
	private void coverMinterms(Mask primaryImplicant, Map<Integer, Integer> mintermToColumnMap,
			boolean[] coveredMinterms) {
		for (Integer minterm : primaryImplicant.getIndexes()) {
			Integer index = mintermToColumnMap.get(minterm);
			if (index != null) {
				coveredMinterms[index] = true;
			}
		}
	}

	/**
	 * Pomoćna metoda koja provjerava jesu li već svi mintermi pokriveni, nakon
	 * što smo pronašli bitne primarne implikante, koji su pohranjeni unutar
	 * parametra <b>importantSet</b>
	 *
	 * @param importantSet
	 *            bitni primarni implikanti pohranjeni unutar {@link Set}a
	 *            primjeraka razreda {@link Mask}
	 * @return <code>true</code> ukoliko bitni primarni implikanti pokrivaju sve
	 *         minterme, <code>false</code> inače
	 */
	private boolean isAllCovered(Set<Mask> importantSet) {
		Set<Integer> allCovered = new LinkedHashSet<>();
		for (Mask mask : importantSet) {
			allCovered.addAll(mask.getIndexes());
		}
		return allCovered.containsAll(mintermSet);
	}

	/**
	 * Pomoćna metoda koja gradi p funkciju koja se koristi prilikom
	 * minimizacije metodom Quine-McCluskey s <b>Pyne-McCluskey pristupom.</b>
	 * Ova funkcija predstavlja implikante koje još moramo uzeti kako bi
	 * booleava funkcija bila minimizirana.
	 *
	 * @param table
	 *            tablica pokrivenosti
	 * @param coveredMinterms
	 *            pomoćna tablica koja sadrži minterme koji su pokriveni.
	 * @return {@link List} {@link Set}ova primjeraka razreda {@link BitSet}
	 *         koji se koriste prilikom izgradnje p-funkcije, radi lakšeg
	 *         provođenja operacija nad njima
	 */
	private List<Set<BitSet>> buildPFunction(boolean[][] table, boolean[] coveredMinterms) {
		List<Set<BitSet>> pFunction = new ArrayList<>();
		for (int j = 0, width = mintermSet.size(); j < width; j++) {
			if (coveredMinterms[j]) {
				continue;
			}
	
			Set<BitSet> bracket = new LinkedHashSet<>();
			pFunction.add(bracket);
			for (int i = 0; i < table.length; i++) {
				if (table[i][j]) {
					BitSet bitset = new BitSet(table.length);
					bitset.set(i);
					bracket.add(bitset);
				}
			}
		}
		return pFunction;
	}

	/**
	 * Pomoćna metoda koja preko konstante {@link #LOG} vrši loggiranje
	 * <b>obj</b> ukoliko je omogućeno loggiranje razine <b>level</b>. Prije
	 * loggiranja metoda u zasebni redak loggira <b>message</b>
	 *
	 * @param obj
	 *            {@link List} {@link Set}ova primjeraka razreda {@link Mask}
	 *            koji predstavljaju minimalne forme minimizacije predane
	 *            booleove funkcije
	 * @param message
	 *            poruka koja se treba ispisati prije loggiranja <b>obj</b>
	 * @param level
	 *            razina loggiranja
	 * 
	 */
	private void logObject(Object obj, String message, Level level) {
		if (!LOG.isLoggable(level)) {
			return;
		}
	
		LOG.log(level, EMPTY);
		// LOG.log(level, "p funkcija je:");
		LOG.log(level, message);
		LOG.log(level, obj.toString());
	}

	/**
	 * Pomoćna metoda koja iz predane p-funkcije ,koja je produkt suma primarnih
	 * implikanata, stvara p-funkciju koja je suma produkata primarnih
	 * imlikanata. Potom traži takve primjerke razreda {@link BitSet} za koje je
	 * količina implikanata u produktu minimalna, preko metode
	 * {@link BitSet#cardinality()}.
	 *
	 * @param pFunction
	 *            p-funkcija koja predstavlja produkt suma primarnih implikanata
	 * @return p-funkcija koja predstavlja sumu produkata primarnih implikanata
	 *         s minimalnim kardinalitetom
	 */
	private Set<BitSet> findMinimalSet(List<Set<BitSet>> pFunction) {
		Set<BitSet> minimalSet = new HashSet<>(pFunction.get(0));
		Set<BitSet> copy = new HashSet<>();
		for (int i = 1, len = pFunction.size(); i < len; i++) {
			copy.addAll(minimalSet);
			minimalSet.clear();
			for (BitSet minimalSoFar : copy) {
				for (BitSet multiplier : pFunction.get(i)) {
					BitSet minimalClone = (BitSet) minimalSoFar.clone();
					minimalClone.or(multiplier);
					minimalSet.add(minimalClone);
				}
			}
			copy.clear();
		}
		logObject(minimalSet, "Nakon prevorbe p-funkcije u sumu produkata:", Level.FINER);
		return calculateLowestCardinalitySets(minimalSet);
	}

	/**
	 * Pomoćna metoda koju poziva {@link #findMinimalSet(List)} kako bi
	 * ekstrahirala one produkte primarnih implikanata koje imaju najmanji
	 * kardinalitet
	 *
	 * @param minimalSet
	 *            p-funkcija koja predstavlja sumu produkata primarnih
	 *            implikanata
	 * @return {@link Set} onih produkata za koje je broj primarnih implikanata
	 *         minimalan
	 */
	private Set<BitSet> calculateLowestCardinalitySets(Set<BitSet> minimalSet) {
		int minimumCardinality = minimalSet.stream().mapToInt(bitSet -> bitSet.cardinality()).min().getAsInt();
		Set<BitSet> lowestCardinalitySets = minimalSet.stream()
				.filter(bitSet -> bitSet.cardinality() == minimumCardinality)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	
		logObject(lowestCardinalitySets, "Minimalna pokrivanja još trebaju:", Level.FINER);
	
		return lowestCardinalitySets;
	}

	/**
	 * Pomoćna metoda koja preko konstante {@link #LOG} vrši loggiranje
	 * <b>minimalForms</b> ukoliko je omogućeno loggiranje razine <b>level</b>
	 *
	 * @param minimalForms
	 *            {@link List} {@link Set}ova primjeraka razreda {@link Mask}
	 *            koji predstavljaju minimalne forme minimizacije predane
	 *            booleove funkcije
	 * @param level
	 *            razina loggiranja
	 */
	private void logMinimalForms(List<Set<Mask>> minimalForms, Level level) {
		if (!LOG.isLoggable(level)) {
			return;
		}

		LOG.log(level, EMPTY);
		LOG.log(level, "Minimalni oblici funkcije su:");
		for (int i = 0, len = minimalForms.size(); i < len; i++) {
			LOG.log(level, String.format("%d. %s", (i + 1), minimalForms.get(i)));
		}
	}

}
