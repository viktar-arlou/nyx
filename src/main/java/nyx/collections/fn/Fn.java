package nyx.collections.fn;

import java.util.Collection;
import java.util.Iterator;

import nyx.collections.NyxList;

public class Fn<T> implements ICollection<T> {

	private final Collection<T> collection;
	
	public Fn(Collection<T> collection) {
		this.collection = collection;
	}
	
	public static <T> Fn<T> on(Collection<T> collection) {
		return new Fn<T>(collection);
	}
	
	@Override
	public IEx<T> each() {
		return new IEx<T>() {

			@Override
			public <E extends IFn<T, ?>> E exec(E iFunc) {
				for (Iterator<T> iterator = collection.iterator(); iterator.hasNext();) 
					iFunc.apply((T) iterator.next());
				return iFunc;
			}

			@Override
			public <Q, E extends IFn<T, Q>> ICollection<Q> mapTo(E iFunc) {
				NyxList<Q> mappedCollection = new NyxList<>();
				for (Iterator<T> iterator = collection.iterator(); iterator .hasNext();) 
					mappedCollection.add(iFunc.apply((T) iterator.next()));
				return new Fn<Q>(mappedCollection);
			}};
	}
	
	public static <E> IFn<E, Boolean> notNull() {
		return new IFn<E, Boolean>() {
			@Override public Boolean apply(E t) { return t != null; }
		};
	}

	public static <E extends Comparable<E>> IFn<E, Boolean> range(final E from, final E to) {
		return new IFn<E, Boolean>() {
			@Override public Boolean apply(E t) { return t.compareTo(from)>=0 && t.compareTo(to)<=0; }
		};
	}

	
	// Constants
	public static IFn<String,String> ToLowerCase = new IFn<String, String>() {
		@Override public String apply(String t) { return t.toLowerCase(); }
	};
		
	public static IFn<String, String> ToUpperCase = new IFn<String, String>() {
		@Override public String apply(String t) { return t.toLowerCase(); }
	};

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Collection<T>> E get() {
		return (E) this.collection;
	}

	@Override
	public <E extends IFn<T, Boolean>> ICollection<T> filter(E iFunc) {
		NyxList<T> filteredCollection = new NyxList<>();
		for (Iterator<T> iterator = collection.iterator(); iterator.hasNext();) {
			T element = (T) iterator.next();
			if (iFunc.apply(element).booleanValue())
				filteredCollection.add(element);
		}
		return new Fn<T>(filteredCollection);
	}
	
}
