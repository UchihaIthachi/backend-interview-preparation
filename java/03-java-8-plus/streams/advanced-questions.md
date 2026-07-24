# Advanced Questions — Parallel Streams

## Question 1: Why can parallel streams reduce performance?

A parallel stream divides a stream into smaller tasks, processes those tasks concurrently, and combines the partial results.

```java
long total = numbers.parallelStream()
        .mapToLong(this::calculate)
        .sum();
```

Parallel execution introduces overhead. When that overhead is greater than the work saved through concurrency, a parallel stream performs worse than a sequential stream.

> Parallel streams provide potential parallelism, not guaranteed performance improvement.

---

## 1. Task Splitting and Coordination Overhead

Before processing can begin, the stream source must be divided into smaller tasks.

After processing, partial results may need to be combined.

```text
Input data
   ↓
Split into tasks
   ↓
Schedule tasks
   ↓
Process in parallel
   ↓
Combine partial results
   ↓
Return final result
```

For a small collection or a cheap operation, the cost of splitting, scheduling, synchronization, and combining can exceed the actual processing cost.

### Poor candidate

```java
int total = numbers.parallelStream()
        .mapToInt(number -> number * 2)
        .sum();
```

Multiplication is extremely cheap. For a small or medium-sized collection, sequential processing will often be faster.

---

## 2. Small Input Size

Parallelism is usually unsuitable when the collection contains only a small number of elements.

```java
List<Integer> numbers = List.of(1, 2, 3, 4);

int total = numbers.parallelStream()
        .mapToInt(Integer::intValue)
        .sum();
```

The work is too small to compensate for parallel execution overhead.

Parallel streams become more plausible when:

- The input is large.
- Each element requires significant computation.
- The work is independent.
- The source can be divided efficiently.

There is no universal minimum collection size. It depends on the cost per element, hardware, source structure, and surrounding workload.

---

## 3. Cheap Per-Element Operations

Parallel execution is more useful when each element requires substantial CPU work.

### Cheap operation

```java
List<String> names = users.parallelStream()
        .map(User::name)
        .toList();
```

Property access is too inexpensive to benefit meaningfully in many cases.

### More suitable CPU-intensive operation

```java
long result = values.parallelStream()
        .mapToLong(this::performExpensiveCalculation)
        .sum();
```

Even then, performance must be measured.

---

## 4. Shared `ForkJoinPool.commonPool()`

Parallel streams commonly execute using the shared `ForkJoinPool.commonPool()`.

```java
values.parallelStream()
        .map(this::process)
        .toList();
```

The same pool may also be used by:

- Other parallel streams
- `CompletableFuture` operations without an explicit executor
- Framework or application tasks using the common pool

Multiple unrelated workloads can compete for the same worker threads.

```text
Parallel stream A ─┐
Parallel stream B ─┼──> Common ForkJoinPool
CompletableFuture ─┘
```

This may cause:

- Thread starvation
- Increased latency
- Reduced throughput
- Unpredictable interference between requests
- Poor production isolation

A parallel stream inside every incoming web request can multiply the contention.

---

## 5. Blocking I/O

Parallel streams are generally a poor choice for blocking operations such as:

- Database queries
- HTTP calls
- File-system access
- Remote service requests
- Waiting for locks
- Sleeping

Problematic example:

```java
List<UserProfile> profiles = userIds.parallelStream()
        .map(profileClient::fetchProfile)
        .toList();
```

Each operation may block while waiting for the network. Blocked common-pool workers cannot process other tasks efficiently.

The result may also overload downstream resources:

- HTTP connection pools
- Database connection pools
- External APIs
- Rate-limited services

For controlled I/O concurrency, prefer an explicit bounded executor or an asynchronous API.

```java
ExecutorService executor = Executors.newFixedThreadPool(20);

List<CompletableFuture<UserProfile>> futures = userIds.stream()
        .map(userId -> CompletableFuture.supplyAsync(
                () -> profileClient.fetchProfile(userId),
                executor
        ))
        .toList();
```

This provides clearer control over concurrency, although timeout, cancellation, failure handling, and executor shutdown still need to be designed.

---

## 6. Poorly Splittable Sources

