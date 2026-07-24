# Basic Questions — Threads, Processes, Lifecycle, and Core APIs

## Question 1: What is a thread in Java? How is it different from a process?

A **process** is an independently running program with its own address space and operating-system resources.

A **thread** is an execution path within a process. Multiple threads inside the same JVM process can execute concurrently and share application state.

### Memory relationship

Threads in the same JVM generally share:

* Heap objects
* Static fields
* Loaded classes
* Open files and sockets
* Process-level resources

Each thread has its own:

* Java stack
* Stack frames
* Program counter
* Execution state
* Thread-local values

Local variables normally live in a thread’s stack frame, but objects referenced by those variables may still live on the shared heap.

### Process vs thread

| Aspect            | Thread                                  | Process                                      |
| ----------------- | --------------------------------------- | -------------------------------------------- |
| Address space     | Shares the process address space        | Has its own address space                    |
| Heap              | Shared with other JVM threads           | Separate from other processes                |
| Stack             | One stack per thread                    | Process contains one or more thread stacks   |
| Communication     | Shared memory and concurrency utilities | IPC such as sockets, pipes, or shared memory |
| Creation cost     | Usually lower                           | Usually higher                               |
| Context switching | Generally cheaper                       | Generally more expensive                     |
| Failure isolation | Limited                                 | Stronger isolation                           |
| Data sharing      | Easy but requires synchronization       | Explicit communication required              |

### Crash-impact correction

An uncaught Java exception normally terminates only the thread in which it occurs:

```java
Thread worker = new Thread(() -> {
    throw new IllegalStateException("Task failed");
});
```

It does not automatically crash the entire JVM.

However, the JVM process may terminate because of:

* An explicit `System.exit()`
* Fatal JVM errors
* Native-code crashes
* Severe operating-system failures
* The end of all non-daemon threads

### Interview-ready answer

> A process is an independently running program with its own address space. A thread is an execution path inside that process. JVM threads share heap objects and process resources but have separate stacks and execution state, so communication is efficient but shared mutable state must be synchronized.

---

## Question 2: What is multithreading in Java?

**Multithreading** is the concurrent execution of multiple threads within one Java process.

Example:

```java
Thread first = new Thread(() ->
        processOrders()
);

Thread second = new Thread(() ->
        generateReports()
);

first.start();
second.start();
```

Multithreading can improve:

* Responsiveness
* Resource utilization
* Throughput
* Overlap of independent work
* Handling of multiple concurrent requests

It does not automatically make an application faster. Performance depends on:

* CPU availability
* Workload type
* Synchronization overhead
* Shared-resource contention
* Database and network capacity
* Number of runnable threads

### Concurrency vs parallelism

* **Concurrency:** Multiple tasks make progress during overlapping time periods.
* **Parallelism:** Multiple tasks physically execute at the same time on different CPU cores.

A single-core system can execute concurrent threads through time slicing without true parallel execution.

---

## Question 3: What are the ways to create and execute concurrent tasks in Java?

It is useful to separate:

1. How the task is represented
2. How the task is executed

---

### 1. Extending `Thread`

```java
public final class ReportThread extends Thread {

    @Override
    public void run() {
        System.out.println(
                "Running on "
                        + Thread.currentThread().getName()
        );
    }
}
```

Usage:

```java
Thread thread = new ReportThread();
thread.start();
```

#### Limitation

Java supports only single class inheritance. Extending `Thread` couples the task to its execution mechanism.

---

### 2. Implementing `Runnable`

```java
public final class ReportTask implements Runnable {

    @Override
    public void run() {
        generateReport();
    }

    private void generateReport() {
    }
}
```

Usage:

```java
Thread thread =
        new Thread(new ReportTask());

thread.start();
```

This keeps task logic separate from the thread.

---

### 3. Using a lambda

Because `Runnable` is a functional interface:

```java
Thread thread = new Thread(() ->
        System.out.println("Lambda task")
);

thread.start();
```

A lambda is not a fundamentally different thread-creation mechanism. It is a concise way to implement `Runnable`.

---

### 4. Using `Callable` with an executor

`Callable<V>` returns a value and may throw checked exceptions.

