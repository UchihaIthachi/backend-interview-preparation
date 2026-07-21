# Scenario-based Questions

**Scenario/Question:** Your application has intermittent deadlocks in production - how will you detect and resolve?

## Scenario 51: Parallel stream corrupts output due to shared mutable state 
You convert a large list of items to a map using parallelStream(), but occasionally see missing or duplicated keys because you appended to a shared HashMap inside forEach. 
Answer: 
In parallel streams, avoid mutating non-thread-safe shared state. Use built-in concurrent collectors like toConcurrentMap (or a proper Collector) so accumulation is thread-safe and associative. 

```java

import java.util.*;
    import java.util.concurrent.ConcurrentMap;
    import java.util.stream.Collectors;
    import java.util.stream.IntStream;
    public class ParallelStreamSafeCollect {
    public static void main(String[] args) {
    List<Integer> ids = IntStream.rangeClosed(1, 10000).boxed().toList();
    // WRONG: shared HashMap with forEach in parallel // Map<Integer, String> bad = new HashMap<>();
    // ids.parallelStream().forEach(i -> bad.put(i, "V" + i));
    // data races! // RIGHT: use concurrent collector ConcurrentMap<Integer, String> good = ids.parallelStream() .collect(Collectors.toConcurrentMap(i -> i, i -> "V" + i));
    System.out.println("size=" + good.size());
    // 10000 }
}

```

Reasoning to interviewer: 
• Parallel pipelines require associative, thread-safe accumulation; mutating a shared HashMap breaks that contract. • toConcurrentMap uses a concurrent container and merges safely across threads. 
• If you must use a custom collector, ensure it’s CONCURRENT and UNORDERED when appropriate. 
Future scope: 
• For ordered results, collect then sort, or use forEachOrdered (with potential performance cost). • If the operation is I/O-bound or includes blocking, prefer CompletableFuture or virtual threads over parallel streams. • Benchmark: for small datasets, sequential streams can outperform due to parallel overhead. 

## Scenario 52: Threads must finish initialization before main continues 
You spin up 5 worker threads to preload caches and initialize connections. The main thread should only start serving requests after all workers report “ready”. 
Answer: 
I’ll use a CountDownLatch initialized with the worker count. Each worker countDown()s when done, and the main thread calls await(). This makes startup deterministic and avoids polling or sleeps. 

```java

import java.util.concurrent.CountDownLatch;
    class InitWorker implements Runnable {
    private final CountDownLatch latch;
    private final String name;
    InitWorker(String name, CountDownLatch latch) {
    this.name = name;
    this.latch = latch;
    }
@Override public void run() {
    try {
    System.out.println(name + " initializing...");
    Thread.sleep(300 + (int)(Math.random()*200));
    // simulate work System.out.println(name + " ready");
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
finally {
    latch.countDown();
    }
}
}
public class StartupLatchDemo {
    public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(5);
    for (int i=1;i<=5;i++) new Thread(new InitWorker("Worker-"+i, latch)).start();
    latch.await();
    // block until all ready System.out.println("All workers ready, starting server...");
    }
}

```

Reasoning to interviewer: 
• CountDownLatch is a one-shot gate: once count hits zero, all await()ers proceed. • Better than Thread.sleep() because it adapts to actual work time. • Always decrement in finally to avoid hanging the system if a worker crashes. 
Future scope: 
• For repeated barriers, use CyclicBarrier or Phaser. • Add timeout to await() to avoid indefinite block on stuck workers. • Wrap init logic in ExecutorService with invokeAll if you need to propagate results or exceptions. 

## Scenario 53: Need both parallel and sequential phases in a pipeline 
You have 10 ETL tasks. They can all extract in parallel, but then must wait before a single sequential consolidation runs. After that, all can transform in parallel. 
Answer: 
I’ll use CyclicBarrier for parallel phases and coordinate the sequential phase via a barrier action. This ensures everyone stops at the end of extract, runs consolidation once, then continues. 

