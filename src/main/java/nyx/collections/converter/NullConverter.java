package nyx.collections.converter;

import nyx.collections.Const;

/**
 * Wrapper around base converter that handles <b>{@code null}</b> values.
 * 
 * @author varlou@gmail.com
 */
public class NullConverter<F,T> implements Converter<F, T> {

	private final Converter<F,T> baseConverter;
	
	public NullConverter(Converter<F, T> base) {
		this.baseConverter = base;
	}

	@Override
	public F decode(T data) {
		F decValue = this.baseConverter.decode(data);
		return (F) decValue != Const.<F> nil() ? decValue : null;
	}

	@Override
	public T encode(F e) {
		return this.baseConverter.encode(e != null ? e : Const.<F> nil());
	}

}
