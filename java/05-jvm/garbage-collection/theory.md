# Garbage Collection: Throughput vs Latency

## 1. Definition

Garbage collection automatically reclaims heap space occupied by objects that are no longer **reachable** from GC roots, such as active thread stacks, static fields, JNI references, and JVM-internal references.

GC removes the need for explicit `free()` or `delete()` calls, but it does **not** prevent every memory leak. A Java application can still leak memory by unintentionally retaining references to objects it no longer needs. ([Oracle Docs][1])

---

## 2. Why Garbage Collection Exists

Manual memory management can introduce problems such as:

- Use-after-free errors
- Double-free errors
- Dangling pointers
- Memory corruption
- Missing deallocation
- Complex ownership rules

Java trades some CPU, memory, and pause-time overhead for automatic memory reclamation and safer object lifecycles.

---

## 3. How Garbage Collection Works

At a high level, collectors perform variations of these operations:

1. Identify reachable objects.
2. Treat unreachable objects as garbage.
3. Reclaim their memory.
4. Optionally move surviving objects to reduce fragmentation.
5. Update references when objects move.

HotSpot collectors use techniques such as generational collection, parallel processing, concurrent processing, and object aging to improve efficiency. ([Oracle Docs][2])

---

# Generational Collection

## 4. Young and Old Generations

Most Java workloads create many temporary objects that quickly become unreachable. Generational collectors exploit this by dividing objects according to age.

```text
Young generation
├── Eden
├── Survivor space
└── Mostly recently created objects

Old generation
└── Objects that survived several collections
```

A simplified lifecycle is:

```text
Object allocated in Eden
        ↓
Survives young collection
        ↓
Moves between survivor regions
        ↓
Survives long enough
        ↓
Promoted to old generation
```

Concentrating collection effort on areas likely to contain reclaimable memory is one of the main motivations for generational collection. ([Oracle Docs][2])

---

## 5. Minor, Major, and Full GC

These terms are useful but are not perfectly standardized across every collector.

### Young or minor collection

Primarily collects recently allocated objects from the young generation.

```text
Frequent
Usually relatively short
Reclaims short-lived objects
May promote survivors
```

### Old or major collection

Primarily works on the old generation.

Its behavior depends heavily on the selected collector. It may include concurrent phases, stop-the-world phases, or both.

### Full collection

Generally refers to a collection that processes most or all of the heap, often with substantial stop-the-world work.

> Do not diagnose a JVM solely from the labels “minor,” “major,” or “full.” Read the collector-specific GC event, phases, cause, pause duration, and before-and-after heap occupancy.

For example, G1 operates over heap regions and performs young, concurrent-marking, mixed, and occasionally full collections rather than behaving exactly like a traditional contiguous young/old collector. ([Oracle Docs][3])

---

# Throughput vs Latency

## 6. Throughput

GC throughput usually means the percentage of total execution time spent running application code rather than garbage collection.

```text
Throughput =
application execution time
÷
total execution time
```

A throughput-focused application may accept longer pauses if the total amount of useful work completed over several minutes or hours is high. Oracle’s GC documentation describes throughput and maximum pause time as competing tuning goals. ([Oracle Docs][4])

Typical throughput-sensitive workloads include:

- Batch processing
- Data conversion
- Offline analytics
- Build systems
- Background ETL
- Large report generation

---

## 7. Latency

Latency-focused systems care about how long individual operations pause or take to complete.

Typical metrics include:

```text
P50 latency
P95 latency
P99 latency
P99.9 latency
Maximum pause duration
```

A collector can have good overall throughput but still cause occasional long pauses that damage tail latency.

Typical latency-sensitive workloads include:

- Payment APIs
- Trading systems
- User-facing web services
- Real-time bidding
- Interactive applications
- Services with strict response-time SLOs

---

## 8. The Fundamental Trade-Off

A low-latency collector performs more work concurrently with application threads. This reduces stop-the-world pauses but may consume more CPU or memory bandwidth while the application is running.