```java

import java.util.concurrent.*;
    class ETLWorker implements Runnable {
    private final CyclicBarrier barrier;
    private final int id;
    ETLWorker(int id, CyclicBarrier barrier) {
    this.id=id;
    this.barrier=barrier;
    }
@Override public void run() {
    try {
    extract();
    barrier.await();
    // sync after extract transform();
    barrier.await();
    // sync after transform load();
    }
catch (Exception e) {
    Thread.currentThread().interrupt();
    }
}
void extract(){
    System.out.println("T"+id+" extracting");
    }
void transform(){
    System.out.println("T"+id+" transforming");
    }
void load(){
    System.out.println("T"+id+" loading");
    }
}
public class ETLPipeline {
    public static void main(String[] args) {
    int workers=4;
    CyclicBarrier barrier = new CyclicBarrier(workers, () -> System.out.println("-- barrier reached, consolidation step --"));
    for (int i=1;i<=workers;i++) new Thread(new ETLWorker(i, barrier)).start();
    }
}

```

Reasoning to interviewer: 
• CyclicBarrier fits multi-phase pipelines; its action runs once per phase. • Each thread does work, then blocks until all others arrive. • Compared to CountDownLatch, it resets automatically and is reusable. 
Future scope: 
• For dynamic number of workers, switch to Phaser. • Add timeouts in await() to prevent one stuck worker blocking all. • Combine with thread pools for structured task management instead of raw threads. 

## Scenario 54: Cancel long-running tasks if they exceed a deadline 
You run a report generator. Some reports take too long and must be aborted after 5 seconds. 
Answer: 
I’ll submit each task to ExecutorService, hold its Future, and call get(timeout, unit). If it times out, I’ll cancel(true) to interrupt the worker. 

```java

import java.util.concurrent.*;
    class Report implements Callable<String> {
    private final int id;
    Report(int id){this.id=id;}
@Override public String call() throws Exception {
    Thread.sleep(7000);
    // simulate slow return "Report " + id;
    }
}
public class TimeoutCancelDemo {
    public static void main(String[] args) {
    ExecutorService pool = Executors.newCachedThreadPool();
    Future<String> f = pool.submit(new Report(42));
    try {
    String res = f.get(5, TimeUnit.SECONDS);
    System.out.println("Got: " + res);
    }
catch (TimeoutException e) {
    System.out.println("Report took too long, cancelling...");
    f.cancel(true);
    }
catch (Exception e) {
    e.printStackTrace();
    }
finally {
    pool.shutdown();
    }
}
}

```

Reasoning to interviewer: 
• Future.get(timeout) throws TimeoutException—perfect for deadlines. • cancel(true) sets interrupt flag; the task must cooperate (e.g., by checking Thread.interrupted() or catching InterruptedException). • Timeout protects system throughput and avoids unbounded waits. 
Future scope: 
• Use CompletableFuture.orTimeout() in Java 9+ for cleaner chaining. • Wrap blocking I/O in libraries that respect interrupts. • Log timeouts for metrics and possibly retry with backoff. 

## Scenario 55: Parallel stream runs slower than sequential on small data 
You squared a list of 100 ints with parallelStream(), but it runs slower than a simple loop. 
Answer: 
Parallel streams split work into subtasks via ForkJoinPool. Overhead of splitting and coordination can outweigh benefits on small datasets. For small lists, sequential is better; parallel helps only when: 
1. 
data is large, 
2. 
tasks are CPU-intensive, 
3. 
no shared mutable state. 

```java

import java.util.*;
    import java.util.stream.*;
    public class ParallelPerfDemo {
    public static void main(String[] args) {
    List<Integer> nums = IntStream.rangeClosed(1, 100).boxed().toList();
    long t1 = System.nanoTime();
    nums.stream().map(n->n*n).toList();
    long t2 = System.nanoTime();
    nums.parallelStream().map(n->n*n).toList();
    long t3 = System.nanoTime();
    System.out.printf("Sequential=%.2fms, Parallel=%.2fms%n", (t2-t1)/1e6, (t3-t2)/1e6);
    }
}

```

