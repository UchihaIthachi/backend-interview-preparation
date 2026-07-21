# Advanced Questions

## Question 1: What is a `HashMap`, and how does it work internally?

A `HashMap` stores data as key-value pairs using a hash-table-based structure.

Internally, it maintains an array of buckets. Each bucket may contain:

* No entry
* A single node
* A linked list of nodes
* A red-black tree when many collisions occur

When `put(key, value)` is called:

1. `HashMap` calculates a hash value from the key's `hashCode()`.
2. It determines the bucket index using the table size and the calculated hash.
3. If the bucket is empty, a new node is inserted.
4. If the bucket already contains entries, `HashMap` compares keys using `equals()`.
5. If an equal key exists, its value is replaced.
6. Otherwise, a new entry is added to the bucket.

In Java 8 and later, a long collision chain may be converted into a red-black tree when:

* The bucket contains at least eight entries.
* The table capacity is at least 64.

This can improve worst-case lookup time from `O(n)` to `O(log n)` for heavily collided buckets.

A `HashMap` normally uses:

* Default initial capacity: `16`
* Default load factor: `0.75`

When the number of entries exceeds `capacity × loadFactor`, the table is resized, normally by doubling its capacity.

### Time complexity

| Operation  | Average case | Worst case                                 |
| :--------- | :----------- | :----------------------------------------- |
| `get()`    | `O(1)`       | `O(n)` or `O(log n)` for treeified buckets |
| `put()`    | `O(1)`       | `O(n)` or `O(log n)`                       |
| `remove()` | `O(1)`       | `O(n)` or `O(log n)`                       |

Correct implementations of `equals()` and `hashCode()` are essential when custom objects are used as keys.

---

## Question 2: Explain the internal working of `ArrayList`

`ArrayList` is backed by a dynamically resized array, internally represented by an `Object[]`.

When an `ArrayList` is created using the default constructor, it initially uses an empty internal array. On the first insertion, its capacity normally becomes 10.

```java
List<String> names = new ArrayList<>();
names.add("Alice");
```

When the internal array becomes full:

1. A larger array is created.
2. The capacity normally grows by approximately 50%.
3. Existing elements are copied into the new array.
4. The new element is inserted.

The growth calculation is approximately:

```java
newCapacity = oldCapacity + (oldCapacity >> 1);
```

### Time complexity

| Operation           | Complexity       | Reason                                    |
| :------------------ | :--------------- | :---------------------------------------- |
| `get(index)`        | `O(1)`           | Direct array access                       |
| `set(index, value)` | `O(1)`           | Direct array access                       |
| `add(value)`        | Amortized `O(1)` | Resizing occasionally requires copying    |
| `add(index, value)` | `O(n)`           | Elements may need to be shifted           |
| `remove(index)`     | `O(n)`           | Remaining elements may need to be shifted |
| `contains(value)`   | `O(n)`           | Linear search                             |
| `indexOf(value)`    | `O(n)`           | Linear search                             |

`ArrayList` is generally a good choice when:

* Fast indexed access is required.
* Most additions occur at the end.
* Insertions and removals from the middle are uncommon.

It is not thread-safe.

---

## Question 3: How does a `Set` ensure uniqueness?

A `Set` determines uniqueness using the equality rules of its implementation.

For a `HashSet`, uniqueness is determined using `hashCode()` and `equals()`.

When `set.add(object)` is called:

1. The object's hash code is calculated.
2. The corresponding bucket is identified.
3. Existing objects in that bucket are compared using `equals()`.
4. If an equal object already exists, the new object is not added.
5. Otherwise, the object is inserted.

```java
Set<String> values = new HashSet<>();

values.add("Java");
values.add("Java");

System.out.println(values.size()); // 1
```

For custom objects, `equals()` and `hashCode()` must be implemented consistently.

```java
@Override
public boolean equals(Object object) {
    // Equality logic
}

@Override
public int hashCode() {
    // Hash calculation
}
```

The contract requires that objects considered equal by `equals()` must return the same hash code.

A `TreeSet` determines uniqueness using natural ordering or a supplied `Comparator`. Two elements are considered duplicates when their comparison result is zero.

---

## Question 4: How does `LinkedHashSet` maintain insertion order?

`LinkedHashSet` is implemented using a `LinkedHashMap`.

The backing structure combines:

* A hash table for fast lookup
* A doubly linked list connecting entries in insertion order

Each entry stores links to the entry inserted before it and the entry inserted after it.

This allows `LinkedHashSet` to provide:

* Average `O(1)` insertion
* Average `O(1)` removal
* Average `O(1)` lookup
* Predictable insertion-order iteration

