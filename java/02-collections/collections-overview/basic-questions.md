# Basic Questions

## Question 1: What is the difference between `HashMap` and `ConcurrentHashMap`?

| Feature           | `HashMap`                                     | `ConcurrentHashMap`                                                  |
| :---------------- | :-------------------------------------------- | :------------------------------------------------------------------- |
| Thread-safe       | No                                            | Yes                                                                  |
| Concurrent reads  | Requires external synchronization when shared | Generally non-blocking                                               |
| Concurrent writes | Unsafe without synchronization                | Uses fine-grained coordination                                       |
| `null` keys       | Allows one                                    | Not allowed                                                          |
| `null` values     | Allows multiple                               | Not allowed                                                          |
| Iterator behavior | Usually fail-fast                             | Weakly consistent                                                    |
| Atomic operations | Limited                                       | Provides methods such as `putIfAbsent()`, `compute()`, and `merge()` |

`HashMap` is intended for single-threaded use or situations where synchronization is handled externally.

```java
Map<String, Integer> map = new HashMap<>();
```

Concurrent modification of the same `HashMap` without proper synchronization can result in lost updates, inconsistent data, or other undefined behavior.

`ConcurrentHashMap` is designed for concurrent applications:

```java
ConcurrentHashMap<String, Integer> map =
        new ConcurrentHashMap<>();

map.putIfAbsent("Java", 1);
map.computeIfPresent("Java", (key, value) -> value + 1);
```

In Java 8 and later, `ConcurrentHashMap` uses techniques such as CAS operations and synchronization on individual buckets rather than locking the entire map.

Its iterators are weakly consistent. They can safely operate while the map is being modified and do not throw `ConcurrentModificationException` merely because concurrent changes occur.

---

## Question 2: What is the difference between `HashSet` and `TreeSet`?

| Feature                           | `HashSet`                     | `TreeSet`                     |
| :-------------------------------- | :---------------------------- | :---------------------------- |
| Backing structure                 | `HashMap`                     | `TreeMap`                     |
| Ordering                          | No guaranteed iteration order | Sorted order                  |
| `add()`, `remove()`, `contains()` | Average `O(1)`                | `O(log n)`                    |
| Duplicate elements                | Not allowed                   | Not allowed                   |
| Equality mechanism                | `equals()` and `hashCode()`   | `compareTo()` or `Comparator` |
| `null` handling                   | Allows one `null`             | Usually rejects `null`        |
| Range operations                  | Not supported                 | Supported                     |

Use `HashSet` when fast membership testing is the priority.

```java
Set<Integer> numbers = new HashSet<>();
numbers.add(30);
numbers.add(10);
numbers.add(20);
```

Use `TreeSet` when elements must remain sorted or range operations are required.

```java
Set<Integer> numbers = new TreeSet<>();
numbers.add(30);
numbers.add(10);
numbers.add(20);

System.out.println(numbers);
// [10, 20, 30]
```

A `TreeSet` using natural ordering normally rejects `null` because `null` cannot be compared. A custom comparator could technically define how `null` should be ordered, but relying on this is generally discouraged.

---

## Question 3: Why does the Collections Framework not have one common interface for both `List` and `Map`?

`List` and `Map` represent fundamentally different abstractions.

A `List` stores individual elements in a sequence:

```java
list.add(element);
list.get(index);
list.remove(index);
```

A `Map` stores associations between keys and values:

```java
map.put(key, value);
map.get(key);
map.remove(key);
```

A common interface would require operations that do not make sense for both structures:

- `get(index)` is not meaningful for a general `Map`.
- `put(key, value)` is not meaningful for a `List`.
- A `Collection` represents individual elements.
- A `Map` represents key-value associations.

Therefore, `List`, `Set`, and `Queue` extend `Collection`, while `Map` remains a separate hierarchy.

However, a map provides collection views through:

```java
map.keySet();
map.values();
map.entrySet();
```

---

## Question 4: Why do Java collections not directly support primitive types?

Java generic collections work with reference types.

This is valid:

```java
List<Integer> numbers = new ArrayList<>();
```

This is not valid:

```java
List<int> numbers; // Compilation error
```