Reasoning to interviewer: 
• ForkJoinPool overhead ≈ splitting + task scheduling + join. • With only 100 elements, cost > work. With millions, parallel wins. • Don’t blindly switch to parallel; measure per workload. 
Future scope: 
• Use thresholds (size, CPU intensity) to pick sequential vs parallel. • For I/O-bound work, prefer CompletableFuture or virtual threads. • Profile using JMH to quantify actual gains. 

## Scenario 56: Multiple readers, single writer with high read frequency 
You’re building a stock price dashboard where dozens of threads read the latest price frequently, while only one thread updates it occasionally. You want high performance for reads without blocking each other. 
Answer: 
I’d use ReadWriteLock or better, StampedLock. With ReadWriteLock, multiple readers can proceed in parallel but writers are exclusive. With StampedLock, I can even attempt optimistic reads, which don’t block unless a write happens. 

```java

import java.util.concurrent.locks.StampedLock;
    class PriceBoard {
    private final StampedLock lock = new StampedLock();
    private double price = 100.0;
    public void update(double newPrice) {
    long stamp = lock.writeLock();
    try {
    price = newPrice;
    }
finally {
    lock.unlockWrite(stamp);
    }
}
public double read() {
    long stamp = lock.tryOptimisticRead();
    double val = price;
    if (!lock.validate(stamp)) {
    // fallback if a write happened stamp = lock.readLock();
    try {
    val = price;
    }
finally {
    lock.unlockRead(stamp);
    }
}
return val;
    }
}

```

Reasoning to interviewer: 
• Reads dominate, so optimistic reads avoid unnecessary locking. • validate() ensures consistency if a write interfered. • Writers still get exclusive access when updating. 
Future scope: 
• If updates increase, switch back to ReentrantReadWriteLock for fairness. • Add versioning to detect stale reads in distributed scenarios. • Integrate with caches (e.g., Guava/Redis) for scale-out read-heavy workloads. 

## Scenario 57: Coordinating different tasks that must run in order 
You have 3 steps: load data, process it, and finally save results. Each step must wait for the previous one to complete across multiple threads. 
Answer: 
I’d use a CountDownLatch chain or CompletableFuture chaining. With latch chaining, each stage waits for the previous latch to reach zero before proceeding. 

```java

import java.util.concurrent.CountDownLatch;
    public class OrderedTasksDemo {
    public static void main(String[] args) throws InterruptedException {
    CountDownLatch loadDone = new CountDownLatch(1);
    CountDownLatch processDone = new CountDownLatch(1);
    Thread loader = new Thread(() -> {
    System.out.println("Loading data...");
    loadDone.countDown();
    });
    Thread processor = new Thread(() -> {
    try {
    loadDone.await();
    System.out.println("Processing data...");
    processDone.countDown();
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
});
    Thread saver = new Thread(() -> {
    try {
    processDone.await();
    System.out.println("Saving results...");
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
});
    loader.start();
    processor.start();
    saver.start();
    }
}

```

Reasoning to interviewer: 
• Each stage gates on the previous latch, ensuring order. • This avoids busy waiting or sleeps. • For complex workflows, CompletableFuture.thenRun() or orchestration frameworks may be better. 
Future scope: 
• For DAG-style dependencies, use CompletableFuture chaining. • Add error handling and early termination if one stage fails. • Move to async/reactive frameworks for large dependency graphs. 

## Scenario 58: Detect and recover from livelock 
Two worker threads politely keep yielding control when they detect contention, but they never make progress (classic livelock). 
Answer: 
This is livelock: threads are active but not progressing. To fix, add a random back-off strategy so they don’t both retry at the same time. 

