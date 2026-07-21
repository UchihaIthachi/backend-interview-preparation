# Scenario-based Questions

**Scenario/Question:** Transaction Failure A multi-step transaction fails midway. How do you ensure rollback? How would you handle distributed transactions?

**Scenario/Question:** Bulk Processing You need to process millions of records. How do you design it efficiently?

**Scenario:** You need to process bulk uploads (e.g., CSV with 1M records).
**Questions:** Would you process synchronously or asynchronously? How would you design retry and failure handling? Would you use event-driven architecture?

**Scenario/Question:** How will you ensure data consistency across services?

**Scenario/Question:** How will you handle distributed transactions?

**Scenario/Question:** How will you implement idempotency in distributed systems?

**Scenario/Question:** How will you handle distributed transactions without 2PC?

**Scenario/Question:** How will you implement saga pattern?

**Scenario/Question:** How will you design NoSQL schema?

**Scenario/Question:** How will you handle transactions in distributed DB?

**Scenario/Question:** How will you handle large-scale data processing?



📈 Scale & Data Processing
 
Design a rate limiting solution for an API serving millions of users.
How would you process a 5 GB CSV file without causing an OutOfMemoryError?
A production bug only appears under heavy traffic and cannot be reproduced locally. How do you investigate it?

SCALE AND DATA:
23. Rate limiting for an API hit by millions of users. Design it.
24. Process a 5 GB CSV without an OutOfMemoryError. How?
25. A bug only fires under heavy traffic, never locally. How do you catch it?

# Advanced Interview Questions & Answers

**Scenario/Question:** 1. A customer transfers ₹50,000 from Savings to Current account. Debit succeeds, but credit fails due to a database timeout. How would you ensure the customer's money is never lost or duplicated?

**Answer:** Use distributed transactions (Saga pattern). If the credit fails, initiate a compensating transaction to reverse the debit. Ensure all operations are idempotent and track state transitions (PENDING, SUCCESS, FAILED) in a database.

**Scenario/Question:** 2. Two customers simultaneously transfer money to the same beneficiary account. One transaction overwrites the other's balance update. How would you prevent this?

**Answer:** Use pessimistic locking (`SELECT FOR UPDATE`) or optimistic locking (version numbers) on the beneficiary account row during the balance update to ensure atomic, sequential updates.

**Scenario/Question:** 3. Your payment service receives the same UPI callback five times because the payment gateway retries. How would you ensure the customer's account is credited only once?

**Answer:** Implement idempotency using a unique transaction ID provided by the gateway. Store processed transaction IDs in a database or cache (e.g., Redis). If a duplicate ID is received, return the previous successful response without processing it again.

**Scenario/Question:** 4. A transaction times out on the customer's mobile app, but the backend successfully debits the account. How should the system respond when the customer retries?

**Answer:** The mobile app should include an idempotency key with the retry. The backend checks this key, sees the debit already succeeded, and returns a success response without re-debiting.

**Scenario/Question:** 5. During peak salary credit hours, thousands of transactions are processed concurrently. How would you guarantee account balances remain consistent?

**Answer:** Batch the updates, use database partitioning, and apply row-level locking. Alternatively, use an event-driven architecture (like Kafka) to serialize updates to specific accounts through a single partition/consumer to avoid lock contention.

**Scenario/Question:** 6. A customer initiates an NEFT transfer just before the banking cutoff time. The transaction reaches the processing queue after the cutoff. Should it be processed or deferred? How would you design this logic?

**Answer:** The logic should evaluate the timestamp of when the request was *received* at the API gateway, not when it was dequeued. If received before cutoff, process it; if after, defer it to the next cycle.

**Scenario/Question:** 7. A transaction is marked "SUCCESS" in the application, but the database rollback occurs due to a late failure. How would you detect and correct such inconsistencies?

**Answer:** Implement reconciliation jobs that periodically compare application state (event logs) with the final database state. Use the Outbox pattern to ensure the database and message broker updates are atomic.

**Scenario/Question:** 8. How would you design an audit trail where no employee—not even a DBA—can alter historical transaction records?

**Answer:** Use an append-only, immutable datastore or a blockchain/ledger database (like Amazon QLDB). Ensure the database is write-once-read-many (WORM) and restrict update/delete permissions at the infrastructure level.

**Scenario/Question:** 9. A fraud detection system blocks a transaction after the debit has already been committed. How would you recover without violating accounting principles?

**Answer:** Execute a compensating transaction (a new credit entry) reversing the original debit, referencing the original transaction ID, rather than deleting or modifying the original debit record.

**Scenario/Question:** 10. A customer disputes a transaction from six months ago. What information would your backend need to reconstruct exactly what happened?

**Answer:** Immutable audit logs containing the request payload, idempotency key, timestamp, client IP, device ID, authentication context, and exact state changes of the accounts involved.

**Scenario/Question:** 11. How would you prevent two different banking channels (mobile app and branch system) from processing conflicting updates to the same account simultaneously?

**Answer:** Use a centralized transaction coordinator or distributed locking (e.g., Redis Redlock) tied to the account ID. Optimistic locking on the database row is also essential.

