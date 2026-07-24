# Scenario-Based Questions — Transactions, Bulk Processing, Consistency, and Scale

The supplied material covers distributed transactions, Saga coordination, idempotency, large-file processing, NoSQL design, high-traffic debugging, banking consistency, and restartable batch processing. The repeated questions are consolidated and technically corrected below.

---

# 1. A Multi-Step Transaction Fails Midway. How Do You Ensure Rollback?

The answer depends on whether all steps use the **same transactional resource** or span several independent systems.

## Case 1: One service and one database

Use a normal local database transaction:

```java
@Service
public class TransferService {

    private final AccountRepository accountRepository;

    public TransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(
            long sourceAccountId,
            long destinationAccountId,
            BigDecimal amount
    ) {
        Account source = accountRepository.findByIdForUpdate(sourceAccountId)
                .orElseThrow(AccountNotFoundException::new);

        Account destination = accountRepository.findByIdForUpdate(destinationAccountId)
                .orElseThrow(AccountNotFoundException::new);

        source.debit(amount);
        destination.credit(amount);
    }
}
```

If the credit operation throws an unchecked exception, Spring marks the transaction for rollback and neither balance change is committed.

## Important requirements

- Keep the transaction short.
- Do not call remote services while holding database locks.
- Lock rows in a consistent order to reduce deadlocks.
- Do not return success until the transaction commits.
- Use decimal types appropriate for money; never use `double`.

---

## Case 2: Multiple services or databases

A local `@Transactional` annotation cannot roll back work committed by another service.

```text
Transfer Service transaction
        ≠
Account Service transaction
        ≠
Payment Provider transaction
```

Use:

```text
Local transactions
+
Saga workflow
+
Transactional outbox
+
Idempotent participants
+
Compensating actions
+
Reconciliation
```

## Example distributed transfer

```text
1. Create transfer as PENDING.
2. Debit source account.
3. Credit destination account.
4. Mark transfer COMPLETED.
```

If credit fails:

```text
Credit failed
    ↓
Create compensating credit for source account
    ↓
Mark transfer COMPENSATED
```

Compensation is a **new business transaction**, not deletion of the original debit.

---

# 2. How Do You Handle Distributed Transactions Without 2PC?

Two-phase commit can coordinate multiple transactional participants, but it introduces:

- Coordinator dependency
- Blocking participants
- Long-held resources
- Reduced availability
- Difficult recovery
- Tight infrastructure coupling
- Poor support for long-running workflows

For independently deployed microservices, a Saga is usually a better fit.

## Recommended architecture

```text
Service-local ACID transaction
        ↓
Outbox event
        ↓
Message broker
        ↓
Next Saga participant
        ↓
Idempotent local transaction
```

## Required components

### Local transaction

Each service modifies only its own database.

### Transactional outbox

The business update and outgoing event are committed together:

```text
Business table update
+
Outbox row
=
One local database transaction
```

### Idempotent consumer

A retried or redelivered message must not repeat the business effect.

### Saga state

Persist workflow progress:

```text
PENDING
DEBIT_COMPLETED
CREDIT_PENDING
COMPLETED
COMPENSATION_PENDING
COMPENSATED
MANUAL_REVIEW_REQUIRED
```

### Reconciliation

A scheduled process identifies workflows that remain incomplete beyond an expected period.

---

# 3. How Do You Implement the Saga Pattern?

A Saga consists of local transactions connected by commands or events.

## Orchestrated Saga

A central orchestrator controls the sequence.

```text
Transfer Orchestrator
    ├── Debit source
    ├── Credit destination
    ├── Complete transfer
    └── Compensate debit on failure
```

### Advantages

- Workflow is visible in one place.
- Timeout and compensation logic is explicit.
- Easier to inspect and operate.
- Suitable for complex financial workflows.

### Disadvantages

- The orchestrator may become too large.
- Participants become coupled to orchestration commands.
- The orchestrator needs durable state and high availability.

---

## Choreographed Saga

Participants react to events.

```text
TransferCreated
    ↓
Source Account Service
    ↓ DebitCompleted
Destination Account Service
    ↓ CreditCompleted
Transfer Service
    ↓ TransferCompleted
```

### Advantages

- Looser temporal coupling
- No central coordinator
- Natural event-driven processing