```java

class LivelockFixed {
    private volatile boolean resourceInUse = false;
    public void useResource(String worker) {
    while (true) {
    if (!resourceInUse) {
    resourceInUse = true;
    System.out.println(worker + " acquired resource");
    try {
    Thread.sleep(200);
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
resourceInUse = false;
    System.out.println(worker + " released resource");
    break;
    }
else {
    try {
    // backoff prevents livelock Thread.sleep((int)(Math.random()*100));
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
}
}
}
}

```

Reasoning to interviewer: 
• Livelock isn’t a dead halt (like deadlock), but no useful work gets done. • Random or exponential backoff breaks the symmetry. • Real systems (e.g., networking, DB retries) use this to avoid livelock storms. 
Future scope: 
• Use Lock.tryLock(timeout) instead of manual busy loops. • Add metrics for retry count to detect potential livelocks. • Apply fairness policies if many threads contend repeatedly. 

## Scenario 59: Cancel tasks cleanly in ExecutorService 
You schedule tasks that may hang (e.g., external API calls). You want to cancel them if a signal is received (like user logout). 
Answer: 
I’d hold on to Future<?> objects returned by submit() and call cancel(true). Each task should check Thread.interrupted() periodically to exit gracefully. 

```java

import java.util.concurrent.*;
    class CancellableTask implements Runnable {
    public void run() {
    try {
    for (int i=0;i<10;i++) {
    if (Thread.interrupted()) {
    System.out.println("Task interrupted, cleaning up...");
    return;
    }
Thread.sleep(500);
    System.out.println("Step " + i);
    }
}
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    System.out.println("Interrupted during sleep, exiting...");
    }
}
}
public class CancelTasksDemo {
    public static void main(String[] args) throws InterruptedException {
    ExecutorService pool = Executors.newSingleThreadExecutor();
    Future<?> f = pool.submit(new CancellableTask());
    Thread.sleep(1200);
    f.cancel(true);
    // cancel after 1.2s pool.shutdown();
    }
}

```

Reasoning to interviewer: 
• cancel(true) sets interrupt flag, which must be checked in the task. • Cooperative cancellation avoids resource leaks and half-updates. • Threads ignoring interrupts will appear “uncancellable”. 
Future scope: 
• Use CompletableFuture.cancel() for async pipelines. • Add cleanup hooks (closing sockets, rolling back DB). • Centralize cancellation via ExecutorService.shutdownNow(). 

## Scenario 60: Parallel aggregation of large datasets 
You need to compute total sales from a dataset of millions of records. Single-threaded loops are too slow. 
Answer: 
I’d use Java 8 parallel streams or ForkJoinPool to split the work across cores. With parallel streams, the workload is automatically partitioned and reduced. 

```java

import java.util.*;
    import java.util.stream.*;
    public class SalesAggregator {
    public static void main(String[] args) {
    List<Integer> sales = IntStream.range(1, 1_000_000).boxed().toList();
    long total = sales.parallelStream() .mapToLong(Integer::longValue) .sum();
    System.out.println("Total sales = " + total);
    }
}

```

Reasoning to interviewer: 
• Parallel streams leverage ForkJoinPool with work-stealing. • Operations must be associative and stateless for correctness. • Aggregation scales well when dataset is large and CPU-bound. 
Future scope: 
• Use Collectors.groupingByConcurrent for grouped aggregations. • For I/O-heavy tasks, switch to async APIs instead of parallel streams. • Tune ForkJoinPool parallelism if default doesn’t suit workload. 

## Scenario 61: Throttling API calls with limited concurrency 
You’re writing a service that calls an external API, but the API provider allows only 3 concurrent calls at a time. Extra calls should wait. 
Answer: 
I’d use a Semaphore with 3 permits. Each API call thread acquires a permit before sending the request and releases it after completion. 

```java

import java.util.concurrent.Semaphore;
    class ApiClient {
    private final Semaphore semaphore = new Semaphore(3);
    // max 3 concurrent calls public void callApi(String name) {
    try {
    semaphore.acquire();
    System.out.println(name + " started API call");
    Thread.sleep(1000);
    // simulate call System.out.println(name + " finished API call");
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
finally {
    semaphore.release();
    }
}
}
public class ApiThrottleDemo {
    public static void main(String[] args) {
    ApiClient client = new ApiClient();
    for (int i = 1;
    i <= 6;
    i++) {
    String name = "Task-" + i;
    new Thread(() -> client.callApi(name)).start();
    }
}
}

```

