# Advanced Java Collections Questions

The uploaded notes cover the main collection internals well. The version below removes repetition, repairs the corrupted exception name, tightens concurrency claims, and separates API guarantees from implementation details.

---

## Question 1: What is a `HashMap`, and how does it work internally?

`HashMap<K, V>` stores key-value mappings using a hash-table-based structure.

Conceptually, it maintains an array of buckets. A bucket may contain:

- No entry
- A single node
- A linked chain of nodes
- A red-black tree when collisions become excessive

```java
Map<String, Integer> scores = new HashMap<>();

scores.put("Alice", 90);
scores.put("Bob", 85);

Integer score = scores.get("Alice");
```

### How `put()` works

When calling:

```java
map.put(key, value);
```

the map broadly performs these steps:

1. Obtains the key’s `hashCode()`.
2. Applies an internal hash-spreading operation.
3. Calculates a bucket index from the hash and table capacity.
4. Inserts a new node when the bucket is empty.
5. Otherwise, compares the key with existing keys using hash values and `equals()`.
6. Replaces the value when an equal key exists.
7. Adds a new node when no equal key exists.
8. Resizes the table when its threshold is exceeded.

### Conceptual lookup

```text
key
 ↓
hashCode()
 ↓
spread hash bits
 ↓
bucket index
 ↓
compare candidate keys using equals()
```

### Collisions

A collision occurs when different keys map to the same bucket.

```java
@Override
public int hashCode() {
    return 1; // Deliberately poor implementation
}
```

A poor hash function creates long collision chains and degrades performance.

### Treeification

In common Java 8+ `HashMap` implementations, a heavily populated bucket can be converted into a red-black tree when:

- The bucket reaches the treeification threshold, commonly `8`.
- The table capacity is at least the minimum treeification capacity, commonly `64`.

When the table is still small, `HashMap` normally resizes instead of immediately treeifying the bucket.

A tree may later be converted back to a linked structure when its population becomes sufficiently small.

These threshold values are implementation details rather than general `Map` interface guarantees.

### Capacity, load factor, and threshold

Common defaults are:

```text
Initial capacity: 16
Load factor:      0.75
Threshold:        capacity × load factor
```

For capacity `16`:

```text
threshold = 16 × 0.75 = 12
```

When the size exceeds the threshold, the bucket array is normally expanded.

### Complexity

| Operation         |          Average |                               Collision-heavy case |
| ----------------- | ---------------: | -------------------------------------------------: |
| `get()`           |           `O(1)` | `O(n)`, or approximately `O(log n)` for a tree bin |
| `put()`           | Amortized `O(1)` |                               `O(n)` or `O(log n)` |
| `remove()`        |           `O(1)` |                               `O(n)` or `O(log n)` |
| `containsValue()` |           `O(n)` |                                             `O(n)` |

### Key requirements

A custom key should have:

- Stable `equals()` behavior
- Stable `hashCode()` behavior
- Consistency between `equals()` and `hashCode()`
- Preferably immutable fields involved in equality

### Interview-ready answer

> `HashMap` stores mappings in an array of buckets. It uses a key’s hash code to select a bucket and `equals()` to find the matching key. Collisions are stored in a linked structure and may become a red-black tree when a bucket becomes heavily populated. Average lookup is `O(1)`, but good key immutability and correct `equals()` and `hashCode()` implementations are essential.

---

## Question 2: How does `ArrayList` work internally?

`ArrayList<E>` is a resizable-array implementation of `List`.

It stores element references in an internal array-like structure.

```java
List<String> names = new ArrayList<>();

names.add("Alice");
names.add("Bob");
```

### Capacity vs size

- **Size** is the number of stored elements.
- **Capacity** is the amount of internal storage currently available.

```java
List<String> names = new ArrayList<>(100);
```

This creates capacity for approximately 100 elements but the list size is still zero.

### Default construction

A newly created empty `ArrayList` may initially share an empty internal array. In common implementations, the first insertion into a default-constructed list expands its capacity to `10`.

This is an implementation detail, not a guarantee of the `List` interface.

### Growth

When the internal storage is full:

1. A larger array is allocated.
2. Existing references are copied.
3. The old array becomes eligible for garbage collection.
4. The new element is inserted.

Common implementations grow by approximately 50%:

```text
newCapacity = oldCapacity + oldCapacity / 2
```

Again, the exact growth policy is an implementation detail.

### Complexity

