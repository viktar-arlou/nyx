package nyx.collections;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.pool.ObjectPool;
import nyx.collections.pool.ObjectPool.Type;
import nyx.collections.storage.ElasticStorage;
import nyx.collections.storage.Storage;

/**
 * Hybrid (on-heap + off-heap) implementation of Java {@link java.util.List} collection.
 * 
 * @author varlou@gmail.com
 */
public class NyxList<E> implements List<E>, Serializable {

	private static final long serialVersionUID = 2004160303284077450L;

	private Storage<Integer, E> storage;

	private List<Integer> elements;
	private int size = 0;

	// counts update and delete operations
	private volatile int modCount = 0;

	// main RW lock guarding all access
	private transient ReadWriteLock lock = new ReentrantReadWriteLock();
	
	/**
	 * Creates NyxList with an initial capacity of 16 elements and 4Kb.
	 */
	public NyxList() {
		this(16, Const._1Kb * 4, Type.WEAK);
	}

	public NyxList(int capacity, int memSize, Type poolType) {
		if (capacity < 1)
			throw new IllegalArgumentException();
		this.storage = new ObjectPool<Integer,E>(poolType, new ElasticStorage<Integer>());
		this.elements = new ArrayList<>(capacity);
	}
	
	public NyxList(Collection<? extends E> copy) {
		this(copy.size(), Const._1Kb * 4, Type.WEAK);
		addAll(copy);
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
	public boolean contains(Object obj) {
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			E next = it.next();
			if (next == obj || next.equals(obj))
				return true;
		}
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private int iCursor = 0;
			@Override public boolean hasNext() { return iCursor <= NyxList.this.elements.size() - 1; }
			@Override public E next() { return get(iCursor++); }
			@Override public void remove() { NyxList.this.remove(get(iCursor)); }
		};
	}

	@Override
	public Object[] toArray() {
		try {
			lock.readLock().lock();
			Object[] result = new Object[size()];
			Iterator<E> it = iterator();
			int cnt = 0;
			while (it.hasNext())
				result[cnt++] = it.next();
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		a = a == null || a.length < size() ? (T[]) Array.newInstance(a
				.getClass().getComponentType(), size()) : a;
		for (int i = 0; i < a.length; i++)
			a[i] = (T) get(i);
		return a;
	}

	@Override
	public boolean add(E e) {
		try {
			lock.writeLock().lock();
			storage.create(size, e);
			this.elements.add(size++);
		} finally {
			lock.writeLock().unlock();
		}
		return e != null ? true : false;
	}

	@Override
	public void add(int index, E element) {
		try {
			lock.writeLock().lock();
			this.storage.create(size, element);
			this.elements.add(index, size++);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean remove(Object obj) {
		boolean deleted = false;
		try {
			lock.writeLock().lock();
			Iterator<Integer> iter = elements.iterator();
			while (iter.hasNext()) {
				Integer key = iter.next();
				E nextElement = this.storage.read(key);
				if (nextElement == obj || nextElement.equals(obj)) {
					iter.remove();
					deleted = true;
					break;
				}
			}
			return deleted;
		} finally {
			if (deleted)
				checkMods();
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Set<?> result = new HashSet<>(c);
		Iterator<E> it = iterator();
		while (it.hasNext())
			result.remove(it.next());
		return result.isEmpty();
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
		for (E e : c)
			add(index++, e);
		return c != null && !c.isEmpty();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object key : c)
			changed = remove(key) || changed;
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Iterator<E> it = iterator();
		boolean modified = false;
		while (it.hasNext()) {
			boolean keep = false;
			for (Object object : c) {
				keep = keep | it.next().equals(object);
				break;
			}
			if (!keep) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public void clear() {
		try {
			lock.writeLock().lock();
			elements.clear();
			storage.clear();
		} finally {
			modCount = 0;
			lock.writeLock().unlock();
		}
	}

	@Override
	public E get(int index) {
		return storage.read(index);
	}

	@Override
	public E set(int index, E element) {
		try {
			lock.writeLock().lock();
			E deleted = remove(index);
			add(index, element);
			return deleted;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public E remove(int index) {
		try {
			lock.writeLock().lock();
			return this.storage.delete(this.elements.get(index));
		} finally {
			checkMods();
			lock.writeLock().unlock();
		}
	}

	/**
	 * Checks the number of modifications and runs
	 * {@link nyx.collectoins.storage.Storage#purge} when it is greater than 1/3
	 * of this collection size.
	 */
	private void checkMods() {
		if (modCount++ > size() / 3) {
			this.storage.purge();
			modCount = 0;
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
		for (int i = size() - 1; i >= 0; i--) {
			if (o.equals(get(i)))
				return i;
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		return new ListItr();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new ListItr(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		try {
			lock.readLock().lock();
			List<E> result = new ArrayList<>(toIndex - fromIndex);
			for (Integer id : this.elements.subList(fromIndex, toIndex))
				result.add(storage.read(id));
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
		for (E e : this)
			hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof List))
			return false;

		ListIterator<E> e1 = listIterator();
		@SuppressWarnings("rawtypes")
		ListIterator e2 = ((List) o).listIterator();
		while (e1.hasNext() && e2.hasNext()) {
			E o1 = e1.next();
			Object o2 = e2.next();
			if (!(o1 == null ? o2 == null : o1.equals(o2)))
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());
	}

	
	private void readObject(java.io.ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		lock = new ReentrantReadWriteLock();
	}
	
	class ListItr implements ListIterator<E> {

		int cursor = 0;

		public ListItr() {}
		public ListItr(int position) { this.cursor = position; }

		@Override public boolean hasNext() { return cursor < size(); }
		@Override public E next() { return get(cursor++); }
		@Override public boolean hasPrevious() { return cursor > 0; }
		@Override public E previous() { return get(cursor--); }
		@Override public int nextIndex() { return cursor + 1; }
		@Override public int previousIndex() { return cursor - 1; }
		@Override public void remove() { NyxList.this.remove(cursor); }
		@Override public void set(E e) { NyxList.this.set(cursor, e); }
		@Override public void add(E e) { NyxList.this.add(e); }

	}
}