```java
Set<String> languages = new LinkedHashSet<>();

languages.add("Java");
languages.add("Go");
languages.add("Python");

System.out.println(languages);
// [Java, Go, Python]
```

The main trade-off compared with `HashSet` is slightly higher memory usage.

---

## Question 5: Why is `TreeSet` usually slower than `HashSet`?

`HashSet` is backed by a `HashMap`, while `TreeSet` is backed by a `TreeMap`, which uses a red-black tree.

### Complexity comparison

| Operation       | `HashSet`      | `TreeSet`  |
| :-------------- | :------------- | :--------- |
| `add()`         | Average `O(1)` | `O(log n)` |
| `remove()`      | Average `O(1)` | `O(log n)` |
| `contains()`    | Average `O(1)` | `O(log n)` |
| Iteration order | Unspecified    | Sorted     |

`TreeSet` is slower because each operation may require traversing and rebalancing a tree.

However, `TreeSet` provides features that `HashSet` does not:

* Sorted iteration
* `first()` and `last()`
* `floor()` and `ceiling()`
* `lower()` and `higher()`
* `headSet()`, `tailSet()`, and `subSet()`

Use `HashSet` when fast membership testing is the priority. Use `TreeSet` when sorted data or range operations are required.

---

## Question 6: What is `Hashtable`, and why is it generally not recommended?

`Hashtable` is a legacy key-value collection introduced before the Java Collections Framework.

It is similar to `HashMap`, but it has several important differences:

* Its public methods are synchronized.
* It does not allow `null` keys.
* It does not allow `null` values.
* It extends the legacy `Dictionary` class.
* It usually creates more lock contention than modern concurrent collections.

```java
Hashtable<String, Integer> table = new Hashtable<>();
table.put("Alice", 10);
```

`Hashtable` is generally avoided in new code because:

* `HashMap` is preferable for non-concurrent use.
* `ConcurrentHashMap` offers better scalability for concurrent use.
* External synchronization can be used when a custom locking strategy is required.

`Hashtable` mainly remains for backward compatibility with older applications.

---

## Question 7: How does `ConcurrentHashMap` work internally?

`ConcurrentHashMap` is a thread-safe map designed to support high levels of concurrent access.

### Java 7 design

Java 7 used multiple segments. Each segment acted as an independently locked portion of the map.

This allowed threads to update different segments concurrently.

### Java 8 and later

Java 8 replaced segments with a more fine-grained design based on:

* A bucket array similar to `HashMap`
* CAS operations for some lock-free updates
* Synchronization on an individual bucket when necessary
* Red-black trees for heavily collided buckets
* Distributed counters for scalable size tracking

When inserting into an empty bucket, `ConcurrentHashMap` can use a compare-and-swap operation.

When a bucket already contains entries, the thread may synchronize on that bucket's first node rather than locking the entire map.

Reads are generally non-blocking.

```java
ConcurrentHashMap<String, Integer> counts =
        new ConcurrentHashMap<>();

counts.put("Java", 1);
```

`ConcurrentHashMap` does not permit `null` keys or `null` values.

---

## Question 8: Why does `ConcurrentHashMap` not throw `ConcurrentModificationException` during iteration?

`ConcurrentHashMap` provides **weakly consistent iterators**.

These iterators:

* Do not use fail-fast `modCount` checks.
* Can safely continue while the map is modified.
* Reflect entries that existed at some point during or after iterator creation.
* May or may not reflect updates performed during iteration.
* Never throw `ConcurrentModificationException` merely because another thread modified the map.

```java
ConcurrentHashMap<String, Integer> map =
        new ConcurrentHashMap<>();

map.put("A", 1);
map.put("B", 2);

for (Map.Entry<String, Integer> entry : map.entrySet()) {
    map.put("C", 3);
    System.out.println(entry);
}
```

The iterator does not represent a fully isolated snapshot, but it remains safe to use while modifications occur.

---

## Question 9: What happens when two threads update the same key in `ConcurrentHashMap`?

Updates to an individual key are performed safely.

For a simple `put()` operation, one update eventually replaces the other. The final value is normally the value written by the last completed update.

```java
map.put("count", 10);
map.put("count", 20);
```

The final value is `20`.

However, a read-modify-write sequence using separate operations is not automatically atomic:

```java
map.put("count", map.get("count") + 1);
```

Two threads could both read the same value and lose an increment.

Use atomic compound methods instead:

```java
map.compute("count", (key, value) ->
        value == null ? 1 : value + 1);
```

Other useful atomic methods include:

* `putIfAbsent()`
* `computeIfAbsent()`
* `computeIfPresent()`
* `compute()`
* `merge()`
* `replace()`

For highly concurrent counters, a `LongAdder` stored in a `ConcurrentHashMap` can also be useful.