| Operation           |       Complexity | Explanation                       |
| ------------------- | ---------------: | --------------------------------- |
| `get(index)`        |           `O(1)` | Direct indexed access             |
| `set(index, value)` |           `O(1)` | Direct indexed replacement        |
| `add(value)`        | Amortized `O(1)` | Occasional resizing               |
| `add(index, value)` |           `O(n)` | Elements may be shifted           |
| `remove(index)`     |           `O(n)` | Remaining elements may be shifted |
| `contains(value)`   |           `O(n)` | Linear search                     |
| `indexOf(value)`    |           `O(n)` | Linear search                     |
| Iteration           |           `O(n)` | Visits each element               |

### Removing an element

```java
names.remove(0);
```

Elements after the removed position are shifted left, and the unused final slot is cleared so the removed reference can be garbage-collected.

### When should it be used?

Use `ArrayList` when:

- Indexed access is common.
- Iteration is common.
- Most insertions happen at the end.
- Middle insertions and removals are relatively rare.

### Thread safety

`ArrayList` is not thread-safe for concurrent mutation.

Alternatives include:

```java
Collections.synchronizedList(new ArrayList<>());
CopyOnWriteArrayList<String>
```

The correct choice depends on the workload.

---

## Question 3: How does a `Set` ensure uniqueness?

A `Set` does not allow duplicate elements, but the mechanism depends on its implementation.

### `HashSet`

`HashSet` uses `hashCode()` and `equals()`.

```java
Set<String> languages = new HashSet<>();

languages.add("Java");
languages.add("Java");

System.out.println(languages.size()); // 1
```

When adding an element:

1. Its hash code is calculated.
2. A bucket is selected.
3. Existing candidate elements are compared using `equals()`.
4. If an equal element exists, `add()` returns `false`.
5. Otherwise, the element is inserted and `add()` returns `true`.

```java
boolean first = languages.add("Go");  // true
boolean second = languages.add("Go"); // false
```

### `TreeSet`

`TreeSet` uses ordering rather than hashing.

Two elements are considered duplicates when:

```java
comparator.compare(first, second) == 0
```

or:

```java
first.compareTo(second) == 0
```

This can differ from `equals()`.

```java
Set<BigDecimal> values = new TreeSet<>();

values.add(new BigDecimal("1.0"));
values.add(new BigDecimal("1.00"));

System.out.println(values.size()); // 1
```

`BigDecimal.compareTo()` considers these numerically equal, although `equals()` does not.

### Custom-key rule

For hash-based sets, equal objects must have equal hash codes:

```java
a.equals(b) == true
```

must imply:

```java
a.hashCode() == b.hashCode()
```

---

## Question 4: How does `LinkedHashSet` maintain insertion order?

`LinkedHashSet` is backed by a `LinkedHashMap`.

Its backing structure combines:

- Hash-based lookup
- Links connecting entries in insertion order

```java
Set<String> languages = new LinkedHashSet<>();

languages.add("Java");
languages.add("Go");
languages.add("Python");

System.out.println(languages);
// [Java, Go, Python]
```

### Complexity

| Operation    | Average complexity |
| ------------ | -----------------: |
| `add()`      |             `O(1)` |
| `contains()` |             `O(1)` |
| `remove()`   |             `O(1)` |
| Iteration    |             `O(n)` |

### Trade-off

`LinkedHashSet` consumes more memory than `HashSet` because entries maintain ordering links.

Use it when:

- Values must remain unique.
- Iteration should preserve insertion order.
- Sorting is not required.

---

## Question 5: Why is `TreeSet` usually slower than `HashSet`?

`HashSet` uses hashing, while `TreeSet` uses a balanced search tree.

| Operation       |      `HashSet` |  `TreeSet` |
| --------------- | -------------: | ---------: |
| `add()`         | Average `O(1)` | `O(log n)` |
| `contains()`    | Average `O(1)` | `O(log n)` |
| `remove()`      | Average `O(1)` | `O(log n)` |
| Iteration order |    Unspecified |     Sorted |

`TreeSet` performs comparison and tree traversal for each operation. Insertions and removals may also require tree rebalancing.

In exchange, it provides:

```java
first();
last();
lower(value);
higher(value);
floor(value);
ceiling(value);
headSet(value);
tailSet(value);
subSet(from, to);
```

Use:

- `HashSet` for fast membership checks.
- `LinkedHashSet` for insertion-order uniqueness.
- `TreeSet` for sorted uniqueness and range queries.

---

## Question 6: What is `Hashtable`, and why is it generally avoided?

