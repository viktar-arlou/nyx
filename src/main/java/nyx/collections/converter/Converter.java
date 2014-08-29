package nyx.collections.converter;

public interface Converter<From,To> {

	To encode(From from);
	
	From decode(To to);
	
}
