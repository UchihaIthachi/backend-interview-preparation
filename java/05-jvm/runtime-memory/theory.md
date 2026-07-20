# JVM Memory: Heap, Stack, Metaspace

## 1. Definition

- **Stack** — per-thread, stores local variables and method call frames.
- **Heap** — shared across threads, stores all objects and arrays.
- **Metaspace** — stores class metadata (replaced PermGen since Java 8), grows
  in native memory rather than the heap.

## 2. Why it exists

Separating these lets the JVM manage memory efficiently: stack memory is
cheap to allocate/free (just moving a pointer, freed automatically on method
return) while heap memory needs garbage collection since object lifetimes are
unpredictable.

## 3. How it works

```text
new Object() -> allocated on the heap
int x = 5    -> stored directly on the calling thread's stack frame
Each thread  -> has its own stack; StackOverflowError if it recurses too deep
Heap         -> shared; OutOfMemoryError if objects can't be reclaimed fast enough
Metaspace    -> class definitions; can also OOM if classes are loaded unboundedly
               (e.g., a classloader leak from dynamic proxies never released)
```

## 4. Code example — common leak pattern

```java
class Cache {
    private static final Map<String, Object> cache = new HashMap<>(); // static!

    static void put(String key, Object value) {
        cache.put(key, value); // never evicted -> unbounded growth -> heap OOM
    }
}
```

## 5. Production use case

Diagnosing `OutOfMemoryError` in production typically starts with a heap dump
(`jmap` or automatic dump-on-OOM) analyzed in a tool like Eclipse MAT, looking
for large retained object graphs — often static collections, unbounded caches,
or listener registrations that are never cleaned up.

## 6. Common mistakes

- Using static collections as ad-hoc caches with no eviction policy.
- Registering listeners/callbacks and never removing them, keeping otherwise
  dead objects reachable.
- Confusing `StackOverflowError` (too much recursion / stack depth) with
  `OutOfMemoryError` (heap or metaspace exhausted) — they have different causes
  and different fixes.

## 7. Trade-offs

Not really a trade-off question — this is foundational knowledge for debugging
memory issues. The practical trade-off is bounded caches (predictable memory,
possible cache misses) vs unbounded caches (best hit rate, risk of OOM).

## 8. Interview questions

1. Heap vs stack vs metaspace — what lives where?
2. What causes `StackOverflowError` vs `OutOfMemoryError`?
3. How would you investigate a suspected memory leak in production?
4. Name three common causes of memory leaks in a long-running Java service.

## 9. Short interview answer

"The stack holds per-thread local variables and call frames and is cheap to
manage since it's freed automatically on method return. The heap holds all
objects and needs garbage collection since object lifetimes vary. Most
production memory leaks I've seen trace back to static collections or
unbounded caches, or listeners that are registered but never removed."

## 10. Related topics

- [Garbage collection](garbage-collection.md)
