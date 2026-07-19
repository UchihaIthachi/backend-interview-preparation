# Go Runtime: Scheduler, GC, pprof

## 1. Definition

Go's runtime includes an M:N scheduler (multiplexing many goroutines onto a
smaller number of OS threads) and a concurrent, low-latency garbage collector
tuned for short pause times.

## 2. Why it exists

Goroutines need to be cheap to create and schedule for Go's concurrency model
to be practical at scale — a 1:1 OS thread mapping (like older threading
models) wouldn't scale to the tens of thousands of goroutines common in Go
services.

## 3. How it works — GMP model

```text
G (Goroutine) — the unit of work
M (Machine)   — an OS thread
P (Processor) — a scheduling context; GOMAXPROCS controls how many exist

Each P has a local run queue of goroutines. An M must hold a P to run
goroutines. When a goroutine blocks on a syscall, the M can be detached and a
new M can pick up the P to keep other goroutines running.
```

## 4. Code example — profiling with pprof

```go
import _ "net/http/pprof"

go func() {
    log.Println(http.ListenAndServe("localhost:6060", nil))
}()
```

```bash
go tool pprof http://localhost:6060/debug/pprof/profile?seconds=30
go tool pprof http://localhost:6060/debug/pprof/heap
```

CPU profiles reveal where time is spent; heap profiles reveal what's
allocating memory — both essential before "optimizing" blindly.

## 5. Production use case

Diagnosing a Go service with high CPU or growing memory almost always starts
with `pprof` — capturing a CPU profile under real load to find hot functions,
or a heap profile to find what's allocating and not being freed (often
unbounded caches or goroutine leaks holding references).

## 6. Common mistakes

- Guessing at performance bottlenecks instead of profiling first.
- Setting `GOMAXPROCS` manually without understanding it already defaults to
  the number of available CPUs.
- Ignoring goroutine count metrics — a steadily increasing goroutine count is
  usually a leak, visible via `runtime.NumGoroutine()` or the pprof goroutine profile.

## 7. Trade-offs

Go's GC prioritizes low pause times over maximum throughput — a deliberate
choice suited to network services where responsiveness matters more than raw
batch throughput.

## 8. Interview questions

1. Explain the GMP scheduling model at a high level.
2. How would you diagnose a Go service using too much CPU?
3. How would you diagnose a Go service with steadily growing memory?
4. What's the difference between a CPU profile and a heap profile in pprof?

## 9. Short interview answer

"Go's scheduler multiplexes goroutines (G) onto OS threads (M) through
logical processors (P), which is what makes goroutines cheap enough to spawn
by the thousands. When I need to diagnose performance, I reach for pprof
first — a CPU profile to find hot functions, a heap profile to find what's
allocating, and goroutine counts to catch leaks — rather than guessing."

## 10. Related topics

- [Concurrency](04-concurrency.md)
