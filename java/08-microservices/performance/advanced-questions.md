# Performance — Advanced Interview Questions

## Important Corrections

- A rising process-memory graph does not automatically prove a Java heap leak. The growth may come from heap, Metaspace, direct buffers, thread stacks, native libraries, memory-mapped files, or the container page cache.
- **10,000 RPS alone is not enough to choose a garbage collector.** Collector selection depends on latency SLOs, live-set size, allocation rate, heap size, available CPU, container memory, and request behavior.
- Virtual threads improve scalability for workloads that spend significant time waiting on blocking I/O. They do not make CPU-intensive work faster or remove database, connection-pool, and downstream-service limits.
- A two-level cache can reduce latency, but it introduces additional invalidation and consistency problems.
- Horizontal scaling does not solve a shared bottleneck such as a saturated database, global lock, hot Kafka partition, or third-party quota.
- Reactor backpressure works only when the upstream producer can respect demand. Uncontrolled external producers still require bounded buffers, admission control, pausing, rejection, or load shedding.
- Lazy initialization can reduce startup time but may move initialization failures and latency to the first production request.

---

# Q81: How Do You Identify and Fix a Memory Leak in a Running Java Microservice?

A memory leak occurs when the application continues retaining objects, classes, or native resources that are no longer logically required.

The first step is to determine **which memory area is growing**.

```text
Container memory
├── Java heap
├── Metaspace
├── Compressed class space
├── Direct buffers
├── Thread stacks
├── Code cache
├── JVM and GC structures
├── JNI/native libraries
└── Memory-mapped files
```

An `OutOfMemoryError` can indicate heap, Metaspace, compressed-class-space, or native-memory exhaustion. It can also indicate that memory is simply undersized rather than leaked. Oracle recommends identifying the exhausted memory pool before assuming a leak. ([Oracle Docs][1])

---

## Step 1: Confirm the Memory Pattern

Monitor:

- Process RSS
- Container working set
- Heap used
- Heap used after major GC
- Metaspace
- Direct-buffer memory
- Thread count
- Allocation rate
- GC frequency and pause duration
- Cache and queue sizes

A strong heap-leak signal is:

```text
Stable traffic
+
stable application behavior
+
heap used after GC continues increasing
```

Oracle describes a continuously rising live set after the application reaches a stable state as a strong indication of a leak. ([Oracle Docs][1])

A normal sawtooth pattern looks like:

```text
Heap usage rises
→ GC runs
→ usage falls
→ cycle repeats around a stable baseline
```

A possible leak looks like:

```text
Heap usage rises
→ GC runs
→ usage falls only slightly
→ post-GC baseline continues rising
```

---

## Step 2: Enable GC Logging

Example:

```bash
-Xlog:gc*,gc+phases=debug,safepoint:file=/var/log/app/gc.log:time,uptime,level,tags
```

Review:

- Heap before and after GC
- Old-generation occupancy
- Full GC frequency
- Allocation failures
- Humongous allocations
- Concurrent-cycle duration
- Metaspace growth
- Time spent at safepoints

GC logs that show repeated full collections without meaningful memory reclamation may indicate retained live data or an undersized heap. ([Oracle Docs][1])

---

## Step 3: Inspect the Running JVM

Basic memory information:

```bash
jcmd <PID> GC.heap_info
```

Class histogram:

```bash
jcmd <PID> GC.class_histogram > class-histogram-1.txt
```

Capture another histogram later:

```bash
jcmd <PID> GC.class_histogram > class-histogram-2.txt
```

Compare them for classes whose instance count and retained size continue growing.

Examples:

```text
java.util.HashMap$Node
byte[]
char[]
java.lang.String
ThreadLocalMap$Entry
CompletableFuture
application-specific DTOs
cache entries
queued tasks
```

A histogram identifies growing classes, but it does not directly show **why they remain reachable**.

---

## Step 4: Capture a Heap Dump

```bash
jcmd <PID> GC.heap_dump /var/dumps/application.hprof
```

Modern JDK documentation recommends `jcmd ... GC.heap_dump` or `jmap` for creating HPROF heap dumps. ([Oracle Docs][2])

Automatic dump on heap exhaustion:

```text
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/dumps
```

### Production warning

A heap dump:

- Can pause or heavily affect the JVM
- May require disk space close to the heap size
- Can contain passwords, tokens, PII, requests, and encryption material
- Should be encrypted and tightly access-controlled

Capture it only after verifying that the target filesystem has enough capacity.

---

## Step 5: Analyze with Eclipse MAT

In Eclipse Memory Analyzer, inspect:

### Leak Suspects Report

Provides likely high-retention objects.

### Dominator Tree

Shows objects retaining the largest amount of memory.

```text
CacheManager
└── ConcurrentHashMap
    └── 5,000,000 Customer objects
```

### Retained Heap

The amount of memory that would become reclaimable if an object were removed.

### Path to GC Roots

Explains why an object cannot be collected.

Example:

```text
CustomerPayload
→ ThreadLocalMap.Entry
→ worker thread
→ thread pool
→ GC root
```

The object is not collected because the long-lived worker thread still references it through a `ThreadLocal`.

---

## Step 6: Distinguish Common Leak Types

### Heap leak

Symptoms:

- Post-GC heap baseline increases
- Old generation remains full
- Object counts continue growing
- Heap dump identifies retained objects

Common causes:

- Unbounded cache
- Unbounded queue
- Static collection
- Listener never deregistered
- `ThreadLocal` not removed
- Large request retained by asynchronous callback
- Uncompleted `CompletableFuture`
- Session state retained indefinitely

Example fix:

```java
private static final ThreadLocal<RequestContext> CONTEXT =
        new ThreadLocal<>();

public void process(Request request) {
    try {
        CONTEXT.set(createContext(request));
        performWork(request);
    } finally {
        CONTEXT.remove();
    }
}
```

---

### Metaspace leak

Symptoms:

- Metaspace continuously increases
- Heap appears stable
- `OutOfMemoryError: Metaspace`
- Many classes and classloaders remain loaded

Common causes:

