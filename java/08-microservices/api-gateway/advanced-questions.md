# Advanced Questions — API Design and Inter-Service Communication

## Important Corrections

- REST defines **six** constraints: client–server, statelessness, cacheability, uniform interface, layered system, and optional code-on-demand.
- HATEOAS is part of REST’s **uniform interface**, not simply “adding some links.”
- gRPC commonly uses Protocol Buffers, but “binary always means faster” is too broad; serialization, payload size, network conditions, and implementation still matter.
- An idempotent operation produces the same intended business effect when repeated; responses do not have to be byte-for-byte identical.
- A correlation ID, trace ID, message ID, and idempotency key solve different problems.
- Contract tests complement integration tests; they do not replace integration, security, performance, or end-to-end tests.

---

# Q21: What are REST constraints? How does HATEOAS improve API discoverability?

REST is an architectural style defined by a set of constraints.

## The six REST constraints

### 1. Client–server

The user interface and data-management responsibilities are separated.

```text
Client
  ↓ HTTP representation
Server
```

This allows clients and servers to evolve independently as long as their contract remains compatible.

### 2. Statelessness

Every request must contain the information required to process it.

```text
Request 1:
Authorization + request data

Request 2:
Authorization + request data
```

The server may store persistent application state, such as orders and users. “Stateless” means it does not rely on hidden conversational request state stored for a particular client between requests.

### 3. Cacheability

Responses indicate whether they may be cached.

```http
Cache-Control: public, max-age=300
ETag: "order-12-v4"
```

Caching can reduce latency and server load, but stale-sensitive data must use appropriate cache directives.

### 4. Uniform interface

The uniform interface includes:

- Resource identification
- Resource manipulation through representations
- Self-descriptive messages
- Hypermedia as the engine of application state

### 5. Layered system

The client does not need to know whether it communicates directly with the final server or through:

- A proxy
- An API Gateway
- A cache
- A load balancer

### 6. Code on demand — optional

A server may send executable code that extends client behaviour. This is optional and is more visible in the Web than in typical JSON APIs.

These constraints originate from Fielding’s REST architectural style. ([UCI Bren School of ICS][1])

---

## What is HATEOAS?

HATEOAS means:

> Hypermedia as the Engine of Application State.

The server includes links and available actions in a representation so the client can discover valid next operations.

Without HATEOAS:

```json
{
  "id": "O-100",
  "status": "PENDING"
}
```

The client must already know that cancellation is performed through:

```text
POST /orders/O-100/cancel
```

With HATEOAS:

```json
{
  "id": "O-100",
  "status": "PENDING",
  "_links": {
    "self": {
      "href": "/orders/O-100"
    },
    "cancel": {
      "href": "/orders/O-100/cancellation"
    },
    "payment": {
      "href": "/orders/O-100/payment"
    }
  }
}
```

After cancellation, the response may no longer contain the `cancel` link:

```json
{
  "id": "O-100",
  "status": "CANCELLED",
  "_links": {
    "self": {
      "href": "/orders/O-100"
    }
  }
}
```

The available links reflect the resource’s current state.

Spring HATEOAS provides models and assemblers for building link-driven representations, including HAL representations. ([Home][2])

## Benefits

- Clients discover related resources.
- Available operations can reflect current state.
- Clients depend less on hard-coded URL construction.
- Links can evolve independently from internal routing.
- API exploration becomes easier.

## Trade-offs

- Responses become larger.
- Clients must understand the hypermedia format.
- Generating links adds implementation effort.
- Many internal APIs gain limited value because both sides already share contracts.
- Adding static links alone does not create a fully hypermedia-driven client.

### Interview-ready answer

> REST has six constraints: client–server, statelessness, cacheability, uniform interface, layered system, and optional code-on-demand. HATEOAS belongs to the uniform-interface constraint. It places links and available actions in resource representations so clients can discover valid next operations rather than hard-coding every endpoint.

---

# Q22: Explain gRPC vs REST vs GraphQL

## Comparison

