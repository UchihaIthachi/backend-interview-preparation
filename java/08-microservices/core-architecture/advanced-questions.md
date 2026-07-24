# Advanced Questions — Microservices Architecture and Production System Design

## Important Corrections

- **Microservices vs SOA is not simply REST vs SOAP.** Both can use REST, messaging, SOAP, or other protocols. The stronger differences are deployment independence, service size, data ownership, governance, and team autonomy.
- Applying **SOLID to microservices is an architectural analogy**. SOLID was created primarily for object-oriented design, but several principles provide useful guidance for service boundaries and contracts.
- An **Ambassador is not primarily an ingress gateway**. It is an out-of-process proxy colocated with a consuming application and handles outbound connectivity concerns. A sidecar is the broader deployment pattern through which an Ambassador may be deployed. ([Microsoft Learn][1])
- Saga compensations are not database rollbacks. They are new business operations and may themselves fail.
- Two-phase commit is not technically impossible in every distributed system, but it is often undesirable across independently deployed microservices because of coupling, blocking, availability, and operational concerns.

---

# Q1: Microservices vs SOA

Both **Service-Oriented Architecture** and microservices divide a system into services. Microservices can be viewed as a more decentralized and independently deployable style of service-oriented architecture.

## Comparison

| Area              | Traditional SOA                             | Microservices                                         |
| ----------------- | ------------------------------------------- | ----------------------------------------------------- |
| Service scope     | Often larger enterprise services            | Smaller business-capability services                  |
| Deployment        | Services may be deployed together           | Independently deployable                              |
| Integration       | Often centralized through an ESB            | Usually decentralized APIs and messaging              |
| Governance        | Central enterprise standards                | Team-level autonomy with platform standards           |
| Data ownership    | Shared enterprise databases are more common | Database ownership per service is preferred           |
| Communication     | SOAP, messaging, REST and ESB mediation     | REST, gRPC and asynchronous events are common         |
| Technology        | Usually centrally standardized              | Controlled technology diversity                       |
| Team ownership    | Shared service teams may own integration    | Product teams often own build-to-production lifecycle |
| Failure model     | Integration middleware may coordinate flows | Services must handle partial network failure          |
| Change management | Central governance and coordinated releases | Independent delivery and compatibility contracts      |

Microservices normally emphasize autonomous services aligned with bounded business contexts, each owning a focused capability and operational lifecycle. ([Microsoft Learn][2])

## When would you choose SOA?

SOA may be appropriate when:

- Integrating many existing enterprise systems
- Central governance is a major requirement
- Standardized enterprise workflows are already built around an ESB
- Legacy applications cannot be independently decomposed
- Shared enterprise services are intentionally managed centrally

## When would you choose microservices?

Microservices may be appropriate when:

- Teams require independent deployment
- Business capabilities have clear boundaries
- Different capabilities scale differently
- Release cycles differ between domains
- Strong fault isolation is valuable
- The organization has mature DevOps and observability practices

## Interview answer

> SOA and microservices both organize systems around services. Traditional SOA usually emphasizes enterprise integration and centralized governance, while microservices emphasize independently deployable services, decentralized data ownership, and autonomous teams. I choose based on organizational and operational requirements rather than assuming microservices are automatically superior.

---

# Q2: Explain the Strangler Fig Pattern

The **Strangler Fig Pattern** replaces a legacy system incrementally rather than rewriting and switching everything at once.

```text
Clients
   ↓
Routing layer
   ├── Existing functionality → Monolith
   └── Migrated functionality → New service
```

As more capabilities move to new services, the monolith becomes smaller until its remaining functionality can be retired. ([Microsoft Learn][3])

## Migration process

### Step 1: Understand the monolith

Before extracting anything:

- Identify business capabilities
- Measure runtime dependencies
- Find high-change and high-pain areas
- Examine database coupling
- Record incoming and outgoing integrations
- Identify transaction boundaries
- Add observability where it is missing

### Step 2: Introduce a routing boundary

Possible routing mechanisms include:

- API gateway
- Reverse proxy
- Backend-for-frontend
- Internal facade
- Message router

Initially, all traffic still goes to the monolith.

```text
Client → Gateway → Monolith
```

### Step 3: Select a suitable capability

Good first candidates often have:

- Clear input and output contracts
- Limited database coupling
- Independent scaling requirements
- Frequent change pressure
- Few synchronous dependencies
- A clear business owner

Avoid starting with the most deeply coupled transaction merely because it causes the most pain.

