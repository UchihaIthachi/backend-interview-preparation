# Scenario-Based Questions — Memory Leaks, OOME, GC Pauses, and Performance Debugging

## Scenario 1: A Spring Boot application crashes after running for several hours. How would you identify and fix a memory leak?

A memory leak occurs when objects that are no longer useful remain reachable, so the garbage collector cannot reclaim them.

The correct investigation starts with evidence—not immediately increasing heap size or restarting the application.

---

## Step 1: Identify the actual failure

First inspect the complete error or termination reason.

Common failures include:

```text
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: GC overhead limit exceeded
java.lang.OutOfMemoryError: Metaspace
java.lang.OutOfMemoryError: Direct buffer memory
java.lang.OutOfMemoryError: unable to create native thread
```

Also verify whether Java actually produced an `OutOfMemoryError`.

In containers, the process may instead be killed externally because total process memory exceeded the container limit:

```text
Exit code 137
OOMKilled
```

That may occur even when Java heap usage is below `-Xmx`, because the JVM process also consumes:

- Metaspace
- Direct buffers
- Thread stacks
- Code cache
- Native libraries
- GC internal structures

---

## Step 2: Examine the memory pattern over time

Monitor:

- Heap used before and after GC
- Old-generation usage
- Allocation rate
- GC frequency and pause duration
- Metaspace usage
- Direct-buffer usage
- Process resident memory
- Thread count
- Executor queue depth
- Cache size
- Connection-pool usage

### Normal allocation pattern

```text
Heap grows
→ GC runs
→ heap returns to approximately the same baseline
```

### Possible memory leak

```text
Heap grows
→ GC runs
→ post-GC heap remains higher
→ post-GC baseline rises continuously
```

A rising memory graph alone does not prove a leak. It could also represent:

- Legitimate cache warming
- Growing queue backlog
- Larger production traffic
- A scheduled batch loading data
- Increased session count
- A larger legitimate live set

The important measurement is usually the **post-GC live-set trend under comparable load**.

---

## Step 3: Enable diagnostic evidence before the failure

### Automatic heap dump

```bash
java \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/my-service \
  -jar application.jar
```

The destination must have:

- Write permission
- Enough disk space
- Appropriate access controls

Heap dumps may contain:

- Access tokens
- Passwords
- Personal data
- Customer requests
- Payment information

Treat them as sensitive production data.

### GC and safepoint logs

```bash
java \
  -Xlog:gc*,safepoint:file=/var/log/my-service/gc.log:time,uptime,level,tags:filecount=10,filesize=20M \
  -jar application.jar
```

### Java Flight Recorder

```bash
jcmd <pid> JFR.start \
  name=memory-investigation \
  settings=profile \
  duration=5m \
  filename=/tmp/memory-investigation.jfr
```

---

## Step 4: Capture evidence manually when necessary

### Heap dump

```bash
jcmd <pid> GC.heap_dump /tmp/application.hprof
```

### Class histogram

```bash
jcmd <pid> GC.class_histogram
```

### Thread dump

```bash
jcmd <pid> Thread.print -l
```

### Class-loader statistics

```bash
jcmd <pid> VM.classloader_stats
```

### Native-memory statistics

Native Memory Tracking must normally be enabled when the JVM starts:

```bash
-XX:NativeMemoryTracking=summary
```

Then:

```bash
jcmd <pid> VM.native_memory summary
```

---

## Step 5: Analyze the heap dump

Tools such as Eclipse Memory Analyzer can be used to inspect:

- Dominator tree
- Retained heap
- Object counts
- Paths to GC roots
- Large collections
- Thread-local values
- Queued tasks
- Class-loader retention
- Duplicate strings and byte arrays

The main question is:

> Which GC root is keeping these objects reachable?

Example:

```text
System class
→ static cache
→ ConcurrentHashMap
→ millions of session objects
```

Or:

```text
Thread-pool worker
→ ThreadLocalMap
→ request context
→ large customer object graph
```