- Repeated dynamic class generation
- Classloader leak during hot redeployment
- Proxy generation
- Scripting engine compilation
- Libraries retaining obsolete classloaders

Inspect:

```bash
jcmd <PID> VM.classloader_stats
jcmd <PID> GC.class_stats
```

Fix the reference retaining the old classloader rather than only increasing `MaxMetaspaceSize`.

---

### Native-memory leak

Symptoms:

```text
Container RSS grows
but
Java heap remains stable
```

Possible causes:

- Direct `ByteBuffer`
- Netty buffers
- JNI library
- Thread-stack growth
- Native compression library
- Memory-mapped files
- JVM internal native allocation

Enable Native Memory Tracking at startup:

```text
-XX:NativeMemoryTracking=summary
```

Establish a baseline:

```bash
jcmd <PID> VM.native_memory baseline
```

Compare later:

```bash
jcmd <PID> VM.native_memory summary.diff
```

Native Memory Tracking must be enabled when the JVM starts. It measures JVM-managed native memory, but it may not account for every allocation performed by third-party JNI libraries. Oracle documents baseline and difference commands as the standard NMT workflow. ([Oracle Docs][3])

---

### Thread leak

Symptoms:

- Thread count continuously increases
- RSS grows because each platform thread requires stack memory
- Thread dump contains thousands of similar threads

Inspect:

```bash
jcmd <PID> Thread.print -l
```

Possible causes:

- Creating executors repeatedly
- Failing to close clients
- One scheduler per request
- Thread pools with no lifecycle management
- Library resources not closed

---

## Step 7: Use Java Flight Recorder

Start a bounded recording:

```bash
jcmd <PID> JFR.start \
  name=memory-investigation \
  settings=profile \
  duration=10m \
  filename=/var/dumps/memory-investigation.jfr
```

JFR can help identify:

- Allocation hotspots
- Live-object trends
- Allocation stack traces
- GC behavior
- Class loading
- Thread growth
- Lock contention

Oracle recommends JFR and JDK Mission Control for detecting slow memory leaks before the application reaches an `OutOfMemoryError`. ([Oracle Docs][1])

---

## Step 8: Fix and Validate

After fixing the suspected leak:

1. Reproduce the production workload.
2. Run a long soak test.
3. Compare post-GC live-set trends.
4. Compare class histograms.
5. Compare process RSS.
6. Confirm cache, queue, and thread counts stabilize.
7. Verify no regression in latency or throughput.

Do not conclude that the issue is fixed merely because the service survives a short load test.

---

## Interview-Ready Answer

> I first determine whether the growth is in Java heap, Metaspace, direct memory, threads, or another native area. I monitor post-GC heap usage, allocation rate, process RSS, thread count, and direct-buffer metrics. For a heap problem, I compare class histograms, capture a secured heap dump with `jcmd`, and use Eclipse MAT to inspect the dominator tree, retained heap, and paths to GC roots. For native growth, I use Native Memory Tracking and operating-system tools. After fixing the retaining reference, I validate the result through a production-like soak test and confirm that the post-GC live set stabilizes.

---

# Q82: What GC Tuning Would You Apply to a Low-Latency Service Handling 10k RPS?

The correct answer begins with:

> I would not select or tune a collector based only on 10,000 RPS.

You first need:

- P99 and P99.9 latency SLO
- Heap size
- Live-set size
- Allocation rate
- Object lifetime distribution
- CPU budget
- Container memory limit
- Number of replicas
- Traffic burstiness
- Payload size
- Current GC logs and JFR evidence

---

## Step 1: Reduce Allocation Before Tuning GC

GC tuning cannot compensate indefinitely for excessive allocation.

Check for:

- Repeated JSON serialization copies
- Large temporary byte arrays
- Reading complete payloads into memory
- Excessive string concatenation
- Unnecessary DTO transformations
- Unbounded logging
- Per-request object graphs
- Oversized batch operations

Measure:

```text
allocation bytes/second
objects allocated/request
live set after GC
promotion rate
```

---

## Step 2: Start with a Baseline

For a modern HotSpot JVM, G1 is the default collector and Oracle recommends beginning with its defaults, normally setting the maximum heap and changing the pause goal only when evidence justifies it. G1 attempts to balance latency and throughput but does not provide hard real-time pause guarantees. ([Oracle Docs][4])

Example baseline:

```text
-Xms2g
-Xmx2g
-Xlog:gc*,safepoint:file=/var/log/app/gc.log:time,uptime,level,tags
```

Setting `Xms` equal to `Xmx` can reduce heap-resizing variability, but it also commits a larger memory footprint. Benchmark the trade-off under the real container limit.

---

## G1GC

Enable explicitly:

```text
-XX:+UseG1GC
```

Possible pause goal:

```text
-XX:MaxGCPauseMillis=100
```

This is a **soft target**, not a guaranteed maximum.

Use G1 when:

- You need a reasonable latency-throughput balance.
- Heap size is moderate or large.
- A few hundred milliseconds or lower pause objectives are acceptable.
- You want mature ergonomics with limited manual tuning.

### Tune only after evidence

Avoid immediately setting:

```text
G1HeapRegionSize
InitiatingHeapOccupancyPercent
G1NewSizePercent
G1MaxNewSizePercent
```

First let G1’s adaptive behavior operate.

Consider deeper tuning only when logs show:

- Concurrent marking starts too late.
- Full GC occurs.
- Evacuation repeatedly fails.
- Humongous allocations dominate.
- Mixed collections cannot reclaim space fast enough.
- Pause goals consistently force unacceptable throughput loss.

---

## ZGC

Example:

```text
-XX:+UseZGC
-Xms4g
-Xmx4g
```

ZGC performs most work concurrently and is designed for very low pause times. Because it is concurrent, heap headroom is especially important: the heap must contain the live set while allowing allocations to continue during collection. Oracle identifies maximum heap sizing and allocation headroom as the most important ZGC tuning consideration. ([Oracle Docs][5])

Use ZGC when:

- Very low tail latency is more important than maximum throughput.
- The workload has sufficient CPU for concurrent GC.
- The heap has sufficient free headroom.
- Long stop-the-world pauses are unacceptable.
- The deployed JDK supports the required ZGC mode.

