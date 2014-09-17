package nyx.collections.fn;

public interface IEx<T> {

	<E extends IFn<T,?>> E exec(E iFunc);
	<Q, E extends IFn<T,Q>> ICollection<Q> mapTo(E iFunc);
	
}
