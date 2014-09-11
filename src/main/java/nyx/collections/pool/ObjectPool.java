package nyx.collections.pool;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nyx.collections.Acme;
import nyx.collections.converter.Converter;
import nyx.collections.converter.ConverterFactory;
import nyx.collections.storage.Storage;
import nyx.collections.vm.GCDetector;
import nyx.collections.vm.GCDetector.Callback;
import nyx.collections.vm.GCDetector.NotAvailable;

/**
 * This class represents a pool of objects and implements
 * {@link nyx.collections.storage.Storage} interface.
 * 
 * @author varlou@gmail.com
 */
public class ObjectPool<K, E> implements Storage<K, E>, Callback<Void>, Serializable {

	public enum Type { NONE, WEAK, SOFT }

	private static final long serialVersionUID = 1188810172228586264L;


	private Map<K, Reference<E>> objectPool = Acme.chashmap();
	private Storage<K, byte[]> offHeapStorage;
	private Converter<E, byte[]> converter = ConverterFactory.get();

	private ReferenceQueue<E> rQueue;
	private Lock rqLock;
	private Condition notEmpty;
	

	private Type type;
	private ValueFactory vf;
	
	private Thread cleaner;
	private volatile boolean stopCleaning = false;

	public ObjectPool(Storage<K, byte[]> offHeapStorage) {
		this(Type.WEAK,offHeapStorage);
	}
	
	public ObjectPool(Type type, Storage<K, byte[]> offHeapStorage) {
		init(type, offHeapStorage);
	}

	private void init(Type type, Storage<K, byte[]> offHeapStorage) {
		this.offHeapStorage = offHeapStorage;
		this.rQueue = new ReferenceQueue<>();
		this.rqLock = new ReentrantLock();
		this.notEmpty = rqLock.newCondition();
		this.vf = new ValueFactory();
		this.type = type;
		try {
			GCDetector.listen(this);
		} catch (NotAvailable e) {
			// run worker thread for cleaning
		}
		if (!isNone()) {
			cleaner = new Thread(new Runnable() {
				@Override public void run() {
					while (!stopCleaning) try { clean(); } catch (InterruptedException e) { } }
				});
			cleaner.start();
		}
	}

	private boolean isNone() {
		return this.type.equals(Type.NONE);
	}

	@Override
	public E read(K key) {
		E res = objectPool.containsKey(key) ? objectPool.get(key).get() : null;
		if (res==null) objectPool.put(key, new WeakReference<>(res = converter.decode(this.offHeapStorage.read(key))));
		return res;
	}

	@Override
	public E create(K key, E value) {
		if (!isNone()) this.objectPool.put(key, vf.make(key, value, rQueue));
		this.offHeapStorage.create(key,converter.encode(value));
		return value;
	}

	@Override
	public E delete(K key) {
		E res = converter.decode(this.offHeapStorage.delete(key));
		this.objectPool.remove(key);
		return res;
	}

	@SuppressWarnings("unchecked")
	public void clean() throws InterruptedException  {
		rqLock.lock();
		try {
			int cleaned = 0;
			Value<K> qe = null;
			while ((qe = (Value<K>) rQueue.poll()) == null) notEmpty.await();
			do { objectPool.remove(qe.getKey());cleaned++; } while ((qe = (Value<K>) rQueue.poll()) != null);
//			System.err.println("cleaned: "+cleaned);
		} finally {
			rqLock.unlock();
		}
	}

	/** This method is called after each GC */
	@Override
	public void handle(Void e) {
		rqLock.lock();
		try { notEmpty.signal(); } finally { rqLock.unlock(); }
	}

	@Override
	public E update(K key, E value) {
		objectPool.remove(key);
		offHeapStorage.update(key, converter.encode(value));
		return value;
	}

	@Override
	public void clear() {
		objectPool.clear();
		offHeapStorage.clear();
		init(type, offHeapStorage);
	}

	@Override
	public int size() {
		return offHeapStorage.size();
	}

	@Override
	public Set<K> keySet() {
		return offHeapStorage.keySet();
	}

	@Override
	public void purge() {
		throw new UnsupportedOperationException();
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(new Object[] {offHeapStorage,type});
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		this.objectPool = Acme.chashmap();
		this.rQueue = new ReferenceQueue<>();
		this.converter = ConverterFactory.get();
		Object[] obj = (Object[]) in.readObject();
		init((Type) obj[1], (Storage<K, byte[]>) obj[0]);
	}

	
	public class ValueFactory {
		public Reference<E> make(K key, E value, ReferenceQueue<E> rQueue) {
			return (ObjectPool.this.type.equals(Type.SOFT)) ? new ValueSoft(key,value) : new ValueWeak(key,value);
		}
	}

	class ValueSoft extends SoftReference<E> implements Value<K> {
		public K key;
		public ValueSoft(K key, E referent) {
			super(referent, rQueue);
			this.key = key;
		}
		public K getKey() { return key; }
	}
	
	class ValueWeak extends WeakReference<E> implements Value<K> {
		public K key;
		public ValueWeak(K key, E referent) {
			super(referent, rQueue);
			this.key = key;
		}
		public K getKey() { return key; }
	}
	
	interface Value<K> { K getKey(); }
	
}