### Step 4: Build an anti-corruption layer

The new service should not inherit the monolith’s internal model directly.

```text
New service domain model
        ↓
Anti-corruption layer
        ↓
Legacy model and protocol
```

An anti-corruption layer translates between two systems without allowing the legacy model to control the design of the new domain. ([Microsoft Learn][4])

### Step 5: Route selected traffic

```text
/orders/**       → New Order Service
/legacy-admin/** → Monolith
```

Routing can be enabled gradually by:

- Tenant
- User group
- Geographic region
- Request percentage
- Feature flag
- Specific operation

### Step 6: Migrate data ownership

This is usually harder than moving code.

Possible transitional techniques include:

- API access to legacy data
- Change-data capture
- Replicated read models
- Events from the monolith
- Temporary synchronization jobs
- Database views used only during migration

Directly sharing and modifying the same tables indefinitely defeats service independence.

### Step 7: Remove the old path

After verifying:

- Functional correctness
- Performance
- Data consistency
- Monitoring
- Rollback
- Operational ownership

the old capability can be removed from the monolith.

## Rollback strategy

Keep rollback simple:

```text
New service unhealthy
        ↓
Route traffic back to monolith
```

Rollback becomes more difficult after both systems independently modify data. Therefore, plan:

- Which system owns writes
- How data written by the new service is handled after rollback
- Whether dual writes are permitted
- How reconciliation works
- How events are replayed
- Whether schema changes are backward-compatible

## Interview answer

> I use the Strangler Fig Pattern by placing a routing layer in front of the monolith, extracting one bounded capability at a time, and directing only that capability to a new service. I use anti-corruption layers to isolate the new domain model, migrate data ownership carefully, deploy gradually, and preserve a traffic-routing rollback path until the new service is proven.

---

# Q3: How do SOLID principles apply to microservices?

## Single Responsibility Principle

A service should own one cohesive business capability and have a clear set of reasons to change.

Good:

```text
Payment Service
→ payment authorization, capture and refund
```

Potentially poor:

```text
Utility Service
→ payments, reports, emails, files and users
```

This does not mean every small action becomes a service. The boundary should be cohesive enough to justify independent deployment.

## Open/Closed Principle

A service should support extension through stable contracts instead of requiring risky modification for every variation.

Examples:

- New payment provider through an adapter
- New event consumer without modifying the producer
- Strategy implementations for pricing rules
- Versioned contract evolution

## Liskov Substitution Principle

Alternative implementations of a contract must preserve its behavioral expectations.

For example, replacing one payment provider with another must not silently change:

- Idempotency guarantees
- Error semantics
- Currency precision
- Timeout behavior
- Refund rules

## Interface Segregation Principle

Do not expose one large API containing unrelated operations.

Instead of:

```text
EnterpriseService
├── createOrder
├── updateCustomer
├── refundPayment
├── generateReport
└── sendEmail
```

prefer capability-focused contracts:

```text
Order API
Payment API
Customer API
Notification API
```

Within one service, separate read, write, administration, and partner contracts when their consumers have different needs.

## Dependency Inversion Principle

Core business logic should depend on abstractions rather than infrastructure details.

```text
Order Domain
    ↓ depends on
PaymentGateway interface
    ↑ implemented by
External Provider Adapter
```

At service level, this also encourages:

- Ports and adapters
- Anti-corruption layers
- Broker abstractions
- Repository interfaces
- Provider-independent domain models

## Important warning

Using SRP does not mean:

```text
One class
=
One microservice
```

Microservice boundaries are shaped by domain cohesion, team ownership, deployment needs, data consistency, and operational cost—not only object-oriented principles.

---

# Q4: How do you define service boundaries using DDD?

Domain-Driven Design helps divide a complex domain into **bounded contexts**. A bounded context defines the boundary within which a particular domain model and terminology are valid.

Microsoft’s microservices guidance similarly recommends aligning services with business capabilities inside bounded contexts. ([Microsoft Learn][2])

## Main DDD concepts

### Ubiquitous language

Developers and domain experts use the same terminology.

For example:

```text
Order
Reservation
Payment authorization
Inventory allocation
Shipment
```

Avoid one word carrying several meanings without an explicit boundary.

### Bounded context

The same real-world concept may have different models in different contexts.

```text
Product in Catalog Context:
description, images, category

Product in Inventory Context:
SKU, quantity, warehouse, reserved stock

Product in Pricing Context:
price, discount eligibility, currency
```