Reasoning to interviewer: 
• Semaphore models the number of available “slots” for resource access. • Threads block automatically if permits are unavailable. • This prevents overload without hand-coded queue management. 
Future scope: 
• Use tryAcquire(timeout) for fail-fast instead of indefinite waiting. • Track rejected/blocked calls for monitoring. • Integrate with rate-limiters (like Guava’s RateLimiter) for QPS control. 

## Scenario 62: Coordinating variable participants across phases 
You’re simulating a game tournament where players can join or leave between rounds. At the end of each round, all current players must sync before moving to the next. 
Answer: 
I’d use Phaser, which supports dynamic registration/deregistration unlike CyclicBarrier. Each player registers, plays, and waits at arriveAndAwaitAdvance(). 

```java

import java.util.concurrent.Phaser;
    class Player implements Runnable {
    private final Phaser phaser;
    private final int rounds;
    private final String name;
    Player(String name, Phaser phaser, int rounds) {
    this.name = name;
    this.phaser = phaser;
    this.rounds = rounds;
    phaser.register();
    }
@Override public void run() {
    try {
    for (int i = 1;
    i <= rounds;
    i++) {
    System.out.println(name + " finished round " + i);
    phaser.arriveAndAwaitAdvance();
    }
}
finally {
    phaser.arriveAndDeregister();
    }
}
}
public class PhaserDemo {
    public static void main(String[] args) {
    Phaser phaser = new Phaser(1);
    // register main new Thread(new Player("Alice", phaser, 3)).start();
    new Thread(new Player("Bob", phaser, 2)).start();
    for (int i = 1;
    i <= 3;
    i++) {
    phaser.arriveAndAwaitAdvance();
    System.out.println("Round " + i + " completed.");
    }
phaser.arriveAndDeregister();
    }

```

} 
Reasoning to interviewer: 
• Phaser is more flexible than CyclicBarrier for dynamic participation. • Deregistration prevents waiting on threads that left early. • Useful for simulations, iterative algorithms, or variable teams. 
Future scope: 
• Override onAdvance to add custom phase logic. • Add timeouts to detect stuck participants. • For hierarchical workflows, use multiple phasers. 

## Scenario 63: Task dependencies with CompletableFuture 
In an order processing system, you must: 
1. 
Validate the order. 
2. 
Charge payment only if validation succeeds. 
3. 
Send confirmation email after payment. 
Answer: 
I’d chain CompletableFutures using thenCompose() for dependent tasks and thenRun() for actions without results. 

```java

import java.util.concurrent.*;
    public class OrderPipelineDemo {
    public static void main(String[] args) {
    ExecutorService executor = Executors.newFixedThreadPool(3);
    CompletableFuture<Void> pipeline = CompletableFuture.supplyAsync(() -> validateOrder("ORD123"), executor) .thenCompose(valid -> chargePayment(valid, executor)) .thenAccept(paymentId -> sendConfirmation(paymentId));
    pipeline.join();
    executor.shutdown();
    }
static boolean validateOrder(String id) {
    System.out.println("Validating " + id);
    return true;
    }
static CompletableFuture<String> chargePayment(boolean valid, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
    if (!valid) throw new RuntimeException("Invalid order");
    System.out.println("Charging payment...");
    return "TXN-999";
    }, executor);
    }
static void sendConfirmation(String txnId) {
    System.out.println("Sending confirmation for " + txnId);
    }
}

```

Reasoning to interviewer: 
• thenCompose flattens dependent async tasks. • thenAccept consumes a result without producing a new one. • Clear pipeline, avoids blocking, keeps error handling async. 
Future scope: 
• Add .exceptionally() or .handle() for fallback logic. • Use allOf() when processing multiple parallel validations. • Combine with timeouts for resilience. 