Monitor:

- Allocation stalls
- Concurrent-cycle duration
- Heap headroom
- CPU consumed by concurrent GC
- Live-set size
- Container memory

---

## Shenandoah

Example, where supported:

```text
-XX:+UseShenandoahGC
```

Shenandoah reduces pause time by performing evacuation and other collection work concurrently with application threads. Availability and exact behavior depend on the selected OpenJDK distribution and JDK version. ([OpenJDK Wiki][6])

Use it when:

- The deployed JDK includes and supports Shenandoah.
- Low and predictable pauses are required.
- The team has benchmarked its CPU and memory overhead.
- Operational tooling supports the selected collector.

---

## Collector Comparison

| Collector   | Primary strength                | Main trade-off                                     |
| ----------- | ------------------------------- | -------------------------------------------------- |
| G1          | Balanced latency and throughput | Pauses are reduced but not ultra-low or guaranteed |
| ZGC         | Very low pause times            | Requires CPU and heap headroom                     |
| Shenandoah  | Concurrent low-pause collection | Availability and tuning vary by JDK distribution   |
| Parallel GC | High throughput                 | Longer stop-the-world pauses                       |

---

## Container Memory Budget

Do not set:

```text
Container memory limit = 4 GiB
-Xmx4g
```

The process also needs memory for:

- Metaspace
- Thread stacks
- Direct buffers
- Code cache
- GC structures
- Native libraries
- JFR and agents

Example conceptual budget:

```text
Container limit: 4 GiB

Heap:                2.7 GiB
Metaspace:           200 MiB
Direct/native:       400 MiB
Thread stacks:       200 MiB
JVM/GC/code cache:   200 MiB
Safety margin:       remaining memory
```

Actual values must come from measurements.

---

## What to Monitor During GC Tests

- Throughput
- P50/P95/P99/P99.9 latency
- Maximum pauses
- Pause-frequency distribution
- Allocation rate
- Live-set size
- Promotion rate
- Full GC count
- CPU throttling
- GC CPU percentage
- Container RSS
- OOM kills
- Request and queue saturation

Run the same workload against each collector. Do not compare collectors using different heap sizes, traffic patterns, or warm-up periods.

---

## Interview-Ready Answer

> I would not tune GC from RPS alone. I first measure the latency SLO, live set, allocation rate, heap size, CPU budget, and container memory. I normally establish a G1 baseline using mostly default ergonomics and a realistic heap size. If tail-latency requirements remain too strict, I benchmark ZGC or a supported Shenandoah build. I avoid premature region and IHOP tuning, reduce application allocation first, preserve non-heap memory headroom, and select the collector using P99 latency, throughput, CPU, and memory results from production-like tests.

---

# Q83: Explain Virtual Threads and Their Impact on Microservice Throughput

Virtual threads are lightweight Java threads managed by the JDK rather than having a one-to-one relationship with operating-system threads.

They became a permanent Java feature in Java 21 through JEP 444. Their goal is to make thread-per-request code scale to large numbers of concurrent, mostly waiting operations. ([OpenJDK][7])

---

## Platform Threads

Traditional platform threads are relatively expensive resources:

```text
Java platform thread
        ↓
Operating-system thread
```

A service using a limited pool may behave like this:

```text
200 request threads
+
200 blocked database or HTTP calls
=
no thread available for new requests
```

---

## Virtual Threads

Many virtual threads are scheduled over a smaller set of platform carrier threads.

```text
Virtual thread 1 ─┐
Virtual thread 2 ─┤
Virtual thread 3 ─┼→ carrier platform threads
Virtual thread N ─┘
```

When a supported blocking operation waits, the JDK can unmount the virtual thread from its carrier, allowing the carrier to execute another virtual thread.

This allows straightforward blocking code such as:

```java
PaymentResponse response =
        paymentClient.authorize(request);
```

without requiring every waiting request to permanently occupy a dedicated operating-system thread.

---

## Basic Java Example

```java
try (ExecutorService executor =
             Executors.newVirtualThreadPerTaskExecutor()) {

    List<Future<Result>> futures = requests.stream()
            .map(request -> executor.submit(
                    () -> callRemoteService(request)
            ))
            .toList();

    for (Future<Result> future : futures) {
        process(future.get());
    }
}
```

Virtual threads are intended to be created per task. They should not generally be pooled like scarce platform threads.

---

## Spring Boot Configuration

```yaml
spring:
  threads:
    virtual:
      enabled: true

  main:
    keep-alive: true
```

Current Spring Boot supports virtual-thread task execution on Java 21 or later through `spring.threads.virtual.enabled=true`. Its documentation recommends Java 24 or later for the best experience and recommends `spring.main.keep-alive=true` because virtual threads are daemon threads. ([Home][8])

---

## What Virtual Threads Improve

They are particularly effective for:

- Blocking HTTP calls
- JDBC operations
- File I/O
- Waiting for queues
- High-concurrency request processing
- Traditional synchronous code with significant waiting

They can reduce the need to size a request-thread pool around worst-case concurrency.

---

## What They Do Not Improve

### CPU-intensive work

```text
10,000 virtual threads
+
8 CPU cores
=
still approximately 8 cores of CPU execution
```

Virtual threads do not create CPU capacity.

### Database capacity

A database connection pool of 50 still permits approximately 50 simultaneous database operations.

```text
100,000 virtual threads
→ waiting for 50 DB connections
```

You still need:

- Connection-pool limits
- Bulkheads
- Admission control
- Backpressure
- Timeouts

### Third-party quotas

Virtual threads can make it easier to create excessive concurrent calls. Concurrency limits remain necessary.

### Memory-free concurrency

Virtual threads are lighter, not free. Their stacks, task objects, `ThreadLocal` values, trace context, and request data still consume memory.

---

## Pinning

A virtual thread may be unable to unmount from its carrier during certain operations, reducing scalability.

Spring Boot documentation recommends monitoring pinned virtual threads using JFR or `jcmd`, particularly on older supported Java versions. ([Home][9])

