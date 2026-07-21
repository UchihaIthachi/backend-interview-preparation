# Advanced Questions

## Question 1: What is a HashMap? How does it work internally?

`HashMap` stores key-value pairs using a hash table. Keys are hashed to determine the index, and collisions are handled using linked lists or trees.

Internally, `HashMap` is backed by an array of Nodes (buckets).
- When `put(key, value)` is called, it calculates the hash of the key and determines the bucket index using `(n - 1) & hash`.
- If the bucket is empty, a new Node is created.
- If the bucket is not empty (collision), it checks if the key already exists (using `equals()`). If it does, the value is updated.
- If it's a new key, it's added to the end of the linked list in that bucket.
- Java 8 Optimization: If the linked list size exceeds the `TREEIFY_THRESHOLD` (default 8) and the array capacity is at least `MIN_TREEIFY_CAPACITY` (64), the linked list is converted into a Red-Black Tree. This improves worst-case search time from O(n) to O(log n).
- When the number of elements exceeds `capacity * loadFactor` (default 0.75), the array is resized (doubled), and elements are rehashed (`rehashing`).

## Question 2: Explain the internal working of ArrayList

`ArrayList` is backed by a dynamic `Object[]` array:
- **Initial capacity:** 10 (default).
- When the array is full and a new element is added, a new array is allocated with `capacity = (oldCapacity * 3) / 2 + 1` (in Java 8+ it's exactly `oldCapacity + (oldCapacity >> 1)`), which is approximately a 1.5x growth.
- The old array contents are copied to the new array using `Arrays.copyOf()` (which internally calls `System.arraycopy()`).
- `get(index)` is O(1) — direct array access.
- `add(element)` at end is amortized O(1) — occasional resizing is O(n) but rare.
- `add(index, element)` or `remove(index)` in the middle is O(n) — requires shifting elements.
- `contains()` and `indexOf()` are O(n) — linear scan.

`ArrayList` is the most commonly used `List` because random access is cheap and cache-friendly due to contiguous memory.

## Question 3: How does Set ensure uniqueness without knowing object equality logic?

`Set` relies on the `hashCode()` and `equals()` methods defined on the objects themselves. When you call `set.add(obj)`:
- `HashSet` (which internally uses a `HashMap`) computes `obj.hashCode()` to find the bucket.
- It then calls `equals()` on each existing object in that bucket.
- If any existing object `equals()` the new object, the add is rejected.

So `Set` does not need to know your equality logic — it delegates entirely to the object's own `equals()` and `hashCode()` methods. This is why overriding these correctly is critical.

## Question 4: How does LinkedHashSet maintain insertion order?

`LinkedHashSet` extends `HashSet` but wraps a `LinkedHashMap` internally. Each entry in the backing `LinkedHashMap` maintains two extra pointers (before and after) forming a doubly-linked list across all entries in insertion order. This gives `LinkedHashSet` the O(1) add/remove/contains of `HashSet` combined with predictable insertion-order iteration. The memory overhead is two extra object references per entry.

## Question 5: Why is TreeSet slower than HashSet?

`TreeSet` is backed by a `TreeMap`, which is a Red-Black Tree — a self-balancing binary search tree.
- `HashSet`: add, remove, contains are O(1) average.
- `TreeSet`: add, remove, contains are O(log n) — each operation traverses or rebalances the tree.

`TreeSet`'s advantage is that it maintains sorted order and supports range queries (headSet, tailSet, subSet, first, last, floor, ceiling). Use `TreeSet` when you need sorted iteration; use `HashSet` when you only need fast membership testing.

## Question 6: What is Hashtable and why is it not recommended?

`Hashtable` is a legacy class from Java 1.0 (before the Collections Framework). It is a synchronized hash map where every method is synchronized on the entire object:
- All methods (`get`, `put`, `remove`, `containsKey`, `size`) are synchronized — extreme contention in multithreaded environments.
- Does not allow `null` keys or `null` values.
- Extends `Dictionary` (another legacy class) instead of `AbstractMap`.
- Does not implement the `Map` interface cleanly in spirit.

Why not recommended:
- `ConcurrentHashMap` provides far better concurrency with lock striping.
- `HashMap` + external synchronization gives more control.
- `Hashtable` is effectively obsolete and kept only for backward compatibility.

## Question 7: Explain the internal working of ConcurrentHashMap

**Java 7:** `ConcurrentHashMap` used a Segment array — each Segment is an independent `ReentrantLock`-protected mini `HashMap`. 16 segments by default meant 16 threads could write concurrently.

**Java 8+ (complete redesign):** `ConcurrentHashMap` now uses:
- A `volatile Node<K,V>[] table` similar to `HashMap`.
- CAS (Compare-And-Swap) operations for lock-free insertions into empty buckets.
- `synchronized` on the head node of a bucket for collision handling (only locks one bucket, not the whole map).
- Red-Black Tree treeification (same `TREEIFY_THRESHOLD=8` as `HashMap`).
- A `volatile` size counter using `LongAdder`-style striped counting for scalable `size()` computation.

This design allows extremely high read concurrency (reads are lock-free) and fine-grained write concurrency (only the affected bucket is locked).

## Question 8: Why does ConcurrentHashMap not throw ConcurrentModificationException?

`ConcurrentHashMap`'s iterators are weakly consistent — they reflect the state of the map at some point during iteration and may (or may not) reflect subsequent modifications. Instead of fail-fast behavior (checking `modCount`), iterators traverse the table snapshot and do not throw `ConcurrentModificationException`. This is by design for concurrent use cases where you want safe iteration without external synchronization.

## Question 9: What happens if two threads update the same key in ConcurrentHashMap?

`ConcurrentHashMap` handles this atomically at the bucket level. When two threads try to update the same key:
- Java 8+: The bucket's head node is locked with `synchronized`. One thread acquires the lock, performs its update, and releases. The second thread then acquires the lock and performs its update.
- The last write wins — there is no automatic merging of values.
- For atomic update-if-exists scenarios, use `compute()`, `merge()`, or `computeIfPresent()` — these are atomic at the bucket level.

## Question 10: How to make our collections thread-safe?

Several strategies exist:
- `Collections.synchronizedList(list)` / `synchronizedMap(map)` / `synchronizedSet(set)` — wraps any collection with synchronized methods. Simple but coarse-grained (single lock for all operations).
- Use concurrent collections: `ConcurrentHashMap`, `CopyOnWriteArrayList`, `ConcurrentLinkedQueue` — designed for high concurrency.
- Use explicit synchronization blocks with your own lock object.
- Use `java.util.concurrent` locks like `ReadWriteLock` for read-heavy scenarios.

## Question 11: Which is better: Collections.synchronizedMap or ConcurrentHashMap?

`ConcurrentHashMap` is almost always better for production use:
- `synchronizedMap` uses a single lock on the entire map — all reads and writes are mutually exclusive, creating a bottleneck under concurrency.
- `ConcurrentHashMap` uses lock striping (segment locks in Java 7, or CAS + `synchronized` on individual buckets in Java 8+). Multiple threads can read and write to different parts of the map simultaneously.
- `ConcurrentHashMap` provides atomic compound operations: `putIfAbsent()`, `computeIfAbsent()`, `compute()`, `merge()` — essential for thread-safe patterns.
- `ConcurrentHashMap`'s iterators are weakly consistent and never throw `ConcurrentModificationException`.

Use `synchronizedMap` only when you need to synchronize an existing map instance or need a fully consistent snapshot view. Prefer `ConcurrentHashMap` for new concurrent code.

## Question 12: What is CopyOnWriteArrayList and explain its use cases?

`CopyOnWriteArrayList` (COW) is a thread-safe variant of `ArrayList` where every mutating operation (`add`, `set`, `remove`) creates a brand new copy of the underlying array. Readers always see a consistent snapshot.

Use cases:
- Event listener lists — listeners are rarely added/removed but iterated frequently when events fire.
- Configuration or whitelist/blacklist collections that are read thousands of times per second but updated rarely.
- Observer pattern implementations.
- Any scenario where reads vastly outnumber writes and you can tolerate slightly stale reads.

## Question 13: Why does CopyOnWriteArrayList allow safe iterations?

When an iterator is created, it captures a reference to the current array snapshot. Even if another thread modifies the list (creating a new array), the iterator continues traversing its original snapshot. Reads are completely lock-free. Since the iterator operates on an immutable array snapshot taken at iterator creation time, there is no shared mutable state between the iterator and any writing thread, hence `ConcurrentModificationException` cannot occur.

## Question 14: What is the Difference between List vs Set?

- **List:** A child interface of `Collection`. If we want to represent a group of individual objects in a single entity where duplicates are allowed and insertion order is preserved, we use the `List` interface.
- **Set:** A child interface of `Collection`. If we want to represent a group of individual objects where duplicates are not allowed and insertion order is generally not preserved (except `LinkedHashSet`), we use the `Set` interface.

## Question 15: What is the Difference between HashSet vs LinkedHashSet?

- **HashSet:** Backed by `HashMap`. Insertion order is not preserved (objects are arranged based on hashcode). Duplicate objects are not allowed. Heterogeneous objects are allowed. Null insertion is possible.
- **LinkedHashSet:** Backed by `LinkedHashMap`. Insertion order is preserved. Duplicate objects are not allowed. Heterogeneous objects are allowed. Null insertion is possible. Introduced in 1.4v.

## Question 16: What is the Difference between HashSet vs TreeSet?

- **HashSet:** Backed by `HashMap`. Insertion order is not preserved. Duplicate objects not allowed. Null insertion possible.
- **TreeSet:** Backed by a Balanced Tree (`TreeMap`). Insertion order is preserved because it's based on sorting order. Duplicate objects not allowed. Null insertion is possible only once (for empty TreeSet initially, but typically throws NPE). Implements `NavigableSet`.

## Question 17: What is the Difference between HashMap vs LinkedHashMap?

- **HashMap:** Backed by a Hash table. Insertion order is not preserved. Null keys (once) and multiple Null values are allowed.
- **LinkedHashMap:** Backed by a Hash table + Doubly linked list. It is a child class of `HashMap`. Insertion order is preserved. Null keys (once) and multiple Null values are allowed.

## Question 18: What is the Difference between HashMap vs TreeMap?

- **HashMap:** Backed by a Hash table. Insertion order is not preserved. Null keys (once) and multiple Null values allowed.
- **TreeMap:** Backed by a RED BLACK TREE. Insertion order is based on sorting order of keys. Null keys are generally not allowed (throws NPE) but multiple Null values are allowed.

## Question 19: What is the Difference between TreeMap vs Hashtable?

- **TreeMap:** Backed by RED BLACK TREE. Insertion order is sorted. Does not allow null keys. Allows multiple null values.
- **Hashtable:** Backed by Hash table. Insertion order is not preserved. Does not allow either null keys or null values (throws NPE). Every method is synchronized.

