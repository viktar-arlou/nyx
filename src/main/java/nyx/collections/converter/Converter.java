package nyx.collections.converter;

/**
 * Converter interface.
 * 
 * @author varlou@gmail.com
 */
public interface Converter<F,T> {
	T encode(F from);
	F decode(T to);
}