Investigate pinning with JFR and thread diagnostics rather than assuming virtual threads automatically improve every application.

---

## `ThreadLocal` Warning

A design that stores large data in `ThreadLocal` may become expensive when the application creates very large numbers of virtual threads.

Avoid:

```java
private static final ThreadLocal<byte[]> LARGE_BUFFER =
        ThreadLocal.withInitial(
                () -> new byte[1024 * 1024]
        );
```

Use narrowly scoped context and remove values when no longer required.

---

## Monitoring

Spring Boot can expose virtual-thread metrics when `micrometer-java21` is present. ([Home][10])

Monitor:

- Active virtual threads
- Pinned events
- Carrier-thread utilization
- DB pool waiting
- HTTP client concurrency
- Heap allocation
- Request latency
- Downstream rejections
- Rate-limit decisions

---

## Interview-Ready Answer

> Virtual threads are lightweight JDK-managed threads introduced as a permanent feature in Java 21. They allow thread-per-request code to support much higher I/O concurrency because a virtual thread waiting on supported blocking I/O can release its carrier thread. They improve scalability for blocking microservices, but they do not make CPU work faster or increase database and downstream capacity. I still apply connection pools, bulkheads, rate limits, and timeouts, and I monitor for pinned virtual threads and excessive `ThreadLocal` usage.

---

# Q84: How Do You Implement Caching in a Microservice?

Caching stores frequently used or expensive-to-compute data closer to the application.

A cache should have an explicit answer for:

```text
What is cached?
What is the source of truth?
How stale may it become?
How is it invalidated?
What happens if the cache fails?
```

---

## Common Architecture

```text
Client
  ↓
Microservice
  ├── L1 local cache: Caffeine
  ├── L2 distributed cache: Redis
  └── Source of truth: database/service
```

### L1 local cache

Benefits:

- Very low latency
- No network call
- Reduces Redis traffic

Trade-offs:

- Each instance has a different copy
- Uses application memory
- Cross-instance invalidation is required

### L2 distributed cache

Benefits:

- Shared across instances
- Larger capacity
- Centralized TTL and invalidation

Trade-offs:

- Network dependency
- Redis can become a bottleneck
- Serialization overhead
- Cache outage can overload the database

---

## Spring Cache Abstraction

Spring provides a cache abstraction through `Cache` and `CacheManager`, but it does not itself provide the actual storage implementation. It supports annotations including `@Cacheable`, `@CachePut`, and `@CacheEvict`. ([Home][11])

Enable caching:

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
}
```

Read-through/cache-aside behavior:

```java
@Service
public class ProductService {

    @Cacheable(
            cacheNames = "products",
            key = "#productId"
    )
    public ProductResponse findProduct(String productId) {
        return productRepository.findById(productId)
                .map(ProductResponse::from)
                .orElseThrow(ProductNotFoundException::new);
    }
}
```

Update the cache:

```java
@CachePut(
        cacheNames = "products",
        key = "#result.productId"
)
public ProductResponse updateProduct(
        String productId,
        UpdateProductRequest request
) {
    return performUpdate(productId, request);
}
```

Evict:

```java
@CacheEvict(
        cacheNames = "products",
        key = "#productId"
)
public void deleteProduct(String productId) {
    productRepository.deleteById(productId);
}
```

Spring Framework includes integration for Caffeine through `CaffeineCacheManager`. ([Home][12])

---

# Cache Patterns

## 1. Cache-aside

```text
Read request
    ↓
Check cache
    ├── hit  → return value
    └── miss → read DB
               ↓
             populate cache
               ↓
             return value
```

Advantages:

- Simple
- Cache contains only accessed data
- Database remains source of truth

Risks:

- Initial cache miss
- Stale values
- Cache stampede

---

## 2. Write-through

```text
Application writes cache
        ↓
Cache synchronously writes source of truth
```

Advantages:

- Cache and source update through one path

Risks:

- Write latency
- Cache-layer dependency
- Failure semantics can be complex

---

## 3. Write-behind

```text
Application writes cache
        ↓
Cache acknowledges
        ↓
Data written to DB asynchronously
```

Advantages:

- Low write latency
- Batchable writes

Risks:

- Data loss
- Ordering problems
- Recovery complexity
- Cache becomes part of the authoritative write path

Use it only when its durability model is acceptable.

---

# Cache Invalidation Strategies

## TTL expiration

```text
product:P-100
TTL = 5 minutes
```

Simple, but data can remain stale until expiry.

Use when:

- Bounded staleness is acceptable.
- Exact immediate consistency is unnecessary.

Add randomized TTL to avoid many popular keys expiring simultaneously.

---

## Explicit eviction after a write

```text
Update database
→ evict cache entry
```

This works within one instance but does not automatically invalidate every local L1 cache.

---

## Event-driven invalidation

```text
Product Service updates database
        ↓
Transactional outbox
        ↓
ProductUpdated event
        ↓
