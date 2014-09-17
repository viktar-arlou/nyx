package nyx.collections.fn;

import java.util.Collection;

public interface ICollection<E> {
	
	IEx<E> each();
	<T extends IFn<E,Boolean>> ICollection<E> filter(T iFunc);	
	<T extends Collection<E>> T get();
}
