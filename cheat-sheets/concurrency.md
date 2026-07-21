# Concurrency Cheat Sheet (final revision — cross-language)

## Java
- volatile = visibility, not atomicity.
- AtomicInteger for lock-free single-variable updates; synchronized/ReentrantLock
  for multi-step critical sections.
- happens-before relationships: synchronized unlock/lock, volatile write/read,
  thread start, thread join.
- Deadlock: multiple threads waiting on each other's locks in a cycle — diagnose
  with thread dumps.

## Go
- Prefer channels for coordination ("share memory by communicating"), mutexes
  for simple shared counters.
- context.Context for cancellation/timeouts across a call chain.
- Goroutine leaks: no exit path (no context check, no channel close) -> goroutine
  blocks forever.

## Cross-cutting principles
- Never guess at concurrency bugs — reproduce under real load / stress tests.
- Race conditions and deadlocks are different bugs: races corrupt data,
  deadlocks freeze progress.
- Always have a way to signal shutdown/cancellation into any long-running
  concurrent task.

## Java Multithreading at a Glance

### Thread Creation Methods
| Method | When to Use |
| :--- | :--- |
| `implements Runnable` | General use; keeps class hierarchy free |
| `extends Thread` | Rarely; only if you need to override Thread behaviour |
| `Callable` + `Future` | When you need a result or exception propagation |
| Lambda / method ref | Short tasks in Java 8+ |
| `ExecutorService.submit()`| Production code; pooled, managed execution |
| `CompletableFuture.supplyAsync()` | Async pipelines with chaining |
| `Thread.ofVirtual()` (Java 21) | I/O-bound tasks; millions of threads |

### Synchronization Toolkit
| Tool | Key Trait | Use When |
| :--- | :--- | :--- |
| `synchronized` | Built-in, reentrant | Simple mutual exclusion |
| `ReentrantLock` | Trylock, fair, condition | Advanced lock control needed |
| `ReadWriteLock` | Multiple readers OR one writer | Read-heavy caches |
| `StampedLock` | Optimistic reads | High-read, low-write scenarios |
| `volatile` | Visibility only | Flags, simple published refs |
| Atomic classes | Lock-free CAS | Counters, accumulators |
| `CountDownLatch` | One-shot N-count | Wait for N tasks to start/finish |
| `CyclicBarrier` | Reusable, all-or-nothing | Iterative parallel phases |
| `Semaphore` | Permit-based rate limit | Connection pool, throttling |
| `Phaser` | Dynamic parties, multi-phase | Complex parallel algorithms |

### Thread Pool Quick Reference
| Pool Type | Threads | Best For |
| :--- | :--- | :--- |
| `FixedThreadPool(n)` | Fixed n | CPU-bound, stable load |
| `CachedThreadPool` | 0 to ∞ | Many short I/O tasks |
| `SingleThreadExecutor` | 1 | Sequential ordering |
| `ScheduledThreadPool`| Fixed n | Cron, periodic tasks |
| `WorkStealingPool` | Parallelism | Recursive, divide-and-conquer |
| `VirtualThreadPerTask` | Unlimited VT | Massive I/O concurrency (J21) |

### Key Terms & Formulas
| Term | Definition |
| :--- | :--- |
| **Race Condition** | Output depends on non-deterministic thread interleaving |
| **Critical Section** | Code segment accessing shared resource – must be protected |
| **Monitor** | Per-object lock; one thread enters synchronized block at a time |
| **happens-before** | JMM guarantee: writes by A visible to B if A hb B |
| **CAS** | Atomic: set new value only if current == expected |
| **Amdahl's Law** | Speedup ≤ 1/(S + (1-S)/N) where S=serial fraction, N=threads |
| **Thread Safety** | Behaves correctly when accessed from multiple threads simultaneously |
| **Reentrancy** | Thread can re-acquire a lock it already holds |
| **False Sharing** | Cache-line contention between independently-modified variables |
| **Memory Barrier** | Instruction preventing CPU/compiler reordering |

### Pattern Cheat Sheet
| Pattern | Implementation |
| :--- | :--- |
| **Thread-safe Singleton** | Initialization-on-demand holder idiom |
| **Producer-Consumer** | `LinkedBlockingQueue.put()` / `take()` |
| **Parallel task + result** | `ExecutorService.invokeAll(callables)` |
| **Async pipeline** | `CompletableFuture.supplyAsync().thenApply().thenAccept()` |
| **Cancel long-running task** | Set volatile boolean flag; check in loop; call `interrupt()` |
| **Read-heavy cache** | `ConcurrentHashMap` or `ReadWriteLock` |
| **Throttle concurrency** | `Semaphore` with N permits |
| **Wait for multiple tasks** | `CountDownLatch` or `CompletableFuture.allOf()` |
| **Divide & conquer** | `ForkJoinPool` + `RecursiveTask` |

## Interview Tips

✅ **Best Practices**
- Start simple – explain the basic problem before discussing optimized solutions.
- Always mention thread safety when designing any shared data structure.
- When asked about synchronization, discuss trade-offs: `synchronized` vs `Lock` vs `Atomic`.
- Mention `volatile` + `happens-before` when discussing visibility problems.
- Always name concurrent collections (`ConcurrentHashMap`) as an alternative to synchronized wrappers.
- For any classic problem (producer-consumer, singleton), know both the low-level and high-level solutions.
- Discuss deadlock prevention proactively – shows maturity.
- Mention Java 21 virtual threads if talking about scalable I/O servers.
- Know how to read a thread dump (`jstack`) – frequently asked in senior interviews.