All instances invalidate product:P-100
```

Consumers must be idempotent, and missed invalidation events require reconciliation or TTL as a safety mechanism.

---

## Versioned cache keys

```text
product:P-100:v14
```

A new version naturally causes reads to stop selecting old entries.

Trade-off:

- Old versions remain until TTL or cleanup.

---

## Refresh ahead

Refresh the value before it expires.

Useful for:

- High-traffic keys
- Expensive database queries
- Data where slightly stale responses are acceptable

---

## Cache Stampede Protection

Problem:

```text
Popular key expires
→ 10,000 requests miss
→ 10,000 DB queries
```

Controls:

- Single-flight/request coalescing
- Per-key lock
- Stale-while-revalidate
- Randomized TTL
- Background refresh
- Bounded DB fallback
- Negative caching

Do not let Redis failure turn into database failure.

---

## Negative Caching

Cache “not found” briefly:

```text
product:P-999 → NOT_FOUND
TTL = 30 seconds
```

This protects the source from repeated requests for nonexistent resources.

Do not use long negative TTLs if the resource may be created shortly afterward.

---

## Cache Consistency Decision

| Data                  | Suggested consistency                                     |
| --------------------- | --------------------------------------------------------- |
| Product description   | TTL and event invalidation                                |
| Recommendations       | Stale cache acceptable                                    |
| Exchange rate         | Short TTL and freshness timestamp                         |
| User authorization    | Conservative expiration and revocation                    |
| Account balance       | Usually authoritative read or carefully designed snapshot |
| Inventory reservation | Atomic source-of-truth operation, not cache-only          |

---

## Metrics

Track:

```text
hit ratio
miss ratio
evictions
entry count
cache size
load latency
load failures
Redis latency
cache errors
fallback DB requests
stale-response count
```

Avoid IDs such as `productId` as Prometheus labels.

---

## Interview-Ready Answer

> I normally use cache-aside with Caffeine for optional local L1 caching and Redis for a shared L2 cache. The database remains the source of truth. Invalidation may use TTL, explicit eviction, versioned keys, or outbox-driven events. I protect against cache stampedes with request coalescing, randomized TTLs, stale-while-revalidate, and bounded database fallback. For every cache, I define acceptable staleness, failure behavior, ownership, and metrics before implementation.

---

# Q85: Horizontal vs Vertical Scaling

## Horizontal Scaling

Horizontal scaling adds more application instances.

```text
3 Pods
→ 10 Pods
```

In Kubernetes, horizontal scaling means deploying more Pods to distribute load. ([Kubernetes][13])

Advantages:

- Better fault tolerance
- Supports rolling replacement
- Distributes traffic
- Can scale across nodes and zones

Requirements:

- Interchangeable instances
- Externalized state
- Idempotent operations
- Shared or partitioned data
- Effective load balancing

---

## Vertical Scaling

Vertical scaling gives an existing instance more CPU or memory.

```text
CPU:    1 core → 4 cores
Memory: 1 GiB  → 8 GiB
```

Kubernetes describes vertical scaling as assigning more CPU or memory to existing Pods, while horizontal scaling adds Pods. ([Kubernetes][14])

Advantages:

- Simple for workloads difficult to partition
- Can improve single-process performance
- Useful for large heaps and memory-heavy operations

Limitations:

- Hardware and quota ceiling
- Potential restart or resize constraints
- Larger failure impact
- Does not provide redundancy

---

# When Horizontal Scaling Stops Helping

## 1. Shared database bottleneck

```text
10 service instances
→ database is healthy

100 service instances
→ database connections and locks are exhausted
```

Scaling application replicas can increase database load.

---

## 2. Stateful local sessions

```text
Request 1 → Pod A stores session locally
Request 2 → Pod B cannot find session
```

Solutions:

- External session store
- Stateless token
- Explicit affinity as a temporary compromise

---

## 3. Global lock

```text
Every instance
→ waits for the same distributed lock
```

Adding instances increases contention rather than throughput.

---

## 4. Non-idempotent operations

More instances increase concurrent duplicate execution risk if request identity and database constraints are missing.

---

## 5. Limited partitions

For a Kafka consumer group:

```text
6 partitions
+
20 consumer instances
=
approximately 6 active partition owners
```

Additional consumers remain idle for that topic assignment.

---

## 6. Hot key or hot partition

One account, tenant, Kafka key, or cache key receives most traffic.

The bottleneck is not evenly distributable.

---

## 7. Third-party quota

```text
Provider limit = 1,000 requests/second
```

Adding service replicas does not increase the provider’s limit.

---

## 8. Serial workflow

A process requiring one leader or strict global ordering may have limited parallelism.

---

## 9. Insufficient cluster capacity

The HPA may request more Pods, but they remain pending because no node has enough CPU or memory.

---

## 10. Cold-start delay

Auto-scaling may react too slowly to sudden bursts because new instances need time for:

- Scheduling
- Image pull
- JVM startup
- Spring initialization
- Readiness

---

## Interview-Ready Answer

> Horizontal scaling adds instances, while vertical scaling adds CPU or memory to an instance. Horizontal scaling is normally preferred for stateless microservices because it improves capacity and resilience. However, it provides diminishing returns when the system has a shared bottleneck such as a database, distributed lock, hot partition, third-party quota, limited Kafka partitions, stateful sessions, or non-idempotent workflows. I scale the complete bottlenecked path rather than only the API layer.

---

# Q86: How Would You Design a Rate Limiter Across Multiple Instances?

A local in-memory counter is insufficient:

```text
Instance A allows 100 requests
Instance B allows 100 requests
Instance C allows 100 requests

Actual total = 300
```

A distributed limiter needs shared or consistently partitioned state.

---

## Token Bucket

A token bucket contains:

- Maximum capacity
- Refill rate
- Current tokens
- Last-refill timestamp
- Request cost

```text
Capacity: 100
Refill:   20 tokens/second
Cost:     1 token/request
```

It permits bursts up to the bucket capacity while enforcing the longer-term refill rate.

---

## Redis-Based Design

```text
API instances
      ↓
Redis atomic rate-limit operation
      ↓
allowed / rejected
```

Use one key per limiting dimension:

```text
rate-limit:{tenantId}:{route}
rate-limit:{apiKey}:{operation}
rate-limit:{userId}:payment-submit
```

Do not create excessively broad global keys that become hot unless a truly global quota is required.

---

## Atomic Lua Script

```lua
local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refill_per_ms = tonumber(ARGV[2])
local request_cost = tonumber(ARGV[3])
local ttl_ms = tonumber(ARGV[4])

local current_time = redis.call("TIME")
local now_ms =
    current_time[1] * 1000
    + math.floor(current_time[2] / 1000)

local values = redis.call(
    "HMGET",
    key,
    "tokens",
    "timestamp"
)

local tokens = tonumber(values[1])
local last_timestamp = tonumber(values[2])

if tokens == nil then
    tokens = capacity
end

if last_timestamp == nil then
    last_timestamp = now_ms
end

local elapsed = math.max(
    0,
    now_ms - last_timestamp
)

tokens = math.min(
    capacity,
    tokens + elapsed * refill_per_ms
)

local allowed = 0

if tokens >= request_cost then
    tokens = tokens - request_cost
    allowed = 1