`Hashtable<K, V>` is a legacy synchronized map implementation.

```java
Hashtable<String, Integer> table =
        new Hashtable<>();

table.put("Alice", 10);
```

Characteristics:

- Public operations are synchronized.
- Null keys are rejected.
- Null values are rejected.
- It extends the legacy `Dictionary` class.
- It implements `Map`.
- It generally uses one monitor for operations.
- It remains mainly for compatibility with old code.

### Why not use it for new code?

For non-concurrent use:

```java
HashMap<K, V>
```

is normally preferable.

For concurrent use:

```java
ConcurrentHashMap<K, V>
```

usually offers better scalability and richer atomic operations.

### Compound operations still matter

This is not safely atomic merely because both methods are synchronized:

```java
if (!table.containsKey(key)) {
    table.put(key, value);
}
```

Another thread can modify the table between the two method calls.

Prefer:

```java
concurrentMap.putIfAbsent(key, value);
```

---

## Question 7: How does `ConcurrentHashMap` work internally?

`ConcurrentHashMap<K, V>` is a thread-safe map designed for scalable concurrent access.

```java
ConcurrentHashMap<String, Integer> counts =
        new ConcurrentHashMap<>();
```

### Earlier segmented design

Older Java versions used a segmented structure. Different segments could be locked independently.

### Java 8+ design

Common modern implementations use:

- A bucket array
- CAS for selected updates
- Fine-grained synchronization on individual bins
- Tree bins for heavy collisions
- Volatile and memory-ordering mechanisms
- Cooperative resizing
- Distributed counters for scalable size estimation

### Reads

Reads are generally non-blocking:

```java
Integer value = counts.get("Java");
```

### Empty-bin insertion

When the target bucket is empty, an insertion may use compare-and-set rather than acquiring a global lock.

### Collided-bin insertion

When entries already exist, the operation may synchronize on the bin’s first node while updating that bin.

This means unrelated buckets can often be updated concurrently.

### Resizing

Resizing is coordinated across threads. Threads may help migrate buckets instead of one thread performing the entire resize alone.

### Null restrictions

These are invalid:

```java
counts.put(null, 1);
counts.put("Java", null);
```

Both throw `NullPointerException`.

Disallowing nulls makes concurrent lookup semantics unambiguous because:

```java
map.get(key) == null
```

can consistently mean that no mapping is currently available.

### Important caution

Internal details are implementation-specific and can change. The stable API guarantees are more important:

- Thread-safe operations
- Atomic compound methods
- Weakly consistent iteration
- No null keys or values

---

## Question 8: Why does `ConcurrentHashMap` not throw `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63` during normal concurrent iteration?

The corrupted exception name in the source should be:

```java
ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63
```

`ConcurrentHashMap` iterators are **weakly consistent**.

They:

- Can run while updates occur.
- Do not use ordinary fail-fast structural-modification checks.
- Do not throw `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63` merely because another thread modifies the map.
- May observe some concurrent updates.
- May not observe every concurrent update.
- Do not provide a fully isolated snapshot.
- Do not return an element more than once during one traversal.

```java
ConcurrentHashMap<String, Integer> map =
        new ConcurrentHashMap<>();

map.put("A", 1);
map.put("B", 2);

for (Map.Entry<String, Integer> entry :
        map.entrySet()) {

    map.put("C", 3);
    System.out.println(entry);
}
```

The iteration remains safe, but whether `"C"` appears in that traversal is not something callers should depend on.

### Fail-fast vs weakly consistent

| Fail-fast iterator                                  | Weakly consistent iterator                        |
| --------------------------------------------------- | ------------------------------------------------- |
| Detects some structural modifications               | Supports concurrent modification                  |
| May throw `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63` | Does not throw it for ordinary concurrent updates |
| Best-effort detection                               | May reflect some updates                          |
| Not a thread-safety mechanism                       | Designed for concurrent use                       |

Fail-fast behavior is not guaranteed detection of every invalid modification.

---

## Question 9: What happens when two threads update the same `ConcurrentHashMap` key?

Each individual map operation is thread-safe and has an atomic effect.

```java
map.put("status", "PENDING");
```

If two threads concurrently call:

```java
map.put("status", "COMPLETED");
map.put("status", "FAILED");
```

the final map contains one of the values according to the operations’ runtime ordering. Code should not assume which one wins unless additional coordination establishes an order.

It is too simplistic to say the source-code line that appears “last” necessarily wins.

### Unsafe read-modify-write

```java
map.put("count", map.get("count") + 1);
```