These models should not be forced into one universal `Product` object.

### Aggregate

An aggregate is a consistency boundary modified as a unit.

Example:

```text
Order aggregate
├── Order
├── OrderLine
└── DeliveryAddress
```

External components interact through the aggregate root rather than modifying internal objects independently.

### Domain event

A domain event records something meaningful that happened:

```text
OrderCreated
InventoryReserved
PaymentAuthorized
OrderCancelled
```

### Anti-corruption layer

When two contexts have different models, translate between them rather than sharing internal domain objects.

## Boundary discovery process

1. Conduct domain discussions with experts.
2. Identify subdomains and business capabilities.
3. Record domain vocabulary.
4. Identify rules that must change together.
5. Identify data that must remain transactionally consistent.
6. Identify separate scaling and security requirements.
7. Map dependencies between contexts.
8. Assign ownership to teams.
9. Validate boundaries through real use cases.
10. Adjust boundaries as domain understanding improves.

## Signs of a poor boundary

- Constant synchronous calls between two services
- One feature requires coordinated deployment across several services
- One service directly updates another service’s database
- Distributed transactions dominate routine operations
- Teams repeatedly modify the same contracts together
- Data is duplicated without clear ownership
- Two services cannot explain separate business responsibilities

These may indicate that the services should be combined or their responsibility redrawn.

---

# Q5: Orchestration vs Choreography

## Orchestration

A central component controls workflow progression.

```text
Order Orchestrator
    ├── Reserve inventory
    ├── Authorize payment
    ├── Arrange shipment
    └── Confirm order
```

### Advantages

- Workflow is visible in one place
- State transitions are explicit
- Timeouts and compensations are easier to coordinate
- Operational debugging is usually clearer

### Disadvantages

- Coordinator may become a bottleneck
- It can accumulate excessive domain logic
- Participants may become tightly coupled to its commands

## Choreography

Services react to events and publish subsequent events.

```text
OrderCreated
    ↓
Inventory Service
    ↓ InventoryReserved
Payment Service
    ↓ PaymentAuthorized
Shipping Service
```

### Advantages

- Loose temporal coupling
- Natural asynchronous processing
- No central workflow coordinator
- Consumers can be added independently

### Disadvantages

- Workflow logic becomes distributed
- Event loops and hidden coupling may develop
- End-to-end state is harder to understand
- Compensation and timeout handling can become unclear

## Comparison

| Orchestration                  | Choreography                            |
| ------------------------------ | --------------------------------------- |
| Central coordinator            | Distributed event reactions             |
| Commands participants          | Participants consume events             |
| Workflow visible centrally     | Workflow spread across consumers        |
| Easier complex compensation    | Better for simple independent reactions |
| Coordinator can become coupled | Event topology can become coupled       |

## When should you choose each?

Use orchestration when:

- The workflow has many ordered steps
- There are complex timeouts
- Compensation is important
- Business users need workflow visibility
- Human approval may be involved

Use choreography when:

- Events have independent reactions
- The process is simple
- No central ordering is required
- New consumers should be added easily

A Saga can use either orchestration or choreography. ([Microsoft Learn][5])

---

# Q6: How do you handle distributed transactions?

A normal Spring transaction is local to a transaction manager and its resources.

```text
@Transactional in Order Service
        ↓
Order database only
```

It does not automatically include:

- Payment Service database
- Inventory Service database
- Kafka
- An HTTP provider
- Another JVM

## Why not use two-phase commit everywhere?

Two-phase commit coordinates participants through prepare and commit phases, but can introduce:

- Blocking participants
- Coordinator dependency
- Reduced availability
- Long-held resources
- Tight technology coupling
- Difficult recovery
- Poor fit for long-running business workflows

It can still be appropriate in limited environments with controlled transactional resources. It is generally not the default choice for independently operated microservices.

## Common approach

Use:

1. Local transaction per service
2. Saga for workflow coordination
3. Transactional outbox for reliable publication
4. Idempotent consumers
5. Compensation for completed steps
6. Reconciliation for unresolved failures

## Example Saga

```text
1. Order Service creates PENDING order
2. Inventory Service reserves stock
3. Payment Service authorizes payment
4. Order Service confirms order
```

Failure:

```text
Payment authorization fails
        ↓
Release inventory
        ↓
Cancel order
```

Saga breaks a distributed operation into local transactions and uses compensating transactions when a subsequent step fails. ([Microsoft Learn][5])

## What happens when compensation fails?

Compensation must be treated as a real distributed operation.