| Area              | REST                              | gRPC                                                 | GraphQL                          |
| ----------------- | --------------------------------- | ---------------------------------------------------- | -------------------------------- |
| Interaction model | Resource-oriented HTTP            | Remote procedure calls                               | Client-selected graph queries    |
| Contract          | OpenAPI commonly used             | `.proto` definition                                  | GraphQL schema                   |
| Common encoding   | JSON                              | Protocol Buffers                                     | Usually JSON                     |
| Transport         | Commonly HTTP/1.1 or HTTP/2       | Commonly HTTP/2                                      | Commonly HTTP                    |
| Streaming         | Possible, but not the basic model | Native client, server and bidirectional streaming    | Subscriptions are possible       |
| Browser support   | Excellent                         | Requires gRPC-Web or translation for normal browsers | Excellent                        |
| Client control    | Server defines representation     | Procedure defines response type                      | Client chooses selected fields   |
| Best fit          | Public and standard web APIs      | Controlled internal systems                          | Client-specific data composition |

---

## REST

REST is suitable when:

- The API is public.
- Browser and HTTP-tool compatibility matter.
- HTTP caching semantics are valuable.
- Resources and standard HTTP methods fit the domain.
- Human-readable JSON is useful.

Example:

```http
GET /orders/O-100
Accept: application/json
```

---

## gRPC

gRPC normally starts with a Protocol Buffer service definition:

```protobuf
syntax = "proto3";

service InventoryService {
  rpc GetInventory(GetInventoryRequest)
      returns (GetInventoryResponse);

  rpc StreamInventory(InventoryRequest)
      returns (stream InventoryUpdate);
}
```

The compiler generates client and server types from the contract.

gRPC supports:

- Unary calls
- Client streaming
- Server streaming
- Bidirectional streaming
- Strongly typed generated clients
- Deadlines
- Metadata
- Standardized RPC status codes

gRPC’s core model uses `.proto` service definitions and generated client/server code, with HTTP/2-based streaming support. ([gRPC][3])

### When would I choose gRPC?

I would consider gRPC when:

- Communication is internal and both client and server are controlled.
- Strong contracts and generated clients are valuable.
- Low-overhead serialization matters.
- There are high request volumes with relatively small messages.
- Client, server, or bidirectional streaming is required.
- Services use different programming languages supported by gRPC.
- Browser accessibility is not the primary concern.

I would not choose it merely because it is described as “faster.” Operational tooling, debugging, proxies, client support, payload characteristics, and team experience also matter.

---

## GraphQL

GraphQL exposes a typed schema and allows the client to select the required fields:

```graphql
query {
  order(id: "O-100") {
    id
    status
    customer {
      displayName
    }
    lineItems {
      productName
      quantity
    }
  }
}
```

The response follows the requested shape:

```json
{
  "data": {
    "order": {
      "id": "O-100",
      "status": "CONFIRMED",
      "customer": {
        "displayName": "Harshana"
      },
      "lineItems": [
        {
          "productName": "Keyboard",
          "quantity": 1
        }
      ]
    }
  }
}
```

The GraphQL specification requires clients to select fields down to leaf values, producing an explicitly shaped response. ([GraphQL Specification][4])

### Advantages

- Reduces over-fetching and under-fetching.
- Useful when web and mobile clients need different views.
- Strong schema and introspection support.
- Can aggregate several backend data sources.

### Risks

- Complex query-cost control
- Resolver-level N+1 problems
- More difficult HTTP caching
- Field-level authorization complexity
- Large or deeply nested queries
- Schema governance requirements

### Interview-ready answer

> REST is usually the best default for public, resource-oriented HTTP APIs. gRPC is strong for controlled internal communication requiring generated contracts, efficient encoding, deadlines, or streaming. GraphQL is useful when clients need flexible, client-selected data from a unified schema, but it requires query-cost controls, resolver optimization, and field-level authorization.

---

# Q23: How do you version a REST API?

The first choice should be **backward-compatible evolution**, not immediately creating a new version.

Usually compatible changes include:

- Adding an optional response field
- Adding an optional request field
- Adding a new endpoint
- Adding a new link
- Adding a new error field

Usually breaking changes include:

- Removing or renaming fields
- Changing a field type
- Changing field meaning
- Making an optional field mandatory
- Changing idempotency behaviour
- Changing status-code semantics

---

## 1. URI versioning

```http
GET /api/v1/orders/O-100
GET /api/v2/orders/O-100
```

### Advantages

