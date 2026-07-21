# Garbage Collection: Throughput vs Latency

## 1. Definition

Garbage collection automatically reclaims heap memory occupied by objects no
longer reachable from any live reference, so developers don't manually free
memory.

## 2. Why it exists

Manual memory management (as in C/C++) is a major source of bugs (use-after-free,
double-free, leaks). GC trades some CPU/pause overhead for memory safety and
developer simplicity.

## 3. How it works

```text
Minor GC  -> collects the young generation (short-lived objects); fast, frequent
Major/Full GC -> collects the old generation (long-lived objects); slower, rarer

Most objects die young (the "generational hypothesis"), so splitting the heap
into young/old generations lets the collector focus effort where garbage is
most common.
```

Common collectors:
- **G1 (Garbage First)** — default since Java 9, balances throughput and pause
  time, divides the heap into regions.
- **Parallel GC** — maximizes throughput, longer pauses, good for batch jobs.
- **ZGC / Shenandoah** — low-latency collectors, sub-millisecond pauses, good
  for latency-sensitive services with large heaps.

## 4. Code example — measuring before tuning

```bash
java -Xlog:gc*:file=gc.log:time,uptime -jar app.jar
```

Never tune GC flags blindly — capture GC logs under real load first, identify
whether the problem is throughput (total time spent collecting) or latency
(individual pause durations), then choose a collector and tune accordingly.

## 5. Production use case

A p99 latency spike investigation should always check GC pause times — a long
"stop-the-world" pause can look identical to a slow downstream service in
request tracing until you check GC logs.

## 6. Common mistakes

- Tuning GC flags before measuring — "cargo cult" tuning based on blog posts
  instead of your actual allocation profile.
- Treating GC pauses as the default suspect without checking logs — sometimes
  the real cause is thread pool saturation or a slow database query.
- Ignoring allocation rate — reducing unnecessary object creation often helps
  more than any GC flag.

## 7. Trade-offs

| Throughput-focused (Parallel) | Latency-focused (G1, ZGC) |
|---|---|
| Higher total throughput | Shorter, more predictable pauses |
| Good for batch/offline jobs | Good for user-facing request/response services |

## 8. Interview questions

1. Minor vs major/full GC — what's the difference?
2. Why does splitting the heap into generations improve GC efficiency?
3. What's the difference in goals between G1 and ZGC?
4. How would you investigate whether a latency spike is GC-related?

## 9. Short interview answer

"GC reclaims memory from objects no longer reachable, using a generational
approach since most objects die young. I always measure with GC logs before
tuning — a p99 spike investigation should check GC pause times alongside
thread pool saturation and downstream latency before assuming any one cause."

## 10. Related topics

- [JVM memory](memory-model.md)


JVM and Memory
6. Your app ran fine for 30 days. Crashed with OutOfMemoryError. No code changes. What happened?
7. GC is running every 30 seconds. Application pauses for 2 seconds each time. How do you fix it?
8. When would you choose G1GC over ZGC in production?