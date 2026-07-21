# Basic Questions

## Question 1: `HashMap` vs `ConcurrentHashMap`?

`HashMap` is not thread-safe. If multiple threads modify it concurrently, it can lead to data inconsistency or infinite loops.
`ConcurrentHashMap` is thread-safe and designed for high-throughput concurrent applications.
Key differences:
- `ConcurrentHashMap` provides lock-free reads and fine-grained bucket-level locks for writes, whereas `HashMap` requires external synchronization.
- `ConcurrentHashMap` iterators are weakly consistent and never throw `ConcurrentModificationException`.
- `HashMap` allows one `null` key and multiple `null` values. `ConcurrentHashMap` does not allow `null` keys or values to avoid ambiguity in concurrent environments.

## Question 2: `HashSet` vs `TreeSet`?

- **HashSet:** Backed by a `HashMap`. Operations like add, remove, and contains are O(1) on average. It does not maintain insertion or sorted order. It allows exactly one `null` value. Use when you only need fast membership testing.
- **TreeSet:** Backed by a `TreeMap` (Red-Black Tree). Operations like add, remove, and contains are O(log n). It maintains sorted order and supports range queries (headSet, tailSet, etc.). It does not allow `null` values because elements must be comparable. Use when you need sorted iteration.

## Question 3: Why doesn't the Collection framework have a common interface for both List and Map?

List and Map represent fundamentally different data structures with incompatible contracts:
- `List` is a sequence of individual elements indexed by position. Its core operations are `add(element)`, `get(index)`, `remove(index)`.
- `Map` stores key-value pairs where each key maps to exactly one value. Its core operations are `put(key, value)`, `get(key)`, `remove(key)`.
A common interface would be forced to include methods that don't make sense for both — for example, `get(int index)` is meaningless for a `Map`, and `put(key, value)` is meaningless for a `List`.
The `Collection` interface was designed to be the root for element-based collections (`List`, `Set`, `Queue`). `Map` is architecturally separate because it deals with associations, not individual elements. Merging them would violate the Interface Segregation Principle and make the API confusing and inconsistent.

## Question 4: What problem would arise if collections allowed primitive types?

Java generics are implemented via type erasure — at runtime, generic type parameters are erased to `Object`. Primitive types (`int`, `long`, `double`, etc.) are not objects and cannot be stored where `Object` is expected.
- Storing primitives would require special JVM-level handling that generics do not support.
- Every operation like `add()`, `get()`, `remove()` would need overloaded versions for each primitive type, causing API explosion.
- Java solves this with autoboxing/unboxing — automatically converting `int` to `Integer` and back. This works transparently but comes with a performance cost due to object allocation.

*Note: Libraries like Eclipse Collections and Koloboke provide primitive collections to avoid boxing overhead in performance-critical code.*

## Question 5: Can you design your own collection and what methods are mandatory?

Yes. To create a custom collection that integrates with the Java Collections Framework, you typically implement the `Collection` interface or extend abstract classes like `AbstractList`, `AbstractSet`, etc. Mandatory methods include size(), isEmpty(), iterator(), add(), remove(), contains(), etc., depending on the specific interface being implemented.

## Question 6: What is Collection?

Collection is an interface which is present in `java.util` package.
It is a root interface for the entire collection framework.
If we want to represent a group of individual objects in a single entity then we need to use Collections.

## Question 7: What is TreeMap?

The underlying data structure is a RED BLACK TREE.
Duplicate keys are not allowed but values can be duplicated.
Insertion order is not preserved. All entries will store in the sorting order of keys.
If it depends upon natural sorting order then keys must be homogeneous and Comparable.
If it depends upon customized sorting order then keys can be heterogeneous and non-comparable.
For an empty TreeMap, if we insert NULL as key then we will get NullPointerException.
After insertion of elements, if we try to insert NULL as key then we will get NullPointerException.
But there is no restriction on NULL values.

## Question 8: Explain Enumeration vs Iterator vs ListIterator

Types of Cursors in java:
Cursors are used to retrieve the objects one by one from Collection. We have three types of cursors in java.

1) **Enumeration**
Enumeration interface present in `java.util` package.
It is used to read objects one by one from Legacy Collection objects.
Enumeration object can be created by using `elements()` method.
ex: `Enumeration e = v.elements();`
Enumeration interface contains following two methods:
`public boolean hasMoreElements();`
`public Object nextElement();`

2) **Iterator**
It is used to retrieve the objects one by one from any Collection object. Hence it is known as a universal cursor.
Using Iterator interface we can perform read and remove operations.
We can create Iterator object by using `iterator()` method.
ex: `Iterator itr = al.iterator();`
Iterator interface contains following three methods:
`public boolean hasNext();`
`public Object next();`
`public void remove();`