### Disadvantages

- Workflow logic becomes distributed.
- Troubleshooting is harder.
- Cyclic event dependencies may develop.
- Compensation paths become less obvious.

For critical transfers with several ordered steps, orchestration is often easier to reason about.

---

## What if compensation fails?

Compensation must be:

- Durable
- Idempotent
- Retryable
- Observable
- Recoverable manually

```text
COMPENSATION_PENDING
    ↓
Retry with backoff
    ├── success → COMPENSATED
    └── exhausted → MANUAL_REVIEW_REQUIRED
```

Never endlessly retry without:

- An attempt limit
- Backoff and jitter
- Alerting
- A recovery queue
- Manual operational tooling

---

# 4. Money Transfer: Debit Succeeds but Credit Fails

## First design decision

If both accounts belong to the same banking database and transaction boundary, use one local ACID transaction rather than introducing a distributed Saga unnecessarily.

```text
Same authoritative ledger
→ one database transaction
```

If source and destination are controlled by different systems, use a pending transfer workflow.

## Recommended ledger model

Do not simply overwrite balances without an immutable transaction record.

```text
Transfer T-100

Ledger entry 1:
Source account    -50,000

Ledger entry 2:
Destination account +50,000
```

Balance can be calculated or maintained as a derived value, but the ledger entries remain the authoritative audit history.

## Distributed version

```text
1. Create transfer T-100 as PENDING.
2. Record source debit with reference T-100.
3. Record destination credit with reference T-100.
4. Mark transfer COMPLETED.
```

If the destination cannot be credited:

```text
Record source reversal referencing T-100
Mark transfer COMPENSATED
```

The following must be unique:

```text
transfer_id + operation_type + account_id
```

This prevents duplicate debit, credit, or compensation entries.

---

# 5. How Do You Implement Idempotency in Distributed Systems?

Idempotency ensures that repeating the same logical operation creates no additional business effect.

```text
One payment request
=
The same request retried five times
=
One payment
```

## Idempotency record

```sql
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    result_reference VARCHAR(100),
    response_body TEXT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);
```

## Processing flow

```text
Receive idempotency key
        ↓
Calculate canonical request hash
        ↓
Attempt to insert PROCESSING record
        ├── Insert succeeds
        │      → process operation
        │      → store result
        │
        └── Duplicate key
               ├── same hash + SUCCESS
               │      → return stored result
               ├── same hash + PROCESSING
               │      → return pending/conflict
               └── different hash
                      → reject key reuse
```

## Concurrent requests

Do not use only:

```text
SELECT key
→ not found
→ INSERT
```

Two requests can both observe “not found.”

Use a database uniqueness constraint and treat the insert as the concurrency decision.

## Redis vs database

Redis may help with fast duplicate detection, but for critical financial processing:

```text
Database uniqueness and durable record
→ authoritative guarantee

Redis
→ optional acceleration
```

A Redis eviction or failure must not allow a duplicate financial posting.

---

# 6. Duplicate Payment-Gateway Callbacks

Suppose a payment gateway sends the same callback five times.

Use the gateway’s unique transaction or event identifier:

```sql
CREATE UNIQUE INDEX uk_gateway_event
ON payment_callback(gateway_name, gateway_event_id);
```

Processing:

```text
Callback arrives
    ↓
Insert gateway event ID
    ├── success → process credit
    └── duplicate → return previously accepted response
```

Do not identify duplicates only by:

- Amount
- Timestamp
- Customer ID
- Account number

Different legitimate payments may share those values.

Also verify:

- Callback signature
- Trusted gateway identity
- Amount and currency
- Merchant reference
- Expected payment state

---

# 7. The Client Times Out but the Backend Succeeds

A timeout does not prove failure.

```text
Backend commits debit
        ↓
Response is lost
        ↓
Mobile client sees timeout
```

The client should retry with the same idempotency key.

```http
POST /transfers
Idempotency-Key: transfer-mobile-8721
```

The backend returns the original result:

```json
{
  "transferId": "T-100",
  "status": "COMPLETED"
}
```

Never tell the user to create a completely new request after an ambiguous timeout without first checking the original operation.

A status endpoint is also useful:

```http
GET /transfers/by-idempotency-key/transfer-mobile-8721
```

---

