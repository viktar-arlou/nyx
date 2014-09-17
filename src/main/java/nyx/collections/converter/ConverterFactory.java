package nyx.collections.converter;

public class ConverterFactory {

	public static Converter<?,byte[]> DEFAULT = new SerialConverter<>();
	
	/** Returns default converter. */
	public static <E> Converter<E,byte[]> get() {
		return new NullConverter<>(new SerialConverter<E>());
	}
	
	/** Returns best suited converter for a given class. */
	public static <E> Converter<E,byte[]> forClass(Class<E> cls) {
		return new SerialConverter<E>();
	}
	
	public static <F,T> Converter<F,T> forTypes(Class<F> from, Class<T> to) {
		return null;
	}

}