---

## Step 6: Fix the ownership or lifecycle problem

Common fixes include:

| Cause                    | Fix                                                     |
| ------------------------ | ------------------------------------------------------- |
| Unbounded cache          | Add maximum size, expiration, and eviction              |
| Static collection        | Remove entries or replace it with a bounded store       |
| `ThreadLocal` retention  | Call `remove()` in `finally`                            |
| Listener leak            | Unregister listeners when the component stops           |
| Executor queue growth    | Use a bounded queue and overload policy                 |
| Scheduled task retention | Cancel unused tasks and manage lifecycle                |
| Session growth           | Add expiration and cleanup                              |
| Class-loader leak        | Stop threads and clear references during redeployment   |
| Large queued lambdas     | Capture identifiers instead of large object graphs      |
| Unclosed resources       | Use structured cleanup and framework-managed lifecycles |

### `ThreadLocal` cleanup

```java
try {
    requestContext.set(context);
    processRequest();
} finally {
    requestContext.remove();
}
```

---

## Step 7: Verify the fix

Run the same representative workload and compare:

- Post-GC heap baseline
- Allocation rate
- Object count
- GC frequency
- Process memory
- Queue depth
- Cache size
- P95 and P99 latency

The fix is not validated merely because the application no longer crashes during a short test.

### Interview-ready answer

> I first determine the exact failure type and inspect the post-GC memory trend. I enable heap dumps, GC logs, and JFR, then analyze retained objects and paths to GC roots. Typical causes are unbounded caches or queues, thread-local values, listeners, scheduled tasks, sessions, and class-loader leaks. I fix the ownership or lifecycle problem and verify that the post-GC live set stabilizes under production-like load.

---

# Scenario 2: An API receives very large JSON payloads. How would you reduce memory usage?

A common mistake is deserializing the entire request into one large object graph:

```java
@PostMapping("/import")
public void importRecords(
        @RequestBody LargeImportRequest request
) {
    process(request);
}
```

For a large payload, memory may simultaneously contain:

```text
Raw HTTP bytes
+ decompressed bytes
+ parser buffers
+ String objects
+ DTO objects
+ collections
+ database batch objects
```

The peak memory usage may therefore be several times larger than the payload size.

---

## 1. Enforce request-size limits

Reject unexpectedly large requests before allocating excessive memory.

Possible enforcement layers include:

- API gateway
- Reverse proxy
- Web server
- Spring configuration
- Application validation

Return an appropriate response when the payload exceeds the supported limit:

```text
413 Payload Too Large
```

A server should never accept unlimited request bodies merely because streaming is available.

---

## 2. Use streaming JSON parsing

Jackson’s streaming API can process one object at a time.

```java
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class OrderImportService {

    private final JsonFactory jsonFactory =
            new JsonFactory();

    public void importOrders(InputStream input)
            throws IOException {

        try (JsonParser parser =
                     jsonFactory.createParser(input)) {

            if (parser.nextToken()
                    != JsonToken.START_ARRAY) {

                throw new IllegalArgumentException(
                        "Expected a JSON array"
                );
            }

            while (parser.nextToken()
                    != JsonToken.END_ARRAY) {

                OrderInput order =
                        parser.readValueAs(
                                OrderInput.class
                        );

                process(order);
            }
        }
    }

    private void process(OrderInput order) {
        // Validate and persist incrementally.
    }
}
```

This avoids keeping the entire array and all DTOs in memory simultaneously.

---

## 3. Process data in bounded batches

Do not perform one database operation per record when a bounded batch can be used.

```java
List<OrderInput> batch =
        new ArrayList<>(500);

while (hasMoreRecords()) {
    batch.add(readNextRecord());

    if (batch.size() == 500) {
        repository.saveBatch(batch);
        batch.clear();
    }
}

if (!batch.isEmpty()) {
    repository.saveBatch(batch);
}
```

Batching balances:

- Memory consumption
- Database round trips
- Transaction size
- Failure recovery
- Throughput