# 8. How Do You Process Millions of Records Efficiently?

Use a **job-oriented asynchronous architecture**.

```text
Client uploads file
        ↓
Object storage
        ↓
Job record created
        ↓
Validation and staging
        ↓
Partitioned workers
        ↓
Chunk processing
        ↓
Results and error report
```

## Do not process a million-row upload synchronously

A synchronous HTTP request can fail because of:

- Client timeout
- Gateway timeout
- Pod restart
- Long-held server thread
- Large memory usage
- Inability to report partial progress

A better API is:

```http
POST /bulk-imports
```

Response:

```http
HTTP/1.1 202 Accepted
```

```json
{
  "jobId": "JOB-100",
  "status": "QUEUED",
  "statusUrl": "/bulk-imports/JOB-100"
}
```

The client checks:

```http
GET /bulk-imports/JOB-100
```

---

# 9. How Do You Process a 5 GB CSV Without `OutOfMemoryError`?

Never load the complete file into memory.

Avoid:

```java
List<String> lines = Files.readAllLines(path);
```

Use streaming and bounded chunks.

```java
public void processCsv(Path path) throws IOException {
    int batchSize = 1_000;
    List<CustomerRecord> batch = new ArrayList<>(batchSize);

    try (BufferedReader reader = Files.newBufferedReader(path)) {
        String line;

        while ((line = reader.readLine()) != null) {
            CustomerRecord record = parseAndValidate(line);
            batch.add(record);

            if (batch.size() == batchSize) {
                persistBatch(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            persistBatch(batch);
        }
    }
}
```

For real CSV, use a streaming CSV parser that correctly supports:

- Quoted commas
- Escaped quotes
- Multiline fields
- Character encoding
- Malformed-row reporting

A plain `split(",")` implementation is not safe for general CSV data.

## Memory characteristics

The memory requirement becomes approximately:

```text
Reader buffer
+
Current chunk
+
Framework overhead
```

rather than the complete 5 GB file.

---

## Recommended phases

### 1. Upload

Stream directly to object storage or disk.

Do not first hold the entire multipart body in heap memory.

### 2. Validate the file

Check:

- File size
- Format
- Encoding
- Required headers
- Malware policy
- Row-size limit
- Maximum column count

### 3. Stage data

Load rows into a staging table when database-based validation or deduplication is needed.

### 4. Process in chunks

```text
Read 1,000 records
→ validate
→ write batch
→ commit
→ save checkpoint
```

### 5. Isolate invalid rows

Do not fail the entire file for every bad record unless atomic all-or-nothing processing is a requirement.

Produce an error file:

```text
row_number
business_key
error_code
error_message
```

### 6. Finalize atomically

Only mark the job `COMPLETED` after all partitions and validation rules succeed.

---

# 10. Retry and Failure Handling for Bulk Processing

Retry only failures that are likely temporary.

## Retryable failures

- Temporary network failure
- Database connection timeout
- Broker unavailability
- Transient external-service error

## Non-retryable failures

- Invalid account number
- Missing mandatory column
- Unsupported currency
- Malformed date
- Business-rule violation

## Record state

```text
PENDING
PROCESSING
SUCCEEDED
FAILED_RETRYABLE
FAILED_PERMANENT
```

## Retry policy

```text
Attempt 1
→ wait 1 second

Attempt 2
→ wait 2 seconds

Attempt 3
→ wait 4 seconds plus jitter

Attempts exhausted
→ error table or dead-letter queue
```

Do not restart a million-record job from the beginning because one record failed.

---

# 11. How Do You Design Checkpoints?

Persist progress after each committed chunk.

Checkpoint data may include:

```text
job_id
partition_id
last_processed_key
rows_read
rows_written
rows_skipped
current_status
updated_at
```

Example:

```sql
UPDATE batch_partition
SET last_processed_customer_id = :customerId,
    rows_processed = :processed,
    updated_at = CURRENT_TIMESTAMP
WHERE job_id = :jobId
  AND partition_id = :partitionId;
```

Checkpoint and business writes should be coordinated so the job does not record progress for data that was rolled back.

## Prefer stable business keys

For database processing:

```text
last processed primary key
```

is generally safer than:

```text
row offset
```

when the source can change.

For immutable files, a byte offset or row number can be used, but the file must be identified by a stable checksum or version.

