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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nyx.collections.Acme;
import nyx.collections.KeyValue;
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
	private Queue<KeyValue<K,E>> storageQueue;
	private Lock storageLock = new ReentrantLock();
	private Condition sqProcess = storageLock.newCondition();
	private Condition sqEmpty = storageLock.newCondition();
	private Thread storageMover;
	
	private Type type;
	private ValueFactory vf;
	
	/* clean-up facility variables */
	private Thread cleaner;
	private volatile boolean stopWorkers = false;

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
		stopWorkers = false;
		try {
			GCDetector.listen(this);
		} catch (NotAvailable e) {
			// TODO: run worker thread for reference queue cleaning
		}
		startCleaner();
		startMover();		
	}

	private void startMover() {
		this.storageMover = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!stopWorkers)
					try {
						storageLock.lock();
						while (!stopWorkers && storageQueue.isEmpty())
							sqProcess.await();
						KeyValue<K, E> kv;
						while ((kv = storageQueue.poll())!=null)
							ObjectPool.this.offHeapStorage.create(kv.key, converter.encode(kv.value));
						sqEmpty.signalAll();
						storageLock.unlock();
					} catch (InterruptedException e) {
					}
			}
		});
		storageMover.setDaemon(true);
		storageMover.start();
	}

	private void startCleaner() {
		if (!isNone()) {
			this.cleaner = new Thread(new Runnable() {
				@Override public void run() {
					while (!stopWorkers) 
						try { 
							clean(); 
						} catch (InterruptedException e) { } }
				});
			cleaner.setDaemon(true);
			cleaner.start();
		}
	}

	private boolean isNone() {
		return this.type.equals(Type.NONE);
	}

	public <T> T syncStorage(IntFn<T> fn ) {
		try {
			this.storageLock.lock();
			if (!storageQueue.isEmpty())
				sqEmpty.await();
			return fn.run();
		} catch (InterruptedException e) {
		} finally { this.storageLock.unlock(); }
		return null;
	}
	
	@Override
	public E read(final K key) {
		return syncStorage(new IntFn<E>() {
			@Override
			public E run() {
				E res = objectPool.containsKey(key) ? objectPool.get(key).get() : null;
				if (res == null)
					objectPool.put(key,
							new WeakReference<>(res = converter
									.decode(ObjectPool.this.offHeapStorage
											.read(key))));
				return res;
			}
		});
	}

	@Override
	public E create(K key, E value) {
		if (!isNone()) this.objectPool.put(key, vf.make(key, value, rQueue));
		try {
			storageLock.lock();
			storageQueue.add(new KeyValue<>(key,value));
			sqProcess.signal();
		} finally { storageLock.unlock(); }
		return value;
	}

	@Override
	public E delete(final K key) {
		return syncStorage(new IntFn<E>() {
			@Override public E run() {
				E res = converter.decode(ObjectPool.this.offHeapStorage.delete(key));
				ObjectPool.this.objectPool.remove(key);
				return res;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public void clean() throws InterruptedException  {
		rqLock.lock();
		try {
			Value<K> qe = null;
			while ((qe = (Value<K>) rQueue.poll()) == null && !stopWorkers) notEmpty.await();
			do { objectPool.remove(qe.getKey()); } while ((qe = (Value<K>) rQueue.poll()) != null);
		} catch (Exception e) {
			/* NPE on qe==null is OK */
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
	public E update(final K key, final E value) {
		return syncStorage(new IntFn<E>() {
			@Override public E run() {
				objectPool.remove(key);
				offHeapStorage.update(key, converter.encode(value));
				return value;
			}
		});
	}

	@Override
	public void clear() {
		stopWorkers = true;
		try {
			storageLock.lock();
			sqProcess.signal();
			storageLock.unlock();
			if (storageMover.isAlive())
				storageMover.join();
			rqLock.lock();
			notEmpty.signal();
			rqLock.unlock();
			if (cleaner.isAlive())
				cleaner.join();
			objectPool.clear();
			offHeapStorage.clear();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			stopWorkers = false;
		}
		init(type, offHeapStorage);
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
	
	interface Value<K> { K getKey(); }
	
}
