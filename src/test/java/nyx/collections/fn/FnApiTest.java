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
		int sum = Fn.on(list1).each().exec(new IFn<Integer, Void>() {
			int counter = 0;
			@Override
			public Void apply(Integer t) {
				counter += t;
				return null;
			}
		}).counter;
		Assert.assertEquals(sum, 45);
		list1 = Fn.on(list1).filter(Fn.<Integer>range(0,5)).get();
		sum = Fn.on(list1).each().exec(new IFn<Integer, Void>() {
			int counter = 0;
			@Override
			public Void apply(Integer t) {
				counter += t;
				return null;
			}
		}).counter;
		Assert.assertEquals(sum, 15);
	}
}
