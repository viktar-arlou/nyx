package nyx.collections.test;

import nyx.collections.vm.GCDetector;
import nyx.collections.vm.GCDetector.Callback;
import nyx.collections.vm.GCDetector.NotAvailable;

import org.junit.Test;

public class GCDetectorTest {

	volatile boolean passed = false;
	Object lock = new Object();

	@Test
	public void test() throws NotAvailable {
		// a local variable is a must, otherwise the callback object may be GC'ed.
		Callback<Void> cb = new Callback<Void>() {
			@Override
			public void handle(Void e) {
				passed = true;
			}
		};
		GCDetector.listen(cb);
		// generates garbage strings - should eventually cause GC to start.
		int i = 0;
		String s="";
		for (i = 0;!passed; i++) s += ""+i;
//		System.out.println("Finished at: " + i);
	}

}
