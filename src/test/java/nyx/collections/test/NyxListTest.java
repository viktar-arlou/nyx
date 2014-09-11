package nyx.collections.test;

import static nyx.collections.test.StrTestUtils.fillWithRandomAlphaNum;
import static nyx.collections.test.StrTestUtils.randomAlphaNum;
import static nyx.collections.test.StrTestUtils.randomStr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import nyx.collections.Acme;
import nyx.collections.Const;
import nyx.collections.NyxList;
import nyx.collections.pool.ObjectPool.Type;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class NyxListTest {

	@Test
	public void testStringWithComparison() throws IOException {
		int NUMBER_OF_ITEMS = 100000;
		int LENGTH = 300;
		List<String> list = new NyxList<>(NUMBER_OF_ITEMS, Const._1Mb * 10, Type.WEAK);
		String[] values = new String[NUMBER_OF_ITEMS];
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			values[i] = i + ":" + randomStr(LENGTH - 2, i % 2 == 0 ? "a" : "b");
			list.add(values[i]);
		}
		System.gc();
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			Assert.assertEquals(i + "'th element is wrong", values[i],
					list.get(i));
		}
		list.clear();
	}

	@Ignore
	@Test
	public void test1GbCollection() throws IOException {
		List<char[]> list = makeNyxList(10_000_000, 30);
		list.clear();
	}

	private List<char[]> makeNyxList(int numberOfElements, int stringLength) {
		List<char[]> list = new NyxList<>(numberOfElements, Const._1Mb * 2000, Type.WEAK);
		char[] value = new char[stringLength];
		int i = 0;
		try {
			for (i = 0; i < numberOfElements; i++) {
				fillWithRandomAlphaNum(value);
				list.add(value);
			}
		} catch (Exception e) {
			System.out.println("Add failed at " + i);
			throw new RuntimeException(e);
		}
		return list;
	}

	/**
	 * 1Gb off-heap list is created and closed 100 times - should give OOM
	 * exception if
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMemoryLeak() throws IOException {
		for (int i = 0; i < 10; i++) {
			List<char[]> list = new NyxList<>(1000, Const._1Mb * 100,Type.WEAK);
			list.clear();
		}
	}

	@Test
	public void testSubList() {
		int NUMBER_OF_ITEMS = 1000;
		int LENGTH = 300;
		List<String> list = new NyxList<>(NUMBER_OF_ITEMS, Const._1Mb * 10, Type.WEAK);
		String[] values = new String[NUMBER_OF_ITEMS];
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			values[i] = randomAlphaNum(LENGTH);
			list.add(values[i]);
		}
		System.gc();
		int shift = 333;
		List<String> sList = list.subList(shift, shift + 100);
		for (int i = 0; i < sList.size(); i++) {
			Assert.assertEquals(i + " element is incorrect", values[i + shift],
					sList.get(i));
		}
		list.clear();
	}

	@Test
	public void testSerializable() throws IOException, ClassNotFoundException {
		int NUMBER_OF_ITEMS = 1000;
		int LENGTH = 300;
		List<String> list = new NyxList<>(NUMBER_OF_ITEMS, Const._1Mb * 10,Type.WEAK);
		String[] values = new String[NUMBER_OF_ITEMS];
		for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
			values[i] = randomAlphaNum(LENGTH);
			list.add(values[i]);
		}
		byte[] serialized = serialize(list);
		Assert.assertTrue(serialized.length > 0);
		list = (List<String>) deserialize(serialized);
		int shift = 333;
		List<String> sList = list.subList(shift, shift + 100);
		for (int i = 0; i < sList.size(); i++) {
			Assert.assertEquals(i + " element is incorrect", values[i + shift],
					sList.get(i));
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
		List<String> list = new ArrayList<>();
		for (int i = 0; i < 10000; i++) {
			list.add("test" + i);
		}
		List<String> list2 = new NyxList<>(list);
		System.gc();
		Thread.sleep(3000);
		for (int i = 0; i < 10000; i++) {
			Assert.assertTrue(list2.get(i) == list.get(i));
		}
	}

	@Test
	public void testLostElements() throws Exception {
		List<String> list = new NyxList<>(16, Const._1Kb * 4, Type.SOFT);
		List<String> list2 = new ArrayList<>();
		int numberOfElements = 1_000_000;
		for (int i = 0; i < numberOfElements; i++) {
			//String element = ""+i; // no elements GC'ed
			list.add(""+i);
			list2.add(""+i);
		}
		Iterator<String> it = list.iterator();
		Iterator<String> it2 = list2.iterator();
		List<String> notFound = new ArrayList<>();
		while (it2.hasNext()) {
			String element;
			if (!(element = it2.next()).equals(it.next())) 
				notFound.add(element);
		}
		Assert.assertTrue(notFound.isEmpty());
	}

	@Test
	public void testConcurrent() throws Exception {
		class A { public volatile AtomicInteger total = new AtomicInteger(0); }
		int nThreads = 100;
		final int nRecords = 10000;
		final List<String> nyx = new NyxList<>();
		Thread[] athr = new Thread[nThreads];
		final A a = new A();
		for (int i = 0;i < athr.length; i++) {
		final int z = i;
		athr[i] = new Thread(new Runnable(){
			@Override
			public void run() {
				for (int y = 0; y < nRecords; y++)	{
					nyx.add(""+z);
					a.total.addAndGet(z);
				}
			}});
		athr[i].start();
		}
		for (int i = 0; i < athr.length; i++) 
			athr[i].join();
		int size = nyx.size();
		int total = 0;
		for (Iterator<String> iterator = nyx.iterator(); iterator.hasNext();) {
			int element = Integer.valueOf(iterator.next());
			total+=element;
		}
		Assert.assertTrue(size == nThreads*nRecords);
		int aTotal = a.total.get();
		Assert.assertTrue(aTotal == total);
	}
	
	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	public static Object deserialize(byte[] data) throws IOException,
			ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}

}