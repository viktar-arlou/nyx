package nyx.collections.fn;

import java.util.List;

import nyx.collections.NyxList;

import org.junit.Assert;
import org.junit.Test;

public class FnApiTest {

	@Test
	public void test() {
		List<Integer> list = new NyxList<>();
		for (int i = 0; i < 10; i++)
			list.add(i);
		list.add(null);
		Assert.assertTrue(list.contains(null));
		/* Filters out null elements */
		List<Integer> list1 = Fn.on(list).filter(Fn.<Integer>notNull()).get();
		Assert.assertFalse(list1.contains(null));
	    /* A field of an anonymous class can be used to accumulate and retrieve
		 * computation result */
		int sum = Fn.on(list1).forEach(new Fn.NoRet<Integer>() {
			int counter = 0;
			@Override
			public void func0(Integer t) { counter += t; }
		}).counter;
		Assert.assertEquals(sum, 45);
		list1 = Fn.on(list1).filter(Fn.<Integer>range(0,5)).get();
		sum = Fn.on(list1).forEach(new Fn.NoRet<Integer>() {
			int counter = 0;
			@Override public void func0(Integer t) { counter += t; }
		}).counter;
		Assert.assertEquals(sum, 15);
		
		Fn.on(list1).mapTo(new IFn<Integer, String>() {
			@Override public String func(Integer t) { return t.toString(); }
		}).forEach(new Fn.NoRet<String>() {
			@Override public void func0(String t) { System.out.println(t); }
		});
	}
}
