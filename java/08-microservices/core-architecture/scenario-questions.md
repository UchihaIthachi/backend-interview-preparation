# Scenario-Based Questions — Microservices and Production System Design

The duplicate questions about configuration, schema evolution, compatibility, versioning, and inter-service security are consolidated below.

---

# 1. Legacy Monolith to Microservices

## Scenario

You are migrating a tightly coupled Java monolith to microservices:

- How do you identify service boundaries?
- How do you handle the shared database?
- What migration risks do you anticipate?

## Step 1: Understand the monolith before splitting it

I would first map:

- Business capabilities
- Module dependencies
- Database-table ownership
- Transaction boundaries
- Incoming and outgoing integrations
- High-change areas
- Performance bottlenecks
- Current production failure patterns

I would not create one microservice per controller, package, or database table.

Bad decomposition:

```text
CustomerTableService
OrderTableService
PaymentTableService
```

Better decomposition:

```text
Customer Management
Order Management
Payment Processing
Inventory Management
Fulfilment
```

## Step 2: Use DDD to find boundaries

Useful signals include:

- Bounded contexts
- Ubiquitous language
- Aggregates
- Business ownership
- Data consistency requirements
- Independent scaling requirements
- Independent deployment needs

For example, “Product” may mean different things in different contexts:

```text
Catalog Product:
name, description, category, images

Inventory Product:
SKU, warehouse, available quantity, reserved quantity

Pricing Product:
currency, base price, discounts
```

These models do not need to share one Java entity or one database table.

## Step 3: Start with a modular boundary

Before creating a network boundary, I would create an internal module boundary:

```text
Monolith
├── order
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── payment
├── inventory
└── customer
```

This exposes coupling while retaining:

- Local method calls
- Local transactions
- Simple debugging
- One deployment

Once a module has a stable responsibility and contract, it becomes a safer extraction candidate.

## Step 4: Use the Strangler Fig approach

```text
Clients
   ↓
Gateway or routing facade
   ├── Migrated capability → New microservice
   └── Remaining capability → Monolith
```

A practical migration sequence is:

1. Add observability to the monolith.
2. Introduce a routing layer.
3. Extract one bounded capability.
4. Create an anti-corruption layer.
5. Route selected traffic to the new service.
6. Compare results and performance.
7. Transfer data ownership.
8. Remove the old implementation.

## Handling the shared database

The desired end state is usually:

```text
Order Service     → Order-owned data
Payment Service   → Payment-owned data
Inventory Service → Inventory-owned data
```

However, immediately splitting a large database is often unsafe.

### Transitional approach

```text
Phase 1:
Services still use shared database,
but table ownership is documented.

Phase 2:
Only one service writes each owned table.
Other services read through APIs, events, or temporary views.

Phase 3:
Data is replicated or migrated into service-owned storage.

Phase 4:
Old direct database access is removed.
```

## Avoid uncontrolled dual writes

This is dangerous:

```text
Request
├── Update monolith database
└── Update microservice database
```

One operation may succeed while the other fails.

Safer approaches include:

- Transactional outbox
- Change-data capture
- Event-driven replication
- Backfill plus incremental synchronization
- One authoritative writer
- Reconciliation jobs

## Migration risks

| Risk                               | Mitigation                                                |
| ---------------------------------- | --------------------------------------------------------- |
| Incorrect service boundaries       | Start modularly and validate using real workflows         |
| Shared database coupling           | Establish table ownership and remove cross-service writes |
| Distributed transaction complexity | Saga, outbox, idempotency and reconciliation              |
| Increased latency                  | Measure call chains and avoid excessive synchronous calls |
| Partial failures                   | Timeouts, circuit breakers and bulkheads                  |
| Data inconsistency                 | Clear source of truth and reconciliation                  |
| Contract breakage                  | Backward-compatible contracts and contract tests          |
| Operational overhead               | Automate deployment, monitoring and incident response     |
| Difficult rollback                 | Retain routing fallback and backward-compatible schemas   |
| Distributed monolith               | Ensure services can deploy and operate independently      |

### Interview-ready answer

> I define service boundaries around cohesive business capabilities and bounded contexts, not tables or technical layers. I first establish modular boundaries inside the monolith, then use the Strangler Fig Pattern to extract one capability at a time. For the shared database, I establish one writer per data domain, migrate ownership incrementally, and use APIs, events, change-data capture, or outbox-based synchronization instead of permanent shared-table access.

---

# 2. How Would You Implement Centralized Logging?

Centralized logging collects logs from every service into one searchable platform.

```text
Order Service ──────┐
Payment Service ────┼──→ Log collector → Central log platform
Inventory Service ──┘
```

A production observability strategy should cover:

```text
Logs
Metrics
Traces
```

Spring Boot describes these as the three main observability pillars and uses Micrometer Observation for metrics and traces. ([Home][1])

## Structured logging

Avoid unstructured messages:

```java
log.info("Something happened in order");
```

Prefer structured, searchable fields:

```java
log.info(
        "event=order_created orderId={} customerId={} outcome={}",
        orderId,
        customerId,
        "success"
);
```

Useful fields include:

- Timestamp
- Service name
- Environment
- Application version
- Trace ID
- Span ID
- Request or correlation ID
- Business operation
- Resource identifier
- Outcome
- Error code
- Pod or instance ID

When tracing is configured, Spring Boot can include correlation information such as trace and span identifiers in logging output. ([Home][2])

## Collection architecture

In containers, the application commonly writes to standard output:

```text
Application stdout
        ↓
Node or sidecar log collector
        ↓
Central log storage
        ↓
Search, dashboards and alerts
```

Possible components include:

- Fluent Bit
- Fluentd
- Logstash
- OpenTelemetry Collector
- Elasticsearch/OpenSearch
- Loki
- Cloud logging platforms

## Sensitive-data controls

Never log:

- Passwords
- Access tokens
- Refresh tokens
- Session cookies
- API keys
- Card details
- Encryption keys
- Full sensitive request bodies

Instead, log safe identifiers:

```text
tokenHash=8a71...
paymentId=PAY-9182
customerId=C-102
```

## Logging levels

```text
ERROR → Operation failed and needs investigation
WARN  → Unexpected but recoverable condition
INFO  → Important business or lifecycle event
DEBUG → Diagnostic detail
TRACE → Very detailed temporary investigation
```

Do not permanently enable verbose SQL, HTTP payload, or token logging in production.

## What I would monitor

- Error rate by endpoint
- Top exception types
- Authentication failures
- Authorization failures
- Retry count
- Circuit-breaker transitions
- Slow-request logs
- Database timeout events
- Message-processing failures
- Dead-letter events

### Interview-ready answer

> I use structured logs containing service, environment, trace ID, operation, resource ID, outcome, and error code. Services write logs to standard output, and a collector forwards them to a centralized searchable platform. I correlate logs with metrics and distributed traces, apply retention and access control, redact sensitive fields, and alert on high-risk patterns rather than relying on developers to inspect individual service files.

---

# 3. How Would You Handle Database Schema Evolution?

Database schema changes must support old and new application versions running at the same time.

A rolling deployment may temporarily run multiple versions concurrently, so the database must remain compatible with both versions during the rollout. Kubernetes Deployments explicitly support multiple application versions during rolling updates. ([Kubernetes][3])

## Use the expand–migrate–contract pattern

### Phase 1: Expand

Add the new structure without removing the old structure.

```sql
ALTER TABLE customer
ADD COLUMN display_name VARCHAR(200);
```

The old version continues using:

```text
first_name
last_name
```

The new version can begin supporting:

```text
display_name
```

### Phase 2: Migrate

- Backfill existing rows.
- Deploy code capable of reading both formats.
- Write the new format.
- Monitor correctness.

```text
Old application → continues reading old columns
New application → can read old or new columns
```

### Phase 3: Contract

Only after all old versions are gone:

```sql
ALTER TABLE customer
DROP COLUMN first_name;

ALTER TABLE customer
DROP COLUMN last_name;
```

## Safe changes

Usually safer:

- Add nullable column
- Add table
- Add index carefully
- Add optional field
- Add new enum value when consumers tolerate it
- Add new API endpoint

Higher-risk changes:

- Rename a column
- Change a column type
- Make a nullable column mandatory
- Remove a column
- Change identifier semantics
- Rewrite a large table
- Add a blocking index on a large table

## Version migrations

Use a migration tool such as:

- Flyway
- Liquibase

Example:

```text
V1__create_order_table.sql
V2__add_order_status.sql
V3__add_customer_reference.sql
V4__backfill_customer_reference.sql
```

Rules:

- Never silently edit an already-applied migration.
- Add a new migration.
- Test with production-like data volume.
- Estimate lock duration.
- Keep migrations backward-compatible during rollout.
- Separate destructive changes from the first deployment.

---

# 4. How Would You Handle Event Schema Evolution?

Message contracts also evolve.

Example initial event:

```json
{
  "eventId": "E-100",
  "orderId": "O-100",
  "status": "CONFIRMED"
}
```

New version:

```json
{
  "eventId": "E-100",
  "orderId": "O-100",
  "status": "CONFIRMED",
  "confirmedAt": "2026-07-22T10:00:00Z"
}
```

Adding an optional field is usually safer than:

- Renaming an existing field
- Changing its meaning
- Changing its type
- Removing it immediately
- Reusing the field for another purpose

## Compatibility strategies

### Backward compatibility

A new consumer can read older messages.

### Forward compatibility

An older consumer can tolerate newer messages.

### Full compatibility

Both directions are supported.

## Practical controls

- Add fields rather than replacing fields.
- Supply meaningful defaults.
- Ignore unknown fields where appropriate.
- Never change the meaning of an existing field.
- Include event type and schema version.
- Use a schema registry where justified.
- Retain old consumers during migration.
- Test producer-consumer combinations.

