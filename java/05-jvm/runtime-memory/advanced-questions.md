# JVM Internals — Interview Questions and Answers

## Question 1: What is the difference between heap and stack memory?

### Stack

Every thread has its own JVM stack. Each method invocation creates a **stack frame** containing items such as:

- Parameters
- Local execution state
- Primitive values
- References to objects
- Operand stack
- Return information

Frames are removed automatically when methods return.

### Heap

The heap is shared by all JVM threads and stores objects and arrays. Heap memory is managed by the garbage collector.

```java
public void process() {
    int quantity = 5;
    Product product = new Product("Phone");
}
```

Conceptually:

```text
Thread stack
├── quantity = 5
└── product reference ───────┐
                            │
Heap                        │
└── Product object <────────┘
```

| Stack                                       | Heap                                    |
| ------------------------------------------- | --------------------------------------- |
| One per thread                              | Shared by all threads                   |
| Stores method frames                        | Stores objects and arrays               |
| Automatically unwound on return             | Reclaimed by garbage collection         |
| Usually much smaller                        | Usually the largest JVM memory area     |
| Excessive depth causes `StackOverflowError` | Exhaustion may cause `OutOfMemoryError` |

The JVM specification defines stacks, heap, method area, runtime constant pools, PC registers and native-method stacks as runtime data areas. Exact physical placement may be optimized by the JVM, so “locals always live physically on the stack” is a conceptual model rather than an absolute implementation guarantee. ([Oracle Docs][1])

### Interview answer

> Each thread has its own stack containing method frames and local execution state. The heap is shared and contains objects and arrays managed by the garbage collector. Stack exhaustion usually causes `StackOverflowError`, while heap exhaustion can cause `OutOfMemoryError: Java heap space`.

---

## Question 2: How does the JVM memory model work?

This question can refer to two different concepts.

### 1. JVM runtime memory areas

```text
JVM process
├── Heap
├── Per-thread JVM stacks
├── PC register per thread
├── Method area / HotSpot Metaspace
├── Runtime constant pools
├── Native-method stacks
├── Code cache
└── Direct and other native memory
```

The JVM specification defines the abstract runtime areas, while HotSpot implements the method area largely through Metaspace and other internal structures. ([Oracle Docs][2])

### 2. Java Memory Model

The **Java Memory Model**, or JMM, defines visibility and ordering between threads.

Its central concept is **happens-before**:

- Monitor unlock happens-before a later lock of the same monitor.
- A volatile write happens-before a later read of that variable.
- Actions before `Thread.start()` are visible to the new thread.
- Actions in a thread are visible after another thread successfully returns from `join()`.

Without proper synchronization, one thread is not guaranteed to observe another thread’s writes correctly.

### Interview answer

> Runtime memory areas describe where execution data is managed: heap, stacks, method area, constant pools and native memory. The Java Memory Model is different—it defines visibility and ordering between threads through happens-before relationships.

---

## Question 3: Explain the class-loading lifecycle

The lifecycle is commonly described as:

```text
Loading
   ↓
Linking
├── Verification
├── Preparation
└── Resolution
   ↓
Initialization
   ↓
Use
   ↓
Unloading
```

The JVM specification formally defines loading, linking and initialization. Linking contains verification, preparation and resolution. ([Oracle Docs][2])

### 1. Loading

The class loader:

- Locates or generates class bytes.
- Reads the class definition.
- Creates the corresponding runtime representation.
- Produces a `Class<?>` object.

```java
Class<?> type =
        Class.forName("com.example.PaymentService");
```

### 2. Verification

The JVM checks that the bytecode is structurally valid and safe to execute.

Examples include:

- Valid class-file structure
- Legal bytecode
- Correct operand types
- Valid control-flow structure
- No illegal stack operations

### 3. Preparation

Memory is allocated for static fields and default values are assigned.

```java
class Example {
    static int count = 10;
}
```

During preparation, `count` conceptually receives `0`. The explicit assignment to `10` occurs during initialization.

Compile-time constants are a special case and may be represented through class-file constant data.

### 4. Resolution

Symbolic references in the constant pool are converted into direct runtime references.

Examples include references to:

- Classes
- Fields
- Methods
- Interfaces