---

# 12. How Do You Partition 50 Million Records?

Possible partitioning strategies include:

## Primary-key range

```text
Worker 1: IDs 1–10,000,000
Worker 2: IDs 10,000,001–20,000,000
```

Works when IDs are relatively evenly distributed.

## Hash partitioning

```text
hash(account_id) mod 16
```

Usually produces more even distribution.

## Business partitioning

Examples:

- Region
- Bank branch
- Customer segment
- Processing date
- Tenant

## Message partitioning

For account updates:

```text
Kafka message key = accountId
```

Events for one account go to the same partition and are processed in order by one consumer within the group.

This can serialize per-account updates while allowing different accounts to process concurrently.

---

# 13. How Do You Ensure a Batch Job Is Idempotent?

A batch execution ID alone is insufficient if a restarted job receives a new execution ID.

Use a stable **business operation key**.

Example:

```sql
CREATE UNIQUE INDEX uk_interest_posting
ON interest_posting(
    account_id,
    calculation_date,
    interest_type
);
```

Then:

```text
Same account + same business date + same interest type
→ only one posting
```

Possible write techniques:

- Unique constraints
- `INSERT ... ON CONFLICT`
- `MERGE`
- Conditional update
- Processed-event table
- Version comparison

The job execution ID is useful for audit and tracing, but business uniqueness must be based on the actual business operation.

---

# 14. How Do You Handle Duplicate Records Inside a File?

Use a staging table:

```sql
CREATE TABLE customer_import_staging (
    job_id VARCHAR(50),
    row_number BIGINT,
    customer_reference VARCHAR(100),
    payload JSONB
);
```

Detect duplicates:

```sql
SELECT customer_reference, COUNT(*)
FROM customer_import_staging
WHERE job_id = :jobId
GROUP BY customer_reference
HAVING COUNT(*) > 1;
```

Or rank them:

```sql
SELECT *,
       ROW_NUMBER() OVER (
           PARTITION BY customer_reference
           ORDER BY row_number
       ) AS duplicate_rank
FROM customer_import_staging
WHERE job_id = :jobId;
```

The business must define which record wins:

- First record
- Last record
- Highest version
- Reject every duplicate
- Merge selected fields

Do not silently choose without a documented rule.

---

# 15. How Do You Handle Corrupted Records?

Separate failures into:

```text
File-level failure
→ invalid header, unreadable encoding, unsupported format

Record-level failure
→ one malformed or invalid row
```

For record-level errors:

```text
Valid rows
→ continue processing

Invalid rows
→ quarantine/error table
```

Error information should include:

- Job ID
- File checksum
- Row number
- Business key where available
- Validation code
- Safe error message
- Original row or protected reference

Be careful when error files contain personal or financial information.

---

# 16. How Do You Handle a Batch Crash During File Generation?

Write to a temporary location:

```text
report-2026-07-22.csv.part
```

After successful completion and validation, atomically rename or move it:

```text
report-2026-07-22.csv
```

Consumers should never see the `.part` file.

Persist:

- Job status
- Output checksum
- Row count
- File size
- Generation timestamp

On restart:

```text
Incomplete temporary file
→ delete or continue from a supported checkpoint
```

Appending to an incomplete CSV is risky unless the framework guarantees correct row and encoding boundaries.

---

# 17. How Do You Prevent Batch Jobs from Blocking Online Traffic?

Avoid:

- Full-table locks
- Long transactions
- Huge uncommitted updates
- Unbounded parallelism
- Running expensive queries on the primary during peak hours

Use:

- Small chunks
- Short transactions
- Appropriate indexes
- Keyset pagination
- Bounded worker counts
- Workload throttling
- Separate resource pools
- Read replicas where stale reads are acceptable
- Dedicated batch windows
- Database workload management

## Read-replica warning

A read replica is not appropriate when the batch requires the latest committed data and replication lag is unacceptable.

Also, “read from replica and write back later” can produce decisions based on stale state. The consistency requirement must be explicit.

---

# 18. NoSQL Schema Design

NoSQL schemas should be designed around access patterns, partitioning, and consistency boundaries—not by directly copying normalized relational tables.

## Questions to answer

