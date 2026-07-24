# Advanced Questions — Distributed Systems and Microservice Architecture

## Important Corrections

- Creating **one Kafka topic per user** is usually not practical for one million users. Use a limited number of partitioned topics keyed by `userId`, while connection gateways maintain the live user-to-connection mapping.
- A distributed rate limiter should not necessarily be a synchronous remote microservice called for every request. That introduces another network hop and failure dependency. A common design uses a centralized remote microservice called **control plane** for policies and a distributed **data plane** at gateways backed by Redis.
- An idempotency key alone is insufficient. The service must also store a request fingerprint, operation state, and previous response under an atomic uniqueness constraint.
- A payment timeout does not prove that the payment failed. The provider may have completed the operation before the response was lost.
- Kubernetes DNS is itself a service-discovery mechanism. A separate application registry such as Eureka is often unnecessary inside a Kubernetes-only environment.
- Active-active multi-region deployment is significantly harder for write-heavy systems because concurrent writes require ownership, conflict prevention, or explicit conflict-resolution rules.
- Audit logs and diagnostic application logs serve different purposes. Compliance audit events require stronger schemas, retention, access control, and tamper-evidence.
- Competing consumers preserve parallelism but do not preserve global event ordering. Kafka guarantees ordering within a partition, n([Apache Kafka][1])earch13turn741831search35
- Dynamic configuration refresh is not one atomic operation across every service instance. Multiple configuration versions may temporarily coexist.

---

# Q96: Design a Real-Time Notification System for One Million Concurrent Users

## 1. Clarify the Requirements

Before designing the system, establish:

### Notification types

- Chat messages
- Financial transaction alerts
- Social notifications
- Operational status updates
- Marketing messages
- Presence updates

### Delivery requirements

- At-most-once, at-least-once, or best effort
- Maximum acceptable latency
- Ordering requirements
- Offline-user handling
- Multi-device delivery
- Message expiry
- Read acknowledgements
- Retry behavior
- Regulatory retention

Example target:

```text
Concurrent connected users: 1,000,000
Peak notifications:         200,000 messages/second
Delivery latency:           P99 below 1 second
Availability:               99.99%
Delivery model:             At-least-once
Ordering:                   Per user or conversation
Offline retention:          7 days
```

The architecture depends more on peak message rate, fan-out factor, payload size, and delivery guarantees than on connection count alone.

---

## 2. High-Level Architecture

```text
                         Clients
               WebSocket / SSE / Mobile Push
                            │
                            ▼
                  Global Load Balancer
                            │
                            ▼
               Real-Time Gateway Cluster
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   Gateway A           Gateway B           Gateway C
   connections         connections         connections
        │                   │                   │
        └──────────── Connection Registry ──────┘
                            │
                            ▼
                  Notification Router
                            │
                            ▼
                 Kafka / Event Backbone
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        Fan-Out Workers  Push Workers  Persistence
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
       APNs / FCM                    Offline Inbox
```

---

## 3. Client Connection Protocol

## WebSocket

Use WebSocket when communication is bidirectional:

- Chat
- Presence
- Typing indicators
- Client acknowledgements
- Interactive collaboration

```text
Client ───── messages and acknowledgements ───── Gateway
```

## Server-Sent Events

Use SSE when communication is primarily server-to-client:

- Financial alerts
- Status updates
- Dashboard events
- Notification feeds

```text
Server ───── events ─────► Client
```

SSE is simpler for one-way delivery and can use standard HTTP reconnection behavior. WebSocket is more suitable when the client must frequently send messages on the same connection.

## Mobile push

Disconnected mobile users normally require:

- Firebase Cloud Messaging
- Apple Push Notification service
- Another platform push provider

Do not keep every notification indefinitely waiting for a WebSocket connection.

---

## 4. Real-Time Gateway Layer

The gateway owns long-lived connections.

Each gateway maintains an in-memory mapping:

```text
userId → connections
```

Example:

```text
user-100
├── mobile connection on Gateway A
├── browser connection on Gateway A
└── tablet connection on Gateway C
```

A user may have several active devices.

### Gateway responsibilities

- Authenticate connection establishment
- Validate token expiry and audience
- Maintain heartbeats
- Detect disconnected clients
- Apply per-connection backpressure
- Serialize outgoing messages
- Process acknowledgements
- Enforce connection and message limits
- Publish connection-state changes

### Do not store durable business state in the gateway

The gateway should be replaceable:

```text
Gateway crashes
→ clients reconnect
→ connection registry updates
→ pending messages are replayed from durable state
```

---

## 5. Connection Registry

The system needs to determine:

> Which gateway currently owns the user’s connection?

Possible record:

```text
userId: user-100
connections:
  - gatewayId: gateway-a-17
    connectionId: conn-881
    deviceId: mobile-91
  - gatewayId: gateway-c-04
    connectionId: conn-992
    deviceId: tablet-15
```

Possible implementations:

- Redis
- Distributed key-value store
- Gateway-owned partition metadata
- Consistent-hash routing
- Service-mesh or load-balancer session affinity where justified

Registry entries must have leases or TTLs so crashed gateways do not leave permanent stale connections.

The registry is routing metadata—not the durable notification source of truth.

---

## 6. Kafka Topic Design

Avoid:

```text
One Kafka topic per user
```

For one million users, this creates excessive topic, partition, metadata, file, and operational overhead.

Prefer:

```text
Topic: user-notifications
Partitions: sized for throughput and consumer parallelism
Key: userId
```

```text
user-100 → partition 12
user-101 → partition 47
user-102 → partition 12
```

Events with the same key are directed to the same partition when the producer uses consistent key-based partitioning. Kafka preserves record or([Apache Kafka][1])earch13turn741831search35

Example event:

```json
{
  "eventId": "evt-9281",
  "notificationId": "ntf-5100",
  "userId": "user-100",
  "type": "PAYMENT_COMPLETED",
  "priority": "HIGH",
  "payload": {
    "paymentId": "PAY-900",
    "status": "COMPLETED"
  },
  "createdAt": "2026-07-24T08:30:00Z",
  "expiresAt": "2026-07-31T08:30:00Z",
  "schemaVersion": 2
}
```

Do not put unrestricted sensitive payment or customer data in the notification event.

---

## 7. Fan-Out Strategy

## Fan-out on write

When an event is created, generate one notification record per recipient.

Suitable when:

- Recipient list is known
- Fast reads are important
- Fan-out size is bounded

Problem:

```text
Celebrity with 20 million followers
→ one event creates 20 million records
```

## Fan-out on read

Store the source event and construct each user’s notification feed when requested.

Suitable when:

- Fan-out size can be extremely large
- Some delay is acceptable

Trade-off:

- More expensive reads
- Harder real-time delivery

## Hybrid approach

Use fan-out on write for normal users and fan-out on read or specialized broadcast channels for very large audiences.

---

## 8. Delivery Semantics

A practical design usually provides **at-least-once delivery**.

```text
Gateway sends notification
→ client receives it
→ acknowledgement is lost
→ gateway sends it again
```

Clients must deduplicate using:

```text
notificationId
```

Example client behavior:

```text
if notificationId already processed:
    ignore duplicate
else:
    display notification
    store notificationId
    acknowledge
```

Exactly-once end-to-end delivery is generally not achievable merely by configuring Kafka because the system crosses brokers, gateways, networks, clients, and device storage.

---

## 9. Online and Offline Users

```text
Notification created
        │
        ├── user online
        │     → route to gateway
        │     → send over WebSocket/SSE
        │
        └── user offline
              → store in notification inbox
              → optionally send mobile push
              → deliver after reconnect
```

The durable inbox might contain:

```text
notification_id
user_id
status
created_at
expires_at
delivered_at
read_at
payload_reference
```

Store large payloads separately and send a compact notification reference where appropriate.

---

## 10. Backpressure

A slow client must not consume unlimited gateway memory.

Per-connection policy:

```text
Maximum queued messages: 100
Maximum queued bytes:    1 MiB
```

When the limit is exceeded:

- Drop replaceable low-priority events
- Keep only the latest state update
- Persist critical events for later delivery
- Disconnect an unresponsive client
- Require the client to resynchronize after reconnecting

Never silently discard:

- Payment confirmations
- Security alerts
- Compliance notifications

---

## 11. Scaling the Gateway Layer

Suppose one tested gateway instance safely supports:

```text
50,000 active connections
```

The theoretical minimum is:

```text
1,000,000 / 50,000 = 20 gateway instances
```

Production capacity should include:

- Failure tolerance
- Uneven connection distribution
- Deployment surge
- CPU and memory headroom
- Connection spikes
- TLS overhead

A safer deployment might require 30–40 instances after load testing.

Scale on:

- Active connection count
- Outbound message rate
- Event-loop latency
- Pending bytes
- Message queue depth
- CPU
- Memory
- Connection failures

CPU alone may not reflect connection pressure.

---

## 12. Failure Scenarios

Test:

- Gateway crashes with 50,000 clients
- Kafka partition temporarily unavailable
- Connection registry failure
- Notification duplicated
- Notification received out of order
- Client acknowledgement lost
- Mobile push provider unavailable
- Slow client fills its queue
- Full regional outage
- Token expires during a long-lived connection

### Interview-ready answer

> I use stateless WebSocket or SSE gateways behind a global load balancer, with a distributed connection registry mapping users to gateway instances. Notification events are published to a partitioned Kafka topic keyed by user ID rather than creating one topic per user. Fan-out workers route online notifications to the correct gateway and persist offline notifications in a durable inbox. Delivery is at least once, so messages carry stable notification IDs and clients deduplicate. The design includes per-connection backpressure, multi-device support, mobile push fallback, partition-based ordering, gateway autoscaling, and regional failure recovery.

---

# Q97: Design a Distributed Rate Limiter as a Shared Service

## 1. Avoid a Central Synchronous Bottleneck

A naïve design:

```text
Every request
→ call Rate Limiter Service
→ wait for response
→ call business service
```

Problems:

- Extra latency
- Rate-limiter-service bottleneck
- New failure dependency
- Large cross-zone traffic
- Cascading failure if the limiter is unavailable

A better design separates:

```text
Control plane
→ stores and distributes policies

Data plane
→ makes low-latency decisions near request handling
```

```text
                   Rate Limit Control Plane
                 configuration and policies
                            │
                            ▼
API Gateway A ───────────── Redis
API Gateway B ───────────── Redis
Service C     ───────────── Redis
```

The control plane can still be a shared microservice, but request-time enforcement should normally happen in gateways, sidecars, or service middleware.

---

## 2. Rate-Limit Policy Model

```json
{
  "tenantId": "tenant-100",
  "operation": "payment.create",
  "algorithm": "TOKEN_BUCKET",
  "capacity": 100,
  "refillTokens": 20,
  "refillPeriodSeconds": 1,
  "requestCost": 5,
  "mode": "ENFORCE",
  "version": 14
}
```

Dimensions may include:

- Tenant
- API key
- User
- Client application
- Route
- Operation
- IP address
- Global platform capacity

Use IP as a secondary signal, not the only identity.

---

## 3. Token Bucket

```text
Capacity:        100 tokens
Refill:           20 tokens/second
Payment request:   5 tokens
Read request:      1 token
```

Token bucket allows controlled bursts while enforcing a long-term rate.

Redis provides official examples of distributed token-bucket rate limiting using shared state and atomic Lua scripts, inclu([Redis][2])earch26turn741831search40

---

## 4. Atomic Redis Operation

The following operation must happen atomically:

```text
1. Load previous token count.
2. Calculate refill.
3. Check request cost.
4. Deduct tokens.
5. Store updated state.
6. Set expiry.
```

Example Lua logic:

```lua
local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refill_per_ms = tonumber(ARGV[2])
local cost = tonumber(ARGV[3])
local ttl_ms = tonumber(ARGV[4])

local redis_time = redis.call("TIME")
local now_ms =
    redis_time[1] * 1000
    + math.floor(redis_time[2] / 1000)

local state = redis.call(
    "HMGET",
    key,
    "tokens",
    "last_refill"
)

local tokens = tonumber(state[1])
local last_refill = tonumber(state[2])

if tokens == nil then
    tokens = capacity
end

if last_refill == nil then
    last_refill = now_ms
end

local elapsed = math.max(0, now_ms - last_refill)

tokens = math.min(
    capacity,
    tokens + elapsed * refill_per_ms
)

local allowed = 0

if tokens >= cost then
    tokens = tokens - cost
    allowed = 1
end

redis.call(
    "HSET",
    key,
    "tokens", tokens,
    "last_refill", now_ms
)

redis.call("PEXPIRE", key, ttl_ms)

return {
    allowed,
    tokens,
    now_ms
}
```

Redis Lua scripts execute atomically relative to other Redis commands, which prevents concurrent li([Redis][2])search2turn741831search11

Keep the script short because long-running scripts block other Redis work.

---

## 5. Rate-Limit Key

```text
rate-limit:{tenant-100}:payment.create
```

Include only dimensions that affect the policy.

Avoid:

```text
rate-limit:{tenant}:{user}:{IP}:{device}:{session}:{requestId}
```

Excessively granular keys increase memory use and cardinality.

---

## 6. Response

When permitted:

```http
HTTP/1.1 200 OK
RateLimit-Limit: 100
RateLimit-Remaining: 45
```

When rejected:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 2
RateLimit-Limit: 100
RateLimit-Remaining: 0
```

---

## 7. Shadow Mode

Before enforcing a new policy:

```text
mode = SHADOW
```

In shadow mode:

- Calculate the limiter decision.
- Do not reject the request.
- Record whether it would have been rejected.
- Evaluate customer and business impact.
- Adjust limits.
- Move gradually to enforcement.

Metrics:

```text
rate_limit_shadow_rejections_total
rate_limit_enforced_rejections_total
rate_limit_allowed_total
```

Do not use tenant IDs as unrestricted metric labels. Put individual tenant details in logs or sampled traces.

---

## 8. Failure Policy

## Fail open

```text
Redis unavailable
→ permit request
```

Suitable only when availability is more important than protection.

## Fail closed

```text
Redis unavailable
→ reject request
```

Suitable for:

- OTP generation
- Login attempts
- Expensive paid provider calls
- High-risk financial operations

## Hybrid

```text
Redis unavailable
→ conservative local limiter
→ alert
→ reject traffic above emergency capacity
```

The hybrid approach avoids both unlimited traffic and complete platform failure.

---

## 9. Multi-Region Limiting

Strong global rate limits require shared coordination and add cross-region latency.

Options:

### Regional budgets

```text
Global quota: 1,000 RPS

