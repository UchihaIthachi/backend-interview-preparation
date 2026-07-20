# Go Error Handling: Wrapping, Panic and Recover

## 1. Definition

Go treats errors as ordinary values (the `error` interface), returned as the
last return value by convention, rather than using exceptions for control
flow. `panic`/`recover` exist for truly exceptional, unrecoverable situations.

## 2. Why it exists

Explicit error returns force every call site to consciously handle or
propagate failure, making failure paths visible in the code rather than
hidden in an invisible exception path.

## 3. How it works — wrapping with %w

```go
func loadConfig(path string) (*Config, error) {
    data, err := os.ReadFile(path)
    if err != nil {
        return nil, fmt.Errorf("loading config from %s: %w", path, err)
    }
    // ...
}

// Callers can unwrap to check the original cause:
if errors.Is(err, os.ErrNotExist) {
    // handle missing file specifically
}

var pathErr *fs.PathError
if errors.As(err, &pathErr) {
    // handle a specific error type
}
```

## 4. Code example — panic/recover for truly unexpected failures

```go
func safeHandler(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        defer func() {
            if rec := recover(); rec != nil {
                log.Printf("recovered from panic: %v", rec)
                http.Error(w, "internal server error", http.StatusInternalServerError)
            }
        }()
        next(w, r)
    }
}
```

## 5. Production use case

A recovery middleware like the one above is standard in Go HTTP servers — it
prevents one panicking request handler goroutine from crashing the entire
server process, converting it into a clean 500 response instead.

## 6. Common mistakes

- Using panic for expected, recoverable error conditions (e.g., validation
  failures) instead of returning an error.
- Swallowing errors with `_ = someCall()` without a deliberate reason.
- Wrapping errors without `%w`, losing the ability for callers to use
  `errors.Is`/`errors.As` to inspect the cause.
- Forgetting that `recover()` only works when called directly inside a
  deferred function.

## 7. Trade-offs

| Explicit error returns | panic/recover |
|---|---|
| Forces handling at every call site | Only for truly unrecoverable/programmer-error cases |
| More verbose | Simpler for currently-unhandleable states, but easy to misuse |

## 8. Interview questions

1. Why does Go prefer explicit error returns over exceptions?
2. What does `%w` do differently from `%v` when wrapping an error?
3. `errors.Is` vs `errors.As` — what's the difference?
4. When is it appropriate to use `panic` instead of returning an error?
5. Why does a single recovery middleware matter so much in a Go HTTP server?

## 9. Short interview answer

"Go treats errors as regular values so failure handling stays visible at every
call site instead of hiding behind an exception path. I wrap errors with %w so
callers can still inspect the root cause with errors.Is or errors.As, and
reserve panic/recover for truly unexpected situations — with a recovery
middleware at the HTTP layer so one panicking handler can't take down the
whole server."

## 10. Related topics

- [Concurrency & goroutine leaks](04-concurrency.md)