Use:

- Durable workflow state
- Idempotent compensation commands
- Bounded retries with backoff
- Dead-letter or recovery queue
- Clear terminal failure status
- Alerting
- Reconciliation jobs
- Manual intervention tooling

Example state model:

```text
PAYMENT_FAILED
    ↓
COMPENSATION_PENDING
    ↓
INVENTORY_RELEASE_RETRYING
    ├── success → CANCELLED
    └── exhausted → MANUAL_REVIEW_REQUIRED
```

Compensating operations can fail and must record their progress so that processing can resume. In some cases, manual intervention is the only safe recovery path. ([Microsoft Learn][6])

## Interview answer

> I use local ACID transactions inside each service and a Saga for cross-service consistency. I combine it with a transactional outbox and idempotent consumers. Compensation is a new business action, not a rollback, so I make it durable, idempotent, retryable, observable, and capable of entering a manual-recovery state.

---

# Q7: What are the Twelve-Factor App principles?

The Twelve-Factor App methodology defines practices for portable, operationally manageable applications using any language and various backing services. ([Twelve-Factor][7])

| Factor                 | Principle                                                 |
| ---------------------- | --------------------------------------------------------- |
| 1. Codebase            | One codebase tracked in version control, many deployments |
| 2. Dependencies        | Explicitly declare and isolate dependencies               |
| 3. Config              | Store deploy-specific configuration outside code          |
| 4. Backing services    | Treat databases, brokers and caches as attached resources |
| 5. Build, release, run | Keep build, release and execution stages distinct         |
| 6. Processes           | Run as stateless, share-nothing processes                 |
| 7. Port binding        | Export services through a bound port                      |
| 8. Concurrency         | Scale through the process model                           |
| 9. Disposability       | Start quickly and shut down gracefully                    |
| 10. Dev/prod parity    | Keep environments as similar as practical                 |
| 11. Logs               | Treat logs as event streams                               |
| 12. Admin processes    | Run administrative tasks as one-off processes             |

## Most critical for microservices

### Externalized configuration

```text
Same artifact
+
different deployment configuration
```

Do not build a different binary for every environment.

### Stateless processes

Persistent state belongs in an appropriate backing service rather than a specific application instance.

### Backing services as attached resources

The application consumes a database or broker through configuration rather than embedding assumptions about one physical instance.

### Logs as streams

Applications write structured events to standard output or a logging interface; the runtime collects, routes and stores them. ([Twelve-Factor][8])

### Disposability

Fast startup and graceful shutdown support:

- Autoscaling
- Rolling deployment
- Failure recovery
- Container rescheduling

## Important nuance

Stateless does not mean the entire system has no state. It means application instances should avoid owning durable state that prevents replacement or horizontal scaling.

---

# Q8: Sidecar vs Ambassador Pattern

## Sidecar

A sidecar deploys a helper component beside the main application, usually with the same lifecycle.

```text
Pod
├── Application container
└── Sidecar container
```

Common uses:

- Log forwarding
- Telemetry collection
- Configuration synchronization
- Secret renewal
- Service-mesh proxy
- Protocol adaptation

The sidecar is separately isolated but colocated with the application instance. ([Microsoft Learn][9])

## Ambassador

An Ambassador is an out-of-process proxy that sends requests on behalf of a consuming application.

```text
Application
    ↓
Local Ambassador proxy
    ↓
Remote service
```

It can handle:

- Service discovery
- Routing
- TLS or mutual TLS
- Authentication
- Retry
- Circuit breaking
- Metrics
- Protocol translation

An Ambassador is commonly deployed as a sidecar, but “Ambassador” describes its outbound connectivity responsibility while “Sidecar” describes the deployment relationship. ([Microsoft Learn][1])

## Comparison

| Sidecar                                       | Ambassador                                         |
| --------------------------------------------- | -------------------------------------------------- |
| General helper deployed beside an application | Proxy that makes outbound calls for an application |
| Describes deployment relationship             | Describes networking responsibility                |
| Can handle logs, secrets or telemetry         | Handles routing, TLS and connectivity              |
| May not proxy traffic                         | Specifically mediates remote communication         |
| Ambassador can be implemented as a sidecar    | One specialized kind of sidecar usage              |

## Real examples

### Sidecar example

An OpenTelemetry Collector runs beside a Java service and forwards logs, metrics, and traces.

### Ambassador example

A legacy Java service sends requests to `localhost:15000`. The local Ambassador:

