# Basic Questions

## Question 1: What is a thread in Java? How is it different from a process?

A thread is the smallest unit of execution within a process. Multiple threads share the same process memory space (heap, static variables, open files) while each has its own stack, program counter, and local variables.

Key differences:
| Aspect | Thread | Process |
| :--- | :--- | :--- |
| **Memory** | Shared heap with other threads | Own separate memory space |
| **Creation cost** | Lightweight – fast to create | Heavyweight – slow to create |
| **Communication** | Direct shared memory | IPC (sockets, pipes, etc.) |
| **Crash impact** | Can crash whole process | Isolated – only process dies |
| **Context switch** | Cheaper | More expensive |

## Question 2: What are the different ways to create a thread in Java?

There are four main ways to create a thread in Java:

1. **Extending Thread class**
```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Running in: " + Thread.currentThread().getName());
    }
}
// Usage: new MyThread().start();
```

2. **Implementing Runnable interface (Preferred)**
```java
class MyRunnable implements Runnable {
    @Override public void run() { /* task logic */ }
}
// Usage: new Thread(new MyRunnable()).start();
```

3. **Lambda (Java 8+)**
```java
Thread t = new Thread(() -> System.out.println("Lambda thread"));
t.start();
```

4. **Callable + Future (returns a value)**
```java
Callable<Integer> task = () -> 42;
ExecutorService es = Executors.newSingleThreadExecutor();
Future<Integer> future = es.submit(task);
int result = future.get(); // blocks until done
```

✅ **Best Practices**
- Prefer `Runnable` / `Callable` over extending `Thread` – keeps class hierarchy free for other bases.
- Always use `start()`, never `run()` – `run()` executes on the calling thread, not a new one.
- Use `ExecutorService` for production code instead of raw `Thread` creation.

## Question 3: Explain the lifecycle / states of a Thread in Java.

A thread passes through six states defined in `Thread.State` enum:
- **NEW** – Thread object created, `start()` not yet called
- **RUNNABLE** – Executing or ready to execute (waiting for CPU time slice)
- **BLOCKED** – Waiting to acquire a monitor lock (synchronized block)
- **WAITING** – Indefinitely waiting (`wait()`, `join()`, `LockSupport.park()`)
- **TIMED_WAITING** – Waiting for a specified time (`sleep(n)`, `wait(n)`, `join(n)`)
- **TERMINATED** – `run()` has completed (normally or via exception)

```java
Thread t = new Thread(() -> {}); // NEW
t.start();                       // → RUNNABLE
// inside run: Thread.sleep(1000) → TIMED_WAITING
// on synchronized block wait     → BLOCKED
// run() ends                    → TERMINATED
```

## Question 4: What is the difference between start() and run() in Java threads?

- `start()` – Creates a NEW thread by the JVM and executes `run()` on that thread. Correct approach.
- `run()` – Simply calls the run method on the CURRENT thread. No new thread is spawned. A common bug.

```java
Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName()));
t.run();   // prints 'main' – no new thread!
t.start(); // prints 'Thread-0' – correct!
```

## Question 5: What is the difference between sleep() and wait()?

| Aspect | `sleep()` | `wait()` |
| :--- | :--- | :--- |
| **Class** | `Thread` | `Object` |
| **Lock release** | Does NOT release the lock | Releases the monitor lock |
| **Wakeup** | After specified duration | By `notify()` / `notifyAll()` |
| **Context** | Any context | Must be inside synchronized block |
| **Interruption** | Throws `InterruptedException` | Throws `InterruptedException` |

## Question 6: What is a daemon thread? When would you use it?

A daemon thread is a background thread that does not prevent the JVM from exiting. When only daemon threads remain, the JVM shuts down. Created by calling `setDaemon(true)` before `start()`.

```java
Thread t = new Thread(() -> { while(true) { /* background task */ } });
t.setDaemon(true); // must be called BEFORE start()
t.start();
```

Use cases: garbage collection, log flushing, heartbeat monitors, resource cleanup.

## Question 7: What is Thread priority? Does it guarantee execution order?

Thread priority is a hint to the OS scheduler (1 = `MIN_PRIORITY`, 5 = `NORM_PRIORITY`, 10 = `MAX_PRIORITY`). It does NOT guarantee execution order – the OS thread scheduler has ultimate control and behavior is platform-specific.

```java
t.setPriority(Thread.MAX_PRIORITY); // 10
```

❌ **Common Mistakes**
- Relying on thread priority for correctness – it is merely a hint.
- Setting high priority and expecting it to always run first.

## Question 8: What is the difference between notify() and notifyAll()?

- `notify()` – wakes ONE arbitrary thread waiting on the monitor. Risky if multiple thread types wait.
- `notifyAll()` – wakes ALL waiting threads; they compete for the lock. Safer, slightly less efficient.

**Rule**: Use `notifyAll()` unless you are certain only one type of thread waits and exactly one needs to wake.

## Question 9: What is interrupt() and how do you handle interruption properly?

`Thread.interrupt()` sets the interrupt flag of the target thread. Blocking methods (sleep, wait, join) throw `InterruptedException` when interrupted.

```java
// Correct pattern:
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // RESTORE the flag!
    return; // or rethrow
}
```

❌ **Common Mistakes**
- Swallowing `InterruptedException` without restoring the flag – breaks cooperative cancellation.
- Checking `Thread.interrupted()` in a loop without resetting (it clears the flag).

## Question 10: Difference: Callable vs Runnable?

`Callable` can return a value and throw checked exceptions. `Runnable` cannot.

## Question 11: Can a static method be synchronized?

Yes – it locks on the `Class` object of the class.

## Question 12: Can constructors be synchronized?

No – a constructor cannot be synchronized; the object isn't yet visible to other threads.


## More Questions
1. What is Multithreading in Java? 
2. Difference between Process and Thread? 
3. Thread vs Runnable interface. 
4. Callable vs Runnable. 
5. What is ExecutorService? 
6. What is Thread Pool? 
7. Fixed Thread Pool vs Cached Thread Pool. 
8. How does Executors.newFixedThreadPool() work? 
9. What is Future? 
10. What is CompletableFuture? 
11. What is Synchronization? 
12. Why do we need synchronization? 
13. synchronized method vs synchronized block. 
14. What is intrinsic lock? 