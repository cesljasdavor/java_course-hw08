package hr.fer.zemris.bf.qmc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import hr.fer.zemris.bf.model.ConstantNode;
import hr.fer.zemris.bf.model.Node;
import hr.fer.zemris.bf.model.UnaryOperatorNode;
import hr.fer.zemris.bf.model.VariableNode;
import hr.fer.zemris.bf.utils.Util;

/**
 * Razred čiji primjerci predstavljaju specifikaciju (moguće nepotpunih)
 * produkata.Primjerice, neka radimo s Booleovom funkcijom definiranom nad
 * varijablama A, B, C, D (tim poretkom). Produkt ABC'D (gdje C' označava
 * komplement varijable C) kraće ćemo zapisati maskom 1101, produkt A'B'CD
 * maskom 0011 i slično. Komplementirane varijable u maski su reprezentirane
 * znamenkom {@value #NEGATIVE}, nekomplementirane znamenkom {@value #POSITIVE}.
 * Produkt AC'D ćemo zapisati maskom 1201, gdje znamenka
 * {@value #NOT_IN_PRODUCT} označava da se varijabla na čijoj je poziciji
 * znamenka {@value #NOT_IN_PRODUCT} ne pojavljuje u produktu. Unutar ovog
 * razreda moguće je mijenjati isključivo je li primjerak ovog razreda
 * kombiniran s nekim drugim primjerkom ovog razreda. Razred nudi 2
 * konstruktora:
 * <ul>
 * <li>{@link #Mask(byte[], Set, boolean)}</li>
 * <li>{@link #Mask(int, int, boolean)}</li>
 * </ul>
 * 
 * Razred također nudi metode:
 * <ul>
 * <li>{@link #getIndexes()}</li>
 * <li>{@link #isDontCare()}</li>
 * <li>{@link #isCombined()}</li>
 * <li>{@link #setCombined(boolean)}</li>
 * <li>{@link #countOfOnes()}</li>
 * <li>{@link #combineWith(Mask)}</li>
 * <li>{@link #toVariableNodes(List)}</li>
 * </ul>
 * 
 * @author Davor Češljaš
 */
public class Mask {

	/**
	 * Konstanta koja predstavlja niz znakova "NOT"
	 */
	private static final String NOT = "NOT";

	/**
	 * Konstanta koja predstavlja primjerak razreda koji implementira sučelje
	 * {@link UnaryOperator}. Ovaj primjerak razreda nad primjerkom razreda
	 * {@link Boolean} vrši logičku negaciju
	 */
	private static final UnaryOperator<Boolean> NOT_OPERATOR = v -> !v;

	/**
	 * Konstanta koja unutar {@link #values} predstavlja da je varijabla na toj
	 * poziciji negirana
	 */
	private static final int NEGATIVE = 0;

	/**
	 * Konstanta koja unutar {@link #values} predstavlja da varijabla na toj
	 * poziciji nije negirana
	 */
	private static final int POSITIVE = 1;

	/**
	 * Konstanta koja unutar {@link #values} predstavlja da varijabla na toj
	 * poziciji nije sadržana u produktu
	 */
	private static final int NOT_IN_PRODUCT = 2;

	/**
	 * Polje zastavica koje ovisno o vrijednosti {@value #NEGATIVE},
	 * {@link #POSITIVE} ili {@value #NOT_IN_PRODUCT} predstavljaju ponašanje
	 * varijable na toj poziciji( postoji li, je li negirana ili nije)
	 */
	private byte[] values;

	/**
	 * Skup primjeraka razreda {@link Integer} koji predstavlja indekse minterma
	 * koji primjerak ovog razreda predstavlja
	 */
	private Set<Integer> indexes;

	/** Booleova zastavica koja predstavlja je li minterm don't care */
	private boolean dontCare;

	/** Hash-vrijednost primjerka ovog razreda */
	private int hashCode;

	/**
	 * Booleova zastavica koja predstavlja je li primjerak ovog razreda
	 * kombiniran s nekim drugim primjerkom ovog razreda
	 */
	private boolean combined;