- Discovers the actual destination
- Applies mutual TLS
- Adds tracing headers
- Uses a circuit breaker
- Forwards the request

---

# Q9: Design an e-commerce system using microservices

## Candidate services

| Service      | Responsibility                      | Owned data                      |
| ------------ | ----------------------------------- | ------------------------------- |
| Identity     | Users, authentication identities    | User identities and credentials |
| Customer     | Profiles and addresses              | Customer profile                |
| Catalog      | Product descriptions and categories | Product catalog                 |
| Pricing      | Prices and discount rules           | Price and promotion data        |
| Inventory    | Available and reserved stock        | Stock by SKU and warehouse      |
| Cart         | Shopping-cart state                 | Active carts                    |
| Order        | Order lifecycle                     | Orders and line-item snapshots  |
| Payment      | Authorization, capture and refund   | Payment transactions            |
| Fulfilment   | Picking and shipment workflow       | Fulfilment state                |
| Notification | Email, SMS and push delivery        | Templates and delivery records  |
| Search       | Search-optimized product view       | Derived search index            |

## Data ownership

Do not query the Catalog database from the Order Service.

At checkout, the Order Service stores a snapshot of relevant information:

```text
Product ID
Product name at purchase
Unit price at purchase
Quantity
Tax
Discount
```

This preserves historical accuracy even when catalog data changes later.

## Checkout workflow

```text
Client sends idempotency key
        ↓
Order Service creates PENDING order
        ↓
Inventory Service reserves stock
        ↓
Payment Service authorizes payment
        ↓
Order becomes CONFIRMED
        ↓
OrderConfirmed event
        ├── Fulfilment starts
        ├── Notification sends receipt
        └── Analytics updates
```

## Failure paths

### Stock unavailable

```text
Order → REJECTED
```

### Payment fails after reservation

```text
Release inventory
Cancel order
```

### Response lost after successful payment

The idempotency key allows the client to retrieve the original result rather than creating another charge.

### Notification fails

Do not roll back a valid order merely because an email failed. Retry notification independently.

## Architecture principles

- Local transactions
- Saga for checkout
- Outbox for event publication
- Idempotency for payment and order creation
- Read models for search and customer history
- Event versioning
- Explicit service ownership
- Timeouts, circuit breakers and bulkheads
- End-to-end tracing

---

# Q10: Microservices vs monolith for a startup with five engineers

For a five-engineer startup, a **modular monolith** is usually the safer initial choice unless there is a concrete reason for independent services.

## Why?

A microservice architecture introduces work that does not directly create product features:

- Multiple deployment pipelines
- Service discovery
- Distributed tracing
- Contract management
- Network-security configuration
- Event infrastructure
- Distributed consistency
- More environments
- More dashboards and alerts
- Cross-service testing
- Incident coordination

## Recommended starting structure

```text
One deployable application
├── identity module
├── catalog module
├── order module
├── payment module
└── notification module
```

Enforce:

- Explicit module APIs
- No circular dependencies
- Clear domain packages
- Separate database schemas where useful
- Domain events between modules
- Ownership boundaries
- Tests that prevent unauthorized module access

## Extract a microservice when evidence appears

Good extraction triggers include:

- One module needs independent scaling
- Different security isolation is required
- A separate team owns the capability
- Its deployment frequency differs significantly
- Failure isolation provides clear value
- Technology requirements genuinely differ
- The module has a stable business boundary

## Interview answer

> For five engineers, I would normally start with a modular monolith. It gives fast development, simple deployment, local transactions, and easier debugging while preserving domain boundaries. I would extract services only when independent scaling, deployment, ownership, security, or reliability requirements justify the distributed-system cost.

---

# Senior Spring Boot and Microservices Questions

## 1. How does JWT authentication actually work?

```text
1. User or client authenticates with an authorization server.
2. The authorization server issues an access token.
3. Client sends Authorization: Bearer <token>.
4. Resource server verifies signature and claims.
5. Claims are mapped to an authenticated principal.
6. Authorization rules decide whether the operation is allowed.
```

A JWT is a claims format; it may be signed, MAC-protected, encrypted, or nested according to the JWT standard. ([IETF Datatracker][10])

A resource server should validate:

- Signature
- Allowed algorithm
- Issuer
- Audience
- Expiration
- Not-before time
- Token type where applicable
- Required scopes or roles

Do not use an OIDC ID token as an API access token.

---

## 2. OAuth 2.0 vs JWT

This is a common interview trap.