Region A budget: 600 RPS
Region B budget: 400 RPS
```

Simple and resilient, but temporarily approximate.

### Home-region ownership

Each tenant has one authoritative limiter region.

### Global datastore

Provides stronger coordination but adds latency and regional dependency.

Decide whether the quota must be mathematically global or operationally approximate.

### Interview-ready answer

> I separate the rate limiter into a control plane and data plane. The control plane manages versioned per-tenant policies, while enforcement runs at the gateway or service boundary using Redis-backed token buckets or sliding windows. Redis state changes are atomic through Lua, and the limiter supports weighted request costs, shadow mode, `429` responses, auditability, and explicit fail-open, fail-closed, or emergency local-limit behavior. For multi-region operation, I normally allocate regional budgets unless strict global coordination is a business requirement.

---

# Q98: Design an Idempotent Payment Service

## 1. API Contract

```http
POST /api/payments
Idempotency-Key: pay-order-100-attempt-1
Content-Type: application/json
```

```json
{
  "orderId": "ORDER-100",
  "customerId": "CUSTOMER-20",
  "amount": "250.00",
  "currency": "USD",
  "paymentMethodToken": "pm-token-901"
}
```

The key should identify one logical payment attempt.

---

## 2. Payment State Machine

Avoid only:

```text
PENDING → SUCCESS / FAILED
```

Use explicit states:

```text
RECEIVED
    ↓
PROCESSING
    ├── SUCCEEDED
    ├── FAILED_FINAL
    └── OUTCOME_UNKNOWN
            ↓
        RECONCILING
            ├── SUCCEEDED
            └── FAILED_FINAL
```

Possible additional states:

```text
CANCELLATION_PENDING
REFUND_PENDING
REFUNDED
MANUAL_REVIEW
```

`FAILED` should not combine permanent rejection with temporary uncertainty.

---

## 3. Idempotency Table

```sql
CREATE TABLE payment_idempotency (
    idempotency_key        VARCHAR(128) PRIMARY KEY,
    request_fingerprint    VARCHAR(128) NOT NULL,
    payment_id             VARCHAR(64) NOT NULL,
    status                 VARCHAR(32) NOT NULL,
    http_status            INTEGER,
    response_body          TEXT,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP NOT NULL,
    expires_at             TIMESTAMP NOT NULL
);
```

Payment table:

```sql
CREATE TABLE payment (
    payment_id             VARCHAR(64) PRIMARY KEY,
    order_id               VARCHAR(64) NOT NULL,
    amount                 DECIMAL(19, 4) NOT NULL,
    currency               VARCHAR(3) NOT NULL,
    provider_reference     VARCHAR(128),
    status                 VARCHAR(32) NOT NULL,
    version                BIGINT NOT NULL,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP NOT NULL
);
```

Also add business uniqueness where required:

```sql
CREATE UNIQUE INDEX uk_payment_order_attempt
ON payment(order_id, payment_id);
```

---

## 4. Request Fingerprint

A client might accidentally reuse the same key for a different request.

```text
Idempotency-Key: key-100
Request 1: amount = 100 USD

Idempotency-Key: key-100
Request 2: amount = 500 USD
```

This must be rejected.

Calculate a canonical fingerprint from relevant fields:

```text
SHA-256(
    orderId
    + amount
    + currency
    + paymentMethodToken
)
```

Do not include volatile headers or whitespace-dependent raw JSON.

---

## 5. Request Processing Algorithm

```text
BEGIN TRANSACTION

Insert idempotency record with:
key
fingerprint
status = RECEIVED

If unique-key conflict:
    load existing record

    if fingerprint differs:
        reject with 409 Conflict

    if previous result completed:
        return stored response

    if operation still running:
        return 202 Processing
        or wait for a bounded period

Create payment record
Commit

Call payment provider using:
stable payment ID
provider idempotency key

Persist final or unknown outcome
Persist response
Publish outbox event
```

The initial insert and payment creation must be atomic.

---

## 6. Simplified Service Logic

```java
public PaymentResponse createPayment(
        String idempotencyKey,
        PaymentRequest request
) {
    String fingerprint = fingerprint(request);

    IdempotencyRecord record =
            idempotencyRepository
                    .findById(idempotencyKey)
                    .orElse(null);

    if (record != null) {
        if (!record.fingerprint().equals(fingerprint)) {
            throw new IdempotencyKeyConflictException(
                    idempotencyKey
            );
        }

        return mapExistingResult(record);
    }

    Payment payment = transactionTemplate.execute(status -> {
        idempotencyRepository.insert(
                IdempotencyRecord.received(
                        idempotencyKey,
                        fingerprint
                )
        );

        return paymentRepository.insert(
                Payment.processing(request)
        );
    });

    return invokeProviderAndFinalize(
            idempotencyKey,
            payment
    );
}
```

In production, concurrent insert handling must depend on the database uniqueness constraint rather than a check-then-insert race.

---

## 7. Provider Call

Send:

```text
providerIdempotencyKey = internal paymentId
```

This prevents duplicate provider-side execution when the service retries.

```text
Payment Service sends request
→ provider completes charge
→ response is lost
→ Payment Service retries with same provider key
→ provider returns original result
```

---

## 8. Timeout and Unknown Outcome

Incorrect:

```text
Provider timeout
→ mark payment FAILED
```

Correct:

```text
Provider timeout
→ mark OUTCOME_UNKNOWN
→ query provider status
→ process provider webhook
→ reconcile
```

Until resolved:

```json
{
  "paymentId": "PAY-100",
  "status": "PENDING",
  "message": "Payment outcome is being confirmed"
}
```

Do not tell the customer to submit a completely new payment immediately when the first charge may have succeeded.

---

## 9. Webhook Deduplication

Provider callbacks may be duplicated.

Store:

```text
provider_event_id
provider_reference
event_type
processed_at
```

Use a unique constraint:

```sql
CREATE UNIQUE INDEX uk_provider_event
ON provider_webhook_event(provider_event_id);
```

Process the deduplication record and payment state update in one transaction.

---

## 10. Transactional Outbox

After the payment commits:

```text
payment.status = SUCCEEDED
+
PaymentSucceeded outbox event
=
same local transaction
```

The relay may publish an event more than once, so consumers must remain idempotent.

---

## 11. Duplicate Request Responses

| Existing status      | Duplicate request response            |
| -------------------- | ------------------------------------- |
| `SUCCEEDED`          | Return stored successful response     |
| `FAILED_FINAL`       | Return stored final failure           |
| `PROCESSING`         | Return `202 Accepted` or bounded wait |
| `OUTCOME_UNKNOWN`    | Return pending/reconciliation status  |
| Fingerprint mismatch | Return `409 Conflict`                 |

### Interview-ready answer

> I require an idempotency key and calculate a canonical request fingerprint. A database table enforces uniqueness and stores the payment ID, state, and previous response. The first request atomically creates the idempotency and payment records. Duplicate requests with the same fingerprint return the existing result; reuse with different data is rejected. Provider calls use the internal payment ID as the provider idempotency key. Timeouts move the payment to `OUTCOME_UNKNOWN`, followed by webhook processing and reconciliation rather than assuming failure. Payment changes and outbox events commit together, and all callbacks and consumers are idempotent.

---

# Q99: Service Discovery Without Eureka

## Kubernetes Service Discovery

Kubernetes Services expose a stable service endpoint over changing backend Pods, and Kubernetes creates([Kubernetes][3])search9turn741831search18

```yaml
apiVersion: v1
kind: Service
metadata:
  name: inventory-service
  namespace: inventory
