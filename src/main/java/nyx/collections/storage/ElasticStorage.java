package nyx.collections.storage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.Constants;
import nyx.collections.Make;

/**
 * Elastic thread-safe storage for byte arrays. This is a base class which is
 * used by Nyx collection classes. Byte arrays are stored in direct
 * {@link java.nio.ByteBuffers} outside garbage-collected memory.
 * 
 * @author varlou@gmail.com
 *
 * @param <E>
 *            key type
 */
public class ElasticStorage<E> implements Storage<E, byte[]> {

	private List<ByteBuffer> dbbs = new ArrayList<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private long cursor = 0;
	private int capacity = Constants._1Kb * 4; // default chunk size is 4Kb
	private Map<E, long[]> elementsLocation = new HashMap<>();

	/**
	 * Creates instance with a default initial capacity (4Kb).
	 * 
	 * @param capacity
	 */
	public ElasticStorage() {}

	/**
	 * Creates instance with a given initial capacity.
	 * 
	 * @param capacity
	 */
	public ElasticStorage(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public E create(E id, byte[] addme) {
		try {
			lock.writeLock().lock();
			int commited = 0;
			long[] location = Make.along2();
			location[0] = this.cursor;
			while (commited < addme.length) {
				ByteBuffer cbuf = currentBuffer();
				cbuf.position(currentOffset());
				int spaceLeft = capacity - cbuf.position();
				int size = Math.min(spaceLeft, addme.length - commited);
				cbuf.put(addme, commited, size);
				this.cursor += size;
				commited += size;
			}
			location[1] = this.cursor;
			elementsLocation.put(id, location);
			return id;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public byte[] read(Object id) {
		try {
			lock.readLock().lock();
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
				freeDirectByteBuffer(byteBuffer);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private ByteBuffer currentBuffer() {
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
		return Make.dbbuffer(capacity);
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
	 *             if no cleaner field found in current implementation of ByteBuffer (may happen in case of non-Sun/Oracle JDK)
	 */
	private void freeDirectByteBuffer(ByteBuffer bb) {
		if (!bb.isDirect())
			return;
		Field cleanerField;
		try {
			cleanerField = bb.getClass().getDeclaredField("cleaner");
			cleanerField.setAccessible(true);
			Object cleaner = cleanerField.get(bb);
			Method cleanMethod = cleaner.getClass().getMethod("clean", new Class[] {});
			cleanMethod.invoke(cleaner, new Object[] {});
		} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Deletes given object from the storage.
	 * <p>
	 * The memory region previously occupied by the object is not freed, so this
	 * method rather forgets than deletes a given object.
	 * 
	 * @param id
	 *            object key
	 */
	@Override
	public byte[] delete(Object id) {
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
		return Collections.unmodifiableSet(this.elementsLocation.keySet());
	}
	
}