## Scenario 64: Handling starvation in lock-heavy system 
A high-priority thread keeps grabbing a lock, while other threads rarely progress. 
Answer: 
This is starvation. I’d use a fair ReentrantLock(true) to ensure FIFO lock acquisition. 

```java

import java.util.concurrent.locks.ReentrantLock;
    class SharedCounter {
    private final ReentrantLock lock = new ReentrantLock(true);
    // fair lock private int counter = 0;
    public void increment(String name) {
    lock.lock();
    try {
    counter++;
    System.out.println(name + " incremented to " + counter);
    }
finally {
    lock.unlock();
    }
}
}
public class FairLockDemo {
    public static void main(String[] args) {
    SharedCounter counter = new SharedCounter();
    for (int i = 1;
    i <= 5;
    i++) {
    String t = "T" + i;
    new Thread(() -> counter.increment(t)).start();
    }
}
}

```

Reasoning to interviewer: 
• Fair locks grant access roughly in order of request, avoiding starvation. • Comes with throughput tradeoff compared to non-fair locks. • Starvation often occurs when some threads hog resources repeatedly. 
Future scope: 
• Use lock sharding or finer-grained locks to reduce contention. • For read-heavy workloads, use ReadWriteLock. • Monitor lock wait times in production to detect hidden starvation. 

## Scenario 65: Run thousands of tasks efficiently 
You have 10,000 short tasks (like sending notifications). Creating a thread per task is too costly. 
Answer: 
I’d use a fixed-size thread pool (ExecutorService) to reuse threads and manage queued tasks. 

```java

import java.util.concurrent.*;
    class NotifyTask implements Runnable {
    private final int id;
    NotifyTask(int id) {
    this.id = id;
    }
public void run() {
    System.out.println("Sending notification " + id + " by " + Thread.currentThread().getName());
    }
}
public class ThreadPoolDemo {
    public static void main(String[] args) {
    ExecutorService pool = Executors.newFixedThreadPool(10);
    for (int i = 1;
    i <= 20;
    i++) {
    pool.submit(new NotifyTask(i));
    }
pool.shutdown();
    }
}

```

Reasoning to interviewer: 
• Thread pools amortize thread creation cost. • A bounded pool prevents resource exhaustion. • Tasks are queued when all threads are busy. 
Future scope: 
• Use newCachedThreadPool for bursty workloads. • For scheduled jobs, use ScheduledExecutorService. • Consider virtual threads (Java 21+) for massive concurrency. 

## Scenario 66: Divide-and-conquer over a big array (ForkJoinPool) 
You need to sum a massive array (or apply a CPU-heavy transform) faster than a single thread can manage. The work splits naturally. 
Answer: 
I’ll use ForkJoinPool with a RecursiveTask<Long>. Each task splits the array until chunks are small, computes locally, and combines results. This keeps cores busy with work-stealing and minimizes coordination overhead. 

```java

import java.util.concurrent.*;
    class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 10_000;
    private final long[] arr;
    private final int lo, hi;
    SumTask(long[] arr, int lo, int hi) {
    this.arr = arr;
    this.lo = lo;
    this.hi = hi;
    }
@Override protected Long compute() {
    int len = hi - lo;
    if (len <= THRESHOLD) {
    long sum = 0;
    for (int i = lo;
    i < hi;
    i++) sum += arr[i];
    return sum;
    }
int mid = lo + len / 2;
    SumTask left = new SumTask(arr, lo, mid);
    SumTask right = new SumTask(arr, mid, hi);
    left.fork();
    long r = right.compute();
    // compute one half directly long l = left.join();
    // then join the other half return l + r;
    }
}
public class ForkJoinSumDemo {
    public static void main(String[] args) {
    long[] data = new long[5_000_000];
    for (int i = 0;
    i < data.length;
    i++) data[i] = i % 10;
    ForkJoinPool pool = new ForkJoinPool();
    // or custom parallelism long total = pool.invoke(new SumTask(data, 0, data.length));
    System.out.println("Total = " + total);

```

