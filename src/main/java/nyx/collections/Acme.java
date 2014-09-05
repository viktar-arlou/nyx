package nyx.collections;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  A Class that Makes Everything.
 * 
 * @author varlou@gmail.com
 */
public class Acme {

	public static long[] along2() {return new long[2];}
	public static ByteBuffer dbbuffer(int capacity) {return ByteBuffer.allocateDirect(capacity);}
	public static byte[] abyte(int size) {return new byte[size];}
	public static <K,V> Map<K, V> hashmap() {return new HashMap<>();}
	public static <K> Set<K> hashset() {return new HashSet<>();}
	public static <K> Set<K> hashset(int capacity) {return new HashSet<>(capacity);}
	public static <K> Set<K> hashset(Set<K> set) {return new HashSet<>(set);}
	public static <K> Set<K> chashset() {return Collections.newSetFromMap(new ConcurrentHashMap<K,Boolean>());}
	public static <K> Set<K> chashset(int capacity) {return Collections.newSetFromMap(new ConcurrentHashMap<K,Boolean>(capacity));}
	public static <K,V> Map<K, V> chashmap() {return new ConcurrentHashMap<>();}
	public static <K> Set<K> umset(Set<K> set) {return Collections.unmodifiableSet(set);}
	
}
