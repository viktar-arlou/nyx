package nyx.collections.storage;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.Acme;
import nyx.collections.Const;

/**
 * Elastic thread-safe storage for byte arrays. This is a base class which is
 * used by Nyx collection classes. Byte arrays are stored in direct
 * {@link java.nio.ByteBuffers} outside garbage-collected memory.
 * 
 * @author varlou@gmail.com
 */
public class ElasticStorage<E> implements Storage<E, byte[]>, Serializable {

	private static final long serialVersionUID = 1408552328267845863L;

	private transient List<ByteBuffer> dbbs = new ArrayList<>();
	private transient ReadWriteLock lock = new ReentrantReadWriteLock();
	private long cursor = 0;
	private int capacity = Const._1Kb * 4; // default chunk size is 4Kb
	private Map<E, long[]> elementsLocation = new HashMap<>();

	/**
	 * Creates instance with a default initial capacity (4Kb).
	 * 
	 * @param capacity
	 */
	public ElasticStorage() {
	}

	/**
	 * Creates instance with a given initial capacity in bytes.
	 * 
	 * @param capacity
	 *            the capacity of this storage in bytes
	 * @throws IllegalArgumentException
	 *             if {@code capacity < 4096}
	 */
	public ElasticStorage(int capacity) {
		if (capacity < Const._1Kb * 4)
			throw new IllegalArgumentException();
		this.capacity = capacity;
	}

	@Override
	public byte[] create(E id, byte[] addme) {
		if (elementsLocation.containsKey(id)) 
			throw new IllegalArgumentException();
		try {
			lock.writeLock().lock();
			int commited = 0;
			long[] location = Acme.along2();
			location[0] = this.cursor;
			while (commited < addme.length) {
				ByteBuffer cbuf = getBuffer();
				cbuf.position(currentOffset());
				int spaceLeft = capacity - cbuf.position();
				int size = Math.min(spaceLeft, addme.length - commited);
				cbuf.put(addme, commited, size);
				this.cursor += size;
				commited += size;
			}
			location[1] = this.cursor;
			elementsLocation.put(id, location);
			return addme;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public byte[] read(E id) {
		try {
			lock.readLock().lock();
			if (!elementsLocation.containsKey(id)) return null;
			long[] location = elementsLocation.get(id);
			assert location.length == 2;
			byte[] result = new byte[(int) (location[1] - location[0])];
			int readed = 0;
			while (readed < result.length) {
				long pos = location[0] + readed;
				ByteBuffer bb = bufferForPosition(pos);
				int offset = offsetForPosition(pos);
				synchronized (bb) {
					bb.position(offset);
					int size = Math.min(bb.remaining(), result.length - readed);
					bb.get(result, readed, size);
					readed += size;
				}
			}
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void clear() {
		try {
			lock.writeLock().lock();
			this.elementsLocation.clear();
			for (ByteBuffer byteBuffer : dbbs)
				deallocDirectByteBuffer(byteBuffer);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private ByteBuffer getBuffer() {
		return bufferForPosition(cursor);
	}

	private ByteBuffer bufferForPosition(long pos) {
		int idx = (int) (pos / capacity);
		ByteBuffer byteBuffer = dbbs.size() > idx ? dbbs.get(idx) : null;
		if (byteBuffer == null)
			dbbs.add(byteBuffer = makeNewBuffer());
		return byteBuffer;
	}

	private ByteBuffer makeNewBuffer() {
		return Acme.dbbuffer(capacity);
	}

	private int currentOffset() {
		return offsetForPosition(this.cursor);
	}

	private int offsetForPosition(long pos) {
		return (int) (pos % capacity);
	}

	/**
	 * @param bb
	 *            ByteBuffer to discard
	 * @throws ReflectiveOperationException
	 *             if no cleaner field found in current implementation of
	 *             ByteBuffer (may happen in case of non-Sun/Oracle JDK)
	 */
	private void deallocDirectByteBuffer(ByteBuffer bb) {
		if (!bb.isDirect())
			return;
		Field cleanerField;
		try {
			cleanerField = bb.getClass().getDeclaredField("cleaner");
			cleanerField.setAccessible(true);
			Object cleaner = cleanerField.get(bb);
			Method cleanMethod = cleaner.getClass().getMethod("clean",
					new Class[] {});
			cleanMethod.invoke(cleaner, new Object[] {});
		} catch (ReflectiveOperationException | SecurityException
				| IllegalArgumentException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Deletes given object from the storage.
	 * <p>
	 * The memory occupied by the object is not actually freed, so this method
	 * only <b>marks the object for deletion</b>.
	 * 
	 * @param id
	 *            the id of the object to be removed
	 */
	@Override
	public byte[] delete(E id) {
		try {
			lock.writeLock().lock();
			byte[] res = read(id);
			this.elementsLocation.remove(id);
			return res;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public int size() {
		return this.elementsLocation.size();
	}

	@Override
	public Set<E> keySet() {
		return Acme.umset(this.elementsLocation.keySet());
	}

	@Override
	public byte[] update(E key, byte[] value) {
		try {
			lock.writeLock().lock();
			byte[] old = delete(key);
			create(key, value);
			return old;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Removes all elements previously marked for deletion (by
	 * {@link #delete(Object)} method) from this storage.
	 * 
	 * This is very basic implementation which needs to be improved in the new
	 * versions of Nyx Collections. It simply copies all remaining elements into
	 * new {@link ByteBuffer} and destroys old ones.
	 */
	@Override
	public void purge() {
		// Basic implementation - copies all remaining elements into new
		// byte buffers and removes old ones. Doubles
		try {
			lock.writeLock().lock();
			ElasticStorage<E> copy = new ElasticStorage<E>(this.capacity);
			for (Entry<E, long[]> entry : elementsLocation.entrySet()) {
				copy.create(entry.getKey(), read(entry.getKey()));
			}
			// deallocate off-heap memory
			for (ByteBuffer oldBuffer : this.dbbs)
				deallocDirectByteBuffer(oldBuffer);
			// switch to the newly created Storage instance.
			
			this.dbbs = copy.dbbs;
			this.elementsLocation = copy.elementsLocation;
			this.cursor = copy.cursor;
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(new Object[]{ this.capacity,this.elementsLocation, this.cursor, this.dbbs.size() });
		for (ByteBuffer byteBuffer : dbbs) {
			byte[] toWrite = new byte[byteBuffer.limit()];
			byteBuffer.position(0);
			byteBuffer.get(toWrite);
			out.writeObject(toWrite);
		}
		out.flush();
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		Object[] fields = (Object[]) in.readObject();
		this.capacity = (int) fields[0];
		this.elementsLocation= (Map<E, long[]>) fields[1];
		this.cursor = (long) fields[2];
		int size = (int)fields[3];
		this.dbbs = Acme.alist(size);
		this.lock = new ReentrantReadWriteLock();
		for (int i = 0; i < size; i++) {
			byte[] toRead = (byte[])in.readObject();
			ByteBuffer byteBuffer = Acme.dbbuffer(this.capacity);
			byteBuffer.put(toRead);
			this.dbbs.add(byteBuffer);
		}
	}
	
}