---

## Question 10: How can Java collections be made thread-safe?

Several strategies are available.

### 1. Synchronized wrappers

```java
List<String> list =
        Collections.synchronizedList(new ArrayList<>());

Map<String, Integer> map =
        Collections.synchronizedMap(new HashMap<>());
```

These wrappers synchronize individual method calls using one shared lock.

Iteration normally requires explicit synchronization:

```java
synchronized (list) {
    for (String value : list) {
        System.out.println(value);
    }
}
```

### 2. Concurrent collections

Examples include:

* `ConcurrentHashMap`
* `CopyOnWriteArrayList`
* `ConcurrentLinkedQueue`
* `ConcurrentLinkedDeque`
* `BlockingQueue` implementations
* `ConcurrentSkipListMap`
* `ConcurrentSkipListSet`

These classes are designed specifically for concurrent use.

### 3. Explicit synchronization

```java
synchronized (lock) {
    // Perform multiple collection operations atomically
}
```

### 4. Explicit lock classes

```java
Lock lock = new ReentrantLock();

lock.lock();
try {
    // Access collection
} finally {
    lock.unlock();
}
```

The best strategy depends on read/write frequency, required atomicity, ordering, and contention.

---

## Question 11: Which is better: `Collections.synchronizedMap()` or `ConcurrentHashMap`?

For most concurrent applications, `ConcurrentHashMap` is preferable.

### `Collections.synchronizedMap()`

* Uses one lock for the wrapped map.
* Individual methods are synchronized.
* Reads and writes can block one another.
* Iteration requires external synchronization.
* Allows `null` keys or values when the wrapped map supports them.

### `ConcurrentHashMap`

* Supports highly concurrent reads.
* Uses fine-grained coordination for writes.
* Provides atomic compound operations.
* Has weakly consistent iterators.
* Does not allow `null` keys or values.

```java
ConcurrentHashMap<String, Integer> map =
        new ConcurrentHashMap<>();

map.putIfAbsent("Java", 1);
map.computeIfPresent("Java", (key, value) -> value + 1);
```

Use `Collections.synchronizedMap()` when synchronizing an existing map is sufficient and low concurrency is expected.

Use `ConcurrentHashMap` for scalable concurrent access.

---

## Question 12: What is `CopyOnWriteArrayList`, and when should it be used?

`CopyOnWriteArrayList` is a thread-safe list that creates a new copy of its internal array whenever a modification occurs.

Mutating operations include:

* `add()`
* `remove()`
* `set()`
* `addAll()`

Readers access the current array without locking.

```java
CopyOnWriteArrayList<String> listeners =
        new CopyOnWriteArrayList<>();

listeners.add("ListenerA");
listeners.add("ListenerB");
```

It is suitable when:

* Reads greatly outnumber writes.
* Iteration happens frequently.
* Modifications are rare.
* Snapshot-style iteration is acceptable.

Common use cases include:

* Event listener collections
* Observer lists
* Configuration lists
* Feature flags
* Small allowlists or blocklists

It is not suitable for write-heavy workloads because each modification copies the entire array.

---

## Question 13: Why is iteration over `CopyOnWriteArrayList` safe?

When an iterator is created, it stores a reference to the array that existed at that moment.

```java
Iterator<String> iterator = list.iterator();
```

If another thread modifies the list, a new internal array is created. The iterator continues reading its original array.

Therefore:

* The iterator sees a stable snapshot.
* It is not affected by later modifications.
* It does not throw `ConcurrentModificationException`.
* No lock is required during iteration.

However, modifications made after iterator creation are not visible to that iterator.

The iterator also does not support modification operations such as `remove()`.

---

## Question 14: What is the difference between `List` and `Set`?

Both `List` and `Set` are child interfaces of `Collection`.

| Feature                | `List`                            | `Set`                                 |
| :--------------------- | :-------------------------------- | :------------------------------------ |
| Duplicates             | Allowed                           | Not allowed                           |
| Positional index       | Supported                         | Not supported                         |
| Ordering               | Usually preserves a defined order | Depends on implementation             |
| Common implementations | `ArrayList`, `LinkedList`         | `HashSet`, `LinkedHashSet`, `TreeSet` |
| Typical use            | Ordered sequence of elements      | Unique collection of elements         |

Example:

```java
List<String> list = new ArrayList<>();
list.add("Java");
list.add("Java");

System.out.println(list);
// [Java, Java]
```

```java
Set<String> set = new HashSet<>();
set.add("Java");
set.add("Java");

System.out.println(set);
// [Java]
```

A `HashSet` has no guaranteed iteration order, a `LinkedHashSet` preserves insertion order, and a `TreeSet` maintains sorted order.

