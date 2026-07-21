# Basic Questions

## Question 1: What are virtual threads (Project Loom)? How do they differ from platform threads?

Virtual threads (Java 21 GA) are lightweight threads managed by the JVM, not the OS. They are designed for high-throughput I/O-bound workloads.

| Aspect | Platform Thread | Virtual Thread |
| :--- | :--- | :--- |
| **Stack size** | ~1 MB OS stack | Small heap object, grows dynamically |
| **Creation cost** | Thousands per JVM | Millions per JVM |
| **Scheduling** | OS scheduler | JVM ForkJoinPool scheduler |
| **Blocking I/O** | Blocks OS thread | Unmounts, carrier thread freed |
| **API** | `Thread.start()` | `Thread.ofVirtual().start(runnable)` |

```java
// Create virtual thread (Java 21)
Thread vt = Thread.ofVirtual().name("vthread-1").start(() -> doWork());

ExecutorService es = Executors.newVirtualThreadPerTaskExecutor();
```

## Question 2: What problem do virtual threads solve?

Virtual threads solve the scalability limit of the thread-per-request model. Platform (OS) threads are expensive, so thread-per-request architectures don't scale past a few thousand concurrent connections. Virtual threads let a blocking, imperative programming style (no reactive/async code needed) scale to huge levels of concurrency for I/O-bound workloads, since the JVM can unmount them when they block and reuse the underlying OS carrier thread.

## Question 3: Why don't virtual threads help with CPU-bound workloads?

Virtual threads don't add CPU parallelism; they add cheap concurrency for waiting. If a task is CPU-bound (e.g., heavy computation in a tight loop), it will occupy the underlying OS carrier thread regardless of whether it's wrapped in a virtual thread. The bottleneck for CPU-bound tasks is the number of available CPU cores, not the cost or number of threads.

## Question 4: What does it mean for a virtual thread to be "pinned," and why is it a problem?

A virtual thread is "pinned" when it performs a blocking operation but cannot be unmounted from its underlying carrier thread by the JVM. Historically, this occurred when executing blocking operations inside a `synchronized` block or when invoking native methods. When pinned, the virtual thread blocks the carrier thread, effectively reducing the system to platform-thread concurrency limits and defeating the purpose of virtual threads.

## Question 5: Would you still need a bounded database connection pool with virtual threads? Why?

Yes. While virtual threads allow you to have millions of concurrent threads waiting for database operations, the database itself (and the network) still has strict physical limits on how many concurrent connections it can handle. A bounded connection pool is required to prevent overwhelming the database and causing it to crash or reject connections, essentially acting as a throttle/bulkhead.
