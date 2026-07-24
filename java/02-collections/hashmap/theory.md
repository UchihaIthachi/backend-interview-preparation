# How `HashMap` Works Internally

## 1. Definition

`HashMap<K, V>` stores key-value mappings using a hash-table-based structure.

Internally, a common Java implementation maintains an array of buckets. A bucket may contain:

- No entry
- One entry
- A linked chain of entries after collisions
- A red-black tree when collision density becomes high enough

```java
Map<String, Integer> scores = new HashMap<>();

scores.put("Alice", 90);
scores.put("Bob", 85);

Integer score = scores.get("Alice");
```

`HashMap` provides average `O(1)` lookup, insertion, and removal when keys have well-distributed hash codes.

---

## 2. Why Does `HashMap` Exist?

Arrays provide constant-time indexed access:

```java
array[index];
```

However, application keys are not always integer indexes. They may be:

- Strings
- User IDs
- Product codes
- Composite business keys
- Immutable domain objects

Hashing converts an arbitrary key into an integer that can be mapped to an array bucket.

```text
Key
 ↓
hashCode()
 ↓
Hash spreading
 ↓
Bucket index
 ↓
Candidate entries
 ↓
equals() comparison
```

This avoids scanning every mapping as a list-based implementation would.

---

# Internal Structure

## 3. What Does a `HashMap` Entry Contain?

Conceptually, each entry contains:

```java
final class Node<K, V> {
    final int hash;
    final K key;
    V value;
    Node<K, V> next;
}
```

The stored information includes:

- The spread hash
- The key
- The value
- A link to the next entry in the same bucket

A treeified bucket uses specialized tree nodes instead of an ordinary linked chain.

---

# Insertion

## 4. What Happens During `put(key, value)`?

Consider:

```java
map.put("Alice", 90);
```

A common Java 8+ implementation broadly follows these steps.

### Step 1: Calculate the key hash

```java
int hash = key == null
        ? 0
        : key.hashCode();
```

`HashMap` allows one `null` key. A null key is handled using hash value zero.

---

### Step 2: Spread the hash bits

A simplified representation is:

```java
int spreadHash = hash ^ (hash >>> 16);
```

This mixes higher hash bits into lower bits.

It is useful because bucket selection primarily uses lower bits when the table capacity is a power of two.

---

### Step 3: Calculate the bucket index

A common implementation calculates the index as:

```java
index = (capacity - 1) & spreadHash;
```

For example, with capacity `16`:

```text
index = spreadHash & 15
```

The internal table length is maintained as a power of two, allowing bit masking instead of a more expensive modulo calculation.

---

### Step 4: Inspect the bucket

If the bucket is empty, a new entry is placed there.

```text
bucket[index] → new node
```

If the bucket already contains entries, a collision has occurred.

---

### Step 5: Compare keys

`HashMap` searches the bucket for an existing matching key.

A key matches when:

1. Its stored hash matches, and
2. The keys are identical or logically equal

Conceptually:

```java
existingKey == newKey
        || newKey.equals(existingKey)
```

If an equal key already exists, its value is replaced:

```java
map.put("Alice", 90);
map.put("Alice", 95);
```

The map still contains one `"Alice"` key:

```text
Alice → 95
```

`put()` returns the previous value when one existed.

---

### Step 6: Handle a collision

When no equal key is found, a new entry is added to the bucket.

The bucket may be represented as:

```text
bucket[5]
   ↓
Entry A → Entry B → Entry C
```

Hash collisions are valid and expected. `equals()` distinguishes keys that share a bucket.

---

### Step 7: Treeify a heavily collided bucket

In common Java 8+ implementations, a bucket may become a red-black tree when:

- Its entry count reaches the treeification threshold, commonly `8`.
- The table capacity is at least the minimum treeification capacity, commonly `64`.

When the table is still small, the map generally resizes instead of immediately converting the bucket into a tree.

These threshold values are implementation details rather than guarantees of the `Map` interface.

---

### Step 8: Check whether resizing is required

