package nyx.collections.test;

import static nyx.collections.test.StrTestUtils.fillWithRandomAlphaNum;
import static nyx.collections.test.StrTestUtils.randomAlphaNum;
import static nyx.collections.test.StrTestUtils.randomStr;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import nyx.collections.Const;
import nyx.collections.NyxList;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class NyxListTest {

	@Test
	public void testStringWithComparison() throws IOException {
		int NUMBER_OF_ITEMS = 1000;
		int LENGTH = 300;
		List<String> list = new NyxList<>(NUMBER_OF_ITEMS, Const._1Mb * 10);
		String[] values = new String[NUMBER_OF_ITEMS];
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			values[i] = i+":"+ randomStr(LENGTH-2, i % 2 == 0 ? "a" : "b");
			list.add(values[i]);
		}
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			Assert.assertEquals(i + "'th element is wrong", values[i], list.get(i));
		}
		list.clear();
	}

	@Ignore
	@Test
	public void test1GbCollection() throws IOException {
		int NUMBER_OF_ITEMS = 10_000_000;
		int LENGTH = 30;
		List<char[]> list = new NyxList<>(NUMBER_OF_ITEMS, Const._1Mb * 2000);
		char[] value = new char[LENGTH];
		int i = 0;
		try {
			for (i = 0; i < NUMBER_OF_ITEMS; i++) {
				fillWithRandomAlphaNum(value);
				list.add(value);
			}
			
		} catch (Exception e) {
			System.out.println("Add failed at " + i);
			throw e;
		}
		list.clear();
	}
	
	/**
	 * 1Gb off-heap list is created and closed 100 times - should give OOM exception if 
	 * @throws IOException
	 */
	@Test
	public void testMemoryLeak() throws IOException {
		for (int i = 0; i < 10; i++) {
			List<char[]> list = new NyxList<>(1000, Const._1Mb * 100);
			list.clear();
		}
	}

	@Test
	public void testSubList() {
		int NUMBER_OF_ITEMS = 1000;
		int LENGTH = 300;
		List<String> list = new NyxList<>(NUMBER_OF_ITEMS, Const._1Mb * 10);
		String[] values = new String[NUMBER_OF_ITEMS];
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			values[i] = randomAlphaNum(LENGTH);
			list.add(values[i]);
		}
		int shift = 333;
		List<String> sList = list.subList(shift, shift+100);
		for (int i = 0; i < sList.size(); i++) {
			Assert.assertEquals(i + " element is incorrect", values[i+shift], sList.get(i));
		}
		list.clear();
	}
	
	@Test
	public void testSerializable() throws IOException, ClassNotFoundException {
		int NUMBER_OF_ITEMS = 1000;
		int LENGTH = 300;
		List<String> list = new NyxList<>(NUMBER_OF_ITEMS, Const._1Mb * 10);
		String[] values = new String[NUMBER_OF_ITEMS];
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			values[i] = randomAlphaNum(LENGTH);
			list.add(values[i]);
		}
		byte[] serialized = serialize(list);
		Assert.assertTrue(serialized.length > 0);
		list = (List<String>) deserialize(serialized);
		int shift = 333;
		List<String> sList = list.subList(shift, shift+100);
		for (int i = 0; i < sList.size(); i++) {
			Assert.assertEquals(i + " element is incorrect", values[i+shift], sList.get(i));
		}
		list.clear();
	}

	@Test
	public void testNull() throws Exception {
		List<String> list = new NyxList<>();
		list.add("test1");
		list.add(null);
		list.add("test2");
		Assert.assertTrue(list.contains(null));
	}

	@Test
	public void testObjectPool() throws Exception {
		List<String> list = new NyxList<>();
		for (int i = 0; i < 1000; i++) {
			list.add("test"+i);
		}
		for (int i = 0; i < 1000; i++) {
			Assert.assertTrue(list.get(i)==list.get(i));
		}
	}
	
	public static byte[] serialize(Object obj) throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    os.writeObject(obj);
	    return out.toByteArray();
	}
	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return is.readObject();
	}
	
}