## Do not use one domain entity as the event contract

Avoid:

```java
kafkaTemplate.send(topic, orderJpaEntity);
```

Use an explicit event contract:

```java
public record OrderConfirmedEvent(
        String eventId,
        String orderId,
        Instant confirmedAt,
        int schemaVersion
) {
}
```

This prevents persistence-model changes from unexpectedly breaking consumers.

---

# 5. How Would You Manage Configuration Across Services and Environments?

Configuration should be externalized from the application binary.

```text
Same application artifact
+
environment-specific configuration
```

Possible sources include:

- Spring Cloud Config
- Kubernetes ConfigMaps
- Environment variables
- Cloud configuration services
- Feature-flag systems
- Secret managers

Spring Cloud Config clients can import configuration using `spring.config.import`, and refresh-scoped beans can be recreated through the refresh mechanism. Spring Cloud Bus can distribute refresh events through `/actuator/busrefresh`. ([Home][4])

## Example Config Client

```yaml
spring:
  application:
    name: order-service

  profiles:
    active: production

  config:
    import: configserver:http://config-server:8888
```

## Typed configuration

```java
@ConfigurationProperties(prefix = "order")
public record OrderProperties(
        int maximumItems,
        Duration reservationTimeout
) {
}
```

Typed configuration is preferable to scattered string-based lookups because it provides:

- Grouping
- Type conversion
- Validation
- Clear ownership
- Easier testing

## Environment organization

```text
application.yml
application-dev.yml
application-test.yml
application-stage.yml
application-prod.yml

order-service.yml
order-service-stage.yml
order-service-prod.yml
```

## Secrets are different from ordinary configuration

Store normal configuration such as:

- Timeout values
- Feature flags
- Service URLs
- Retry limits
- Page-size limits

in a configuration platform.

Store secrets such as:

- Database passwords
- Private keys
- OAuth client secrets
- API credentials

in a dedicated secrets manager.

## Runtime refresh caution

Not every property should change dynamically.

Safer candidates:

- Feature flags
- Business thresholds
- Selected timeout values
- Logging levels

Riskier candidates:

- Database connection URLs
- Kafka group IDs
- Server ports
- Encryption keys
- Security filter configuration
- Thread-pool architecture

For risky infrastructure changes, prefer a controlled rolling deployment.

### Interview-ready answer

> I externalize configuration and separate shared, service-specific, environment-specific, and secret values. I use typed `@ConfigurationProperties`, version and review configuration changes, and use Spring Cloud Config or the deployment platform as appropriate. Dynamic refresh is limited to properties that are safe to rebind; infrastructure-level changes normally go through a rolling restart.

---

# 6. How Would You Handle API and Service Version Compatibility?

There are several different versions:

```text
Application build version
API contract version
Database schema version
Message schema version
Configuration version
```

They should not be treated as one value.

## Prefer backward-compatible evolution

Safer API changes:

- Add an optional response field
- Add a new endpoint
- Add an optional request field
- Add a new error code
- Add pagination metadata

Breaking changes:

- Remove a field
- Rename a field
- Change its type
- Make an optional field mandatory
- Change status-code semantics
- Change idempotency behavior
- Change the meaning of an existing enum value

## Example

Old response:

```json
{
  "id": "O-100",
  "status": "CONFIRMED"
}
```

Compatible addition:

```json
{
  "id": "O-100",
  "status": "CONFIRMED",
  "confirmedAt": "2026-07-22T10:00:00Z"
}
```

Potentially breaking change:

```json
{
  "orderIdentifier": "O-100",
  "state": 4
}
```

## Version only when necessary

Possible strategies:

```text
/api/v1/orders
/api/v2/orders
```

or:

```http
Accept: application/vnd.company.order-v2+json
```

Versioning should not replace compatibility discipline. Supporting many active versions creates:

- Testing overhead
- Security patch duplication
- Documentation complexity
- Data-model complexity
- Consumer confusion

## Compatibility process

1. Identify active consumers.
2. Publish the proposed contract.
3. Add new fields or operations.
4. Deploy providers that support old and new behavior.
5. Migrate consumers.
6. Measure old-version usage.
7. Announce deprecation.
8. Remove the old contract only after consumers have migrated.

## Contract testing

Use:

- Provider integration tests
- Consumer-driven contract tests
- OpenAPI validation
- Event schema compatibility tests
- Compatibility tests in CI
- Production traffic observation

### Interview-ready answer

> I evolve service contracts additively wherever possible and ensure providers support old and new consumers during rolling deployment. I version only genuinely breaking contracts, publish a deprecation timeline, test provider-consumer combinations, and remove old behavior only after usage confirms that consumers have migrated.

---

# 7. How Would You Refactor a Tightly Coupled System Using Design Patterns?

I would not begin by randomly adding interfaces. I would identify the specific coupling first.

## Common problems and useful patterns

