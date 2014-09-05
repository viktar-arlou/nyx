package nyx.collections.pool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

import nyx.collections.Acme;

/**
 * Objects pool for Nyx Collection classes. This class is used by Nyx
 * Collections classes to ensure that only one instance of a collection element
 * created until it is recycled by the garbage collector.
 * 
 * @author varlou@gmail.com
 *
 * @param <K>
 *            key
 * @param <E>
 *            value
 */
public class ObjectPool<K, E> {

	private Map<K, WeakReference<E>> objectPool = Acme.chashmap();

	ReferenceQueue<E> rQueue = new ReferenceQueue<>();

	public E lookup(K key) {
		return objectPool.containsKey(key) ? objectPool.get(key).get() : null;
	}

	public E pool(K key, E value) {
		this.objectPool.put(key, new Value(key, value, rQueue));
		return value;
	}

	public E remove(K key) {
		return this.objectPool.remove(key).get();
	}

	@SuppressWarnings("unchecked")
	public void clean() {
		for (Value qe; (qe = (Value) rQueue.poll()) != null;)
			qe.cleanup();
	}

	class Value extends WeakReference<E> {

		private K key;

		public Value(K key, E referent) {
			super(referent);
			this.key = key;
		}

		public Value(K key, E referent, ReferenceQueue<? super E> q) {
			super(referent, q);
			this.key = key;
		}

		public void cleanup() {
			ObjectPool.this.objectPool.remove(key);
		}
	}

}
