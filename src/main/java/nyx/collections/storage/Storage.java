package nyx.collections.storage;

import java.util.Set;

/**
 * Defines interface of a key-value storage class. 
 * 
 * @author varlou@gmail.com
 */
public interface Storage<K, V> {

	V put(K key, V value);
	V update(K key, V value);
	V get(K id);
	V remove(K id);
	boolean contains(V value);
	void clear();
	int size();
	Set<K> keySet();
	void purge();
}
