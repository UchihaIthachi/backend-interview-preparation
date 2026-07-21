# Scenario-based Questions

**Scenario/Question:** How will you implement producer-consumer pattern?

## Scenario 31: Mixed-duration jobs with limited workers 
A video platform needs to generate thumbnails, transcode clips, and extract subtitles. Each job takes a different amount of time. You’ve got only a few worker threads, and you want those threads to stay busy—no one should sit idle just because a long job is blocking a result. 
Answer: 
I’ll run everything on a bounded ExecutorService so we cap concurrency and reuse threads. To avoid “head-of-line blocking” when collecting results, I’ll consume completions as they arrive (via ExecutorCompletionService) instead of calling get() in submission order. This keeps workers busy and makes the system responsive to short jobs finishing early. 

```java

import java.util.*;
    import java.util.concurrent.*;
    class VariableDurationTask implements Callable<String> {
    private final String name;
    private final long ms;
    // variable duration VariableDurationTask(String name, long ms) {
    this.name = name;
    this.ms = ms;
    }
@Override public String call() throws Exception {
    Thread.sleep(ms);
    // simulate work return name + " completed in " + ms + "ms";
    }
}
public class VariableTaskThreadPool {
    public static void main(String[] args) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(3);
    // limited workers ExecutorCompletionService<String> ecs = new ExecutorCompletionService<>(pool);
    List<Callable<String>> jobs = List.of( new VariableDurationTask("thumb-A", 300), new VariableDurationTask("transcode-X", 1500), new VariableDurationTask("subs-M", 700), new VariableDurationTask("thumb-B", 200), new VariableDurationTask("transcode-Y", 1200) );

```

jobs.forEach(ecs::submit); for (int i = 0; i < jobs.size(); i++) { System.out.println(ecs.take().get()); // process as each finishes } pool.shutdown(); } } 
Reasoning to interviewer: 
• Fixed pool avoids spawning one thread per task and keeps CPU/memory predictable. • Completion-first collection prevents a slow task from delaying all subsequent results. • I keep tasks independent and short enough that thread reuse is effective; for CPU-bound work, size the pool ≈ number of cores. 
Future scope: 
• Track queue length and latency; apply backpressure (bounded queue + CallerRunsPolicy). • For CPU-heavy divide-and-conquer, consider ForkJoinPool/parallel streams. • For I/O-heavy work, consider virtual threads to simplify blocking without huge pools. 

## Scenario 32: Cap concurrent client connections 
Your TCP server can handle only 3 simultaneous connections to an expensive backend. Any extra clients should wait until a slot frees up. 
Answer: 
Use a Semaphore with 3 permits. Each handler acquires a permit before beginning and releases it on completion. Threads block (or time out) when no permits are available, which naturally limits concurrency. 

```java

import java.util.concurrent.Semaphore;
    class Server {
    private final Semaphore permits = new Semaphore(3, true);
    // fair to reduce starvation public void handleClient(String clientName) {
    boolean acquired = false;
    try {
    acquired = permits.tryAcquire();
    // or tryAcquire(timeout, unit) if (!acquired) {
    System.out.println(clientName + " waiting for a free slot");
    permits.acquire();
    // block until available }
System.out.println(clientName + " connected");
    Thread.sleep(1000);
    // simulate work }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
finally {
    if (acquired || permits.availablePermits() < 3) {
    System.out.println(clientName + " disconnected");
    permits.release();
    }
}
}
}
public class SemaphoreServerDemo {
    public static void main(String[] args) {
    Server server = new Server();
    for (int i = 1;
    i <= 6;
    i++) {
    final String name = "Client-" + i;
    new Thread(() -> server.handleClient(name)).start();
    }

```

} } 
Reasoning to interviewer: 
• A counting semaphore models a pool of scarce resources (connections). • Fair mode approximates FIFO admission to avoid starvation under load. • tryAcquire(timeout) gives clients a deadline, useful for fail-fast behavior. 
Future scope: 
• Track active sessions; refuse low-priority clients when load is high. • Replace sleep with real I/O that supports timeouts/cancellation. • Combine with a connection pool and per-tenant rate limits. 

## Scenario 33: Start many tasks, handle results the moment they’re ready 
You launch multiple enrichment calls for a search results page. Some finish fast, others slow. You want to render partial sections as soon as data arrives. 
Answer: 
Pair your executor with an ExecutorCompletionService. Submit all tasks, then pull completed Futures using take()/poll(). This yields results in completion order, not submission order. 

