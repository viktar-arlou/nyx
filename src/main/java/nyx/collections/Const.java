package nyx.collections;

import java.io.Serializable;

/**
 * Various Nyx constants 
 * 
 * @author varlou@gmail.com
 *
 */
public class Const {

	public static int _1Mb = 1 << 20;
	public static int _1Kb = 1 << 10;

	@SuppressWarnings("unchecked")
	public static <E> E nil() { return (E) NULL.inst; }
	public static <E> E maskNull(E e) { return e!=null ? e : Const.<E>nil(); }

	/**
	 * This class represents a <b>{@code null}</b> value.
	 * It is implemented as serialization-proof singleton that can be used in 
	 * {@code ==} or {@code != } comparison operations.
	 * 
	 * @author varlou@gmail.com
	 * @param <T>
	 */
	public static class NULL<T> implements Serializable {
		private static final long serialVersionUID = 8697657103912545978L;
		static NULL<?> inst = new NULL<>();
		private NULL() { }
		/** Ensures that only one single instance remains after de/serialization. */
		private Object readResolve()  { return inst; }
	}
}