A very large single transaction can create:

- Long-held database locks
- Large persistence contexts
- Large rollback cost
- Connection-pool pressure

---

## 4. Clear ORM persistence state

When using JPA for large imports, managed entities can accumulate in the persistence context.

A batch import may need periodic:

```java
entityManager.flush();
entityManager.clear();
```

This prevents every processed entity from remaining managed until the entire transaction ends.

The exact batching strategy should be tested with the chosen database and ORM configuration.

---

## 5. Avoid unnecessary data copies

Potential copies include:

- Reading the request into a `String`
- Converting between `byte[]` and `String`
- Mapping through several DTO layers
- Logging the complete request body
- Re-serializing the payload for validation

Avoid:

```java
String body =
        new String(
                request.getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );
```

For very large payloads, this loads the whole body into memory.

---

## 6. Consider chunked upload or object storage

For extremely large imports, a synchronous JSON API may be the wrong interface.

A safer workflow can be:

```text
Client uploads file
→ object storage
→ API creates import job
→ background worker streams file
→ progress tracked separately
```

This supports:

- Retry
- Resumability
- Progress tracking
- Checkpointing
- Bounded processing
- Reduced request timeout risk

---

## Streaming vs chunking

| Streaming                                             | Chunking                                   |
| ----------------------------------------------------- | ------------------------------------------ |
| Parses one continuous request incrementally           | Client sends several independent portions  |
| Lower memory without changing the external model much | Supports retries per chunk                 |
| One broken connection may fail the full request       | Requires chunk IDs and ordering            |
| Good for sequential processing                        | Good for resumable large uploads           |
| Still needs total-size and time limits                | Needs duplicate and missing-chunk handling |

### Interview-ready answer

> I enforce a maximum payload size, avoid deserializing the entire request, and use a streaming parser to process records incrementally. I persist in bounded batches and clear ORM state between batches. For extremely large imports, I prefer an asynchronous file-upload workflow with object storage, checkpoints, and progress tracking rather than one huge synchronous JSON request.

---

# Scenario 3: A memory leak is reported in production. How would you investigate?

Use a structured sequence:

```text
Confirm symptom
→ classify memory area
→ preserve evidence
→ identify retention path
→ fix lifecycle
→ verify under load
```

## Investigation checklist

1. Is Java heap growing, or is process RSS growing?
2. Does memory decrease after GC?
3. What is the exact OOME or container termination reason?
4. Did traffic, payload size, queue depth, or cache keys increase?
5. Are thread count and direct-memory usage stable?
6. Is Metaspace growing?
7. Did a deployment or plugin reload occur?
8. Are scheduled jobs involved?
9. Are caches, queues, sessions, or listeners unbounded?
10. Which object dominates retained heap?

Do not call every rising memory graph a leak.

---

# Scenario 4: The service crashes with `OutOfMemoryError`. What steps would you take?

## Immediate steps

1. Preserve the full exception and termination details.
2. Avoid repeatedly restarting before collecting evidence.
3. Verify whether the process was JVM-OOM or container-OOM killed.
4. Collect heap dump, GC logs, JFR, and thread dumps.
5. Check recent traffic, deployment, and configuration changes.
6. Inspect queue, cache, session, and thread counts.
7. Analyze the relevant memory area.
8. Apply a temporary safety measure only after identifying the risk.

---

## Interpret the exact message

### `Java heap space`

Investigate:

- Retained heap
- Large arrays
- Unbounded caches
- Queues
- Payload sizes
- Legitimate live-set size

### `GC overhead limit exceeded`

The JVM is spending excessive time collecting while reclaiming little memory.

Investigate:

- Live-set size
- Leak behavior
- Heap sizing
- Allocation pressure

### `Metaspace`

Investigate:

- Class-loader counts
- Dynamic proxy generation
- Hot deployments
- Plugin lifecycle

### `Direct buffer memory`

Investigate:

- Direct `ByteBuffer`
- Netty or networking buffers
- Native-memory limits
- Buffer retention

### `Unable to create native thread`

Investigate:

- Platform-thread count
- Operating-system thread limits
- Per-thread stack size
- Native-memory headroom
- Unbounded executors

---

## Temporary mitigation vs permanent fix

Temporary mitigation might include:

- Reducing incoming traffic
- Disabling a faulty batch job
- Reducing queue capacity
- Clearing a non-critical cache
- Increasing memory with confirmed host headroom
- Restarting after evidence is preserved

Increasing `-Xmx` is not a permanent fix for an unbounded retention problem.

---

# Scenario 5: A method is slow because it creates too many objects. How would you optimize it?

## Step 1: Prove the allocation hotspot

Use:

- JFR allocation profiling
- Allocation flame graphs
- JMH for isolated benchmarks
- Heap histograms
- Application profiling

Do not optimize based only on code appearance.

---

## Step 2: Remove unnecessary intermediate objects

Consider:

- Avoiding temporary collections
- Avoiding repeated DTO transformations
- Avoiding boxing in hot loops
- Reusing immutable compiled resources
- Pre-sizing collections
- Streaming instead of collecting
- Processing primitive values directly
- Avoiding repeated string concatenation

### Repeated string concatenation

Less efficient in a loop:

```java
String result = "";

for (String value : values) {
    result += value;
}
```

Prefer:

```java
StringBuilder result =
        new StringBuilder();

for (String value : values) {
    result.append(value);
}

return result.toString();
```

### Repeated regular-expression compilation

Avoid:

```java
public boolean valid(String value) {
    return Pattern
            .compile("[A-Z]+")
            .matcher(value)
            .matches();
}
```

Prefer:

```java
private static final Pattern CODE_PATTERN =
        Pattern.compile("[A-Z]+");
```

---

## Step 3: Pre-size collections

```java
List<Result> results =
        new ArrayList<>(expectedSize);
```

This can reduce resizing and temporary-array allocation when the expected size is known.

---

## Step 4: Avoid unnecessary boxing

Potential boxing:

```java
List<Integer> values =
        IntStream.range(0, 1_000_000)
                .boxed()
                .toList();
```

For numeric hot paths, primitive arrays or primitive streams may reduce allocations.

---

## Step 5: Avoid premature object pooling

Pooling ordinary small Java objects often introduces:

- Synchronization
- Complex lifecycle
- Stale-state bugs
- Memory retention
- Worse cache behavior

Modern generational collectors handle short-lived small objects efficiently.

Pooling is more appropriate for genuinely expensive or constrained resources such as:

- Database connections
- Network connections
- Large reusable native buffers

### Interview-ready answer

> I use allocation profiling to confirm which objects dominate the hot path. Then I remove unnecessary transformations and temporary collections, pre-size collections, avoid repeated parsing and boxing, and use streaming or batching. I benchmark the result because reducing object count does not automatically improve end-to-end performance.

---

# Scenario 6: How would you prevent memory leaks in Java applications?

## Prevention checklist

### Bound all potentially growing structures

- Caches
- Executor queues
- Message buffers
- Sessions
- Retry queues
- Batch collections

### Clean up thread-local state

```java
try {
    context.set(value);
    execute();
} finally {
    context.remove();
}
```

### Unregister listeners and callbacks

```java
subscription.close();
```

### Shut down executors

```java
executor.shutdown();

if (!executor.awaitTermination(
        30,
        TimeUnit.SECONDS
)) {
    executor.shutdownNow();
}
```

### Use structured resource cleanup

```java
try (InputStream input =
             Files.newInputStream(path)) {

    process(input);
}
```

### Avoid accidental object capture

```java
LargePayload payload = loadPayload();

executor.submit(() ->
        process(payload)
);
```

A queued task retains the entire payload until it executes.

### Monitor resource growth

Alert on trends involving:

- Post-GC heap
- Queue depth
- Cache size
- Session count
- Thread count
- Direct memory
- Metaspace
- Process RSS