Resolution may occur eagerly or lazily, provided JVM rules are respected.

### 5. Initialization

The JVM executes class initialization logic, including:

- Static field initializers
- Static initializer blocks
- The generated `<clinit>` method

```java
class Example {
    static int value = loadValue();

    static {
        System.out.println("Initialized");
    }
}
```

A superclass is initialized before its subclass.

### 6. Unloading

Classes can generally be unloaded only when their defining class loader becomes unreachable and the JVM decides to perform class unloading.

A class-loader leak can therefore retain:

- Classes
- Static fields
- Class metadata
- Application object graphs

---

## Question 4: What are Bootstrap, Platform and Application ClassLoaders?

## Bootstrap ClassLoader

The bootstrap loader loads fundamental Java runtime classes and modules.

Examples include classes such as:

```text
java.lang.Object
java.lang.String
java.util.List
```

It is implemented as part of the JVM rather than as an ordinary Java `ClassLoader` object. For a bootstrap-loaded class:

```java
System.out.println(
        String.class.getClassLoader()
); // null
```

`null` represents the bootstrap loader.

## Platform ClassLoader

The platform loader loads platform classes that are not defined by the bootstrap loader, including Java platform APIs, implementation classes and JDK runtime classes assigned to it or its ancestors. ([Oracle Docs][3])

```java
ClassLoader platform =
        ClassLoader.getPlatformClassLoader();
```

## Application ClassLoader

The application loader, also called the **system class loader**, usually loads application classes from:

- Class path
- Module path
- Application JARs
- Configured runtime locations

```java
ClassLoader application =
        ClassLoader.getSystemClassLoader();
```

## Parent delegation

The normal delegation strategy is:

```text
Application loader
       ↓
Platform loader
       ↓
Bootstrap loader
```

A loader usually asks its parent first before attempting to load the class itself. This helps prevent application code from replacing core platform classes. The `ClassLoader` documentation describes parent delegation as the standard strategy. ([Oracle Docs][3])

Custom loaders may intentionally implement different behavior, such as child-first loading in plugin or application-container environments.

### Class identity

A class is identified not only by its binary name but also by its defining loader.

```text
com.example.Plugin
loaded by Loader A
```

and:

```text
com.example.Plugin
loaded by Loader B
```

are different runtime types, even when their bytecode is identical.

---

## Question 5: How does garbage collection work internally?

Garbage collection determines which heap objects remain reachable and reclaims memory occupied by unreachable objects.

### Step 1: Start from GC roots

Typical roots include:

- Active thread-stack references
- Static fields
- JNI references
- JVM internal references
- Active monitors

### Step 2: Trace reachable objects

```text
GC roots
   ↓
Object A
   ↓
Object B
   ↓
Object C
```

These objects remain live.

Objects that cannot be reached from the root graph are garbage.

### Step 3: Reclaim or relocate memory

Collectors use combinations of:

- **Marking:** Identify live objects.
- **Sweeping:** Reclaim unreachable regions.
- **Copying or evacuation:** Move live objects elsewhere.
- **Compaction:** Move objects to reduce fragmentation.
- **Concurrent marking:** Perform part of the analysis while application threads run.

### Generational collection

Many collectors separate objects by age because most allocations become unreachable relatively quickly.

```text
Young objects
→ survive collections
→ age
→ may move into old-generation storage
```

### Stop-the-world and concurrent phases

Some collector operations pause application threads. Modern collectors also perform substantial work concurrently.

GC implementations use supporting mechanisms such as:

- Write barriers
- Card tables
- Remembered sets
- Region tracking
- Allocation buffers

G1, for example, divides the heap into regions and uses previous collection behavior to decide which regions are most profitable to reclaim while targeting pause goals. ([Oracle Docs][4])

---

## Question 6: G1 GC vs ZGC vs Serial GC

| Collector | Main objective                           | Typical use                         |
| --------- | ---------------------------------------- | ----------------------------------- |
| Serial GC | Simplicity and low overhead              | Small heaps, small applications     |
| G1 GC     | Balanced throughput and pause-time goals | General-purpose backend services    |
| ZGC       | Very low pause latency                   | Large or latency-sensitive services |

### Serial GC

Enable with:

```bash
-XX:+UseSerialGC
```

Characteristics:

- Uses a small number of GC resources.
- Collection work is primarily serial.
- Can produce long pauses on large heaps.
- Suitable for small and simple applications.

### G1 GC

Enable with:

```bash
-XX:+UseG1GC
```

Characteristics:

- Region-based
- Generational
- Mostly concurrent
- Tries to meet a pause-time target
- Strong balance between throughput and latency
- Default collector in current HotSpot configurations documented by Oracle

A pause target such as:

```bash
-XX:MaxGCPauseMillis=200
```

is a goal, not a hard deadline.

### ZGC

Enable with:

```bash
-XX:+UseZGC
```

Characteristics:

- Performs most expensive work concurrently.
- Designed for pause times of a few milliseconds.
- Pause duration is designed to remain largely independent of heap size.
- May consume more concurrent CPU and trade some throughput for lower latency.

Oracle’s current HotSpot documentation describes Serial GC as suitable for small applications, G1 as the default balanced collector and ZGC as the low-latency collector. ([Oracle Docs][5])

### Selection rule

- Use **Serial GC** for small, simple processes.
- Start with **G1** for ordinary server workloads.
- Evaluate **ZGC** when GC pause latency violates the service SLO.
- Benchmark using production-like traffic rather than choosing by reputation.

---

## Question 7: What triggers a Full GC?

The meaning of “Full GC” is collector-dependent. It generally refers to an expensive collection involving most or all of the heap.

Possible triggers include:

### Old-generation allocation failure

The JVM cannot allocate or promote an object into available old-generation space.

### Collector cannot keep up

A concurrent collector fails to reclaim space before allocation demand exhausts usable capacity.

### Evacuation or promotion failure

A collector attempts to move surviving objects but cannot find sufficient destination space.

### Fragmentation or large allocations

There may be enough total free memory but not enough usable space for the required allocation.

Large or humongous objects can increase this pressure.

### Explicit collection request

```java
System.gc();
```

or:

```java
Runtime.getRuntime().gc();
```

These are requests, not absolute guarantees. Depending on JVM options and collector behavior, they may cause an expensive collection.

### Metadata pressure

Metaspace growth can trigger collection activity to attempt class unloading, although the exact behavior depends on the collector and JVM configuration.

### Diagnostic operations

Some heap-dump operations may request collection before creating the dump unless configured to include all objects.

### What to investigate

Never stop at:

```text
Full GC occurred
```

Inspect:

- GC cause
- Collector
- Heap before and after
- Pause duration
- Reclaimed memory
- Allocation rate
- Promotion rate
- Humongous-object allocation
- Metaspace usage

```bash
-Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags
```

---

## Question 8: How do you identify memory leaks in production?

A Java leak occurs when objects that are no longer useful remain reachable.

### Step 1: Observe the post-GC baseline

```text
Normal allocation churn:
heap rises
→ GC
→ heap returns to a stable baseline

Likely retention problem:
heap rises
→ GC
→ baseline remains higher
→ baseline continues increasing
```

A steadily increasing live set under stable load is a strong leak indicator. Oracle recommends monitoring live-set growth and analyzing retained objects rather than assuming every OOME is a leak. ([Oracle Docs][6])

### Step 2: Check common retention sources

- Static collections
- Unbounded caches
- Executor queues
- `ThreadLocal` values
- Listeners that are never removed
- Sessions that never expire
- Scheduled tasks
- Class-loader retention
- Large captured objects in queued lambdas

### Step 3: Capture supporting evidence

- Heap dump
- GC logs
- JFR recording
- Class histogram
- Thread dump
- Cache and queue sizes
- Process RSS
- Native-memory information

### Step 4: Analyze retained memory

In a heap-analysis tool, inspect:

- Dominator tree
- Retained size
- Paths to GC roots
- Large collections
- Duplicate strings or arrays
- Thread-local maps
- Class loaders

The key question is:

> Which GC root is retaining this object?

---

## Question 9: What is a heap dump, and when would you analyze it?

A heap dump is a snapshot of objects in the Java heap and their reference relationships.

It can reveal:

- Object counts
- Object sizes
- Retained graphs
- Class instances
- Reference chains
- GC roots
- Thread-local values
- Class-loader retention

