The attached draft contains concurrency scenarios 51–70 plus an unanswered production-deadlock question. The version below fixes the malformed code, completes the missing answer, and corrects several concurrency issues in the original examples.

# Scenario-Based Concurrency Questions

## Scenario: Your application has intermittent deadlocks in production. How will you detect and resolve them?

### Answer

I would first confirm that the problem is an actual deadlock rather than ordinary lock contention.

My investigation would be:

1. Capture several thread dumps a few seconds apart.
2. Look for Java’s deadlock report and circular lock dependencies.
3. Identify the threads, locks, and code paths involved.
4. Use Java Flight Recorder to inspect lock contention and monitor blocking.
5. Determine why the locks are acquired in inconsistent order.
6. fix the locking design rather than trying to catch or ignore the problem.

### Production detection

Useful commands include:

```bash
jcmd <pid> Thread.print -l
```

```bash
jstack -l <pid>
```

A deadlock generally appears as a cycle:

```text
Thread A:
    owns Lock 1
    waits for Lock 2

Thread B:
    owns Lock 2
    waits for Lock 1
```

Capture multiple dumps because one snapshot may only show temporary contention.

### Programmatic detection

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

public final class DeadlockDetector {

    private final ThreadMXBean threadMxBean =
            ManagementFactory.getThreadMXBean();

    public void checkForDeadlocks() {
        long[] threadIds =
                threadMxBean.findDeadlockedThreads();

        if (threadIds == null) {
            return;
        }

        ThreadInfo[] threadInfos =
                threadMxBean.getThreadInfo(
                        threadIds,
                        true,
                        true
                );

        Arrays.stream(threadInfos)
                .forEach(System.err::println);
    }
}
```

`findDeadlockedThreads()` detects cycles involving both:

- Intrinsic monitors used by `synchronized`
- Ownable synchronizers such as `ReentrantLock`

### Common root cause

```java
// Thread A
synchronized (accountA) {
    synchronized (accountB) {
        transfer();
    }
}

// Thread B
synchronized (accountB) {
    synchronized (accountA) {
        transfer();
    }
}
```

The two threads acquire the same locks in opposite order.

### Fix using consistent lock ordering

```java
public void transfer(
        Account source,
        Account destination,
        long amount
) {
    Account first;
    Account second;

    if (source.id() < destination.id()) {
        first = source;
        second = destination;
    } else {
        first = destination;
        second = source;
    }

    synchronized (first) {
        synchronized (second) {
            source.withdraw(amount);
            destination.deposit(amount);
        }
    }
}
```

Because every thread follows the same global order, circular waiting cannot form.

### Other resolution strategies

- Reduce nested locking.
- Keep critical sections small.
- Never perform slow network or database calls while holding a lock.
- Use `tryLock()` with a timeout when failing or retrying is acceptable.
- Avoid calling unknown callback code while holding internal locks.
- Partition unrelated state behind separate locks.
- Use message passing or single-owner processing.
- Add timeouts, metrics, and alerts for abnormal lock-wait duration.

### Important production point

Java cannot safely force another thread to release a lock. An immediate production restart may be required to restore service, but the permanent solution is to correct the lock design.

### Interview-ready answer

> I would capture multiple thread dumps using `jcmd` or `jstack`, identify the circular dependency and lock owners, and confirm it with Java Flight Recorder or `ThreadMXBean`. I would then eliminate the cycle using consistent lock ordering, smaller critical sections, timed lock acquisition, or a design with less shared mutable state. A restart may restore service temporarily, but it does not resolve the root cause.

---

# Scenario 51: Parallel Stream Corrupts Output Due to Shared Mutable State

## Scenario

You convert a large list into a map using `parallelStream()`, but occasionally observe missing or incorrect entries because multiple threads write to a shared `HashMap`.

## Incorrect implementation

```java
Map<Integer, String> result = new HashMap<>();

ids.parallelStream()
        .forEach(id ->
                result.put(id, "V" + id)
        );
