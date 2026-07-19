# Go Backend: net/http, Middleware, database/sql

## 1. Definition

Go's standard library provides `net/http` for HTTP servers/clients and
`database/sql` for a database-agnostic SQL interface backed by driver
implementations (e.g., `pgx`, `lib/pq` for Postgres).

## 2. Why it exists

Go deliberately keeps a capable standard library so many backend services
need no framework at all — middleware is just functions wrapping
`http.Handler`, with no magic.

## 3. How it works — middleware chaining

```go
type Middleware func(http.Handler) http.Handler

func Logging(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        start := time.Now()
        next.ServeHTTP(w, r)
        log.Printf("%s %s took %v", r.Method, r.URL.Path, time.Since(start))
    })
}

func Chain(h http.Handler, mws ...Middleware) http.Handler {
    for i := len(mws) - 1; i >= 0; i-- {
        h = mws[i](h)
    }
    return h
}

handler := Chain(finalHandler, Logging, Auth, Recovery)
```

## 4. Code example — graceful shutdown

```go
srv := &http.Server{Addr: ":8080", Handler: router}

go func() {
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatal(err)
    }
}()

stop := make(chan os.Signal, 1)
signal.Notify(stop, os.Interrupt, syscall.SIGTERM)
<-stop

ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
defer cancel()
srv.Shutdown(ctx) // stops accepting new connections, waits for in-flight ones
```

## 5. Production use case

Graceful shutdown matters in containerized/Kubernetes environments — when a
pod receives SIGTERM before being killed, the server needs to stop accepting
new connections but let in-flight requests finish, or requests get dropped
mid-flight during deploys.

## 6. Common mistakes

- Not closing `sql.Rows` after a query, leaking database connections from the pool.
- Forgetting to set connection pool limits (`SetMaxOpenConns`,
  `SetMaxIdleConns`), letting the pool grow unbounded under load.
- Not implementing graceful shutdown, causing dropped requests during every deploy.
- Using global mutable state for request-scoped data instead of passing
  through `context.Context` or explicit parameters.

## 7. Trade-offs

Using the standard library directly (vs. a framework like Gin/Echo) means
more boilerplate for routing but zero framework lock-in and full clarity
about what's actually happening on each request.

## 8. Interview questions

1. How would you implement middleware chaining in idiomatic Go?
2. Why is graceful shutdown important, especially in containerized deployments?
3. What connection pool settings matter for `database/sql`, and why?
4. How do you avoid leaking database connections when a query fails partway through?

## 9. Short interview answer

"I build middleware as plain functions wrapping http.Handler and chain them
explicitly, since Go has no framework magic here. For services running in
containers, graceful shutdown is non-negotiable — on SIGTERM, stop accepting
new connections but let in-flight requests finish within a bounded timeout, or
every deploy drops live requests."

## 10. Related topics

- [Concurrency](04-concurrency.md)
- [Runtime & performance](06-runtime-performance.md)
