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
	public static <K,V> Map<K, V> hashmap() {return new HashMap<K,V>();}
	public static <E> Set<E> hashset() {return new HashSet<E>();}
	public static <E> Set<E> hashset(int capacity) {return new HashSet<E>(capacity);}
	public static <E> Set<E> hashset(Set<E> set) {return new HashSet<E>(set);}
	public static <E> Set<E> copy(Set<E> set) {return new HashSet<E>(set);}
	public static <E> List<E> copy(List<E> set) {return new ArrayList<E>(set);}
	public static <E> Set<E> chashset() {return Collections.newSetFromMap(new ConcurrentHashMap<E,Boolean>());}
	public static <E> Set<E> chashset(int capacity) {return Collections.newSetFromMap(new ConcurrentHashMap<E,Boolean>(capacity));}
	public static <K,V> Map<K, V> chashmap() {return new ConcurrentHashMap<K,V>();}
	public static <E> Set<E> umset(Set<E> set) {return Collections.unmodifiableSet(set);}
	public static <E> List<E> alist() {return new ArrayList<E>();}
	public static <E> List<E> alist(int capacity) {return new ArrayList<E>(capacity);}
}
