# SQL Cheat Sheet (final revision)

- ACID: Atomicity, Consistency, Isolation, Durability.
- Anomalies (weakest to strongest isolation prevents more): dirty read,
  non-repeatable read, phantom read, lost update.
- Isolation levels: READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE.
- Composite index column order matters — leftmost prefix rule.
- Adding an index doesn't automatically help; verify with EXPLAIN.
- Optimistic locking = version column, conflict caught at commit. Pessimistic
  locking = SELECT ... FOR UPDATE, blocks immediately.
- Indexes speed reads, slow writes — don't over-index.