```java
Callable<Integer> task = () -> {
    Thread.sleep(100);
    return 42;
};

ExecutorService executor =
        Executors.newSingleThreadExecutor();

try {
    Future<Integer> future =
            executor.submit(task);

    int result = future.get();

    System.out.println(result);
} finally {
    executor.shutdown();
}
```

---

### 5. Using `ExecutorService`

For application tasks, managed executors are usually preferable to manually creating platform threads.

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);

executor.submit(this::processOrder);
```

The executor manages:

* Worker creation
* Thread reuse
* Queuing
* Concurrency limits
* Task results
* Shutdown

---

### 6. Using virtual threads

For large numbers of blocking tasks:

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {

    Future<Response> future =
            executor.submit(
                    remoteClient::fetch
            );

    Response response = future.get();
}
```

Virtual threads make thread-per-task blocking code more scalable, but they do not increase database connections, CPU cores, or downstream capacity.

### Best practice

> Represent work as `Runnable` or `Callable` and let an executor decide how it should run.

---

## Question 4: `Thread` vs `Runnable`

| `Thread`                                        | `Runnable`                                               |
| ----------------------------------------------- | -------------------------------------------------------- |
| Represents a thread and its execution mechanism | Represents only a task                                   |
| Class                                           | Functional interface                                     |
| Extending it consumes class inheritance         | A class can implement it while extending another class   |
| Tightly couples task and execution              | Separates task from execution                            |
| Can call `start()`                              | Must be passed to a thread or executor                   |
| Not reusable as cleanly                         | Same task abstraction can be used by different executors |

Example:

```java
public final class InvoiceTask
        extends BaseService
        implements Runnable {

    @Override
    public void run() {
        generateInvoice();
    }
}
```

### Interview-ready answer

> `Thread` represents the actual execution thread, while `Runnable` represents work to perform. I normally implement `Runnable` or `Callable` and submit it to an executor because that separates business logic from thread management.

---

## Question 5: `Callable` vs `Runnable`

| `Runnable`                                        | `Callable<V>`                  |
| ------------------------------------------------- | ------------------------------ |
| `run()` method                                    | `call()` method                |
| Returns nothing                                   | Returns a value                |
| Cannot declare checked exceptions                 | Can declare checked exceptions |
| Commonly submitted with `execute()` or `submit()` | Submitted with `submit()`      |
| `submit()` returns `Future<?>`                    | `submit()` returns `Future<V>` |

### `Runnable`

```java
Runnable task = () ->
        sendNotification();
```

### `Callable`

```java
Callable<Report> task = () ->
        generateReport();
```

Submission:

```java
Future<Report> future =
        executor.submit(task);
```

### Interview-ready answer

> `Runnable` performs an action without returning a result and cannot declare checked exceptions. `Callable` returns a value, may throw checked exceptions, and is normally submitted to an executor that returns a `Future`.

---

## Question 6: Explain the lifecycle and states of a Java thread

Java defines six thread states in `Thread.State`:

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

There is no separate Java `RUNNING` state.

| State           | Meaning                                 |
| --------------- | --------------------------------------- |
| `NEW`           | Thread constructed but not started      |
| `RUNNABLE`      | Running or ready to run                 |
| `BLOCKED`       | Waiting to acquire an intrinsic monitor |
| `WAITING`       | Waiting indefinitely for another action |
| `TIMED_WAITING` | Waiting for a limited period            |
| `TERMINATED`    | Execution finished                      |

### `NEW`

```java
Thread thread = new Thread(task);
```

`start()` has not yet been called.

### `RUNNABLE`

```java
thread.start();
```

The thread may be executing or waiting for CPU scheduling.

### `BLOCKED`

```java
synchronized (lock) {
    performWork();
}
```

If another thread owns the same monitor, the contender becomes `BLOCKED`.

`BLOCKED` specifically refers to intrinsic monitor acquisition. A thread waiting for `ReentrantLock` commonly appears as `WAITING` or `TIMED_WAITING`.

### `WAITING`

Possible causes include:

```java
object.wait();
thread.join();
LockSupport.park();
latch.await();
```

### `TIMED_WAITING`

Possible causes include:

```java
Thread.sleep(1_000);
object.wait(1_000);
thread.join(1_000);
```

### `TERMINATED`

The `run()` method completed normally or ended because an uncaught exception escaped.

### Conceptual lifecycle

