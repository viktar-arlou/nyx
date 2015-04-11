package nyx.collections;

import java.util.Map;

public class KVal<K, V> implements Map.Entry<K, V> {

	public K key;
	public V value;
	
	public KVal(K key, V value) {
		this.value = value;
		this.key = key;
	}
	
	@Override public K getKey() { return key; }
	@Override public V getValue() { return value; }
	@Override public V setValue(V value) { return this.value = value; }
}