# Concurrency Cheat Sheet (final revision — cross-language)

## Java
- volatile = visibility, not atomicity.
- AtomicInteger for lock-free single-variable updates; synchronized/ReentrantLock
  for multi-step critical sections.
- happens-before relationships: synchronized unlock/lock, volatile write/read,
  thread start, thread join.
- Deadlock: multiple threads waiting on each other's locks in a cycle — diagnose
  with thread dumps.

## Go
- Prefer channels for coordination ("share memory by communicating"), mutexes
  for simple shared counters.
- context.Context for cancellation/timeouts across a call chain.
- Goroutine leaks: no exit path (no context check, no channel close) -> goroutine
  blocks forever.

## Cross-cutting principles
- Never guess at concurrency bugs — reproduce under real load / stress tests.
- Race conditions and deadlocks are different bugs: races corrupt data,
  deadlocks freeze progress.
- Always have a way to signal shutdown/cancellation into any long-running
  concurrent task.