| Problem                                         | Pattern                |
| ----------------------------------------------- | ---------------------- |
| Business code constructs infrastructure clients | Dependency Injection   |
| Large provider-specific integration             | Adapter                |
| Long conditional selecting algorithms           | Strategy               |
| Complex object construction                     | Builder                |
| Legacy model pollutes new model                 | Anti-Corruption Layer  |
| Caller coordinates many subsystems              | Facade                 |
| Multiple components react to an event           | Observer/domain events |
| Cross-cutting transactions or caching           | Proxy                  |
| Database logic spreads across services          | Repository             |
| Incremental replacement required                | Strangler Fig          |
| Switching implementation gradually              | Branch by Abstraction  |

## Example: tightly coupled payment logic

```java
public class CheckoutService {

    public void checkout(Order order) {
        StripeSdk sdk = new StripeSdk("secret");

        if (order.getPaymentType().equals("CARD")) {
            sdk.charge(order.getAmount());
        }
    }
}
```

Refactored port:

```java
public interface PaymentGateway {

    PaymentResult authorize(PaymentRequest request);
}
```

Adapter:

```java
@Component
public class StripePaymentGateway
        implements PaymentGateway {

    private final StripeClient client;

    public StripePaymentGateway(StripeClient client) {
        this.client = client;
    }

    @Override
    public PaymentResult authorize(
            PaymentRequest request
    ) {
        return map(
                client.authorize(map(request))
        );
    }
}
```

Service:

```java
@Service
public class CheckoutService {

    private final PaymentGateway paymentGateway;

    public CheckoutService(
            PaymentGateway paymentGateway
    ) {
        this.paymentGateway = paymentGateway;
    }

    public PaymentResult checkout(
            PaymentRequest request
    ) {
        return paymentGateway.authorize(request);
    }
}
```

Now the business logic does not depend directly on the provider SDK.

## Refactoring sequence

```text
1. Add characterization tests.
2. Introduce a stable abstraction.
3. Place an adapter around the legacy implementation.
4. Move callers to the abstraction.
5. Introduce the new implementation.
6. Switch traffic gradually.
7. Remove the old implementation.
```

This is safer than a large rewrite.

---

# 8. How Would You Design an API Gateway with Spring Cloud Gateway?

## Responsibilities

A gateway can handle:

- Routing
- TLS termination
- Authentication at the edge
- Coarse-grained authorization
- Rate limiting
- Request-size limits
- Header normalization
- Correlation IDs
- Traffic splitting
- Circuit breakers
- Protocol adaptation

Spring Cloud Gateway uses routes, predicates, and filters. It provides integrations for load balancing, circuit breakers, retries, and rate limiting. ([Home][5])

## Example route

```java
@Configuration
public class GatewayConfiguration {

    @Bean
    RouteLocator routes(
            RouteLocatorBuilder builder
    ) {
        return builder.routes()
                .route("inventory-service", route -> route
                        .path("/inventory/**")
                        .filters(filters -> filters
                                .stripPrefix(1)
                                .circuitBreaker(config -> config
                                        .setName("inventoryCircuitBreaker")
                                )
                        )
                        .uri("lb://inventory-service")
                )
                .build();
    }
}
```

The `lb://` URI lets the load-balancing infrastructure select a service instance.

## Security flow

```text
Client
  ↓
Gateway validates authentication
  ↓
Gateway applies routing and coarse controls
  ↓
Backend validates token again
  ↓
Backend enforces business authorization
```

The gateway should not be the only security boundary.

## Rate limiting

Apply limits using appropriate keys:

- Client ID
- User ID
- Tenant ID
- API key
- Source network
- Endpoint group

A rejected request commonly returns:

```http
HTTP/1.1 429 Too Many Requests
```

Spring Cloud Gateway’s rate-limiter filters support rejecting excess requests with HTTP 429. ([Home][6])

## Avoid putting business logic in the gateway

Do not place:

- Order-pricing rules
- Payment calculations
- Database writes
- Long-running workflow orchestration
- Detailed domain authorization

inside the gateway.

### Interview-ready answer

> I use Spring Cloud Gateway for routing, edge authentication, rate limiting, request limits, header normalization, resilience, and controlled traffic migration. I configure routes with predicates and filters and resolve services through discovery and load balancing. The gateway performs coarse controls, but each backend still validates authentication and enforces its own resource-level business authorization.

---

# 9. How Would You Design a System with Eventual Consistency?

Eventual consistency accepts that different services may temporarily observe different states.

## Example order workflow

```text
Order Service:
Creates order as PENDING

Inventory Service:
Reserves stock

Payment Service:
Authorizes payment

Order Service:
Moves order to CONFIRMED
```

## State machine

```text
PENDING
├── INVENTORY_RESERVED
├── PAYMENT_PENDING
├── CONFIRMED
├── CANCELLATION_PENDING
├── CANCELLED
└── MANUAL_REVIEW_REQUIRED
```

Do not represent every incomplete state as a generic failure.

## Core mechanisms

### Local transaction

Each service updates only its own database atomically.

### Transactional outbox

