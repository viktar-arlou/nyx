package nyx.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 *	Basic implementation of Set collection interface.
 * 
 * @author varlou@gmail.com
 */
public class NyxSet<E> extends NyxList<E> implements Set<E>, Serializable {

	private static final long serialVersionUID = -7388374589811129603L;

	@Override
	public boolean add(E e) {
		if (!super.contains(e)) {
			super.add(e);
			return true;
		} else return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for (E e : c) result |= add(e);
		return result;
	}
}
