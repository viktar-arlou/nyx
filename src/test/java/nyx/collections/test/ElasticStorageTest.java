package nyx.collections.test;

import java.util.Arrays;

import nyx.collections.storage.ElasticByteStorage;

import org.junit.*;

import static org.junit.Assert.*;

public class ElasticStorageTest {

	@Test
	public void testAppendSmall() {
		ElasticByteStorage<Integer> es = new ElasticByteStorage<>();
		byte[] addme = new byte[1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.put(i, addme);
		}
		es.clear();
	}

	@Test
	public void testAppendBig() {
		ElasticByteStorage<Integer> es = new ElasticByteStorage<>();
		byte[] addme = new byte[100 * 1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.put(i, addme);
		}
		es.clear();
	}

	@Test
	public void testRetrieveSmall() {
		ElasticByteStorage<Integer> es = new ElasticByteStorage<>();
		byte[] addme = new byte[1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.put(i, addme);
		}
		// compare arrays
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			byte[] fromEs = es.get(i);
			assertArrayEquals(addme, fromEs);
		}
		es.clear();
	}

	@Test
	public void testRetrieveBig() {
		ElasticByteStorage<Integer> es = new ElasticByteStorage<>();
		byte[] addme = new byte[100 * 1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.put(i, addme);
		}
		// compare arrays
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			byte[] fromEs = es.get(i);
			assertArrayEquals(addme, fromEs);
		}
		es.clear();
	}

	@Test
	public void testSerialization() throws Exception {
		ElasticByteStorage<Integer> es = new ElasticByteStorage<>();
		byte[] addme = new byte[100 * 1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.put(i, addme);
		}
		ElasticByteStorage<Integer> es2 = (ElasticByteStorage<Integer>) NyxListTest
				.deserialize(NyxListTest.serialize(es));
		// compare arrays
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			byte[] fromEs = es2.get(i);
			assertArrayEquals(addme, fromEs);
		}
		es.clear();
	}

	@Test
	public void testPurge() throws Exception {
		ElasticByteStorage<Integer> es = new ElasticByteStorage<>();
		byte[] addme = new byte[100 * 1024];
		for (int i = 0; i < 20; i++) {
			Arrays.fill(addme, (byte) i);
			es.put(i, addme);
		}
		for (int i = 5; i < 15; i++)
			es.remove(i);
		// force deletion
		es.purge();
		Range<Integer> _5to15 = Range.make(5, 14);

		// compare arrays
		for (int i = 0; i < 20; i++) {
			// skip deleted elements
			if (_5to15.within(i))
				continue;
			Arrays.fill(addme, (byte) i);
			byte[] fromEs = es.get(i);
			assertArrayEquals(addme, fromEs);
		}
		es.clear();
	}

	public static class Range<T extends Comparable<T>> {
		T from;
		T to;

		public Range(T from, T to) {
			this.from = from;
			this.to = to;
		}

		public static <T extends Comparable<T>> Range<T> make(T from, T to) {
			return new Range<T>(from, to);
		}

		public boolean within(T var) {
			return var.compareTo(from) >= 0 && var.compareTo(to) <= 0;
		}
	}

}
