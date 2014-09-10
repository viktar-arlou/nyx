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

	private final Set<K> elements;
	private Storage<K, V> storage;
	
	// main RW lock guarding all access to this Map
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	// counts update and delete operations
	private volatile int modCount = 0;

	public NyxMap() {
		this(16);
	}
	
	public NyxMap(int capacity) {
		if (capacity<1) throw new IllegalArgumentException();
		this.elements = Acme.chashset(capacity);
		this.storage = new ObjectPool<K,V>(new ElasticStorage<K>());
	}

	@Override
	public int size() {
		return this.elements.size();
	}

	@Override
	public boolean isEmpty() {
		return this.elements.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.elements.contains(key);
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
		V prevValue = null;
		try {
			lock.writeLock().lock();
			prevValue = remove(key);
			storage.create(key, value);
			return prevValue;
		} finally {
			if (prevValue!=null) checkMods();
			lock.writeLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		boolean delete = false;
		try {
			lock.writeLock().lock();
			delete = key != null && elements.remove(key);
			return delete ? storage.delete((K) key) : null;
		} finally {
			if (delete) checkMods();
			lock.writeLock().unlock();
		}
	}

	/**
	 * @see Map#putAll(Map)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		try {
			lock.writeLock().lock();
			for (Map.Entry<? extends K, ? extends V> e : map.entrySet())
				put(e.getKey(), e.getValue());
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	/**
	 * Checks total number of modifications and executes
	 * {@link nyx.collectoins.storage.Storage#purge} when it exceeds 1/3 of this
	 * collection size.
	 */
	private void checkMods() {
		if (modCount++ > size() / 3) {
			this.storage.purge();
			modCount = 0;
		}
	}

	@Override
	public void clear() {
		try {
			lock.writeLock().lock();
			elements.clear();
			storage.clear();
			modCount = 0;
		} finally {
			lock.writeLock().unlock();
		}

	}

	@Override
	public Set<K> keySet() {
		try {
			lock.readLock().lock();
			return Acme.umset(elements);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Collection<V> values() {
		try {
			lock.readLock().lock();
			List<V> result = new ArrayList<>(elements.size());
			for (K e : elements) result.add(storage.read(e));
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> result = Acme.hashset(size());
		for (K key : this.elements)	
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
