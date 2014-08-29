package nyx.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.storage.Storage;

public class NyxMap<E> implements Map<E, byte[]> {

	private Set<E> elements = Collections.newSetFromMap(new ConcurrentHashMap<E, Boolean>());
	private Storage<E, byte[]> storage;
	ReadWriteLock lock = new ReentrantReadWriteLock();

	public NyxMap() {}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return elements.contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] get(Object key) {
		try {
			lock.readLock().lock();
			return elements.contains(key) ? storage.read(key) : null;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public byte[] put(E key, byte[] value) {
		try {
			lock.writeLock().lock();
			byte[] prev = remove(key);
			this.elements.add(storage.create(key, value));
			return prev;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public byte[] remove(Object key) {
		try {
			lock.writeLock().lock();
			this.elements.remove(key);
			return storage.read(key);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void putAll(Map<? extends E, ? extends byte[]> map) {
		try {
			lock.writeLock().lock();
			for (Map.Entry<? extends E, ? extends byte[]> el : map.entrySet())
				put(el.getKey(), el.getValue());
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
	public Set<E> keySet() {
		try {
			lock.readLock().lock();
			return Collections.unmodifiableSet(elements);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Collection<byte[]> values() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<java.util.Map.Entry<E, byte[]>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

}
