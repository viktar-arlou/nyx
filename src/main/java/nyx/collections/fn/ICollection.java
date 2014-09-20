package nyx.collections.fn;

import java.util.Collection;

public interface ICollection<E> {
	
	<T extends IFn<E,?>> T forEach(T iFunc);
	<Q, T extends IFn<E,Q>> ICollection<Q> mapTo(T iFunc);
	<T extends IFn<E,Boolean>> ICollection<E> filter(T iFunc);	
	<T extends Collection<E>> T get();
}