	/**
	 * Predstavlja broj vrijednosti {@value #POSITIVE} unutar {@link #values}
	 */
	private int countOfOnes;

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Konstruktor iz
	 * parametra <b>index</b> dobiva informaciju koji minterm će primjerak ovog
	 * razreda predstavljati. Također iz <b>index</b>a dobiva kombinaciju
	 * varijabli koje ovaj razred predstavlja, a koja se interno pohranjuje
	 *
	 * @param index
	 *            parametar koji predstavlja jedan minterm koji će predstavljati
	 *            primjerak ovog razreda
	 * @param numberOfVariables
	 *            parametar koji predstavlja broj varijabli unutar neke funkcije
	 *            čiji je ovaj minterm sastavni dio
	 * @param dontCare
	 *            zastavica koja predstavlja je li ovaj primjerak razreda don't
	 *            care
	 * @throws IllegalArgumentException
	 *             ukoliko je parametar <b>numberOfVariables</b> manji od 1 ili
	 *             se preda <b>index</b> koji je van raspona [0,
	 *             2^<b>numberOfVariables</b>]
	 */
	public Mask(int index, int numberOfVariables, boolean dontCare) {
		if (numberOfVariables < 1) {
			throw new IllegalArgumentException("Broj varijabli mora biti barem 1. Ja sam dobio: " + numberOfVariables);
		}
		if (index < 0 || index > Math.pow(2, numberOfVariables) - 1) {
			throw new IllegalArgumentException("Predali ste indeks van raspona!");
		}
		constructMask(Util.indexToByteArray(index, numberOfVariables), new TreeSet<>(Arrays.asList(index)), dontCare);
	}

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Ovaj konstruktor
	 * već prima kombinaciju varijabli koje će predstavljati kroz parametre
	 * <b>values</b> i <b>indexes</b> te ih samo interno pohranjuje ukoliko
	 * odgovaraju kriterijima.
	 *
	 * @param values
	 *            kombinacija varijabli koje ovaj će primjerak ovog razreda
	 *            predstavljati
	 * @param indexes
	 *            svi mintermi koje će primjerak ovog razreda predstavljati
	 * @param dontCare
	 *            zastavica koja predstavlja je li ovaj primjerak razreda don't
	 *            care
	 * @throws IllegalArgumentException
	 *             ukoliko je maska null ili je skup indeksa null ili prazan.
	 */
	public Mask(byte[] values, Set<Integer> indexes, boolean dontCare) {
		if (values == null) {
			throw new IllegalArgumentException("Maska nesmije biti null");
		}
		if (indexes == null || indexes.isEmpty()) {
			throw new IllegalArgumentException("Skup indeksa ne smije biti null ili prazan.");
		}
		constructMask(Arrays.copyOf(values, values.length), new TreeSet<>(indexes), dontCare);
	}

	/**
	 * Pomoćna metoda koja objedinjuje konstruktore
	 * {@link #Mask(byte[], Set, boolean)} i {@link #Mask(int, int, boolean)} te
	 * pohranjuje vrijednosti. Metoda se koristi isključivo kako u
	 * konstruktorima ne bi bilo ponavljanja koda
	 *
	 * @param values
	 *            kombinacija varijabli koje ovaj će primjerak ovog razreda
	 *            predstavljati
	 * @param indexes
	 *            svi mintermi koje će primjerak ovog razreda predstavljati
	 * @param dontCare
	 *            zastavica koja predstavlja je li ovaj primjerak razreda don't
	 *            care
	 */
	private void constructMask(byte[] values, Set<Integer> indexes, boolean dontCare) {
		this.values = values;
		this.hashCode = Arrays.hashCode(values);
		calculateCountOfOnes();
		this.indexes = Collections.unmodifiableSet(indexes);
		this.dontCare = dontCare;
	}

	/**
	 * Pomoćna metoda koja računa broj vrijednosti {@value #POSITIVE} unutar
	 * {@link #values} te taj broj pohranjuje u {@link #countOfOnes}.
	 */
	private void calculateCountOfOnes() {
		for (int i = 0; i < values.length; i++) {
			countOfOnes += values[i] == POSITIVE ? POSITIVE : NEGATIVE;
		}
	}

	/**
	 * Metoda koja dohvaća sve indekse minterma koje primjerak ovog razreda
	 * predstavlja.
	 *
	 * @return sve indekse minterma koje primjerak ovog razreda predstavlja.
	 */
	public Set<Integer> getIndexes() {
		return indexes;
	}

	/**
	 * Metoda koja provjerava ima li primjerak ovog razreda podignutu zastavicu
	 * don't care
	 *
	 * @return <code>true</code> ukoliko je don't care zastavica ovog razreda
	 *         podignuta, <code>false</code> inače
	 */
	public boolean isDontCare() {
		return dontCare;
	}

	/**
	 * Metoda koja provjerava je li primjerak ovog razreda već kombiniran s
	 * nekim drugim primjerkom ovog razreda
	 *
	 * @return <code>true</code> ako i samo ako je primjerak ovog razreda već
	 *         kombiniran s nekim drugim primjerkom ovog razreda,
	 *         <code>false</code> inače
	 */
	public boolean isCombined() {
		return combined;
	}

	/**
	 * Metoda koja postavlja zastavicu da je ovaj razred kombiniran na
	 * vrijednost <b>combined</b>
	 *
	 * @param combined
	 *            nova vrijednost zastavice <b>combined</b>
	 */
	public void setCombined(boolean combined) {
		this.combined = combined;
	}

	/**
	 * Metoda koja dohvaća koliko vrijednosti {@link #POSITIVE} je sadržano u
	 * primjerku ovog razreda (konkretno u maski koja je spremljena).
	 *
	 * @return koliko vrijednosti {@link #POSITIVE} je sadržano u primjerku ovog
	 *         razreda (konkretno u maski koja je spremljena).
	 */
	public int countOfOnes() {
		return countOfOnes;
	}