end

redis.call(
    "HSET",
    key,
    "tokens",
    tokens,
    "timestamp",
    now_ms
)

redis.call("PEXPIRE", key, ttl_ms)

return {
    allowed,
    tokens,
    now_ms
}
```

Redis guarantees that a Lua script executes atomically relative to other Redis operations. Scripts should remain short because their execution blocks other server activity while they run. ([Redis][15])

---

## Response

When rejected:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 2
```

Return useful headers where appropriate:

```text
RateLimit-Limit
RateLimit-Remaining
RateLimit-Reset
```

Do not expose sensitive internal capacity details unnecessarily.

---

## Limiting Dimensions

Apply limits by:

- API key
- Tenant
- Authenticated user
- Route
- Operation cost
- IP as a secondary signal
- Global platform capacity

An expensive report request might cost more tokens than a lightweight lookup.

---

## Redis Cluster Considerations

A Lua script accessing multiple keys must follow Redis Cluster key-slot constraints.

Prefer:

- One rate-limit key per script
- Hash tags when multiple related keys must share a slot
- Short atomic operations
- No network calls inside the script

---

## Failure Policy

### Fail open

```text
Redis unavailable
→ allow request
```

Useful only where availability is more important than enforcement.

Risk:

- Abuse protection disappears.

### Fail closed

```text
Redis unavailable
→ reject request
```

Useful for:

- OTP generation
- Authentication attempts
- Expensive paid integrations
- High-risk financial operations

Risk:

- Redis becomes an availability dependency.

### Hybrid fallback

```text
Redis unavailable
→ conservative local emergency limit
→ alert
→ reject above safe local capacity
```

This often provides a better balance.

---

## Multi-Region Design

Possible approaches:

- Independent regional quotas
- Partition user ownership by region
- Global Redis deployment
- Approximate regional budgets
- Central global limiter for selected high-risk operations

A strongly consistent global limiter increases latency and creates cross-region dependency. Decide whether an approximate distributed limit is sufficient.

---

## Monitor

- Allowed and rejected rate
- Redis latency
- Script failures
- Hot keys
- Rate-limit-store availability
- Fail-open/fail-closed activations
- Quota exhaustion by tenant
- Abuse patterns
- False-positive rejections

---

## Interview-Ready Answer

> I use a distributed token-bucket or sliding-window algorithm backed by Redis. The decision and state update execute atomically through Lua or a Redis function, and the key represents the correct dimension such as tenant, API key, and route. I define capacity, refill rate, burst allowance, request cost, expiry, and fail-open or fail-closed behavior. I also add a conservative local emergency limiter so a Redis outage does not automatically cause unlimited traffic or total service failure.

---

# Q87: What Is Backpressure in Spring WebFlux?

Backpressure prevents a fast producer from overwhelming a slower consumer.

```text
Producer: 100,000 events/second
Consumer: 10,000 events/second
```

Without backpressure:

```text
queue grows
→ latency grows
→ memory grows
→ process fails
```

Reactive Streams communicates demand upstream through `request(n)`. Reactor describes downstream demand as the mechanism used to control how much data an upstream publisher produces. ([projectreactor.io][16])

---

## `limitRate`

```java
public Flux<Result> process(Flux<Input> source) {
    return source
            .limitRate(64)
            .flatMap(
                    this::processOne,
                    16
            );
}
```

`limitRate(64)` reshapes downstream demand into smaller requests to the upstream publisher. Reactor documents it as limiting upstream request batches. ([projectreactor.io][17])

`flatMap(..., 16)` limits the number of concurrent asynchronous operations.

---

## Bounded Buffer

For a source that can temporarily outpace downstream processing:

```java
public Flux<Result> processHotSource(
        Flux<Input> source
) {
    return source
            .onBackpressureBuffer(
                    256,
                    dropped -> log.warn(
                            "Backpressure buffer exhausted"
                    ),
                    BufferOverflowStrategy.ERROR
            )
            .flatMap(
                    this::processOne,
                    16
            );
}
```

Available overflow strategies may include:

- Error
- Drop latest
- Drop oldest
- Keep only latest
- Bounded buffering

Reactor provides backpressure operators for bounded buffering, dropping, keeping the latest value, and signalling errors. ([projectreactor.io][18])

---

## Choosing an Overflow Strategy

### Error

```text
Buffer full
→ terminate pipeline with error
```

Use when data loss is unacceptable and upstream must retry durably.

### Drop

```text
Buffer full
→ discard selected data
```

Use only for replaceable data such as:

- UI refresh signals
- Telemetry samples
- Position updates where only the latest matters

Never silently drop:

- Payments
- Ledger entries
- Inventory reservations
- Compliance events

### Latest

Keep the newest state and discard obsolete intermediate states.

Suitable for:

```text
current temperature
latest location
latest dashboard refresh
```

### Durable queue

For business events, use Kafka, RabbitMQ, or another durable broker rather than relying on a large process-memory buffer.

---

## Backpressure Across Boundaries

### HTTP

A server cannot always make external clients obey Reactive Streams demand.

Use:

- Bounded request concurrency
- `429 Too Many Requests`
- `503 Service Unavailable`
- Request-size limits
- Rate limiting
- Timeouts

### Database

Control:

- Connection-pool size
- Query concurrency
- Result streaming
- Fetch size
- Transaction duration

### Kafka

Use:

- Bounded processing concurrency
- Controlled batch size
- Partition pause/resume
- Consumer lag monitoring
- No unbounded handoff queue

### External API

Use:

- Bulkhead
- Rate limiter
- Timeout
- Circuit breaker
- Bounded retry

---

## Blocking Calls in WebFlux

Do not run blocking JDBC or legacy SDK calls on Netty event-loop threads.

Bad:

```java
public Mono<Customer> findCustomer(String id) {
    return Mono.just(
            blockingRepository.findById(id)
    );
}
```

Safer transitional approach:

```java
public Mono<Customer> findCustomer(String id) {
    return Mono.fromCallable(
                    () -> blockingRepository.findById(id)
            )
            .subscribeOn(
                    Schedulers.boundedElastic()
            );
}
```