Oracle recommends `jcmd` as the preferred diagnostic utility for generating heap dumps. ([Oracle Docs][7])

```bash
jcmd <pid> GC.heap_dump /tmp/application.hprof
```

Enable automatic dump creation:

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/application
```

### Analyze a heap dump when

- Heap usage continually rises.
- An application throws `OutOfMemoryError: Java heap space`.
- A cache or queue appears unbounded.
- A class-loader leak is suspected.
- Unexpected object counts grow.
- Large structures remain reachable after GC.

### Important operational concern

Heap dumps can:

- Be approximately as large as the used heap
- Pause or affect the target process
- Require significant disk space
- Contain credentials, tokens and customer data

They must be stored and handled securely.

---

## Question 10: How does the JIT compiler improve performance?

Java source is compiled into bytecode. The JVM can initially interpret bytecode and collect runtime profiling information.

Frequently executed code becomes **hot** and may be compiled into optimized native machine code.

```text
Bytecode
   ↓
Interpretation and profiling
   ↓
Hot method detected
   ↓
JIT compilation
   ↓
Optimized machine code
```

### Common JIT optimizations

- Method inlining
- Devirtualization
- Constant folding
- Dead-code elimination
- Loop optimizations
- Bounds-check elimination
- Escape analysis
- Lock elimination
- Scalar replacement
- Hardware intrinsics

Current HotSpot uses tiered compilation by default, combining faster initial compilation with more aggressive optimization for sufficiently hot code. Oracle documents tiered compilation, inlining, escape analysis and code-cache behavior through the JVM options. ([Oracle Docs][5])

### Speculative optimization

The JVM can optimize using assumptions derived from runtime profiles.

For example, if an interface call has always targeted one implementation, the JVM may inline it.

If that assumption later becomes invalid, the JVM may:

```text
Deoptimize compiled code
→ return to a less optimized form
→ gather new information
→ compile again
```

### Warm-up effect

A Java application may become faster after running for some time because:

- More methods become compiled.
- Profiles become more accurate.
- Optimized code replaces interpreted code.
- Classes and resources become initialized.

This is why reliable benchmarks require warm-up and tools such as JMH rather than simple one-off timing.

---

## Question 11: What is escape analysis?

Escape analysis determines whether an object reference can escape the scope in which it was created.

```java
public int calculate() {
    Point point = new Point(10, 20);
    return point.x() + point.y();
}
```

If `point` does not escape, the JIT may optimize its allocation.

Possible outcomes include:

### Scalar replacement

Instead of allocating a `Point`, the JVM may treat its fields as independent scalar values.

```text
Point object
→ eliminated