Two threads can read the same old value and lose an increment.

### Use `compute()`

```java
map.compute(
        "count",
        (key, current) ->
                current == null
                        ? 1
                        : current + 1
);
```

### Use `merge()`

```java
map.merge("count", 1, Integer::sum);
```

### Highly contended counters

```java
ConcurrentHashMap<String, LongAdder> frequencies =
        new ConcurrentHashMap<>();

frequencies
        .computeIfAbsent(
                word,
                ignored -> new LongAdder()
        )
        .increment();
```

### Important caveat

The remapping function passed to `compute()` or `merge()` should be:

- Short
- Non-blocking where possible
- Free from unrelated expensive I/O
- Free from recursive updates that can interfere with the same mapping

---

## Question 10: How can Java collections be made thread-safe?

There is no single best mechanism. Choose based on the required consistency and workload.

### 1. Thread confinement

Keep the collection accessible to one thread only.

```java
public List<Result> process() {
    List<Result> results =
            new ArrayList<>();

    // Local mutable list
    return results;
}
```

### 2. Immutability

```java
List<String> roles =
        List.of("ADMIN", "USER");
```

Immutable collections are easy to share after safe publication.

### 3. Synchronized wrappers

```java
List<String> list =
        Collections.synchronizedList(
                new ArrayList<>()
        );
```

Iteration requires synchronization on the wrapper:

```java
synchronized (list) {
    for (String value : list) {
        System.out.println(value);
    }
}
```

### 4. Concurrent collections

Examples:

```java
ConcurrentHashMap<K, V>
CopyOnWriteArrayList<E>
ConcurrentLinkedQueue<E>
ConcurrentLinkedDeque<E>
ConcurrentSkipListMap<K, V>
ConcurrentSkipListSet<E>
BlockingQueue<E>
```

### 5. Explicit locking

```java
lock.lock();

try {
    // Multi-operation invariant
} finally {
    lock.unlock();
}
```

### 6. Atomic collection methods

Prefer:

```java
map.putIfAbsent(key, value);
map.computeIfAbsent(key, this::load);
map.merge(key, 1, Integer::sum);
```

over check-then-act sequences.

### Selection guide

| Requirement                   | Typical choice          |
| ----------------------------- | ----------------------- |
| Non-shared mutable collection | `ArrayList`, `HashMap`  |
| Fixed shared data             | Immutable copy          |
| Simple low-contention wrapper | Synchronized wrapper    |
| Concurrent key-value access   | `ConcurrentHashMap`     |
| Read-mostly listener list     | `CopyOnWriteArrayList`  |
| Producer-consumer flow        | `BlockingQueue`         |
| Sorted concurrent map         | `ConcurrentSkipListMap` |

---

## Question 11: `Collections.synchronizedMap()` vs `ConcurrentHashMap`

### Synchronized map

```java
Map<String, Integer> map =
        Collections.synchronizedMap(
                new HashMap<>()
        );
```

Characteristics:

- Uses one synchronization lock.
- Individual method calls are synchronized.
- Reads and writes contend for the same lock.
- Iteration requires external synchronization.
- Null handling follows the wrapped map.
- Multi-call operations need external coordination.

### Concurrent map

```java
ConcurrentMap<String, Integer> map =
        new ConcurrentHashMap<>();
```

Characteristics:

- Reads are highly concurrent.
- Writes use finer-grained coordination.
- Atomic compound operations are available.
- Iterators are weakly consistent.
- Null keys and values are rejected.
- It scales better under concurrent access in most workloads.

### Comparison

| Feature          | Synchronized map               | `ConcurrentHashMap`               |
| ---------------- | ------------------------------ | --------------------------------- |
| Lock granularity | One wrapper lock               | Fine-grained coordination         |
| Concurrent reads | Serialize on wrapper lock      | Highly concurrent                 |
| Iteration        | External synchronization       | Weakly consistent                 |
| Null support     | Depends on wrapped map         | No                                |
| Atomic APIs      | Limited                        | `compute`, `merge`, `putIfAbsent` |
| Typical use      | Simple or low-contention cases | Scalable concurrent access        |

---

## Question 12: What is `CopyOnWriteArrayList`?

`CopyOnWriteArrayList<E>` is a thread-safe list optimized for workloads with:

- Very frequent reads
- Frequent iteration
- Rare modifications
- Relatively small lists

```java
CopyOnWriteArrayList<Listener> listeners =
        new CopyOnWriteArrayList<>();
```

