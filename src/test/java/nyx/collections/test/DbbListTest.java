package nyx.collections.test;

import java.io.IOException;
import java.util.List;

import nyx.collections.Constants;
import nyx.collections.DbbList;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static nyx.collections.test.StrTestUtils.*;

public class DbbListTest {

	@Test
	public void testStringWithComparison() throws IOException {
		int NUMBER_OF_ITEMS = 1000;
		int LENGTH = 300;
		List<String> list = new DbbList<>(String.class, NUMBER_OF_ITEMS, Constants._1Mb * 10);
		String[] values = new String[NUMBER_OF_ITEMS];
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			values[i] = i+":"+ randomAlphaNum(LENGTH-2);
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
		List<char[]> list = new DbbList<>(char[].class, NUMBER_OF_ITEMS, Constants._1Mb * 2000);
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
			List<char[]> list = new DbbList<>(char[].class, 1000, Constants._1Mb * 100);
			list.clear();
		}
	}

	@Test
	public void testSubList() {
		int NUMBER_OF_ITEMS = 1000;
		int LENGTH = 300;
		List<String> list = new DbbList<>(String.class, NUMBER_OF_ITEMS, Constants._1Mb * 10);
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

}