spec:
  selector:
    app: inventory-service
  ports:
    - port: 8080
      targetPort: 8080
```

Same namespace:

```text
http://inventory-service:8080
```

Different namespace:

```text
http://inventory-service.inventory:8080
```

Fully qualified:

```text
http://inventory-service.inventory.svc.cluster.local:8080
```

Flow:

```text
Order Service
    ↓ DNS lookup
inventory-service.inventory
    ↓
Kubernetes Service
    ↓
EndpointSlice backends
    ↓
Ready Inventory Pod
```

Kubernetes automatically manages EndpointSlice information for t([Kubernetes][4]). citeturn741831search18

---

## Headless Service

A headless Service has:

```yaml
spec:
  clusterIP: None
```

Example:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: broker
spec:
  clusterIP: None
  selector:
    app: broker
  ports:
    - port: 9092
```

DNS can return individual Pod addresses instead of one virtual Service address.

Use headless Services for:

- StatefulSet peer discovery
- Application-controlled load balancing
- Cluster membership
- Direct Pod addressing
- Database or broker nodes

StatefulSets commonly use headless Services to control ([Kubernetes][5])earch24turn741831search38

Do not use a headless Service merely because ordinary Service routing already meets the requirement.

---

## Other Registry-Free Approaches

### Cloud load balancer

```text
Service
→ internal load balancer DNS
→ backend instances
```

### DNS records

Useful when addresses change slowly and DNS TTL/failure behavior is acceptable.

### Service mesh

A mesh control plane discovers endpoints through Kubernetes or cloud APIs while applications call logical service names.

### Static configuration

Suitable only for genuinely stable, small environments. It does not handle dynamic scaling well.

---

## Client-Side Load Balancing

A client can resolve multiple endpoints and select one using:

- Round robin
- Random
- Least connections
- Zone preference
- Consistent hashing

It must still handle:

- Stale DNS
- Connection failure
- Timeout
- Unhealthy endpoint
- Endpoint changes
- Safe retry

### Interview-ready answer

> In Kubernetes, I normally use Services and cluster DNS instead of Eureka. The consumer calls a stable DNS name, while Kubernetes maintains EndpointSlices for ready Pods and routes traffic to the backends. When a clustered application needs individual Pod addresses, I use a headless Service, often with a StatefulSet. In cloud environments, internal load balancer DNS or service-mesh discovery can provide the same abstraction. Regardless of discovery mechanism, calls still need timeouts, circuit breakers, and safe retries.

---

# Q100: Migrate a Shared Database to Database-per-Service

## Initial State

```text
Service A ─┐
Service B ─┼── Shared Database
Service C ─┘
```

Common problems:

- Services update each other’s tables.
- Schema changes require cross-team coordination.
- Database joins hide domain coupling.
- One service can corrupt another service’s data.
- Scaling and security boundaries are weak.
- Independent deployment is difficult.

---

## Target State

```text
Customer Service  → Customer DB
Order Service     → Order DB
Payment Service   → Payment DB
```

Communication occurs through:

- APIs
- Domain events
- Read models
- Explicit data contracts

Database-per-service means private ownership, not necessarily a separate physical database server for every small service.

---

## Step 1: Discover Current Ownership

Create a table map:

| Table      | Reads                    | Writes            | Intended owner   |
| ---------- | ------------------------ | ----------------- | ---------------- |
| `customer` | Customer, Order, Support | Customer, Support | Customer Service |
| `order`    | Order, Payment           | Order             | Order Service    |
| `payment`  | Payment, Order           | Payment           | Payment Service  |

Identify:

- Cross-service SQL joins
- Stored procedures
- Triggers
- Shared sequences
- Foreign keys across domains
- Batch jobs
- Reporting queries
- Direct table updates
- Transaction boundaries

Do not start by physically moving tables before understanding behavior.

---

## Step 2: Establish Logical Ownership

Declare:

```text
Only Customer Service writes customer data.
Only Order Service writes order data.
Only Payment Service writes payment data.
```

Temporarily, the tables may remain in the shared database, but ownership is enforced in code and database permissions.

This is a useful intermediate step:

```text
Shared physical database
+
separate schemas/users
+
single writer per domain
```

---

## Step 3: Add an Anti-Corruption Layer

Replace direct foreign-table access:

```text
Order Service
→ SELECT * FROM customer
```

with:

```text
Order Service
→ CustomerClient.getCustomerSummary()
```

The adapter prevents shared database structures from leaking further into business logic.

---

## Step 4: Replace Cross-Service Reads

Possible approaches:

### Synchronous API composition

```text
Order Service
→ Customer Service API
```

Use when current data is required immediately.

### Event-driven local projection

```text
CustomerUpdated event
→ Order Service customer summary table
```

Use when bounded eventual consistency is acceptable.

### Reporting/read-model platform

Use for analytical joins spanning many service-owned datasets.

Do not replace a local SQL join with ten sequential remote calls on every request.

---

## Step 5: Introduce Outbox Events

When Customer Service changes customer data:

```text
Update customer
+
insert CustomerUpdated outbox record
=
one local transaction
```

A relay publishes the event, and consumers update their own projections idempotently.

---

## Step 6: Extract One Domain at a Time

Choose a suitable first extraction:

- Clear boundaries
- Limited cross-domain writes
- Few shared transactions
- Lower operational risk
- Useful business value

Avoid beginning with the most tightly coupled central table.

---

## Step 7: Copy Historical Data

```text
Initial snapshot
→ new service database
→ capture changes during migration
→ reconcile counts and checksums
```

Possible mechanisms:

- Batch copy
- CDC
- Outbox events
- Migration jobs

Validation:

```text
row counts
business totals
checksums
version numbers
missing records
orphan references
```

---

## Step 8: Shadow Reads

```text
Production response comes from old DB.
Service also reads new DB asynchronously.
Compare results.
Record mismatches.
```

Do not expose shadow-read differences to the customer until confidence is established.

---

## Step 9: Cut Over Reads and Writes

Preferred progression:

```text
1. New service becomes authoritative writer.
2. Legacy callers use the new service API.
3. Reads move to new database.
4. Old table becomes read-only.
5. Reconciliation confirms consistency.
6. Old path is removed.
```