- Clear and visible
- Easy to route
- Easy to test from a browser
- Works well with gateways and documentation

### Disadvantages

- Treats version as part of the resource URI.
- Can lead to duplicated controllers.
- Clients may remain on old versions indefinitely.
- Supporting many versions becomes expensive.

---

## 2. Query-parameter versioning

```http
GET /api/orders/O-100?api-version=2
```

### Advantages

- Easy to introduce
- Resource path remains stable
- Simple for optional experimental behaviour

### Disadvantages

- Less visible than URI versioning
- Cache keys must include the parameter correctly
- Can produce unclear routing rules
- Often less conventional for major API contracts

---

## 3. Custom-header versioning

```http
GET /api/orders/O-100
X-API-Version: 2
```

### Advantages

- Keeps the URI clean
- Separates representation/version concerns from resource identity

### Disadvantages

- Harder to test manually
- Less visible in logs and browser navigation
- Requires correct cache variation
- Custom headers require additional documentation

---

## 4. Media-type or content-negotiation versioning

```http
Accept: application/vnd.company.orders.v2+json
```

### Advantages

- Expresses the version as a representation format
- Fits HTTP content negotiation
- Resource URI remains stable

### Disadvantages

- More complicated for consumers
- Harder to debug manually
- Gateway and cache configuration becomes more involved
- Documentation must be very clear

---

## Recommended lifecycle

```text
1. Introduce additive behaviour.
2. Support old and new consumers.
3. Publish the breaking version when necessary.
4. Measure old-version usage.
5. Announce a deprecation period.
6. Migrate consumers.
7. Remove the old version only after verification.
```

Google’s API design guidance similarly treats API versions as part of a managed compatibility lifecycle rather than as a substitute for compatibility. ([Google Cloud Documentation][5])

### Interview-ready answer

> I prefer additive, backward-compatible changes and introduce a new version only for a genuine breaking contract. URI versioning is the most visible and easiest to operate; header or media-type versioning keeps URLs stable but is harder to inspect and cache. Regardless of the strategy, I define deprecation, migration, usage monitoring, and removal policies.

---

# Q24: What is idempotency? How do you implement it in payments?

An operation is idempotent when repeating the same logical request produces no additional intended business effect.

```text
One payment request
=
The same payment request retried several times
=
One charge
```

A duplicate request may return a different timestamp or HTTP status while still being idempotent. The important guarantee is that the charge is not duplicated.

---

## Idempotency-key flow

```http
POST /payments
Idempotency-Key: checkout-81-payment
Content-Type: application/json
```

```json
{
  "orderId": "O-100",
  "amount": 125.0,
  "currency": "USD"
}
```

Store a record such as:

```text
idempotency_key
request_hash
status
payment_id
response_body
created_at
expires_at
```

Database protection:

```sql
CREATE UNIQUE INDEX uk_payment_idempotency_key
ON payment_request(idempotency_key);
```

## Processing flow

```text
Receive key and payload
        ↓
Calculate request hash
        ↓
Try to create PROCESSING record
        ├── Insert succeeds
        │      ↓
        │   Process payment
        │      ↓
        │   Store result
        │
        └── Duplicate key
               ↓
            Load record
               ├── Same hash + SUCCEEDED
               │      → return stored result
               ├── Same hash + PROCESSING
               │      → return pending/conflict
               └── Different hash
                      → reject key reuse
```

## Response behaviour

There is no universal response rule.

A practical contract might be:

| Situation                   | Possible result                   |
| --------------------------- | --------------------------------- |
| First successful request    | `201 Created` or `200 OK`         |
| Completed duplicate         | Return original successful result |
| Same key, different payload | `409 Conflict`                    |
| Original still processing   | `202 Accepted` or `409 Conflict`  |
| Previous retryable failure  | Follow documented retry rules     |

## External payment provider problem

Consider:

```text
Provider charge succeeds
        ↓
Application crashes
        ↓
Local result was not recorded
```

Local idempotency alone cannot fully solve this.

Use:

- The same idempotency key at the payment provider
- A stable merchant transaction reference
- Provider-status lookup
- Reconciliation
- Durable workflow state

## TTL cleanup

Idempotency records can eventually expire, but the TTL must be longer than the maximum realistic retry window. Financial records or transaction references may require much longer retention than ordinary API deduplication entries.

