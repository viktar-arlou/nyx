package nyx.collections.storage;

import java.util.Set;

/**
 * Key-value storage API. 
 * 
 * @param <K> 
 * @param <V>
 * 
 * @author varlou@gmail.com
 */
public interface Storage<K, V> {

	K create(K key, V value);
	V update(K key, V value);
	V read(Object id);
	V delete(Object id);
	void clear();
	int size();
	Set<K> keySet();
	void purge();
}