Avoid long-term application-level dual writes:

```text
write old DB
write new DB
```

One write can succeed while the other fails. Use outbox, CDC, or a temporary replayable migration pipeline.

---

## Step 10: Remove Cross-Database Constraints

A foreign key such as:

```text
order.customer_id → customer.id
```

cannot remain a normal database foreign key across independent services.

Replace it with:

- Stable external IDs
- API validation
- Local reference snapshots
- Event-driven lifecycle handling
- Reconciliation

---

## Transaction Changes

Before:

```text
One database transaction:
create order
reserve inventory
create payment
```

After:

```text
Local transactions
+
Saga
+
outbox
+
idempotency
+
compensation
```

The migration requires changing transaction semantics, not only copying tables.

### Interview-ready answer

> I begin by mapping table reads, writes, joins, stored procedures, and transaction boundaries, then establish one authoritative service owner per domain. Direct cross-table access is replaced with an anti-corruption layer, APIs, events, or local read models. I extract one low-coupling domain at a time, copy and reconcile historical data, use CDC or outbox events for ongoing changes, shadow-read the new database, and then cut over writes and reads. I avoid unsafe long-term dual writes and replace shared ACID transactions with local transactions, Sagas, idempotency, and reconciliation.

---

# Q101: Multi-Region Failover for a Critical Microservice

## Begin with Business Objectives

Define:

### RTO

> How quickly must service recover?

### RPO

> How much data loss is acceptable?

Example:

```text
RTO: below 5 minutes
RPO: below 30 seconds
```

Architecture choices must come from these objectives.

---

## Active-Passive

```text
Region A: active
Region B: warm standby
```

```text
Normal:
Users → Region A

Failure:
Users → Region B
```

### Advantages

- Simpler write ownership
- Fewer data conflicts
- Lower operational complexity
- Easier reasoning for financial workloads

### Disadvantages

- Standby capacity cost
- Failover delay
- Replication lag
- Failover testing required

Route 53 failover routing is intended for active-passive configurations, and health checks can direct DNS respo([AWS Documentation][6])2search0turn467772search4

---

## Active-Active

```text
Users in Asia   → Region A
Users in Europe → Region B

Both regions serve traffic.
```

### Advantages

- Lower local latency
- Both regions use provisioned capacity
- Potentially faster traffic failover

### Disadvantages

- Concurrent-write conflicts
- Cross-region event ordering
- Duplicate processing
- Split-brain risk
- More complex reconciliation
- Harder operations and testing

AWS guidance notes that active-active write replication can create conflicts when the same data is up([AWS Documentation][7])search2turn467772search22

---

## Traffic Routing

Possible routing strategies:

- Active-passive failover
- Latency-based routing
- Weighted routing
- Geoproximity routing
- Global accelerator
- Tenant home-region routing

Route 53 supports failover, latency, weighted, and geoproximity routing policies for different ([AWS Documentation][6])search0turn467772search32

Do not fail over based only on:

```text
HTTP /health returned 200 once
```

Use a composite regional health signal based on:

- API success rate
- Critical dependency health
- Database write capability
- Queue processing
- Business transaction success
- Regional capacity
- Replication condition

---

## Data Strategies

## Single-writer/home-region

```text
customer-100 → Region A
customer-200 → Region B
```

Each aggregate has one authoritative write region.

Advantages:

- Avoids most write conflicts
- Local writes for assigned tenants
- Easier ordering

During failover, ownership must move safely using an epoch, lease, or fencing token.

---

## Multi-active datastore

Some managed datastores support multi-region writes, but applications still need to understand their consistency and conflict rules. For example, DynamoDB global tables support multi-region, multi-active replication and use defined conflict resolution fo([AWS Documentation][8])earch10turn467772search30

Last-writer-wins may be unacceptable for:

- Account balances
- Inventory decrements
- Payment state transitions
- Approval workflows

For these operations, consider:

- Single-writer ownership
- Append-only ledger
- Commutative operations
- CRDT where semantics fit
- Global consensus database
- Explicit conflict queue and reconciliation

---

## Messaging

Events should include:

```text
eventId
aggregateId
aggregateVersion
originRegion
occurredAt
schemaVersion
```

Consumers remain idempotent because cross-region replication can replay or duplicate events.

Per-aggregate ordering is easier when each aggregate has a home region and one ordered event stream.

---

## Failover Runbook

```text
1. Confirm regional failure.
2. Freeze or fence writes in failed/uncertain region.
3. Verify replication position and data integrity.
4. Promote standby dependencies.
5. Move traffic gradually.
6. Verify technical and business metrics.
7. Reconcile ambiguous operations.
8. Keep failed region isolated until safe.
9. Plan controlled failback.
```

Do not allow both regions to believe they are the only active writer.

---

## Failback

Failback is often harder than failover.

```text
Region A fails
→ Region B becomes writer
→ Region A returns with stale state
```

Before moving traffic back:

- Synchronize data
- Reconcile conflicts
- Validate events
- Reset ownership/fencing epoch
- Warm capacity
- Shift traffic gradually

---

## Testing

Run scheduled exercises:

- Region-level traffic evacuation
- Database promotion
- DNS/global-routing switch
- Queue replication delay
- Split-brain prevention
- Secret and certificate availability
- Data reconciliation
- Controlled failback

AWS describes active/passive and active/active as distinct disaster-recovery approaches with different comple([AWS Documentation][9])search8turn467772search18

### Interview-ready answer

> I begin with RTO and RPO, then choose active-passive or active-active. For critical financial writes, I often prefer active-passive or home-region single-writer ownership because it avoids uncontrolled concurrent updates. A global routing layer sends traffic only to regions that can complete critical business operations. Data is replicated with known consistency and conflict semantics, events carry IDs and versions, and consumers are idempotent. Failover fences the old writer before promoting the new region, followed by reconciliation and a separately tested failback procedure.

---

# Q102: Decompose a 500,000-Line Monolith with No Tests

## Do Not Begin with a Rewrite

Avoid:

```text
Stop feature development
→ rewrite entire system as microservices
→ replace production in one release
```

This loses hidden business behavior and creates a long period without production feedback.

Use incremental strangulation.

---

## Step 1: Establish Observability

Before changing boundaries, understand production behavior.

Add:

- Request tracing
- Endpoint latency
- Error metrics
- Database query telemetry
- Dependency calls
- Business transaction outcomes
- Deployment and version labels

Map:

```text
Endpoint
→ modules
→ database tables
→ external systems
→ business owner
```

---

## Step 2: Add Characterization Tests

Characterization tests capture current behavior—even when that behavior is imperfect.

Example:

```java
@Test
void shouldPreserveCurrentCancellationFeeBehaviour() {
    CancellationResult result =
            legacyOrderService.cancel(
                    existingOrder(),
                    fixedClock()
            );

    assertThat(result.fee())
            .isEqualByComparingTo("12.50");
}
```

The first goal is not redesigning the behavior. It is preventing accidental changes while refactoring.

Focus on:

- Critical revenue paths
- Financial calculations
- Authorization
- Data updates
- Known production defects
- High-change modules

---

## Step 3: Identify Domains and Seams

