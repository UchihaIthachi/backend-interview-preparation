# Java Cheat Sheet (final revision)

- Pass-by-value always; objects pass a copy of the reference.
- Strings immutable -> use StringBuilder for loops, not `+=`.
- equals()/hashCode() must be overridden together; never mutate a HashMap key
  after insertion.
- Prefer composition over deep inheritance; interface = pure contract,
  abstract class = contract + shared implementation.
- PECS: `? extends T` for producers (read), `? super T` for consumers (write).
- volatile = visibility only, not atomicity. Use AtomicInteger or synchronized
  for compound operations like increment.
- ExecutorService: size CPU-bound pools near core count, I/O-bound pools much
  higher (or use virtual threads).
- Virtual threads help I/O-bound concurrency, not CPU-bound work.
- G1 balances throughput/latency; ZGC/Shenandoah for very low pause times;
  Parallel GC for max throughput batch jobs.
- Constructor injection > field injection: testable, immutable, fails fast on
  circular deps.
- `@Transactional` is a proxy — self-invocation within the same class bypasses it.
- N+1: fix with fetch join, entity graph, or DTO projection.
- JWT is a token format, not an auth protocol; OAuth 2.0 = authorization,
  OIDC = identity layer on top of OAuth.