Parallel streams depend on the source’s `Spliterator` to divide data efficiently.

Some structures split more efficiently than others.

| Source                | Splitting quality      |
| --------------------- | ---------------------- |
| `ArrayList`           | Usually good           |
| Arrays                | Usually good           |
| Numeric ranges        | Usually good           |
| `HashMap`             | Generally reasonable   |
| `HashSet`             | Generally reasonable   |
| `LinkedList`          | Usually less efficient |
| Iterator-based source | May be poor            |
| I/O stream            | Often unsuitable       |

`ArrayList` can divide its index range efficiently:

```text
0 ─────────────── n
        ↓
0 ─── n/2    n/2 ─── n
```

A linked structure may need traversal to find split points, adding overhead and reducing parallel efficiency.

---

## 7. Uneven Work Distribution

Parallel processing performs best when tasks contain approximately equal amounts of work.

Consider:

```java
results = inputs.parallelStream()
        .map(this::process)
        .toList();
```

Suppose most elements take 1 millisecond, but one element takes 10 seconds.

```text
Worker 1: 1 ms + 1 ms + 1 ms
Worker 2: 1 ms + 1 ms + 1 ms
Worker 3: 1 ms + 1 ms + 1 ms
Worker 4: 10 seconds
```

The entire operation must wait for the slow task. This is called load imbalance or work skew.

Fork/join work stealing can reduce some imbalance, but it cannot eliminate a single extremely expensive task.

---

## 8. Ordering Requirements

Preserving encounter order may reduce the benefits of parallelism.

This operation does not guarantee ordered output:

```java
numbers.parallelStream()
        .forEach(System.out::println);
```

This preserves encounter order:

```java
numbers.parallelStream()
        .forEachOrdered(System.out::println);
```

However, `forEachOrdered()` may require additional coordination and can reduce parallel execution benefits.

Other ordering-sensitive operations include:

- `findFirst()`
- Ordered `limit()`
- Ordered `skip()`
- Sorting
- Producing results in encounter order

When order is unnecessary, unordered processing may offer more opportunities for parallel execution:

```java
values.parallelStream()
        .unordered()
        .distinct()
        .toList();
```

Use this only when removing encounter-order guarantees is semantically correct.

---

## 9. Stateful Intermediate Operations

Some stream operations must retain information about previously processed elements.

Examples include:

```java
sorted()
distinct()
limit()
skip()
```

### Sorting

```java
List<User> sortedUsers = users.parallelStream()
        .sorted(Comparator.comparing(User::name))
        .toList();
```

Sorting requires collecting and coordinating elements. Parallel sorting can help for sufficiently large inputs, but it also introduces merging and memory overhead.

### `distinct()`

```java
List<String> uniqueNames = names.parallelStream()
        .distinct()
        .toList();
```

`distinct()` must track previously observed values. For ordered parallel streams, maintaining both uniqueness and order can require expensive coordination.

---

## 10. Shared Mutable State

Parallel streams become unsafe or slow when multiple workers modify the same object.

Incorrect:

```java
List<Integer> result = new ArrayList<>();

numbers.parallelStream()
        .filter(number -> number % 2 == 0)
        .forEach(result::add);
```

`ArrayList` is not thread-safe. The result may contain missing elements, incorrect data, or internal corruption.

Adding synchronization may fix correctness but reduce performance:

```java
List<Integer> result =
        Collections.synchronizedList(new ArrayList<>());

numbers.parallelStream()
        .filter(number -> number % 2 == 0)
        .forEach(result::add);
```

All threads may contend for the same list lock.

Prefer a collector:

```java
List<Integer> result = numbers.parallelStream()
        .filter(number -> number % 2 == 0)
        .toList();
```

The stream framework creates and combines intermediate result containers appropriately.

---

## 11. Contention on Shared Resources

Even thread-safe operations can become bottlenecks.

```java
AtomicLong total = new AtomicLong();

numbers.parallelStream()
        .forEach(total::addAndGet);
```

Each worker contends for the same atomic variable.

Prefer a reduction:

```java
long total = numbers.parallelStream()
        .mapToLong(Long::longValue)
        .sum();
```