```text
Business update
+
Outbox event
=
One local transaction
```

### Idempotent consumer

A consumer records processed event IDs or applies naturally idempotent updates.

### Saga

Coordinates forward steps and compensations.

### Reconciliation

A scheduled process finds workflows that remain incomplete beyond an expected duration.

## Example

```text
1. Order created as PENDING.
2. OrderCreated written to outbox.
3. Publisher sends OrderCreated.
4. Inventory reserves stock.
5. Inventory publishes InventoryReserved.
6. Payment authorizes payment.
7. Order becomes CONFIRMED.
```

Failure:

```text
Payment fails
→ release inventory
→ cancel order
```

## User experience

Expose the actual state:

```json
{
  "orderId": "O-100",
  "status": "PAYMENT_PENDING"
}
```

The client can:

- Poll a status endpoint
- Receive a WebSocket update
- Receive a notification
- Consume a webhook

## Important design decisions

- Which service is authoritative for each field?
- How long may inconsistency last?
- Which operations require stronger consistency?
- What compensation is possible?
- How are stuck workflows detected?
- What requires manual recovery?

---

# 10. How Would You Design API Contracts Between Services?

A service contract should define more than a URL.

## Contract elements

- Request structure
- Response structure
- Field semantics
- Validation rules
- Authentication requirements
- Authorization scopes
- Error responses
- Idempotency behavior
- Pagination
- Timeout expectations
- Retry safety
- Rate limits
- Versioning policy

## Example error contract

```json
{
  "code": "INVENTORY_NOT_AVAILABLE",
  "message": "Requested quantity is unavailable",
  "traceId": "83ef01a2",
  "details": {
    "productId": "P-100"
  }
}
```

Avoid exposing:

- Java class names
- SQL errors
- Stack traces
- Internal hostnames
- Database structure

## Ownership

The provider owns implementation, but the contract must reflect consumer needs.

A good process includes:

1. Provider proposes a contract.
2. Consumers review semantics.
3. Contract is documented.
4. Compatibility tests are added.
5. Changes are introduced additively.
6. Deprecations are measured and communicated.

## REST vs gRPC vs messaging

| Mechanism | Suitable for                                  |
| --------- | --------------------------------------------- |
| REST      | Public APIs and standard request-response     |
| gRPC      | Strongly typed internal low-latency calls     |
| Messaging | Asynchronous workflows and event distribution |

The protocol should follow the interaction requirement rather than team preference.

---

# 11. How Would You Secure Service-to-Service Communication?

## Recommended flow

```text
Service A
   ↓ obtains OAuth access token
Authorization Server
   ↓ issues scoped token
Service A
   ↓ HTTPS + Bearer token
Service B
   ↓ validates token
Authorization decision
```

Spring Security separates OAuth support into client, resource-server, and authorization-server capabilities. A JWT resource server can validate the issuer, signature, token lifetime, and scopes from bearer tokens. ([Home][7])

## Controls

### TLS

Encrypt communication in transit.

### Mutual TLS

For high-trust internal environments, mTLS can authenticate both sides at the transport layer.

### OAuth client credentials

A service acting as itself receives a token with narrow permissions:

```text
inventory.reserve
orders.read
payments.authorize
```

Avoid:

```text
full_access
```

### Audience validation

A token intended for the inventory API should not automatically be accepted by the payment API.

### Local authorization

Service B checks:

- Trusted issuer
- Audience
- Token expiry
- Client identity
- Scope
- Tenant
- Business operation

### Secret management

Do not place client secrets or private keys in:

- Source code
- Docker images
- Plain Git configuration
- Logs
- Command-line arguments

### Token propagation

Do not blindly forward the user’s token to every downstream service.

Choose deliberately between:

- Propagating a user token
- Token exchange
- Obtaining a service token
- Passing a signed internal identity context

### Network controls

Use:

- Namespace isolation
- Network policies
- Firewall rules
- Private service endpoints
- Egress controls
- Service-mesh policies where justified

### Interview-ready answer

> I secure service communication with TLS, workload or client identity, OAuth client credentials or token exchange, narrow scopes, audience validation, and local authorization in every service. For stronger environments I add mTLS and network policies. Secrets remain in a secret manager, and tokens are never logged or forwarded to unrelated services.

---

# 12. How Would You Implement Load Balancing Between Services?

Load balancing can happen at several layers.

## External load balancing

```text
Client
  ↓
Cloud or ingress load balancer
  ↓
Gateway instances
```

## Platform-level load balancing

In Kubernetes:

```text
Calling service
   ↓
Kubernetes Service
   ↓
Ready Pods
```

## Client-side load balancing

```text
Order Service
   ↓ discovers instances
Spring Cloud LoadBalancer
   ↓ chooses one instance
Inventory Service instance
```

Spring Cloud LoadBalancer provides a load-balancing abstraction and can obtain service instances through discovery-backed suppliers. ([Home][8])

## Selection strategies

Possible strategies include:

- Round robin
- Random
- Weighted
- Zone-aware
- Least connections
- Consistent hashing

The best strategy depends on:

- Request duration
- Session state
- Instance capacity
- Geographic location
- Cache locality
- Workload variation

## Health considerations

Discovery information may become stale. A selected instance can fail after it was declared healthy.

Therefore, callers still need:

- Connection timeout
- Response timeout
- Circuit breaker
- Safe retry to another instance
- Idempotency
- Health metrics

Do not use retries to hide permanently unhealthy instances.

---

# 13. How Would You Design Fault-Tolerant Communication?

Use several controls together:

```text
Timeout
→ bounds one call

Retry
→ handles a temporary failure

Circuit breaker
→ stops repeated calls

Bulkhead
→ limits resource impact

Rate limit
→ protects capacity

Backpressure
→ prevents an unlimited backlog

Fallback
→ provides an alternative only when safe
```

## Recommended order of thinking

### 1. Set timeouts

Separate:

- Connection timeout
- Connection-pool acquisition timeout
- TLS handshake timeout
- Response timeout
- Overall operation deadline

### 2. Retry selectively

Retry only when:

- The failure is temporary.
- The operation is idempotent.
- The retry fits the caller’s total deadline.
- Attempts are bounded.
- Backoff and jitter are applied.

Do not retry:

- Invalid requests
- Authorization failures
- Business-rule failures
- Permanent not-found results
- Non-idempotent writes without protection

### 3. Add a circuit breaker

Open the circuit when failure or slow-call thresholds indicate an unhealthy dependency.

### 4. Apply bulkheads

Examples:

- Separate connection pools
- Separate executors
- Per-dependency concurrency limits
- Bounded queues

### 5. Define a truthful fallback

Good:

```text
Recommendation API unavailable
→ return cached recommendations
```

Dangerous:

```text
Inventory API unavailable
→ assume stock is available
```

### 6. Observe everything

Measure:

- Dependency latency
- Timeout rate
- Retry count
- Rejected calls
- Circuit state
- Queue length
- Pool waiting
- Fallback usage

---

# 14. How Would You Design a High-Throughput REST API?

“Millions of requests” is incomplete without a time unit.

```text
One million requests per day
≈ 12 requests per second on average

One million requests per second
= fundamentally different architecture
```

## Clarify requirements first

- Peak requests per second
- Read/write ratio
- Payload size
- Latency target
- Consistency requirement
- Geographic distribution
- Traffic shape
- Failure tolerance
- Hot-key distribution

## Architecture

```text
Clients
  ↓
CDN or edge protection
  ↓
Load balancer / gateway
  ↓
Stateless API replicas
  ↓
Cache / database / broker
```

## Main techniques

- Stateless application instances
- Horizontal scaling
- Pagination
- Bounded request bodies
- Efficient serialization
- Connection pooling
- Database indexes
- Read replicas where appropriate
- Cache hot reads
- Asynchronous processing for non-immediate work
- Idempotency for retried writes
- Backpressure
- Rate limits
- Load shedding
- Batch database operations
- Partitioning only when justified

## Avoid excessive synchronous chains

```text
Gateway
→ Service A
→ Service B
→ Service C
→ Service D
```

Total latency and failure probability accumulate across the chain.

## Validate with load testing

Measure:

- Throughput
- P50, P95 and P99 latency
- Error rate
- CPU
- Heap
- GC
- Thread pools
- Connection pools
- Queue depth
- Database waits
- Downstream saturation

---

# 15. A Service Is Slow in Production. How Do You Find the Root Cause?

Do not begin by increasing timeouts.

## Step 1: Define the symptom

- Which endpoints?
- Which customers or tenants?
- Constant or intermittent?
- P50, P95 or P99?
- When did it begin?
- Was there a code, configuration, traffic, or infrastructure change?

## Step 2: Use distributed traces

```text
Total request: 8 seconds

Gateway:            10 ms
Application logic:  30 ms
DB pool waiting:  5,000 ms
Database query:   2,900 ms
```

The trace distinguishes execution time from queueing time.

## Step 3: Check saturation

- CPU
- Memory
- GC pauses
- Request-thread pool
- Database connection pool
- HTTP connection pool
- Queue depth
- Consumer lag
- Disk I/O
- Network latency

## Step 4: Capture runtime evidence

- Thread dump
- Java Flight Recorder
- Heap histogram
- GC logs
- Database wait events
- Query execution plan
- Container throttling metrics

## Step 5: Compare normal and slow requests

Look for:

- Different input size
- Different database plan
- Cache miss
- Lock waiting
- Slow dependency
- Retry loop
- Large response
- N+1 queries
- Connection-pool wait

### Interview-ready answer

> I start with latency percentiles and distributed traces to locate where time is spent. Then I inspect saturation metrics, pool waiting, database plans, thread dumps, GC behavior, and downstream latency. I compare successful fast and successful slow requests because an eight-second response can still produce no error log.

---

# 16. How Do You Prevent Duplicate Data from Concurrent Requests?

