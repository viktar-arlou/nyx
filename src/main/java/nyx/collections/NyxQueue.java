package nyx.collections;

import java.util.NoSuchElementException;
import java.util.Queue;

public class NyxQueue<E> extends NyxList<E> implements Queue<E> {

	private static final long serialVersionUID = -5865537730193466157L;

	@Override
	public boolean offer(E e) {
		return super.add(e);
	}

	@Override
	public E remove() {
		return super.remove(0);
	}

	@Override
	public E poll() {
		try {
			return super.remove(0);
		} catch (IndexOutOfBoundsException e) { return null; }
	}

	@Override
	public E element() {
		E res = poll();
		if (res==null) throw new NoSuchElementException();
		return res;
		
	}

	@Override
	public E peek() {
		try {
			return super.get(0);
		} catch (IndexOutOfBoundsException e) { return null; }
	}
}
