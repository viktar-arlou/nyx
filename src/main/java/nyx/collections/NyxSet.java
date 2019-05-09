package nyx.collections;

import java.io.Serializable;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 *	Basic implementation of Set collection interface.
 * 
 * @author varlou@gmail.com
 */
public class NyxSet<E> implements Set<E>, Serializable {

	private static final long serialVersionUID = -7388374589811129603L;

	private final NyxList<E> internList = new NyxList<>();

	@Override
	public boolean add(E e) {
		if (!internList.contains(e)) {
			internList.add(e);
			return true;
		} else return false;
	}

	@Override
	public boolean remove(Object obj) {
		return internList.remove(obj);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return internList.containsAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return internList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return internList.retainAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for (E e : c) result |= add(e);
		return result;
	}

	public void add(int index, E element) {
		internList.add(index, element);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		return internList.addAll(index, c);
	}

	public E get(int index) {
		return internList.get(index);
	}

	public E set(int index, E element) {
		return internList.set(index, element);
	}

	public E remove(int index) {
		return internList.remove(index);
	}

	public int indexOf(Object o) {
		return internList.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return internList.lastIndexOf(o);
	}

	public ListIterator<E> listIterator() {
		return internList.listIterator();
	}

	public ListIterator<E> listIterator(int index) {
		return internList.listIterator(index);
	}

	public List<E> subList(int fromIndex, int toIndex) {
		return internList.subList(fromIndex, toIndex);
	}

	@Override
	public int hashCode() {
		return internList.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return internList.equals(o);
	}

	public void replaceAll(UnaryOperator<E> operator) {
		internList.replaceAll(operator);
	}

	public void sort(Comparator<? super E> c) {
		internList.sort(c);
	}

	@Override
	public void clear() {
		internList.clear();
	}

	@Override
	public int size() {
		return internList.size();
	}

	@Override
	public boolean isEmpty() {
		return internList.isEmpty();
	}

	@Override
	public boolean contains(Object obj) {
		return internList.contains(obj);
	}

	@Override
	public Iterator<E> iterator() {
		return internList.iterator();
	}

	@Override
	public Object[] toArray() {
		return internList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return internList.toArray(a);
	}
}