```java

import java.util.concurrent.*;
    class SimpleTask implements Callable<String> {
    private final String name;
    private final long ms;
    SimpleTask(String name, long ms) {
    this.name = name;
    this.ms = ms;
    }
@Override public String call() throws Exception {
    Thread.sleep(ms);
    return name + " finished";
    }
}
public class CompletionServiceDemo {
    public static void main(String[] args) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(3);
    ExecutorCompletionService<String> ecs = new ExecutorCompletionService<>(pool);
    ecs.submit(new SimpleTask("Task-1", 900));
    ecs.submit(new SimpleTask("Task-2", 300));
    ecs.submit(new SimpleTask("Task-3", 600));
    for (int i = 0;
    i < 3;
    i++) {
    Future<String> done = ecs.take();
    // next finished System.out.println(done.get());
    // process immediately }
pool.shutdown();
    }
}

```

Reasoning to interviewer: 
• Completion service decouples execution from completion order, improving perceived latency. 
• It’s still backed by your chosen pool size, so concurrency stays bounded. • Add per-task error handling (get with try/catch) to keep the loop robust when a task fails. 
Future scope: 
• Use poll(timeout) to avoid indefinite blocking and surface slow tasks. • Mix with CompletableFuture to compose results and add fallbacks. • Emit metrics (task duration, queue wait) to spot hotspots and tune pool size. 

## Scenario 34: Producer hands off data; consumer waits until it’s ready 
Imagine a metrics pipeline: a sensor thread produces readings; a reporter thread publishes them. If no reading exists yet, the reporter must wait; once produced, it should wake up and consume. 
Answer: 
I’ll coordinate the two threads with a shared monitor using wait()/notifyAll(). The producer sets data and signals; the consumer waits in a loop until a flag says data is available. I use a while guard (not if) to handle spurious wakeups and preserve correctness under bursts. 

```java

class SharedBuffer {
    private Integer data;
    private boolean available = false;
    public synchronized void produce(int value) throws InterruptedException {
    while (available) {
    // buffer full -> wait wait();
    }
data = value;
    available = true;
    System.out.println("Produced: " + data);
    notifyAll();
    // wake consumers }
public synchronized int consume() throws InterruptedException {
    while (!available) {
    // buffer empty -> wait wait();
    }
int val = data;
    available = false;
    System.out.println("Consumed: " + val);
    notifyAll();
    // wake producers return val;
    }
}
public class ProducerConsumerDemo {
    public static void main(String[] args) {
    SharedBuffer buf = new SharedBuffer();
    Thread producer = new Thread(() -> {
    for (int i = 1;
    i <= 5;
    i++) {

```

try { buf.produce(i); Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } } }, "producer"); Thread consumer = new Thread(() -> { for (int i = 1; i <= 5; i++) { try { buf.consume(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } } }, "consumer"); producer.start(); consumer.start(); } } 
Reasoning to interviewer: 
• wait()/notifyAll() must be called while holding the same monitor (synchronized on the same object). • The while loop re-checks the predicate after waking—critical for spurious wakeups and avoiding lost-notify bugs. • I signal with notifyAll() here because multiple waiters or role flips (produce/consume) can otherwise lose a wakeup. 
Future scope: 
• Replace with BlockingQueue (e.g., ArrayBlockingQueue) to avoid hand-rolled coordination. • Add multiple producers/consumers and bounded capacity to apply backpressure. • Add timeouts and metrics to detect stalls. 

## Scenario 35: Threads grab different locks and end up waiting forever 
Two maintenance tasks each need resources A and B. Task1 locks A then tries B; Task2 locks B then tries A. Under load, they can wait indefinitely—nothing progresses. 
Answer: 
That’s deadlock: circular wait between threads. I prevent it by enforcing a global lock order (e.g., always lock the lower-id resource first), or by using tryLock with timeouts and backing off if I can’t get both quickly. 

