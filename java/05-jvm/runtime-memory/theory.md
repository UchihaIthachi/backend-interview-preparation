# JVM Memory: Heap, Stack, and Metaspace

## 1. Definition

The JVM uses several memory areas to execute Java applications.

### Stack

Each thread has its own stack. It stores **stack frames** created for active method calls.

A stack frame conceptually contains:

- Local variables
- Method parameters
- Intermediate computation values
- Return information
- References to heap objects

### Heap

The heap is shared by all application threads and is the main runtime area in which objects and arrays are allocated.

It is managed by the garbage collector.

### Metaspace

Metaspace stores class-related metadata, such as:

- Class structure
- Method metadata
- Field metadata
- Runtime constant-pool information
- Class-loader-associated metadata

Metaspace replaced PermGen starting with Java 8 and uses native memory rather than the regular Java heap.

---

# 2. Why Are These Areas Separated?

Different data has different lifetimes and ownership requirements.

### Stack data

Method-local execution state normally follows a predictable last-in, first-out lifecycle:

```text
Method called
→ stack frame created
→ method executes
→ method returns
→ stack frame removed
```

This makes stack-frame allocation and removal relatively inexpensive.

### Heap data

Object lifetimes are less predictable:

```text
Object created in one method
→ returned to another method
→ stored in a collection
→ shared with other threads
→ becomes unreachable later
```

The JVM therefore uses garbage collection to determine when heap objects are no longer reachable.

### Metaspace data

Class metadata usually remains available while its defining class loader remains reachable.

```text
Class loader created
→ classes loaded
→ metadata stored in Metaspace
→ class loader becomes unreachable
→ classes may be unloaded
→ metadata may be reclaimed
```

---

# 3. Memory Layout Overview

```text
JVM process
│
├── Java heap
│   ├── Objects
│   ├── Arrays
│   └── Instance data
│
├── Per-thread stacks
│   ├── Method frames
│   ├── Local variables
│   ├── Parameters
│   └── Object references
│
├── Metaspace
│   └── Class metadata
│
├── Code cache
│   └── JIT-compiled machine code
│
├── Direct/native memory
│   ├── Direct byte buffers
│   ├── Native libraries
│   └── JVM internal structures
│
└── Thread-native memory
    └── Native stack for each platform thread
```

The heap, stack, and Metaspace are important, but they do not represent all memory used by a JVM process.

---

# 4. What Lives on the Stack?

Consider:

```java
public void calculate() {
    int quantity = 5;
    Product product = new Product("Phone");
}
```

Conceptually:

```text
Thread stack frame
├── quantity = 5
└── product reference ─────────────┐
                                  │
Heap                              │
└── Product object <──────────────┘
```

The local primitive value and object reference are associated with the method’s stack frame, while the `Product` object is normally allocated on the heap.

## Important nuance

Statements such as these are useful conceptual rules:

```text
Local primitives → stack
Objects → heap
```

But they are not absolute physical guarantees.

The JIT compiler may optimize code through:

- Escape analysis
- Scalar replacement
- Register allocation
- Stack allocation-like optimizations
- Elimination of unused allocations

The Java language specifies observable behavior, not the exact physical location of every value.

### Interview-safe wording

> Local execution state belongs to a thread’s stack frame, while dynamically created objects are conceptually allocated on the shared heap. The JVM may optimize the physical representation as long as program behavior remains correct.

---

# 5. What Lives on the Heap?

The heap commonly contains:

- Ordinary objects
- Arrays
- Collection elements
- Instance fields
- Strings
- Records
- Lambda objects when materialized
- Objects referenced by static fields
- Objects shared among threads

Example:

```java
List<Order> orders = new ArrayList<>();
orders.add(new Order("ORD-101"));
```

Conceptually:

```text
Stack
└── orders reference
        ↓
Heap
├── ArrayList object
├── backing array
└── Order object
```

A local variable can disappear when the method returns while the object remains alive because another reference still points to it.

---

# 6. What Lives in Metaspace?

When a class is loaded, the JVM stores metadata describing that class.

```java
public final class PaymentService {

    private final PaymentRepository repository;

    public void process() {
    }
}
```

Metaspace contains metadata representing concepts such as:

- The class name
- Its superclass and interfaces
- Declared methods
- Declared fields
- Method signatures
- Runtime constant-pool information
- Class-loader relationships

The corresponding Java `Class<?>` object is itself a heap object, while much of the underlying VM class metadata is stored in Metaspace.

---

# 7. PermGen vs Metaspace

