package nyx.collections;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	public static <E> Set<E> hashset() {return new HashSet<>();}
	public static <E> Set<E> hashset(int capacity) {return new HashSet<>(capacity);}
	public static <E> Set<E> hashset(Set<E> set) {return new HashSet<>(set);}
	public static <E> Set<E> copy(Set<E> set) {return new HashSet<>(set);}
	public static <E> List<E> copy(List<E> set) {return new ArrayList<>(set);}
	public static <E> Set<E> chashset() {return Collections.newSetFromMap(new ConcurrentHashMap<E,Boolean>());}
	public static <E> Set<E> chashset(int capacity) {return Collections.newSetFromMap(new ConcurrentHashMap<E,Boolean>(capacity));}
	public static <K,V> Map<K, V> chashmap() {return new ConcurrentHashMap<>();}
	public static <E> Set<E> umset(Set<E> set) {return Collections.unmodifiableSet(set);}
	public static <E> List<E> alist() {return new ArrayList<>();}
	public static <E> List<E> alist(int capacity) {return new ArrayList<>(capacity);}
}