---

# Scenario 7: How would you handle high GC pauses in production?

## Step 1: Confirm GC is responsible

Correlate:

```text
Application latency spike
↕
GC pause timestamp
↕
Safepoint or JFR event
```

Do not assume every P99 spike is caused by garbage collection.

Other possible causes include:

- Database latency
- Connection-pool exhaustion
- Executor queueing
- Lock contention
- Network delays
- CPU throttling
- Retry storms

---

## Step 2: Identify the collector and event

Determine:

- Active collector
- Pause type
- Pause cause
- Pause duration
- Heap before and after
- Reclaimed memory
- Old-generation occupancy
- Humongous allocations
- Allocation rate

---

## Step 3: Determine the actual cause

### Excessive allocation

```text
High allocation rate
→ young generation fills rapidly
→ collections occur frequently
```

Reduce unnecessary object creation.

### Large live set

```text
Most objects survive
→ collector must process more live memory
→ pauses become longer
```

Reduce retained data or adjust heap sizing.

### Memory leak

Repeated collections reclaim very little, and post-GC memory continues increasing.

### Heap too small

The collector has insufficient headroom and operates too frequently.

### Heap too large for the pause target

A very large live set can increase work, depending on collector and workload.

### Large objects

Large arrays, payloads, and buffers may create region or allocation pressure.

### Explicit GC calls

Search for:

```java
System.gc();
```

and external tools that may request full collection.

---

## Step 4: Apply measured improvements

Possible actions include:

- Reduce allocation rate
- Fix memory retention
- Stream large payloads
- Bound caches and queues
- Adjust heap size
- Choose a collector suited to the latency SLO
- Remove explicit full-GC requests
- Increase container memory only with native-memory headroom
- Tune one controlled variable at a time

### Collector selection

| Collector   | Typical goal                        |
| ----------- | ----------------------------------- |
| Parallel GC | High throughput                     |
| G1          | Balanced throughput and pause goals |
| ZGC         | Very low GC pause latency           |
| Serial GC   | Small and simple workloads          |

Changing collector does not fix a leak or uncontrolled allocation.

### Interview-ready answer

> I first correlate the latency spike with GC and safepoint events. Then I inspect the collection cause, pause phases, allocation rate, live-set size, and reclaimed memory. The solution may be reducing allocations, fixing retention, resizing the heap, removing explicit full GCs, or selecting a lower-latency collector. I change one factor at a time and compare P99 latency, throughput, CPU, and process memory.

---

# Performance and Latency Scenarios

## Question 1: API response time jumps from 200 milliseconds to 5 seconds. Where do you investigate first?

Start with the request timeline.

Determine where the additional 4.8 seconds is spent:

```text
Gateway queue
+ application queue
+ application execution
+ database
+ downstream HTTP
+ serialization
+ network
```

## Investigation order

1. Confirm which endpoints and instances are affected.
2. Check whether the problem began after a deployment or traffic change.
3. Examine distributed traces.
4. Check executor queues and active threads.
5. Check database and HTTP connection pools.
6. Check slow queries and lock waits.
7. Check downstream latency.
8. Check GC and safepoint events.
9. Check CPU throttling and container limits.
10. Check retries and timeout amplification.

Averages may hide the problem. Compare:

- P50
- P95
- P99
- Error rate
- Queue-wait time

### Interview-ready answer

> I use tracing to identify which span gained latency, then correlate it with executor queues, connection pools, database waits, downstream calls, GC, and infrastructure metrics. I separate queue time from execution time because the method itself may still execute quickly after waiting several seconds for a worker or connection.

---

## Question 2: The system works with 100 users but struggles with 10,000. What usually breaks first?

There is no universal first failure, but bounded shared resources commonly fail before raw CPU.

Typical bottlenecks include:

- Request-thread pool
- Database connection pool
- HTTP connection pool
- Executor queue
- Database locks
- Downstream rate limits
- File descriptors
- Memory
- Network bandwidth
- Kafka partitions or consumer capacity