| PermGen                                                  | Metaspace                                    |
| -------------------------------------------------------- | -------------------------------------------- |
| Used before Java 8                                       | Used from Java 8 onward                      |
| Part of JVM-managed memory with a fixed configured limit | Uses native memory                           |
| Configured with older PermGen flags                      | Commonly limited with `-XX:MaxMetaspaceSize` |
| Could fail with `PermGen space`                          | Can fail with `Metaspace`                    |

Metaspace can grow dynamically, but it is not unlimited in practice. Growth is constrained by:

- Configured limits
- Container memory
- Operating-system memory
- Native-memory availability

Optional limit:

```bash
-XX:MaxMetaspaceSize=512m
```

A very low limit can cause unnecessary class-loading failures. An unlimited setting can allow a class-loader leak to consume large amounts of native memory.

---

# 8. Stack Lifecycle

Each method invocation creates a frame.

```java
public void first() {
    second();
}

public void second() {
    third();
}

public void third() {
    System.out.println("done");
}
```

Conceptually:

```text
Top of stack
┌───────────────┐
│ third() frame │
├───────────────┤
│ second()      │
├───────────────┤
│ first()       │
├───────────────┤
│ main()        │
└───────────────┘
Bottom of stack
```

As methods return:

```text
third() removed
→ second() removed
→ first() removed
```

No garbage collection is required to remove completed stack frames.

---

# 9. What Causes `StackOverflowError`?

`StackOverflowError` occurs when a thread requires more stack space than is available.

The most common cause is uncontrolled recursion:

```java
public void recurse() {
    recurse();
}
```

Execution:

```text
recurse()
→ recurse()
→ recurse()
→ recurse()
→ stack limit reached
→ StackOverflowError
```

Another example is recursion without adequate termination:

```java
public long factorial(long number) {
    return number * factorial(number - 1);
}
```

This implementation has no base case.

Correct:

```java
public long factorial(long number) {
    if (number <= 1) {
        return 1;
    }

    return number * factorial(number - 1);
}
```

Deep but valid recursion can also exhaust the stack.

## Possible remedies

- Fix missing termination conditions.
- Replace recursion with iteration.
- Reduce call depth.
- Avoid excessively large stack frames.
- Increase stack size only after confirming the design is valid.

Example JVM option:

```bash
-Xss1m
```

Increasing stack size also increases native-memory consumption per platform thread.

---

# 10. What Causes `OutOfMemoryError`?

`OutOfMemoryError` is not limited to the Java heap.

Common messages include:

```text
Java heap space
GC overhead limit exceeded
Metaspace
Compressed class space
Direct buffer memory
Unable to create native thread
```

Each points to a different resource problem.

---

## Java heap space

```text
java.lang.OutOfMemoryError: Java heap space
```

Possible causes:

- Memory leak
- Heap too small for the legitimate live set
- Unbounded cache
- Unbounded task queue
- Large payload
- Excessive in-memory aggregation
- Workload growth

---

## GC overhead limit exceeded

```text
java.lang.OutOfMemoryError:
GC overhead limit exceeded
```

The JVM is spending excessive time garbage collecting while recovering very little memory.

This often means:

- The heap is almost full.
- Most objects remain reachable.
- Collection cannot create enough free space.

---

## Metaspace

```text
java.lang.OutOfMemoryError: Metaspace
```

Possible causes:

- Class-loader leak
- Unbounded runtime class generation
- Repeated application redeployment
- Dynamic proxy generation
- Bytecode-generation frameworks
- Metaspace limit too low

---

## Direct buffer memory

```text
java.lang.OutOfMemoryError:
Direct buffer memory
```

Possible sources include:

- `ByteBuffer.allocateDirect()`
- Networking frameworks
- Large native buffers
- Delayed cleanup
- Excessive direct-memory allocation

This is not regular heap exhaustion.

---

## Unable to create native thread

```text
java.lang.OutOfMemoryError:
unable to create native thread
```

Possible causes:

- Too many platform threads
- Native-memory exhaustion
- Operating-system thread limits
- Large per-thread stack configuration
- Unbounded thread creation

---

# 11. Heap Leak Example: Unbounded Static Cache

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UnsafeCache {

    private static final Map<String, Object> CACHE =
            new ConcurrentHashMap<>();

    public static void put(
            String key,
            Object value
    ) {
        CACHE.put(key, value);
    }
}
```

The use of `ConcurrentHashMap` makes concurrent access safer, but it does not make the cache bounded.

```text
Static field
→ map
→ entries
→ values
```

Because the static field remains reachable through the loaded class, the cached values remain reachable as well.

If entries are never removed:

```text
New keys continuously added
→ retained heap grows
→ GC cannot reclaim values
→ eventual OutOfMemoryError
```

## Better cache design

Define:

- Maximum size
- Expiration
- Eviction
- Explicit invalidation
- Cache ownership
- Failure behavior
- Metrics

A simplified bounded cache concept:

```java
public final class BoundedCache<K, V> {