### Interview-ready answer

> I require an idempotency key, bind it to a hash of the request, and protect it with a database uniqueness constraint. The first request reserves the key and stores the final response. Concurrent duplicates either receive the stored result or a documented processing response. Reusing the key with a different payload returns a conflict. For external payments, I also pass a stable idempotency key to the provider and run reconciliation for crash windows.

---

# Q25: How do you implement rate limiting at an API Gateway?

Rate limiting restricts how many requests a client may make over a period.

## Common algorithms

### Fixed window

```text
100 requests per minute
```

Simple, but clients can burst near a window boundary.

### Sliding window

Counts requests over a moving interval.

More accurate, but requires more state and computation.

### Token bucket

Tokens are replenished at a fixed rate.

```text
Bucket capacity: 20
Replenishment: 10 tokens/second
Each request consumes one token
```

It permits controlled bursts.

### Leaky bucket

Requests leave at a controlled rate, smoothing traffic.

---

## Choosing the rate-limit key

Possible keys include:

- User ID
- OAuth client ID
- Tenant ID
- API key
- IP address
- Route
- Operation type

A production system may combine limits:

```text
Per user:
100 requests/minute

Per tenant:
10,000 requests/minute

Per payment operation:
5 requests/minute

Global:
50,000 requests/second
```

Do not trust arbitrary `X-Forwarded-For` values unless they came through a trusted proxy that sanitizes forwarding headers.

---

