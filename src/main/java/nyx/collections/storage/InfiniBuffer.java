package nyx.collections.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InfiniBuffer<E> implements Map<E, byte[]> {

	private Set<E> elements = new HashSet<>();
	private Storage<E> storage;
	ReadWriteLock lock = new ReentrantReadWriteLock();

	public InfiniBuffer() {}

	@Override
	public int size() {
		try {
			lock.readLock().lock();
			return elements.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		try {
			lock.readLock().lock();
			return elements.contains(key);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] get(Object key) {
		try {
			lock.readLock().lock();
			return elements.contains(key) ? storage.retrieve(key) : null;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public byte[] put(E key, byte[] value) {
		try {
			lock.writeLock().lock();
			byte[] prev = remove(key);
			this.elements.add(storage.append(key, value));
			return prev;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public byte[] remove(Object key) {
		try {
			lock.writeLock().lock();
			return null;
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
