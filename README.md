### Nyx

Nyx Collections is a Java library that offers implementation of standard Java Collections API which uses off-heap memory as a storage of collection elements. It is fully compatible with standard Java Collections API and designed to support an XL-size collections which make Nyx Collections library ideal for implementing memory and computationally intensive applications.

###Features
* Thread-safe high-performance implementation of List, Set, Map and Queue collections with data elements allocated in off-heap memory. 
* Specifically designed for XL size collections, ensures high throughput and memory conservation.
* Extended Nyx Collections API offers easy and efficient implementation of various computation scenarios with operations like #foreach, #filter, #mapTo.  

###Examples
```
/* Create Nyx List collection */
List<MyObject> nyx = new NyxList<>();

/* Filters out null elements */
List<Integer> list1 = Fn.on(list).filter(Fn.<Integer>notNull()).get();

/* Filters out elements outside given range */
list1 = Fn.on(list1).filter(Fn.<Integer>range(0, 5)).get();

/* A field of an anonymous class can be used to accumulate and return computation result */
int sum = Fn.on(list1).each().exec(new IFn<Integer, Void>() {
    int sum = 0;
    @Override
    public Void apply(Integer t) {
        sum += t;
        return null;
    }
}).counter;

```
### Requirements

Nyx can be used on Java 6 (or later) platform. Compatibility with Java 5 is foreseen in future releases. 

### License

* [LGPL](http://www.gnu.org/copyleft/lesser.html) 

### Releases

* v0.2
	- New extended API for easy Nyx collections traversal and modifications (foreach, mapTo, filter).
	- Asynchronous data exchange with off-heap storage for better throughput. 
* v0.1
	- Implementation of Java collections allocated in off-heap memory. 
	- Uses standard Java serialization mechanism to move data objects.
	- GC detection for Nyx Collections housekeeping facilities.

### Contact

* Viktar Arlou (varlou@gmail.com)
