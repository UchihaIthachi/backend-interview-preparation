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


## Optimistic Locking vs. Pessimistic Locking

🚀 Optimistic Locking vs. Pessimistic Locking — How SQL Databases Handle Concurrent Updates 
 
Have you ever wondered what happens when two users try to update the same database row at exactly the same time? 
Without proper concurrency control, one update could overwrite the other, leading to inconsistent or incorrect data. 
SQL databases typically solve this problem using two concurrency control strategies: Optimistic Locking and Pessimistic Locking. 
The infographic below illustrates how each approach works. 
 
✅ Optimistic Locking (Version-Based) 
Instead of locking the row, each transaction reads the current version number. 
When updating, the database verifies that the version hasn't changed: 
UPDATE accounts 
SET balance = ?, version = version + 1 
WHERE id = ? AND version = 5; 
If another transaction has already modified the row, the version check fails immediately. 
Key characteristics: 
✔ No row-level lock 
✔ Better throughput for read-heavy systems 
✔ Conflicts detected during UPDATE 
✔ Application retries on failure 
Think of it as: "Try first. If someone already changed the data, fail fast and retry." 
 
🔒 Pessimistic Locking (Row-Level Lock) 
Before modifying data, the transaction explicitly locks the row: 
SELECT * FROM accounts 
WHERE id = 101 
FOR UPDATE; 
While one transaction holds the lock: 
Other transactions requesting the same row wait in a queue 
They execute only after the lock is released 
No version conflict occurs because updates happen sequentially 
Key characteristics: 
✔ Row locked immediately 
✔ Other requests wait instead of failing 
✔ Strong consistency for highly contended data 
✔ Lower concurrency but predictable execution 
Think of it as: "Wait your turn before making changes." 
 
📌 When should you use each? 
Choose Optimistic Locking when: 
Conflicts are rare 
Your application is read-heavy 
High throughput is important 
Retries are acceptable 
Examples: 
E-commerce product catalog 
User profiles 
Content management systems 
Choose Pessimistic Locking when: 
Data integrity is critical 
Many users update the same records 
Lost updates are unacceptable 
Examples: 
Banking transactions 
Digital wallets 
Inventory management 
Ticket booking systems 
 
💡 Key Takeaway 
There isn't a universally "better" approach. 
The right choice depends on your application's contention level, consistency requirements, throughput goals, and user experience. 
Understanding these trade-offs is essential for designing scalable, reliable, and high-performance backend systems.

𝗧𝗿𝗮𝗻𝘀𝗮𝗰𝘁𝗶𝗼𝗻𝘀 & 𝗗𝗮𝘁𝗮
11. ACID - A transaction either fully commits or fully rolls back, nothing halfway
12. Isolation Level - How much one transaction can see another's uncommitted changes
13. Eventual Consistency - All nodes agree on the value, just not at the same instant
14. N+1 Query - One query for the list, then one more per row, performance dies
15. Connection Pool - Reusable DB connections so you do not open one per request