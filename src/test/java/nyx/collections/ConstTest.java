package nyx.collections;

import static org.junit.Assert.*;

import java.io.IOException;

import nyx.collections.test.NyxListTest;

import org.junit.Test;

public class ConstTest {

	@Test
	public void testNULL() throws IOException, ClassNotFoundException {
		Object s = Const.<String>nil();
		Object s1 = NyxListTest.deserialize(NyxListTest.serialize(s));
		assertTrue(s==s1);
	}

}
