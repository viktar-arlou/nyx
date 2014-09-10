package nyx.collections.vm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import nyx.collections.Acme;

import com.sun.management.GarbageCollectionNotificationInfo;

/**
 * @author varlou@gmail.com
 */
public class GCDetector implements NotificationListener {

	private static boolean notAvailable = false;
	private Set<WeakReference<Callback<?>>> handlers = Acme.chashset();
	private ThreadPoolExecutor tp;
	private static final GCDetector inst = new GCDetector();
	
	
	private GCDetector() { 
		try {
			init();
			tp = new ThreadPoolExecutor(1, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
			tp.allowCoreThreadTimeOut(true);
		} catch (NotAvailable e) { notAvailable = true; } 
	}
	
	public static GCDetector listen(Callback<?> cb) throws NotAvailable {
		if (notAvailable) throw new NotAvailable();
		inst.handlers.add(new WeakReference<Callback<?>>(cb));
		return inst;
	}
	
	public void init() throws NotAvailable {
		List<GarbageCollectorMXBean> gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
		try {
			for (GarbageCollectorMXBean gcbean : gcbeans) ((NotificationEmitter) gcbean).addNotificationListener(this, null, null);
		} catch (ClassCastException e) {
			/** No GC notifications prior to java7 (u4?) */
			throw new NotAvailable();
		}
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))
//			System.err.println("Gc...");
			for (Iterator<WeakReference<Callback<?>>> it = this.handlers.iterator(); it.hasNext();) {
				final Callback<?> callback = it.next().get();
				// non-blocking call to subscribes
				if (callback != null)
					tp.execute(new Runnable() { @Override public void run() { callback.handle(null); } });
				else
					it.remove();
			}
	}

	/** Thrown to indicate that GCDetector feature is not available. JVM prior to Java 7u4 might be the reason. */
	public static class NotAvailable extends Exception { private static final long serialVersionUID = 2595119471777336192L; }
	
	public interface Callback<E> {
		void handle(E e);
	}
	
}