Example:

```text
500 request workers
→ 20 database connections
→ most workers wait
→ queue grows
→ clients time out
→ clients retry
→ overload becomes worse
```

Increasing the request-thread count does not help when the real capacity limit is the database.

### Interview-ready answer

> I look for the first saturated resource rather than assuming CPU. Under high concurrency it is often a connection pool, worker pool, queue, database lock, downstream service, or memory limit. I compare arrival rate, completion rate, queue depth, active workers, and downstream capacity.

---

## Question 3: CPU usage is low, but latency is extremely high. What could cause this?

Low CPU with high latency usually indicates that threads are waiting rather than computing.

Possible causes include:

- Database connection waits
- Slow database queries
- Remote API calls
- Socket timeouts
- Lock contention
- Executor queueing
- Thread-pool starvation
- Rate limiting
- Disk I/O
- Kafka lag
- Long retry backoff
- Container throttling measurements reported incorrectly

Capture repeated thread dumps and inspect where threads wait.

```bash
jcmd <pid> Thread.print -l
```

Look for stacks involving:

- `HikariPool.getConnection`
- `SocketInputStream.read`
- `CompletableFuture.join`
- `FutureTask.get`
- `LockSupport.park`
- `ReentrantLock`
- `BlockingQueue`
- `CountDownLatch.await`

### Interview-ready answer

> Low CPU with high latency usually means work is blocked or queued. I inspect thread dumps, executor queues, connection pools, lock waits, network calls, and downstream latency. The application may have plenty of CPU but no available database connection or worker.

---

## Question 4: How do you determine whether the bottleneck is application code, database, network, or a downstream service?

Use end-to-end tracing and metrics.

```text
Request
├── gateway
├── application queue
├── business logic
├── database query
├── downstream HTTP call
└── serialization
```

### Application code

Evidence:

- High per-thread CPU
- Hot methods in JFR
- Long application spans
- Serialization or parsing hotspots
- Lock contention inside the JVM

### Database

Evidence:

- Slow-query logs
- Query-plan change
- Connection-pool waiting
- Lock waits
- High database CPU or I/O
- Large scanned-row count

### Network

Evidence:

- Connection setup delay
- DNS delay
- Packet loss
- Retransmissions
- Cross-region traffic
- TLS handshake cost

### Downstream service

Evidence:

- Long client span
- Downstream latency increased
- Connection pool saturated
- Timeouts and retries increased
- Circuit breaker opened

### Interview-ready answer

> I instrument the request into timed spans and correlate application traces with JVM, database, network, and downstream metrics. The span where time accumulates identifies the likely subsystem; thread dumps and resource-pool metrics then explain why that subsystem is slow.

---

## Question 5: A query runs in milliseconds in development but takes 10 seconds in production. Why?

Possible differences include:

- Production has much more data.
- Production statistics are stale.
- An index is missing or different.
- The optimizer chooses a different execution plan.
- Parameter values produce different selectivity.
- The query waits for locks.
- Disk or buffer-cache conditions differ.
- Production has greater concurrency.
- Network latency is involved.
- Development uses a different database version or configuration.
- The production schema is not identical.
- Fetch size or ORM behavior differs.
- The application retrieves far more rows than expected.

## Investigation

- Capture the exact SQL and parameters.
- Obtain the production execution plan.
- Check actual rows versus estimated rows.
- Check indexes and table statistics.
- Check lock and wait events.
- Check rows scanned and returned.
- Check connection-pool waiting separately.
- Compare schemas and configuration.

Be careful with:

```sql
EXPLAIN ANALYZE
```

It executes the query, so it must be used cautiously for expensive production operations.

### Interview-ready answer

> I compare the exact query, bind values, execution plan, statistics, indexes, lock waits, data volume, and rows scanned. Development and production often choose different plans because of different data distributions and concurrency. I also separate database execution time from time spent waiting for a connection.