After inserting a new mapping, `HashMap` compares its size against the resize threshold.

```text
threshold = capacity × load factor
```

Using the common defaults:

```text
capacity   = 16
loadFactor = 0.75
threshold  = 12
```

When the number of mappings exceeds the threshold, the internal table is expanded.

---

# Lookup

## 5. What Happens During `get(key)`?

Consider:

```java
Integer score = map.get("Alice");
```

The map:

1. Calculates and spreads the key’s hash.
2. Calculates the bucket index.
3. Inspects the first entry in that bucket.
4. Compares the stored hash and key.
5. Traverses the linked chain or searches the tree when necessary.
6. Returns the matching value.
7. Returns `null` when no mapping is found.

```text
get(key)
   ↓
calculate spread hash
   ↓
find bucket
   ↓
compare candidate keys
   ↓
return matching value
```

Because `HashMap` allows null values, this result is ambiguous:

```java
map.get(key) == null
```

It can mean:

- The key is absent.
- The key exists and maps to `null`.

Use:

```java
map.containsKey(key);
```

when the distinction matters.

---

# Resizing

## 6. How Does Resizing Work?

When the threshold is exceeded, the internal bucket array is normally doubled.

```text
Old capacity: 16
New capacity: 32
```

Resizing requires redistributing existing entries across the new bucket array.

### Important correction

It is common to say that resizing “rehashes every key,” but a common Java 8+ implementation does not need to call every key’s `hashCode()` again.

Because the capacity doubles, an entry generally either:

- Remains at its old bucket index, or
- Moves to `oldIndex + oldCapacity`

The decision can be made using one additional hash bit.

Conceptually:

```text
Old index: 5

After resize:
    stays at 5
or
    moves to 5 + 16 = 21
```

Resizing is still expensive because bucket structures must be transferred, but the stored hash can be reused.

### Why choose an initial capacity?

If the expected number of mappings is known, presizing can reduce resize operations.

```java
int expectedEntries = 10_000;

Map<String, User> users = new HashMap<>(
        (int) Math.ceil(expectedEntries / 0.75)
);
```

Avoid selecting an unnecessarily large capacity because it wastes memory and can make iteration more expensive.

---

# Time Complexity

## 7. Complexity of Common Operations

| Operation         |         Average case |                              Collision-heavy case |
| ----------------- | -------------------: | ------------------------------------------------: |
| `get()`           |               `O(1)` | `O(n)` or approximately `O(log n)` for a tree bin |
| `put()`           |     Amortized `O(1)` |                              `O(n)` or `O(log n)` |
| `remove()`        |               `O(1)` |                              `O(n)` or `O(log n)` |
| `containsKey()`   |               `O(1)` |                              `O(n)` or `O(log n)` |
| `containsValue()` |               `O(n)` |                                            `O(n)` |
| Iteration         | `O(capacity + size)` |                              `O(capacity + size)` |

The actual performance depends on:

- Hash-code quality
- Collision distribution
- Load factor
- Table capacity
- Key comparison cost
- Resize frequency

---

# `equals()` and `hashCode()`

## 8. Why Must They Agree?

Hash-based collections first use `hashCode()` to locate a bucket and then use `equals()` to identify the exact key.

The required contract is:

```text
If two objects are equal according to equals(),
they must return the same hash code.
```

```java
first.equals(second) == true
```

must imply:

```java
first.hashCode() == second.hashCode()
```

The reverse is not required. Unequal objects may produce the same hash code.

---

## 9. Correct Immutable Key Example

```java
import java.util.Objects;

public final class UserKey {

    private final long tenantId;
    private final String username;

    public UserKey(long tenantId, String username) {
        this.tenantId = tenantId;
        this.username = Objects.requireNonNull(username);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof UserKey other)) {
            return false;
        }

        return tenantId == other.tenantId
                && username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, username);
    }
}
```

A record can provide the same value-based behavior more concisely:

```java
public record UserKey(
        long tenantId,
        String username
) {
}
```

---

# Mutable Keys

## 10. Why Do Mutable Keys Break `HashMap`?

