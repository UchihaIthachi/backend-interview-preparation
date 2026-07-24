# Basic Questions — Classic Multithreading Problems

## Question 1: Implement the Producer–Consumer problem

The Producer–Consumer pattern contains:

- One or more producers that create work
- One or more consumers that process work
- A shared buffer between them
- Backpressure when the buffer is full
- Waiting when the buffer is empty

## Using `BlockingQueue` — Recommended

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ProducerConsumerExample {

    private static final int CAPACITY = 10;
    private static final int ITEM_COUNT = 100;
    private static final int POISON_PILL = -1;

    private final BlockingQueue<Integer> queue =
            new ArrayBlockingQueue<>(CAPACITY);

    public Runnable producer() {
        return () -> {
            try {
                for (int value = 0; value < ITEM_COUNT; value++) {
                    queue.put(value);
                }

                // Tells the consumer that production is complete.
                queue.put(POISON_PILL);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };
    }

    public Runnable consumer() {
        return () -> {
            try {
                while (true) {
                    Integer item = queue.take();

                    if (item == POISON_PILL) {
                        break;
                    }

                    process(item);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private void process(Integer item) {
        System.out.println(
                Thread.currentThread().getName()
                        + " processed "
                        + item
        );
    }

    public static void main(String[] args)
            throws InterruptedException {

        ProducerConsumerExample example =
                new ProducerConsumerExample();

        Thread producer =
                new Thread(example.producer(), "producer");

        Thread consumer =
                new Thread(example.consumer(), "consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }
}
```

### How it works

- `put()` blocks while the queue is full.
- `take()` blocks while the queue is empty.
- The queue safely coordinates producers and consumers.
- A bounded queue applies backpressure.
- The poison pill provides controlled shutdown.

### Multiple consumers

When several consumers exist, insert one poison pill per consumer:

```java
for (int index = 0; index < consumerCount; index++) {
    queue.put(POISON_PILL);
}
```

Alternatively, shutdown may be controlled using interruption, executor lifecycle, or an explicit completion protocol.

### Why use a bounded queue?

An unbounded queue may grow indefinitely when producers are faster than consumers:

```text
Production rate > consumption rate
        ↓
Queue continuously grows
        ↓
Memory pressure
        ↓
Possible OutOfMemoryError
```

### Interview-ready answer

> I normally implement Producer–Consumer using a bounded `BlockingQueue`. Producers call `put()`, which waits when the queue is full, and consumers call `take()`, which waits when the queue is empty. The bounded capacity provides backpressure, and shutdown can be implemented with interruption or a poison-pill message.

---

## Question 2: Implement a thread-safe Singleton class

A thread-safe singleton must guarantee that only one instance is created within the relevant class loader, even when multiple threads request it concurrently.

> A Java singleton is generally one instance per class loader, not one instance across multiple JVMs or distributed service instances.

---

## Option 1: Initialization-on-Demand Holder

```java
public final class Singleton {

    private Singleton() {
    }

    private static final class Holder {
        private static final Singleton INSTANCE =
                new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

### Why it works

The nested `Holder` class is not initialized until `getInstance()` accesses it.

JVM class initialization guarantees:

- Thread-safe initialization
- Exactly one initialization per class loader
- Safe publication of the created object

### Advantages

- Lazy initialization
- No explicit synchronization
- Simple implementation
- Efficient after initialization

This is usually the preferred lazy singleton implementation.

---

## Option 2: Enum Singleton

```java
public enum Singleton {
    INSTANCE;

    public void performOperation() {
        System.out.println("Executing");
    }
}
```

Usage:

```java
Singleton.INSTANCE.performOperation();
```

### Advantages

- Thread-safe initialization
- Concise
- Serialization-safe
- Strong protection against ordinary reflective duplication

Use this when enum syntax fits the design.

---

## Option 3: Double-Checked Locking

```java
public final class Singleton {

    private static volatile Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        Singleton result = instance;

        if (result == null) {
            synchronized (Singleton.class) {
                result = instance;

                if (result == null) {
                    result = new Singleton();
                    instance = result;
                }
            }
        }

        return result;
    }
}
```

### Why are there two checks?

The first check avoids synchronization after initialization:

```java
if (result == null)
```

The second check prevents multiple instances when several threads enter the first check simultaneously:

```java
synchronized (Singleton.class) {
    if (result == null) {
        // Create instance once
    }
}
```

### Why is `volatile` required?

Without `volatile`, another thread might observe the reference without being guaranteed to observe the fully initialized object state.

`volatile` provides:

- Visibility
- Safe publication
- Ordering guarantees around the instance assignment

### Which approach should be used?

| Approach               |        Lazy |      Explicit locking | Typical recommendation              |
| ---------------------- | ----------: | --------------------: | ----------------------------------- |
| Holder idiom           |         Yes |                    No | Excellent lazy implementation       |
| Enum                   | JVM-managed |                    No | Simplest when enum form is suitable |
| Double-checked locking |         Yes | During initialization | Valid but more complex              |
| Synchronized method    |         Yes |          Every access | Correct but unnecessary overhead    |

### Interview-ready answer

> For a lazy singleton, I prefer the initialization-on-demand holder idiom because JVM class initialization is thread-safe and requires no explicit synchronization. Double-checked locking is also valid when the instance field is `volatile`, but it is more complex. An enum singleton is another robust option.

---

# Question 3: What is the Dining Philosophers problem, and how can it be solved?

Five philosophers sit around a table. A fork exists between each pair of philosophers.

Each philosopher alternates between:

- Thinking
- Acquiring two forks
- Eating
- Releasing the forks

A philosopher needs both adjacent forks to eat.

## The deadlock problem

Suppose every philosopher:

1. Picks up the left fork.
2. Waits for the right fork.

```text
Philosopher 1 holds Fork 1 and waits for Fork 2
Philosopher 2 holds Fork 2 and waits for Fork 3
Philosopher 3 holds Fork 3 and waits for Fork 4
Philosopher 4 holds Fork 4 and waits for Fork 5
Philosopher 5 holds Fork 5 and waits for Fork 1
```

This creates circular waiting, so no philosopher can continue.

---

## Solution 1: Ordered Resource Acquisition

Assign every fork a fixed ID and always acquire the lower-ID fork first.

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Philosopher {

    private final Lock firstFork;
    private final Lock secondFork;

    public Philosopher(
            Lock leftFork,
            int leftForkId,
            Lock rightFork,
            int rightForkId
    ) {
        if (leftForkId < rightForkId) {
            this.firstFork = leftFork;
            this.secondFork = rightFork;
        } else {
            this.firstFork = rightFork;
            this.secondFork = leftFork;
        }
    }

    public void eat() {
        firstFork.lock();

        try {
            secondFork.lock();

            try {
                consumeMeal();
            } finally {
                secondFork.unlock();
            }
        } finally {
            firstFork.unlock();
        }
    }

    private void consumeMeal() {
        System.out.println(
                Thread.currentThread().getName()
                        + " is eating"
        );
    }

    public static Lock newFork() {
        return new ReentrantLock();
    }
}
```

Because all philosophers acquire locks in the same global order, circular waiting cannot occur.

---

## Solution 2: Waiter or Arbiter

For `N` philosophers, allow only `N - 1` philosophers to attempt fork acquisition simultaneously.

```java
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;

public final class DiningTable {

    private final Semaphore waiter;

    public DiningTable(int philosopherCount) {
        waiter = new Semaphore(
                philosopherCount - 1,
                true
        );
    }

    public void eat(
            Lock leftFork,
            Lock rightFork
    ) throws InterruptedException {

        waiter.acquire();

        try {
            leftFork.lock();

            try {
                rightFork.lock();

                try {
                    consumeMeal();
                } finally {
                    rightFork.unlock();
                }
            } finally {
                leftFork.unlock();
            }
        } finally {
            waiter.release();
        }
    }

    private void consumeMeal() {
        System.out.println("Eating");
    }
}
```

At least one philosopher can acquire both forks, breaking the all-hold-one-fork deadlock condition.

---

## Solution 3: Timed `tryLock()`

A philosopher attempts to acquire both forks but releases the first one when the second is unavailable.

```java
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public final class TimedPhilosopher {

    public boolean tryToEat(
            Lock leftFork,
            Lock rightFork
    ) throws InterruptedException {

        boolean leftAcquired =
                leftFork.tryLock(
                        100,
                        TimeUnit.MILLISECONDS
                );

        if (!leftAcquired) {
            return false;
        }

        try {
            boolean rightAcquired =
                    rightFork.tryLock(
                            100,
                            TimeUnit.MILLISECONDS
                    );

            if (!rightAcquired) {
                return false;
            }

            try {
                eat();
                return true;
            } finally {
                rightFork.unlock();
            }
        } finally {
            leftFork.unlock();
        }
    }

    public void retryWithBackoff()
            throws InterruptedException {

        long delayMillis =
                ThreadLocalRandom.current()
                                 .nextLong(10, 100);

        Thread.sleep(delayMillis);
    }

    private void eat() {
        System.out.println("Eating");
    }
}
```

Randomized backoff helps reduce livelock, where all philosophers repeatedly release and retry at the same time.

---

## Solution 4: Chandy–Misra Algorithm

The Chandy–Misra solution is a distributed algorithm in which:

- Forks have ownership.
- Forks can be marked clean or dirty.
- Philosophers request forks from their owners.
- Fork ownership is transferred according to defined rules.

It uses message-based coordination to prevent deadlock and reduce starvation.

It is more accurate to describe it as distributed ownership and message passing rather than saying it has “no shared state.” The fork ownership and state still exist, but access is coordinated through explicit transfer rules instead of direct competing locks.

### Interview-ready answer

> Dining Philosophers demonstrates deadlock caused by inconsistent resource acquisition. I can solve it by imposing a global fork order, using a semaphore to limit competing philosophers, or using timed lock acquisition with randomized backoff. The simplest general technique is consistent lock ordering.

---

# Question 4: Implement a thread-safe blocking queue from scratch

This implementation demonstrates:

- Monitor locking
- Condition waiting
- Bounded capacity
- Producer–consumer coordination
- Interruption support

```java
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public final class BoundedQueue<T> {

    private final Queue<T> queue =
            new ArrayDeque<>();

    private final int capacity;

    public BoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.capacity = capacity;
    }

    public synchronized void put(T item)
            throws InterruptedException {

        Objects.requireNonNull(
                item,
                "Null elements are not supported"
        );

        while (queue.size() == capacity) {
            wait();
        }

        queue.add(item);
        notifyAll();
    }

    public synchronized T take()
            throws InterruptedException {

        while (queue.isEmpty()) {
            wait();
        }

        T item = queue.remove();
        notifyAll();

        return item;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized boolean isFull() {
        return queue.size() == capacity;
    }
}
```

---

## How `put()` works

```java
while (queue.size() == capacity) {
    wait();
}
```

When the queue is full:

1. The producer calls `wait()`.
2. The producer releases the queue monitor.
3. The producer remains suspended.
4. A consumer removes an item and calls `notifyAll()`.
5. The producer wakes and attempts to reacquire the monitor.
6. It rechecks the full condition before inserting.

---

## How `take()` works

```java
while (queue.isEmpty()) {
    wait();
}
```

When the queue is empty:

1. The consumer releases the monitor and waits.
2. A producer inserts an element.
3. The producer calls `notifyAll()`.
4. The consumer wakes.
5. It reacquires the monitor and rechecks the condition.
6. It removes and returns an element.

---

## Why use `while` instead of `if`?

Incorrect:

```java
if (queue.isEmpty()) {
    wait();
}
```

Correct:

```java
while (queue.isEmpty()) {
    wait();
}
```

A thread must recheck the condition because:

- Spurious wakeups are permitted.
- Another awakened consumer may remove the item first.
- The condition may become false again before the thread reacquires the monitor.
- `notifyAll()` may wake threads waiting for different conditions.

This is called the **guarded-block pattern**.

---

## Why use `notifyAll()`?

Both producers and consumers wait on the same monitor.

After a queue modification, either type of thread may need to wake:

- Removing an item may allow a producer to continue.
- Adding an item may allow a consumer to continue.

Using `notify()` can awaken a thread whose condition is still false. `notifyAll()` wakes all waiting threads, and each thread rechecks its own condition.

The trade-off is additional wake-up overhead.

---

## Limitation of the monitor-based implementation

Both conditions share one wait set:

```text
Queue not full
Queue not empty
```

Using `ReentrantLock` with two `Condition` objects provides more targeted signalling.

---

# Blocking Queue Using `ReentrantLock` and `Condition`

```java
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class LockBasedBoundedQueue<T> {

    private final Queue<T> queue =
            new ArrayDeque<>();

    private final int capacity;

    private final ReentrantLock lock =
            new ReentrantLock();

    private final Condition notEmpty =
            lock.newCondition();

    private final Condition notFull =
            lock.newCondition();

    public LockBasedBoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.capacity = capacity;
    }

    public void put(T item)
            throws InterruptedException {

        Objects.requireNonNull(
                item,
                "Null elements are not supported"
        );

        lock.lockInterruptibly();

        try {
            while (queue.size() == capacity) {
                notFull.await();
            }

            queue.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take()
            throws InterruptedException {

        lock.lockInterruptibly();

        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }

            T item = queue.remove();
            notFull.signal();

            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();

        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}
```

### Advantages

- Separate wait sets for producers and consumers
- `notEmpty.signal()` wakes a consumer
- `notFull.signal()` wakes a producer
- Interruptible lock acquisition
- Better control than one monitor wait set

For production code, use an existing `BlockingQueue` unless implementing the structure is itself the requirement.

---

# Common Mistakes

## 1. Continuing production after interruption

Problematic:

```java
catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
}
```

inside a loop that then continues producing.

Better:

```java
catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    return;
}
```

Restoring the interrupt status is useful, but the method should also stop or propagate the interruption when appropriate.

---

## 2. Using an infinite consumer without shutdown

```java
while (true) {
    queue.take();
}
```

The consumer needs a termination strategy:

- Interruption
- Poison pill
- Cancellation token
- Executor shutdown
- Explicit queue closing protocol

---

## 3. Using `if` around `wait()`

Always use a condition loop.

---

## 4. Calling `wait()` outside synchronization

This is invalid:

```java
queue.wait();
```

unless the current thread owns `queue`’s monitor.

Otherwise, Java throws:

```text
IllegalMonitorStateException
```

---

## 5. Forgetting to release locks in `finally`

Incorrect:

```java
lock.lock();
performWork();
lock.unlock();
```

Correct:

```java
lock.lock();

try {
    performWork();
} finally {
    lock.unlock();
}
```

---

## 6. Assuming thread-safe methods make a workflow atomic

Individual queue operations may be thread-safe, while a multi-step workflow is not automatically atomic.

```java
if (!queue.isEmpty()) {
    T item = queue.remove();
}
```

Another thread may remove the final item between these calls.

Use one atomic operation:

```java
T item = queue.poll();
```

---

## 7. Treating Singleton as distributed coordination

A singleton does not ensure one scheduler, one job runner, or one leader across a cluster.

Distributed uniqueness requires mechanisms such as:

- Database locks
- Distributed leases
- Leader election
- Consensus systems
- Fencing tokens

---

# Interview Questions

## Why is `BlockingQueue` preferred over `wait()` and `notifyAll()`?

> It already implements thread-safe waiting, capacity management, visibility, and signalling. It reduces the chance of subtle monitor errors and clearly represents the Producer–Consumer model.

## Why should a bounded queue be used?

> It provides backpressure and prevents producers from consuming unlimited memory when consumers cannot keep up.

## Why is `volatile` necessary for double-checked locking?

> It guarantees visibility and prevents unsafe publication of a partially initialized singleton.

## Why does ordered fork acquisition prevent deadlock?

> Every thread follows the same resource order, so a circular wait dependency cannot form.

## Why does `wait()` release the monitor?

> Waiting threads must allow another thread to acquire the monitor and change the condition they are waiting for.

## Does `sleep()` release a held monitor?

> No. `Thread.sleep()` pauses the thread but does not release locks it owns.

## Why is `notifyAll()` safer than `notify()` here?

> Producers and consumers wait on the same monitor for different conditions. Waking all waiters ensures eligible threads can recheck their conditions, while `notify()` may awaken an unsuitable waiter.

---

# Short Interview Answer

> I implement Producer–Consumer with a bounded `BlockingQueue`, because `put()` and `take()` provide safe waiting and backpressure. For a thread-safe singleton, I prefer the holder idiom or enum, while double-checked locking requires a `volatile` instance. Dining Philosophers is a deadlock problem that can be solved with global resource ordering, an arbiter, or timed lock acquisition. When implementing a blocking queue manually, I protect state with one lock, wait inside `while` loops, support interruption, and signal waiting producers or consumers after every relevant state change.