x = 10
y = 20
```

### Lock elimination

If an object is proven thread-confined, synchronization on it may be removed.

### Reduced allocation pressure

Eliminating allocations can reduce:

- Heap traffic
- Garbage collection
- Memory writes
- Object-header overhead

Escape analysis is enabled by default in current HotSpot JVM options. ([Oracle Docs][5])

### Important correction

Escape analysis does **not** mean every non-escaping object is physically allocated on the stack.

The JVM may:

- Eliminate the allocation
- Replace the object with scalar values
- Retain an ordinary heap allocation

The optimization is not guaranteed.

---

## Question 12: What is Metaspace, and how is it different from PermGen?

Metaspace stores class-related JVM metadata.

Examples include information about:

- Classes
- Methods
- Fields
- Runtime constant pools
- Class-loader relationships

The corresponding Java `Class<?>` object still exists on the heap, while much underlying VM metadata is stored in Metaspace.

| PermGen                       | Metaspace                       |
| ----------------------------- | ------------------------------- |
| Used before Java 8            | Used since Java 8               |
| Fixed JVM-managed region      | Uses native memory              |
| Configured with PermGen flags | Configured with Metaspace flags |
| Failure: `PermGen space`      | Failure: `Metaspace`            |

Useful options include:

```bash
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=512m
```

Modern HotSpot’s Elastic Metaspace implementation reduces fragmentation and can return unused Metaspace memory to the operating system after class unloading. ([OpenJDK][8])

### Common Metaspace leak causes

- Repeated hot deployment
- Old class loaders retained by threads
- `ThreadLocal` values referencing old application classes
- Unbounded proxy generation
- Runtime bytecode generation
- Plugin loaders that are never released

Useful diagnostics:

```bash
jcmd <pid> VM.classloader_stats
jcmd <pid> GC.class_histogram
```

---

## Question 13: How do you troubleshoot `OutOfMemoryError`?

Start with the complete error message.

| Message                                 | Likely area                            |
| --------------------------------------- | -------------------------------------- |
| `Java heap space`                       | Heap objects                           |
| `GC overhead limit exceeded`            | Heap nearly full with little reclaimed |
| `Metaspace`                             | Class metadata                         |
| `Compressed class space`                | Class-pointer metadata area            |
| `Direct buffer memory`                  | Direct/native buffers                  |
| `unable to create native thread`        | Threads, native memory or OS limits    |
| `Requested array size exceeds VM limit` | Invalid or excessively large array     |

Oracle’s troubleshooting guidance emphasizes that OOME can represent heap exhaustion, native-memory failure, insufficient configuration or genuine retention, so the exact detail message is the first diagnostic clue. ([Oracle Docs][6])

### Workflow

1. Preserve the complete exception and stack trace.
2. Check heap, process RSS and container limits.
3. Preserve GC logs and heap dump.
4. Capture JFR and thread information.
5. Check queue, cache and thread counts.
6. Analyze retained objects and GC roots.
7. Determine whether the live set is legitimate or leaked.
8. Change memory limits only after identifying the cause.

### Native-memory investigation

Enable Native Memory Tracking at startup:

```bash
-XX:NativeMemoryTracking=summary
```

Then inspect:

```bash
jcmd <pid> VM.native_memory summary
```

A low Java-heap value does not mean total process memory is low.

---

## Question 14: How do you troubleshoot high CPU usage in JVM applications?

### Step 1: Confirm the responsible process

On Linux:

```bash
top
```

or:

```bash
ps -eo pid,pcpu,pmem,cmd --sort=-pcpu
```

### Step 2: Identify hot threads

```bash
top -H -p <pid>
```

This shows operating-system thread IDs.

### Step 3: Capture several thread dumps

```bash
jcmd <pid> Thread.print -l > dump-1.txt
sleep 5
jcmd <pid> Thread.print -l > dump-2.txt
sleep 5
jcmd <pid> Thread.print -l > dump-3.txt
```

Look for repeated `RUNNABLE` stacks involving:

- Infinite loops
- Excessive parsing
- Serialization
- Regular expressions
- Compression
- Encryption
- Retry loops
- CAS spinning
- Garbage collection
- Logging
- Application hot methods

### Step 4: Record JFR data

```bash
jcmd <pid> JFR.start \
  name=cpu-investigation \
  settings=profile \
  duration=2m \
  filename=/tmp/cpu-investigation.jfr
```

JFR provides JVM and thread CPU events, method samples and hot-method views. Oracle recommends examining `jdk.CPULoad`, `jdk.ThreadCPULoad`, hot methods and call trees when diagnosing CPU-heavy applications. ([Oracle Docs][9])

### Step 5: Distinguish JVM CPU from machine CPU

```text
Machine CPU high, JVM CPU low
→ another process or kernel activity

JVM CPU high
→ application, JIT, GC or JVM activity

One Java thread high
→ likely hot loop or expensive method

Many Java threads high
→ parallel workload, contention or oversubscription
```

### Step 6: Correlate with GC

High CPU may be caused by frequent garbage collection.

Check:

- GC CPU time
- Allocation rate
- Full collections
- Live-set growth
- Object-allocation hotspots

---

## Question 15: What tools do you use for JVM performance analysis?

## Built-in JVM tools

| Tool                | Main purpose                                     |
| ------------------- | ------------------------------------------------ |
| `jcmd`              | General diagnostic command interface             |
| JFR                 | Low-overhead event recording and profiling       |
| JDK Mission Control | Analyze JFR recordings                           |
| `jstack`            | Thread dumps; older/specialized usage            |
| `jmap`              | Heap information and dumps; often prefer `jcmd`  |
| `jstat`             | GC and JVM statistics                            |
| `jinfo`             | JVM flags and configuration                      |
| `javap`             | Inspect bytecode                                 |
| `jhsdb`             | Advanced serviceability and post-mortem analysis |

Oracle recommends `jcmd`, JFR and Mission Control for modern JVM troubleshooting, with `jcmd` generally preferred over older separate utilities for many diagnostics. ([Oracle Docs][10])

### Useful commands

```bash
# List JVM processes
jcmd