```java

import java.util.concurrent.TimeUnit;
    import java.util.concurrent.locks.ReentrantLock;
    class Resource {
    final int id;
    final ReentrantLock lock = new ReentrantLock();
    Resource(int id) {
    this.id = id;
    }
}
class ResourceManager {
    public boolean useBoth(Resource a, Resource b) throws InterruptedException {
    Resource first = a.id < b.id ? a : b;
    // global ordering Resource second = a.id < b.id ? b : a;
    if (!first.lock.tryLock(200, TimeUnit.MILLISECONDS)) return false;
    try {
    if (!second.lock.tryLock(200, TimeUnit.MILLISECONDS)) return false;
    try {
    System.out.println("Acquired " + first.id + " & " + second.id);
    // do work return true;
    }
finally {
    second.lock.unlock();
    }
}
finally {
    if (first.lock.isHeldByCurrentThread()) first.lock.unlock();
    }
}
}
public class DeadlockAvoidDemo {
    public static void main(String[] args) {
    Resource A = new Resource(1), B = new Resource(2);
    ResourceManager rm = new ResourceManager();

```

Runnable r1 = () -> { try { rm.useBoth(A, B); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }; Runnable r2 = () -> { try { rm.useBoth(B, A); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }; new Thread(r1, "t1").start(); new Thread(r2, "t2").start(); } } 
Reasoning to interviewer: 
• Deadlock needs four conditions (mutual exclusion, hold-and-wait, no preemption, circular wait). Breaking circular wait via ordering is simplest and fastest. • tryLock with timeout plus retry/backoff gives resilience against unexpected paths violating the order. 
Future scope: 
• For N resources, sort all targets, then lock in ascending order. • Include logging/metrics on lock acquisition times to spot contention hot spots. • Consider lock-free or staged pipelines to minimize overlapping lock scopes. 

## Scenario 36: Many quick tasks—don’t spawn a thread per task 
A web crawler schedules thousands of short parsing tasks. Creating/destroying a thread for each is wasteful and trashes caches. 
Answer: 
Use an ExecutorService (thread pool). Threads are created once and reused for many tasks. A fixed-size pool caps concurrency; a cached pool grows and shrinks for bursty, I/O-heavy workloads. 

```java

import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    public class ThreadPoolDemo {
    public static void main(String[] args) {
    ExecutorService pool = Executors.newFixedThreadPool(3);
    // cap concurrency for (int i = 0;
    i < 5;
    i++) {
    pool.submit(() -> {
    System.out.println("Running on " + Thread.currentThread().getName());
    try {
    Thread.sleep(200);
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
});
    }
pool.shutdown();
    // graceful stop after tasks finish }
}

```

Reasoning to interviewer: 
• Pools amortize thread creation cost and smooth bursts via a task queue. • Fixed vs cached depends on CPU-bound vs I/O-bound: fixed ~ cores for CPU work; cached for I/O with many waits. • Always shut down pools to avoid thread leaks. 
Future scope: 
• For backpressure, use a bounded queue and a rejection policy (CallerRunsPolicy). • Expose pool metrics (active count, queue depth) and tune. 
• Consider virtual threads for massive I/O concurrency with simple blocking code. 

## Scenario 37: Don’t start the app until all subsystems are ready 
At startup, you spawn workers to warm caches, load configs, and establish DB connections. The main thread should proceed only when all those steps finish. 
Answer: 
CountDownLatch fits perfectly. Initialize with the number of init tasks. Each worker countDown()s when done; the main thread calls await() to block until the count reaches zero. 

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
    Thread.sleep(300);
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
public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(3);
    new Thread(new InitWorker("Cache", latch)).start();
    new Thread(new InitWorker("Config", latch)).start();
    new Thread(new InitWorker("Database", latch)).start();
    latch.await();
    // main waits here System.out.println("All initialization completed. Starting server...");
    }
}

```

Reasoning to interviewer: 
• CountDownLatch is one-shot: once it hits zero, it can’t be reused (unlike CyclicBarrier). • The latch count should reflect required components only—optional ones can signal separately. • Always countDown() in a finally to avoid hanging the main thread if a worker fails. 
Future scope: 
• For repeated phase gates, use CyclicBarrier/Phaser. • Add timeouts to await() and fail fast if a subsystem stalls. • Feed latch progress into health checks so orchestration can observe readiness. 

## Scenario 38: Multi-phase workers that must rendezvous at each step 
You’re coordinating three analytics workers that must all finish Phase 1 before any starts Phase 2, and so on. If one runs late, the others should wait at a “meeting point” and then move together. 
Answer: 
I’ll use a CyclicBarrier with the party count set to the number of workers. Each worker does its phase work, calls barrier.await() to wait for peers, and only then continues. The barrier is reusable, so the same instance gates every phase. A barrier action can run once per phase to perform “phase-close” logic (e.g., flush metrics). 

```java