```

`HashMap` is not thread-safe. Concurrent writes can cause lost updates or corrupt internal state.

## Correct implementation

```java
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ParallelStreamSafeCollect {

    public static void main(String[] args) {
        List<Integer> ids =
                IntStream.rangeClosed(1, 10_000)
                        .boxed()
                        .toList();

        ConcurrentMap<Integer, String> result =
                ids.parallelStream()
                        .collect(Collectors.toConcurrentMap(
                                Function.identity(),
                                id -> "V" + id,
                                (existing, replacement) ->
                                        existing
                        ));

        System.out.println(result.size());
    }
}
```

The merge function is necessary when duplicate keys are possible.

### Key points

- Avoid modifying shared mutable containers inside parallel operations.
- Prefer stream collectors.
- Accumulation and combining logic must be associative.
- `toConcurrentMap()` does not preserve encounter order.
- `forEachOrdered()` preserves order but may reduce parallel performance.

### Interview-ready answer

> A parallel stream may execute multiple elements concurrently. Mutating one shared `HashMap` creates a data race. I would use `toConcurrentMap()` or another correctly designed collector instead of manually sharing mutable state.

---

# Scenario 52: Main Must Wait Until All Workers Finish Initialization

## Scenario

Five worker tasks preload caches and initialize connections. The application must not start serving requests until all workers have finished successfully.

## Solution using `CountDownLatch`

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class StartupCoordinator {

    public static void main(String[] args)
            throws InterruptedException {

        int workerCount = 5;

        CountDownLatch ready =
                new CountDownLatch(workerCount);

        AtomicReference<Throwable> failure =
                new AtomicReference<>();

        ExecutorService executor =
                Executors.newFixedThreadPool(workerCount);

        for (int index = 1; index <= workerCount; index++) {
            String workerName = "Worker-" + index;

            executor.submit(() -> {
                try {
                    initialize(workerName);
                } catch (Throwable throwable) {
                    failure.compareAndSet(null, throwable);
                } finally {
                    ready.countDown();
                }
            });
        }

        boolean completed =
                ready.await(30, TimeUnit.SECONDS);

        executor.shutdown();

        if (!completed) {
            executor.shutdownNow();
            throw new IllegalStateException(
                    "Initialization timed out"
            );
        }

        if (failure.get() != null) {
            throw new IllegalStateException(
                    "Initialization failed",
                    failure.get()
            );
        }

        System.out.println(
                "All workers are ready. Starting server."
        );
    }

    private static void initialize(String name)
            throws InterruptedException {

        System.out.println(name + " initializing");
        Thread.sleep(300);
        System.out.println(name + " ready");
    }
}
```

### Important nuance

Calling `countDown()` in `finally` prevents the application from waiting forever, but the application must separately record whether initialization succeeded.

Otherwise, the latch can reach zero even though one worker failed.

### Key points

- `CountDownLatch` is a one-shot synchronization gate.
- `await()` is better than polling or sleeping.
- Always use a timeout in production startup logic.
- Use `CyclicBarrier` or `Phaser` for reusable phases.

### Interview-ready answer

> I would initialize a `CountDownLatch` with the number of workers. Each worker decrements it in `finally`, and the main thread waits with a timeout. I would also propagate worker failures, because reaching zero only means the workers finished, not that they all succeeded.

---

# Scenario 53: Parallel and Sequential Phases in an ETL Pipeline

## Scenario

Multiple workers extract data in parallel. All extraction must complete before one consolidation step runs. Workers then transform in parallel, and loading begins only after every transformation finishes.