```text
More concurrent GC work
        ↓
Shorter pauses
        ↓
Potentially more CPU and memory overhead
        ↓
Possible reduction in peak throughput
```

The correct collector depends on the application’s latency requirements, live-data size, allocation rate, available CPU, memory limit, and workload behavior. Oracle explicitly presents collector-selection guidance as a starting point rather than a universal rule. ([Oracle Docs][5])

---

# Common Garbage Collectors

## 9. Collector Comparison

| Collector   | Primary goal                                   | Typical use                                                     |
| ----------- | ---------------------------------------------- | --------------------------------------------------------------- |
| Serial GC   | Simplicity and low overhead on small workloads | Small heaps and limited hardware                                |
| Parallel GC | Maximum throughput                             | Batch and offline processing                                    |
| G1 GC       | Balance throughput and pause-time goals        | General-purpose services                                        |
| ZGC         | Very low pause latency                         | Large or latency-sensitive services                             |
| Shenandoah  | Low pause times through concurrent work        | Supported OpenJDK distributions and latency-sensitive workloads |

Oracle’s current GC documentation describes Parallel GC as the throughput collector, G1 as a mostly concurrent collector that balances pause goals and throughput, and ZGC as a low-latency collector. G1 remains selected by default on most supported hardware configurations. ([Oracle Docs][5])

---

## 10. Parallel GC

Enable it with:

```bash
java -XX:+UseParallelGC -jar application.jar
```

Parallel GC uses multiple GC threads and prioritizes application throughput. Both its young and old-generation collections can run using parallel GC workers. ([Oracle Docs][6])

### Good fit

- Batch processing
- High CPU availability
- Pause times of hundreds of milliseconds or longer are acceptable
- Total completed work matters more than individual response latency

### Trade-off

```text
Higher throughput
but
potentially longer stop-the-world pauses
```

---

## 11. G1 GC

Enable it explicitly with:

```bash
java -XX:+UseG1GC -jar application.jar
```

G1 divides the heap into regions and performs generational, incremental, parallel, and mostly concurrent collection. It attempts to meet pause-time goals while maintaining high throughput. It became the default collector in JDK 9 and remains the default on most current hardware configurations. ([OpenJDK][7])

### Good fit

- General-purpose backend services
- Moderate or large heaps
- A balance between throughput and latency
- Applications without extremely strict pause requirements
- Teams wanting a strong default before specialized tuning

### Pause-time goal

```bash
-XX:MaxGCPauseMillis=200
```

This is a **goal**, not a hard real-time guarantee.

---

## 12. ZGC

Enable ZGC with:

```bash
java -XX:+UseZGC -jar application.jar
```

ZGC performs most expensive collection work concurrently with application threads. Current Oracle documentation describes maximum pauses below a millisecond and pause behavior largely independent of heap size, while noting a potential throughput cost. ([Oracle Docs][5])

### Good fit

- Strict tail-latency requirements
- Large heaps
- Services where long GC pauses are unacceptable
- Sufficient CPU and memory headroom for concurrent collection

### Important limitation

ZGC can reduce GC pause latency, but it does not fix latency caused by:

- Database calls
- Thread-pool saturation
- CPU exhaustion
- Lock contention
- Network delays
- Excessive allocation
- Downstream timeouts

---

## 13. Shenandoah

Shenandoah is an OpenJDK low-pause collector that performs more collection work, including compaction, concurrently with the running application. ([OpenJDK][8])

It is commonly enabled in JDK distributions that provide it with:

```bash
java -XX:+UseShenandoahGC -jar application.jar
```

Collector availability and support can vary by JDK distribution, operating system, and version, so verify it against the exact runtime deployed in production.

---

# Measuring Before Tuning

## 14. Enable GC Logging

A basic command is:

```bash
java \
  -Xlog:gc*:file=gc.log:time,uptime,level,tags \
  -jar application.jar
```

