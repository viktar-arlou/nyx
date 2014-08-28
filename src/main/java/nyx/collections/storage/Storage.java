package nyx.collections.storage;

interface Storage<Key> {
	final class along2 {
		public long l1, l2;
	}
	Key append(Key key, byte[] addme);
	byte[] retrieve(Object id);
	void clear();
}
