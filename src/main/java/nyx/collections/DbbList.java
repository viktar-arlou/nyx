package nyx.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Java collection backed by off-heap memory storage.
 * 
 *  @author varlou@gmail.com
 */
public class DbbList<E> implements List<E> {

	private Class<E> type;
	private ByteBuffer dbb;

	private int size = -1;
	private int[] from;
	private int[] to;
	private int[] number;
	private int cursor = 0;
	ReadWriteLock dbbLock = new ReentrantReadWriteLock();

	private ObjectStreamClass osc;

	private int bufferSize = Constants._1Mb;

	// reusable 64Kb bytearray stream for object serialization
	private ByteArrayOutputStream baos = new ByteArrayOutputStream(Constants._1Kb * 64);

	public DbbList(Class<E> type, int size, int bufferSize) {
		super();
		this.type = type;
		this.size = size;
		this.bufferSize = bufferSize;
		this.osc = ObjectStreamClass.lookupAny(type);
		allocateMemory();
	}

	private void allocateMemory() {
		this.dbb = ByteBuffer.allocateDirect(bufferSize);
		this.from = new int[size];
		this.to = new int[size];
		this.number = new int[size];
		if (!this.dbb.isDirect())
			throw new RuntimeException("Can not allocate direct byte buffer");
	}

	/**
	 * @param bb
	 *            ByteBuffer to discard
	 * @throws ReflectiveOperationException
	 *             if no cleaner field present in the current ByteBuffer
	 *             implementation (e.g. non-Sun JDK)
	 */
	private void freeMemory(ByteBuffer bb) throws ReflectiveOperationException {
		Arrays.fill(from, 0);
		Arrays.fill(to, 0);
		Arrays.fill(number, 0);
		cursor = 0;
		// attemtps to deallocate direct byte buffer
		if (!bb.isDirect())
			return;
		Field cleanerField = bb.getClass().getDeclaredField("cleaner");
		cleanerField.setAccessible(true);
		Object cleaner = cleanerField.get(bb);
		Method cleanMethod = cleaner.getClass().getMethod("clean",
				new Class[] {});
		cleanMethod.invoke(cleaner, new Object[] {});
	}

	public Class<E> getType() {
		return this.type;
	}

	@Override
	public int size() {
		return this.cursor + 1;
	}

	@Override
	public boolean isEmpty() {
		return this.from[0] == 0 && this.to[0] == 0;
	}

	@Override
	public boolean contains(Object o) {
		return true;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean add(E e) {
		try {
			ObjectOutputStream os = new ObjectOutputStream(baos);
			os.writeUnshared(e);
		} catch (IOException e1) {
		}
		// int pos = index > 0 ? to[index - 1] : 0;
		// buffer.position(pos);
		try {
			dbbLock.writeLock().lock();
			from[cursor] = dbb.position();
			byte[] data = baos.toByteArray();
			try {
				dbb.put(data);
			} catch (BufferOverflowException err) {
				System.err.printf("Failed at position %s", from[cursor]);
				throw err;
			}
			baos.reset(); // reuse buffer
			number[cursor] = cursor;
			to[cursor++] = dbb.position();
		} finally {
			dbbLock.writeLock().unlock();
		}
		return true;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		for (E e : c)
			add(e);
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		try {
			freeMemory(dbb);
		} catch (ReflectiveOperationException e) {
		}
		allocateMemory();
	}

	@Override
	public E get(int index) {
		return subList(index, index).get(0);
	}

	@Override
	public E set(int index, E element) {
		return null;
	}

	@Override
	public void add(int index, E element) {
		// TODO Auto-generated method stub

	}

	@Override
	public E remove(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<E> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		try {
			dbbLock.readLock().lock();
			int realIdxA = this.number[fromIndex];
			int realIdxB = this.number[toIndex];
			dbb.position(from[realIdxA]);
			int size = to[realIdxB] - from[realIdxA];
			byte[] data = new byte[(int) size];
			dbb.get(data, 0, size);
			try {
				List<E> result = new ArrayList<>(toIndex - fromIndex);
				int offset = 0;
				for (int i = fromIndex; i <= toIndex; i++) {
					ByteArrayInputStream in = new ByteArrayInputStream(data,
							offset, data.length);
					ObjectInputStream ois = new ObjectInputStream(in);
					result.add((E) ois.readUnshared());
					offset += to[realIdxA] - from[realIdxA++]; //realIdxA reused as counter
				}
				return result;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		} finally {
			dbbLock.readLock().unlock();
		}
	}

}
