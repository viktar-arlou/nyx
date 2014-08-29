package nyx.collections.pool;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Basic implementation of a fixed-size object pool for keeping shared
 * resources.
 * 
 * @author varlou@gmail.com
 */
public class ResourcePool<T> {

	private BlockingQueue<T> pool;

	public ResourcePool(List<T> resources) {
		super();
		this.pool = new ArrayBlockingQueue<>(resources.size());
		this.pool.addAll(resources);
	}

	public T acquire() {
		return this.pool.poll();
	}

	public void release(T resource) {
		try {
			this.pool.put(resource);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
