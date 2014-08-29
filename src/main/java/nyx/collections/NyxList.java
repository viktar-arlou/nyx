package nyx.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.converter.Converter;
import nyx.collections.converter.SerialConverter;
import nyx.collections.storage.ElasticStorage;
import nyx.collections.storage.Storage;

/**
 * Java {@link java.util.List} collection backed by off-heap memory storage.
 * 
 * @author varlou@gmail.com
 */
public class NyxList<E> implements List<E> {

	private Storage<Integer, byte[]> storage;
	private Converter<Object, byte[]> converter = new SerialConverter();

	private List<Integer> elements;
	private int cursor = 0;

	ReadWriteLock lock = new ReentrantReadWriteLock();

	public NyxList(int capacity, int memSize) {
		super();
		this.storage = new ElasticStorage<>(memSize);
		this.elements = new ArrayList<>(capacity);
	}

	@Override
	public int size() {
		return storage.size();
	}

	@Override
	public boolean isEmpty() {
		return storage.size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			if (it.next().equals(o)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {

			private int iCursor = -1;

			@Override
			public boolean hasNext() {
				return iCursor < NyxList.this.elements.size();
			}

			@Override
			public E next() {
				return get(iCursor++);
			}

			@Override
			public void remove() {
				NyxList.this.remove(get(iCursor));
			}
		};
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return null;
	}

	@Override
	public boolean add(E e) {
		try {
			lock.writeLock().lock();
			this.storage.create(cursor, this.converter.encode(e));
			this.elements.add(cursor++);
		} finally {
			lock.writeLock().unlock();
		}
		return e != null ? true : false;
	}

	@Override
	public boolean remove(Object o) {
		try {
			lock.writeLock().lock();
			boolean deleted = false;
			Iterator<Integer> iter = elements.iterator();
			while (iter.hasNext()) {
				Integer i = iter.next();
				if (this.storage.read(i).equals(o)) {
					iter.remove();
					deleted = true;
				}
			}
			return deleted;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		try {
			lock.writeLock().lock();
			for (E e : c)
				add(e);
		} finally {
			lock.writeLock().unlock();
		}
		return c != null && !c.isEmpty();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object key : c) {
			changed = remove(key) || changed;
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			boolean keep = false;
			for (Object object : c) {
				keep = keep | it.next().equals(object);
				break;
			}
			if (!keep)
				it.remove();
		}
		throw new UnsupportedOperationException();
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
	public E get(int index) {
		return decode(storage.read(this.elements.get(index)));
	}

	@SuppressWarnings("unchecked")
	E decode(byte[] data) {
		return (E) this.converter.decode(data);
	}

	@Override
	public E set(int index, E element) {
		return null;
	}

	@Override
	public void add(int index, E element) {
		// TODO Auto-generated method stub
		this.cursor++;
	}

	@Override
	public E remove(int index) {
		try {
			lock.writeLock().lock();
			return decode(this.storage.delete(this.elements.get(index)));
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public int indexOf(Object o) {
		int id = 0;
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			Object r = it.next();
			if (r.equals(o))
				return id;
			id++;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		return 0;
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		try {
			lock.readLock().lock();
			List<E> result = new ArrayList<>(toIndex - fromIndex);
			for (Integer id : this.elements.subList(fromIndex, toIndex))
				result.add(decode(storage.read(id)));
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

}