```text
NEW
  ↓ start()
RUNNABLE
  ├── BLOCKED
  ├── WAITING
  ├── TIMED_WAITING
  └── TERMINATED
```

A thread cannot be restarted after reaching `TERMINATED`.

---

## Question 7: What is the difference between `start()` and `run()`?

### `start()`

```java
thread.start();
```

`start()` requests the JVM to begin a new thread of execution. The new thread eventually invokes `run()`.

### `run()`

```java
thread.run();
```

Calling `run()` directly is an ordinary method call on the current thread. It does not create a new thread.

Example:

```java
Thread thread = new Thread(() ->
        System.out.println(
                Thread.currentThread().getName()
        )
);

thread.run();   // Usually prints main
thread.start(); // Prints the new thread's name
```

### Additional rule

A `Thread` object can be started only once:

```java
thread.start();
thread.start(); // IllegalThreadStateException
```

### Interview-ready answer

> `start()` creates a new execution path and causes the JVM to invoke `run()` on that thread. Calling `run()` directly executes it like a normal method on the current thread.

---

## Question 8: What is the difference between `sleep()` and `wait()`?

| `Thread.sleep()`                      | `Object.wait()`                                                     |
| ------------------------------------- | ------------------------------------------------------------------- |
| Static method of `Thread`             | Instance method of `Object`                                         |
| Pauses for a duration                 | Waits for a condition or signal                                     |
| Does not release held locks           | Releases that object’s monitor                                      |
| Can be called without synchronization | Must own the object’s monitor                                       |
| Wakes after timeout or interruption   | Wakes after notification, timeout, interruption, or spurious wakeup |
| Used for delays or backoff            | Used for inter-thread coordination                                  |

### `sleep()`

```java
Thread.sleep(1_000);
```

If called inside a synchronized block, the thread continues holding the monitor while sleeping.

```java
synchronized (lock) {
    Thread.sleep(1_000);
}
```

Other threads cannot enter synchronized sections guarded by `lock` during that time.

### `wait()`

```java
synchronized (lock) {
    while (!conditionIsTrue()) {
        lock.wait();
    }
}
```

`wait()` releases the monitor belonging to `lock`, allowing another thread to acquire it and modify the condition.

It does not necessarily release other locks the thread may hold.

### Interview-ready answer

> `sleep()` pauses the current thread without releasing locks. `wait()` is a monitor-coordination method that must be called while owning the monitor and temporarily releases that monitor while waiting.

---

## Question 9: What is a daemon thread?

A daemon thread is a background thread that does not, by itself, keep the JVM alive.

When no non-daemon threads remain, the JVM may terminate even if daemon threads are still running.

```java
Thread monitor = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        monitorSystem();
    }
});

monitor.setDaemon(true);
monitor.start();
```

`setDaemon(true)` must be called before `start()`.

Calling it after the thread starts throws `IllegalThreadStateException`.

### Suitable uses

Daemon threads are appropriate for best-effort supporting work such as:

* Internal monitoring
* Non-critical background observation
* Cache housekeeping
* Diagnostic activity

### Avoid daemon threads for critical work

Do not depend on daemon threads for operations that must finish:

* Persisting important data
* Completing financial transactions
* Flushing critical audit events
* Releasing external resources reliably

The JVM does not guarantee an orderly completion of daemon-thread work during termination.

### Additional detail

A newly created thread normally inherits its daemon status from the thread that created it.

### Interview-ready answer

> A daemon thread performs background work but does not keep the JVM alive. It is suitable for best-effort support activity, not work that must complete reliably before shutdown.

---

## Question 10: What is thread priority? Does it guarantee execution order?

Java thread priorities range from:

```java
Thread.MIN_PRIORITY  // 1
Thread.NORM_PRIORITY // 5
Thread.MAX_PRIORITY  // 10
```

Example:

```java
thread.setPriority(Thread.MAX_PRIORITY);
```

Priority is only a scheduling hint.

It does not guarantee:

* Execution order
* Immediate execution
* More CPU time
* Fairness
* Starvation prevention
* Consistent behavior across operating systems

Never use priority as part of correctness logic.

Incorrect assumption:

```text
High-priority thread must execute before low-priority thread.
```

Correct approach:

Use explicit coordination such as:

* `join()`
* `CountDownLatch`
* `CyclicBarrier`
* Locks
* Queues
* Futures

---

## Question 11: What is the difference between `notify()` and `notifyAll()`?

Both methods operate on an object’s intrinsic monitor and must be called while owning that monitor.

### `notify()`

```java
synchronized (lock) {
    updateState();
    lock.notify();
}
```

It wakes one arbitrary thread waiting on that monitor.

The awakened thread must still reacquire the monitor before continuing.

### `notifyAll()`

```java
synchronized (lock) {
    updateState();
    lock.notifyAll();
}
```

It wakes all waiting threads. They then compete to reacquire the monitor and recheck their conditions.

### Why use `notifyAll()`?

Suppose both producers and consumers wait on the same monitor. `notify()` may wake a thread whose condition is still false.

`notifyAll()` is generally safer when:

* Multiple logical conditions use one monitor
* Several producer and consumer threads exist
* It is difficult to prove which waiter should wake

### Always wait in a loop

```java
synchronized (lock) {
    while (!conditionIsTrue()) {
        lock.wait();
    }

    performOperation();
}
```

The loop is necessary because:

* Spurious wakeups are allowed.
* Another thread may consume the condition first.
* A notification does not guarantee the condition is now true.

### Preferred high-level alternative

For producer-consumer workflows, use `BlockingQueue` rather than manually coordinating with `wait()` and `notifyAll()`.

---

## Question 12: What is interruption, and how should it be handled?

`Thread.interrupt()` is a cooperative cancellation and signalling mechanism.

```java
worker.interrupt();
```

It does not forcibly stop the target thread.

### When the thread is running normal code

The interrupt status is set.

```java
while (!Thread.currentThread().isInterrupted()) {
    performWork();
}
```

### When the thread is blocked

Methods such as these may throw `InterruptedException`:

* `sleep()`
* `wait()`
* `join()`
* `BlockingQueue.put()` and `take()`
* `CountDownLatch.await()`

When `InterruptedException` is thrown, the interrupt status is generally cleared.

### Correct handling

#### Propagate the exception

```java
public void process()
        throws InterruptedException {

    Thread.sleep(1_000);
}
```

#### Restore the status when propagation is impossible

```java
try {
    queue.take();
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    return;
}
```

### `isInterrupted()` vs `Thread.interrupted()`

```java
thread.isInterrupted();
```

Checks a thread’s interrupt status without clearing it.

```java
Thread.interrupted();
```

Checks the current thread’s status and clears it.

### Common mistake

```java
catch (InterruptedException exception) {
    // Ignore
}
```

Swallowing interruption can prevent cancellation and graceful shutdown.

### Interview-ready answer

> Interruption is a cooperative request, not forced termination. Blocking methods may throw `InterruptedException`, and if I cannot propagate it, I restore the interrupt flag and exit or perform cleanup.

---

## Question 13: What is `ExecutorService`?

`ExecutorService` is a higher-level API for submitting tasks without manually creating and managing a thread for every task.

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);

Future<Result> future =
        executor.submit(this::calculate);

Result result = future.get();

executor.shutdown();
```

It supports:

* `Runnable` and `Callable`
* Task results through `Future`
* Cancellation
* Bulk execution
* Managed worker threads
* Queuing
* Shutdown and termination

### Interview-ready answer

> `ExecutorService` separates task submission from execution management. It controls worker threads, queues tasks, returns futures, supports cancellation, and provides lifecycle operations such as shutdown.

---

## Question 14: What is a thread pool?

A thread pool maintains worker threads that repeatedly execute submitted tasks.

```text
Submitted tasks
      ↓
Task queue
      ↓
Reusable worker threads
```

Benefits include:

* Reduced thread-creation overhead
* Controlled concurrency
* Reuse of workers
* Work queuing
* Centralized lifecycle management
* Monitoring and rejection handling

A thread pool must be designed with:

* Worker count
* Queue capacity
* Rejection policy
* Task duration
* Downstream capacity
* Shutdown behavior

A fixed worker count does not automatically mean bounded memory if the queue is unbounded.

---

## Question 15: Fixed thread pool vs cached thread pool

| Fixed pool                              | Cached pool                                               |
| --------------------------------------- | --------------------------------------------------------- |
| Fixed platform-worker count             | Can create many platform workers                          |
| Tasks wait in an unbounded queue        | Uses direct handoff through `SynchronousQueue`            |
| Predictable active worker count         | Adapts aggressively to submitted tasks                    |
| Queue can grow indefinitely             | Thread count can grow very large                          |
| Useful when workload is already bounded | Suitable only when task creation is externally controlled |

### Fixed pool

```java
ExecutorService executor =
        Executors.newFixedThreadPool(10);