- What are the main reads?
- What are the main writes?
- Which fields change together?
- What is the partition key?
- Can one partition become hot?
- What ordering is required?
- What data can be duplicated?
- How will duplicates be synchronized?
- Which operations require conditional writes?
- What is the retention policy?

## Example order document

```json
{
  "orderId": "O-100",
  "customerId": "C-10",
  "status": "CONFIRMED",
  "version": 4,
  "createdAt": "2026-07-22T10:00:00Z",
  "items": [
    {
      "productId": "P-1",
      "productName": "Keyboard",
      "unitPrice": 100.0,
      "quantity": 1
    }
  ],
  "totals": {
    "subtotal": 100.0,
    "tax": 18.0,
    "total": 118.0
  }
}
```

The product name and price may be intentionally duplicated as an order-time snapshot.

## Avoid unbounded documents

Do not place an endlessly growing list inside one document:

```text
Customer document
└── every transaction ever created
```

Use separate transaction records or bounded buckets.

## Concurrency

Use conditional writes or versions:

```text
Update only if version = 4
→ write version 5
```

Do not assume every NoSQL database provides the same transaction, consistency, indexing, or query guarantees.

---

# 19. Transactions in a Distributed Database

The design depends on the database’s actual guarantees.

Possible mechanisms include:

- Single-partition atomic writes
- Multi-document transactions
- Conditional writes
- Compare-and-set
- Optimistic version checks
- Quorum reads and writes
- Consensus-based transactions

## Design preference

Keep strongly consistent operations within one partition or aggregate where possible.

```text
Account aggregate
→ balance
→ account state
→ account version
```

A transaction spanning many partitions or regions is normally more expensive and less available.

## Questions to clarify

- Is the transaction linearizable?
- Can reads be stale?
- What happens during a network partition?
- Is transaction isolation configurable?
- What is the cross-region latency?
- Are retries automatically performed?
- Can a timed-out commit later succeed?
- How are duplicate writes detected?

A timeout should often be treated as **unknown outcome**, not automatic failure.

---

# 20. Production Bug Appears Only Under Heavy Traffic

This often indicates:

- Race condition
- Lock contention
- Pool exhaustion
- Queue growth
- Cache stampede
- Thread-safety bug
- Memory visibility problem
- Database plan change
- Hot key
- Retry storm
- GC pressure
- Resource leak

## Investigation process

### 1. Capture the exact production symptom

- Which endpoint?
- Which tenant?
- At what concurrency?
- Which application version?
- P95/P99 latency?
- Error rate?
- Request payload characteristics?

### 2. Inspect saturation

- Request threads
- Executor queues
- Database connections
- HTTP connections
- CPU throttling
- Heap and GC
- File descriptors
- Broker lag
- Redis latency
- Database locks

### 3. Compare fast and slow traces

```text
Normal request:
20 ms total

Slow request:
8 seconds total
├── DB connection wait: 5.5 seconds
├── Query: 2.4 seconds
└── Application code: 100 ms
```

### 4. Capture evidence during the event

- Thread dumps
- Java Flight Recorder
- Heap histogram
- GC logs
- Database wait events
- Query plans
- Pool metrics
- Kernel and container metrics

### 5. Reproduce the traffic shape

Do not test only average requests per second.

Reproduce:

- Burst pattern
- Payload size distribution
- Hot accounts or keys
- Slow downstream dependency
- Cache hit/miss ratio
- Number of concurrent users
- Retry behaviour
- Production-like data volume

### 6. Add deterministic tests

For concurrency defects, use:

- Repeated stress tests
- Barriers to align threads
- Deliberate delays around critical sections
- Race-oriented unit tests
- Fault injection
- Controlled dependency latency

Do not “fix” the problem only by increasing timeouts or pool sizes before locating the constrained resource.

---

# 21. Corrected Banking Scenarios

## Scenario 1: Debit succeeds but credit fails

Use one local transaction if both accounts are in the same authoritative database. For truly distributed accounts, persist a pending transfer, perform idempotent debit and credit steps, and compensate with a new reversal entry if credit cannot complete.

---

## Scenario 2: Concurrent credits overwrite each other

Avoid read-modify-write where possible.

Prefer atomic SQL:

```sql
UPDATE account
SET balance = balance + :amount
WHERE account_id = :accountId;
```

For additional invariants, use:

