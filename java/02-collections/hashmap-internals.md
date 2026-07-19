# How HashMap Works Internally

## 1. Definition

`HashMap` stores key-value pairs in an array of buckets. Each bucket holds entries
whose keys hash to the same index, as a linked list (or a balanced tree once a
bucket gets large enough).

## 2. Why it exists

Arrays give O(1) index access but need a way to map arbitrary keys to array
indices — hashing solves that, giving average O(1) lookup instead of O(n) linear
search.

## 3. How it works

```text
1. key.hashCode() is computed and spread (XORed with its own bits) to reduce
   clustering.
2. index = spreadHash & (capacity - 1)   // capacity is always a power of two
3. The entry is added to the linked list at that bucket index.
4. On lookup, Java walks the bucket's list, calling .equals() on each key
   until it finds a match.
5. When the map exceeds capacity * loadFactor (default 0.75), it resizes:
   doubles capacity and rehashes every entry into new buckets.
6. Since Java 8, if a single bucket's list grows past 8 entries (and the table
   is large enough), that bucket converts to a red-black tree, turning
   worst-case O(n) lookup into O(log n).
```

## 4. Code example — why mutable keys break HashMap

```java
class BadKey {
    int value;
    BadKey(int value) { this.value = value; }
    // no equals()/hashCode() override -> identity-based, or worse: mutable fields
}

Map<BadKey, String> map = new HashMap<>();
BadKey key = new BadKey(1);
map.put(key, "data");

key.value = 2;              // mutating the key after insertion
map.get(key);                // may return null! the entry now hashes to a
                              // different bucket than the one it was stored in
```

## 5. Production use case

Caching layers, deduplication, and lookup tables all rely on correct
`equals()`/`hashCode()` implementations. Using a mutable object (or forgetting to
override both methods together) as a map key is one of the most common
production bugs — entries silently become unreachable.

## 6. Common mistakes

- Overriding `equals()` without overriding `hashCode()` (violates the contract:
  equal objects must have equal hash codes).
- Using a mutable object as a key and mutating it after insertion.
- Assuming `HashMap` preserves insertion order (use `LinkedHashMap` instead).
- Assuming `HashMap` is thread-safe (use `ConcurrentHashMap` for concurrent access).

## 7. Trade-offs

| HashMap | ConcurrentHashMap | TreeMap |
|---|---|---|
| Fast, not thread-safe | Thread-safe, segmented locking | Sorted, O(log n) |

## 8. Interview questions

1. Walk through what happens on `map.put(key, value)` step by step.
2. Why must `equals()` and `hashCode()` be overridden together?
3. What happens if you mutate a key after inserting it into a `HashMap`?
4. What changed about bucket structure in Java 8 (treeification)?
5. `HashMap` vs `ConcurrentHashMap` — how does the latter avoid full-map locking?

## 9. Short interview answer

"A HashMap hashes each key, spreads the bits to reduce collisions, and maps that
to a bucket index. Collisions in a bucket are handled with a linked list, or a
red-black tree once a bucket gets large, keeping worst-case lookup at O(log n)
instead of O(n). It resizes and rehashes everything once the load factor
threshold is crossed. The most common production bug is using a mutable key,
since mutating it after insertion can make the entry unreachable."

## 10. Related topics

- [equals() and hashCode() contract](../01-core-java/oop.md)
- [ConcurrentHashMap](../04-concurrency/questions.md)
