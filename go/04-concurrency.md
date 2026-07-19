# Go Concurrency: Goroutines, Channels, Context

## 1. Definition

A goroutine is a lightweight, runtime-managed thread (starting at ~2KB stack,
growable). Channels are typed pipes for communicating between goroutines
safely, embodying Go's philosophy: "don't communicate by sharing memory,
share memory by communicating."

## 2. Why it exists

Manually coordinating shared memory access with locks is error-prone.
Channels give a structured way to hand off data and coordinate goroutines
without explicit locking in most common patterns.

## 3. How it works — worker pool with cancellation

```go
func workerPool(ctx context.Context, jobs <-chan Job, results chan<- Result, workers int) {
    var wg sync.WaitGroup
    for i := 0; i < workers; i++ {
        wg.Add(1)
        go func() {
            defer wg.Done()
            for {
                select {
                case job, ok := <-jobs:
                    if !ok {
                        return // channel closed, no more work
                    }
                    results <- process(job)
                case <-ctx.Done():
                    return // caller cancelled or timed out
                }
            }
        }()
    }
    wg.Wait()
    close(results)
}
```

`context.Context` propagates cancellation and deadlines through a call chain —
essential for making sure a client timeout or cancellation actually stops
in-flight work instead of leaking goroutines that run to completion uselessly.

## 4. Code example — goroutine leak

```go
func leaky() <-chan int {
    ch := make(chan int)
    go func() {
        ch <- expensiveComputation() // if nobody ever reads from ch, this
    }()                               // goroutine blocks FOREVER -> leak
    return ch
}
```

Fix: use a buffered channel, a `select` with a `ctx.Done()` case, or ensure
there's always a reader.

## 5. Production use case

Fan-out/fan-in patterns (dispatch N requests to downstream services
concurrently, collect results) and worker pools processing a queue are the
two most common concurrency patterns in Go backend services.

## 6. Common mistakes

- Starting a goroutine with no way to signal it to stop, causing goroutine
  leaks that show up as steadily increasing memory/goroutine count in production.
- Forgetting `sync.WaitGroup.Add()` must happen before starting the goroutine,
  not inside it (race condition otherwise).
- Not passing `context.Context` through call chains, making cancellation
  impossible to propagate.
- Closing a channel from the receiver side (only the sender should close a channel).

## 7. Trade-offs

| Channels | Mutex + shared memory |
|---|---|
| Clearer data flow / ownership | Can be faster for simple shared counters |
| Easy to introduce leaks if misused | Easy to introduce classic race conditions if misused |

## 8. Interview questions

1. What is a goroutine leak, and how do you detect one in production?
2. How does `context.Context` propagate cancellation through a call chain?
3. Why should only the sender close a channel, never the receiver?
4. Walk through how you'd build a worker pool with graceful shutdown.
5. `select` with multiple ready channels — how does Go choose which case runs?

## 9. Short interview answer

"I design goroutines with an explicit way to stop them — usually a context
that gets cancelled or a channel close — because a goroutine with no exit path
is a leak that shows up as ever-increasing goroutine count in production.
Context propagates cancellation and deadlines through the whole call chain, so
a client timeout actually stops in-flight downstream work instead of letting
it run to completion uselessly."

## 10. Related topics

- [Error handling](03-error-handling.md)
- [Runtime & performance](06-runtime-performance.md)
