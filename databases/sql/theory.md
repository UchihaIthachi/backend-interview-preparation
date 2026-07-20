# SQL: Transactions, Isolation Levels, Indexing

## 1. Definition

ACID transactions guarantee atomicity, consistency, isolation, and durability.
Indexes are auxiliary data structures (typically B-trees) that speed up
lookups at the cost of extra storage and slower writes.

## 2. Why it exists

Without transactions, concurrent operations on shared data corrupt each other
(lost updates, inconsistent reads). Without indexes, every query is a full
table scan — fine for small tables, unusable at scale.

## 3. How it works — isolation anomalies

```text
Dirty read:        reading uncommitted data from another transaction
Non-repeatable read: re-reading the same row within a transaction gives a
                     different value because another transaction committed
                     a change in between
Phantom read:       re-running the same query returns a different SET of rows
                     because another transaction inserted/deleted matching rows
Lost update:        two transactions read-modify-write the same row; one
                     transaction's update is silently overwritten by the other
```

Isolation levels (weakest to strongest): READ UNCOMMITTED, READ COMMITTED,
REPEATABLE READ, SERIALIZABLE — each level prevents more of the above
anomalies at the cost of more locking/blocking (or transaction abort/retry
under optimistic concurrency control).

## 4. Code example — composite index ordering matters

```sql
CREATE INDEX idx_order_customer_status ON orders(customer_id, status);

-- Uses the index efficiently (leftmost prefix):
SELECT * FROM orders WHERE customer_id = 123;
SELECT * FROM orders WHERE customer_id = 123 AND status = 'PENDING';

-- Does NOT use this index efficiently (skips the leftmost column):
SELECT * FROM orders WHERE status = 'PENDING';
```

## 5. Production use case

Debugging a slow query starts with `EXPLAIN`/`EXPLAIN ANALYZE` to see whether
the planner is doing a sequential scan when it should be using an index, and
whether row estimates match reality (a large mismatch usually means stale
table statistics).

## 6. Common mistakes

- Adding an index and assuming it automatically helps — indexes slow down
  writes and only help if the query planner actually chooses to use them.
- Ignoring index column order for composite indexes.
- Choosing SERIALIZABLE by default "to be safe," paying a large concurrency
  cost when READ COMMITTED (with careful application logic) would suffice.
- Confusing optimistic locking (version column, conflict detected at commit)
  with pessimistic locking (`SELECT ... FOR UPDATE`, blocks immediately).

## 7. Trade-offs

| More indexes | Fewer indexes |
|---|---|
| Faster reads | Faster writes |
| More storage | Less storage |

## 8. Interview questions

1. Define atomicity, consistency, isolation, and durability with a concrete example each.
2. What's the difference between a dirty read, non-repeatable read, and phantom read?
3. Why does composite index column order matter?
4. Optimistic vs pessimistic locking — when do you use each?
5. Why might an index exist but still not be used by the query planner?

## 9. Short interview answer

"I think about isolation levels in terms of which anomaly they prevent — dirty
reads, non-repeatable reads, phantom reads, lost updates — and pick the
weakest level that still prevents the anomalies my use case actually cares
about, since stronger isolation costs concurrency. For indexes, I always
verify with EXPLAIN that a new index is actually being used before assuming
it helped, since column order in a composite index determines which queries
can use it at all."

## 10. Related topics

- [JPA / N+1 problem](../java/06-spring/spring-data-jpa.md)
