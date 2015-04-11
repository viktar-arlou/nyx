package nyx.collections.pool;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nyx.collections.Acme;
import nyx.collections.KVal;
import nyx.collections.converter.Converter;
import nyx.collections.converter.ConverterFactory;
import nyx.collections.storage.Storage;
import nyx.collections.vm.GCDetector;
import nyx.collections.vm.GCDetector.Callback;

/**
 * This class represents a pool of objects and implements
 * {@link nyx.collections.storage.Storage} interface.
 * 
 * @author varlou@gmail.com
 */
public class ObjectPool<K, E> implements Storage<K, E>, Callback<Void>, Serializable {

	public interface IntFn<T> { T run(); }

	public enum Type { NONE, WEAK, SOFT }

	private static final long serialVersionUID = 1188810172228586264L;


	private Map<K, Reference<E>> objectPool = Acme.chashmap();
	private Storage<K, byte[]> offHeapStorage;
	private Converter<E, byte[]> converter = ConverterFactory.get();

	private ReferenceQueue<E> rQueue;
	private Lock rqLock;
	private Condition notEmpty;
	
	/* asynchronous Storage#create */
	private Queue<KVal<K,E>> storageQueue;
	private Lock storageLock;
	private Condition sqProcess;
	private Condition sqEmpty;
	private Thread objectRelocator;
	
	private Type type;
	private ValueFactory vf;
	
	/* clean-up worker */
	private Thread cleaner;
	private Thread gcTimer;
	
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
		this.storageQueue = new ConcurrentLinkedQueue<>();
		this.storageLock = new ReentrantLock();
		this.sqProcess = storageLock.newCondition();
		this.sqEmpty = storageLock.newCondition();
		this.notEmpty = rqLock.newCondition();
		this.vf = new ValueFactory();
		this.type = type;
		try {
			GCDetector.listen(this);
		} catch (GCDetector.NotAvailable e) {
			// obsolete JVM without GC detection capabilities
			startGCTimer();
		}
		startCleaner();
		startRelocator();		
	}

	private void startGCTimer() {
		this.gcTimer = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						TimeUnit.SECONDS.sleep(10);
						handle(null);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}});
		this.gcTimer.setDaemon(true);
		this.gcTimer.start();
	}

	private void startRelocator() {
		this.objectRelocator = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					storageLock.lock();
					try {
						while (storageQueue.isEmpty()) sqProcess.await();
						KVal<K, E> kv;
						while ((kv = storageQueue.poll())!=null)
							ObjectPool.this.offHeapStorage.put(kv.key, converter.encode(kv.value));
						sqEmpty.signalAll();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally { storageLock.unlock(); }
				}
			}
		});
		objectRelocator.setDaemon(true);
		objectRelocator.start();
	}

	private void startCleaner() {
		if (!isNone()) {
			this.cleaner = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!Thread.currentThread().isInterrupted()) {
						try {
							rqLock.lock(); 
							Value<K> qe;
							while ((qe = (Value<K>) rQueue.poll()) == null) notEmpty.await();
							do { objectPool.remove(qe.getKey()); } while ((qe = (Value<K>) rQueue.poll()) != null);
						} catch (InterruptedException e) {
							// restore interrupted status
							Thread.currentThread().interrupt();
						} finally { rqLock.unlock(); }
					}
				}
			});
			cleaner.setDaemon(true);
			cleaner.start();
		}
	}

	private boolean isNone() {
		return this.type.equals(Type.NONE);
	}

	public <T> T syncStorage(IntFn<T> fn ) {
		storageLock.lock();
		try {
			if (!storageQueue.isEmpty()) sqEmpty.await();
			return fn.run();
		} catch (InterruptedException e) {
		} finally { storageLock.unlock(); }
		return null;
	}
	
	@Override
	public E get(final K key) {
		return syncStorage(new IntFn<E>() {
			@Override
			public E run() {
				E res = objectPool.containsKey(key) ? objectPool.get(key).get() : null;
				if (res == null)
					objectPool.put(key,
							new WeakReference<>(res = converter
									.decode(ObjectPool.this.offHeapStorage
											.get(key))));
				return res;
			}
		});
	}

	@Override
	public E put(K key, E value) {
		if (!isNone()) this.objectPool.put(key, vf.make(key, value, rQueue));
		try {
			storageLock.lock();
			storageQueue.add(new KVal<>(key,value));
			sqProcess.signal();
		} finally { storageLock.unlock(); }
		return value;
	}

	@Override
	public E remove(final K key) {
		return syncStorage(new IntFn<E>() {
			@Override public E run() {
				E res = converter.decode(ObjectPool.this.offHeapStorage.remove(key));
				ObjectPool.this.objectPool.remove(key);
				return res;
			}
		});
	}

	/** This method is called after each GC */
	@Override
	public void handle(Void e) {
		rqLock.lock();
		try { notEmpty.signal(); } finally { rqLock.unlock(); }
	}

	@Override
	public E update(final K key, final E value) {
		return syncStorage(new IntFn<E>() {
			@Override public E run() {
				objectPool.remove(key);
				offHeapStorage.update(key, converter.encode(value));
				return value;
			}
		});
	}

	/**
	 * Removes all of the elements from underlying and internal storages.
	 * This instance of ObjectPool can no longer be used after #clear() is called.
	 */
	@Override
	public void clear() {
		try {
			objectRelocator.interrupt();
			cleaner.interrupt();
			if (this.gcTimer!=null) {
				this.gcTimer.interrupt();
				this.gcTimer.join();
			}
			if (objectRelocator.isAlive()) objectRelocator.join();
			if (cleaner.isAlive()) cleaner.join();
			objectPool.clear();
			offHeapStorage.clear();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		init(type, offHeapStorage);
	}

	@Override
	public int size() {
		return syncStorage(new IntFn<Integer>() {
			@Override public Integer run() { return offHeapStorage.size(); }
		});
	}

	@Override
	public Set<K> keySet() {
		return syncStorage(new IntFn<Set<K>>() {
			@Override public Set<K> run() {
				return offHeapStorage.keySet();
			}
		});
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
	

	@Override
	public boolean contains(final E value) {
		return syncStorage(new IntFn<Boolean>() {
			@Override
			public Boolean run() {
				for (K key : offHeapStorage.keySet())
					if (converter.decode(offHeapStorage.get(key)).equals(value))
						return true;
				return false;
			}
		}).booleanValue();
	}

	interface Value<K> { K getKey(); }

}