Java generics historically use type erasure, where generic type information is largely represented using reference types at runtime. Primitive values such as `int`, `long`, and `double` are not objects.

Java solves this using wrapper classes and autoboxing:

```java
List<Integer> numbers = new ArrayList<>();
numbers.add(10);       // int is boxed into Integer
int value = numbers.get(0); // Integer is unboxed into int
```

Boxing may introduce:

- Additional memory usage
- Object creation or wrapper reuse
- Unboxing overhead
- Possible `NullPointerException` during unboxing
- Reduced performance in large numerical workloads

Specialized libraries such as Eclipse Collections provide primitive collections such as `IntList` to reduce boxing overhead.

---

## Question 5: Can you create your own collection, and which methods are mandatory?

Yes. A custom collection can be created by implementing a collection interface or extending an abstract collection class.

### Implementing `Collection` directly

Implementing `Collection<E>` directly requires implementing many methods, including:

```java
size();
isEmpty();
contains();
iterator();
toArray();
add();
remove();
containsAll();
addAll();
removeAll();
retainAll();
clear();
```

Some modification methods may throw `UnsupportedOperationException` when the collection is intentionally immutable.

### Extending `AbstractCollection`

A simpler approach is to extend `AbstractCollection<E>`. At minimum, a basic implementation generally needs:

```java
public Iterator<E> iterator();
public int size();
```

To support insertion, `add()` should also be overridden.

```java
public class CustomCollection<E>
        extends AbstractCollection<E> {

    @Override
    public Iterator<E> iterator() {
        // Return an iterator
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return 0;
    }
}
```

Other useful abstract base classes include:

- `AbstractList`
- `AbstractSet`
- `AbstractQueue`
- `AbstractMap`

For example, a read-only random-access list extending `AbstractList` normally needs to implement `get(index)` and `size()`.

---

## Question 6: What is `Collection`?

`Collection` is an interface in the `java.util` package.

It is the root interface for collections that represent groups of individual elements.

Important child interfaces include:

- `List`
- `Set`
- `Queue`
- `Deque`

```java
Collection<String> values = new ArrayList<>();
values.add("Java");
values.add("Go");
```

`Map` does not extend `Collection` because it stores key-value associations rather than individual elements.

Do not confuse the following:

- `Collection` is an interface.
- `Collections` is a utility class containing static methods.

Examples of `Collections` utility methods include:

```java
Collections.sort(list);
Collections.reverse(list);
Collections.unmodifiableList(list);
Collections.synchronizedList(list);
```

---

## Question 7: What is `TreeMap`?

`TreeMap` is a sorted map implementation backed by a red-black tree.

```java
Map<Integer, String> map = new TreeMap<>();

map.put(30, "C");
map.put(10, "A");
map.put(20, "B");

System.out.println(map);
// {10=A, 20=B, 30=C}
```

Its main characteristics are:

- Keys are unique.
- Values may be duplicated.
- Entries are sorted by key.
- Natural ordering or a custom `Comparator` may be used.
- `get()`, `put()`, and `remove()` take `O(log n)` time.
- Multiple `null` values are allowed.
- `null` keys are normally not allowed.

When natural ordering is used, keys must implement `Comparable` and must be mutually comparable.

```java
Map<String, Integer> map = new TreeMap<>();
```

When a custom comparator is supplied, keys must be comparable according to that comparator:

```java
Map<String, Integer> map =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
```

A custom comparator can technically support heterogeneous or `null` keys, but it must be capable of consistently comparing every inserted key.

`TreeMap` also supports navigation and range operations:

```java
firstKey();
lastKey();
floorKey(key);
ceilingKey(key);
headMap(key);
tailMap(key);
subMap(fromKey, toKey);
```

---

## Question 8: What is the difference between `Enumeration`, `Iterator`, and `ListIterator`?

These interfaces are used to traverse collection elements.

| Feature          | `Enumeration`      | `Iterator`          | `ListIterator`         |
| :--------------- | :----------------- | :------------------ | :--------------------- |
| Introduced for   | Legacy collections | General collections | Lists                  |
| Direction        | Forward only       | Forward only        | Forward and backward   |
| Read elements    | Yes                | Yes                 | Yes                    |
| Remove elements  | No                 | Yes, when supported | Yes                    |
| Add elements     | No                 | No                  | Yes                    |
| Replace elements | No                 | No                  | Yes                    |
| Applicable to    | Legacy classes     | Most collections    | `List` implementations |

