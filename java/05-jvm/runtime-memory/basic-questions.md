# Basic Questions — JVM Memory

## Question 1: What is the difference between heap, stack, and metaspace?

The JVM uses different memory areas for different purposes.

| Memory area   | Scope                 | Stores                                                              | Common failure                      |
| ------------- | --------------------- | ------------------------------------------------------------------- | ----------------------------------- |
| **Stack**     | One per thread        | Method frames, parameters, local execution state, object references | `StackOverflowError`                |
| **Heap**      | Shared by all threads | Objects, arrays, and instance data                                  | `OutOfMemoryError: Java heap space` |
| **Metaspace** | Shared native memory  | Class metadata associated with loaded classes                       | `OutOfMemoryError: Metaspace`       |

---

### Stack

Each Java thread has its own stack. Every method invocation creates a stack frame.

A stack frame conceptually contains:

- Method parameters
- Local primitive values
- References to objects
- Intermediate calculation values
- Return information

```java
public void process() {
    int quantity = 5;
    Product product = new Product("Phone");
}
```

Conceptually:

```text
Thread stack
├── quantity = 5
└── product reference ───────────┐
                                │
Heap                            │
└── Product object <────────────┘
```

When the method returns, its stack frame is removed automatically. Stack frames are not reclaimed by the garbage collector.

Each thread has a separate stack, so creating many platform threads also consumes significant native memory.

---

### Heap

The heap is shared by all application threads and stores objects and arrays.

```java
Product product = new Product("Phone");
int[] values = new int[100];
```

Both the `Product` object and the array are conceptually allocated on the heap.

The garbage collector reclaims heap objects that are no longer reachable from GC roots.

Because the heap is shared, mutable heap objects accessed by several threads may require:

- Synchronization
- Locks
- Atomic operations
- Immutability
- Concurrent collections

---

### Metaspace

Metaspace stores class-related metadata, including information about:

- Classes
- Methods
- Fields
- Interfaces
- Runtime constant pools
- Class-loader relationships

Metaspace replaced PermGen starting with Java 8 and uses native memory rather than the regular Java heap.

A Metaspace leak commonly occurs when old class loaders remain reachable.

```text
Long-lived thread
→ ThreadLocal value
→ old application class loader
→ loaded classes cannot be unloaded
→ Metaspace continues growing
```

### Important nuance

The rule:

```text
Local variables live on the stack.
Objects live on the heap.
```

is a useful conceptual model, not an absolute physical guarantee. The JIT compiler may optimize allocations through escape analysis, scalar replacement, and register allocation.

### Interview-ready answer

> Each thread has its own stack containing method frames and local execution state. The heap is shared and stores objects and arrays managed by the garbage collector. Metaspace stores class metadata in native memory and is primarily reclaimed when class loaders and their classes can be unloaded.

---

## Question 2: What is garbage collection?

Garbage collection is the JVM process of identifying and reclaiming heap memory occupied by objects that are no longer reachable.

```java
Product product = new Product("Phone");
product = null;
```

Setting the reference to `null` does not immediately delete the object. It merely removes one reference.

The object becomes eligible for garbage collection only when no reachable reference can access it.

### GC roots commonly include

- References in active thread stacks
- Static fields
- JNI references
- Active monitors
- JVM-internal references

Conceptually:

```text
GC roots
   ↓
Reachable objects
   ↓
Objects reachable from those objects
```

Anything not reachable through this graph may be reclaimed.

### Important distinction

Garbage collection does not guarantee immediate reclamation. The JVM decides when a collection should run.

It also does not close application resources such as:

- Files
- Database connections
- Sockets
- HTTP responses

Use explicit cleanup:

```java
try (InputStream input =
        Files.newInputStream(path)) {

    process(input);
}
```

### Interview-ready answer

> Garbage collection automatically reclaims heap memory from objects that are no longer reachable from GC roots. It manages memory, but it does not replace explicit cleanup of files, sockets, database connections, or other external resources.

---

## Question 3: What is a memory leak in Java?

A Java memory leak occurs when objects that are no longer logically needed remain reachable, preventing the garbage collector from reclaiming them.

```java
public final class UnsafeCache {

    private static final Map<String, Object> CACHE =
            new HashMap<>();

    public static void put(
            String key,
            Object value
    ) {
        CACHE.put(key, value);
    }
}
```

Because the static map remains reachable, every object stored in it also remains reachable.

```text
Class
→ static map
→ entries
→ cached objects
```

If entries are continuously added and never removed, heap usage keeps growing.

### Common causes

- Unbounded static collections
- Caches without size or expiration limits
- Listeners that are registered but never removed
- `ThreadLocal` values left in thread-pool workers
- Unbounded executor queues
- Scheduled tasks that are never cancelled
- Tasks capturing large object graphs
- Old class loaders retained after redeployment
- Sessions that never expire

### Memory leak vs high allocation rate

These are different problems.

#### Memory leak

```text
Heap grows
→ GC runs
→ post-GC heap remains high
→ post-GC baseline continues rising
```

#### High allocation rate

```text
Heap grows quickly
→ GC runs
→ heap returns to a stable baseline
```

A high allocation rate can cause frequent GC without being a leak.

### Interview-ready answer

> A Java memory leak occurs when unused objects remain reachable through references such as static collections, caches, listeners, thread locals, or queues. GC cannot reclaim them because they are still reachable, even though the business logic no longer needs them.

---

## Question 4: What does “Stop the World” mean?

A **Stop-the-World**, or STW, pause is a JVM phase during which application threads are paused at safe execution points.

During the pause, application code does not continue normal execution.