Each mutating operation creates a new internal array representation.

```java
listeners.add(listener);
listeners.remove(listener);
```

### Benefits

- Readers do not need to lock the array being traversed.
- Iterators observe a stable snapshot.
- Concurrent iteration is straightforward.
- No `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63` is thrown because of later writes.

### Costs

- Every write copies the current array.
- Write cost is `O(n)`.
- Temporary memory allocation can be significant.
- Iterators do not see modifications made after iterator creation.
- Stale snapshot behavior must be acceptable.

### Good use cases

- Event listeners
- Small routing tables
- Small feature-configuration lists
- Observer registries
- Read-mostly allowlists

### Poor use cases

- Large lists
- Frequent writes
- Queue-like workloads
- Frequently updated transactional data

---

## Question 13: Why is `CopyOnWriteArrayList` iteration safe?

An iterator holds a reference to the immutable snapshot array that existed when the iterator was created.

```java
Iterator<String> iterator =
        list.iterator();
```

If another thread modifies the list, the list publishes a new array. The iterator continues traversing its old array.

Therefore:

- Its view is stable.
- No iteration lock is required.
- Later changes are not visible.
- Mutation through the iterator is unsupported.

These operations throw `UnsupportedOperationException`:

```java
iterator.remove();
listIterator.set(value);
listIterator.add(value);
```

### Snapshot does not mean current

```java
Iterator<String> iterator =
        list.iterator();

list.add("New");

while (iterator.hasNext()) {
    System.out.println(iterator.next());
}
```

The iterator normally does not display `"New"` because it traverses the earlier snapshot.

---

## Question 14: What is the difference between `List` and `Set`?

| Feature                | `List`                        | `Set`                                 |
| ---------------------- | ----------------------------- | ------------------------------------- |
| Duplicate elements     | Allowed                       | Not allowed                           |
| Positional index       | Supported                     | Not supported                         |
| Order                  | Defined by the implementation | Defined by the implementation         |
| Typical purpose        | Sequence                      | Unique membership                     |
| Common implementations | `ArrayList`, `LinkedList`     | `HashSet`, `LinkedHashSet`, `TreeSet` |

```java
List<String> list =
        new ArrayList<>();

list.add("Java");
list.add("Java");
```

Result:

```text
[Java, Java]
```

```java
Set<String> set =
        new HashSet<>();

set.add("Java");
set.add("Java");
```

The set contains one `"Java"` element.

### Ordering nuance

- `List` preserves positional sequence.
- `HashSet` has no guaranteed iteration order.
- `LinkedHashSet` preserves insertion order.
- `TreeSet` maintains sorted order.

---

## Question 15: `HashSet` vs `LinkedHashSet`

| Feature        | `HashSet`         | `LinkedHashSet`   |
| -------------- | ----------------- | ----------------- |
| Backing map    | `HashMap`         | `LinkedHashMap`   |
| Uniqueness     | Hash and equality | Hash and equality |
| Order          | Unspecified       | Insertion order   |
| Average lookup | `O(1)`            | `O(1)`            |
| Null support   | One               | One               |
| Memory usage   | Lower             | Higher            |

Use `LinkedHashSet` when a stable insertion order is part of the requirement.

```java
Set<Integer> numbers =
        new LinkedHashSet<>();

numbers.add(30);
numbers.add(10);
numbers.add(20);

System.out.println(numbers);
// [30, 10, 20]
```

---

## Question 16: `HashSet` vs `TreeSet`

| Feature            | `HashSet`                   | `TreeSet`         |
| ------------------ | --------------------------- | ----------------- |
| Structure          | Hash table                  | Red-black tree    |
| Order              | Unspecified                 | Sorted            |
| Average operation  | `O(1)`                      | `O(log n)`        |
| Equality mechanism | `equals()` and `hashCode()` | Comparison result |
| Null               | One supported               | Usually rejected  |
| Range operations   | No                          | Yes               |

### Important distinction

`HashSet` considers elements duplicates based on equality.

`TreeSet` considers elements duplicates when comparison returns zero.

Therefore, comparison should normally be consistent with `equals()` to avoid surprising behavior.

---

## Question 17: `HashMap` vs `LinkedHashMap`

| Feature                 | `HashMap`   | `LinkedHashMap` |
| ----------------------- | ----------- | --------------- |
| Lookup structure        | Hash table  | Hash table      |
| Ordering links          | No          | Yes             |
| Default order           | Unspecified | Insertion order |
| Optional access order   | No          | Yes             |
| Average `get()`/`put()` | `O(1)`      | `O(1)`          |
| Memory                  | Lower       | Higher          |