A key becomes problematic when fields used by `equals()` or `hashCode()` change after insertion.

```java
public final class MutableKey {

    private int value;

    public MutableKey(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof MutableKey other)) {
            return false;
        }

        return value == other.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }
}
```

Usage:

```java
Map<MutableKey, String> map = new HashMap<>();

MutableKey key = new MutableKey(1);
map.put(key, "data");

key.setValue(2);

System.out.println(map.get(key)); // May return null
```

### What happened?

During insertion:

```text
hashCode based on value 1
        ↓
entry stored in bucket A
```

After mutation:

```text
hashCode based on value 2
        ↓
lookup searches bucket B
```

The entry still physically exists in bucket A, but normal lookup no longer reaches it.

Even removal may fail:

```java
map.remove(key); // May return null
```

### Important correction to a common bad example

This class does **not** demonstrate the problem:

```java
class BadKey {
    int value;
}
```

If it does not override `equals()` and `hashCode()`, it inherits identity-based behavior from `Object`. Changing `value` does not normally change that inherited identity hash behavior, so lookup using the same reference may still succeed.

The mutable-key bug occurs when mutable state participates in `equals()` or `hashCode()`.

### Safe rule

> Never change equality-relevant fields while an object is stored as a `HashMap` key or `HashSet` element.

Prefer immutable keys.

---

# Collision Example

## 11. Can Different Keys Have the Same Hash Code?

Yes.

```java
String first = "FB";
String second = "Ea";

System.out.println(first.equals(second));  // false
System.out.println(first.hashCode());      // same value
System.out.println(second.hashCode());     // same value
```

Both strings can coexist in a map:

```java
Map<String, Integer> map = new HashMap<>();

map.put(first, 1);
map.put(second, 2);
```

They may enter the same bucket, but `equals()` distinguishes them.

A collision is not an error. Poorly distributed collisions become a performance problem.

---

# `HashMap` vs Related Maps

## 12. Comparison

| Feature        | `HashMap`         | `ConcurrentHashMap`   | `TreeMap`                     |
| -------------- | ----------------- | --------------------- | ----------------------------- |
| Structure      | Hash table        | Concurrent hash table | Red-black tree                |
| Thread-safe    | No                | Yes                   | No                            |
| Key order      | Unspecified       | Unspecified           | Sorted                        |
| Average lookup | `O(1)`            | `O(1)`                | `O(log n)`                    |
| Null key       | One               | No                    | Normally no                   |
| Null values    | Yes               | No                    | Yes                           |
| Iterator       | Usually fail-fast | Weakly consistent     | Usually fail-fast             |
| Main use       | General lookup    | Concurrent lookup     | Sorted and range-based lookup |

### Important correction

Modern `ConcurrentHashMap` should not generally be described as using segmented locking.

- Older versions used segments.
- Java 8+ implementations use CAS, fine-grained bin synchronization, cooperative resizing, and other concurrency mechanisms.

---

# Production Use Cases

## 13. Entity Lookup

```java
Map<Long, User> usersById = new HashMap<>();

usersById.put(user.id(), user);

User selected = usersById.get(userId);
```

---

## 14. Grouping Values

```java
Map<String, List<Order>> ordersByStatus =
        new HashMap<>();

ordersByStatus
        .computeIfAbsent(
                order.status(),
                ignored -> new ArrayList<>()
        )
        .add(order);
```

---

## 15. Frequency Counting

```java
Map<String, Integer> frequencies =
        new HashMap<>();

for (String word : words) {
    frequencies.merge(
            word,
            1,
            Integer::sum
    );
}
```

---

## 16. Deduplication

A `HashSet` uses the same hashing and equality principles:

```java
Set<String> uniqueIds =
        new HashSet<>(allIds);
```

Use a set rather than a map when only uniqueness is required.

---

# Common Mistakes

## 17. Overriding `equals()` Without `hashCode()`

Incorrect:

```java
@Override
public boolean equals(Object object) {
    // Logical equality
}
```

without a corresponding `hashCode()` implementation.

