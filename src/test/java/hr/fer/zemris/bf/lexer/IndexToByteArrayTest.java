package hr.fer.zemris.bf.lexer;

import static org.junit.Assert.*;

import java.util.StringJoiner;

import static hr.fer.zemris.bf.utils.Util.indexToByteArray;

import org.junit.Test;

public class IndexToByteArrayTest {

	@Test
	public void predanaNulaZaVelicinu() {
		byte[] values = indexToByteArray(25, 0);
		assertArrayEquals(values, new byte[0]);
	}

	@Test
	public void primjerIzZadataka1() {
		byte[] values = indexToByteArray(3, 2);
		ispisi(values);
		assertArrayEquals(values, new byte[] { 1, 1 });
	}

	@Test
	public void primjerIzZadataka2() {
		byte[] values = indexToByteArray(3, 4);
		ispisi(values);
		assertArrayEquals(values, new byte[] { 0, 0, 1, 1 });
	}

	@Test
	public void primjerIzZadataka3() {
		byte[] values = indexToByteArray(3, 6);
		ispisi(values);
		assertArrayEquals(values, new byte[] { 0, 0, 0, 0, 1, 1 });
	}

	@Test
	public void primjerIzZadataka4() {
		byte[] values = indexToByteArray(-2, 32);
		byte[] test = new byte[32];
		for (int i = 0; i < test.length-1; i++) {
			test[i] = 1;
		}
		ispisi(values);
		assertArrayEquals(values, test);
	}
	
	@Test
	public void primjerIzZadataka5() {
		byte[] values = indexToByteArray(-2, 16);
		byte[] test = new byte[16];
		for (int i = 0; i < test.length-1; i++) {
			test[i] = 1;
		}
		ispisi(values);
		assertArrayEquals(values, test);
	}
	
	
	@Test
	public void primjerIzZadataka6() {
		byte[] values = indexToByteArray(19, 4);
		ispisi(values);
		assertArrayEquals(values, new byte[] {0,0,1,1});
	}
	
	@Test
	public void predanaPrevelikaVelicina() {
		byte[] values= indexToByteArray(-2, 65);
		byte[] test = new byte[65];
		for (int i = 0; i < test.length-1; i++) {
			test[i] = 1;
		}
		ispisi(values);
		assertArrayEquals(values, test);
	}
	
	private void ispisi(byte[] values) {
		StringJoiner sj = new StringJoiner(", ", "[", "]");
		for (int i = 0; i < values.length; i++) {
			sj.add(Byte.valueOf(values[i]).toString());
		}
		System.out.println(sj.toString());
	}
}