## Solution using two barriers

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EtlPipeline {

    private final CyclicBarrier extractionBarrier;
    private final CyclicBarrier transformationBarrier;

    public EtlPipeline(int workers) {
        extractionBarrier =
                new CyclicBarrier(
                        workers,
                        this::consolidate
                );

        transformationBarrier =
                new CyclicBarrier(workers);
    }

    public void executeWorker(int workerId) {
        try {
            extract(workerId);
            extractionBarrier.await();

            transform(workerId);
            transformationBarrier.await();

            load(workerId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException exception) {
            throw new IllegalStateException(
                    "ETL barrier was broken",
                    exception
            );
        }
    }

    private void extract(int id) {
        System.out.println("T" + id + " extracting");
    }

    private void consolidate() {
        System.out.println(
                "Running consolidation exactly once"
        );
    }

    private void transform(int id) {
        System.out.println("T" + id + " transforming");
    }

    private void load(int id) {
        System.out.println("T" + id + " loading");
    }

    public static void main(String[] args) {
        int workers = 4;

        EtlPipeline pipeline =
                new EtlPipeline(workers);

        ExecutorService executor =
                Executors.newFixedThreadPool(workers);

        for (int id = 1; id <= workers; id++) {
            int workerId = id;
            executor.submit(
                    () -> pipeline.executeWorker(workerId)
            );
        }

        executor.shutdown();
    }
}
```

### Correction to the original design

A single `CyclicBarrier` with a barrier action would run that action every time the barrier is reused. With two `await()` calls, consolidation would run twice.

Use:

- One barrier with the consolidation action
- A second barrier without that action

### Interview-ready answer

> I would use a `CyclicBarrier` after extraction with consolidation as its barrier action. I would use another barrier after transformation if loading must wait for every transform. `CyclicBarrier` is reusable, but its action runs on every completed barrier cycle.

---

# Scenario 54: Cancel a Long-Running Task After a Deadline

## Solution using `Future`

```java
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ReportTimeoutDemo {

    private record Report(int id)
            implements Callable<String> {

        @Override
        public String call()
                throws InterruptedException {

            for (int step = 0; step < 20; step++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                Thread.sleep(500);
            }

            return "Report " + id;
        }
    }

    public static void main(String[] args) {
        ExecutorService executor =
                Executors.newFixedThreadPool(4);

        Future<String> future =
                executor.submit(new Report(42));

        try {
            String result =
                    future.get(5, TimeUnit.SECONDS);

            System.out.println(result);
        } catch (TimeoutException exception) {
            System.out.println(
                    "Report exceeded its deadline"
            );

            future.cancel(true);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            future.cancel(true);
        } catch (Exception exception) {
            System.err.println(
                    "Report failed: " + exception
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
```

### Important point

`cancel(true)` does not forcibly terminate a task. It requests interruption.

The task must cooperate by:

- Checking its interrupted status
- Responding to `InterruptedException`
- Using libraries with cancellation-aware timeouts

### Interview-ready answer

> I would call `Future.get()` with a timeout. On `TimeoutException`, I would call `cancel(true)`, but I would also ensure that the task and its blocking operations respond to interruption. Cancellation in Java is cooperative.

---

# Scenario 55: A Parallel Stream Is Slower on a Small Collection

Parallel streams add:

- Data partitioning
- Task scheduling
- Worker coordination
- Result merging
- Shared common-pool contention

For a list of 100 integers, this overhead is usually larger than the work being performed.

```java
List<Integer> squares = numbers.stream()
        .map(number -> number * number)
        .toList();
```

Parallel processing becomes a candidate only when:

- The input is sufficiently large.
- Work per item is expensive.
- The work is CPU-bound.
- Operations are independent and stateless.
- The source divides efficiently.
- Measurement proves an improvement.

### Important benchmarking point

Do not use a single `System.nanoTime()` comparison as evidence. JVM warm-up, JIT compilation and dead-code elimination can invalidate the result.

Use JMH for reliable microbenchmarks.

### Interview-ready answer

> Parallel execution has splitting, scheduling and joining overhead. For small or cheap workloads, that overhead exceeds the useful work. I use parallel streams only for measured CPU-intensive workloads and never assume that “parallel” means “faster.”

---

# Scenario 56: Many Readers and an Occasional Writer

## Solution using `StampedLock`

```java
import java.util.concurrent.locks.StampedLock;

public final class PriceBoard {

    private final StampedLock lock =
            new StampedLock();

    private double price = 100.0;
    private long updateTime;

    public void update(
            double newPrice,
            long newUpdateTime
    ) {
        long stamp = lock.writeLock();

        try {
            price = newPrice;
            updateTime = newUpdateTime;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public PriceSnapshot read() {
        long stamp = lock.tryOptimisticRead();

        double observedPrice = price;
        long observedTime = updateTime;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();

            try {
                observedPrice = price;
                observedTime = updateTime;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return new PriceSnapshot(
                observedPrice,
                observedTime
        );
    }

    public record PriceSnapshot(
            double price,
            long updateTime
    ) {
    }
}
```

### Important points

- Copy all related fields before calling `validate()`.
- If validation fails, repeat the read under a real read lock.
- `StampedLock` is not reentrant.
- It does not directly support conditions.
- For one independently updated primitive value, `volatile` may be simpler.
- Measure before assuming it is faster than `ReentrantReadWriteLock`.

### Interview-ready answer

> For read-dominated compound state, I may use `StampedLock` with optimistic reads. I copy the state, validate the stamp and fall back to a read lock if a write occurred. For one simple value, a volatile field may be a clearer solution.

---

# Scenario 57: Load, Process and Save Must Execute in Order

For result-dependent stages, `CompletableFuture` is generally clearer than chaining latches.

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class OrderedPipeline {

    public static void main(String[] args) {
        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        try {
            CompletableFuture<Void> pipeline =
                    CompletableFuture
                            .supplyAsync(
                                    OrderedPipeline::load,
                                    executor
                            )
                            .thenApplyAsync(
                                    OrderedPipeline::process,
                                    executor
                            )
                            .thenAcceptAsync(
                                    OrderedPipeline::save,
                                    executor
                            )
                            .orTimeout(
                                    30,
                                    TimeUnit.SECONDS
                            );

            pipeline.join();
        } finally {
            executor.shutdown();
        }
    }

    private static String load() {
        System.out.println("Loading");
        return "raw-data";
    }

    private static String process(String data) {
        System.out.println("Processing " + data);
        return "processed-data";
    }

    private static void save(String result) {
        System.out.println("Saving " + result);
    }
}
```

### Interview-ready answer

> A latch can enforce stage gates, but `CompletableFuture` is better when one stage produces the input for the next. It propagates results and failures through the pipeline without manually managing multiple latches.

---

# Scenario 58: Detect and Recover from Livelock

## Definition

In livelock, threads remain active and repeatedly react to each other, but no useful work is completed.

Symmetric retries can cause this:

```text
Thread A releases and retries.
Thread B releases and retries.
Both collide again.
```

## Mitigation using bounded retries and jitter

```java
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public final class LivelockAvoidance {

    public boolean perform(
            Lock first,
            Lock second
    ) throws InterruptedException {

        int maximumAttempts = 10;

        for (int attempt = 1;
             attempt <= maximumAttempts;
             attempt++) {

            boolean firstAcquired =
                    first.tryLock(
                            50,
                            TimeUnit.MILLISECONDS
                    );

            if (!firstAcquired) {
                backoff();
                continue;
            }

            try {
                boolean secondAcquired =
                        second.tryLock(
                                50,
                                TimeUnit.MILLISECONDS
                        );

                if (secondAcquired) {
                    try {
                        performWork();
                        return true;
                    } finally {
                        second.unlock();
                    }
                }
            } finally {
                first.unlock();
            }

            backoff();
        }

        return false;
    }

    private void backoff()
            throws InterruptedException {

        long delay =
                ThreadLocalRandom.current()
                        .nextLong(10, 100);

        Thread.sleep(delay);
    }

    private void performWork() {
        System.out.println("Work completed");
    }
}
```

### Correction to the original example

A `volatile boolean` with:

```java
if (!resourceInUse) {
    resourceInUse = true;
}
```

is not atomic. Two threads may both observe `false` and both enter.

### Interview-ready answer

> Livelock means threads are running but repeatedly interfering with each other. I would detect high retry activity without successful completion and break the symmetry using bounded retries, randomized or exponential backoff, ownership ordering or a centralized coordinator.

---

# Scenario 59: Cancel Tasks Cleanly

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class CancellationDemo {

    private static final class CancellableTask
            implements Runnable {

        @Override
        public void run() {
            try {
                for (int step = 0; step < 100; step++) {
                    if (Thread.currentThread()
                            .isInterrupted()) {

                        return;
                    }

                    Thread.sleep(500);
                    System.out.println("Step " + step);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                cleanUp();
            }
        }

        private void cleanUp() {
            System.out.println("Cleaning up resources");
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        Future<?> future =
                executor.submit(new CancellableTask());

        Thread.sleep(1_200);

        future.cancel(true);
        executor.shutdownNow();

        executor.awaitTermination(
                5,
                TimeUnit.SECONDS
        );
    }
}
```

### Important points

- `cancel(true)` requests interruption.
- `Thread.interrupted()` checks and clears the flag.
- `isInterrupted()` checks without clearing it.
- Cleanup belongs in `finally`.
- Network and database calls also need their own timeouts.

### Interview-ready answer

> I retain the returned `Future` and call `cancel(true)`. The task must treat interruption as a cancellation request, stop at safe points and release resources in `finally`.

---

# Scenario 60: Parallel Aggregation of a Large Dataset

```java
import java.util.stream.LongStream;

public final class SalesAggregator {

    public static void main(String[] args) {
        long totalCents =
                LongStream.rangeClosed(
                                1,
                                10_000_000
                        )
                        .parallel()
                        .sum();

        System.out.println(totalCents);
    }
}
```

Using primitive streams avoids boxing each value.

### Conditions for parallel benefit

- Large in-memory input
- CPU-bound processing
- Associative reduction
- Stateless operations
- Efficient source splitting
- Available processor capacity
- Benchmark-confirmed improvement

For financial calculations, use an appropriate representation such as integer minor units or carefully designed `BigDecimal` operations.

### Interview-ready answer

> A parallel reduction can work well when the data is large, in memory and CPU-bound, and when the reduction operation is associative. I would use primitive streams where possible and compare the implementation against a sequential version using realistic benchmarks.

---

# Scenario 61: Limit an External API to Three Concurrent Calls

```java
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class ApiClient {

    private final Semaphore permits =
            new Semaphore(3, true);

    public void callApi(String requestName) {
        boolean acquired = false;

        try {
            acquired =
                    permits.tryAcquire(
                            2,
                            TimeUnit.SECONDS
                    );

            if (!acquired) {
                throw new IllegalStateException(
                        "API concurrency limit unavailable"
                );
            }

            invokeRemoteApi(requestName);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            if (acquired) {
                permits.release();
            }
        }
    }

    private void invokeRemoteApi(String requestName)
            throws InterruptedException {

        System.out.println(requestName + " started");
        Thread.sleep(1_000);
        System.out.println(requestName + " finished");
    }
}
```

### Important correction

Do not release a permit when acquisition failed. Track whether the permit was successfully acquired.

### Concurrency limit vs rate limit

A semaphore limits simultaneous calls:

```text
Maximum three active calls
```

It does not directly enforce:

```text
Maximum three calls per second
```

Requests-per-second limits require a rate-limiting algorithm such as a token bucket.

### Interview-ready answer

> I would use a semaphore with three permits to cap active calls. I would acquire with a timeout, release only after successful acquisition and still configure HTTP timeouts. A semaphore limits concurrency, not requests per second.

---

# Scenario 62: Dynamic Participants Across Multiple Phases

```java
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public final class Tournament {

    private record Player(
            String name,
            int rounds
    ) {
    }

    public static void main(String[] args) {
        List<Player> players = List.of(
                new Player("Alice", 3),
                new Player("Bob", 2),
                new Player("Carol", 3)
        );

        Phaser phaser = new Phaser(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        players.size()
                );

        for (Player player : players) {
            phaser.register();

            executor.submit(() -> {
                try {
                    for (int round = 1;
                         round <= player.rounds();
                         round++) {

                        playRound(player.name(), round);
                        phaser.arriveAndAwaitAdvance();
                    }
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }

        int maximumRounds =
                players.stream()
                        .mapToInt(Player::rounds)
                        .max()
                        .orElse(0);

        for (int round = 1;
             round <= maximumRounds;
             round++) {

            phaser.arriveAndAwaitAdvance();

            System.out.println(
                    "Round " + round + " completed"
            );
        }

        phaser.arriveAndDeregister();
        executor.shutdown();
    }

    private static void playRound(
            String player,
            int round
    ) {
        System.out.println(
                player + " completed round " + round
        );
    }
}
```

### Key points

- `Phaser` supports registration and deregistration.
- A participant that leaves must deregister.
- `CyclicBarrier` expects a fixed participant count.
- For interruptible waits or timeouts, use the appropriate `Phaser` waiting methods.

### Interview-ready answer

> I would use a `Phaser` because participants can join or deregister between phases. Each active participant arrives at the phase boundary, and participants leaving the workflow call `arriveAndDeregister()`.

---

# Scenario 63: Dependent Tasks with `CompletableFuture`

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class OrderPipeline {

    public static void main(String[] args) {
        ExecutorService executor =
                Executors.newFixedThreadPool(4);

        try {
            CompletableFuture<Void> pipeline =
                    CompletableFuture
                            .supplyAsync(
                                    () -> validateOrder("ORD-123"),
                                    executor
                            )
                            .thenCompose(valid ->
                                    chargePayment(
                                            valid,
                                            executor
                                    )
                            )
                            .thenAcceptAsync(
                                    OrderPipeline::sendConfirmation,
                                    executor
                            )
                            .orTimeout(
                                    10,
                                    TimeUnit.SECONDS
                            )
                            .whenComplete((ignored, failure) -> {
                                if (failure != null) {
                                    System.err.println(
                                            "Order failed: "
                                                    + failure
                                    );
                                }
                            });

            pipeline.join();
        } finally {
            executor.shutdown();
        }
    }

    private static boolean validateOrder(String orderId) {
        System.out.println("Validating " + orderId);
        return true;
    }

    private static CompletableFuture<String> chargePayment(
            boolean valid,
            ExecutorService executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (!valid) {
                throw new IllegalArgumentException(
                        "Invalid order"
                );
            }

            System.out.println("Charging payment");
            return "TXN-999";
        }, executor);
    }

    private static void sendConfirmation(
            String transactionId
    ) {
        System.out.println(
                "Confirmation for " + transactionId
        );
    }
}
```

### Key points

- `thenCompose()` flattens dependent asynchronous operations.
- `thenAccept()` consumes a result and returns no new value.
- Add timeout and exception handling.
- Payment processing must be idempotent because retries may occur.

### Interview-ready answer

> I would use `thenCompose()` because payment depends on successful validation and already returns a future. I would then use `thenAccept()` for confirmation, with explicit timeout, failure handling and idempotency around the payment step.

---

# Scenario 64: Starvation in a Lock-Heavy System

```java
import java.util.concurrent.locks.ReentrantLock;

public final class SharedCounter {

    private final ReentrantLock lock =
            new ReentrantLock(true);

    private int counter;

    public void increment() {
        lock.lock();

        try {
            counter++;
        } finally {
            lock.unlock();
        }
    }
}
```

A fair lock generally favors threads that have waited longer.

### Important nuance

A fair `ReentrantLock`:

- Reduces lock starvation risk
- Does not guarantee perfect FIFO execution
- Does not solve CPU scheduling starvation
- May reduce throughput
- Does not remove the need to shorten critical sections

### Better investigation

Before enabling fairness, check:

- Lock-hold duration
- Repeated lock reacquisition
- Slow work under the lock
- Whether one global lock can be partitioned
- Whether the shared state can be redesigned

### Interview-ready answer

> A fair `ReentrantLock` can reduce starvation among queued lock waiters, but it is not a strict FIFO scheduler and can reduce throughput. I would first inspect lock scope and contention, then use fairness when bounded waiting is an actual requirement.

---

# Scenario 65: Run Thousands of Tasks Without Creating One Thread per Task

`Executors.newFixedThreadPool()` uses an unbounded queue, so it does not provide complete overload protection.

A bounded executor is safer:

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class NotificationExecutor {

    public static void main(String[] args) {
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        10,
                        10,
                        0,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1_000),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );

        for (int id = 1; id <= 10_000; id++) {
            int notificationId = id;

            executor.execute(() ->
                    sendNotification(notificationId)
            );
        }

        executor.shutdown();
    }

    private static void sendNotification(int id) {
        System.out.println(
                "Sending notification " + id
        );
    }
}
```

### Why this is safer

- Threads are reused.
- Queue capacity is bounded.
- `CallerRunsPolicy` applies backpressure.
- The system cannot silently accumulate unlimited queued work.

Virtual threads may be appropriate for large numbers of blocking tasks, but they do not remove downstream connection, rate or memory limits.

### Interview-ready answer

> I would use a bounded executor rather than one thread per task. The pool limits active workers, the queue limits backlog and the rejection policy defines overload behavior. For blocking workloads, virtual threads may simplify concurrency, but downstream capacity still must be bounded.

---

# Scenario 66: Divide-and-Conquer with `ForkJoinPool`

```java
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public final class ForkJoinSum {

    private static final class SumTask
            extends RecursiveTask<Long> {

        private static final int THRESHOLD = 10_000;

        private final long[] values;
        private final int start;
        private final int end;

        private SumTask(
                long[] values,
                int start,
                int end
        ) {
            this.values = values;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;

            if (length <= THRESHOLD) {
                long total = 0;

                for (int index = start;
                     index < end;
                     index++) {

                    total += values[index];
                }

                return total;
            }

            int middle = start + length / 2;

            SumTask left =
                    new SumTask(
                            values,
                            start,
                            middle
                    );

            SumTask right =
                    new SumTask(
                            values,
                            middle,
                            end
                    );

            left.fork();
            long rightResult = right.compute();
            long leftResult = left.join();

            return leftResult + rightResult;
        }
    }

    public static void main(String[] args) {
        long[] data = new long[5_000_000];

        for (int index = 0;
             index < data.length;
             index++) {

            data[index] = index % 10;
        }

        ForkJoinPool pool = new ForkJoinPool();

        try {
            long total = pool.invoke(
                    new SumTask(
                            data,
                            0,
                            data.length
                    )
            );

            System.out.println(total);
        } finally {
            pool.shutdown();
        }
    }
}
```

### Key points

- Divide work until a suitable threshold.
- Compute one branch directly and fork the other.
- Combine associative partial results.
- Tune the threshold using benchmarks.
- Use a custom pool when workload isolation matters.

### Interview-ready answer

> I would model the operation as a `RecursiveTask`, split until chunks are large enough to justify sequential processing, fork one branch, compute the other and join the result. The threshold determines whether parallel overhead is worthwhile.

---

# Scenario 67: Process Results in Completion Order

```java
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class CompletionOrderDemo {

    private record FetchTask(
            String name,
            long delayMillis
    ) implements Callable<String> {

        @Override
        public String call()
                throws InterruptedException {

            Thread.sleep(delayMillis);
            return name + " OK";
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        ExecutorCompletionService<String> completion =
                new ExecutorCompletionService<>(executor);

        List<FetchTask> tasks = List.of(
                new FetchTask("pricing", 800),
                new FetchTask("shipping", 300),
                new FetchTask("tax", 600)
        );

        try {
            tasks.forEach(completion::submit);

            for (int index = 0;
                 index < tasks.size();
                 index++) {

                Future<String> future =
                        completion.take();

                try {
                    System.out.println(future.get());
                } catch (ExecutionException exception) {
                    System.err.println(
                            exception.getCause()
                    );
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
```

### Interview-ready answer

> `ExecutorCompletionService` separates submission order from completion order. I submit all calls and consume completed futures from its completion queue, allowing fast results to be processed without waiting for slower earlier submissions.

---

# Scenario 68: Asynchronous Calls with Independent Fallbacks

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AsyncFallbackDemo {

    private static CompletableFuture<String> callService(
            String name,
            long delay,
            boolean fail,
            ExecutorService executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CompletionException(exception);
            }

            if (fail) {
                throw new IllegalStateException(
                        name + " failed"
                );
            }

            return name + ":data";
        }, executor);
    }

    public static void main(String[] args) {
        ExecutorService executor =
                Executors.newFixedThreadPool(4);

        try {
            CompletableFuture<String> first =
                    callService(
                            "A",
                            300,
                            false,
                            executor
                    ).exceptionally(failure -> {
                        System.err.println(failure);
                        return "A:default";
                    });

            CompletableFuture<String> second =
                    callService(
                            "B",
                            600,
                            true,
                            executor
                    ).exceptionally(failure -> {
                        System.err.println(failure);
                        return "B:default";
                    });

            CompletableFuture<String> third =
                    callService(
                            "C",
                            200,
                            false,
                            executor
                    ).exceptionally(failure -> {
                        System.err.println(failure);
                        return "C:default";
                    });

            String result =
                    CompletableFuture
                            .allOf(
                                    first,
                                    second,
                                    third
                            )
                            .thenApply(ignored ->
                                    String.join(
                                            "|",
                                            first.join(),
                                            second.join(),
                                            third.join()
                                    )
                            )
                            .orTimeout(
                                    5,
                                    TimeUnit.SECONDS
                            )
                            .join();

            System.out.println(result);
        } finally {
            executor.shutdownNow();
        }
    }
}
```

The pipeline remains asynchronous internally, although `join()` at the application boundary waits for the final result.

### Interview-ready answer

> I would convert failures into branch-specific fallback values before aggregation. `allOf()` then waits for every branch, and joining the individual futures is safe once they have all completed. I would also add per-call timeouts and failure metrics.

---

# Scenario 69: Double-Buffer Handoff with `Exchanger`

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

public final class DoubleBufferDemo {

    public static void main(String[] args) {
        Exchanger<List<Integer>> exchanger =
                new Exchanger<>();

        Thread producer = new Thread(() -> {
            List<Integer> buffer =
                    new ArrayList<>(5);

            try {
                for (int round = 0;
                     round < 5;
                     round++) {

                    buffer.clear();

                    for (int index = 0;
                         index < 5;
                         index++) {

                        buffer.add(round * 10 + index);
                    }

                    buffer = exchanger.exchange(buffer);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            List<Integer> buffer =
                    new ArrayList<>(5);

            try {
                for (int round = 0;
                     round < 5;
                     round++) {

                    buffer = exchanger.exchange(buffer);

                    System.out.println(
                            "Consumed: " + buffer
                    );

                    buffer.clear();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
    }
}
```

### Key points

- `Exchanger` swaps object references between exactly two parties.
- The producer receives the consumer’s empty buffer.
- The consumer receives the producer’s full buffer.
- Data copying between buffers is avoided.
- Both parties synchronize at every exchange.
- Use timed exchange operations when one party may fail.

### Interview-ready answer

> I would use an `Exchanger<List<T>>` so the producer and consumer exchange buffer ownership. This avoids copying the elements and naturally synchronizes the two processing stages.

---

# Scenario 70: Graceful Shutdown with Poison Pills

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class PoisonPillPipeline {

    private record Job(
            String payload,
            boolean stop
    ) {
        private static Job work(String payload) {
            return new Job(payload, false);
        }

        private static Job poisonPill() {
            return new Job("", true);
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        int consumerCount = 3;

        BlockingQueue<Job> queue =
                new ArrayBlockingQueue<>(10);

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
                        Job job = queue.take();

                        if (job.stop()) {
                            return;
                        }

                        process(job.payload());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        try {
            for (int id = 1; id <= 20; id++) {
                queue.put(
                        Job.work("job-" + id)
                );
            }
        } finally {
            for (int index = 0;
                 index < consumerCount;
                 index++) {

                queue.put(Job.poisonPill());
            }
        }

        consumers.shutdown();

        if (!consumers.awaitTermination(
                5,
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

### Why one pill per consumer?

Each poison pill is consumed by only one worker. With three consumers, one pill would stop only one consumer and leave the other two waiting.

### Important considerations

- Use an explicit message type rather than a normal data value that could collide with real input.
- Insert poison pills after all ordinary work.
- If producers can fail, send shutdown messages from cleanup logic.
- For immediate cancellation, interruption may be more appropriate than draining the queue.
- A bounded queue provides backpressure.

### Interview-ready answer

> I would enqueue one poison pill per consumer after all normal work. Each consumer exits after receiving one. I would use an explicit message type rather than a magic string and combine the protocol with executor shutdown and a termination timeout.
