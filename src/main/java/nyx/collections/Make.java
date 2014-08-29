package nyx.collections;

import java.nio.ByteBuffer;

/**
 * Utility class for creating Nyx objects. 
 * 
 * @author varlou@gmail.com
 */
public class Make {

	/**
	 * @return an array that contains 2 long elements.
	 */
	public static long[] along2() {return new long[2];}
	/**
	 * @param capacity
	 * @return a direct {@link java.nio.ByteBuffer} with a given capacity.
	 */
	public static ByteBuffer dbbuffer(int capacity) {return ByteBuffer.allocateDirect(capacity);}
	
	public static byte[] abyte(int size) {return new byte[size];}
	
}