3) **ListIterator**
It is a child interface of Iterator interface.
It is used to read objects one by one from List Collection objects.
Using ListIterator we can read objects in forward direction and backward direction. Hence it is a bi-directional cursor.
Using ListIterator we can perform read, remove, adding and replacement of new objects.
We can create ListIterator interface by using `listIterator()` method.
ex: `ListIterator litr = al.listIterator();`

## Question 9: Can ArrayList store null multiple times?

Yes. `ArrayList` uses a plain `Object[]` array and has no restriction on `null` values. You can add `null` as many times as you want:
```java
list.add(null);
list.add(null);
list.add(null); // perfectly valid
```
Methods like `contains(null)` and `indexOf(null)` work correctly and use `null` checks instead of `equals()` when searching.

## Question 10: Why is LinkedList slower for search operations?

`LinkedList` is a doubly-linked list — each node stores a reference to the previous and next node. There is no direct array-style index access.
- `get(index)` is O(n) — the list must traverse from the head or tail (Java optimizes by starting from the closer end) node by node.
- Memory layout is non-contiguous. CPU cache is ineffective because each node can be anywhere in heap memory, causing cache misses.
- `ArrayList`'s contiguous memory layout is cache-friendly and gets hardware prefetching benefits.
In practice, `LinkedList` is often slower than `ArrayList` even for its supposed strength (middle insertions) because the traversal to find the position is O(n).

## Question 11: What are the use cases of ArrayList and LinkedList?

**ArrayList use cases:**
- Frequent random access by index (e.g., `get(i)` in loops).
- Read-heavy workloads where the list is rarely modified.
- As a general-purpose list — it outperforms `LinkedList` in most real-world scenarios.

**LinkedList use cases:**
- Implementing a Queue or Deque — `addFirst`/`removeFirst` are O(1).
- When you frequently add/remove from both ends (head/tail).
- Implementing an LRU cache manually (though `LinkedHashMap` is easier).

Modern recommendation: Prefer `ArrayList` by default. Only use `LinkedList` when you specifically need Deque behavior.

## Question 12: Does Set allow null values?

It depends on the implementation:
- `HashSet`: allows exactly one `null` value (`null` hashes to bucket 0).
- `LinkedHashSet`: allows exactly one `null` value.
- `TreeSet`: does NOT allow `null` — `null` cannot be compared using `compareTo()` or `Comparator`, causing a `NullPointerException`.

## Question 13: Why is there no get method in Set?

`Set` is an unordered collection (conceptually). The purpose of a `Set` is to answer membership queries: 'Is this element in the set?' — answered by `contains()`. Providing a `get()` method would require defining what 'getting' means from an unordered collection — by index? By some key? Neither makes sense for a `Set`. If you need to retrieve the actual object stored in a `Set` (e.g., to get a cached instance), use a `Map<T,T>` instead, where keys and values are the same object.

## Question 14: Difference between Comparator and Comparable?

- **Comparable (`java.lang.Comparable`)**: Defines the default or natural sorting order for a class. It contains a single method `compareTo(T obj)`. A class implementing this interface defines how its instances are compared to one another.
- **Comparator (`java.util.Comparator`)**: Defines an external or custom sorting order. It contains the method `compare(T obj1, T obj2)`. It is useful when you want to sort objects in different ways or when you cannot modify the class to implement `Comparable`.

## Question 15: When should immutable collections (`List.of()`, `Collections.unmodifiableList()`) be used?

Immutable collections should be used when you want to ensure that the collection's contents cannot be modified after creation. This is useful for thread safety, providing defensive copies of internal states, or representing fixed data sets like configurations or predefined lists of values.

## Question 16: `map()` vs `flatMap()`?

- `map()`: Transforms each element of a stream into exactly one other element. It is a 1-to-1 mapping.
- `flatMap()`: Transforms each element of a stream into a stream of other elements, and then flattens the resulting streams into a single stream. It is a 1-to-N mapping.

## Question 17: What is Setting and Getting Name of a thread?

In java, every thread has a name automatically generated by JVM or explicitly provided by the programmer.
We have following methods to set and get name of a thread.
ex: `public final void setName(String name)`
`public final String getName()`

## Question 18: What is ResultSetMetaData?

`ResultSetMetaData` is an interface which is present in `java.sql` package.
It provides metadata of a table, giving information about number of columns, datatype of a column, size of a column, etc.
We can create `ResultSetMetaData` object by using `getMetaData()` method of `ResultSet` obj.
ex: `ResultSetMetaData rsmd = rs.getMetaData();`