### Enumeration

`Enumeration` is mainly used with legacy classes such as `Vector` and `Hashtable`.

```java
Vector<String> vector = new Vector<>();
vector.add("Java");
vector.add("Go");

Enumeration<String> enumeration = vector.elements();

while (enumeration.hasMoreElements()) {
    System.out.println(enumeration.nextElement());
}
```

Its primary methods are:

```java
boolean hasMoreElements();
E nextElement();
```

### Iterator

`Iterator` is the general traversal mechanism for collections.

```java
Iterator<String> iterator = list.iterator();

while (iterator.hasNext()) {
    String value = iterator.next();

    if (value.isEmpty()) {
        iterator.remove();
    }
}
```

Its primary methods are:

```java
boolean hasNext();
E next();
void remove();
```

`remove()` is optional and may throw `UnsupportedOperationException`.

### ListIterator

`ListIterator` is available only for `List` implementations.

```java
ListIterator<String> iterator = list.listIterator();
```

It can move in both directions:

```java
iterator.hasNext();
iterator.next();

iterator.hasPrevious();
iterator.previous();
```

It also supports:

```java
iterator.add(element);
iterator.set(element);
iterator.remove();
```

---

## Question 9: Can an `ArrayList` store `null` multiple times?

Yes. An `ArrayList` can contain any number of `null` elements.

```java
List<String> list = new ArrayList<>();

list.add(null);
list.add("Java");
list.add(null);
list.add(null);

System.out.println(list);
// [null, Java, null, null]
```

Methods such as `contains(null)`, `indexOf(null)`, and `lastIndexOf(null)` handle `null` correctly.

```java
System.out.println(list.contains(null));     // true
System.out.println(list.indexOf(null));      // 0
System.out.println(list.lastIndexOf(null));  // 3
```

---

## Question 10: Why is `LinkedList` slower for search operations?

`LinkedList` is implemented as a doubly linked list.

Each node stores:

- The element
- A reference to the previous node
- A reference to the next node

It does not provide direct array-style access.

```java
list.get(index);
```

To retrieve an element, `LinkedList` must traverse nodes from the head or tail. It starts from whichever end is closer, but the operation is still `O(n)`.

By comparison, `ArrayList` uses an array:

```java
array[index];
```

Therefore, indexed access in `ArrayList` is `O(1)`.

`LinkedList` may also perform poorly because:

- Nodes are separately allocated.
- Nodes may be scattered throughout heap memory.
- It has poor CPU cache locality.
- Each node requires additional memory for links.
- Traversal requires pointer chasing.

As a result, `LinkedList` is often slower than `ArrayList` for searching and indexed access.

---

## Question 11: What are the use cases of `ArrayList` and `LinkedList`?

### Use `ArrayList` when:

- Fast indexed access is required.
- Iteration is frequent.
- Most additions occur at the end.
- Reads greatly outnumber middle insertions and removals.
- A general-purpose `List` is required.

```java
List<String> names = new ArrayList<>();
```

### Use `LinkedList` when:

- It is specifically needed as both a `List` and a `Deque`.
- Frequent additions or removals occur at the beginning or end.
- Direct references or iterators already identify insertion positions.

```java
Deque<String> deque = new LinkedList<>();

deque.addFirst("A");
deque.addLast("B");
deque.removeFirst();
```

In many queue and deque scenarios, `ArrayDeque` is preferable to `LinkedList` because it usually has better memory locality and lower allocation overhead.

For most general-purpose list requirements, prefer `ArrayList`.

---

## Question 12: Does `Set` allow `null` values?

It depends on the implementation.

| Implementation          | `null` support          |
| :---------------------- | :---------------------- |
| `HashSet`               | Allows one `null`       |
| `LinkedHashSet`         | Allows one `null`       |
| `TreeSet`               | Normally rejects `null` |
| `ConcurrentSkipListSet` | Rejects `null`          |
| `CopyOnWriteArraySet`   | Allows one `null`       |
| `Set.of(...)`           | Rejects `null`          |

