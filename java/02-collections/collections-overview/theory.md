# Collections Framework: List, Set, Map, Queue

## 1. Definition

Java's collections framework provides standard interfaces (`List`, `Set`, `Map`,
`Queue`) and implementations (`ArrayList`, `HashSet`, `HashMap`, `ArrayDeque`, etc.)
for storing and manipulating groups of objects.

## 2. Why it exists

Different access patterns need different underlying structures — the framework
lets you pick a data structure by contract (ordered? unique? key-value?) rather
than reinventing arrays/linked structures for every use case.

## 3. How it works — choosing the right structure

```text
ArrayList     -> fast random access, ordered API results
LinkedList    -> fast insert/remove at ends, rarely the best default choice
HashSet       -> unique values, no order guarantee, O(1) average lookup
TreeSet       -> unique values, sorted order, O(log n) operations
HashMap       -> key-value lookup table, O(1) average
TreeMap       -> sorted key-value store, O(log n)
ArrayDeque    -> stack or queue, faster than legacy Stack/LinkedList
PriorityQueue -> job scheduling, always pop smallest/largest first
```

## 4. Code example

```java
Map<String, List<String>> permissionsByRole = new HashMap<>();
permissionsByRole.computeIfAbsent("ADMIN", k -> new ArrayList<>()).add("DELETE_USER");

Set<String> uniquePermissionIds = new HashSet<>(allPermissionIds);

Queue<Task> taskQueue = new ArrayDeque<>();
taskQueue.offer(new Task("send-email"));

PriorityQueue<Job> jobs = new PriorityQueue<>(Comparator.comparingInt(Job::priority));
```

## 5. Production use case

A rate limiter or job scheduler commonly uses a `PriorityQueue`; a permission
system commonly uses a `HashSet` for uniqueness checks; a lookup cache for
entities by ID commonly uses a `HashMap`.

## 6. Common mistakes

- Defaulting to `LinkedList` for a queue when `ArrayDeque` is faster and simpler.
- Using `ArrayList` for frequent inserts/removals in the middle of a large list.
- Iterating a `HashMap` expecting insertion order (use `LinkedHashMap` for that).
- Modifying a collection while iterating without an `Iterator.remove()` (causes
  `ConcurrentModificationException` — this is "fail-fast" iteration).

## 7. Trade-offs

| Structure | Best for | Weakness |
|---|---|---|
| ArrayList | Random access | Slow middle insert/remove |
| LinkedList | Sequential insert/remove | Poor random access, extra memory |
| HashSet/HashMap | O(1) average lookup | No order guarantee |
| TreeSet/TreeMap | Sorted iteration | O(log n) instead of O(1) |

## 8. Interview questions

1. `ArrayList` vs `LinkedList` — when would each actually win in practice?
2. Why is `ArrayDeque` usually preferred over the legacy `Stack` class?
3. What is fail-fast iteration and what triggers `ConcurrentModificationException`?
4. When would you reach for `TreeMap` over `HashMap`?

## 9. Short interview answer

"I pick the collection based on the access pattern I need: HashMap for O(1)
lookups, TreeMap when I need sorted iteration, ArrayDeque over legacy Stack for
both stack and queue use cases, and PriorityQueue whenever I need to always
process the smallest or highest-priority item next."

## 10. Related topics

- [HashMap internals](hashmap-internals.md)
- [Comparable vs Comparator](questions.md)
