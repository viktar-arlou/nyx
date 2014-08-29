package nyx.collections.storage;

import java.util.Set;

/**
 * Generic interface representing a key-value storage API. 
 * 
 * @param <K> 
 * @param <V>
 * 
 * @author varlou@gmail.com
 */
public interface Storage<K, V> {

	K create(K key, V value);
	
	V read(Object id);
	
	V delete(Object id);

	void clear();
	
	int size();
	
	Set<K> keySet();
	
}
