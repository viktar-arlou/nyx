### Nyx

Nyx is a Java library that offers implementation of standard Java Collections API which uses off-heap memory to store collection elements. It is fully compatible with standard Java Collections API and designed to support an XL-size collections which make Nyx ideal for implementing memory-intensive applications.
<p>Nyx can be used as a caching solution or to process large amounts of data without filling up the java heap. 	
<p>Starting from v0.2, Nyx library comes with an extended API based on the concept of fluent interfaces that allows to implement collections data traversal and manipulation in easy and concise manner with operations like #foreach, #filter, #mapTo etc.

###Examples
```
/* Filters out null elements */
List<Integer> list1 = Fn.on(list).filter(Fn.<Integer>notNull()).get();

/* A field of an anonymous class can be used to accumulate and retrieve
* computation result */
int sum = Fn.on(list1).each().exec(new IFn<Integer, Void>() {
    int sum = 0;
    @Override
    public Void apply(Integer t) {
        sum += t;
        return null;
    }
}).counter;
/* Filters out elements outside given range */
list1 = Fn.on(list1).filter(Fn.<Integer>range(0, 5)).get();
sum = Fn.on(list1).each().exec(new IFn<Integer, Void>() {
    int sum = 0;
    @Override
    public Void apply(Integer t) {
        sum += t;
        return null;
    }
}).sum;
```
### Requirements

Nyx can be used on Java 6 (or later) platform. Compatibility with Java 5 is foreseen in future releases. 

### License

* [LGPL](http://www.gnu.org/copyleft/lesser.html) 

### Releases

* v0.2
	- New API for easy Nyx collections traversal and modifications (foreach, mapTo, filter).
	- Asynchronous objects store and fetch operations for better throughput. 
* v0.1
	- Implementation of Java collections allocated in off-heap memory. 
	- Uses standard Java serialization mechanism to move data objects.

### Contact

* Viktar Arlou (varlou@gmail.com)