A set cannot contain duplicate elements, so implementations that support `null` can contain it only once.

```java
Set<String> set = new HashSet<>();

set.add(null);
set.add(null);

System.out.println(set.size()); // 1
```

A `TreeSet` using natural ordering rejects `null` because it cannot compare `null` with other elements. A custom comparator could define null ordering, although this is not commonly recommended.

---

## Question 13: Why is there no `get()` method in `Set`?

A `Set` represents unique elements, not positions or key-value associations.

A `List` supports retrieval by index:

```java
list.get(0);
```

A `Map` supports retrieval by key:

```java
map.get(key);
```

A `Set` has neither indexes nor separate keys. Its primary purpose is membership testing:

```java
set.contains(element);
```

For that reason, a general `get()` operation would be ambiguous:

- Get by index?
- Get by hash?
- Get an arbitrary matching object?

Elements can be accessed by iteration:

```java
for (String value : set) {
    System.out.println(value);
}
```

When the stored canonical object must be retrieved using an equal lookup object, a `Map<T, T>` or a key-to-object map may be more suitable.

---

## Question 14: What is the difference between `Comparable` and `Comparator`?

| Feature             | `Comparable`             | `Comparator`                 |
| :------------------ | :----------------------- | :--------------------------- |
| Package             | `java.lang`              | `java.util`                  |
| Method              | `compareTo(T other)`     | `compare(T first, T second)` |
| Defines             | Natural order            | External/custom order        |
| Implemented by      | The class being compared | A separate object or lambda  |
| Number of orderings | Usually one              | Multiple possible            |

### Comparable

A class implements `Comparable` to define its natural order.

```java
public class Student implements Comparable<Student> {

    private int id;

    @Override
    public int compareTo(Student other) {
        return Integer.compare(this.id, other.id);
    }
}
```

Usage:

```java
Collections.sort(students);
```

### Comparator

A comparator defines an external ordering.

```java
Comparator<Student> byName =
        Comparator.comparing(Student::getName);
```

Usage:

```java
students.sort(byName);
```

Multiple comparators can be created:

```java
Comparator<Student> byAge =
        Comparator.comparingInt(Student::getAge);

Comparator<Student> byNameThenAge =
        Comparator.comparing(Student::getName)
                  .thenComparingInt(Student::getAge);
```

A comparison result should be:

- Negative when the first object comes before the second
- Zero when they have equal ordering
- Positive when the first object comes after the second

For sorted sets and maps, comparison consistency with `equals()` is strongly recommended.

---

## Question 15: When should immutable or unmodifiable collections be used?

Immutable or unmodifiable collections should be used when callers must not modify the collection through the provided reference.

Common use cases include:

- Fixed configuration values
- Constants
- Defensive API return values
- Shared read-only data
- Preventing accidental modification
- Simplifying concurrent access

### `List.of()`

`List.of()` creates an immutable list.

```java
List<String> roles =
        List.of("ADMIN", "USER", "AUDITOR");
```

It:

- Rejects modification operations
- Rejects `null` elements
- Does not depend on an externally mutable source list

### `Collections.unmodifiableList()`

This creates an unmodifiable view of an existing list.

```java
List<String> source = new ArrayList<>();
source.add("Java");

List<String> view =
        Collections.unmodifiableList(source);
```

The view cannot be changed directly:

```java
view.add("Go"); // UnsupportedOperationException
```

However, changes made through the original list are visible:

```java
source.add("Go");

System.out.println(view);
// [Java, Go]
```

Therefore, it is unmodifiable through the wrapper, but it is not necessarily immutable.

### `List.copyOf()`

Use `List.copyOf()` when an immutable snapshot is required:

```java
List<String> snapshot = List.copyOf(source);
```

Later modifications to `source` do not affect `snapshot`.

---

## Question 16: What is the difference between `map()` and `flatMap()`?

Both methods transform stream elements.

### `map()`

`map()` transforms each input element into one output element.

```java
List<String> names =
        List.of("alice", "bob");

List<String> upper =
        names.stream()
             .map(String::toUpperCase)
             .toList();
```