- Optimistic locking
- Pessimistic locking
- Account-keyed event serialization
- Immutable ledger entries

---

## Scenario 3: Same UPI callback arrives repeatedly

Use a unique provider event or transaction ID protected by a database constraint. Validate callback authenticity and return the previously recorded result for duplicates.

---

## Scenario 4: Mobile timeout after successful debit

Retry with the original idempotency key. Return the original transfer status instead of debiting again.

---

## Scenario 5: Thousands of salary credits

Parallelize across different accounts while serializing or atomically updating one account’s state.

Possible design:

```text
Kafka key = beneficiaryAccountId
```

Do not simply “batch all balances” if batching weakens transaction correctness.

---

## Scenario 6: Transfer received before cutoff but processed afterward

Persist both:

```text
received_at
processing_started_at
```

The business rule should explicitly define which timestamp determines eligibility. Use a trusted server-side timestamp—not a client-supplied clock.

Also persist the applied cutoff calendar/version for later audit.

---

## Scenario 7: Application says SUCCESS but database rolls back

The service should not expose success before commit.

Correct flow:

```text
Perform database work
→ commit succeeds
→ return success
```

For asynchronous notifications, use an outbox after the database transaction. Reconciliation is a safety mechanism, not a substitute for correct commit ordering.

---

## Scenario 8: Audit trail that even a DBA cannot alter

No software can absolutely guarantee immutability when one actor controls the database, operating system, backups, keys, and audit platform.

Use defence in depth:

- Append-only ledger
- No update/delete permission for application roles
- Separate audit account and infrastructure
- WORM/object-lock retention
- Hash chaining
- Signed audit batches
- External timestamping or anchoring
- Separation of duties
- Independent backups
- Regular integrity verification

A blockchain is not automatically required.

---

## Scenario 9: Fraud block after debit

Record a compensating credit referencing the original debit. Do not delete or rewrite the original ledger entry.

---

## Scenario 10: Reconstructing a six-month-old transaction

Retain:

- Transaction and correlation IDs
- Idempotency key
- Ledger entries
- State transitions
- Authenticated actor or service identity
- Received and processed timestamps
- Source channel
- Applied rules and configuration versions
- External provider references
- Safe request fingerprint
- Relevant consent and authorization evidence
- Audit events

Avoid retaining unnecessary raw secrets, complete tokens, or sensitive payloads.

---

## Scenario 11: Mobile and branch update the same account

Prefer database-level atomicity, optimistic locking, pessimistic locking, or per-account event serialization.

A Redis lock should not be the primary correctness guarantee for the account ledger. Leases can expire while a paused process still continues. The authoritative database must reject stale or duplicate writes.

---

## Scenario 12: Transaction remains `PROCESSING`

A sweeper should:

1. Identify stale records.
2. Acquire safe ownership of recovery work.
3. Query the external provider using a stable reference.
4. Update the state idempotently.
5. Retry within a bounded policy.
6. Escalate unresolved outcomes.

Do not assume timeout means failed; the external operation may have succeeded.

---

## Scenario 13: Service unavailable for two hours

Durable messaging allows backlog processing after recovery, but also require:

- Consumer lag monitoring
- Idempotency
- Replay support
- Dead-letter handling
- Ordering policy
- Reconciliation against the source of truth

---

## Scenario 14: Replica returns a stale balance

For correctness-sensitive balance reads:

- Read from the primary.
- Use session consistency or read-your-writes routing.
- Wait for a known version/LSN where supported.
- Return a freshness/version indicator.
- Avoid the replica for authorization of a withdrawal.

---

## Scenario 15: Downstream system cannot roll back

Design compensation where possible. For irreversible actions:

- Perform them late in the workflow.
- Use pending/approval states.
- Delay side effects until commit is known.
- Send correction events when reversal is impossible.
- Require manual review for exceptional cases.

An SMS is usually a notification side effect and should not determine whether the financial transaction commits.

---

# 22. Corrected Batch Scenarios

## Scenario 46: Job crashes after 70%

Use restartable chunk processing with durable job metadata. Resume from the last committed checkpoint, not merely the last row read.

---

## Scenario 47: Job starts twice

Use:

- Scheduler-level single execution
- Unique business-run key
- Database constraint
- Idempotent writes
- Job status table

Example:

