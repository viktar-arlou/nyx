package nyx.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.converter.Converter;
import nyx.collections.converter.SerialConverter;
import nyx.collections.storage.Storage;

/**
 * Implementation of Java {@link java.util.Map} collection backed by off-heap memory storage.
 * 
 * @author varlou@gmail.com
 */
public class NyxMap<K, V> implements Map<K, V> {

	private Set<K> elements = Acme.chashset();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private Storage<K, byte[]> storage;
	private Converter<Object, byte[]> converter = new SerialConverter();

	public NyxMap() {}

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
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(Object key) {
		try {
			lock.readLock().lock();
			return decode(storage.read(key));
		} finally {
			lock.readLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private V decode(byte[] data) {
		return (V) converter.decode(data);
	}

	@Override
	public V put(K key, V value) {
		try {
			lock.writeLock().lock();
			storage.create(key, converter.encode(value));
			return value;
			} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public V remove(Object key) {
		try {
			lock.writeLock().lock();
			return key != null && elements.remove(key) ? decode(storage.delete(key)) : null;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		try {
			lock.writeLock().lock();
			for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
				put(e.getKey(), e.getValue());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void clear() {
		try {
			lock.writeLock().lock();
			elements.clear();
			storage.clear();
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
			for (K e : elements) result.add(decode(storage.read(e)));
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

}