A more production-friendly rotating configuration is:

```bash
java \
  -Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags:filecount=10,filesize=20M \
  -jar application.jar
```

Oracle recommends enabling GC logging as part of preparing a JVM for troubleshooting; separate rotating log files make the history easier to retain across long-running incidents. ([Oracle Docs][9])

For more detailed G1 diagnosis:

```bash
-Xlog:gc*=debug
```

Detailed logs expose collection causes, heap occupancy, concurrent phases, and stop-the-world phase durations. ([Oracle Docs][10])

---

## 15. Enable Automatic Heap Dumps

```bash
java \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/my-app \
  -jar application.jar
```

A heap dump can show which objects consume memory and which reference chains keep them reachable. Oracle recommends enabling `HeapDumpOnOutOfMemoryError` before an incident because heap exhaustion may be difficult to reproduce. ([Oracle Docs][9])

Make sure the output location has sufficient disk space and is writable by the JVM process.

---

## 16. Use Java Flight Recorder

Example:

```bash
jcmd <pid> JFR.start \
  name=gc-investigation \
  duration=5m \
  filename=/tmp/gc-investigation.jfr
```

JFR can help correlate:

- Object allocation
- GC cycles
- Heap usage
- CPU utilization
- Thread activity
- Lock contention
- Safepoints
- File and socket I/O

Oracle recommends continuous or incident-time flight recordings as a low-overhead troubleshooting source. ([Oracle Docs][1])

---

# Investigating GC-Related Latency

## 17. How Would You Determine Whether a P99 Spike Is Caused by GC?

### Step 1: Correlate timestamps

Compare:

```text
Application P99 spike
        ↕
GC pause timestamp
        ↕
JFR or safepoint event
```

If request latency rises at exactly the same time as a stop-the-world pause, GC is a likely contributor.

---

### Step 2: Identify the event

Check:

- Collector name
- Collection cause
- Pause type
- Pause duration
- Heap before and after collection
- Old-generation occupancy
- Humongous allocations
- Concurrent-cycle timing
- Promotion or evacuation failures

---

### Step 3: Measure allocation rate

```text
Allocation rate =
bytes allocated
÷
time
```

A high allocation rate causes collections to occur more frequently even if the application does not have a leak.

Possible causes include:

- Repeated object conversion
- Excessive temporary collections
- Serialization buffers
- Logging argument construction
- Boxing and unboxing
- Large response generation
- Repeated regular-expression compilation

---

### Step 4: Examine the live set

The **live set** is the amount of memory still occupied after an effective collection.

```text
Post-GC heap remains stable
→ probably normal allocation churn

Post-GC heap steadily increases
→ possible memory leak or growing workload
```

Oracle identifies a consistently growing post-collection live set under stable load as a strong sign of retained-memory growth. ([Oracle Docs][1])

---

### Step 5: Check non-GC causes

A similar P99 spike can result from:

- Database connection waits
- Thread-pool queueing
- Lock contention
- CPU throttling
- Container memory pressure
- Network timeouts
- Retry storms

> GC should be investigated with evidence, not assumed to be the cause of every latency spike.

---

# Scenario 6: The Application Ran for 30 Days and Then Crashed with `OutOfMemoryError`

## What might have happened?

“No code changes” does not mean “no state changes.”

Likely causes include:

### 1. Slow heap leak

```text
Unbounded cache
Unremoved listeners
ThreadLocal values
Queued tasks
Session retention
Static collections
```

Objects remain reachable and accumulate slowly until the heap fills.

---

### 2. Workload or data growth

The code remained unchanged, but:

- Traffic increased
- More users became active
- Cached keys increased
- Message backlog increased
- Larger files were processed
- A downstream system slowed down
- More tasks accumulated in queues

This may indicate insufficient capacity rather than a programming leak.

---

### 3. Metaspace or class-loader leak

Repeated dynamic class generation or application redeployment can retain class loaders and class metadata.