# JVM flags
jcmd <pid> VM.flags

# JVM command line
jcmd <pid> VM.command_line

# Thread dump
jcmd <pid> Thread.print -l

# Heap histogram
jcmd <pid> GC.class_histogram

# Heap dump
jcmd <pid> GC.heap_dump /tmp/heap.hprof

# Class-loader statistics
jcmd <pid> VM.classloader_stats

# Native memory
jcmd <pid> VM.native_memory summary

# Start JFR
jcmd <pid> JFR.start \
  name=diagnostic \
  settings=profile \
  duration=2m \
  filename=/tmp/diagnostic.jfr
```

## Heap-analysis tools

- Eclipse Memory Analyzer
- JDK Mission Control
- VisualVM
- Commercial profilers

## Operating-system and container tools

- `top`
- `htop`
- `pidstat`
- `vmstat`
- `iostat`
- `sar`
- `perf`
- `pmap`
- Container CPU and memory metrics
- Kubernetes resource and throttling metrics

## Application observability

- Prometheus and Grafana
- Micrometer
- OpenTelemetry
- APM tools
- Database monitoring
- Connection-pool metrics
- Executor metrics

The strongest JVM diagnosis usually combines:

```text
JVM evidence
+ operating-system evidence
+ application metrics
+ downstream metrics
```

---

# Quick Interview Cheat Sheet

```text
Heap:
Shared objects and arrays; managed by GC.

Stack:
Per thread; method frames and local execution state.

Class loading:
Load → Verify → Prepare → Resolve → Initialize.

ClassLoaders:
Bootstrap → Platform → Application.

GC:
Trace reachable objects from GC roots,
then reclaim or relocate unreachable memory.

Serial:
Small and simple workloads.

G1:
Balanced throughput and pause goals.

ZGC:
Very low pause latency.

Full GC:
Collector-specific; investigate the logged cause.

Memory leak:
Unused objects remain reachable.

Heap dump:
Snapshot of objects and reference relationships.

JIT:
Profiles hot code and compiles it into optimized native code.

Escape analysis:
May eliminate objects, replace fields with scalars,
or remove unnecessary locks.

Metaspace:
Native memory for class metadata.

OOME:
Read the exact detail message before diagnosing.

High CPU:
Find process → find hot thread → repeated dumps → JFR.

Primary tools:
jcmd, JFR, JMC, heap dumps, GC logs and OS metrics.
```

[1]: https://docs.oracle.com/javase/specs/jvms/se25/html/index.html?utm_source=chatgpt.com "The Java® Virtual Machine Specification"
[2]: https://docs.oracle.com/javase/specs/jvms/se25/html/index.html "The Java® Virtual Machine Specification"
[3]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ClassLoader.html "ClassLoader (Java SE 25 & JDK 25)"
[4]: https://docs.oracle.com/en/java/javase/25/gctuning/garbage-first-g1-garbage-collector1.html "Garbage-First (G1) Garbage Collector"
[5]: https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html "The java Command"
[6]: https://docs.oracle.com/en/java/javase/25/troubleshoot/troubleshooting-guide.pdf?utm_source=chatgpt.com "Troubleshooting Guide"
[7]: https://docs.oracle.com/en/java/javase/25/troubleshoot/diagnostic-tools.html "Diagnostic Tools"
[8]: https://openjdk.org/jeps/387 "JEP 387: Elastic Metaspace"
[9]: https://docs.oracle.com/en/java/javase/25/troubleshoot/troubleshoot-performance-issues-using-jfr.html?utm_source=chatgpt.com "4 Troubleshoot Performance Issues Using Flight Recorder"
[10]: https://docs.oracle.com/en/java/javase/25/troubleshoot/diagnostic-tools.html?utm_source=chatgpt.com "2 Diagnostic Tools - Java"