## Spring Cloud Gateway example

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: order-service
              uri: lb://order-service
              predicates:
                - Path=/api/orders/**
              filters:
                - name: RequestRateLimiter
                  args:
                    key-resolver: "#{@userKeyResolver}"
                    redis-rate-limiter.replenishRate: 10
                    redis-rate-limiter.burstCapacity: 20
                    redis-rate-limiter.requestedTokens: 1
```

Reactive key resolver:

```java
@Bean
KeyResolver userKeyResolver() {
    return exchange -> exchange.getPrincipal()
            .map(Principal::getName)
            .defaultIfEmpty("anonymous");
}
```

Spring Cloud Gateway’s `RequestRateLimiter` rejects blocked requests with `429 Too Many Requests` by default, and its Redis implementation supports token-bucket-style distributed limiting. ([Home][6])

A rejected response may include:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 10
```

## Production concerns

- Redis or limiter availability
- Atomic counter updates
- Multi-region consistency
- Clock behaviour
- Hot tenants
- Internal traffic bypassing the gateway
- Trusted identity extraction
- Different cost per endpoint
- Monitoring rejected traffic

### Fail-open vs fail-closed

When the rate-limit store is unavailable:

- **Fail open:** preserve availability, but lose protection.
- **Fail closed:** preserve strict protection, but reject legitimate traffic.

The correct decision depends on the operation. A public search endpoint and a high-risk payment endpoint may use different policies.

### Interview-ready answer

> I select an algorithm such as token bucket, choose a meaningful identity key, and maintain the counters in a distributed store such as Redis. I apply user, tenant, route, and global limits where needed and return `429` with retry guidance. I also define what happens when the limiter is unavailable and monitor rejection rates to distinguish abuse from insufficient capacity.

---

# Q26: Synchronous vs asynchronous inter-service communication

## Synchronous communication

The caller waits for the result.

```text
Order Service
    ↓ HTTP/gRPC request
Inventory Service
    ↓ response
Order Service continues
```

Examples:

- REST
- gRPC unary call

### Advantages

- Simple request-response model
- Immediate result
- Easier sequential business logic
- Easier for queries and validation

### Disadvantages

- Temporal coupling
- Caller waits for the dependency
- Latency accumulates across call chains
- Partial failures affect the request
- Threads, streams, and connections remain occupied

Use it when:

- The result is immediately required.
- The dependency is part of the user’s response.
- The interaction is short and predictable.
- The consistency requirement justifies waiting.

---

## Asynchronous communication

The producer publishes a message and does not wait for the consumer to finish.

```text
Order Service
    ↓ OrderCreated
Message broker
    ├── Inventory Consumer
    ├── Notification Consumer
    └── Analytics Consumer
```

Examples:

- Kafka
- RabbitMQ
- Cloud queues

### Advantages

- Loose temporal coupling
- Broker can absorb traffic spikes
- Consumer can recover independently
- Multiple consumers can react
- Good for long-running workflows

### Disadvantages

- Eventual consistency
- Duplicate delivery
- Ordering challenges
- Schema evolution
- More difficult end-to-end debugging
- Retry and dead-letter handling

Use it when:

- An immediate result is unnecessary.
- Work can be processed later.
- Buffering and independent scaling are valuable.
- Several consumers react independently.
- Temporary consumer unavailability must be tolerated.

## Important nuance

Request-reply over a message broker uses asynchronous transport, but if the caller blocks waiting for a reply, it is semantically still synchronous and retains temporal coupling.

### Interview-ready answer

> I use synchronous REST or gRPC when the caller needs an immediate result. I use asynchronous messaging when work can complete later and decoupling, buffering, or independent consumers are more important. With messaging, I explicitly handle idempotency, ordering, schema evolution, retries, dead letters, and eventual consistency.

---

# Q27: Cursor-based vs offset-based pagination

## Offset pagination

```http
GET /orders?limit=50&offset=100
```

SQL:

```sql
SELECT *
FROM orders
ORDER BY created_at DESC, id DESC
LIMIT 50 OFFSET 100;
```

### Advantages

- Simple
- Supports arbitrary page numbers
- Easy to explain
- Works well for small, stable datasets

### Problems

#### Deep offsets

The database may still scan or walk past many rows before returning the page.

```text
OFFSET 1,000,000
→ skip many rows
→ return only 50
```

#### Data drift

Suppose a new row is inserted after page one is read. Offset positions shift, potentially causing duplicate or missing records.

---

## Cursor pagination

Request:

```http
GET /orders?limit=50&after=eyJjcmVhdGVkQXQiOiIyMDI2...
```

Conceptual cursor contents:

```json
{
  "createdAt": "2026-07-22T08:30:00Z",
  "id": 8412
}
```

SQL:

```sql
SELECT *
FROM orders
WHERE created_at < :createdAt
   OR (created_at = :createdAt AND id < :id)
ORDER BY created_at DESC, id DESC
LIMIT 50;
```

Response:

```json
{
  "items": [],
  "page": {
    "nextCursor": "eyJjcmVhdGVkQXQiOiIyMDI2...",
    "hasNext": true
  }
}
```

## Why include a tie-breaker?

`created_at` may not be unique.

Use a stable unique combination:

```text
created_at + id
```

Otherwise, records with identical timestamps may be duplicated or skipped.

## Cursor design rules

- Treat it as an opaque token.
- Include all sort-key components.
- Sign it when clients must not modify it.
- Include filter or tenant context where necessary.
- Use deterministic ordering.
- Define what happens when a sort key changes.
- Avoid exposing sensitive internal data.

GraphQL’s connection model also uses opaque cursors to traverse collections consistently. ([GraphQL][7])

## Comparison

| Offset                         | Cursor                            |
| ------------------------------ | --------------------------------- |
| Easy arbitrary page navigation | Best for next/previous traversal  |
| Simple implementation          | More implementation complexity    |
| Can become slow at deep pages  | Efficient indexed seek            |
| Drifts under inserts/deletes   | More stable under mutations       |
| Easy total-page calculation    | Total count often separate        |
| Good for small admin screens   | Good for feeds and large datasets |

### Interview-ready answer

> Offset pagination is simple and supports page numbers, but deep offsets are expensive and concurrent inserts can shift records. Cursor pagination encodes the final sort position and performs an indexed seek. I use a deterministic sort such as `createdAt` plus `id`, expose the cursor as an opaque token, and return `hasNext` and `nextCursor`.

---

# Q28: What is the Backend for Frontend pattern?

A **Backend for Frontend**, or BFF, is a backend tailored to the needs of one client type.

```text
Mobile App → Mobile BFF
Web App    → Web BFF
Partner UI → Partner BFF
```

A mobile application may require:

- Small payloads
- Fewer round trips
- Mobile-specific authentication
- Offline-aware data
- Aggressive aggregation

A web interface may require:

- Richer data
- Different page composition
- Web-session integration
- Browser-oriented response models

---

## API Gateway vs BFF

| API Gateway                                     | BFF                                            |
| ----------------------------------------------- | ---------------------------------------------- |
| Generic platform entry point                    | Client-specific backend                        |
| Routing and edge policies                       | Client-specific composition                    |
| Rate limiting and TLS                           | Tailored payloads                              |
| Coarse authentication                           | UI-oriented orchestration                      |
| Shared across clients                           | Usually owned with a client                    |
| Should avoid domain-specific presentation logic | May contain client-specific presentation logic |

They can coexist:

```text
Client
  ↓
API Gateway
  ↓
Client-specific BFF
  ↓
Domain services
```

Microsoft’s BFF guidance describes separate backend services tailored to different frontend experiences, while warning that the pattern introduces additional services and operational overhead. ([Microsoft Learn][8])

## When to use it

- Mobile and web needs differ significantly.
- One generic API causes excessive client logic.
- Client teams require independent evolution.
- Aggregation reduces high-latency client calls.
- Security or protocol requirements differ by client.

## Risks

- Duplicated logic across BFFs
- Too many deployment units
- Business logic leaking into presentation services
- Inconsistent behaviour between clients
- BFF becoming another monolith

### Interview-ready answer

> An API Gateway provides generic routing and edge policies for the platform. A BFF is tailored to one frontend and handles client-specific aggregation, payload shape, and interaction needs. I use a BFF when mobile, web, or partner clients genuinely need different contracts, while keeping core business rules in domain services.

---

# Q29: How do you implement request/response correlation in asynchronous systems?

Several identifiers may be required.

| Identifier      | Purpose                                            |
| --------------- | -------------------------------------------------- |
| Trace ID        | Observability across distributed operations        |
| Correlation ID  | Relates messages to one workflow or request        |
| Message ID      | Identifies one message delivery                    |
| Causation ID    | Identifies the message that caused another message |
| Idempotency key | Prevents duplicate business effects                |

Do not use one identifier for every purpose without defining its semantics.

---

## Message-based request-reply

Request:

```text
messageId: MSG-101
correlationId: REQ-9001
replyTo: order-replies
```

Response:

```text
messageId: MSG-102
correlationId: REQ-9001
causationId: MSG-101
```

Flow:

```text
1. Client generates a unique correlation ID.
2. Client stores a pending Future keyed by that ID.
3. Client sends the request with correlationId and replyTo.
4. Server processes the request.
5. Server sends the response to replyTo.
6. Response copies the correlation ID.
7. Client matches it to the pending Future.
8. Timeout removes abandoned pending entries.
```

RabbitMQ’s official request-reply example uses `replyTo` for the callback queue and `correlationId` to match responses with requests. It also warns that duplicate responses can occur and the operation should ideally be idempotent. ([RabbitMQ][9])

## Important failure cases

- Response arrives after timeout.
- Request is processed twice.
- Response is delivered twice.
- Client restarts and loses in-memory pending state.
- Reply queue disappears.
- Consumer crashes after sending the response but before acknowledging the request.

## Long-running workflows

For long-running work, avoid holding a request thread or temporary reply queue open.

Prefer:

```text
POST /reports
→ 202 Accepted
→ operationId=R-100

GET /reports/R-100
→ PROCESSING / COMPLETED / FAILED
```

or publish a durable completion event.

### Interview-ready answer

> I propagate a correlation ID through request and response messages and keep message IDs separate for delivery deduplication. In request-reply messaging, the request carries `replyTo` and `correlationId`; the response copies the correlation ID so the client can complete the matching pending operation. I add timeouts, late-response handling, idempotency, and durable workflow state for long-running operations.

---

# Q30: What is contract testing with Pact?

Pact implements **consumer-driven contract testing**.

The consumer defines the interactions it relies on, and the provider verifies that it still satisfies those interactions.

## Typical workflow

```text
Consumer test
    ↓
Runs against Pact mock provider
    ↓
Generates Pact contract
    ↓
Publishes contract to Pact Broker
    ↓
Provider verification test
    ↓
Runs contract against real provider implementation
    ↓
Publishes verification result
```

---

## Consumer test

The consumer specifies:

- Expected request
- Required headers
- Expected status
- Required response structure
- Relevant field values or matchers

Conceptually:

```java
given("order O-100 exists")
    .uponReceiving("a request for order O-100")
    .path("/orders/O-100")
    .method("GET")
    .willRespondWith()
    .status(200);
```

This generates a contract representing what the consumer genuinely uses.

---

## Provider verification

The provider:

1. Loads the contract.
2. Creates the required provider state.
3. Replays each request.
4. Verifies the actual response against the contract.
5. Publishes the result.

Pact’s provider verification confirms that the provider adheres to pacts authored by its consumers and supports explicit provider-state setup. ([Pact Docs][10])

---

## Contract testing vs integration testing

| Contract testing                        | Integration testing                              |
| --------------------------------------- | ------------------------------------------------ |
| Verifies consumer/provider expectations | Verifies components work together                |
| Narrow and fast                         | Broader and usually slower                       |
| Consumer drives required interactions   | Test team defines full scenarios                 |
| Can run independently                   | Often requires several real dependencies         |
| Detects incompatible contract changes   | Detects wiring, configuration and runtime issues |
| Supports independent deployment         | Validates a more complete environment            |

## What Pact does not replace

Pact does not replace:

- Provider unit tests
- Full business-flow tests
- Database integration tests
- Authentication infrastructure tests
- Performance tests
- Load tests
- Security tests
- A small number of end-to-end tests

Pact is most valuable where consumer and provider teams both control actively developed services. Its documentation explicitly notes that it is not intended as a load-testing tool or a general replacement for provider functional tests. ([Pact Docs][11])

### Interview-ready answer

> Pact is a consumer-driven contract-testing tool. The consumer records the request and response interactions it relies on, and the provider verifies those contracts against its real implementation. This catches incompatible changes before deployment and supports independent service releases. Integration tests remain necessary for real infrastructure, configuration, persistence, security, and full workflow behaviour.

---

# Quick Interview Cheat Sheet

```text
REST constraints:
Client-server, stateless, cacheable, uniform interface,
layered system, optional code on demand.

HATEOAS:
Server-provided links and actions guide valid transitions.

REST:
Public, browser-friendly and resource-oriented.

gRPC:
Strong internal contracts, generated clients and streaming.

GraphQL:
Client-selected data with schema and resolver complexity.

API versioning:
Prefer additive changes; version genuine breaking changes.

Idempotency:
Unique key + request hash + stored result + database constraint.

Rate limiting:
Token bucket or another algorithm, distributed state and 429.

Synchronous:
Immediate result, but temporally coupled.

Asynchronous:
Buffered and decoupled, but eventually consistent.

Cursor pagination:
Opaque cursor based on a stable unique ordering.

BFF:
Client-specific backend, not a generic API Gateway.

Correlation:
Correlation ID links workflow messages;
message ID identifies one delivery.

Pact:
Consumer-generated contract plus provider verification.
```

[1]: https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm?utm_source=chatgpt.com "CHAPTER 5: Representational State Transfer (REST)"
[2]: https://docs.spring.io/spring-hateoas/docs/current/reference/html/?utm_source=chatgpt.com "Spring HATEOAS - Reference Documentation"
[3]: https://grpc.io/docs/what-is-grpc/core-concepts/?utm_source=chatgpt.com "Core concepts, architecture and lifecycle"
[4]: https://spec.graphql.org/October2021/?utm_source=chatgpt.com "GraphQL Specification"
[5]: https://docs.cloud.google.com/apis/design/versioning?utm_source=chatgpt.com "AIP-185: API Versioning"
[6]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com "RequestRateLimiter GatewayFilter Factory"
[7]: https://graphql.org/learn/pagination/?utm_source=chatgpt.com "Pagination"
[8]: https://learn.microsoft.com/en-us/azure/architecture/patterns/backends-for-frontends "Backends for Frontends Pattern - Azure Architecture Center | Microsoft Learn"
[9]: https://www.rabbitmq.com/tutorials/tutorial-six-java "RabbitMQ tutorial - Remote procedure call (RPC) | RabbitMQ"
[10]: https://docs.pact.io/implementation_guides/jvm/provider "Pact provider | Pact Docs"
[11]: https://docs.pact.io/getting_started/what_is_pact_good_for "When to use Pact | Pact Docs"