```text
OAuth 2.0
→ Authorization framework

JWT
→ Token format
```

OAuth access tokens can be:

- JWTs
- Opaque strings
- Other formats understood by the authorization server

A JWT can be used outside OAuth entirely.

Therefore:

> OAuth 2.0 and JWT are not competing alternatives. OAuth defines how authorization is delegated and tokens are obtained; JWT is one possible representation of a token.

---

## 3. `RestTemplate` vs `RestClient` vs `WebClient`

| Client         | Model                 | Recommended usage                |
| -------------- | --------------------- | -------------------------------- |
| `RestTemplate` | Blocking              | Existing legacy applications     |
| `RestClient`   | Blocking, fluent      | New synchronous code             |
| `WebClient`    | Reactive/non-blocking | Reactive pipelines and streaming |

Current Spring Framework 7 documentation deprecates `RestTemplate` in favour of `RestClient`; this deprecation is version-specific, so older Spring applications can continue maintaining existing `RestTemplate` code while planning migration appropriately. ([Home][11])

Use `WebClient` when the entire flow benefits from non-blocking processing. Calling `.block()` turns that part of the flow back into blocking execution.

---

## 4. How do two microservices communicate?

### Synchronously

- REST
- gRPC

Use when the caller needs an immediate response.

Always configure:

- Connection timeout
- Response timeout
- Bounded connection pool
- Circuit breaker
- Safe retry rules
- Tracing
- Authentication

### Asynchronously

- Kafka
- RabbitMQ
- Cloud queues

Use when:

- Immediate response is unnecessary
- Buffering is useful
- Consumers should be independent
- Temporary unavailability should be tolerated

Messaging requires:

- Idempotent consumers
- Schema evolution
- Retry and dead-letter handling
- Ordering strategy
- Monitoring
- Reconciliation

---

## 5. What does an API Gateway earn its place doing?

Useful gateway responsibilities include:

- Routing
- TLS termination
- Authentication at the edge
- Rate limiting
- Request-size enforcement
- Coarse authorization
- Correlation IDs
- Protocol transformation
- Client-specific aggregation
- Gradual traffic migration

A gateway should generally not contain:

- Core business rules
- Database access
- Every service’s detailed permissions
- Long-running orchestration
- A large shared domain model

Each service must still enforce its own authorization.

---

## 6. Global exception handling

Spring provides controller advice that can apply exception-handling methods across controllers. ([Home][12])

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
            OrderNotFoundException exception
    ) {
        ApiError error = new ApiError(
                "ORDER_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(
            Exception exception
    ) {
        String errorId = UUID.randomUUID().toString();

        log.error(
                "Unexpected error. errorId={}",
                errorId,
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        "INTERNAL_ERROR",
                        "Unexpected error. Reference: " + errorId
                ));
    }
}
```

Good error responses should contain:

- Stable error code
- Safe user-facing message
- Trace or error reference
- Field validation details where appropriate

Do not expose stack traces or infrastructure details.

---

## 7. How does `@Transactional` work internally?

Spring commonly creates an AOP proxy around the bean.

```text
Caller
  ↓
Transactional proxy
  ↓
Start or join transaction
  ↓
Call target method
  ↓
Commit or roll back
```

The proxy applies transaction propagation, isolation, timeout and rollback rules before and after the target method.

---

## 8. Why does self-invocation skip `@Transactional`?

```java
@Service
public class PaymentService {

    public void process() {
        savePayment();
    }

    @Transactional
    public void savePayment() {
        // Database work
    }
}
```

The internal call is effectively:

```java
this.savePayment();
```

It does not pass through the Spring proxy, so `savePayment()` does not receive a new transactional interception. Spring’s default proxy mode intercepts calls entering through the proxy, not self-invocation within the target object. ([Home][13])

Preferred correction:

```java
@Service
public class PaymentWriter {

    @Transactional
    public void savePayment() {
        // Database work
    }
}
```

---

# Idempotency and Consistency

## Scenario: A user double-clicks Pay. How do you prevent a double charge?

Require an idempotency key:

```http
POST /payments
Idempotency-Key: checkout-84391-payment
```

Store:

```text
Idempotency key
Request fingerprint
Processing status
Result or payment ID
Created time
Expiration
```

Add a database uniqueness constraint:

```sql
CREATE UNIQUE INDEX uk_payment_idempotency_key
ON payment_request(idempotency_key);
```

Processing logic:

```text
Insert PROCESSING record
    ├── insert succeeds → perform payment
    └── duplicate key → inspect existing record