pool.shutdown(); } } 
Reasoning to interviewer: 
• ForkJoinPool uses work-stealing, which keeps threads busy and reduces contention. • Splitting until a threshold balances parallel overhead vs. work size. • Calling compute() on one branch and fork()/join() on the other improves locality. 
Future scope: 
• Tune THRESHOLD with JMH. • Use RecursiveAction for in-place transforms. • Provide a custom pool when the common pool is busy with other tasks. 

## Scenario 67: Process results as soon as they finish (ExecutorCompletionService) 
You launch multiple downstream calls (pricing, shipping, tax). Each completes at different times; you want to stream results immediately instead of waiting for all. 
Answer: 
I’ll wrap a fixed thread pool with ExecutorCompletionService. Submit N callables, then pull take() in a loop to get futures in completion order. Handle exceptions per-future to avoid stalling the loop. 

```java

import java.util.concurrent.*;
    class FetchTask implements Callable<String> {
    private final String name;
    private final long delayMs;
    FetchTask(String name, long delayMs) {
    this.name = name;
    this.delayMs = delayMs;
    }
@Override public String call() throws Exception {
    Thread.sleep(delayMs);
    if (Math.random() < 0.2) throw new RuntimeException(name + " failed");
    return name + " OK";
    }
}
public class CompletionServiceDemo {
    public static void main(String[] args) throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(3);
    ExecutorCompletionService<String> ecs = new ExecutorCompletionService<>(pool);
    ecs.submit(new FetchTask("pricing", 800));
    ecs.submit(new FetchTask("shipping", 300));
    ecs.submit(new FetchTask("tax", 600));
    for (int i = 0;
    i < 3;
    i++) {
    Future<String> f = ecs.take();
    try {
    System.out.println("-> " + f.get());
    }
catch (ExecutionException e) {
    System.out.println("-> error: " + e.getCause().getMessage());
    }
}
pool.shutdown();
    }

```

} 
Reasoning to interviewer: 
• Completion queue decouples submission order from completion order—great for responsiveness. • Per-future try/catch prevents one failure from killing the whole loop. • A bounded pool keeps concurrency under control. 
Future scope: 
• Use poll(timeout) to surface stragglers. • Attach correlation IDs for partial rendering. • Combine with a circuit breaker when failures spike. 

## Scenario 68: Robust async pipeline with fallbacks (CompletableFuture) 
You call three services; any one might fail. You want defaults for failures, still aggregate everything, and log what broke—without blocking. 
Answer: 
I’ll compose with CompletableFuture, apply exceptionally/handle on each branch to convert failures into safe defaults, then allOf and aggregate. No get() in the pipeline—only at the edges. 

```java

import java.util.concurrent.*;
    public class CFErrorHandlingDemo {
    private static final ExecutorService io = Executors.newFixedThreadPool(4);
    static CompletableFuture<String> svc(String name, long ms, boolean fail) {
    return CompletableFuture.supplyAsync(() -> {
    try {
    Thread.sleep(ms);
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
if (fail) throw new RuntimeException(name + " boom");
    return name + ":data";
    }, io);
    }
public static void main(String[] args) {
    CompletableFuture<String> a = svc("A", 300, false) .exceptionally(ex -> {
    System.out.println("A failed: " + ex.getMessage());
    return "A:default";
    });
    CompletableFuture<String> b = svc("B", 600, true) .handle((val, ex) -> ex == null ? val : "B:default");
    CompletableFuture<String> c = svc("C", 200, false) .exceptionally(ex -> "C:default");
    String result = CompletableFuture.allOf(a, b, c) .thenApply(v -> String.join("|", a.join(), b.join(), c.join())) .join();
    System.out.println("Aggregated -> " + result);
    io.shutdown();
    }
}

```

