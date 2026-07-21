# Basic Questions

## Question 1: Implement the Producer-Consumer problem.

**Using BlockingQueue (Recommended)**

```java
BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);

// Producer
Runnable producer = () -> {
    for (int i = 0; i < 100; i++) {
        try {
            queue.put(i); // blocks if queue is full
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
};

// Consumer
Runnable consumer = () -> {
    while (true) {
        try {
            Integer item = queue.take(); // blocks if queue is empty
            process(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
};
```

## Question 2: Implement a Singleton class that is thread-safe.

**Double-Checked Locking (Java 5+, requires volatile)**

```java
public class Singleton {
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) { // first check (no lock)
            synchronized (Singleton.class) {
                if (instance == null) { // second check (with lock)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**Initialization-on-demand holder (Best – lazy + thread-safe + no sync)**

```java
public class Singleton {
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }

    private Singleton() {}

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

## Question 3: What is the Dining Philosophers problem and how to solve it?

Five philosophers sit at a table. Each needs two forks to eat. Deadlock can occur if each picks up the left fork first.

**Solutions:**
- **Ordered resource acquisition** – philosopher N picks min-fork first, max-fork second.
- **Waiter / arbiter** – a semaphore limits concurrent eaters to N-1.
- **Chandy/Misra algorithm** – forks passed as messages, no shared state.

## Question 4: Implement a thread-safe blocking queue from scratch.

```java
class BoundedQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    BoundedQueue(int capacity) { this.capacity = capacity; }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) wait(); // full – wait
        queue.add(item);
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) wait(); // empty – wait
        T item = queue.poll();
        notifyAll();
        return item;
    }
}
```

✅ **Best Practices**
- Always use `while` (not `if`) for `wait()` checks – guards against spurious wakeups.
- Use `notifyAll()` instead of `notify()` unless you know exactly one waiter needs to wake.