```text
interest-calculation + business-date
→ unique
```

---

## Scenario 48: Process 50 million customers

Partition by a stable, balanced key and use bounded parallel workers. Ensure one record cannot be processed concurrently by multiple partitions.

---

## Scenario 49: Regulatory report is late

Investigate before only adding workers:

- Critical-path duration
- Database plan
- Full scans
- Sort spills
- Network round trips
- Serialization cost
- Skewed partitions
- Resource contention
- Replication lag
- Upstream data delay

Precompute immutable daily aggregates where regulations permit.

---

## Scenario 50: Long-running checkpoints

Checkpoint after each committed chunk. Persist the partition, business key, counters, and source-file identity.

---

## Scenario 51: Batch blocks online transactions

Use short transactions, small batches, appropriate indexes, bounded concurrency, and workload isolation. A read replica helps only when stale reads are acceptable.

---

## Scenario 52: Notify operations

Publish structured alerts containing:

- Job name and ID
- Environment
- Failed partition
- Last checkpoint
- Failure category
- Retry count
- Error reference
- Runbook link
- Whether manual action is required

Do not send secrets or full sensitive rows to chat systems.

---

## Scenario 53: Replica data is stale

Use the primary when strict consistency is required, or start only after replication lag is below a documented threshold. Record the data snapshot or cutoff used by the batch.

---

## Scenario 54: Server crashes during file generation

Write to a temporary file, calculate row count and checksum, then atomically publish it under the final name.

---

## Scenario 55: Account closure conflicts with a batch

Model explicit account states:

```text
ACTIVE
CLOSURE_PENDING
CLOSED
```

Both operations must use atomic state transitions or locking. Merely “checking before processing” is vulnerable to a race unless the state is revalidated during the write.

---

## Scenario 56: Keep batches idempotent

Use business uniqueness, upserts, and conditional updates. A job execution ID helps audit but does not alone prevent duplicate business postings.

---

## Scenario 57: Corrupted external file

Validate file-level structure first. Quarantine invalid rows separately, enforce an error threshold, and stop the file when corruption suggests the source cannot be trusted.

---

## Scenario 58: Monitor progress

Expose:

- Rows read
- Rows processed
- Rows skipped
- Rows failed
- Processing rate
- Estimated completion
- Current partition
- Oldest retry
- Job and partition status

Avoid updating the metadata table after every row; update periodically or per chunk.

---

## Scenario 59: Duplicate customer records

Stage the file and deduplicate using business-defined keys. Decide whether to reject, retain first, retain last, or merge. Report the decision.

---

## Scenario 60: Deploy a new version while a job runs

Pin each job execution to a code and configuration version.

```text
Running jobs
→ continue on version A

New jobs
→ start on version B
```

Use:

- Separate worker deployments
- Versioned queues
- Blue-green workers
- Backward-compatible metadata
- Draining before shutdown

A feature flag alone may not protect a running job when its code or data model changes underneath it.

---

# 23. Rate Limiting at Millions-of-User Scale

This topic is covered separately in depth, but the core design is:

```text
Edge/WAF
→ coarse abuse control

API Gateway
→ tenant, client, user, and route quotas

Service
→ business-specific and concurrency limits
```

For synchronous APIs, Token Bucket is usually a strong default:

```text
Shared Redis state
+
Atomic script
+
Authenticated rate-limit key
+
HTTP 429
+
Retry-After
```

For expensive long-running operations, add a concurrency limiter. Rate per second alone does not control how many operations remain active.

---

# Interview-Ready Summary

> For work inside one database, I rely on a short local ACID transaction and return success only after commit. Across services, I use Saga, transactional outbox, idempotent participants, compensating transactions, explicit workflow states, and reconciliation rather than expecting `@Transactional` to span the network.
>
> For large files, I accept the upload asynchronously, stream rather than loading the file into memory, process in bounded chunks, checkpoint after committed work, partition using stable keys, and isolate invalid rows. Every write is idempotent and protected by business-key constraints so retries or duplicate job execution do not create duplicate postings.
>
> In financial systems, I prefer immutable ledger entries, atomic database updates, and durable transaction references. Timeouts are treated as unknown outcomes, duplicate callbacks are deduplicated using provider identifiers, and stale workflows are recovered through status checks and reconciliation.
