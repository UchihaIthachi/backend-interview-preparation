# Scenario-Based Concurrency Questions

## Scenario: How would you implement the Producer–Consumer pattern?

### Recommended solution: bounded `BlockingQueue`

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ProducerConsumerDemo {

    private record Message(
            String payload,
            boolean stop
    ) {
        static Message data(String payload) {
            return new Message(payload, false);
        }

        static Message poisonPill() {
            return new Message("", true);
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        int consumerCount = 3;

        BlockingQueue<Message> queue =
                new ArrayBlockingQueue<>(100);

        ExecutorService consumers =
                Executors.newFixedThreadPool(consumerCount);

        for (int index = 0;
             index < consumerCount;
             index++) {

            consumers.submit(() -> {
                try {
                    while (true) {
                        Message message = queue.take();

                        if (message.stop()) {
                            return;
                        }

                        process(message.payload());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        try {
            for (int index = 1; index <= 500; index++) {
                queue.put(
                        Message.data("event-" + index)
                );
            }
        } finally {
            for (int index = 0;
                 index < consumerCount;
                 index++) {

                queue.put(Message.poisonPill());
            }
        }

        consumers.shutdown();

        if (!consumers.awaitTermination(
                30,
                TimeUnit.SECONDS
        )) {
            consumers.shutdownNow();
        }
    }

    private static void process(String payload) {
        System.out.println(
                Thread.currentThread().getName()
                        + " processed "
                        + payload
        );
    }
}
```

### Why this design works

- `put()` waits when the queue is full.
- `take()` waits when the queue is empty.
- A bounded queue provides backpressure.
- The queue supplies synchronization and visibility guarantees.
- One poison pill per consumer ensures every consumer terminates.

### Interview-ready answer

> I normally implement Producer–Consumer with a bounded `BlockingQueue`. Producers call `put()`, consumers call `take()`, and the bounded capacity prevents unlimited memory growth. For shutdown, I use interruption, executor lifecycle management, or one poison pill per consumer.

---

# Scenario 31: Mixed-Duration Jobs with Limited Workers

## Scenario

A video platform processes thumbnails, subtitles, and transcoding jobs. The tasks have different durations, and completed results should be handled immediately rather than in submission order.

## Solution

Use:

- A bounded `ThreadPoolExecutor`
- `ExecutorCompletionService` to consume results in completion order

```java
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class VariableTaskExecutor {

    private record MediaTask(
            String name,
            long durationMillis
    ) implements Callable<String> {

        @Override
        public String call()
                throws InterruptedException {

            Thread.sleep(durationMillis);

            return name
                    + " completed in "
                    + durationMillis
                    + " ms";
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        3,
                        3,
                        0,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(100),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );

        ExecutorCompletionService<String> completion =
                new ExecutorCompletionService<>(executor);

        List<MediaTask> jobs = List.of(
                new MediaTask("thumb-A", 300),
                new MediaTask("transcode-X", 1_500),
                new MediaTask("subs-M", 700),
                new MediaTask("thumb-B", 200),
                new MediaTask("transcode-Y", 1_200)
        );

        try {
            jobs.forEach(completion::submit);

            for (int index = 0;
                 index < jobs.size();
                 index++) {

                try {
                    String result =
                            completion.take().get();

                    System.out.println(result);
                } catch (ExecutionException exception) {
                    System.err.println(
                            "Job failed: "
                                    + exception.getCause()
                    );
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
```

### Important correction

`Executors.newFixedThreadPool()` has a fixed worker count but an unbounded queue. It should not be described as a fully bounded executor.

### Interview-ready answer

> I would use a bounded executor to limit workers and queued work, then wrap it with `ExecutorCompletionService`. That lets me process short tasks as soon as they finish instead of waiting behind an earlier long-running task.

---

# Scenario 32: Limit Concurrent Client Connections

## Scenario

Only three requests may access an expensive backend at the same time.

## Solution using `Semaphore`

```java
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class LimitedBackendClient {

    private final Semaphore permits =
            new Semaphore(3, true);

    public void handle(String clientName) {
        boolean acquired = false;

        try {
            acquired =
                    permits.tryAcquire(
                            2,
                            TimeUnit.SECONDS
                    );

            if (!acquired) {
                System.out.println(
                        clientName
                                + " rejected: no capacity"
                );

                return;
            }

            System.out.println(
                    clientName + " connected"
            );

            callBackend(clientName);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            if (acquired) {
                permits.release();

                System.out.println(
                        clientName + " disconnected"
                );
            }
        }
    }

    private void callBackend(String clientName)
            throws InterruptedException {

        Thread.sleep(1_000);
    }
}
```

### Critical rule

Release a permit only when acquisition succeeded.

This is unsafe:

```java
finally {
    permits.release();
}
```

If acquisition failed or the thread was interrupted before acquiring, releasing would incorrectly increase the number of permits.

### Interview-ready answer

> I would use a semaphore with three permits. Each operation acquires one permit before calling the backend and releases it in `finally` only if acquisition succeeded. A timed acquisition prevents callers from waiting indefinitely.

---

# Scenario 33: Process Results as Soon as They Finish

## Scenario

Several independent enrichment calls finish at different times. The application should process each completed result immediately.

## Solution

```java
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class CompletionOrderDemo {

    private record EnrichmentTask(
            String name,
            long delayMillis
    ) implements Callable<String> {

        @Override
        public String call()
                throws InterruptedException {

            Thread.sleep(delayMillis);
            return name + " finished";
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        ExecutorCompletionService<String> completion =
                new ExecutorCompletionService<>(executor);

        List<EnrichmentTask> tasks = List.of(
                new EnrichmentTask("pricing", 900),
                new EnrichmentTask("shipping", 300),
                new EnrichmentTask("reviews", 600)
        );

        try {
            tasks.forEach(completion::submit);

            for (int index = 0;
                 index < tasks.size();
                 index++) {

                Future<String> completed =
                        completion.poll(
                                2,
                                TimeUnit.SECONDS
                        );

                if (completed == null) {
                    System.err.println(
                            "Timed out waiting for result"
                    );

                    continue;
                }

                try {
                    render(completed.get());
                } catch (ExecutionException exception) {
                    renderFailure(exception.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void render(String result) {
        System.out.println(result);
    }

    private static void renderFailure(Throwable failure) {
        System.err.println(failure);
    }
}
```

### Interview-ready answer

> `ExecutorCompletionService` places finished futures into a completion queue. I submit all tasks and consume that queue, so execution remains bounded while results are handled in completion order.

---

# Scenario 34: Producer Hands Off Data and Consumer Waits

## Low-level solution using `wait()` and `notifyAll()`

```java
public final class SingleSlotBuffer<T> {

    private T value;
    private boolean available;

    public synchronized void put(T newValue)
            throws InterruptedException {

        while (available) {
            wait();
        }

        value = newValue;
        available = true;

        notifyAll();
    }

    public synchronized T take()
            throws InterruptedException {

        while (!available) {
            wait();
        }

        T result = value;

        value = null;
        available = false;

        notifyAll();

        return result;
    }
}
```

### Important rules

- Call `wait()`, `notify()`, and `notifyAll()` while owning the same monitor.
- Use `while`, not `if`, around the waiting condition.
- `wait()` releases the monitor while the thread waits.
- `Thread.sleep()` does not release held locks.
- Prefer `BlockingQueue` in production.

### Interview-ready answer

> I can implement a single-slot buffer with synchronized methods, guarded `while` loops, `wait()`, and `notifyAll()`. In production I would normally use a `BlockingQueue`, because it already implements this coordination safely.

---

# Scenario 35: Two Tasks Acquire Locks in Opposite Order

## Scenario

One task locks resource A then B, while another locks B then A.

## Solution: global lock ordering

```java
import java.util.concurrent.locks.ReentrantLock;

public final class ResourceManager {

    public static final class Resource {

        private final long id;
        private final ReentrantLock lock =
                new ReentrantLock();

        public Resource(long id) {
            this.id = id;
        }
    }

    public void useBoth(
            Resource firstResource,
            Resource secondResource
    ) {
        if (firstResource == secondResource) {
            firstResource.lock.lock();

            try {
                performOperation();
            } finally {
                firstResource.lock.unlock();
            }

            return;
        }

        Resource first =
                firstResource.id < secondResource.id
                        ? firstResource
                        : secondResource;

        Resource second =
                first == firstResource
                        ? secondResource
                        : firstResource;

        first.lock.lock();

        try {
            second.lock.lock();

            try {
                performOperation();
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    private void performOperation() {
        System.out.println("Using both resources");
    }
}
```

### Assumption

Resource IDs must provide a stable, unique ordering. If duplicate IDs are possible, use an additional deterministic tie-breaker.

### Alternative

Use timed `tryLock()` with bounded retries when failing and retrying is acceptable.

### Interview-ready answer

> This is a circular-wait deadlock. I would impose one global lock order so every path acquires the resources consistently. For operations where failure is acceptable, timed `tryLock()` plus bounded backoff is another option.

---

# Scenario 36: Thousands of Short Tasks

## Problem

Creating one platform thread per task wastes resources and allows uncontrolled concurrency.

## Bounded pool solution

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ParsingExecutor {

    public static void main(String[] args) {
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        4,
                        4,
                        0,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1_000),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );

        try {
            for (int index = 0;
                 index < 10_000;
                 index++) {

                int taskId = index;

                executor.execute(
                        () -> parse(taskId)
                );
            }
        } finally {
            executor.shutdown();
        }
    }

    private static void parse(int taskId) {
        System.out.println(
                "Parsing "
                        + taskId
                        + " on "
                        + Thread.currentThread().getName()
        );
    }
}
```

### Important correction

A cached thread pool should not be selected merely because work is I/O-heavy. Under sustained blocking load it can create a very large number of platform threads.

For high-concurrency blocking work, virtual threads may be appropriate, but downstream resources still need explicit limits.

### Interview-ready answer

> I would use a bounded executor so worker concurrency and queued backlog are both controlled. The rejection policy defines overload behavior. I would use virtual threads for suitable blocking workloads, but still limit database, HTTP, and other scarce resources.

---

# Scenario 37: Do Not Start Until All Subsystems Are Ready

## Solution using `CountDownLatch`

```java
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class StartupCoordinator {

    public static void main(String[] args)
            throws InterruptedException {

        List<String> components = List.of(
                "Cache",
                "Configuration",
                "Database"
        );

        CountDownLatch completed =
                new CountDownLatch(
                        components.size()
                );

        AtomicReference<Throwable> failure =
                new AtomicReference<>();

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        components.size()
                );

        for (String component : components) {
            executor.submit(() -> {
                try {
                    initialize(component);
                } catch (Throwable throwable) {
                    failure.compareAndSet(
                            null,
                            throwable
                    );
                } finally {
                    completed.countDown();
                }
            });
        }

        boolean finished =
                completed.await(
                        30,
                        TimeUnit.SECONDS
                );

        executor.shutdownNow();

        if (!finished) {
            throw new IllegalStateException(
                    "Startup timed out"
            );
        }

        if (failure.get() != null) {
            throw new IllegalStateException(
                    "Startup failed",
                    failure.get()
            );
        }

        System.out.println(
                "All required components are ready"
        );
    }

    private static void initialize(String component)
            throws InterruptedException {

        System.out.println(
                component + " initializing"
        );

        Thread.sleep(300);

        System.out.println(
                component + " ready"
        );
    }
}
```

### Important nuance

`countDown()` in `finally` prevents an infinite wait, but reaching zero means only that every worker finished—not that every worker succeeded.

Track failures separately.

### Interview-ready answer

> I would use a `CountDownLatch` with one count per required subsystem, wait with a timeout, and separately record initialization failures. A latch is a one-shot completion gate.

---

# Scenario 38: Workers Rendezvous at Each Phase

## Solution using `CyclicBarrier`

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PhaseWorker implements Runnable {

    private final int id;
    private final int phaseCount;
    private final CyclicBarrier barrier;

    public PhaseWorker(
            int id,
            int phaseCount,
            CyclicBarrier barrier
    ) {
        this.id = id;
        this.phaseCount = phaseCount;
        this.barrier = barrier;
    }

    @Override
    public void run() {
        try {
            for (int phase = 1;
                 phase <= phaseCount;
                 phase++) {

                performPhase(phase);

                barrier.await(
                        10,
                        TimeUnit.SECONDS
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (
                BrokenBarrierException
                | TimeoutException exception
        ) {
            System.err.println(
                    "Worker "
                            + id
                            + " aborted: "
                            + exception
            );
        }
    }

    private void performPhase(int phase)
            throws InterruptedException {

        Thread.sleep(200);

        System.out.println(
                "Worker "
                        + id
                        + " finished phase "
                        + phase
        );
    }
}
```

Create the barrier:

```java
int workerCount = 3;

CyclicBarrier barrier =
        new CyclicBarrier(
                workerCount,
                () -> System.out.println(
                        "Phase completed"
                )
        );
```

### Important point

If one participant is interrupted, times out, or fails at the barrier, the barrier becomes broken and other participants receive `BrokenBarrierException`.

### Interview-ready answer

> I would use a `CyclicBarrier` for a fixed group that repeatedly synchronizes at phase boundaries. It automatically resets after each completed cycle, and its barrier action can run once per phase.

---

# Scenario 39: Parallel Processing of a Large List

## Solution

```java
import java.util.List;
import java.util.stream.IntStream;

public final class ParallelScoring {

    public static void main(String[] args) {
        List<Integer> orderIds =
                IntStream.rangeClosed(1, 100_000)
                        .boxed()
                        .toList();

        List<Integer> scores =
                orderIds.parallelStream()
                        .map(ParallelScoring::score)
                        .toList();

        System.out.println(scores.size());
    }

    private static int score(int orderId) {
        return (orderId * 31) & 1_023;
    }
}
```

### Ordering correction

For an ordered source such as a list, ordered collection operations such as `toList()` normally preserve encounter order even when the stream is parallel.

This terminal operation is not ordered:

```java
orderIds.parallelStream()
        .map(ParallelScoring::score)
        .forEach(System.out::println);
```

Use:

```java
orderIds.parallelStream()
        .map(ParallelScoring::score)
        .forEachOrdered(System.out::println);
```

when ordered side effects are required, recognizing the potential performance cost.

### Good candidate

- Large data set
- CPU-heavy processing
- Independent elements
- Stateless functions
- Efficiently splittable source
- Measured improvement

### Poor candidate

- Database or HTTP calls
- Small collections
- Cheap transformations
- Shared mutable state
- Strictly ordered side effects
- A busy shared common pool

### Interview-ready answer

> Parallel streams are useful for measured CPU-bound processing over large, efficiently splittable data. I keep operations stateless, avoid shared mutation, and do not use them for blocking I/O.

---

# Scenario 40: Ensure Critical Work Finishes Before Exit

## Raw thread solution

```java
public final class CsvImport {

    public static void main(String[] args)
            throws InterruptedException {

        Thread worker =
                new Thread(
                        CsvImport::parseCsv,
                        "csv-worker"
                );

        worker.start();
        worker.join();

        System.out.println(
                "Import finished"
        );
    }

    private static void parseCsv() {
        System.out.println(
                "Parsing CSV"
        );
    }
}
```

### Important nuance

A non-daemon thread already keeps the JVM alive. `join()` is still useful because it:

- Makes the main thread wait
- Provides an explicit lifecycle boundary
- Allows subsequent code to run only after completion

An exception in the worker is not automatically rethrown by `join()`.

For multiple tasks, use an executor:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);

executor.submit(CsvImport::parseCsv);

executor.shutdown();

executor.awaitTermination(
        30,
        TimeUnit.SECONDS
);
```

### Interview-ready answer

> A non-daemon worker keeps the JVM alive, while `join()` explicitly makes the coordinator wait for it. For multiple tasks, I prefer executor shutdown and `awaitTermination()`.

---

# Scenario 41: A Class Already Extends Another Type

## Solution

Implement `Runnable` or `Callable` rather than extending `Thread`.

```java
public final class ReportGenerator
        extends BaseService
        implements Runnable {

    private final String reportId;

    public ReportGenerator(String reportId) {
        this.reportId = reportId;
    }

    @Override
    public void run() {
        generate(reportId);
    }

    private void generate(String reportId) {
        System.out.println(
                "Generating " + reportId
        );
    }
}
```

Named thread factory:

```java
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NamedThreadFactory
        implements ThreadFactory {

    private final AtomicInteger sequence =
            new AtomicInteger();

    @Override
    public Thread newThread(Runnable task) {
        return new Thread(
                task,
                "report-worker-"
                        + sequence.incrementAndGet()
        );
    }
}
```

### Exception-handling nuance

An `UncaughtExceptionHandler` can observe exceptions from tasks run using `execute()`.

When using `submit()`, task exceptions are captured in the returned `Future`, so the handler may not receive them.

```java
Future<?> future =
        executor.submit(
                new ReportGenerator("R-101")
        );

try {
    future.get();
} catch (ExecutionException exception) {
    logFailure(exception.getCause());
}
```

### Interview-ready answer

> I separate the task from its execution by implementing `Runnable` or `Callable`. That preserves the class’s existing inheritance and lets an executor control threading. I use a `ThreadFactory` for naming and inspect returned futures for task failures.

---

# Scenario 42: Prevent an Inconsistent Account Balance

```java
import java.util.concurrent.locks.ReentrantLock;

public final class Account {

    private final ReentrantLock lock =
            new ReentrantLock();

    private long balance;

    public Account(long initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException(
                    "Initial balance cannot be negative"
            );
        }

        balance = initialBalance;
    }

    public boolean withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        lock.lock();

        try {
            if (balance < amount) {
                return false;
            }

            balance -= amount;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public long currentBalance() {
        lock.lock();

        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }
}
```

### Why locking is required

The following must occur atomically:

```text
Check available balance
→ subtract amount
```

Two threads must not both pass the balance check using the same old value.

### Distributed limitation

A Java lock protects only threads sharing that exact in-memory object inside one JVM. It does not coordinate multiple application instances.

Distributed account correctness normally requires:

- Database transactions
- Row-level locking
- Optimistic version checks
- Atomic database updates
- Idempotency

### Interview-ready answer

> I protect the balance check and mutation with one private lock so they form one atomic operation. For multiple service instances, I enforce the invariant at the database or transactional storage layer.

---

# Scenario 43: Consumer Occasionally Waits Forever

## Typical causes

- Using `if` instead of `while`
- Waiting and notifying on different objects
- Updating state outside the synchronized region
- Using `notify()` with multiple producer and consumer roles
- A producer terminating without a shutdown signal
- Swallowing interruption

## Correct monitor implementation

```java
import java.util.ArrayDeque;
import java.util.Queue;

public final class SafeBuffer<T> {

    private final Queue<T> queue =
            new ArrayDeque<>();

    private final int capacity;

    public SafeBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T value)
            throws InterruptedException {

        while (queue.size() == capacity) {
            wait();
        }

        queue.add(value);
        notifyAll();
    }

    public synchronized T take()
            throws InterruptedException {

        while (queue.isEmpty()) {
            wait();
        }

        T value = queue.remove();
        notifyAll();

        return value;
    }
}
```

### Preferred solution

```java
BlockingQueue<T> queue =
        new ArrayBlockingQueue<>(capacity);
```

### Interview-ready answer

> I would verify that the condition is checked in a `while` loop, that all threads synchronize on the same monitor, and that signalling happens after the state change. In production I would replace the custom monitor code with a `BlockingQueue`.

---

# Scenario 44: Avoid Deadlock in a Two-Account Transfer

```java
import java.util.concurrent.locks.ReentrantLock;

public final class BankAccount {

    private final long id;
    private final ReentrantLock lock =
            new ReentrantLock();

    private long balance;

    public BankAccount(long id, long balance) {
        this.id = id;
        this.balance = balance;
    }
}
```

Transfer service:

```java
public final class TransferService {

    public boolean transfer(
            BankAccount source,
            BankAccount destination,
            long amount
    ) {
        if (source == destination) {
            return true;
        }

        BankAccount first =
                source.id < destination.id
                        ? source
                        : destination;

        BankAccount second =
                first == source
                        ? destination
                        : source;

        first.lock.lock();

        try {
            second.lock.lock();

            try {
                if (source.balance < amount) {
                    return false;
                }

                source.balance -= amount;
                destination.balance += amount;

                return true;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }
}
```

In real code, expose the necessary operations without unnecessarily making account internals public. The example focuses on the lock-ordering concept.

### Interview-ready answer

> I acquire both account locks according to a stable global order, such as ascending account ID. This removes circular waiting between opposite-direction transfers.

---

# Scenario 45: Prevent Starvation Under Contention

```java
import java.util.concurrent.locks.ReentrantLock;

public final class FairState {

    private final ReentrantLock lock =
            new ReentrantLock(true);

    private int value;

    public void increment() {
        lock.lock();

        try {
            value++;
        } finally {
            lock.unlock();
        }
    }
}
```

### Important nuance

A fair lock:

- Generally favors longer-waiting threads
- Reduces lock-starvation risk
- Is not a perfect FIFO execution scheduler
- May reduce throughput
- Does not solve CPU, executor, database, or queue starvation

Before enabling fairness, investigate:

- Lock-hold time
- Slow operations inside the lock
- One global lock protecting unrelated data
- Repeated immediate reacquisition
- Whether state can be partitioned

### Interview-ready answer

> A fair `ReentrantLock` can reduce starvation among queued lock waiters, but it trades throughput for more predictable acquisition. I would first minimize and partition the critical section, then enable fairness if bounded waiting is required.

---

# Scenario 46: Run Recurring Tasks Reliably

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ScheduledJobs {

    public static void main(String[] args) {
        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(2);

        Runnable cacheRefresh = () -> {
            try {
                refreshCache();
            } catch (RuntimeException exception) {
                System.err.println(
                        "Cache refresh failed: "
                                + exception
                );
            }
        };

        Runnable heartbeat = () -> {
            try {
                publishHeartbeat();
            } catch (RuntimeException exception) {
                System.err.println(
                        "Heartbeat failed: "
                                + exception
                );
            }
        };

        scheduler.scheduleAtFixedRate(
                cacheRefresh,
                0,
                1,
                TimeUnit.MINUTES
        );

        scheduler.scheduleWithFixedDelay(
                heartbeat,
                0,
                5,
                TimeUnit.SECONDS
        );
    }

    private static void refreshCache() {
    }

    private static void publishHeartbeat() {
    }
}
```

### Fixed rate vs fixed delay

| Method                     | Schedule                                     |
| -------------------------- | -------------------------------------------- |
| `scheduleAtFixedRate()`    | Based on planned start times                 |
| `scheduleWithFixedDelay()` | Delay starts after the previous run finishes |

### Important nuances

- Repeated execution of the same periodic task does not overlap with itself.
- If one execution runs late, later fixed-rate executions may start late.
- An uncaught exception suppresses future executions of that periodic task.
- Multiple scheduler threads allow different scheduled tasks to run concurrently.

### Interview-ready answer

> I use `ScheduledExecutorService`, select fixed rate for cadence and fixed delay for spacing after completion, and catch task exceptions because an uncaught exception stops future executions of that periodic task.

---

# Scenario 47: Fan Out to Several Services and Aggregate

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ProductAggregator {

    private final ExecutorService ioExecutor =
            Executors.newFixedThreadPool(12);

    public ProductPage load(String sku) {
        CompletableFuture<Double> price =
                CompletableFuture
                        .supplyAsync(
                                () -> fetchPrice(sku),
                                ioExecutor
                        )
                        .orTimeout(
                                2,
                                TimeUnit.SECONDS
                        )
                        .exceptionally(failure -> {
                            logFailure("price", failure);
                            return null;
                        });

        CompletableFuture<Integer> stock =
                CompletableFuture
                        .supplyAsync(
                                () -> fetchStock(sku),
                                ioExecutor
                        )
                        .orTimeout(
                                2,
                                TimeUnit.SECONDS
                        )
                        .exceptionally(failure -> {
                            logFailure("stock", failure);
                            return null;
                        });

        CompletableFuture<Double> rating =
                CompletableFuture
                        .supplyAsync(
                                () -> fetchRating(sku),
                                ioExecutor
                        )
                        .orTimeout(
                                2,
                                TimeUnit.SECONDS
                        )
                        .exceptionally(failure -> {
                            logFailure("rating", failure);
                            return null;
                        });

        return CompletableFuture
                .allOf(price, stock, rating)
                .thenApply(ignored ->
                        new ProductPage(
                                price.join(),
                                stock.join(),
                                rating.join()
                        )
                )
                .join();
    }

    private double fetchPrice(String sku) {
        return 999.0;
    }

    private int fetchStock(String sku) {
        return 12;
    }

    private double fetchRating(String sku) {
        return 4.6;
    }

    private void logFailure(
            String service,
            Throwable failure
    ) {
        System.err.println(
                service + " failed: " + failure
        );
    }

    public record ProductPage(
            Double price,
            Integer stock,
            Double rating
    ) {
    }
}
```

### Design considerations

- Defaults such as zero may have business meaning and can hide failures.
- Consider explicit “unavailable” values.
- Add per-call timeouts.
- Bound concurrency according to downstream capacity.
- Retry only idempotent transient failures.
- Record fallback and timeout metrics.

### Interview-ready answer

> I launch independent calls concurrently, recover each branch separately, coordinate with `allOf()`, and aggregate after all branches complete. I add per-call timeouts and avoid silently mapping every failure to a valid-looking business value.

---

# Scenario 48: Producer–Consumer with Backpressure and Multiple Consumers

## Correct poison-pill protocol

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class IndexingPipeline {

    private record Event(
            String value,
            boolean stop
    ) {
        static Event data(String value) {
            return new Event(value, false);
        }

        static Event stop() {
            return new Event("", true);
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        int consumerCount = 3;

        BlockingQueue<Event> queue =
                new ArrayBlockingQueue<>(100);

        ExecutorService consumers =
                Executors.newFixedThreadPool(
                        consumerCount
                );

        for (int index = 0;
             index < consumerCount;
             index++) {

            consumers.submit(() -> {
                try {
                    while (true) {
                        Event event = queue.take();

                        if (event.stop()) {
                            return;
                        }

                        index(event.value());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        try {
            for (int index = 1;
                 index <= 500;
                 index++) {

                queue.put(
                        Event.data("event-" + index)
                );
            }
        } finally {
            for (int index = 0;
                 index < consumerCount;
                 index++) {

                queue.put(Event.stop());
            }
        }

        consumers.shutdown();

        if (!consumers.awaitTermination(
                30,
                TimeUnit.SECONDS
        )) {
            consumers.shutdownNow();
        }
    }

    private static void index(String event) {
    }
}
```

### Why not pass one sentinel between consumers?

Reinserting one sentinel can work in simple cases, but it complicates failure handling and makes the protocol dependent on every consumer correctly putting it back.

One poison pill per consumer is clearer.

### Kafka-specific consideration

When events originate from Kafka, also define:

- When offsets are committed
- What happens if indexing succeeds but offset commit fails
- Whether processing is idempotent
- Retry and dead-letter behavior

### Interview-ready answer

> I use a bounded `BlockingQueue` for backpressure and one poison pill per consumer for orderly shutdown. For Kafka ingestion, I also align queue processing with offset-commit and idempotency requirements.

---

# Scenario 49: Dynamic Participants Across Phases

## Correct `Phaser` registration

Register participants before submitting or starting them so the coordinator cannot advance before workers are registered.

```java
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public final class DynamicPipeline {

    private record WorkerDefinition(
            String name,
            int phaseCount
    ) {
    }

    public static void main(String[] args) {
        List<WorkerDefinition> definitions =
                List.of(
                        new WorkerDefinition("A", 3),
                        new WorkerDefinition("B", 2),
                        new WorkerDefinition("C", 3)
                );

        Phaser phaser = new Phaser(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        definitions.size()
                );

        for (WorkerDefinition definition
                : definitions) {

            phaser.register();

            executor.submit(() -> {
                try {
                    for (int phase = 1;
                         phase <= definition.phaseCount();
                         phase++) {

                        performPhase(
                                definition.name(),
                                phase
                        );

                        phaser.arriveAndAwaitAdvance();
                    }
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }

        int maximumPhases =
                definitions.stream()
                        .mapToInt(
                                WorkerDefinition::phaseCount
                        )
                        .max()
                        .orElse(0);

        for (int phase = 1;
             phase <= maximumPhases;
             phase++) {

            phaser.arriveAndAwaitAdvance();

            System.out.println(
                    "Coordinator completed phase "
                            + phase
            );
        }

        phaser.arriveAndDeregister();
        executor.shutdown();
    }

    private static void performPhase(
            String worker,
            int phase
    ) {
        System.out.println(
                worker
                        + " finished phase "
                        + phase
        );
    }
}
```

### Interview-ready answer

> I use `Phaser` when participants can register or deregister between phases. I register them before execution to avoid a startup race, and each worker deregisters after its final phase.

---

# Scenario 50: Timebox and Cancel an Asynchronous Pipeline

## Important correction

`CompletableFuture.orTimeout()` changes the completion state of the future, but it does not necessarily stop the underlying operation.

Similarly:

```java
future.cancel(true);
```

does not guarantee that the supplier running through `CompletableFuture.supplyAsync()` will be interrupted.

Cancellation also does not automatically propagate backward through every dependent stage.

## Timeout and fallback example

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class TimeoutPipeline {

    private final ExecutorService ioExecutor =
            Executors.newFixedThreadPool(6);

    public CompletableFuture<String> execute() {
        CompletableFuture<String> first =
                call("A", 800)
                        .orTimeout(
                                500,
                                TimeUnit.MILLISECONDS
                        )
                        .exceptionally(failure ->
                                "A:default"
                        );

        CompletableFuture<String> second =
                call("B", 200)
                        .orTimeout(
                                500,
                                TimeUnit.MILLISECONDS
                        );

        CompletableFuture<String> third =
                call("C", 300)
                        .orTimeout(
                                500,
                                TimeUnit.MILLISECONDS
                        );

        return first
                .thenCombine(
                        second,
                        (a, b) -> a + "+" + b
                )
                .thenCombine(
                        third,
                        (ab, c) -> ab + "+" + c
                );
    }

    private CompletableFuture<String> call(
            String name,
            long durationMillis
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(durationMillis);
                return name;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CompletionException(exception);
            }
        }, ioExecutor);
    }
}
```

## To stop real work

Configure cancellation where the operation actually occurs:

- HTTP request timeout
- Socket read timeout
- JDBC query timeout
- Cancellation-aware client API
- Explicit `Future.cancel(true)`
- Cooperative cancellation token
- Closing the underlying resource when safe

Example with a directly submitted `Future`:

```java
Future<String> future =
        executor.submit(
                () -> blockingClient.call()
        );

try {
    return future.get(
            500,
            TimeUnit.MILLISECONDS
    );
} catch (TimeoutException exception) {
    future.cancel(true);
    throw exception;
}
```

Even then, the task must respond to interruption.

### Interview-ready answer

> I use `orTimeout()` or `completeOnTimeout()` to control the pipeline’s completion, but I do not assume that they stop the underlying I/O. Real cancellation must be implemented at the operation boundary with interruptible tasks, client timeouts, cancellation-aware APIs, or explicit resource closure.

---

# Corrections Applied to the Original Draft

| Scenario | Correction                                                         |
| -------- | ------------------------------------------------------------------ |
| 31       | Replaced unbounded fixed-pool queue with a bounded executor        |
| 32       | Released semaphore permit only after successful acquisition        |
| 37       | Added failure propagation and startup timeout                      |
| 38       | Preserved interruption and added barrier timeout                   |
| 39       | Corrected claim that ordered collection results lose order         |
| 41       | Explained why `submit()` may bypass uncaught-exception handling    |
| 42       | Clarified JVM-local locking versus distributed correctness         |
| 46       | Clarified fixed-rate behavior and periodic-task exception handling |
| 48       | Replaced sentinel reinsertion with one poison pill per consumer    |
| 49       | Registered phaser participants before starting tasks               |
| 50       | Corrected `CompletableFuture` timeout and cancellation semantics   |

# Short Interview Summary

> I choose concurrency utilities according to the coordination problem: bounded executors for controlled task execution, completion services for completion-order processing, semaphores for scarce-resource concurrency, blocking queues for producer-consumer backpressure, latches for one-time completion, barriers or phasers for phases, and global lock ordering for deadlock prevention. I also treat interruption and cancellation as cooperative mechanisms and never assume that a future timeout automatically stops the underlying work.