```

Risk:

```text
Tasks arrive faster than completion
→ queue grows
→ latency and memory usage grow
```

### Cached pool

```java
ExecutorService executor =
        Executors.newCachedThreadPool();
```

Risk:

```text
No idle worker
→ create another platform thread
→ sustained blocking load
→ excessive thread creation
```

For important production workloads, an explicitly bounded `ThreadPoolExecutor` is often safer.

---

## Question 16: How does `Executors.newFixedThreadPool()` work?

Conceptually, it creates a `ThreadPoolExecutor` with:

* `corePoolSize = n`
* `maximumPoolSize = n`
* An unbounded `LinkedBlockingQueue`
* Reusable platform threads

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);
```

Behavior:

1. Workers are created as tasks arrive until the pool reaches four workers.
2. At most four tasks run concurrently.
3. Additional tasks enter the unbounded queue.
4. An idle worker takes the next queued task.
5. Workers remain available until shutdown.

### Important implication

The worker count is bounded, but the task backlog is not.

Under sustained overload, queued tasks can consume memory and experience very long delays.

---

## Question 17: What is `Future`?

`Future<T>` represents the eventual outcome of an asynchronous task.

```java
Future<Integer> future =
        executor.submit(() -> 42);
```

It supports:

```java
future.get();
future.get(5, TimeUnit.SECONDS);
future.cancel(true);
future.isDone();
future.isCancelled();
```

### Task exception

```java
try {
    Integer result = future.get();
} catch (ExecutionException exception) {
    Throwable taskFailure =
            exception.getCause();
}
```

### Limitations

* `get()` blocks.
* No direct transformation API.
* No convenient chaining.
* No direct result combination.
* No callback-based recovery.
* A regular `Future` cannot be manually completed.

---

## Question 18: What is `CompletableFuture`?

`CompletableFuture<T>` supports asynchronous composition in addition to the `Future` contract.

```java
CompletableFuture<String> result =
        CompletableFuture
                .supplyAsync(
                        this::loadUser,
                        executor
                )
                .thenApply(User::name)
                .thenApply(
                        String::toUpperCase
                );
```

Important methods include:

| Method            | Purpose                                  |
| ----------------- | ---------------------------------------- |
| `runAsync()`      | Run an action with no result             |
| `supplyAsync()`   | Run an operation that returns a value    |
| `thenApply()`     | Transform a result                       |
| `thenCompose()`   | Chain a dependent asynchronous operation |
| `thenCombine()`   | Combine independent results              |
| `thenAccept()`    | Consume a result                         |
| `allOf()`         | Wait for all stages                      |
| `anyOf()`         | Complete after any stage                 |
| `exceptionally()` | Convert failure into fallback            |
| `handle()`        | Process success or failure               |
| `orTimeout()`     | Complete exceptionally after a timeout   |

`CompletableFuture` does not automatically make blocking work non-blocking. The operation executed inside a stage may still block its executor thread.

---

## Question 19: What is synchronization?

Synchronization is the coordination of concurrent access to shared state.

It can provide:

* Mutual exclusion
* Visibility
* Ordering
* Atomic execution of a critical section
* Protection of invariants

Example:

```java
private final Object lock = new Object();
private int count;

public void increment() {
    synchronized (lock) {
        count++;
    }
}
```

Synchronization is broader than the `synchronized` keyword. It also includes locks, atomics, volatile variables, concurrent collections, queues, and synchronizers.

---

## Question 20: Why do we need synchronization?

Without synchronization, operations may interleave incorrectly.

```java
private int count;

public void increment() {
    count++;
}
```

Two threads can both read the same old value and lose one update.

Synchronization also provides memory visibility. Protecting only writes but not reads may still be incorrect.

The complete shared-state protocol must be consistent across all participating threads.

---

## Question 21: Synchronized method vs synchronized block