Existing SUCCEEDED
→ return stored response

Existing PROCESSING
→ return 409/202 or wait safely

Existing FAILED_RETRYABLE
→ retry according to policy
```

The unique constraint is critical because two concurrent requests can both pass an application-level “does it exist?” check.

## Request fingerprint

Bind the key to the request content.

```text
Same key + same amount/order
→ return original result

Same key + different amount/order
→ reject request
```

Otherwise, a client could accidentally reuse one key for two different payments.

## Payment-provider idempotency

Pass a stable idempotency key to the external provider too. Local deduplication alone cannot prevent duplicate provider calls after a crash between the remote success and local commit.

---

## Scenario: Same request is sent twice. How do you return the same result?

Persist the first completed response or a stable resource reference.

```text
Request 1:
Process → Payment ID P100 → store result

Request 2 with same key:
Read stored result → return Payment ID P100
```

Idempotency concerns the intended effect. The exact HTTP status or response timestamp does not have to be byte-for-byte identical.

---

## Scenario: A transaction spans several microservices

Use:

```text
Local transaction
+
Saga
+
Transactional outbox
+
Idempotent consumers
+
Compensation
+
Reconciliation
```

Do not place one database transaction around network calls and assume it creates cross-service atomicity.

---

# Production Scenarios

## Scenario 1: Service A calls Service B. B is down and A keeps retrying

A circuit breaker is part of the solution, but not the whole solution.

Use:

1. Short connection and response timeouts
2. Limited retries for transient failures
3. Exponential backoff and jitter
4. Circuit breaker
5. Bulkhead or concurrency limit
6. Load shedding
7. Fallback only when business-safe

```text
Timeout
→ bounds one call

Retry
→ handles temporary failure

Circuit breaker
→ stops repeated calls

