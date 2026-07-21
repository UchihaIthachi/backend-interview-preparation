# Classic Multithreading Problems

## 1. Definition

Classic multithreading problems are well-known scenarios that highlight the complexities of concurrent programming, such as synchronization, deadlock avoidance, and thread coordination.

## 2. Why it exists

Understanding and solving these classic problems (like Producer-Consumer, Dining Philosophers, and Thread-Safe Singleton) demonstrates a solid grasp of concurrency primitives and design patterns.

## 3. Related topics

- [java.util.concurrent Utilities](../java-util-concurrent/theory.md)
- [Race conditions](../race-conditions/theory.md)


## Fairness in Java Concurrency

Today I studied Fairness in Java Concurrency.

While learning Java concurrency, I explored the concept of fairness and how it affects the way threads acquire locks. Through practical examples, I learned that fairness is about giving waiting threads a more predictable opportunity to access shared resources.

The concepts I learned are:

- Fairness – A scheduling policy that attempts to grant a lock to the thread that has been waiting the longest.

- Fair Locks – By enabling fairness (for example, new ReentrantLock(true)), waiting threads are generally served in a first-come, first-served order.

- Unfair Locks – By default, many locks are unfair, allowing newly arrived threads to acquire a lock even if other threads have been waiting longer.

- Reduced Starvation – Fair locks help minimize the chances of a thread waiting indefinitely while others repeatedly acquire the same lock.

- Performance Trade-Off – Fairness improves predictability but may reduce overall throughput because the JVM has less flexibility in scheduling threads.

- Choosing the Right Strategy – The decision between fair and unfair locking depends on whether an application prioritizes maximum performance or predictable thread access.

One thing I found interesting is that fairness does not guarantee that threads execute in a perfectly fixed order. It simply provides a fairer lock acquisition policy, while the operating system and JVM scheduler still influence when threads actually run.

Understanding fairness is helping me build a stronger foundation in Java Concurrency before exploring semaphores, concurrent collections, and advanced thread coordination techniques.