Result:

```text
[ALICE, BOB]
```

Conceptually:

```text
T -> R
```

### `flatMap()`

`flatMap()` transforms each input element into a stream of zero or more elements and then combines those streams into one stream.

```java
List<List<String>> groups = List.of(
        List.of("A", "B"),
        List.of("C", "D")
);

List<String> values =
        groups.stream()
              .flatMap(List::stream)
              .toList();
```

Result:

```text
[A, B, C, D]
```

Conceptually:

```text
T -> Stream<R>
```

followed by flattening:

```text
Stream<Stream<R>> -> Stream<R>
```

Another example:

```java
List<String> words =
        List.of("Java Stream", "Flat Map");

List<String> tokens =
        words.stream()
             .flatMap(text ->
                     Arrays.stream(text.split(" ")))
             .toList();
```

Result:

```text
[Java, Stream, Flat, Map]
```

---

## Question 17: What is the difference between minor, major, and full garbage collection?

These terms describe different types of JVM garbage collection, but their exact meaning can vary between garbage collectors.

### Minor GC

A minor collection primarily collects the young generation.

The young generation commonly contains:

- Eden
- Survivor spaces

Most newly created objects are allocated there. Because many objects die young, minor collections are usually relatively frequent and fast.

Surviving objects may be:

- Moved between survivor spaces
- Promoted to the old generation

### Major GC

A major collection generally refers to collecting the old generation.

However, the term is not defined consistently across every collector and monitoring tool. In some contexts, “major GC” is used loosely to mean a costly old-generation or whole-heap collection.

### Full GC

A full collection generally examines most or all of the Java heap and may also process additional JVM structures such as class metadata.

It is usually more expensive than a young-generation collection and commonly causes a longer stop-the-world pause.

### Important distinction

The exact behavior depends on the collector:

- Serial GC
- Parallel GC
- G1 GC
- ZGC
- Shenandoah GC

For example, modern concurrent collectors may perform much of their work concurrently rather than in one long pause.

Therefore, production analysis should rely on the actual GC collector, event name, and GC logs rather than only the labels “minor,” “major,” or “full.”

---

## Question 18: How do you set and get a thread's name?

Every Java thread has a name.

The JVM assigns a default name when one is not explicitly provided:

```text
Thread-0
Thread-1
Thread-2
```

Use `setName()` to change a thread's name:

```java
Thread thread = new Thread(() -> {
    System.out.println(
            Thread.currentThread().getName()
    );
});

thread.setName("order-processor");
thread.start();
```

Use `getName()` to retrieve it:

```java
String name = thread.getName();
```

A name can also be supplied through a constructor:

```java
Thread thread =
        new Thread(task, "payment-worker");
```

Thread names are useful for:

- Log analysis
- Debugging
- Thread dumps
- Monitoring
- Identifying executor workloads

Thread names do not affect scheduling priority or execution order.

---

## Question 19: What is `ResultSetMetaData`?

`ResultSetMetaData` is an interface in the `java.sql` package.

It provides information about the columns returned by a SQL query.

```java
ResultSet resultSet =
        statement.executeQuery(
                "SELECT id, name, salary FROM employee"
        );

ResultSetMetaData metadata =
        resultSet.getMetaData();
```

Common methods include:

```java
int columnCount = metadata.getColumnCount();

String name = metadata.getColumnName(1);
String label = metadata.getColumnLabel(1);
int sqlType = metadata.getColumnType(1);
String typeName = metadata.getColumnTypeName(1);
int precision = metadata.getPrecision(1);
int scale = metadata.getScale(1);
boolean nullable =
        metadata.isNullable(1)
        != ResultSetMetaData.columnNoNulls;
```

Column indexes begin at `1`, not `0`.

Example:

```java
int count = metadata.getColumnCount();

for (int i = 1; i <= count; i++) {
    System.out.println(
            metadata.getColumnLabel(i)
                    + " - "
                    + metadata.getColumnTypeName(i)
    );
}
```

`ResultSetMetaData` describes the query result, which may include aliases, expressions, or joined columns. It does not necessarily describe every column in the underlying database table.
