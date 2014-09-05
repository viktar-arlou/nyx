package nyx.collections.test;

import java.util.Arrays;

import nyx.collections.Constants;
import nyx.collections.Acme;
import nyx.collections.converter.SerialConverter;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;

public class SerialConverterTest {

	/**
	 * Verifies if the byte buffer in {@link nyx.collections.converter.SerialConverter} is properly recycled.  
	 */
	@Test
	public void testReusableConverter() {
		SerialConverter converter = new SerialConverter();
		Assert.assertThat(converter.encode("test #1"), IsNot.not(IsEqual.equalTo(converter.encode("test #2"))));
	}
	
	/**
	 * Verifies if {@link nyx.collections.converter.SerialConverter} can handle objects bigger than its default capacity.
	 */
	@Test
	public void testCanExpand() {
		SerialConverter converter = new SerialConverter();
		byte[] ba = Acme.abyte(Constants._1Mb);
		Arrays.fill(ba, (byte)0xab);
		Assert.assertNotNull(converter.encode(ba));
	}

}
