package hr.fer.zemris.bf.qmc;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.junit.Test;

import hr.fer.zemris.bf.utils.Util;

public class MaskTest {

	@Test(expected = IllegalArgumentException.class)
	public void predanKriviBrojVarijabli1Konstruktor() {
		new Mask(0, -1, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void predanKriviIndeks1Konstruktor() {
		new Mask(-1, 5, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void predanaMaskaNull2Konstruktor() {
		new Mask(null, new TreeSet<>(Arrays.asList(2, 7, 8, 9)), false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void predanaSkupNull2Konstruktor() {
		new Mask(Util.indexToByteArray(5, 4), null, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void predanaPrazanSkup2Konstruktor() {
		new Mask(Util.indexToByteArray(5, 4), Collections.emptySet(), true);
	}

	@Test
	public void provjeraBrojaJedinica1() {
		assertEquals(new Mask(5, 4, true).countOfOnes(), 2);
	}

	@Test
	public void provjeraBrojaJedinica2() {
		assertEquals(new Mask(15, 4, true).countOfOnes(), 4);
	}

	@Test
	public void provjeraBrojaJedinica3() {
		assertEquals(new Mask(127, 8, true).countOfOnes(), 7);
	}

	@Test
	public void kombiniranjeDvijeMaskeIspravno1() {
		Mask mask1 = new Mask(13, 4, false);
		Mask mask2 = new Mask(12, 4, false);
		Mask result = mask1.combineWith(mask2).get();
		assertEquals(result, new Mask(new byte[] { 1, 1, 0, 2 }, new TreeSet<>(Arrays.asList(13, 12)), false));
	}

	@Test
	public void kombiniranjeDvijeMaskeIspravno2() {
		Mask mask1 = new Mask(1, 2, false);
		Mask mask2 = new Mask(3, 2, false);
		Mask result = mask1.combineWith(mask2).get();
		assertEquals(result, new Mask(new byte[] { 2, 1 }, new TreeSet<>(Arrays.asList(1, 3)), false));
	}

	@Test
	public void kombiniranjeDvijeMaskeIspravno3() {
		Mask mask1 = new Mask(new byte[] { 2, 2, 0, 1 }, new TreeSet<>(Arrays.asList(1, 5, 9, 13)), true);
		Mask mask2 = new Mask(new byte[] { 2, 2, 0, 0 }, new TreeSet<>(Arrays.asList(0, 4, 8, 12)), true);
		Mask result = mask1.combineWith(mask2).get();
		assertEquals(result,
				new Mask(new byte[] { 2, 2, 0, 2 }, new TreeSet<>(Arrays.asList(1, 5, 9, 13, 0, 4, 8, 12)), true));
		assertTrue(result.isDontCare());
	}

	@Test(expected = NoSuchElementException.class)
	public void kombiniranjeDvijeMaskeNeispravno1() {
		Mask mask1 = new Mask(13, 4, false);
		Mask mask2 = new Mask(14, 4, false);
		mask1.combineWith(mask2).get();
	}

	@Test(expected = NoSuchElementException.class)
	public void kombiniranjeDvijeMaskeNeispravno2() {
		Mask mask1 = new Mask(0, 2, false);
		Mask mask2 = new Mask(3, 2, false);
		mask1.combineWith(mask2).get();
	}

	@Test(expected = NoSuchElementException.class)
	public void kombiniranjeDvijeMaskeNeispravno3() {
		Mask mask1 = new Mask(new byte[] { 2, 2, 0, 2 }, new TreeSet<>(Arrays.asList(0, 1, 4, 5, 8, 9, 12, 13)), true);
		Mask mask2 = new Mask(new byte[] { 2, 2, 0, 0 }, new TreeSet<>(Arrays.asList(0, 4, 8, 12)), true);
		mask1.combineWith(mask2).get();
	}
}
