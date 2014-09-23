package nyx.collections.test;

import java.util.Set;

import nyx.collections.NyxSet;

import org.junit.Assert;
import org.junit.Test;

public class NyxSetTest {

	@Test
	public void testDoubleAdd() {
		Set<String> test = new NyxSet<>();
		test.add("test1");
		test.add("test1");
		Assert.assertEquals(test.size(), 1);
	}
	
}