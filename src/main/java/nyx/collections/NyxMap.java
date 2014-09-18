package nyx.collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.pool.ObjectPool;
import nyx.collections.storage.ElasticStorage;
import nyx.collections.storage.Storage;

/**
 * Hybrid (on-heap + off-heap) implementation of Java {@link java.util.Map} collection.
 * 
 * @author varlou@gmail.com
 */
public class NyxMap<K, V> implements Map<K, V> {

	private Storage<K, V> storage;
	
	// main RW lock guarding all access to this Map
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	// counts update and delete operations
	private volatile int modCount = 0;
	private final int MOD_THRESHOLD = 3;
	
	public NyxMap() {
		this(16);
	}
	
	public NyxMap(int capacity) {
		if (capacity<1) throw new IllegalArgumentException();
		this.storage = new ObjectPool<K,V>(new ElasticStorage<K>());
	}

	@Override
	public int size() {
		return this.storage.size();
	}

	@Override
	public boolean isEmpty() {
		return this.storage.size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return storage.keySet().contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (Map.Entry<K, V> entry : entrySet())
			if (entry.getValue().equals(value)) return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		try {
			lock.readLock().lock();
			return storage.read((K) key);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public V put(K key, V value) {
		V prevValue = checkMods(remove(key));
		storage.create(key, value);
		return prevValue;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		return checkMods(storage.delete((K) key));
	}

	/**
	 * @see Map#putAll(Map)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		for (Map.Entry<? extends K, ? extends V> e : map.entrySet())
			put(e.getKey(), e.getValue());
	}
	
	/**
	 * Checks total number of modifications and executes
	 * {@link nyx.collectoins.storage.Storage#purge} when it exceeds 1/3 of this
	 * collection size.
	 * 
	 * @param prevValue variable equal to <b>null</b> causes this method to skip.
	 */
	private V checkMods(V prevValue) {
		
		if (prevValue!=null && modCount++ > size() / MOD_THRESHOLD) {
			this.storage.purge();
			modCount = 0;
		}
		return prevValue;
	}

	@Override
	public void clear() {
		modCount = 0;
		storage.clear();
	}

	@Override
	public Set<K> keySet() {
		return storage.keySet();
	}

	@Override
	public Collection<V> values() {
		List<V> result = new ArrayList<>(storage.size());
		for (K key : this.storage.keySet()) result.add(storage.read(key));
		return result;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> result = Acme.hashset(size());
		for (K key : this.storage.keySet())	
			result.add(new ANyxEntry(key) {
				@Override public V getValue() {return NyxMap.this.get(getKey());
			}});
		return result;
	}
	
	private void readObject(java.io.ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		this.lock = new ReentrantReadWriteLock();
	}	
	
	abstract class ANyxEntry implements Map.Entry<K,V> {
		private K key;
		public ANyxEntry(K key) {this.key = key;}
		@Override public K getKey() {return key;}
		@Override public V setValue(V value) {return put(key, value);}
	}

}