import java.util.concurrent.BrokenBarrierException;
    import java.util.concurrent.CyclicBarrier;
    class PhaseWorker implements Runnable {
    private final CyclicBarrier barrier;
    private final int totalPhases;
    private final int id;
    PhaseWorker(int id, CyclicBarrier barrier, int totalPhases) {
    this.id = id;
    this.barrier = barrier;
    this.totalPhases = totalPhases;
    }
@Override public void run() {
    try {
    for (int phase = 1;
    phase <= totalPhases;
    phase++) {
    doPhaseWork(phase);
    System.out.printf("T%d reached barrier for phase %d%n", id, phase);
    barrier.await();
    // wait until all arrive }
}
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
catch (BrokenBarrierException e) {
    System.out.printf("T%d: barrier broken;
    aborting%n", id);
    }
}
private void doPhaseWork(int phase) {
    try {
    Thread.sleep(200 + (long)(Math.random() * 300));
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
}
}

```

public class CyclicBarrierDemo { public static void main(String[] args) { int workers = 3, phases = 3; CyclicBarrier barrier = new CyclicBarrier(workers, () -> System.out.println("-- all threads completed this phase --")); for (int i = 1; i <= workers; i++) { new Thread(new PhaseWorker(i, barrier, phases), "worker-" + i).start(); } } } 
Reasoning to interviewer: 
• CyclicBarrier is ideal when a fixed set of threads repeats the same rendezvous across phases. • await() blocks each thread until the last arrives; then everyone proceeds and the barrier resets automatically. • If a thread fails/interrupts, the barrier breaks; peers get BrokenBarrierException, so nothing hangs silently. 
Future scope: 
• If the number of participants can change between phases, switch to Phaser. • Add a timeout to await() to fail fast on stuck phases. • Put aggregation or logging in the barrier action to run once per phase. 

## Scenario 39: Speed up independent element processing over a big list 
You have a large list of orders, and each can be scored independently. You want to use all cores without writing thread plumbing. 
Answer: 
I’ll use Java 8 parallel streams. parallelStream() splits work across the common ForkJoinPool, typically leveraging available cores. I’ll keep operations stateless and side-effect-free, and use terminal ops/collectors that are safe under parallelism. 

```java

import java.util.*;
    import java.util.stream.*;
    import static java.util.stream.Collectors.*;
    public class ParallelStreamDemo {
    public static void main(String[] args) {
    List<Integer> orders = IntStream.rangeClosed(1, 10_000).boxed().toList();
    // Example 1: compute sum of squares (numeric reduce is naturally parallel-friendly) long sumOfSquares = orders.parallelStream() .mapToLong(o -> (long)o * o) .sum();
    System.out.println("Sum of squares = " + sumOfSquares);
    // Example 2: produce a result list (note: result order is not guaranteed) List<Integer> scores = orders.parallelStream() .map(ParallelStreamDemo::score) .collect(toList());
    System.out.println("Scored " + scores.size() + " orders");
    // If you need encounter order preserved in the terminal stage: orders.parallelStream() .map(ParallelStreamDemo::score) .forEachOrdered(s -> {
    /* emit in original order */ });
    }
static int score(int orderId) {
    // pretend CPU-bound scoring return (orderId * 31) & 1023;
    }
}

```

Reasoning to interviewer: 
• Parallel streams parallelize map/filter/reduce across the common ForkJoinPool with work-stealing—great for CPU-bound, independent operations. • Avoid shared mutable state; prefer pure functions and built-in collectors. • forEach on a parallel stream is unordered; use forEachOrdered when you need deterministic output order (with some performance cost). 
Future scope: 
• For different parallelism than the common pool, submit the stream pipeline to a custom ForkJoinPool (e.g., pool.submit(() -> stream.parallel().collect(...)).join()). • If tasks are I/O-bound or include blocking calls, consider virtual threads or CompletableFuture to avoid starving the common pool. • Measure: parallel overhead can outweigh benefits for tiny datasets—use thresholds to switch between sequential and parallel. 

## Scenario 40: Ensure JVM waits for critical work before exit 
Your file-import tool spawns a worker thread to parse a large CSV. Sometimes the JVM exits before parsing finishes because the main thread ends early. 
Answer: 
I’ll start the worker as a non-daemon thread and use join() on it from main (or a coordinator) so the JVM won’t exit until the worker completes. If I truly need a background thread, I’ll keep it daemon but still coordinate shutdown via ExecutorService and awaitTermination(). 

```java

class CsvWorker extends Thread {
    public CsvWorker() {
    setName("csv-worker");
    /* non-daemon by default */ }
@Override public void run() {
    parseCsv();
    }
private void parseCsv() {
    /* long-running parse */ }
}
public class JoinDemo {
    public static void main(String[] args) throws InterruptedException {
    Thread worker = new CsvWorker();
    worker.start();
    worker.join();
    // ensure completion before JVM exit System.out.println("Import finished, exiting cleanly");
    }
}

```

Reasoning to interviewer: 
• Daemon threads don’t keep the JVM alive; non-daemon + join() does. • join() provides a simple lifecycle barrier without busy-waiting or sleeps. • For multiple tasks, ExecutorService.shutdown() + awaitTermination() scales better than manual joins. 
Future scope: 
• Add timeouts to join() and a fallback cancel if a thread is stuck. • Use structured shutdown hooks to flush logs and close connections. • Prefer ExecutorService over raw threads for pooled, observable lifecycles. 

## Scenario 41: Class already extends a base type but must run concurrently 
You have a ReportGenerator that extends BaseService. You also need to execute it on a thread and identify each run in logs. 
Answer: 
I’ll implement Runnable (not extend Thread) so I can still extend BaseService. I’ll name threads via a ThreadFactory to get clean, searchable logs. 

```java

import java.util.concurrent.*;
    import java.util.concurrent.atomic.AtomicInteger;
    class ReportGenerator extends BaseService implements Runnable {
    private final String reportId;
    ReportGenerator(String reportId) {
    this.reportId = reportId;
    }
@Override public void run() {
    generate(reportId);
    }
private void generate(String id) {
    /* heavy compute */ }
}
class BaseService {
    /* existing base behavior */ }
class NamedFactory implements ThreadFactory {
    private final AtomicInteger seq = new AtomicInteger(1);
    public Thread newThread(Runnable r) {
    Thread t = new Thread(r);
    t.setName("report-worker-" + seq.getAndIncrement());
    t.setUncaughtExceptionHandler((th, ex) -> System.err.println("Uncaught in " + th.getName() + ": " + ex));
    return t;
    }
}
public class RunnableWithFactoryDemo {
    public static void main(String[] args) {
    ExecutorService pool = Executors.newFixedThreadPool(2, new NamedFactory());
    pool.submit(new ReportGenerator("R-101"));
    pool.submit(new ReportGenerator("R-202"));
    pool.shutdown();
    }
}

```

Reasoning to interviewer: 
• Runnable decouples task from execution and preserves single inheritance. • ThreadFactory standardizes naming and exception handling. • Using a pool avoids one-thread-per-task overhead. 
Future scope: 
• Switch to Callable<T> if I need results or checked exceptions. • Propagate MDC (logging context) per task for traceability. • Move to virtual threads when blocking I/O dominates. 

## Scenario 42: Prevent inconsistent balance with synchronization 
Two ATM withdrawals hit the same account concurrently; sometimes the balance briefly goes negative. 
Answer: 
I’ll protect the read–modify–write with a private lock (monitor or ReentrantLock). Keep the critical section minimal: only the balance mutation is locked. 

```java

import java.util.concurrent.locks.ReentrantLock;
    class Account {
    private final ReentrantLock lock = new ReentrantLock();
    private int balance;
    Account(int initial) {
    this.balance = initial;
    }
public boolean withdraw(int amt) {
    lock.lock();
    try {
    if (amt <= 0) throw new IllegalArgumentException();
    if (balance < amt) return false;
    balance -= amt;
    return true;
    }
finally {
    lock.unlock();
    }
}
public int snapshot() {
    lock.lock();
    try {
    return balance;
    }
finally {
    lock.unlock();
    }
}
}

```

Reasoning to interviewer: 
• A private lock prevents outside code from interfering (no locking on this). • Only mutate inside the lock; validate inputs outside to reduce contention. • ReentrantLock gives me tryLock, fairness, and interruptible acquisition if needed. 
Future scope: 
• If reads dominate, shift to ReadWriteLock/StampedLock. • For distributed correctness, pair with DB row locks or optimistic versions. • Expose metrics for lock wait time to detect contention. 

## Scenario 43: Consumer sometimes waits forever despite data being produced 
You implemented producer/consumer with wait()/notify(), but occasionally the consumer hangs. 
Answer: 
Common bugs: using if instead of while around wait(), signaling the wrong monitor, or using notify with multiple waiters. I’ll guard with while and use notifyAll, or switch to BlockingQueue. 

```java

import java.util.LinkedList;
    import java.util.Queue;
    class SafeBuffer<T> {
    private final Queue<T> q = new LinkedList<>();
    private final int cap = 3;
    public synchronized void put(T item) throws InterruptedException {
    while (q.size() == cap) wait();
    q.add(item);
    notifyAll();
    // wake consumers }
public synchronized T take() throws InterruptedException {
    while (q.isEmpty()) wait();
    T v = q.remove();
    notifyAll();
    // wake producers return v;
    }
}

```

Reasoning to interviewer: 
• while re-check protects against spurious wakeups and missed signals. • notifyAll avoids waking the wrong role when multiple producers/consumers exist. • A BlockingQueue is the safer high-level primitive. 
Future scope: 
• Replace with ArrayBlockingQueue/LinkedBlockingQueue. • Add timeouts for take/put to avoid indefinite stalls. • Introduce backpressure by bounding capacity and measuring queue depth. 

## Scenario 44: Avoid deadlock in two-account transfer 
Two transfers run simultaneously: A→B and B→A. Threads sometimes freeze. 
Answer: 
I’ll take locks in a strict global order (by account id). That breaks circular wait and removes the deadlock condition. 

```java

import java.util.concurrent.locks.ReentrantLock;
    class BankAccount {
    final long id;
    final ReentrantLock lock = new ReentrantLock();
    private int bal;
    BankAccount(long id, int bal) {
    this.id=id;
    this.bal=bal;
    }
void add(int d){
    bal+=d;
    }
int get(){
    return bal;
    }
}
class SafeTransfer {
    static void transfer(BankAccount from, BankAccount to, int amt) {
    BankAccount first = from.id < to.id ? from : to;
    BankAccount second = from.id < to.id ? to : from;
    first.lock.lock();
    try {
    second.lock.lock();
    try {
    if (from.get() < amt) throw new IllegalStateException("Insufficient");
    from.add(-amt);
    to.add(amt);
    }
finally {
    second.lock.unlock();
    }
}
finally {
    first.lock.unlock();
    }
}
}

```

Reasoning to interviewer: 
• Deadlock needs circular wait; global ordering removes the cycle. • tryLock + timeout can be added as a defensive fallback. • Keep lock scope tight; never call out to user code while holding locks. 
Future scope: 
• For N accounts per transfer, lock them sorted by id. • Add audit logs/metrics for long lock waits. • Consider optimistic concurrency with retry for scalability. 

## Scenario 45: Prevent starvation under heavy contention 
A hot spot lock is usually grabbed by a busy thread; quieter threads rarely progress. 
Answer: 
Switch to a fair ReentrantLock(true) so acquisition is roughly FIFO. I’ll also minimize time inside the lock and avoid lock reentrancy where possible. 

```java

import java.util.concurrent.locks.ReentrantLock;
    class FairHotspot {
    private final ReentrantLock lock = new ReentrantLock(true);
    // fair private int state;
    public void update() {
    lock.lock();
    try {
    state++;
    // minimal work }
finally {
    lock.unlock();
    }
}
}

```

Reasoning to interviewer: 
• Fairness trades throughput for predictability; useful when starvation is unacceptable. • Starvation can also be reduced by backoff (don’t spin), and by sharding locks to reduce contention. • Thread priorities are not portable—don’t rely on them. 
Future scope: 
• Replace shared state with per-thread buffers + periodic merge. • Use queue-based handoff (e.g., BlockingQueue) instead of a hot lock. • Instrument lock contention and adapt dynamically (shard factor, fairness). 

## Scenario 46: Run recurring tasks reliably 
You need to refresh a cache every minute and publish a heartbeat every 5 seconds. 
Answer: 
Use ScheduledThreadPoolExecutor. Choose scheduleAtFixedRate for regular cadence (catching up after delays) or scheduleWithFixedDelay to wait a fixed delay after each run. Handle exceptions so the task doesn’t silently die. 

```java

import java.util.concurrent.*;
    public class SchedulerDemo {
    public static void main(String[] args) {
    ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);
    Runnable cacheRefresh = () -> {
    try {
    /* refresh cache */ }
catch (Exception e) {
    System.err.println("Cache refresh failed: " + e);
    }
};
    Runnable heartbeat = () -> System.out.println("beat " + System.currentTimeMillis());
    ses.scheduleAtFixedRate(cacheRefresh, 0, 1, TimeUnit.MINUTES);
    ses.scheduleWithFixedDelay(heartbeat, 0, 5, TimeUnit.SECONDS);
    // add shutdown hook in real apps }
}

```

Reasoning to interviewer: 
• Fixed-rate is clock-based; fixed-delay waits after completion. • Uncaught exceptions cancel scheduled tasks—wrap bodies with try/catch. • A pool >1 prevents one long task from delaying others. 
Future scope: 
• Use CompletableFuture inside scheduled tasks to compose async subtasks. • Apply jitter to avoid thundering herds across instances. • Add awaitTermination on shutdown for graceful stop. 

## Scenario 47: Fan-out to multiple services and aggregate safely 
A product page needs price, stock, and reviews from different services. Any one may fail, but the page should still render. 
Answer: 
Kick off independent calls with CompletableFuture.supplyAsync. Use per-branch exceptionally/handle to convert failures to defaults. Wait with allOf, then read results with join(). 

```java

import java.util.concurrent.*;
    public class FanoutAggregateDemo {
    private static final ExecutorService io = Executors.newFixedThreadPool(6);
    static CompletableFuture<Double> price(String sku) {
    return CompletableFuture.supplyAsync(() -> fetchPrice(sku), io) .exceptionally(ex -> 0.0);
    }
static CompletableFuture<Integer> stock(String sku) {
    return CompletableFuture.supplyAsync(() -> fetchStock(sku), io) .handle((v, ex) -> ex == null ? v : 0);
    }
static CompletableFuture<Double> rating(String sku) {
    return CompletableFuture.supplyAsync(() -> fetchRating(sku), io) .exceptionally(ex -> 0.0);
    }
public static void main(String[] args) {
    var p = price("SKU-9");
    var s = stock("SKU-9");
    var r = rating("SKU-9");
    CompletableFuture<Void> all = CompletableFuture.allOf(p, s, r);
    all.join();
    System.out.printf("price=%.2f stock=%d rating=%.1f%n", p.join(), s.join(), r.join());
    io.shutdown();
    }
// mocks static double fetchPrice(String s){
    return 999.0;
    }
static int fetchStock(String s){
    return 12;
    }
static double fetchRating(String s){
    return 4.6;
    }
}

```

Reasoning to interviewer: 
• Handle errors at the leaf futures so the aggregator doesn’t blow up. • allOf coordinates completion; join() is safe afterward. • A dedicated I/O pool prevents starving CPU-bound work. 
Future scope: 
• Add orTimeout per branch; apply retries with backoff for transient failures. • Use anyOf for “first result wins” (e.g., mirror endpoints). • Migrate to virtual threads for simpler blocking I/O with huge fan-out. 

## Scenario 48: Producer–consumer with backpressure and multiple consumers 
You’re ingesting events from Kafka and pushing them to 3 different indexer threads. If producers are faster than consumers, memory grows and GC thrashes. You want built-in coordination and backpressure without hand-rolling wait/notify. 
Answer: 
I’ll use a bounded BlockingQueue (e.g., ArrayBlockingQueue) as the handoff buffer. Producers put() (blocking when full), consumers take() (blocking when empty). This automatically throttles producers and wakes consumers without manual wait/notify code. 

```java

import java.util.concurrent.*;
    public class BlockingQueuePipeline {
    public static void main(String[] args) {
    ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
    // bounded = backpressure ExecutorService pool = Executors.newFixedThreadPool(4);
    // Producer pool.submit(() -> {
    for (int i = 1;
    i <= 500;
    i++) {
    queue.put("event-" + i);
    // blocks if full }
queue.put("END");
    // simple sentinel });
    // Consumers for (int c = 0;
    c < 3;
    c++) {
    pool.submit(() -> {
    try {
    for (;;) {
    String e = queue.take();
    // blocks if empty if ("END".equals(e)) {
    queue.put("END");
    break;
    }
// propagate sentinel index(e);
    }
}
catch (InterruptedException ie) {
    Thread.currentThread().interrupt();
    }
});
    }
pool.shutdown();
    }

```

static void index(String e) { /* index to search store */ } } 
Reasoning to interviewer: 
• Bounded queues create natural backpressure—no need for custom flags or sleeps. • Blocking operations handle coordination; no risk of missed signals or spurious wakeups. • A sentinel is a simple, safe shutdown protocol for fan-out consumers. 
Future scope: 
• Replace sentinel with ExecutorService coordinated shutdown or poison pills per consumer. • Prefer LinkedBlockingQueue for unbounded (with caution) or SynchronousQueue for direct handoff. • Add metrics on queue depth to autoscale consumers. 

## Scenario 49: Dynamic team size across phases (CyclicBarrier won’t fit) 
A data pipeline has phases, but the number of workers changes between phases (some tasks finish early or new ones join). You still need all active workers to rendezvous before moving on. 
Answer: 
I’ll use Phaser. Unlike CyclicBarrier, workers can register/deregister between phases. Each participant calls arriveAndAwaitAdvance() to wait for others in the current phase. 

```java

import java.util.concurrent.Phaser;
    class Worker implements Runnable {
    private final Phaser phaser;
    private final int myPhases;
    Worker(Phaser phaser, int myPhases) {
    this.phaser = phaser;
    this.myPhases = myPhases;
    }
@Override public void run() {
    phaser.register();
    try {
    for (int p = 0;
    p < myPhases;
    p++) {
    doWork(p);
    phaser.arriveAndAwaitAdvance();
    // rendezvous }
}
finally {
    phaser.arriveAndDeregister();
    }
}
void doWork(int phase) {
    try {
    Thread.sleep(200 + (long)(Math.random() * 300));
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
System.out.println(Thread.currentThread().getName() + " completed phase " + phase);
    }
}
public class PhaserDemo {
    public static void main(String[] args) {
    Phaser phaser = new Phaser(1);
    // main as a party to prevent premature termination

```

new Thread(new Worker(phaser, 3), "A").start(); new Thread(new Worker(phaser, 2), "B").start(); // B leaves earlier new Thread(new Worker(phaser, 3), "C").start(); // main participates until last phase completes for (int p = 0; p < 3; p++) phaser.arriveAndAwaitAdvance(); phaser.arriveAndDeregister(); } } 
Reasoning to interviewer: 
• Phaser supports dynamic registration/deregistration, which CyclicBarrier doesn’t. • The “main” party prevents the phaser from terminating before we’re done coordinating. • Deregistration ensures we don’t wait for workers that have legitimately finished. 
Future scope: 
• Use onAdvance hook (subclass Phaser) to log or abort on timeouts. • Combine with per-phase time budgets and cancellations. • If phases have distinct thread pools, coordinate pool boundaries via the phaser. 

## Scenario 50: Timebox and cancel an async pipeline (CompletableFuture) 
You fan-out to three services and then enrich the result. If any branch hangs, you want a per-branch timeout, and if enrichment is cancelled, you want the whole chain to stop quickly. 
Answer: 
I’ll compose with CompletableFuture and apply orTimeout(...)/completeOnTimeout(...) per branch. I’ll propagate cancellation by cancelling the combined future, and each stage will check interruption/cancellation by not blocking on get() inside stages. 

```java

import java.util.concurrent.*;
    public class CFTimeoutCancelDemo {
    private static final ExecutorService io = Executors.newFixedThreadPool(4);
    static CompletableFuture<String> slow(String name, long ms) {
    return CompletableFuture.supplyAsync(() -> {
    try {
    Thread.sleep(ms);
    }
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    }
return name;
    }, io);
    }
public static void main(String[] args) {
    CompletableFuture<String> fA = slow("A", 800).orTimeout(500, TimeUnit.MILLISECONDS) // will timeout .exceptionally(ex -> "A:default");
    CompletableFuture<String> fB = slow("B", 200);
    CompletableFuture<String> fC = slow("C", 300);
    CompletableFuture<String> combined = fA.thenCombine(fB, (a, b) -> a + "+" + b) .thenCombine(fC, (ab, c) -> ab + "+" + c);
    // Example: cancel whole pipeline based on a condition // combined.cancel(true);
    try {
    System.out.println("Result: " + combined.join());
    }
finally {
    io.shutdown();
    }

```

} } 
Reasoning to interviewer: 
• orTimeout completes a future exceptionally if it doesn’t finish in time; completeOnTimeout can return a default instead. • I handle timeouts locally (map to default) so the aggregator still succeeds. • Avoid blocking get() inside async stages—compose futures instead so cancellation/timeouts propagate naturally. 
Future scope: 
• Add retries with exponential backoff for transient branches. • Use anyOf to race equivalent mirrors and take the first winner. • With virtual threads, you can also write blocking code and rely on cheap cancellation, while keeping composition semantics. 
