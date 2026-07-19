# Go Cheat Sheet (final revision)

- Slices share a backing array; append may or may not (depends on capacity).
- Value receiver = copy, no mutation visible to caller; pointer receiver =
  mutates the real thing. Be consistent across a type's methods.
- nil interface gotcha: an interface is nil only if BOTH type and value are nil.
  Return literal `nil`, not a nil-valued typed variable.
- Errors are values; wrap with `%w`, inspect with errors.Is/errors.As.
- panic/recover only for truly unrecoverable situations; always have a
  recovery middleware at the HTTP layer.
- Goroutines need an explicit stop signal (context or channel close) or they leak.
- Only the sender should close a channel.
- context.Context propagates cancellation/deadlines through a call chain.
- GMP scheduler: Goroutines run on Machines (OS threads) via Processors
  (GOMAXPROCS-bound scheduling contexts).
- pprof: CPU profile for hot functions, heap profile for allocation sources,
  goroutine count for leaks.
- Table-driven tests + t.Run for subtests; always run `-race` for concurrent code.