Bulkhead
→ protects A's resources
```

Unlimited or synchronized retries can create a retry storm and increase pressure on the failing service. Retries should be bounded and combined with circuit breaking and telemetry. ([Microsoft Learn][14])

---

## Scenario 2: Redis is working, but database load remains at 100%

Possible causes include:

### Low cache hit ratio

The cache exists, but most requests miss it.

Measure:

```text
hits / (hits + misses)
```

### Cache key mismatch

Equivalent requests generate different cache keys.

### Short TTL

Entries expire before they provide value.

### Cache stampede

Many requests miss the same key simultaneously and all query the database.

Use:

- Single-flight loading
- Distributed lock where justified
- Stale-while-revalidate
- TTL jitter
- Request coalescing

### N+1 queries

The top-level object is cached, but related data still triggers many queries.

### Queries bypass the cache

Some endpoints or repository methods may not use the cached access path.

### Write-heavy workload

Caching reads does not reduce database updates.

### Cache invalidation removes entries too aggressively

Every update may invalidate a large region.

### Database load comes from unrelated tasks

Examples:

- Reports
- Batch jobs
- Migrations
- Cleanup jobs
- Replication
- Missing indexes

## Diagnostic order

1. Measure hit ratio by cache and key type.
2. Identify top database queries.
3. Compare cache misses with query volume.
4. Inspect TTL and eviction rates.
5. Look for stampedes.
6. Trace one representative request.
7. Check N+1 behavior.
8. Inspect background workloads.

---

## Scenario 3: Response time rises from 20 ms to 8 seconds under load, but there are no errors

This usually indicates **queueing or resource saturation** rather than explicit failures.

Check:

- Request queue length
- Thread-pool active and queued tasks
- Database connection-pool waiting
- HTTP client connection-pool waiting
- Database locks
- CPU saturation
- Garbage-collection pauses
- Disk and network latency
- Downstream P95/P99 latency
- Executor rejection or hidden blocking
- Queue and consumer lag

A dependency may still return successfully after eight seconds, so no error appears in logs.

## Investigation approach

```text
1. Reproduce with controlled load.
2. Compare normal and slow distributed traces.
3. Locate the span containing the extra time.
4. Inspect pool and queue metrics for that time.
5. Capture thread dumps during the spike.
6. Check database waits and execution plans.
7. Verify GC and CPU behaviour.
8. Test one hypothesis at a time.
```

Average latency can hide this problem. Inspect percentiles such as P95 and P99.

---

## Scenario 4: A feature was deployed Friday. Everything is slow by Monday, although no code changed over the weekend

Look for a **time-dependent resource problem**.

Possible causes:

### Memory leak

Retained objects grow gradually, increasing GC work.

### Thread or connection leak

Resources are acquired but not released.

### Unbounded queue

Background work accumulates faster than it is processed.

### Cache growth or poor eviction

The cache consumes memory or causes longer lookup and GC behaviour.

### Cache expiry wave

Many entries expire together, causing a database surge.

### Scheduled or batch jobs

Weekend reconciliation, reporting, cleanup, or indexing may overlap.

### Disk growth

Logs, temporary files, or database storage may fill disks and increase I/O latency.

### Consumer lag

A message backlog may build gradually.

### Data-volume threshold

A query may change execution plan or begin spilling after enough rows accumulate.

### External changes

- Dependency slowdown
- DNS or certificate issues
- Infrastructure scaling change
- Credential or token renewal issue
- Cloud quota
- Traffic growth

## Diagnostic process

Compare Friday and Monday:

| Signal      | What to inspect                    |
| ----------- | ---------------------------------- |
| Heap        | Growth and retained objects        |
| GC          | Frequency and pause duration       |
| Threads     | Count and blocked states           |
| Connections | Active, idle and waiting           |
| Queues      | Size and oldest item age           |
| Database    | Top queries, locks and storage     |
| Cache       | Entry count, hit rate and eviction |
| Disk        | Capacity and I/O latency           |
| Messages    | Consumer lag and retries           |
| Traffic     | Volume and request mix             |

## Senior interview answer

> I would not begin by rolling back blindly. I would correlate the degradation with deployment time, inspect trends rather than one snapshot, and look for resources that accumulate over time: heap, threads, connections, queues, disk, cache entries and consumer lag. Then I would use traces, thread dumps, heap analysis and database metrics to identify where the elapsed time is being spent.

---

# Senior-Level Interview Summary

> I define microservices around bounded business contexts, data ownership, deployment independence, and team responsibility—not around tables or controllers. I prefer a modular monolith when distributed boundaries are not yet justified. For migrations, I use the Strangler Fig Pattern with routing, anti-corruption layers, incremental data ownership, and rollback.
>
> Across services, I use local transactions, Saga, transactional outbox, idempotent consumers, compensation and reconciliation rather than expecting one `@Transactional` annotation to create distributed ACID. For reliability, I combine timeouts, bounded retries, circuit breakers, bulkheads and backpressure. In production diagnosis, I focus on queueing, resource saturation, percentiles, distributed traces and time-dependent leaks rather than relying only on application error logs.

[1]: https://learn.microsoft.com/en-us/azure/architecture/patterns/ambassador?utm_source=chatgpt.com "Ambassador Pattern - Azure Architecture Center"
[2]: https://learn.microsoft.com/en-us/azure/architecture/guide/architecture-styles/microservices?utm_source=chatgpt.com "Microservices Architecture Style - Azure Architecture Center"
[3]: https://learn.microsoft.com/en-us/azure/architecture/microservices/design/patterns?utm_source=chatgpt.com "Design Patterns for Microservices - Azure Architecture Center"
[4]: https://learn.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer?utm_source=chatgpt.com "Anti-Corruption Layer Pattern - Azure Architecture Center"
[5]: https://learn.microsoft.com/en-us/azure/architecture/patterns/saga?utm_source=chatgpt.com "Saga Design Pattern - Azure Architecture Center"
[6]: https://learn.microsoft.com/en-us/azure/architecture/patterns/compensating-transaction?utm_source=chatgpt.com "Compensating Transaction Pattern - Azure"
[7]: https://www.12factor.net/?utm_source=chatgpt.com "The Twelve-Factor App"
[8]: https://12factor.net/logs?utm_source=chatgpt.com "Treat logs as event streams"
[9]: https://learn.microsoft.com/en-us/azure/architecture/patterns/sidecar?utm_source=chatgpt.com "Sidecar Pattern - Azure Architecture Center"
[10]: https://datatracker.ietf.org/doc/html/rfc7519?utm_source=chatgpt.com "RFC 7519 - JSON Web Token (JWT) - Datatracker - IETF"
[11]: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com "REST Clients :: Spring Framework"
[12]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/ControllerAdvice.html?utm_source=chatgpt.com "ControllerAdvice (Spring Framework 7.0.7 API)"
[13]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com "Using @Transactional :: Spring Framework"
[14]: https://learn.microsoft.com/en-us/azure/architecture/patterns/circuit-breaker?utm_source=chatgpt.com "Circuit Breaker Pattern - Azure Architecture Center"