The exception may contain:

```text
java.lang.OutOfMemoryError: Metaspace
```

Oracle documents separate OOME causes for Java heap, Metaspace, compressed class space, native allocation, and excessive GC overhead. ([Oracle Docs][1])

---

### 4. Native or direct-memory exhaustion

Possible sources include:

- Direct byte buffers
- JNI libraries
- Thread stacks
- Native HTTP clients
- Memory-mapped files
- Excessive thread creation

A process can run out of native memory even while Java heap usage appears acceptable. ([Oracle Docs][1])

---

### 5. Unbounded queue growth

```text
Producer rate > consumer rate
        ↓
Queue grows for days
        ↓
Tasks and payloads remain reachable
        ↓
OutOfMemoryError
```

---

## Investigation Process

### Step 1: Read the complete OOME message

Examples:

```text
Java heap space
GC overhead limit exceeded
Metaspace
Compressed class space
Unable to create native thread
Direct buffer memory
```

The exact message determines which memory area to investigate.

---

### Step 2: Preserve evidence

Collect:

- Heap dump
- GC logs
- JFR recording
- Thread dump
- Container memory metrics
- JVM flags
- Traffic and queue metrics

---

### Step 3: Analyze the heap dump

Look for:

- Largest retained objects
- Dominator tree
- Paths to GC roots
- Growing collections
- Duplicate byte arrays or strings
- Thread-local values
- Queued tasks
- Old class loaders

---

### Step 4: Examine post-GC memory

If repeated full collections reclaim almost nothing, either:

- The heap is undersized for the legitimate live set, or
- The application is retaining objects unintentionally.

Oracle’s troubleshooting guide uses repeated full collections with little reclaimed space as evidence of either insufficient heap sizing or a memory leak. ([Oracle Docs][1])

### Interview-ready answer

> I first inspect the exact `OutOfMemoryError` message because it may be heap, Metaspace, native memory, direct memory, thread creation, or GC overhead. For a 30-day failure, I suspect a slow leak, an unbounded queue or cache, workload growth, or a class-loader leak. I compare post-GC live-set growth, analyze the heap dump and GC logs, and inspect paths to GC roots before increasing the heap.

---

# Scenario 7: GC Runs Every 30 Seconds and Pauses for Two Seconds

## Is the collection frequency itself the problem?

Not necessarily.

The concerning measurement is:

```text
Two-second stop-the-world pause
```

The first task is to identify:

- Which collector is active
- Which GC event lasts two seconds
- Why it was triggered
- How much memory it reclaimed

---

## Possible causes

### Heap is too small

A small heap fills rapidly and gives the collector little room to operate.

```text
Small heap
→ frequent collection
→ premature promotion
→ old-generation pressure
```

---

### Live set is too large

If most heap objects remain reachable, the collector must repeatedly process a large amount of live data and reclaim little memory.

---

### Allocation rate is excessive

```text
Very high allocation rate
→ young generation fills rapidly
→ frequent collections
```

Reducing unnecessary allocation may help more than changing collector flags.

---

### Full collections are occurring

Look for events such as:

```text
Pause Full
Allocation Failure
System.gc()
Evacuation Failure
Promotion Failure
```

The exact terminology varies by collector.

---

### Humongous objects

With G1, very large objects occupy one or more special regions and can increase old-generation pressure.

Common sources include:

- Large byte arrays
- Large JSON payloads
- File buffering
- Large in-memory reports
- Oversized caches

---

## Fixing the Problem

### 1. Measure before changing flags

Collect GC logs and JFR data under representative load.

### 2. Reduce allocation

- Reuse expensive immutable resources
- Avoid unnecessary intermediate collections
- Stream large payloads
- Limit response sizes
- Remove excessive object conversion
- Bound queues and caches

### 3. Correct retention

Fix leaks revealed by the heap dump and live-set trend.

### 4. Review heap sizing

Increase the heap only when:

- The host or container has enough memory
- The legitimate live set requires more space
- The collector needs additional headroom
- Larger heaps still meet the pause target

### 5. Select the collector according to the SLO

For example:

```bash
# Balanced default
-XX:+UseG1GC

# Throughput priority
-XX:+UseParallelGC

# Strict pause-latency priority
-XX:+UseZGC
```

### 6. Validate after every change

Compare:

- P99 latency
- Maximum GC pause
- GC CPU percentage
- Allocation rate
- Live-set size
- Throughput
- Process memory
- Error rate

### Interview-ready answer

> I would inspect the GC logs to determine whether the two-second event is a young, mixed, or full collection and why it occurs. I would measure allocation rate, live-set size, old-generation occupancy, and reclaimed memory. The fix might be reducing allocation, correcting a leak, resizing the heap, removing explicit full GCs, or selecting a lower-latency collector. I would not tune flags without measuring the result.

---

# Scenario 8: When Would You Choose G1 Over ZGC?

## Choose G1 when

- You want a strong general-purpose default.
- Throughput and latency are both important.
- Pause requirements are moderate rather than extremely strict.
- CPU and memory overhead must remain controlled.
- The current G1 measurements already meet the application’s SLO.
- You want minimal collector-specific tuning.

G1 is designed to balance high throughput with pause-time goals and is the default on most current HotSpot configurations. ([Oracle Docs][5])

---

## Choose ZGC when

- Strict P99 or P99.9 pause latency is required.
- The application uses a large heap.
- Even occasional long G1 pauses violate the SLO.
- The environment can provide enough CPU and memory headroom.
- Benchmarking shows that the throughput trade-off is acceptable.

ZGC is specifically designed for very low pause times and performs most costly work concurrently with application execution. ([Oracle Docs][5])

---

## Comparison

| G1                                      | ZGC                                       |
| --------------------------------------- | ----------------------------------------- |
| Balanced throughput and latency         | Prioritizes very low pauses               |
| Strong general-purpose default          | Specialized latency-sensitive choice      |
| May provide higher throughput           | May use more concurrent CPU resources     |
| Pause goals rather than hard guarantees | Designed for sub-millisecond GC pauses    |
| Suitable for many backend services      | Suitable when GC tail latency is critical |

### Decision rule

> Do not select ZGC merely because it is newer or select G1 merely because it is the default. Run both against the same production-like workload and compare end-to-end latency, throughput, CPU, memory, and allocation behavior.

---

# Common Mistakes

## 1. Assuming every OOME is a heap leak

The failure may involve:

- Heap space
- Metaspace
- Compressed class space
- Native memory
- Direct buffers
- Excessive threads
- GC overhead

---

## 2. Calling every old-generation event a Full GC

Collector terminology and behavior differ. Read the actual event and phases.

---

## 3. Increasing `-Xmx` without checking container limits

A larger heap leaves less process memory for:

- Metaspace
- Thread stacks
- Code cache
- Direct buffers
- Native libraries
- GC data structures

The container or operating system may terminate the process even before Java throws `OutOfMemoryError`.

---

## 4. Treating low GC pause as low application latency

Application latency also includes:

```text
Queueing
CPU execution
Locks
Database access
Network access
Retries
Serialization
Safepoints
```

---

## 5. Tuning many flags at once

Changing several parameters simultaneously makes it difficult to determine which change helped or harmed performance.

Use:

```text
Measure baseline
→ change one controlled variable
→ rerun representative workload
→ compare
```

---

## 6. Ignoring allocation rate

An application with a reasonable live set can still create excessive GC pressure by allocating temporary objects too quickly.

---

# Corrected Trade-Off Table