| Synchronized method                  | Synchronized block              |
| ------------------------------------ | ------------------------------- |
| Locks the whole method body          | Locks only the selected block   |
| Instance method locks `this`         | Can use any chosen monitor      |
| Static method locks the class object | Often uses a private final lock |
| Simple and readable                  | More control over scope         |
| May protect more code than necessary | Can reduce lock duration        |

### Method

```java
public synchronized void increment() {
    count++;
}
```

### Block

```java
private final Object lock = new Object();

public void increment() {
    synchronized (lock) {
        count++;
    }
}
```

Blocks are not universally better. A synchronized method is appropriate when the entire method is one atomic operation.

---

## Question 22: What is an intrinsic lock?

Every Java object can act as an intrinsic lock, also called a **monitor**.

```java
synchronized (lockObject) {
    updateSharedState();
}
```

Before entering, the thread must acquire `lockObject`’s monitor.

When the block exits, the monitor is automatically released, including during exceptional exit.

### Instance synchronized method

Uses the current object’s monitor:

```java
public synchronized void update() {
}
```

Equivalent to:

```java
synchronized (this) {
}
```

### Static synchronized method

Uses the class object’s monitor:

```java
public static synchronized void reset() {
}
```

Equivalent to:

```java
synchronized (Example.class) {
}
```

Intrinsic locks are reentrant: a thread that already owns a monitor can acquire it again.

---

## Question 23: Can a static method be synchronized?

Yes.

```java
public static synchronized void reset() {
    total = 0;
}
```

It locks the class object:

```java
public static void reset() {
    synchronized (Counter.class) {
        total = 0;
    }
}
```

A static synchronized method does not use the monitor of any individual instance.

Consequently, an instance synchronized method and a static synchronized method do not automatically block each other because they use different monitors.

---

## Question 24: Can a constructor be synchronized?

A constructor cannot use the `synchronized` modifier:

```java
public synchronized Example() {
}
```

This is not valid Java syntax.

A synchronized block can technically appear inside a constructor:

```java
public Example() {
    synchronized (SOME_LOCK) {
        initializeSharedState();
    }
}
```

However, construction safety should not be explained by claiming that the object can never be visible to other threads. An object can escape during construction through:

* A static field
* A callback
* A listener registration
* Starting a thread from the constructor
* Passing `this` to another object

Unsafe escape:

```java
public Example(EventBus eventBus) {
    eventBus.register(this);
}
```

Another thread may access the object before construction finishes.

### Better practice

* Do not publish `this` from a constructor.
* Complete initialization before sharing the object.
* Prefer final fields where appropriate.
* Publish the constructed object through a safe mechanism.

---

# Common Mistakes

## Calling `run()` instead of `start()`

```java
thread.run();
```

No new thread is created.

---

## Swallowing interruption

```java
catch (InterruptedException exception) {
}
```

This breaks cooperative cancellation.

---

## Using daemon threads for critical work

Daemon work may be abandoned when the JVM exits.

---

## Assuming thread priority guarantees order

Priority is not a correctness mechanism.

---

## Using `if` around `wait()`

Incorrect:

```java
if (!ready) {
    lock.wait();
}
```

Correct:

```java
while (!ready) {
    lock.wait();
}
```

---

## Starting a thread from a constructor

```java
public Worker() {
    new Thread(this::run).start();
}
```

The new thread may observe the object before construction is complete.

---

## Assuming more threads always improve performance

Too many runnable threads can cause:

* Context switching
* Memory pressure
* Lock contention
* Downstream overload
* Higher latency

---

# Short Interview Summary

> A Java thread is an execution path inside a JVM process. Threads share heap objects and process resources but have independent stacks and execution state. Java defines six thread states: `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, and `TERMINATED`. I represent work with `Runnable` or `Callable` and normally submit it to an executor rather than manually creating a thread per task. I use `start()` to begin a new thread, treat interruption as cooperative cancellation, and use synchronization or concurrent utilities whenever threads share mutable state.

## Related Topics

* [Thread Lifecycle and JMM](thread-lifecycle-and-jmm.md)
* [ExecutorService](executor-service.md)
* [Synchronization](synchronization.md)
* [Race Conditions](race-conditions.md)
* [Virtual Threads](virtual-threads.md)