This isolates blocking work, but the bounded scheduler and downstream connection pools still require capacity controls.

---

## Monitor

- In-flight operations
- Buffer size
- Overflow count
- Dropped elements
- Processing rate
- Event-loop utilization
- Bounded-elastic queueing
- DB pool waiting
- Consumer lag
- End-to-end latency

---

## Interview-Ready Answer

> Backpressure allows a consumer to signal how much work it can accept, preventing an upstream publisher from producing unbounded data. In WebFlux, I control demand using operators such as `limitRate`, limit asynchronous concurrency with `flatMap`, and use only bounded buffers with an explicit overflow strategy. For sources that cannot honor Reactive Streams demand, I add rate limiting, bounded queues, rejection, Kafka pause/resume, or load shedding. Critical business events are never silently dropped.

---

# Q88: How Do You Optimize Spring Boot Cold Start for Serverless?

First measure the startup phases:

```text
Container or runtime initialization
→ JVM startup
→ class loading
→ Spring context creation
→ bean initialization
→ external connections
→ cache warm-up
→ application readiness
```

Do not optimize only the `main()` method while image pulling, secrets retrieval, or database connections dominate startup.

---

# Baseline Optimizations

## Remove blocking startup work

Avoid:

```java
@PostConstruct
public void initialize() {
    loadAllCustomers();
    callMultipleExternalServices();
    warmEntireCache();
}
```

Move optional work after readiness or load data lazily with bounded concurrency.

---

## Reduce application context size

Review:

- Unused starters
- Unnecessary auto-configuration
- Broad component scanning
- Unused Actuator integrations
- Duplicate clients
- Excessive proxy and bean creation
- Large configuration trees

---

## Avoid migrations on every cold start

For serverless functions or many rapidly created instances:

```text
100 cold starts
→ 100 instances attempt migration
```

Run schema migrations through a controlled deployment job or pipeline step.

---

## Optimize packaging

- Small runtime image
- No build tools in runtime layer
- Dependency layers that can be cached
- Regional container registry
- Minimal file count
- Avoid downloading dependencies at startup

---

# Option 1: GraalVM Native Image

GraalVM Native Image compiles the application ahead of time into a platform-specific executable.

Benefits:

- Faster startup
- Lower memory footprint
- No separate JVM required at runtime

Trade-offs:

- Longer and more complex build
- Closed-world/static analysis
- Reflection, proxies, resources, and serialization may need runtime hints
- Fixed classpath
- Some dynamic Java behavior may require redesign

Spring Boot documents native images as especially suitable for containers and Function-as-a-Service environments because of their faster startup and smaller memory footprint. ([Home][19])

Use when:

- Cold-start latency is critical.
- Runtime behavior is compatible with AOT analysis.
- Build time and native-image testing are acceptable.
- The application’s libraries have suitable reachability metadata.

---

# Option 2: CRaC

CRaC checkpoints an already initialized and potentially warmed JVM, then restores that process state later.

```text
Start application
→ initialize Spring
→ warm common paths
→ checkpoint process
→ restore checkpoint for new instance
```

Advantages:

- Very fast restoration
- Retains HotSpot JVM and continued JIT optimization
- Can preserve warmed application state

Challenges:

- Requires a CRaC-enabled JDK and compatible Linux environment
- Open sockets, files, thread pools, credentials, clocks, and external connections require lifecycle handling
- Checkpoint artifacts may contain sensitive memory
- Restore environment must be compatible

Spring Boot currently supports CRaC lifecycle integration for selected resources but warns that application and dependency-specific resource management may still be required. ([Home][20])

---

# Option 3: AppCDS

Application Class Data Sharing archives loaded class metadata in a format that the JVM can load more efficiently.

Generate a dynamic archive:

```bash
java \
  -XX:ArchiveClassesAtExit=application.jsa \
  -jar application.jar
```

Use it:

```bash
java \
  -XX:SharedArchiveFile=application.jsa \
  -jar application.jar
```

AppCDS can improve startup time and reduce memory footprint by storing application classes in a quickly loadable shared archive. ([Oracle Docs][21])

Advantages:

- Retains normal HotSpot JVM operation
- Less restrictive than native compilation
- Can improve class-loading startup cost

Limitations:

- Improvement may be smaller than native image or CRaC.
- Archive must match the application and runtime environment.
- It does not remove slow network initialization or database setup.

---

# Option 4: Lazy Bean Initialization

```yaml
spring:
  main:
    lazy-initialization: true
```

Potential benefit:

- Fewer beans initialized before startup completes

Trade-offs:

- First request may be slower.
- Configuration errors may appear only when a bean is first used.
- Production traffic may trigger expensive initialization.
- It does not help if most beans are required immediately.

Use selective lazy initialization rather than enabling it blindly for every bean.

---

# Option 5: Virtual Threads

Virtual threads do not directly remove Spring class loading or bean creation, but they can help when startup performs many independent blocking operations.

However, startup should not launch unlimited remote requests. Keep initialization bounded and deterministic.

---

# Serverless-Specific Techniques

- Use provisioned concurrency or minimum instances for critical latency paths.
- Keep the deployment artifact small.
- Avoid creating a new database connection pool per invocation.
- Reuse initialized clients across invocations where the platform supports it.
- Keep functions focused.
- Avoid large dependency graphs.
- Do not fetch large configuration sets during every invocation.
- Place dependencies in the same region.
- Use short connection timeouts.
- Measure cold and warm invocation latency separately.

---

# Choosing an Approach

| Approach             |            Startup improvement | Runtime model      | Main trade-off                         |
| -------------------- | -----------------------------: | ------------------ | -------------------------------------- |
| Normal JVM tuning    |                Low to moderate | Full JVM           | Limited cold-start reduction           |
| AppCDS               |                       Moderate | Full JVM           | Archive management                     |
| CRaC                 |        Very high restore speed | Warm HotSpot JVM   | Checkpoint compatibility               |
| GraalVM Native Image |              Very fast startup | Native executable  | AOT restrictions and build complexity  |
| Lazy initialization  |      Faster reported readiness | Normal JVM         | First-use latency and delayed failures |
| Provisioned capacity | Avoids user-visible cold start | Platform-dependent | Cost                                   |