Look for natural boundaries:

- Customer
- Order
- Payment
- Inventory
- Notification
- Reporting

Evidence of a seam:

- Separate package/module
- Clear data ownership
- Limited dependency direction
- Existing interface
- Batch boundary
- External API boundary
- Distinct business owner

Create a dependency graph:

```text
Payment
→ Order
→ Customer
→ Shared Utility
→ Global Database Access
```

---

## Step 4: Modularize Inside the Monolith

Before network extraction:

```text
monolith/
├── customer-module
├── order-module
├── payment-module
└── notification-module
```

Enforce:

- Explicit module APIs
- No cross-module repository access
- No cyclic dependencies
- Package visibility
- Domain ownership

A modular monolith can deliver most boundary benefits without distributed-system complexity.

---

## Step 5: Introduce Interfaces

Before:

```java
PaymentProcessor processor =
        new LegacyPaymentProcessor();
```

After:

```java
public interface PaymentGateway {
    PaymentResult authorize(PaymentCommand command);
}
```

The monolith uses the interface. Later, the implementation may become an HTTP or messaging adapter.

This is branch by abstraction:

```text
Existing callers
→ stable interface
→ legacy implementation

Later:
Existing callers
→ stable interface
→ extracted service
```

---

## Step 6: Extract a Leaf Domain First

Good candidates:

- Notification
- Document generation
- Search indexing
- Reporting
- Image processing

They often have:

- Few inbound dependencies
- Limited transaction coupling
- Asynchronous behavior
- Lower risk

Do not start with the central payment/order transaction unless the boundary is already well understood.

---

## Step 7: Use the Strangler Pattern

```text
API Gateway / Routing Layer
        │
        ├── /legacy/** → Monolith
        └── /notifications/** → New Service
```

Move one capability at a time.

---

## Step 8: Handle the Database

Initially:

```text
New service reads legacy DB through controlled adapter
```

Then:

```text
New service owns new schema/database
Monolith calls service API
Changes propagate through events
```

Do not allow permanent shared writes between the new service and monolith.

---

## Step 9: Build a Safety Net

For each extraction:

- Unit tests
- Characterization tests
- Contract tests
- Integration tests
- Shadow traffic
- Result comparison
- Feature-flagged routing
- Rollback path
- Business reconciliation

---

## Step 10: Measure Progress by Coupling Removed

Do not measure only:

```text
Number of microservices created
```

Measure:

- Cross-module dependencies removed
- Shared tables eliminated
- Independent deployments enabled
- Change lead time
- Failure isolation
- Test coverage of critical behavior
- Reduced release coordination

### Interview-ready answer

> I do not rewrite a 500,000-line untested monolith in one project. I first add observability and characterization tests around critical behavior, map domains, dependencies, and table ownership, and modularize boundaries within the existing process. I introduce interfaces around external seams, extract a low-coupling leaf domain, and route new traffic through the Strangler pattern. Data ownership moves gradually, with shadow comparisons, contract tests, feature flags, and rollback. I create a microservice only when the boundary and operational benefit justify the distributed-system cost.

---

# Q103: Design a Compliance Audit-Log System

## Audit Logs vs Application Logs

### Application log

```text
Payment provider call timed out.
```

### Audit event

```text
User U-100 approved payment PAY-900
from PENDING_APPROVAL to APPROVED
at 2026-07-24T08:40:00Z
using privileged role CHECKER.
```

Audit events must answer:

```text
Who?
What?
Which object?
When?
From where?
Under which authority?
What changed?
Was it successful?
```

---

## Architecture

```text
Microservices
    ↓ structured audit events
Local transactional outbox
    ↓
Kafka audit topic
    ↓
Audit ingestion service
    ├── searchable operational index
    ├── immutable archive
    └── compliance reporting
```

---

## Event Schema

```json
{
  "eventId": "audit-9001",
  "eventType": "PAYMENT_APPROVED",
  "occurredAt": "2026-07-24T08:40:00Z",
  "recordedAt": "2026-07-24T08:40:01Z",
  "actor": {
    "type": "USER",
    "id": "user-101",
    "roles": ["PAYMENT_CHECKER"]
  },
  "subject": {
    "type": "PAYMENT",
    "id": "PAY-900"
  },
  "action": "APPROVE",
  "outcome": "SUCCESS",
  "tenantId": "bank-10",
  "sourceService": "payment-service",
  "serviceVersion": "4.2.1",
  "traceId": "trace-981",
  "requestId": "request-672",
  "reasonCode": "MAKER_CHECKER_APPROVAL",
  "previousState": "PENDING_APPROVAL",
  "newState": "APPROVED",
  "schemaVersion": 3
}
```

---

## Transactional Generation

Incorrect:

```text
Commit payment
→ attempt to send audit message
→ application crashes
→ audit event missing
```

Correct:

```text
Update payment
+
insert audit outbox event
=
one local database transaction
```

The relay publishes the audit record later. Duplicate publication is acceptable when the audit ingestion layer deduplicates by `eventId`.

---

## Immutable Archive

Store long-term records in append-only or WORM-capable storage.

S3 Object Lock supports write-once-read-many protection and can prevent protected object versions from being overwritten or deleted duri([AWS Documentation][10])2search1turn467772search5

Architecture:

```text
Kafka
→ hourly immutable audit segment
→ object storage
→ Object Lock retention
→ separate security account
```

Keep the searchable index separate from the immutable archive. An index can be rebuilt from the archive.

---

## Tamper Evidence

Use:

- Append-only permissions
- Separate cloud account or security boundary
- WORM storage
- Hash-chained batches
- Digital signatures
- Trusted timestamps
- Sequence-gap detection
- Independent integrity verification

Batch hash example:

```text
batchHashN =
    SHA-256(
        batchHashN-1
        + canonicalBatchContent
    )
```

Store signatures or hash anchors in a separately controlled system.

Tamper-evident does not mean absolutely impossible to alter when one administrator controls every key, account, and verifier. Separation of duties is required.

---

## Ordering

Do not require one global Kafka partition merely to obtain total order.

Prefer:

```text
Partition key = audited aggregate ID
```

This preserves order for one payment, account, or approval flow while retaining parallelism.

Store both:

```text
occurredAt
recordedAt
aggregateVersion
```

Wall-clock timestamps alone are not sufficient for distributed global ordering.

---

## Sensitive Data

Audit logs may themselves contain sensitive information.

Do not include:

- Full card number
- Password
- Access token
- Private key
- Complete request payload
- Unnecessary PII

Use:

```text
customerId
paymentId
masked account suffix
field names changed
reason codes
```

Restrict audit access independently from normal application-log access.

---

## Failure Handling

If Kafka is unavailable:

- Keep the audit event in the local outbox.
- Retry asynchronously.
- Alert on outbox age.
- Prevent silent loss.

For especially regulated actions, decide whether the business operation must fail closed when durable audit recording cannot be guaranteed.

---

## Monitoring

Track:

- Outbox age
- Audit publication lag
- Duplicate count
- Schema validation failures
- Archive-write failures
- Sequence gaps
- Integrity-check failures
- Unauthorized audit access
- Retention status
- Search index vs archive mismatch