**Scenario/Question:** 12. A transaction remains in "PROCESSING" for several hours due to an external dependency failure. How should the backend recover automatically?

**Answer:** A background sweeping job should periodically check for stale "PROCESSING" transactions, query the external system for the final status (using a status API), and update the internal state accordingly.

**Scenario/Question:** 13. How would you reconcile daily account balances if one microservice was unavailable for two hours?

**Answer:** Rely on an event-driven architecture where missed events are stored in a persistent queue (like Kafka). Once the service recovers, it processes the backlog to achieve eventual consistency. A daily end-of-day batch job performs a final reconciliation against the source of truth.

**Scenario/Question:** 14. A database replication delay causes stale balance reads. How would you prevent customers from making decisions based on outdated information?

**Answer:** Route balance read queries to the primary (writer) database immediately after a write (read-your-own-writes consistency). Alternatively, include a version or timestamp in the read response to indicate data freshness.

**Scenario/Question:** 15. How would you implement transaction rollback when some downstream systems do not support rollback?

**Answer:** Use the Saga pattern with compensating transactions. For downstream systems that cannot be rolled back (e.g., sending an SMS), ensure they are the final step in the transaction, or send a subsequent 'correction' notification if possible.

**Scenario/Question:** 46. An overnight interest calculation job crashes after processing 70% of accounts. How would you restart it without recalculating completed accounts?

**Answer:** Use Spring Batch or similar framework with chunk-based processing and state persistence. Store the last successfully processed offset/ID in a checkpoint table and resume from that point.

**Scenario/Question:** 47. A batch process accidentally executes twice. How would you prevent duplicate postings?

**Answer:** Ensure the batch logic is idempotent. Use unique constraints in the database based on date, account ID, and batch run ID, or check if the target records for that specific run already exist before inserting.

**Scenario/Question:** 48. How would you split a batch job processing 50 million customer records into manageable units?

**Answer:** Partition the data based on account ID ranges, hash of account ID, or geographic regions. Process each partition in parallel using multiple worker nodes or threads.

**Scenario/Question:** 49. A regulatory report must be generated before 6 AM every day. The batch is running behind schedule. What optimizations would you consider?

**Answer:** Optimize SQL queries (indexes, bulk reads/writes), increase parallel processing (partitioning), eliminate network chatter by processing closer to the DB, or pre-calculate intermediate aggregations during the day.

**Scenario/Question:** 50. How would you design checkpoints for long-running batch jobs?

**Answer:** Commit state after every N records (chunk processing). Write the current primary key or offset being processed to a separate checkpoint table so that upon failure, the job queries this table to find where to resume.

**Scenario/Question:** 51. A batch job locks tables, blocking online banking transactions. How would you redesign it?

**Answer:** Read from a read-replica database, process in memory, and write back using small, incremental transactions rather than locking the entire table. Avoid long-running transactions and use row-level locks.

**Scenario/Question:** 52. How would you notify operations teams when batch failures require manual intervention?

**Answer:** Integrate the batch framework with monitoring tools (Prometheus/Grafana) or alerting systems (PagerDuty, Slack). Send an alert with the job name, point of failure, and exception trace.

**Scenario/Question:** 53. A batch reads stale data due to replication lag. How would you ensure consistency?

**Answer:** Configure the batch job to read exclusively from the primary (writer) database if strict consistency is required, or pause the job until replication lag metrics fall below an acceptable threshold.

**Scenario/Question:** 54. How would you recover if the batch server crashes midway through file generation?

**Answer:** Write output to a temporary file. When complete, atomically rename/move it to the final destination. If a crash occurs, delete the incomplete temporary file and restart the job from the last checkpoint.

**Scenario/Question:** 55. A customer requests account closure while an overnight batch is processing the account. How would you handle this conflict?

**Answer:** Implement state checks. If the account is locked/marked for closure, the batch skips it and logs it for review. Alternatively, the closure process checks for running batch locks and waits.

**Scenario/Question:** 56. How would you ensure batch jobs remain idempotent?

**Answer:** Tag every operation with a unique Job Execution ID. Before writing, verify that the operation for that ID hasn't been completed. Rely on database upserts (MERGE/ON CONFLICT) rather than plain inserts.

**Scenario/Question:** 57. A batch imports corrupted files from an external agency. How would you isolate bad records?

**Answer:** Implement a validation phase before processing. Process valid records and route invalid records to a 'Dead Letter Table' or error file, generating an alert for manual review, without failing the entire batch.

**Scenario/Question:** 58. How would you monitor batch progress in real time?

**Answer:** Update a central job metadata table (or use Spring Batch tables) with current chunk counts. Expose these metrics via an API or export to a time-series database (Prometheus) for dashboard visualization.

**Scenario/Question:** 59. A file contains duplicate customer records. How would the batch detect and handle them?

**Answer:** Load data into a temporary staging table, use SQL window functions (like `ROW_NUMBER()`) or GROUP BY to identify and eliminate duplicates based on unique keys, and then process only the distinct records.

**Scenario/Question:** 60. How would you safely deploy a new batch version while another batch is still running?

**Answer:** Use a blue-green deployment or feature flags. Ensure the new code version checks the current active version flag in a database. Running jobs complete on the old version, while new triggers use the new version.