Similarly, logging from every element can serialize work:

```java
values.parallelStream()
        .forEach(value -> log.info("Processing {}", value));
```

The logging framework, output destination, or appender may become the bottleneck.

---

## 12. Memory Allocation and Garbage Collection

Parallel streams may create:

- More temporary tasks
- More partial result containers
- More intermediate objects
- Additional merge structures
- Increased allocation pressure

This can increase:

- Heap usage
- Garbage-collection frequency
- CPU consumption
- Memory-bandwidth pressure

For example, boxing can add considerable overhead:

```java
int total = IntStream.range(0, 1_000_000)
        .parallel()
        .boxed()
        .map(value -> value * 2)
        .reduce(0, Integer::sum);
```

Prefer primitive streams when working with numeric values:

```java
long total = IntStream.range(0, 1_000_000)
        .parallel()
        .map(value -> value * 2)
        .asLongStream()
        .sum();
```

---

## 13. CPU Core Competition

Parallel streams use additional CPU workers. They do not create additional physical CPU capacity.

When the application server is already busy, parallel work may compete with:

- Request-processing threads
- Garbage-collection threads
- Database drivers
- Serialization
- Encryption
- Other background jobs

A single operation may complete faster while total application throughput becomes worse.

For example:

```text
One request:
parallel stream may reduce response time

Hundreds of concurrent requests:
every request creates parallel work
→ CPU oversubscription
→ context switching
→ higher overall latency
```

Always evaluate system-wide throughput and tail latency, not only the duration of one isolated method.

---

## 14. Nested Parallelism

Nested parallel streams usually provide little benefit and may create severe pool contention.

```java
departments.parallelStream()
        .map(department ->
                department.employees()
                        .parallelStream()
                        .map(this::calculate)
                        .toList()
        )
        .toList();
```

Both levels commonly use the same shared common pool.

Prefer parallelizing at one appropriate level:

```java
departments.stream()
        .flatMap(department ->
                department.employees().stream()
        )
        .parallel()
        .map(this::calculate)
        .toList();
```

Even this should be benchmarked before production use.

---

## 15. Side Effects and Thread Safety

A parallel stream can expose thread-safety problems in code that worked sequentially.

Problematic:

```java
SimpleDateFormat formatter =
        new SimpleDateFormat("yyyy-MM-dd");

List<String> dates = values.parallelStream()
        .map(formatter::format)
        .toList();
```

`SimpleDateFormat` is not thread-safe.

Prefer immutable, thread-safe APIs:

```java
DateTimeFormatter formatter =
        DateTimeFormatter.ISO_LOCAL_DATE;

List<String> dates = values.parallelStream()
        .map(formatter::format)
        .toList();
```

All functions used by a parallel stream should be safe for concurrent execution.

---

# Sequential vs Parallel Example

## Sequential version

```java
long total = transactions.stream()
        .filter(Transaction::completed)
        .mapToLong(Transaction::amountInCents)
        .sum();
```

## Parallel version

```java
long total = transactions.parallelStream()
        .filter(Transaction::completed)
        .mapToLong(Transaction::amountInCents)
        .sum();
```

The parallel version may be faster only when:

- The collection is large enough.
- The filtering and mapping work is sufficiently expensive.
- The data source splits efficiently.
- The operations are stateless.
- CPU cores are available.
- Pool contention is low.
- Benchmarking demonstrates improvement.

For simple field access and addition, parallel overhead may dominate.

---

# When Parallel Streams Are Good Candidates

Parallel streams may be appropriate when all or most of these conditions are true:

- Large in-memory data set
- CPU-intensive processing
- Independent operations
- Stateless functions
- Efficiently splittable source
- Associative reduction
- No blocking I/O
- Minimal ordering requirements
- No shared mutable state
- Available CPU capacity
- Performance verified through benchmarking

Example:

```java
double result = largeDataSet.parallelStream()
        .mapToDouble(this::cpuIntensiveCalculation)
        .sum();
```

---

# When to Avoid Parallel Streams

Avoid or carefully evaluate them for:

- Small collections
- Trivial transformations
- Database calls
- HTTP requests
- File I/O
- Operations with side effects
- Shared mutable state
- Strict encounter ordering
- Nested parallelism
- Latency-sensitive shared servers
- Code requiring custom thread limits
- Workloads using the common pool elsewhere

---

# How to Measure Correctly

Do not benchmark streams using a simple single-run timer:

```java
long start = System.nanoTime();
// Run operation once
long duration = System.nanoTime() - start;
```

Results can be distorted by:

- JVM warm-up
- JIT compilation
- Dead-code elimination
- Garbage collection
- CPU frequency changes
- Background processes
- Insufficient repetitions

For reliable Java microbenchmarks, use a proper benchmarking harness and test:

- Sequential execution
- Parallel execution
- Different input sizes
- Different operation costs
- Realistic server contention
- Throughput and latency
- GC allocation rate

Production monitoring should also include:

- CPU usage
- Common-pool saturation
- Request latency
- Tail latency
- Throughput
- Downstream resource usage
- Connection-pool pressure

---

# Common Mistakes

## 1. Assuming parallel means faster

Incorrect:

> A parallel stream uses more threads, so it must be faster.

Correct:

> Parallel execution adds overhead and helps only when the workload can offset that overhead.

---

## 2. Using parallel streams for remote calls

```java
users.parallelStream()
        .map(remoteClient::loadProfile)
        .toList();
```

This provides poor concurrency control and can overload the remote dependency.

---

## 3. Mutating a synchronized collection

```java
List<Result> results =
        Collections.synchronizedList(new ArrayList<>());

values.parallelStream()
        .forEach(value ->
                results.add(process(value))
        );
```

Although list corruption may be prevented, lock contention can remove much of the performance benefit.

---

## 4. Ignoring common-pool contention

A method may look isolated but share workers with unrelated application tasks.

---

## 5. Running a parallel stream inside every request

```java
@GetMapping("/report")
public Report generateReport() {
    return records.parallelStream()
            .map(this::process)
            .collect(reportCollector());
}
```

Under high request concurrency, this can oversubscribe the CPU and increase overall latency.

---

## 6. Using a non-associative reduction

Parallel reductions require an associative operation.

Problematic:

```java
int result = numbers.parallelStream()
        .reduce(0, (first, second) -> first - second);
```

Subtraction is not associative:

```text
(10 - 5) - 2 ≠ 10 - (5 - 2)
```

The result may differ based on grouping.

Suitable:

```java
int result = numbers.parallelStream()
        .reduce(0, Integer::sum);
```

Addition is associative for mathematical integers, although fixed-width overflow and floating-point behavior require their own considerations.

---

# Interview Questions

## Question 1: Does a parallel stream always create one thread per element?

No. Elements are divided into tasks, and the tasks are executed by a pool of worker threads. It does not normally create one platform thread for every element.

## Question 2: Which thread pool does a parallel stream use?

Parallel streams commonly use `ForkJoinPool.commonPool()`, unless execution is arranged through more specialized mechanisms. Application code should not rely on parallel streams for precise executor control.

## Question 3: Are parallel streams suitable for database calls?

Usually not. Database calls are blocking and constrained by the connection pool and database capacity. An explicit bounded concurrency design provides better control.

## Question 4: Why can `forEachOrdered()` reduce performance?

It requires results to respect encounter order, which adds coordination and limits how freely workers can publish their results.

## Question 5: Why must reduction operations be associative?

Parallel execution can group and combine elements in different ways. An associative operation produces the same logical result regardless of grouping.

## Question 6: When have you used a parallel stream in production?

A strong answer is:

> I use parallel streams only for measured CPU-bound transformations over large in-memory data sets. For request handling, database access, or remote calls, I prefer explicit bounded concurrency because it gives clearer control over resources and failure handling.

---

# Short Interview Answer

> Parallel streams can reduce performance because they introduce task-splitting, scheduling, synchronization, and result-merging overhead. They commonly use the shared Fork/Join common pool, so unrelated workloads may contend for the same workers. They are especially unsuitable for small collections, cheap operations, blocking I/O, shared mutable state, strict ordering, or poorly splittable sources. I use them only for large, independent, CPU-intensive workloads after benchmarking both method-level speed and total application throughput.