Reasoning to interviewer: 
• Handle errors at the leaf futures so the aggregator doesn’t explode. • exceptionally maps failure → fallback; handle inspects both success/failure. • allOf waits for completion; join() is safe afterward and avoids checked exceptions noise. 
Future scope: 
• Add per-branch orTimeout and retries with backoff. • Collect error metrics to tune fallbacks. • Use anyOf to race mirrors for latency-sensitive branches. 

## Scenario 69: Double-buffer handoff between two threads (Exchanger) 
One thread fills a buffer (producer), the other drains it (consumer). You want zero copying and to swap buffers each cycle. 
Answer: 
I’ll use Exchanger<T> to exchange buffer references. Each thread calls exchange(buffer): the producer hands off a full buffer and receives an empty one; the consumer does the opposite. 

```java

import java.util.ArrayList;
    import java.util.List;
    import java.util.concurrent.Exchanger;
    public class ExchangerDemo {
    public static void main(String[] args) {
    Exchanger<List<Integer>> ex = new Exchanger<>();
    List<Integer> empty = new ArrayList<>();
    List<Integer> full = new ArrayList<>();
    Thread producer = new Thread(() -> {
    List<Integer> buf = empty;
    try {
    for (int round = 0;
    round < 5;
    round++) {
    buf.clear();
    for (int i = 0;
    i < 5;
    i++) buf.add(round * 10 + i);
    buf = ex.exchange(buf);
    // hand off full;
    get empty back }
}
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
}, "producer");
    Thread consumer = new Thread(() -> {
    List<Integer> buf = full;
    try {
    for (int round = 0;
    round < 5;
    round++) {
    buf = ex.exchange(buf);
    // receive full;
    return empty System.out.println("Consumed: " + buf);
    buf.clear();
    // now empty for next round }
}
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
}, "consumer");
    producer.start();
    consumer.start();
    }
}

```

Reasoning to interviewer: 
• Exchanger swaps references atomically—no copying, minimal GC. • Ideal for two-party pipelines with batch boundaries. • Both sides synchronize naturally at each exchange. 
Future scope: 
• Add timeouts to exchange() for robustness. • For more than two parties, use queues/barriers instead. • Use object pools for reusable buffers and fewer allocations. 

## Scenario 70: Graceful shutdown of multi-consumer pipeline (BlockingQueue + poison pill) 
You have a bounded queue with many consumers. When the producer finishes, all consumers should exit cleanly—no hanging threads. 
Answer: 
I’ll insert one poison pill per consumer after production ends. Each consumer exits when it dequeues the pill. This avoids races around “is-done” flags. 

```java

import java.util.concurrent.*;
    import java.util.*;
    public class PoisonPillDemo {
    private static final String PILL = "__STOP__";
    public static void main(String[] args) throws InterruptedException {
    int consumers = 3;
    BlockingQueue<String> q = new ArrayBlockingQueue<>(10);
    ExecutorService pool = Executors.newFixedThreadPool(consumers);
    // Consumers for (int i = 0;
    i < consumers;
    i++) {
    pool.submit(() -> {
    try {
    for (;;) {
    String msg = q.take();
    if (PILL.equals(msg)) break;
    // process System.out.println(Thread.currentThread().getName() + " -> " + msg);
    }
}
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
});
    }
// Producer for (int i = 1;
    i <= 20;
    i++) q.put("job-" + i);
    // Stop signal: one pill per consumer for (int i = 0;
    i < consumers;
    i++) q.put(PILL);
    pool.shutdown();

```

pool.awaitTermination(5, TimeUnit.SECONDS); System.out.println("Pipeline stopped cleanly"); } } 
Reasoning to interviewer: 
• A bounded BlockingQueue gives built-in backpressure. • One poison pill per consumer guarantees all workers see a stop signal. • No fragile shared flags or timing assumptions. 
Future scope: 
• Replace pills with ExecutorService shutdown + queue drain if tasks are cancellable. • Add retries/dead-letter queue for failures. • Track queue depth for autoscaling consumers.
