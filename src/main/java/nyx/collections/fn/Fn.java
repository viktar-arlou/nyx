package nyx.collections.fn;

import java.util.Collection;
import java.util.Iterator;

import nyx.collections.NyxList;

public class Fn<E> implements ICollection<E> {

	private final Collection<E> collection;
	
	public Fn(Collection<E> collection) {
		this.collection = collection;
	}
	
	public static <T> Fn<T> on(Collection<T> collection) {
		return new Fn<T>(collection);
	}
	
	public static <E> IFn<E, Boolean> notNull() {
		return new IFn<E, Boolean>() {
			@Override public Boolean func(E t) { return t != null; }
		};
	}

	public static <E extends Comparable<E>> IFn<E, Boolean> range(final E from, final E to) {
		return new IFn<E, Boolean>() {
			@Override public Boolean func(E t) { return t.compareTo(from)>=0 && t.compareTo(to)<=0; }
		};
	}

	
	// Constants
	public static IFn<String,String> ToLowerCase = new IFn<String, String>() {
		@Override public String func(String t) { return t.toLowerCase(); }
	};
		
	public static IFn<String, String> ToUpperCase = new IFn<String, String>() {
		@Override public String func(String t) { return t.toLowerCase(); }
	};

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Collection<E>> T get() {
		return (T) this.collection;
	}

	@Override
	public <T extends IFn<E, Boolean>> ICollection<E> filter(T iFunc) {
		NyxList<E> filteredCollection = new NyxList<>();
		for (Iterator<E> iterator = collection.iterator(); iterator.hasNext();) {
			E element = (E) iterator.next();
			if (iFunc.func(element).booleanValue())
				filteredCollection.add(element);
		}
		return new Fn<E>(filteredCollection);
	}

	@Override
	public <Q, T extends IFn<E, Q>> ICollection<Q> mapTo(T iFunc) {
			NyxList<Q> mappedCollection = new NyxList<>();
			for (Iterator<E> iterator = collection.iterator(); iterator .hasNext();) 
				mappedCollection.add(iFunc.func((E) iterator.next()));
			return new Fn<Q>(mappedCollection);
	}
	
	@Override
	public <T extends IFn<E, ?>> T forEach(T iFunc) {
		for (Iterator<E> iterator = collection.iterator(); iterator.hasNext();) 
			iFunc.func((E) iterator.next());
		return iFunc;
	}

	public static abstract class NoRet<T> implements IFn<T, Void> {
		@Override public final Void func(T t) { func0(t); return null; }
		abstract public void func0(T t);
	}

}