### Interview-ready answer

> Every service emits a versioned structured audit event containing actor, action, resource, outcome, previous and new state, service identity, trace ID, and business reason. The business change and audit outbox record commit in one local transaction. Events are published to Kafka, deduplicated by event ID, indexed for search, and stored in a separately controlled WORM archive. Access is least privileged, sensitive values are minimized, and signed or hash-chained batches provide tamper evidence. Monitoring detects publication lag, missing sequences, integrity failures, and unauthorized access.

---

# Q104: Competing Consumers and Message Ordering

## Pattern

Multiple consumers process messages from the same logical queue.

```text
Queue
 ├── Consumer 1
 ├── Consumer 2
 ├── Consumer 3
 └── Consumer 4
```

Benefits:

- Higher throughput
- Horizontal scaling
- Load sharing
- Failure recovery

---

## Kafka Consumer Group

```text
Topic with 6 partitions

Consumer A → partitions 0, 1
Consumer B → partitions 2, 3
Consumer C → partitions 4, 5
```

Within a consumer group, each topic partition is assigned to one consumer at a time. Kafka’s partitions are ordered logs, so ordering is preser([Apache Kafka][11])earch20turn741831search35

---

## Global Ordering Is Lost

Suppose:

```text
Partition 0: Event A
Partition 1: Event B
```

Consumers process partitions independently. The application cannot assume whether A or B completes first.

Therefore:

```text
More partitions
→ more parallelism
→ no topic-wide total ordering
```

---

## Preserve Ordering per Aggregate

Use:

```text
Kafka key = accountId
```

```text
Account A events → partition 3
Account B events → partition 8
```

For Account A:

```text
AccountCreated
→ DepositReceived
→ WithdrawalApproved
```

All events with the same key should use the same partitioning strategy.

---

## Consumer Parallelism Limit

```text
6 partitions
20 consumers
```

At most approximately six consumers can own those six partitions for that topic in one consumer group. Additional consumers remain idle for that assignment.

Increasing consumer count beyond partition count does not increase useful partition parallelism.

---

## Retry and Ordering

Suppose:

```text
Event 1 fails.
Event 2 is ready.
```

Options:

### Block partition

Retry Event 1 before processing Event 2.

```text
Ordering preserved
Throughput for that partition reduced
```

### Retry topic

Move Event 1 to a retry topic and process Event 2.

```text
Throughput continues
Original ordering may be lost
```

### Per-key sequencing

Use an aggregate sequence and defer events until missing earlier versions are resolved.

The correct strategy depends on whether ordering is a business requirement.

---

## Duplicate Processing

```text
Consumer updates DB
→ crashes before committing offset
→ Kafka redelivers event
```

The consumer must be idempotent.

Example:

```sql
INSERT INTO processed_event (
    consumer_name,
    event_id,
    processed_at
)
VALUES (
    :consumerName,
    :eventId,
    CURRENT_TIMESTAMP
);
```

Add a unique constraint on:

```text
consumer_name + event_id
```

Perform the deduplication insert and business update in one local transaction.

---

## Rebalancing

When consumers join, leave, crash, or change subscriptions, partition ownership may be reassigned.

During that period:

- Processing may pause.
- In-flight work must complete or be abandoned safely.
- Records may be redelivered.
- Idempotency remains required.

### Interview-ready answer

> The Competing Consumers pattern increases throughput by allowing several consumers to share work from one queue or consumer group. In Kafka, each partition is owned by one consumer in a group at a time, and ordering is guaranteed only within that partition. I key events by the aggregate ID when per-account or per-order ordering is required. Parallelism is bounded by partition count, retries may break ordering if moved to another topic, and consumers must be idempotent because a crash after a side effect but before offset commit causes redelivery.

---

# Q105: Distributed Configuration for More Than 50 Microservices

## Objectives

The system should provide:

- Central configuration ownership
- Environment-specific values
- Version history
- Validation
- Controlled rollout
- Auditability
- Rollback
- Secret separation
- High availability
- Configuration-version visibility

---

## High-Level Architecture

```text
                     Git Configuration Repository
                  reviewed and version-controlled
                                │
                                ▼
                     Configuration Control Plane
                   Spring Cloud Config / equivalent
                                │
               ┌────────────────┼────────────────┐
               ▼                ▼                ▼
          Service A         Service B         Service C
               │                │                │
               └──── configuration version telemetry ────┘

Secrets:
External Secret Manager
        ↓ workload identity
Services retrieve only permitted secrets
```

Spring Cloud Config provides an HTTP-based external-configuration API, and its default backend model is Git-based with labell([Home][12])earch23turn467772search35

---

## Repository Structure

```text
configuration/
├── shared/
│   ├── application.yml
│   └── observability.yml
├── services/
│   ├── order-service.yml
│   ├── payment-service.yml
│   └── inventory-service.yml
├── environments/
│   ├── development/
│   ├── staging/
│   └── production/
└── schemas/
    ├── order-service-config.schema.json
    └── payment-service-config.schema.json
```

Another Spring-oriented structure:

```text
application.yml
application-production.yml

order-service.yml
order-service-production.yml

payment-service.yml
payment-service-production.yml
```

Avoid deeply duplicated environment files that make it impossible to see the effective configuration.

---

## Configuration Precedence

Define a predictable order:

```text
Safe application defaults
        ↓
Shared organization configuration
        ↓
Service-specific configuration
        ↓
Environment overlay
        ↓
Deployment-time override
```

Limit emergency overrides because they create invisible drift.

---

## Validation Pipeline

Configuration changes should pass:

```text
Syntax validation
→ schema validation
→ range validation
→ semantic validation
→ policy checks
→ compatibility tests
→ review
→ staged rollout
```

Example rules:

```text
payment.timeout must be between 100 ms and 10 s
payment.retry.maxAttempts must be between 1 and 3
database.pool.maxSize must not exceed service budget
production debug logging must be disabled
```

Test combinations, not only individual fields:

```text
request deadline
>
retry attempts × per-attempt timeout + backoff
```

---

## Secret Separation

Keep ordinary configuration in Git:

```yaml
payment:
  provider:
    timeout: 2s
    base-url: https://provider.example
```

Keep secrets in a secret manager:

```text
payment-provider-client-secret
database-password
JWT-signing-private-key
```

Do not store encrypted secret values in Git unless key ownership, rotation, access control, and threat model justify it. A dedicated secrets platform normally provides stronger lifecycle controls.

---

## Configuration Delivery

## Pull at startup

```text
Service starts
→ requests configuration
→ validates configuration
→ starts or fails safely
```

For critical configuration, fail startup rather than using unsafe defaults.

## Runtime refresh

```text
Configuration changed
→ refresh event
→ selected instances reload
```

Spring Cloud Bus can use a broker to broadcast configuration-change events, and its `busrefresh` operation reloads configuration across li([Home][13])search7turn467772search31

Runtime refresh is not globally atomic.

```text
Instance A → config version 14
Instance B → config version 13
Instance C → restarting
```

Design configuration changes so temporary version coexistence is safe.

---

## What May Be Dynamically Refreshed?

Usually safer:

- Feature flags
- Display messages
- Timeouts within validated bounds
- Non-structural thresholds
- Some routing preferences

Usually safer through rolling restart:

- Database connection-pool topology
- Server port
- Kafka bootstrap infrastructure
- Serialization contracts
- Security filter-chain structure
- Thread-pool architecture
- Fundamental bean graph
- Encryption algorithm

Dynamic does not automatically mean safe.

---

## Rollout Strategy

```text
Commit configuration version 20
        ↓
validate
        ↓
development
        ↓
staging
        ↓
one production canary instance
        ↓
10% instances
        ↓
50% instances
        ↓
100% instances
```

Monitor:

- Startup failures
- Error rate
- P99 latency
- Dependency failures
- Business success metrics
- Configuration adoption

---

## Configuration Version

Every service should expose a safe version:

```text
applicationVersion = 4.2.1
configVersion      = 7fc21d8
schemaVersion      = 12
```

Include it in:

- Health metadata
- Structured startup log
- Traces
- Deployment inventory
- Operational dashboard

Do not expose configuration values or secrets.

---

## High Availability and Failure Behavior

Use multiple Config Server instances behind a stable endpoint.

At startup, decide explicitly:

### Fail fast

Use when missing configuration could cause:

- Wrong database access
- Unsafe security settings
- Incorrect financial behavior

### Safe local fallback

Use only when a complete, validated, deliberately supported fallback exists.

At runtime, already loaded configuration remains available when the server is temporarily down, but new instances and refresh operations may fail.

The Git backend can be configured to control how often Config Server fetche([Home][14])search6turn467772search39

---

## Audit Trail

Record:

```text
config change ID
Git commit
author
reviewers
ticket
affected services
environment
rollout start
rollout completion
instances updated
rollback decision
```

Require stronger approval for:

- Authentication changes
- Financial thresholds
- Endpoint allowlists
- Retention periods
- Rate limits
- Encryption configuration

---

## Avoid Bootstrap Cycles

Bad:

```text
Config Server requires Service Registry.
Service Registry requires Config Server.
Secret Manager adapter requires both.
```

Keep foundational dependencies simple.

Possible foundation:

```text
DNS
→ Config Server
→ services
```

or use Kubernetes ConfigMaps/Secrets and an operator when that better matches the platform.

---

## Monitoring

Track:

- Config Server availability
- Repository fetch errors
- Configuration request latency
- Validation failures
- Refresh failures
- Version distribution across instances
- Instances using obsolete configuration
- Secret retrieval failures
- Rollbacks
- Unauthorized changes

### Interview-ready answer

> I use a centralized configuration control plane backed by version-controlled Git, with shared defaults, service-specific settings, and environment overlays. Every change passes schema, semantic, security, and compatibility validation before staged rollout. Secrets remain in a dedicated secret manager and are retrieved using workload identity. Services report their configuration version, and runtime changes are refreshed only when temporary mixed versions are safe. Structural changes use rolling restarts. The platform is highly available, auditable, reversible, and has an explicit fail-fast or safe-fallback policy for Config Server outages.

---

# Quick Interview Cheat Sheet

| Topic                      | Core answer                                                      |
| -------------------------- | ---------------------------------------------------------------- |
| Million-user notifications | Gateway connections, Kafka routing, durable offline inbox        |
| User Kafka topics          | Use partitioned topics keyed by user, not one topic per user     |
| Delivery semantics         | At-least-once plus client deduplication                          |
| Distributed limiter        | Central policy, local enforcement, Redis atomic state            |
| Shadow rate limit          | Record decisions without rejecting traffic                       |
| Idempotent payment         | Key, fingerprint, unique DB record, stored result                |
| Payment timeout            | Unknown outcome followed by reconciliation                       |
| Registry-free discovery    | Kubernetes Service and DNS                                       |
| Headless Service           | Individual Pod addresses for clustered workloads                 |
| Database-per-service       | Ownership, APIs/events, gradual extraction                       |
| Multi-region               | RTO/RPO, traffic routing, data ownership, fencing                |
| Monolith decomposition     | Characterization tests and gradual strangulation                 |
| Audit logs                 | Transactional outbox, immutable archive, tamper evidence         |
| Competing consumers        | Parallelism with ordering only per partition                     |
| Configuration platform     | Git control plane, validation, staged rollout, secret separation |

# Senior Interview Summary

> For large distributed systems, I separate control-plane and data-plane responsibilities. Notification and rate-limiting policies are centrally governed, while high-volume delivery and enforcement happen close to the user request using partitioned brokers, gateway-local state, and atomic distributed storage.
>
> Financial correctness depends on durable identity and explicit state. An idempotent payment service stores the idempotency key, request fingerprint, payment state, and previous response atomically. Provider timeouts remain unresolved until webhooks or reconciliation establish the actual outcome.
>
> Service and data boundaries are migrated incrementally. Kubernetes DNS can replace a dedicated registry, shared databases are decomposed through ownership and event-driven projections, and monoliths are strangled only after characterization tests and internal modular boundaries exist.
>
> Multi-region and compliance designs require stronger guarantees than simple replication or centralized logging. Multi-region writes need ownership or conflict rules, while audit events require transactional generation, immutable retention, access separation, and integrity verification. Configuration is versioned, validated, auditable, gradually rolled out, and ept separate from secrets.

[1]: https://kafka.apache.org/documentation/?utm_source=chatgpt.com "Introduction | Apache Kafka"
[2]: https://redis.io/docs/latest/develop/use-cases/rate-limiter/php/?utm_source=chatgpt.com "Token bucket rate limiter with Redis and PHP | Docs"
[3]: https://kubernetes.io/docs/concepts/services-networking/service/?utm_source=chatgpt.com "Service"
[4]: https://kubernetes.io/docs/concepts/services-networking/?utm_source=chatgpt.com "Services, Load Balancing, and Networking"
[5]: https://kubernetes.io/docs/reference/kubernetes-api/core/service-v1/?utm_source=chatgpt.com "Service"
[6]: https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-policy.html?utm_source=chatgpt.com "Choosing a routing policy"
[7]: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Appendix.PostgreSQL.CommonDBATasks.pgactive.actact.replication.html?utm_source=chatgpt.com "Understanding active-active conflicts"
[8]: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html?utm_source=chatgpt.com "Global tables - multi-active, multi-Region replication"
[9]: https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/disaster-recovery-options-in-the-cloud.html?utm_source=chatgpt.com "Disaster recovery options in the cloud"
[10]: https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html?utm_source=chatgpt.com "Locking objects with Object Lock"
[11]: https://kafka.apache.org/25/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html?utm_source=chatgpt.com "KafkaConsumer (kafka 2.5.0 API)"
[12]: https://docs.spring.io/spring-cloud-config/reference/index.html?utm_source=chatgpt.com "Spring Cloud Config"
[13]: https://docs.spring.io/spring-cloud-bus/docs/current/reference/html/?utm_source=chatgpt.com "Spring Cloud Bus"
[14]: https://docs.spring.io/spring-cloud-config/reference/server/environment-repository/git-backend.html?utm_source=chatgpt.com "Git Backend :: Spring Cloud Config"