	/**
	 * Metoda koja pokušava kombinirati dva primjerka ovog razreda u novi
	 * primjerak ovog razreda. Metoda pri tome ne modificira trenutni primjerak
	 * razreda niti primjerak razreda preda kao parametar <b>other</b>. Ukoliko
	 * je kombiniranje uspjelo metoda vraća primjerak razreda {@link Optional}
	 * koji enkapsulira rezultantni primjerak razreda {@link Mask}. U suprotnom
	 * vraća {@link Optional#empty()}.
	 *
	 * @param other
	 *            primjerak razreda {@link Mask} s kojim se ovaj primjerak
	 *            kombinira
	 * @return Ukoliko je kombiniranje uspjelo metoda vraća primjerak razreda
	 *         {@link Optional} koji enkapsulira rezultantni primjerak razreda
	 *         {@link Mask}. U suprotnom vraća {@link Optional#empty()}.
	 * @throws IllegalArgumentException
	 *             ukoliko je kao parametar <b>other</b> predan
	 *             <code>null</code>
	 */
	public Optional<Mask> combineWith(Mask other) {
		if (other == null) {
			throw new IllegalArgumentException("Metoda ne može raditi sa argumentom null!");
		}
		if (this.values.length != other.values.length) {
			throw new IllegalArgumentException(
					String.format("Predali ste masku različite širine! Traženo je: %d, a dobiveno je: %d",
							this.values.length, other.values.length));
		}
		if (Math.abs(this.countOfOnes - other.countOfOnes) != 1) {
			return Optional.empty();
		}
		return combine(other);
	}

	/**
	 * Pomoćna metoda koja nakon provjera argumenata unutar metode
	 * {@link #combineWith(Mask)} vrši izgradnju novog primjerka razreda
	 * {@link Mask} koji je neovisan od trenutnom primjerku i primjerku predanom
	 * kao <b>other</b> ukoliko je to semantički moguće (ukoliko se ova dva
	 * razreda mogu kombinirati prema teoremu simplifikacije <a href =
	 * "https://www.electrical4u.com/boolean-algebra-theorems-and-laws-of-boolean-algebra/">teoremu
	 * simplifikacije</a>)
	 *
	 * @param other
	 *            primjerak razreda {@link Mask} s kojim se ovaj primjerak
	 *            kombinira
	 * @return Ukoliko je kombiniranje uspjelo metoda vraća primjerak razreda
	 *         {@link Optional} koji enkapsulira rezultantni primjerak razreda
	 *         {@link Mask}. U suprotnom vraća {@link Optional#empty()}.
	 */
	private Optional<Mask> combine(Mask other) {
		boolean difference = false;
		byte[] newValues = new byte[this.values.length];
		for (int i = 0; i < this.values.length; i++) {
			if (this.values[i] != other.values[i]) {
				if (this.values[i] == NOT_IN_PRODUCT || other.values[i] == NOT_IN_PRODUCT || difference) {
					return Optional.empty();
				}
				difference = true;
				newValues[i] = NOT_IN_PRODUCT;
			} else {
				newValues[i] = this.values[i];
			}
		}
		Set<Integer> newIndexes = new TreeSet<>(this.indexes);
		newIndexes.addAll(other.indexes);
		return Optional.of(new Mask(newValues, newIndexes, this.dontCare && other.dontCare));
	}

	/**
	 * Metoda koja masku koju predstavlja ovaj razred parsira u {@link List}
	 * primjeraka razreda {@link Node}. O tome koji će se nazivi varijabli
	 * koristiti odlučuje se korisnik ove metode. Nazivi pojedinih varijabli
	 * predaju se kroz {@link List} primjeraka razreda {@link String}
	 * <b>variables</b>
	 *
	 * @param variables
	 *            Nazivi pojedinih varijabli
	 * @return {@link List} primjeraka razreda {@link Node} koji predstavljaju
	 *         logičke izraze koje predstavlja ovaj maska
	 * @throws IllegalArgumentException
	 *             ukoliko je duljina maske različita broju predanih varijabli
	 * 
	 * @see Node
	 *
	 */
	public List<Node> toVariableNodes(List<String> variables) {
		if (values.length != variables.size()) {
			throw new IllegalArgumentException("Rekonstrukcija nije moguća: Predali ste previše varijabli!");
		}
		List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < values.length; i++) {
			byte value = values[i];
			if (value == NEGATIVE) {
				nodes.add(new UnaryOperatorNode(NOT, new VariableNode(variables.get(i)), NOT_OPERATOR));
			} else if (value == POSITIVE) {
				nodes.add(new VariableNode(variables.get(i)));
			}
		}
		//tautologija(svi su 2)
		if(nodes.isEmpty()) {
			nodes.add(new ConstantNode(true));
		}
		return nodes;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Mask)) {
			return false;
		}
		Mask other = (Mask) obj;
		if (this.hashCode != obj.hashCode()) {
			return false;
		}
		return Arrays.equals(this.values, other.values);
	}

	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(" ");
		StringBuilder joinedValues = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			joinedValues.append(values[i] == NOT_IN_PRODUCT ? "-" : values[i]);
		}
		sj.add(joinedValues.toString());
		sj.add(dontCare ? "D" : ".");
		sj.add(combined ? "*" : " ");
		sj.add(indexes.stream().map(value -> value.toString()).collect(Collectors.joining(", ", "[", "]")));
		return sj.toString();
	}
}