---

## Question 15: What is the difference between `HashSet` and `LinkedHashSet`?

| Feature                     | `HashSet`          | `LinkedHashSet`                    |
| :-------------------------- | :----------------- | :--------------------------------- |
| Backing structure           | `HashMap`          | `LinkedHashMap`                    |
| Duplicate elements          | Not allowed        | Not allowed                        |
| Iteration order             | Unspecified        | Insertion order                    |
| `null` support              | One `null` element | One `null` element                 |
| Average add/remove/contains | `O(1)`             | `O(1)`                             |
| Memory usage                | Lower              | Higher due to linked-list pointers |

Use `HashSet` when order does not matter.

Use `LinkedHashSet` when unique values must be returned in insertion order.

```java
Set<Integer> values = new LinkedHashSet<>();

values.add(30);
values.add(10);
values.add(20);

System.out.println(values);
// [30, 10, 20]
```

---

## Question 16: What is the difference between `HashSet` and `TreeSet`?

| Feature                | `HashSet`                   | `TreeSet`                        |
| :--------------------- | :-------------------------- | :------------------------------- |
| Backing structure      | `HashMap`                   | `TreeMap`                        |
| Ordering               | Unspecified                 | Sorted                           |
| Average operation time | `O(1)`                      | `O(log n)`                       |
| Duplicate elements     | Not allowed                 | Not allowed                      |
| Equality rule          | `equals()` and `hashCode()` | `compareTo()` or `Comparator`    |
| `null` support         | One `null` element          | Normally does not support `null` |
| Navigation methods     | No                          | Yes                              |

`TreeSet` does not preserve insertion order. It stores elements according to natural ordering or a supplied comparator.

```java
Set<Integer> numbers = new TreeSet<>();

numbers.add(30);
numbers.add(10);
numbers.add(20);

System.out.println(numbers);
// [10, 20, 30]
```

Elements must be mutually comparable. Mixing unrelated, non-comparable types normally causes a `ClassCastException`.

---

## Question 17: What is the difference between `HashMap` and `LinkedHashMap`?

| Feature           | `HashMap`   | `LinkedHashMap`                   |
| :---------------- | :---------- | :-------------------------------- |
| Backing structure | Hash table  | Hash table and doubly linked list |
| Iteration order   | Unspecified | Insertion order by default        |
| `null` keys       | One         | One                               |
| `null` values     | Multiple    | Multiple                          |
| Average get/put   | `O(1)`      | `O(1)`                            |
| Memory usage      | Lower       | Higher                            |

`LinkedHashMap` can also be configured to maintain access order instead of insertion order:

```java
Map<String, Integer> cache =
        new LinkedHashMap<>(16, 0.75f, true);
```

With access order enabled, recently accessed entries move toward the end of the iteration order. This behavior is useful for implementing LRU-style caches.

---

## Question 18: What is the difference between `HashMap` and `TreeMap`?

| Feature                | `HashMap`                   | `TreeMap`                    |
| :--------------------- | :-------------------------- | :--------------------------- |
| Backing structure      | Hash table                  | Red-black tree               |
| Key order              | Unspecified                 | Sorted                       |
| Average get/put/remove | `O(1)`                      | `O(log n)`                   |
| `null` key             | One allowed                 | Normally not allowed         |
| `null` values          | Allowed                     | Allowed                      |
| Comparison             | `equals()` and `hashCode()` | `Comparable` or `Comparator` |
| Navigation methods     | No                          | Yes                          |

`TreeMap` provides methods such as:

* `firstKey()`
* `lastKey()`
* `floorKey()`
* `ceilingKey()`
* `lowerKey()`
* `higherKey()`
* `subMap()`
* `headMap()`
* `tailMap()`

Use `HashMap` for fast general-purpose key lookup.

Use `TreeMap` when keys must remain sorted or range queries are required.

---

## Question 19: What is the difference between `TreeMap` and `Hashtable`?

| Feature                  | `TreeMap`                         | `Hashtable`                       |
| :----------------------- | :-------------------------------- | :-------------------------------- |
| Backing structure        | Red-black tree                    | Hash table                        |
| Key order                | Sorted                            | Unspecified                       |
| Thread-safe              | No                                | Yes, through synchronized methods |
| Key operation complexity | `O(log n)`                        | Average `O(1)`                    |
| `null` keys              | Normally not allowed              | Not allowed                       |
| `null` values            | Allowed                           | Not allowed                       |
| Modern recommendation    | Use when sorted keys are required | Generally avoid in new code       |

`TreeMap` is designed for sorted key-value storage.

`Hashtable` is a synchronized legacy map. For modern concurrent applications, `ConcurrentHashMap` is normally preferred.