```text
Application threads running
        ↓
Safepoint requested
        ↓
Application threads pause
        ↓
JVM operation executes
        ↓
Application threads resume
```

Stop-the-world pauses can occur during:

- Garbage-collection phases
- Some class redefinition activities
- Certain JVM diagnostic operations
- Deoptimization or related VM operations

### Important correction

Garbage collection is not always entirely stop-the-world.

Modern collectors such as G1 and ZGC perform substantial work concurrently with application threads, but they still require some pause phases.

### Production impact

A long STW pause can cause:

- API latency spikes
- Request timeouts
- Missed heartbeat deadlines
- Kafka consumer delays
- Temporary queue growth
- Apparent downstream slowness

Example:

```text
Normal API latency: 50 ms
GC pause:          2 seconds

Requests active during the pause
→ may experience approximately 2 seconds of additional latency
```

### How to investigate

Enable GC and safepoint logging:

```bash
java \
  -Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags \
  -jar application.jar
```

Correlate pause timestamps with:

- P95 and P99 latency
- CPU usage
- Thread-pool queues
- Request timeouts
- GC events
- Java Flight Recorder data

### Interview-ready answer

> Stop-the-world means application threads are paused while the JVM performs a particular operation. Some GC phases require these pauses, although modern collectors perform much of their work concurrently. I diagnose their impact by correlating GC and safepoint events with application latency.

---

## Question 5: What is `OutOfMemoryError`?

`OutOfMemoryError` occurs when the JVM cannot satisfy a memory or resource allocation request.

It does not always mean that the Java heap is full.

Common forms include:

```text
OutOfMemoryError: Java heap space
OutOfMemoryError: GC overhead limit exceeded
OutOfMemoryError: Metaspace
OutOfMemoryError: Direct buffer memory
OutOfMemoryError: unable to create native thread
```

---

### Java heap space

```text
java.lang.OutOfMemoryError: Java heap space
```

Possible causes:

- Heap memory leak
- Heap too small for the legitimate working set
- Unbounded cache
- Unbounded task queue
- Very large object or array
- Excessive in-memory aggregation
- Increased production traffic

---

### GC overhead limit exceeded

```text
java.lang.OutOfMemoryError:
GC overhead limit exceeded
```

The JVM is spending excessive time collecting garbage while reclaiming very little memory.

This often means most heap objects remain reachable.

---

### Metaspace

```text
java.lang.OutOfMemoryError: Metaspace
```

Possible causes:

- Class-loader leak
- Repeated application redeployment
- Unbounded proxy or class generation
- Metaspace limit configured too low

---

### Direct buffer memory

```text
java.lang.OutOfMemoryError:
Direct buffer memory
```

This indicates exhaustion of direct or native buffer capacity rather than ordinary Java heap space.

---

### Unable to create native thread

```text
java.lang.OutOfMemoryError:
unable to create native thread
```

Possible causes:

- Too many platform threads
- Operating-system process or thread limits
- Native memory exhaustion
- Excessive stack size per thread

---

## How would you investigate an OOME?

### 1. Read the exact error message

Do not assume every OOME is a heap problem.

### 2. Enable automatic heap dumps

```bash
java \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/application \
  -jar application.jar
```

### 3. Capture relevant evidence

- Heap dump
- GC logs
- Java Flight Recorder
- Thread dump
- Process memory
- Container memory
- Queue and cache sizes
- Thread count
- Metaspace usage

### 4. Analyze paths to GC roots

Look for:

- Static collections
- Large caches
- Queued tasks
- `ThreadLocal` values
- Listeners
- Session objects
- Class-loader retention

### Interview-ready answer

> `OutOfMemoryError` means the JVM could not allocate a required memory or resource. I first inspect the exact message because the problem may involve heap, Metaspace, direct memory, GC overhead, or native-thread creation. I preserve heap, GC, thread, and process-memory evidence before changing JVM limits.

---

# Quick Comparison

| Concept              | Meaning                                             |
| -------------------- | --------------------------------------------------- |
| Heap                 | Shared memory for objects and arrays                |
| Stack                | Per-thread method frames and local execution state  |
| Metaspace            | Native memory for class metadata                    |
| Garbage collection   | Reclaims unreachable heap objects                   |
| Memory leak          | Unneeded objects remain reachable                   |
| Stop-the-world       | Application threads pause for a JVM operation       |
| `StackOverflowError` | One thread exhausts its stack                       |
| `OutOfMemoryError`   | A memory or resource allocation cannot be satisfied |

---

# Common Mistakes

## Assuming every local object is stored on the stack

```java
Product product = new Product();
```

The local reference belongs to the method’s execution state, while the object is conceptually heap allocated.

---

## Assuming GC prevents memory leaks

GC can reclaim only unreachable objects. It cannot determine whether a reachable object is still useful to the business.

---

## Treating every OOME as heap exhaustion

Always inspect the complete message.

---

## Increasing `-Xmx` before investigating

A larger heap may delay the failure while leaving less memory for:

- Metaspace
- Direct buffers
- Thread stacks
- Code cache
- Native libraries
- GC structures

---

## Using an unbounded collection as a cache

```java
private static final Map<String, Result> CACHE =
        new ConcurrentHashMap<>();
```

Thread safety does not provide eviction or bounded memory.

---

# Short Interview Summary

> The stack is private to each thread and contains method frames and local execution state. The heap is shared and stores objects and arrays managed by the garbage collector. Metaspace stores class metadata in native memory. GC reclaims unreachable objects, but reachable objects can still create memory leaks. A stop-the-world pause temporarily suspends application threads, while `OutOfMemoryError` can indicate heap, Metaspace, direct-memory, native-thread, or GC-overhead exhaustion.