### Access-order mode

```java
Map<String, Integer> cache =
        new LinkedHashMap<>(
                16,
                0.75f,
                true
        );
```

In access-order mode, successful accesses affect iteration order.

### LRU-style cache

```java
public final class LruCache<K, V>
        extends LinkedHashMap<K, V> {

    private final int maximumSize;

    public LruCache(int maximumSize) {
        super(16, 0.75f, true);
        this.maximumSize = maximumSize;
    }

    @Override
    protected boolean removeEldestEntry(
            Map.Entry<K, V> eldest
    ) {
        return size() > maximumSize;
    }
}
```

This simple cache is not thread-safe and does not provide expiration, loading, metrics, or distributed behavior.

---

## Question 18: `HashMap` vs `TreeMap`

| Feature           | `HashMap`         | `TreeMap`      |
| ----------------- | ----------------- | -------------- |
| Structure         | Hash table        | Red-black tree |
| Key order         | Unspecified       | Sorted         |
| Typical operation | `O(1)` average    | `O(log n)`     |
| Null key          | One               | Normally no    |
| Null values       | Yes               | Yes            |
| Key matching      | Hash and equality | Comparison     |
| Range operations  | No                | Yes            |

Use `TreeMap` for operations such as:

```java
firstEntry();
lastEntry();
floorEntry(key);
ceilingEntry(key);
subMap(from, to);
headMap(to);
tailMap(from);
```

Use `HashMap` when sorted traversal and range queries are unnecessary.

---

## Question 19: `TreeMap` vs `Hashtable`

| Feature        | `TreeMap`         | `Hashtable`             |
| -------------- | ----------------- | ----------------------- |
| Data structure | Red-black tree    | Hash table              |
| Order          | Sorted keys       | Unspecified             |
| Thread-safe    | No                | Synchronized methods    |
| Typical lookup | `O(log n)`        | Average `O(1)`          |
| Null key       | Normally no       | No                      |
| Null value     | Yes               | No                      |
| Status         | Modern sorted map | Legacy synchronized map |

These classes solve different problems:

- `TreeMap` provides sorted navigation.
- `Hashtable` provides legacy synchronized hashing.

For a thread-safe sorted map, consider:

```java
ConcurrentSkipListMap<K, V>
```

For a concurrent unsorted map, use:

```java
ConcurrentHashMap<K, V>
```

---

# Important Technical Corrections

## 1. Corrupted exception name

Replace every occurrence of:

```text
ConcurpZELkorMwhkkpShF5PvT8YnqRCupnLTNG
```

with:

```java
ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63
```

---

## 2. Concurrent updates to one key

Replace:

> The final value is the value written by the last completed update.

with:

> Each update is atomic, and the final value is determined by the operations’ runtime linearization order. Without explicit coordination, application code should not assume which competing update wins.

---

## 3. `ConcurrentHashMap` iterators

Avoid saying that an iterator reflects entries that existed “during or after” creation as though it defines a precise snapshot interval.

Use:

> A weakly consistent iterator may observe some concurrent updates but is not guaranteed to observe all of them.

---

## 4. HashMap treeification

Use wording such as:

> Common Java 8+ implementations use thresholds around eight entries and a minimum table capacity of 64.

This correctly communicates that these are implementation details rather than promises of the `Map` API.

---

## 5. ArrayList growth

The common 1.5× growth behavior should also be described as an implementation detail.

Do not design application correctness around a particular internal growth formula.

---

# Recommended File Split

```text
java/
└── 02-collections/
    ├── advanced-questions.md
    ├── internals/
    │   ├── hashmap.md
    │   ├── arraylist.md
    │   ├── concurrent-hashmap.md
    │   └── copy-on-write-array-list.md
    ├── maps/
    │   ├── hashmap-vs-linkedhashmap.md
    │   ├── hashmap-vs-treemap.md
    │   ├── hashmap-vs-hashtable.md
    │   └── synchronized-map-vs-concurrent-map.md
    ├── sets/
    │   ├── uniqueness.md
    │   ├── hashset-vs-linkedhashset.md
    │   └── hashset-vs-treeset.md
    └── concurrency/
        ├── thread-safe-collections.md
        ├── weakly-consistent-iteration.md
        └── atomic-map-operations.md
```

This keeps implementation internals separate from comparison questions and prevents `advanced-questions.md` from becoming one oversized collection document.