| Collector  |                        Throughput |            Pause latency | Best starting use                 |
| ---------- | --------------------------------: | -----------------------: | --------------------------------- |
| Serial     |                   Low to moderate |         Potentially long | Small workloads                   |
| Parallel   |                  Highest priority | Longer pauses acceptable | Batch processing                  |
| G1         |                 High and balanced |  Moderate, goal-oriented | General backend services          |
| ZGC        |     Possible throughput trade-off |                 Very low | Strict latency systems            |
| Shenandoah | Possible concurrent-work overhead |                 Very low | Supported low-latency deployments |

---

# Interview Questions

## Question 1: What is the difference between young, major, and full GC?

> A young collection primarily reclaims recently allocated objects. Major usually refers to old-generation work, while full GC generally processes most or all of the heap. The terminology is collector-dependent, so I confirm the exact event and phases in the GC logs.

## Question 2: Why does generational collection improve efficiency?

> Most workloads create many short-lived objects. By concentrating frequent collection on the young generation, the collector can reclaim large amounts of memory without repeatedly scanning the entire heap.

## Question 3: What is the main difference between G1 and ZGC?

> G1 balances throughput and pause-time goals and is a strong general-purpose collector. ZGC prioritizes extremely low pause times by performing more work concurrently, potentially trading some throughput and resource overhead for lower latency.

## Question 4: How do you determine whether a latency spike is GC-related?

> I correlate request latency timestamps with GC and safepoint events, then inspect pause duration, collection cause, heap occupancy, allocation rate, and reclaimed memory. I also compare thread-pool, CPU, database, and downstream metrics before concluding that GC is responsible.

## Question 5: Does garbage collection prevent memory leaks?

> No. GC reclaims unreachable objects. If the application unintentionally retains references through caches, queues, listeners, static fields, or thread locals, those objects remain reachable and cannot be collected.

## Question 6: Should you immediately increase the heap after an OOME?

> No. First identify the memory area and determine whether the problem is an undersized heap, legitimate workload growth, retained objects, Metaspace, native memory, direct buffers, or excessive threads. Increasing the heap can delay a leak without fixing it.

---

# Short Interview Answer

> Garbage collection reclaims objects that are no longer reachable. Generational collectors improve efficiency because most objects die young. The main tuning trade-off is throughput versus pause latency: Parallel GC prioritizes throughput, G1 balances throughput and pause goals, and ZGC prioritizes very low pauses. I enable GC logs, heap dumps on OOME, and JFR before an incident. For latency spikes, I correlate pauses with P99 timing and inspect allocation rate, live-set growth, collection causes, and downstream metrics before changing collectors or heap flags.

[1]: https://docs.oracle.com/en/java/javase/25/troubleshoot/troubleshooting-memory-leaks.html "Troubleshoot Memory Leaks"
[2]: https://docs.oracle.com/en/java/javase/25/gctuning/introduction-garbage-collection-tuning.html?utm_source=chatgpt.com "Introduction to Garbage Collection Tuning - Java"
[3]: https://docs.oracle.com/en/java/javase/25/gctuning/garbage-first-g1-garbage-collector1.html?utm_source=chatgpt.com "7 Garbage-First (G1) Garbage Collector - Java"
[4]: https://docs.oracle.com/en/java/javase/25/gctuning/ergonomics.html?utm_source=chatgpt.com "Ergonomics"
[5]: https://docs.oracle.com/en/java/javase/25/gctuning/available-collectors.html "Available Collectors"
[6]: https://docs.oracle.com/en/java/javase/25/gctuning/parallel-collector1.html?utm_source=chatgpt.com "6 The Parallel Collector"
[7]: https://openjdk.org/jeps/248 "JEP 248: Make G1 the Default Garbage Collector"
[8]: https://openjdk.org/projects/shenandoah/ "OpenJDK: Shenandoah"
[9]: https://docs.oracle.com/en/java/javase/25/troubleshoot/prepare-java-troubleshooting.html "Prepare Java for Troubleshooting"
[10]: https://docs.oracle.com/en/java/javase/17/gctuning/garbage-first-garbage-collector-tuning.html?utm_source=chatgpt.com "8 Garbage-First Garbage Collector Tuning - Java"
