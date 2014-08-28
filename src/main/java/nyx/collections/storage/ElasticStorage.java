package nyx.collections.storage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nyx.collections.Constants;

public class ElasticStorage<E> implements Storage<E> {

	private List<ByteBuffer> dbbs = new ArrayList<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private long cursor = 0;
	private int capacity = Constants._1Kb * 4; // default chunk size is 4Kb
	Map<E, along2> coords = new HashMap<>();

	public ElasticStorage() {}

	public ElasticStorage(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public E append(E id, byte[] addme) {
		try {
			lock.writeLock().lock();
			int commited = 0;
			along2 coord = new along2();
			coord.l1 = this.cursor;
			while (commited < addme.length) {
				ByteBuffer cbuf = currentBuffer();
				cbuf.position(currentOffset());
				int spaceLeft = capacity - cbuf.position();
				int size = Math.min(spaceLeft, addme.length - commited);
				cbuf.put(addme, commited, size);
				this.cursor += size;
				commited += size;
			}
			coord.l2 = this.cursor;
			coords.put(id, coord);
			return id;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public byte[] retrieve(Object id) {
		try {
			lock.readLock().lock();
			along2 coord = coords.get(id);
			byte[] result = new byte[(int) (coord.l2 - coord.l1)];
			int readed = 0;
			while (readed < result.length) {
				long pos = coord.l1 + readed;
				ByteBuffer bb = bufferForPosition(pos);
				int offset = offsetForPosition(pos);
				bb.position(offset);
				int size = Math.min(bb.remaining(), result.length - readed);
				bb.get(result, readed, size);
				readed += size;
			}
			return result;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void clear() {
		for (ByteBuffer byteBuffer : dbbs)
			freeDirectByteBuffer(byteBuffer);
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
		return ByteBuffer.allocateDirect(capacity);
	}

	private int currentOffset() {
		return offsetForPosition(this.cursor);
	}

	private int offsetForPosition(long pos) {
		return (int) (pos % capacity);
	}

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

}