    private final Map<K, V> values =
            new LinkedHashMap<>();

    private final int maximumSize;

    public BoundedCache(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    public synchronized void put(K key, V value) {
        values.put(key, value);

        if (values.size() > maximumSize) {
            K oldestKey =
                    values.keySet()
                            .iterator()
                            .next();

            values.remove(oldestKey);
        }
    }
}
```

For production applications, use a well-tested cache library or managed cache rather than a simplistic custom implementation.

---

# 12. Listener Leak

```java
eventPublisher.register(listener);
```

A long-lived publisher may retain the listener:

```text
Publisher
→ listener
→ service
→ large object graph
```

If the listener is no longer needed but never removed, the entire graph can remain reachable.

Use an explicit lifecycle:

```java
Subscription subscription =
        eventPublisher.subscribe(listener);

try {
    useSubscription();
} finally {
    subscription.close();
}
```

Explicit unregistration is usually clearer than relying solely on weak references.

---

# 13. ThreadLocal Leak

A pool worker may remain alive for the entire application lifetime.

```java
private static final ThreadLocal<RequestContext> CONTEXT =
        new ThreadLocal<>();
```

Unsafe:

```java
public void process(RequestContext context) {
    CONTEXT.set(context);
    handleRequest();
}
```

The worker can retain the context after request processing.

Correct:

```java
public void process(RequestContext context) {
    try {
        CONTEXT.set(context);
        handleRequest();
    } finally {
        CONTEXT.remove();
    }
}
```

---

# 14. Unbounded Executor Queue

```java
ExecutorService executor =
        Executors.newFixedThreadPool(10);
```

A fixed pool limits workers but normally uses an unbounded queue.

Under sustained overload:

```text
Tasks arrive faster than workers complete them
→ queue grows
→ task objects remain reachable
→ captured payloads remain reachable
→ latency rises
→ possible heap exhaustion
```

Prefer explicit capacity:

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                10,
                20,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

---

# 15. Class-Loader Leak and Metaspace

A class is normally eligible for unloading only when its defining class loader is unreachable.

Possible leak:

```text
Old application class loader
        ↑
ThreadLocal value
        ↑
long-lived container thread
```

Even after redeployment, a container worker can retain an object loaded by the old application class loader.

This can retain:

- Classes
- Static fields
- Class metadata
- Application objects
- Related native resources

Repeated redeployments may then produce:

```text
Metaspace growth
→ old class loaders remain
→ classes cannot unload
→ OutOfMemoryError: Metaspace
```

---

# 16. Stack vs Heap vs Metaspace

| Area                | Ownership           | Stores                                    | Reclaimed by                   |
| ------------------- | ------------------- | ----------------------------------------- | ------------------------------ |
| Stack               | Per thread          | Frames, local execution state, references | Frame removal on method return |
| Heap                | Shared              | Objects and arrays                        | Garbage collector              |
| Metaspace           | JVM/native memory   | Class metadata                            | Class unloading                |
| Code cache          | JVM/native memory   | JIT-compiled code                         | JVM code-cache management      |
| Direct memory       | Native              | Direct buffers and native data            | Cleaner/resource lifecycle     |
| Native thread stack | Per platform thread | Native execution stack                    | Thread termination             |

---

# 17. `StackOverflowError` vs `OutOfMemoryError`

| `StackOverflowError`                 | `OutOfMemoryError`                                                  |
| ------------------------------------ | ------------------------------------------------------------------- |
| One thread exhausts its stack        | A memory or resource area cannot satisfy allocation                 |
| Often caused by deep recursion       | Often caused by retention, insufficient limits, or unbounded growth |
| Usually affects one thread initially | May destabilize the entire JVM                                      |
| Inspect recursive call stack         | Inspect OOME message and relevant memory area                       |
| Fix algorithm or call depth          | Fix retention, capacity, allocation, or configuration               |

A stack failure and heap failure require different investigations.

---

# Production Investigation

## 18. Start with the Exact Error

Do not stop at:

```text
OutOfMemoryError
```

Capture the complete message:

```text
Java heap space
Metaspace
Direct buffer memory
Unable to create native thread
```

That determines which memory area to investigate.

---

## 19. Enable Automatic Heap Dumps

```bash
java \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/my-app \
  -jar application.jar
```

Ensure:

- The directory exists.
- The JVM user can write to it.
- Sufficient disk space is available.
- Dumps containing sensitive data are protected.

Heap dumps may contain passwords, tokens, customer information, and request payloads.

---

## 20. Capture a Heap Dump Manually

Prefer `jcmd` for a live JVM:

```bash
jcmd <pid> GC.heap_dump /tmp/application.hprof
```

A dump may pause the application and consume substantial disk space, so production collection should be planned carefully.

---

## 21. Analyze the Heap Dump

Tools such as Eclipse Memory Analyzer can help inspect:

- Dominator tree
- Retained heap
- Shallow heap
- Paths to GC roots
- Large collections
- Duplicate strings or byte arrays
- Thread-local values
- Queued tasks
- Class-loader retention

### Shallow size

Memory used directly by one object.

### Retained size

Memory that could potentially be reclaimed if that object became unreachable.

An object with a small shallow size can retain a huge object graph.

```text
Small cache object
→ map
→ one million entries
→ large retained size
```

---

## 22. Understand GC Roots

Garbage collection starts from objects considered immediately reachable.

Typical GC roots include:

- Active thread stack references
- Static fields
- JNI references
- JVM internal references
- Active monitors

When investigating a leak, ask:

> Which GC root keeps this object reachable?

A path-to-GC-roots report may show:

```text
System class
→ static cache
→ map
→ session
→ customer data
```

That reveals why collection cannot reclaim the object.

---

## 23. Compare Live-Set Growth

A common leak pattern is:

```text
Heap rises
→ GC runs
→ heap falls only slightly
→ post-GC baseline continues rising
```

Under a stable workload:

```text
Increasing post-GC live set
→ likely retained-memory growth
```

By contrast:

```text
Heap rises rapidly
→ GC returns it to a stable baseline
```

may indicate high allocation churn rather than a leak.

---

## 24. Capture Native Memory Information

When heap usage looks normal but process memory is high, investigate native memory.

Useful JVM option at startup:

```bash
-XX:NativeMemoryTracking=summary
```

Then inspect:

```bash
jcmd <pid> VM.native_memory summary
```

Potential native consumers include:

- Thread stacks
- Metaspace
- Code cache
- Direct buffers
- GC internal data
- JNI libraries
- JVM internal structures

Native Memory Tracking adds overhead and must generally be enabled when the JVM starts.

---

## 25. Inspect Class-Loader Statistics

```bash
jcmd <pid> VM.classloader_stats
```

Also inspect class histograms:

```bash
jcmd <pid> GC.class_histogram
```

Look for:

- Many class-loader instances
- Duplicate loaded application classes
- Increasing generated proxy classes
- Old deployment class loaders
- Unexpected class-count growth

---

## 26. Use Java Flight Recorder

```bash
jcmd <pid> JFR.start \
  name=memory-investigation \
  duration=5m \
  filename=/tmp/memory-investigation.jfr
```

JFR can help identify:

- Allocation hotspots
- Object-allocation rate
- Thread creation
- GC activity
- Lock contention
- File and socket activity
- Long-lived allocation patterns

---

# Common Memory-Leak Causes

## 27. Unbounded static collections

```java
private static final Map<String, Object> VALUES =
        new HashMap<>();
```

---

## 28. Unbounded caches

Thread safety and eviction are separate concerns.

```java
private final ConcurrentMap<String, Result> cache =
        new ConcurrentHashMap<>();
```

---

## 29. Listeners that are never removed

```java
publisher.addListener(listener);
```

---

## 30. ThreadLocal values in pool workers

```java
CONTEXT.set(value);
// Missing remove()
```

---

## 31. Unbounded task or message queues

```java
new LinkedBlockingQueue<>();
```

---

## 32. Executors and scheduler threads never shut down

```java
Executors.newScheduledThreadPool(4);
```

---

## 33. Tasks capturing large object graphs

```java
LargeReport report = loadReport();

executor.submit(() ->
        process(report)
);
```

The queued task retains `report` until it executes or is removed.

---

## 34. Class-loader leaks

Common in:

- Hot redeployment
- Plugin systems
- Dynamic class generation
- Application servers
- Script engines

---

## 35. Native resource retention

Examples:

- Unclosed files
- Sockets
- Direct buffers
- Native library allocations
- Database and HTTP connections

These are not always visible as dominant Java heap objects.

---

# Common Mistakes

## Mistake 1: Saying all local variables are physically on the stack

That is a conceptual model, not an absolute JVM implementation rule.

---

## Mistake 2: Assuming all memory problems are heap problems

A JVM can exhaust:

- Heap
- Metaspace
- Direct memory
- Native memory
- Thread capacity
- Code cache

---

## Mistake 3: Increasing `-Xmx` before investigating

A larger heap may:

- Delay the failure
- Increase pause duration
- Hide a leak temporarily
- Leave too little native memory
- Cause container termination

---

## Mistake 4: Treating `ConcurrentHashMap` as a cache

It provides concurrency safety, not eviction, expiration, or bounded memory.

---

## Mistake 5: Assuming GC collects everything no longer useful to the business

GC collects unreachable objects, not objects the application has logically finished using.

```text
Still reachable
but no longer useful
→ not garbage to the JVM
```

---

## Mistake 6: Using an unbounded cache for the highest possible hit rate

The apparent cache benefit can eventually destabilize the service.

A bounded cache trades some misses for predictable memory usage.

---

# Trade-Offs

## Bounded vs unbounded cache

| Bounded cache                  | Unbounded cache              |
| ------------------------------ | ---------------------------- |
| Predictable maximum size       | Potentially unlimited growth |
| May evict useful entries       | Higher hit rate initially    |
| Requires eviction strategy     | Simple to implement          |
| Safer under changing workloads | Risk of heap exhaustion      |
| Easier capacity planning       | Difficult long-term behavior |

## Larger vs smaller thread stacks

| Larger stack                    | Smaller stack                     |
| ------------------------------- | --------------------------------- |
| Supports deeper call chains     | Supports more platform threads    |
| More native memory per thread   | Greater `StackOverflowError` risk |
| Useful for legitimate recursion | Better native-memory density      |

---

# Interview Questions

## Question 1: What is stored in the stack, heap, and Metaspace?

> Each thread’s stack contains method frames and local execution state. The shared heap contains objects and arrays. Metaspace uses native memory to store class metadata associated with loaded classes and class loaders.

## Question 2: Are all objects always physically allocated on the heap?

> Objects are conceptually heap allocated, but the JIT may eliminate or transform allocations through optimizations such as escape analysis and scalar replacement. Java does not guarantee a physical location for every value.

## Question 3: What causes `StackOverflowError`?

> It usually occurs when a thread exceeds its stack capacity because of infinite recursion, excessive recursion depth, or unusually large stack usage.

## Question 4: What causes `OutOfMemoryError`?

> It occurs when the JVM cannot satisfy a memory or resource allocation. The exact message may indicate Java heap, Metaspace, direct memory, native-thread creation, compressed class space, or excessive GC overhead.

## Question 5: Does garbage collection prevent memory leaks?

> No. GC reclaims unreachable objects. Objects retained accidentally by static fields, caches, queues, listeners, or thread locals remain reachable and therefore cannot be collected.

## Question 6: How would you investigate a suspected heap leak?

> I enable a heap dump on OOME, compare post-GC live-set growth, analyze a heap dump using retained size and paths to GC roots, and correlate the findings with cache, queue, traffic, and thread metrics.

## Question 7: How would you investigate Metaspace exhaustion?

> I inspect class-loader statistics, class counts, generated classes, deployment history, and paths retaining old class loaders. Common causes include class-loader leaks and unbounded runtime class generation.

## Question 8: Why can too many threads cause an OOME?

> Every platform thread consumes native resources, including stack memory. Unbounded thread creation can exhaust native memory or operating-system thread limits and produce `unable to create native thread`.

## Question 9: What is retained heap?

> Retained heap estimates how much memory could become reclaimable if a particular object were removed. It includes the object and other objects reachable only through it.

## Question 10: What are three common leaks in a long-running Java service?

> Common examples are unbounded caches or queues, `ThreadLocal` values left in pool workers, and listener or callback registrations that are never removed.

---

# Short Interview Answer

> A Java thread has its own stack containing method frames and local execution state. The heap is shared and stores objects and arrays, while Metaspace stores class metadata in native memory. `StackOverflowError` typically comes from excessive call depth, whereas `OutOfMemoryError` can involve the heap, Metaspace, direct memory, native memory, or excessive thread creation. For production memory incidents, I start with the exact error message, preserve heap and GC evidence, analyze retained objects and paths to GC roots, and check common retention sources such as static collections, unbounded caches, queues, listeners, and thread locals.

## Related Topics

- [Garbage Collection](garbage-collection.md)
- [ThreadLocal](thread-local.md)
- [Thread Lifecycle](thread-lifecycle.md)
- [ExecutorService](executor-service.md)
- [Memory Leak Analysis](memory-leak-analysis.md)
