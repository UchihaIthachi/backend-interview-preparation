# Advanced Questions

## Question 1: How does the `volatile` keyword affect thread behavior?

The `volatile` keyword tells the Java Memory Model that a variable may be accessed and modified by multiple threads.

It provides two important guarantees:

1. **Visibility** — when one thread writes to a volatile variable, subsequent reads by other threads observe the latest value.
2. **Ordering** — the compiler and CPU cannot reorder certain operations across volatile reads and writes.

A write to a volatile variable **happens-before** every subsequent read of that same variable. This means the reading thread can also observe writes performed before the volatile write.

### Example: stopping a worker thread

```java
public class Worker implements Runnable {

    private volatile boolean running = true;

    @Override
    public void run() {
        while (running) {
            // Perform background work
        }

        System.out.println("Worker stopped");
    }

    public void stop() {
        running = false;
    }
}
```

```java
public class Main {

    public static void main(String[] args)
            throws InterruptedException {

        Worker worker = new Worker();
        Thread thread = new Thread(worker);

        thread.start();

        Thread.sleep(1_000);
        worker.stop();

        thread.join();
    }
}
```

Without `volatile`, the worker thread may repeatedly read an older value of `running` and fail to observe that another thread changed it to `false`.

It is more accurate to say that `volatile` guarantees visibility according to the Java Memory Model—not that it literally disables all CPU caching.

---

## What does `volatile` not guarantee?

`volatile` does **not** provide mutual exclusion or make compound operations atomic.

```java
private volatile int count = 0;

public void increment() {
    count++;
}
```

The expression `count++` consists of multiple operations:

```text
Read count
    ↓
Add one
    ↓
Write count
```

Two threads can read the same value before either writes the updated value, causing a lost update.

```text
Initial count = 10

Thread A reads 10
Thread B reads 10

Thread A writes 11
Thread B writes 11

Expected result: 12
Actual result:   11
```

Use `AtomicInteger` for an atomic counter:

```java
import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

    private final AtomicInteger count = new AtomicInteger();

    public int increment() {
        return count.incrementAndGet();
    }
}
```

Alternatively, protect the complete operation with `synchronized` or a lock.

---

## Visibility, atomicity, and mutual exclusion

| Feature                  | `volatile` |             `synchronized` |               `AtomicInteger` |
| ------------------------ | ---------: | -------------------------: | ----------------------------: |
| Visibility               |        Yes |                        Yes |                           Yes |
| Ordering guarantees      |        Yes |                        Yes |                           Yes |
| Mutual exclusion         |         No |                        Yes |                            No |
| Atomic compound updates  |         No |                        Yes | Yes, for supported operations |
| Threads may block        |         No |                        Yes |                            No |
| Suitable for counters    |         No |                        Yes |                           Yes |
| Suitable for state flags |        Yes | Yes, but often unnecessary |       Possible, but excessive |

---

## Common use cases for `volatile`

### 1. Simple state flags

```java
private volatile boolean shutdownRequested;
```

### 2. Publishing a newly assigned reference

```java
private volatile Configuration currentConfiguration;
```

A thread replacing `currentConfiguration` safely publishes the reference to subsequent readers, provided the object is correctly constructed and is not later mutated unsafely.

### 3. Double-checked locking

```java
public final class Singleton {

    private static volatile Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }

        return instance;
    }
}
```

`volatile` is required because object creation may otherwise be observed out of order:

```text
1. Allocate memory
2. Assign the reference
3. Initialize the object
```

Without `volatile`, another thread could theoretically observe a non-null reference before initialization has completed. The volatile write and read establish the required visibility and ordering relationship.

---

## When should `volatile` not be used alone?

`volatile` is insufficient when:

- An update depends on the variable’s current value.
- Multiple variables must change consistently.
- A check-and-act sequence must be atomic.
- A collection or object is mutated by multiple threads.
- A critical section must be accessed by only one thread.

Incorrect example:

```java
private volatile int balance = 1_000;

public void withdraw(int amount) {
    if (balance >= amount) {
        balance -= amount;
    }
}
```

Both the check and update must be protected together:

```java
private int balance = 1_000;

public synchronized boolean withdraw(int amount) {
    if (balance < amount) {
        return false;
    }

    balance -= amount;
    return true;
}
```

## Interview-ready answer

> The `volatile` keyword guarantees visibility and ordering for a shared variable. A write to a volatile variable happens-before a subsequent read of that variable, so other threads observe the latest value and preceding writes. However, volatile does not provide locking or make compound operations such as `count++` atomic. It is suitable for state flags and safely published references, while counters and multi-step state changes require atomic classes, synchronization, or locks.