---

## Question 6: Memory gradually increases for days and then the application crashes. How do you find the leak?

1. Check the exact termination reason.
2. Plot post-GC heap, not only raw heap.
3. Compare heap growth with cache, queue, session, and traffic growth.
4. Capture a heap dump near the high-water mark.
5. Analyze dominators and GC-root paths.
6. Compare class histograms over time.
7. Check native memory if heap remains stable.
8. Fix the retaining reference.
9. repeat the long-duration workload.

Common suspects include:

- Unbounded caches
- Static collections
- Queue backlogs
- Thread locals
- Listeners
- Sessions
- Scheduled tasks
- Class loaders
- Direct buffers

---

# Nightly Production Crash Scenario

## “The Spring Boot service works in development but crashes every night at 2 AM. Walk me through the investigation.”

The exact time is a major clue, but it does not immediately prove a memory leak.

---

## Step 1: Confirm the pattern

Ask what happens around 2 AM:

- Spring `@Scheduled` job
- CronJob
- Batch import
- Database backup
- Database maintenance
- Report generation
- Log rotation
- Certificate or token refresh
- Cache refresh
- File ingestion
- Kubernetes restart policy
- Infrastructure snapshot
- External partner traffic

Search for scheduling definitions:

```java
@Scheduled(cron = "0 0 2 * * *")
public void nightlyJob() {
    // ...
}
```

Also inspect:

- Kubernetes CronJobs
- Linux cron
- CI/CD schedules
- Database maintenance windows
- Cloud automation

---

## Step 2: Determine what “crash” means

Possible meanings include:

- `OutOfMemoryError`
- Container `OOMKilled`
- Health-check failure and restart
- Application deadlock
- Connection-pool exhaustion
- Fatal native error
- Unhandled exception
- Scheduled job terminated the JVM
- Pod eviction
- CPU or memory limit violation

Check:

- Exit code
- Pod status
- Previous container logs
- JVM fatal error log
- Application logs
- Orchestrator events

---

## Step 3: Build a timeline around the incident

Compare from perhaps 1:30 AM to 2:15 AM:

- Heap and post-GC heap
- RSS
- GC pauses
- Thread count
- CPU
- Executor queue depth
- Database connections
- HTTP connections
- Scheduled-job duration
- Database latency
- Disk space
- Request rate
- Error rate

A leak is only one possible pattern.

### Memory-retention pattern

```text
Batch begins
→ objects accumulate
→ post-GC heap rises
→ OOME
```

### Connection-pool pattern

```text
Batch begins
→ connections remain checked out
→ pending connection requests rise
→ API requests block
→ health check fails
```

### Executor pattern

```text
Batch submits thousands of tasks
→ unbounded queue grows
→ memory and latency increase
→ service fails
```

---

## Step 4: Preserve evidence before restart

Collect:

```bash
jcmd <pid> Thread.print -l
jcmd <pid> GC.class_histogram
jcmd <pid> JFR.start name=nightly settings=profile duration=10m filename=/tmp/nightly.jfr
```

Ensure heap dump on OOME is configured in advance.

---

## Step 5: Inspect scheduled-job resource handling

### Database connections

When using JDBC directly:

```java
try (
        Connection connection =
                dataSource.getConnection();

        PreparedStatement statement =
                connection.prepareStatement(sql);

        ResultSet results =
                statement.executeQuery()
) {
    process(results);
}
```

Spring Data and transaction-managed code normally manages connection lifecycle, but long transactions, streaming result sets, and incorrect custom JDBC code can still exhaust the pool.

A connection leak primarily causes **pool exhaustion**. It may indirectly create memory growth because waiting requests and queued tasks accumulate, but it should not automatically be described as a heap leak.

---

## Step 6: Check pool behavior

Monitor:

- Active connections
- Idle connections
- Pending requests
- Acquisition time
- Connection lifetime
- Leak-detection warnings
- Transaction duration

Do not rely on an assumed default pool size. The configured maximum should be checked in the actual environment.

