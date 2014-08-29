package nyx.collections.test;

import java.util.Arrays;

import nyx.collections.storage.ElasticStorage;

import org.junit.*;

import static org.junit.Assert.*;

public class ElasticStorageTest {

	@Test
	public void testAppendSmall() {
		ElasticStorage<Integer> es = new ElasticStorage<>();
		byte[] addme = new byte[1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.create(i, addme);
		}
		es.clear();
	}

	@Test
	public void testAppendBig() {
		ElasticStorage<Integer> es = new ElasticStorage<>();
		byte[] addme = new byte[10 * 1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.create(i, addme);
		}
		es.clear();
	}
	
	@Test
	public void testRetrieveSmall() {
		ElasticStorage<Integer> es = new ElasticStorage<>();
		byte[] addme = new byte[1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.create(i, addme);
		}
		// compare arrays
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			byte[] fromEs = es.read(i);
			assertArrayEquals(addme, fromEs);
		}
		es.clear();
	}
	
	@Test
	public void testRetrieveBig() {
		ElasticStorage<Integer> es = new ElasticStorage<>();
		byte[] addme = new byte[10*1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.create(i, addme);
		}
		// compare arrays
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			byte[] fromEs = es.read(i);
			assertArrayEquals(addme, fromEs);
		}
		es.clear();
	}

}