This can cause equal keys to be searched in different buckets.

---

## 18. Using Mutable Equality Fields

```java
map.put(customer, value);

customer.setEmail("new@example.com");
```

If email participates in equality and hashing, the entry may become unreachable.

---

## 19. Assuming Insertion Order

`HashMap` does not guarantee insertion-order iteration.

Use:

```java
Map<String, Integer> ordered =
        new LinkedHashMap<>();
```

when insertion order matters.

---

## 20. Assuming It Is Thread-Safe

This is unsafe when the map is shared and modified concurrently:

```java
private final Map<String, Integer> counts =
        new HashMap<>();
```

Use an appropriate concurrent design:

```java
private final ConcurrentMap<String, Integer> counts =
        new ConcurrentHashMap<>();
```

However, concurrent compound operations must still use atomic APIs:

```java
counts.merge(key, 1, Integer::sum);
```

---

## 21. Using Check-Then-Act Operations

This is not atomic:

```java
if (!map.containsKey(key)) {
    map.put(key, createValue());
}
```

For concurrent maps, prefer:

```java
map.putIfAbsent(key, value);
```

or:

```java
map.computeIfAbsent(
        key,
        this::createValue
);
```

---

## 22. Assuming Hash Codes Are Unique

Hash codes are not unique identifiers.

Different keys may return the same hash code, and `HashMap` resolves that using `equals()`.

---

## 23. Returning a Constant Hash Code

This is contract-valid but inefficient:

```java
@Override
public int hashCode() {
    return 1;
}
```

All keys enter the same bucket, eliminating the main benefit of hashing.

---

## 24. Depending on Implementation Thresholds

Values such as:

```text
Treeification threshold: 8
Minimum capacity:        64
Default capacity:        16
Default load factor:     0.75
```

describe common implementations. Application correctness should not depend on undocumented internal thresholds.

---

# Interview Questions

## Question 1: What happens during `put()`?

> `HashMap` calculates and spreads the key’s hash, maps it to a bucket, and checks that bucket for an equal key. If the key exists, the value is replaced; otherwise, a new node is added. A heavily collided bucket may become a tree, and the table resizes when its threshold is exceeded.

## Question 2: Why are both `hashCode()` and `equals()` required?

> `hashCode()` selects the likely bucket, while `equals()` identifies the exact matching key. Equal keys must therefore return equal hash codes.

## Question 3: What happens after a key is mutated?

> When equality-related state changes, the key may produce a different hash and point to a different bucket. The entry remains in its original bucket, so `get()` and `remove()` may fail.

## Question 4: What changed in Java 8?

> Heavily collided buckets can be represented as red-black trees instead of remaining long linked chains. This improves collision-heavy lookup from linear behavior toward logarithmic behavior.

## Question 5: What happens during resizing?

> The table capacity normally doubles. Existing entries are redistributed, and a common implementation determines whether each entry remains at its old index or moves by the old capacity, without recalculating every key’s hash code.

## Question 6: Why must the table capacity be a power of two?

> It allows efficient bucket calculation using `(capacity - 1) & hash` and supports efficient redistribution when the capacity doubles.

## Question 7: Can `HashMap` contain nulls?

> It allows one null key and multiple null values. A null key is handled with hash zero.

---

# Short Interview Answer

> `HashMap` stores key-value pairs in an array of buckets. It spreads the key’s hash code and uses `(capacity - 1) & hash` to choose a bucket. Within that bucket, it uses `equals()` to identify the exact key. Collisions are stored in a linked structure and may become a red-black tree in heavily collided buckets. When the resize threshold is exceeded, the capacity normally doubles and entries are redistributed. Correct, stable `equals()` and `hashCode()` implementations are critical, which is why immutable keys are preferred.

---

## Related Topics

- [`equals()` and `hashCode()` Contract](../01-core-java/oop.md)
- [Advanced Collections Questions](advanced-questions.md)
- [Concurrent Collections](../04-concurrency/concurrent-collections.md)
- [Comparable vs Comparator](questions.md)