Application-level checking is insufficient:

```text
Request A checks → row absent
Request B checks → row absent
Request A inserts
Request B inserts
```

## Use a database constraint

```sql
CREATE UNIQUE INDEX uk_order_external_reference
ON orders(external_reference);
```

The database resolves the race atomically.

## Use an idempotency key

```http
POST /payments
Idempotency-Key: payment-92831
```

Store:

```text
Idempotency key
Request fingerprint
Status
Result reference
Created time
```

Add a unique constraint on the key.

## Concurrent handling

```text
First request inserts PROCESSING
Second request receives duplicate-key conflict
        ↓
Read existing record
        ├── SUCCEEDED → return original result
        ├── PROCESSING → return pending/conflict
        └── FAILED → apply defined retry policy
```

## Bind the key to the request

```text
Same key + same payload
→ return original result

Same key + different payload
→ reject
```

---

# 17. Which Caching Strategy Would You Use?

## Cache-aside

```text
Read cache
├── Hit → return value
└── Miss
    → read database
    → populate cache
    → return value
```

This is common for read-heavy APIs.

## Write-through

Write cache and underlying store through one abstraction.

## Write-behind

Write to cache first and persist asynchronously.

This improves write latency but introduces durability and recovery complexity.

## What to cache

Good candidates:

- Read-heavy data
- Expensive computations
- Stable reference data
- Public catalog content
- Derived read models

Poor candidates:

- Highly volatile values
- Authorization decisions without careful invalidation
- Sensitive data without proper controls
- Data where stale values are unacceptable

## Invalidation approaches

- TTL
- Explicit invalidation after update
- Event-based invalidation
- Versioned cache keys
- Short TTL plus refresh
- Stale-while-revalidate

## Cache risks

### Stampede

Many requests miss the same key simultaneously.

Mitigations:

- Single-flight loading
- TTL jitter
- Request coalescing
- Locking where justified
- Stale value during refresh

### Stale data

Define the allowed staleness explicitly.

### Hot key

One key receives disproportionate traffic.

### Low hit rate

The cache exists but does not reduce database load.

Measure:

```text
hit ratio = hits / (hits + misses)
```

---

# 18. How Would You Handle Concurrent Updates to the Same Record?

## Option 1: Optimistic locking

```java
@Version
private Long version;
```

Best when conflicts are uncommon.

```text
A reads version 4
B reads version 4
A updates version 4 → 5
B updates version 4 → conflict
```

## Option 2: Pessimistic locking

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Product> findForUpdate(Long id);
```

Best for short operations with frequent contention.

Risks:

- Blocking
- Deadlocks
- Lock timeouts
- Reduced throughput

## Option 3: Atomic SQL

For inventory:

```sql
UPDATE inventory
SET available_quantity = available_quantity - :quantity
WHERE product_id = :productId
  AND available_quantity >= :quantity;
```

Check affected rows:

```text
1 → reservation succeeded
0 → insufficient stock or missing product
```

This is often better than read-modify-write for counters.

---

# 19. How Would You Optimize a Query That Became Slow?

## Investigation sequence

1. Capture the exact SQL and parameters.
2. Check execution frequency.
3. Run `EXPLAIN`.
4. Use `EXPLAIN ANALYZE` carefully.
5. Compare estimated and actual rows.
6. Inspect table scans, joins and sorts.
7. Check indexes.
8. Check lock waits.
9. Check data growth and distribution.
10. Verify statistics.
11. Measure result-set size.
12. Look for N+1 queries around it.

Common causes:

- Missing index
- Incorrect composite-index order
- Low-selectivity index
- Function on indexed column
- Implicit type conversion
- Deep offset pagination
- Large sort
- Stale statistics
- Plan change
- Lock contention
- Returning too many rows

Do not add an index without checking its write and storage cost.

---

# 20. A Downstream Service Is Unavailable. What Should Happen?

```text
Caller
  ↓
Short timeout
  ↓
Limited safe retries
  ↓
Circuit breaker
  ↓