---

## Interview-Ready Answer

> I first measure image-pull time, JVM startup, class loading, Spring context initialization, and external dependency setup. I remove blocking startup work, reduce unnecessary starters and scanning, move migrations out of instance startup, and keep the artifact small. For stricter serverless cold-start requirements, I evaluate GraalVM Native Image, CRaC, and AppCDS. Native image offers fast startup with AOT restrictions, CRaC restores a warmed JVM but requires resource lifecycle handling, and AppCDS accelerates class loading while retaining the normal JVM. I use lazy initialization carefully because it can shift latency and failures to the first request.

---

# Quick Interview Cheat Sheet

| Topic               | Core answer                                                  |
| ------------------- | ------------------------------------------------------------ |
| Memory leak         | Rising retained live set, not merely rising heap before GC   |
| Heap analysis       | Histogram, heap dump, dominator tree, GC roots               |
| Native leak         | RSS grows while heap stays stable; use NMT and OS tools      |
| GC tuning           | Measure SLO, live set, allocation rate, CPU and memory first |
| G1                  | Balanced latency and throughput                              |
| ZGC                 | Very low pauses with CPU and heap-headroom trade-offs        |
| Virtual threads     | Scalable blocking I/O concurrency, not faster CPU            |
| L1 cache            | Fast local cache, per-instance consistency problem           |
| L2 cache            | Shared distributed cache with network dependency             |
| Cache invalidation  | TTL, eviction, events, versioned keys                        |
| Horizontal scaling  | Add instances                                                |
| Vertical scaling    | Add CPU or memory                                            |
| Distributed limiter | Atomic Redis token bucket or sliding window                  |
| Backpressure        | Bound demand, concurrency, buffers, and queues               |
| Native image        | Fast startup and smaller footprint with AOT restrictions     |
| CRaC                | Restore a checkpointed warm JVM                              |
| AppCDS              | Faster class loading through shared archives                 |

# Senior Interview Summary

> For performance problems, I begin with measurements rather than JVM flags. Memory investigations distinguish heap, Metaspace, direct memory, thread stacks, and native allocations. I compare post-GC live sets, class histograms, JFR recordings, heap dumps, and NMT data to identify the retaining path.
>
> Garbage-collector selection is driven by latency SLO, allocation rate, live-set size, CPU, and memory headroom—not RPS alone. G1 provides a strong balanced baseline, while ZGC or Shenandoah may be evaluated for stricter tail-latency requirements.
>
> Virtual threads improve blocking-I/O scalability but do not remove database, downstream, or CPU constraints. Caches require explicit source-of-truth, staleness, invalidation, and failure policies. Horizontal scaling works only while the workload and its dependencies can be partitioned.
>
> Distributed rate limiting uses atomic shared state, while reactive backpressure uses bounded demand, concurrency, and overflow handling. For serverless cold starts, I first optimize normal startup and then evaluate AppCDS, CRaC, GraalVM Native Image, or provisioned capacity according to compatibility, operational complexity, and latency requirements.

[1]: https://docs.oracle.com/en/java/javase/17/troubleshoot/troubleshooting-memory-leaks.html "Troubleshoot Memory Leaks"
[2]: https://docs.oracle.com/en/java/javase/21/migrate/removed-tools-and-components.html?utm_source=chatgpt.com "5 Removed Tools and Components"
[3]: https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr007.html?utm_source=chatgpt.com "2.7 Native Memory Tracking"
[4]: https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html "Garbage-First (G1) Garbage Collector"
[5]: https://docs.oracle.com/en/java/javase/21/gctuning/z-garbage-collector.html?utm_source=chatgpt.com "9 The Z Garbage Collector"
[6]: https://wiki.openjdk.org/spaces/shenandoah/pages/25002018/Main?utm_source=chatgpt.com "Main - Shenandoah"
[7]: https://openjdk.org/jeps/444?utm_source=chatgpt.com "JEP 444: Virtual Threads"
[8]: https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html "Task Execution and Scheduling :: Spring Boot"
[9]: https://docs.spring.io/spring-boot/reference/features/spring-application.html "SpringApplication :: Spring Boot"
[10]: https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com "Metrics :: Spring Boot"
[11]: https://docs.spring.io/spring-framework/reference/integration/cache/strategies.html?utm_source=chatgpt.com "Understanding the Cache Abstraction"
[12]: https://docs.spring.io/spring-framework/reference/integration/cache/store-configuration.html?utm_source=chatgpt.com "Configuring the Cache Storage"
[13]: https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/?utm_source=chatgpt.com "Horizontal Pod Autoscaling"
[14]: https://kubernetes.io/docs/concepts/workloads/autoscaling/vertical-pod-autoscale/?utm_source=chatgpt.com "Vertical Pod Autoscaling"
[15]: https://redis.io/docs/latest/develop/programmability/eval-intro/?utm_source=chatgpt.com "Scripting with Lua | Docs"
[16]: https://projectreactor.io/docs/core/release/reference/coreFeatures/simple-ways-to-create-a-flux-or-mono-and-subscribe-to-it.html?utm_source=chatgpt.com "Simple Ways to Create a Flux or Mono and Subscribe to It"
[17]: https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html?utm_source=chatgpt.com "Flux (reactor-core 3.8.6)"
[18]: https://projectreactor.io/docs/core/release/reference/apdx-operatorChoice.html?utm_source=chatgpt.com "Which operator do I need? :: Reactor Core Reference Guide"
[19]: https://docs.spring.io/spring-boot/reference/packaging/native-image/introducing-graalvm-native-images.html?utm_source=chatgpt.com "Introducing GraalVM Native Images"
[20]: https://docs.spring.io/spring-boot/reference/packaging/checkpoint-restore.html "Checkpoint and Restore With the JVM :: Spring Boot"
[21]: https://docs.oracle.com/en/java/javase/21/vm/class-data-sharing.html?utm_source=chatgpt.com "4 Class Data Sharing - Java"