A batch job that monopolizes every connection can make the API appear dead even when no connection is permanently leaked.

---

## Step 7: Check batch-memory design

Unsafe pattern:

```java
List<Order> allOrders =
        repository.findAll();

process(allOrders);
```

For a very large table, prefer paging or streaming:

```java
PageRequest pageRequest =
        PageRequest.of(0, 1_000);

Page<Order> page;

do {
    page = repository.findAll(pageRequest);

    process(page.getContent());

    pageRequest = page.nextPageable();
} while (page.hasNext());
```

Also consider:

- Short transactions
- Bounded batches
- Clearing persistence context
- Avoiding complete result accumulation
- Checkpointing
- Preventing overlapping scheduled executions

---

## Step 8: Prevent overlapping runs

A job scheduled every night may still be running when the next trigger begins.

Possible protections include:

- Distributed scheduling lock
- Job-state record in the database
- Single scheduler leader
- Explicit overlap rejection
- Idempotent batch design

A Java in-memory flag is insufficient when multiple service instances may execute the same schedule.

---

## Step 9: Fix and verify

Examples:

- Bound batch size
- Close resources
- Add transaction boundaries
- Add connection-acquisition timeout
- Prevent overlapping jobs
- Bound executor queues
- Stream large data
- Add cache eviction
- Remove thread-local values
- Fix the actual retained reference

Re-run the same job with production-like data and monitor the full cycle.

---

# Corrections to Common Interview Claims

## “If memory rises before 2 AM, it is definitely a memory leak”

Not necessarily.

It may be:

- A legitimate batch working set
- A growing queue
- Cache warm-up
- Large file processing
- Delayed cleanup
- A transaction retaining managed entities

Confirm through post-GC behavior and heap analysis.

---

## “One unclosed database connection causes a memory leak”

An unreturned connection generally causes **connection-pool exhaustion**.

The resulting blocked requests and queued work may cause additional memory retention, but the primary diagnosis is resource leakage or pool exhaustion.

---

## “Increase the heap and the problem is solved”

Increasing heap may:

- Delay the crash
- Hide a leak temporarily
- Increase GC work
- Reduce native-memory headroom
- Cause container OOM termination

The retaining reference or unbounded workload must still be fixed.

---

## “Alert when heap reaches 80%”

A single percentage threshold can be noisy because heap is expected to grow and shrink.

Better alerts consider:

- Post-GC heap percentage
- Sustained old-generation occupancy
- Allocation rate
- GC pause duration
- GC frequency
- Queue growth
- Time remaining before exhaustion

---

# Production Monitoring Checklist

Monitor these categories together:

| Category       | Key metrics                                  |
| -------------- | -------------------------------------------- |
| Heap           | Used heap, post-GC heap, old generation      |
| GC             | Pause time, frequency, cause, GC CPU         |
| Native memory  | RSS, direct buffers, Metaspace               |
| Threads        | Live count, blocked threads, deadlocks       |
| Executors      | Active workers, queue depth, rejected tasks  |
| Database       | Active, idle, pending connections            |
| HTTP clients   | Active and pending connections               |
| Application    | P95/P99 latency, throughput, errors          |
| Queues         | Backlog size and oldest-message age          |
| Scheduled jobs | Duration, result, overlap, records processed |

---

# Short Interview Answer

> For a production memory or latency incident, I first identify the exact failure and build a timeline using heap, post-GC memory, RSS, GC, thread pools, connection pools, queues, and traces. For memory leaks, I preserve a heap dump and analyze dominators and paths to GC roots. For large payloads, I use request limits, streaming parsing, and bounded batches rather than materializing the entire object graph. For high GC pauses, I measure allocation rate, live-set size, and pause causes before changing collectors or heap flags. For a repeatable 2 AM failure, I inspect scheduled jobs and infrastructure activity first, then verify whether the actual cause is memory retention, connection exhaustion, task backlog, or another bounded resource.