Fallback or controlled failure
```

Possible outcomes:

### Read operation

- Return cached data.
- Return partial data clearly marked.
- Return `503 Service Unavailable`.
- Queue refresh work.

### Write operation

- Reject safely.
- Store a pending command.
- Publish to a durable queue.
- Return `202 Accepted` where asynchronous processing is part of the contract.

Do not claim success when the operation has not completed.

---

# 21. What Logging and Monitoring Strategy Would You Use?

## Metrics

Track the golden signals:

- Latency
- Traffic
- Errors
- Saturation

Also track:

- Pool usage
- Queue depth
- Consumer lag
- Cache hit ratio
- Circuit-breaker state
- Retry count
- JVM heap and GC
- Database latency

Spring Boot Actuator uses Micrometer for metrics and supports multiple monitoring backends. ([Home][9])

## Traces

Use distributed tracing to follow a request across:

```text
Gateway
→ Order Service
→ Inventory Service
→ Payment Service
```

## Logs

Use structured, correlated and redacted logs.

## Alerts

Alert on symptoms that affect users:

- High P99 latency
- Error-budget burn
- Authentication failures
- Queue age
- Consumer lag
- Failed compensations
- Database connection waiting

Avoid alerting only on CPU without user-impact context.

---

# 22. How Would You Secure REST APIs?

Use layered controls:

```text
HTTPS
+
OAuth 2.0/OIDC authentication
+
JWT or opaque-token validation
+
Scopes and roles
+
Object-level authorization
+
Input validation
+
Parameterized queries
+
Rate limiting
+
Safe logging
+
Audit events
```

Spring Security resource servers can process bearer tokens and validate JWT signature and claims through issuer configuration. ([Home][10])

Do not rely only on:

- CORS
- The API Gateway
- Hidden URLs
- JWT signature without issuer/audience checks
- Frontend authorization

---

# 23. How Would You Achieve Zero or Minimal Downtime?

## Rolling deployment

```text
Old Pods serving
        ↓
Start new Pods
        ↓
Wait until new Pods are ready
        ↓
Send traffic to new Pods
        ↓
Terminate old Pods gradually
```

Kubernetes rolling updates incrementally replace Pods and can keep the application available while the rollout proceeds. ([Kubernetes][11])

## Required controls

### Readiness probe

Only send traffic when the application is ready.

### Startup probe

Protect slow-starting applications from premature restart checks.

### Liveness probe

Restart an application that is irrecoverably unhealthy.

Kubernetes provides separate readiness, startup and liveness probes for these purposes. ([Kubernetes][12])

### Graceful shutdown

- Stop accepting new traffic.
- Stop consuming new messages.
- Complete bounded in-flight work.
- Release resources.
- Exit within the platform’s termination window.

### Backward-compatible contracts

Old and new application versions may run simultaneously.

Therefore:

- APIs must be compatible.
- Events must be compatible.
- Database schemas must support both versions.
- Configuration must not break old instances.

### Deployment strategies

| Strategy     | Use                                              |
| ------------ | ------------------------------------------------ |
| Rolling      | Gradual replacement                              |
| Blue-green   | Switch traffic between complete environments     |
| Canary       | Send a small percentage to the new version       |
| Feature flag | Separate code deployment from feature activation |

### Rollback

Rollback should include:

- Application version
- Configuration version
- Traffic routing
- Database compatibility
- Event compatibility

A destructive migration can make application rollback impossible, which is why schema contraction must happen after the old version is retired.

---

# Senior Interview Framework

For almost any scenario question, structure the answer as follows:

```text
1. Clarify requirements and scale.
2. Identify the source of truth.
3. Define consistency requirements.
4. Design the normal flow.
5. Identify partial-failure cases.
6. Add idempotency and concurrency controls.
7. Add timeouts, backpressure and resilience.
8. Secure every trust boundary.
9. Add logs, metrics, traces and audit events.
10. Explain deployment, compatibility and rollback.
11. State the trade-offs.
```

# Final Interview Summary

> I design microservices around cohesive business capabilities, data ownership, deployment independence, and clear contracts. For monolith migration, I first create modular boundaries and then use the Strangler Fig Pattern with an anti-corruption layer and controlled traffic routing. I evolve database, API, and event schemas additively so old and new versions can coexist during deployment.
>
> For consistency, I use local transactions, Saga, transactional outbox, idempotent consumers, and reconciliation. For reliability, I combine deadlines, limited retries, circuit breakers, bulkheads, load shedding, and backpressure. For operations, I use centralized structured logs, metrics, distributed tracing, readiness checks, graceful shutdown, canary or rolling deployment, and explicit rollback plans.

[1]: https://docs.spring.io/spring-boot/reference/actuator/observability.html?utm_source=chatgpt.com "Observability :: Spring Boot"
[2]: https://docs.spring.io/spring-boot/reference/features/logging.html?utm_source=chatgpt.com "Logging :: Spring Boot"
[3]: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/?utm_source=chatgpt.com "Deployments"
[4]: https://docs.spring.io/spring-cloud-config/reference/client.html?utm_source=chatgpt.com "Spring Cloud Config Client"
[5]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/circuitbreaker-filter-factory.html?utm_source=chatgpt.com "CircuitBreaker GatewayFilter Factory"
[6]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/filters/ratelimiter.html?utm_source=chatgpt.com "RateLimiter Filter :: Spring Cloud Gateway"
[7]: https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html?utm_source=chatgpt.com "OAuth2 :: Spring Security"
[8]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html?utm_source=chatgpt.com "Spring Cloud LoadBalancer"
[9]: https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com "Metrics :: Spring Boot"
[10]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server JWT"
[11]: https://kubernetes.io/docs/tutorials/kubernetes-basics/update/update-intro/?utm_source=chatgpt.com "Performing a Rolling Update"
[12]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/?utm_source=chatgpt.com "Configure Liveness, Readiness and Startup Probes"
