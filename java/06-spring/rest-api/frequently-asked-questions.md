# REST API and Spring Boot — Interview Questions and Answers

## Question 1: What is a REST API?

A **REST API** is an HTTP-based interface designed according to the principles of **Representational State Transfer**.

It exposes application data and functionality as **resources**, each identified by a URI.

```text
GET    /api/v1/orders/101
POST   /api/v1/orders
PUT    /api/v1/orders/101
PATCH  /api/v1/orders/101
DELETE /api/v1/orders/101
```

Clients send HTTP requests, and the server returns a representation of the resource, commonly as JSON.

```http
GET /api/v1/orders/101
Accept: application/json
```

```json
{
  "id": 101,
  "status": "CONFIRMED",
  "total": 2500
}
```

REST is an architectural style rather than a protocol or framework. An API is not automatically RESTful merely because it uses HTTP and JSON.

### Interview-ready answer

> A REST API models application capabilities as resources identified by URIs. Clients interact with those resources using standard HTTP methods, and each request is self-contained. A well-designed REST API follows principles such as statelessness, cacheability, client-server separation, layered architecture, and a uniform interface.

---

## Question 2: What are the main REST constraints?

REST defines several architectural constraints.

### 1. Client-server separation

The client handles presentation and user interaction, while the server handles business logic and data.

```text
Frontend application
        ↓ HTTP
Backend REST API
        ↓
Database
```

The client and server can evolve independently as long as the API contract remains compatible.

---

### 2. Statelessness

Each request must contain the information required to process it.

```http
GET /api/v1/orders/101
Authorization: Bearer <access-token>
```

The server should not depend on hidden conversational state from a previous request.

Statelessness does not mean that the application has no database or persistent state.

---

### 3. Cacheability

Responses should indicate whether they can be cached.

Relevant headers include:

```http
Cache-Control: max-age=300
ETag: "order-101-v3"
Last-Modified: Tue, 21 Jul 2026 10:30:00 GMT
```

Caching can reduce:

- Server load
- Database calls
- Network traffic
- Response latency

---

### 4. Uniform interface

Resources are accessed using consistent HTTP semantics.

```text
GET    /orders/101
POST   /orders
PUT    /orders/101
PATCH  /orders/101
DELETE /orders/101
```

---

### 5. Layered system

The client does not need to know whether it is communicating directly with the application server or through:

- API gateway
- Reverse proxy
- Load balancer
- Cache
- Security layer

---

### 6. Code on demand

The server may optionally send executable code to the client. This constraint is optional and is rarely central to ordinary REST API interviews.

---

## Question 3: Which HTTP methods are commonly used in REST APIs?

REST does not define a fixed number of HTTP methods. It uses the methods provided by HTTP.

The most commonly used methods are:

| Method    | Typical purpose                | Safe? |               Idempotent? |
| --------- | ------------------------------ | ----: | ------------------------: |
| `GET`     | Retrieve a resource            |   Yes |                       Yes |
| `HEAD`    | Retrieve headers only          |   Yes |                       Yes |
| `POST`    | Create or trigger processing   |    No |            Not inherently |
| `PUT`     | Replace a resource             |    No |                       Yes |
| `PATCH`   | Partially update a resource    |    No | Depends on implementation |
| `DELETE`  | Remove a resource              |    No |    Yes in intended effect |
| `OPTIONS` | Discover communication options |   Yes |                       Yes |

HTTP also defines methods such as `CONNECT` and `TRACE`, but they are not commonly used for ordinary resource APIs.

---

## Question 4: What is the difference between safe and idempotent HTTP methods?

### Safe method

A safe method is intended only to retrieve information and should not change application state.

Examples:

```text
GET
HEAD
OPTIONS
```

A server may still record logs or metrics, but the client-visible resource state should not be modified.

### Idempotent method

An operation is idempotent when executing the same request multiple times has the same intended effect as executing it once.

```text
PUT /users/10
DELETE /users/10
```

Idempotency concerns the resulting server state, not necessarily an identical HTTP response.

For example:

```text
First DELETE  /users/10 → 204 No Content
Second DELETE /users/10 → 404 Not Found
```

The responses differ, but the final state is the same: the user does not exist.

---

## Question 5: What is idempotency in REST APIs?

Idempotency protects operations from accidental duplicate execution.

Consider an order request:

```text
Client sends request
→ server creates order
→ response is lost
→ client retries
→ duplicate order is created
```

An idempotency key identifies one logical request:

```http
POST /api/v1/orders
Idempotency-Key: checkout-7f83b1
Content-Type: application/json
```

The server stores:

- Idempotency key
- Request fingerprint
- Processing state
- Created resource ID
- Final response

Conceptual flow:

```text
Receive key
    ↓
Attempt atomic insert
    ├── New key → execute operation
    └── Existing key
          ├── Same payload → return original result
          └── Different payload → reject reuse
```

A durable database uniqueness constraint is usually the final correctness guarantee.

```sql
ALTER TABLE orders
ADD CONSTRAINT uk_orders_idempotency_key
UNIQUE (idempotency_key);
```

### Important nuance

HTTP `POST` is not inherently idempotent, but an API can make a particular POST operation idempotent through an idempotency-key design.

---

## Question 6: What is the difference between `PUT` and `PATCH`?

### `PUT`

`PUT` usually replaces the complete current representation of a resource.

```http
PUT /api/v1/users/10
Content-Type: application/json
```

```json
{
  "name": "Amal",
  "email": "amal@example.com",
  "phone": "+94770000000"
}
```

The client sends the desired full resource representation.

`PUT` is defined as idempotent.

---

### `PATCH`

`PATCH` applies a partial modification.

```http
PATCH /api/v1/users/10
Content-Type: application/merge-patch+json
```

```json
{
  "email": "new-email@example.com"
}
```

Only the specified field is updated.

`PATCH` is not automatically idempotent. It depends on the patch operation.

Idempotent patch:

```json
{
  "status": "ACTIVE"
}
```

Potentially non-idempotent patch:

```json
{
  "operation": "increment",
  "amount": 1
}
```

Repeated execution of the second request changes the state each time.

---

## Question 7: What is the difference between REST and SOAP?

REST and SOAP are different kinds of concepts:

- **REST** is an architectural style.
- **SOAP** is an XML-based messaging framework.

| REST                             | SOAP                                     |
| -------------------------------- | ---------------------------------------- |
| Architectural style              | Messaging framework                      |
| Commonly uses HTTP               | Can use HTTP and other transports        |
| Commonly uses JSON               | Uses XML-based SOAP messages             |
| Resource-oriented                | Usually operation/message-oriented       |
| Uses HTTP status codes naturally | Uses SOAP faults                         |
| Generally lighter                | More structured and verbose              |
| Often documented with OpenAPI    | Commonly associated with WSDL            |
| Common for web/mobile APIs       | Common in legacy enterprise integrations |

### REST example

```http
GET /api/v1/orders/101
```

### SOAP example

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
    <soap:Body>
        <GetOrderRequest xmlns="https://example.com/orders">
            <OrderId>101</OrderId>
        </GetOrderRequest>
    </soap:Body>
</soap:Envelope>
```

### Important corrections

- REST does not require JSON.
- SOAP is not limited to HTTP.
- REST can be stateful at the application-data level, but REST interactions must be stateless.
- SOAP services may be designed with stateful or stateless workflows.
- REST security is not limited to OAuth or JWT.
- SOAP security is not limited to WS-Security.

### Interview-ready answer

> REST is a lightweight architectural style based on resources and standard HTTP semantics. SOAP is an XML messaging framework with envelopes, headers, bodies, faults, and often formal WSDL contracts. REST is common for modern web APIs, while SOAP remains useful in formal contract-heavy and legacy enterprise systems.

---

# HTTP Responses

## Question 8: What are common HTTP status codes?

### Successful responses

| Status           | Meaning                                      |
| ---------------- | -------------------------------------------- |
| `200 OK`         | Request completed successfully               |
| `201 Created`    | A new resource was created                   |
| `202 Accepted`   | Request accepted for asynchronous processing |
| `204 No Content` | Successful request with no response body     |

### Client errors

| Status                       | Meaning                                            |
| ---------------------------- | -------------------------------------------------- |
| `400 Bad Request`            | Invalid syntax or malformed request                |
| `401 Unauthorized`           | Authentication is missing or invalid               |
| `403 Forbidden`              | Authenticated client lacks permission              |
| `404 Not Found`              | Resource does not exist                            |
| `405 Method Not Allowed`     | Method not supported by the resource               |
| `409 Conflict`               | Request conflicts with current resource state      |
| `413 Content Too Large`      | Request body exceeds supported size                |
| `415 Unsupported Media Type` | Unsupported `Content-Type`                         |
| `422 Unprocessable Content`  | Structurally valid request violates semantic rules |
| `429 Too Many Requests`      | Rate limit exceeded                                |

### Server errors

| Status                      | Meaning                                   |
| --------------------------- | ----------------------------------------- |
| `500 Internal Server Error` | Unexpected server-side failure            |
| `502 Bad Gateway`           | Invalid response from an upstream service |
| `503 Service Unavailable`   | Temporary overload or unavailable service |
| `504 Gateway Timeout`       | Upstream service did not respond in time  |

### Creation example

```java
URI location = URI.create(
        "/api/v1/orders/" + response.id()
);

return ResponseEntity
        .created(location)
        .body(response);
```

This produces `201 Created` with a `Location` header.

---

# Spring Boot REST APIs

## Question 9: How does Spring Boot support REST APIs?

Spring Boot simplifies REST API development through:

- Spring MVC
- Embedded HTTP servers
- Auto-configuration
- JSON serialization through Jackson
- Bean Validation
- Spring Data integration
- Spring Security integration
- Centralized exception handling
- Externalized configuration
- Actuator endpoints
- Testing support

A basic application can run as a standalone JAR:

```java
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                OrderApplication.class,
                args
        );
    }
}
```

Spring Boot commonly uses an embedded server, so a separate application-server deployment is not required.

---

## Question 10: What annotations are used to build REST APIs in Spring Boot?

| Annotation              | Purpose                                 |
| ----------------------- | --------------------------------------- |
| `@RestController`       | Declares a REST controller              |
| `@RequestMapping`       | Defines a base path or general mapping  |
| `@GetMapping`           | Maps GET requests                       |
| `@PostMapping`          | Maps POST requests                      |
| `@PutMapping`           | Maps PUT requests                       |
| `@PatchMapping`         | Maps PATCH requests                     |
| `@DeleteMapping`        | Maps DELETE requests                    |
| `@PathVariable`         | Reads a value from the URL path         |
| `@RequestParam`         | Reads a query parameter                 |
| `@RequestHeader`        | Reads an HTTP header                    |
| `@RequestBody`          | Deserializes the request body           |
| `@ResponseStatus`       | Defines a response status               |
| `@Valid`                | Triggers Bean Validation                |
| `@ExceptionHandler`     | Handles selected exceptions             |
| `@RestControllerAdvice` | Provides global REST exception handling |

`ResponseEntity` is not an annotation. It is a response type used to control:

- Status
- Headers
- Body

---

## Question 11: What is the difference between `@RestController` and `@Controller`?

### `@Controller`

Used mainly in Spring MVC applications that render views.

```java
@Controller
public class PageController {

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
```

The returned string is interpreted as a view name.

### `@RestController`

Equivalent to:

```text
@Controller
+
@ResponseBody
```

Example:

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping("/{id}")
    public OrderResponse findById(
            @PathVariable long id
    ) {
        return orderService.findById(id);
    }
}
```

The returned object is serialized into the HTTP response body.

### Important nuance

A method inside a regular `@Controller` can also return JSON by adding `@ResponseBody`.

---

## Question 12: How do you validate REST API request bodies?

Use request DTOs with Jakarta Bean Validation.

```java
public record CreateOrderRequest(

        @NotBlank
        String customerId,

        @NotBlank
        String productId,

        @NotNull
        @Positive
        Integer quantity
) {
}
```

Controller:

```java
@PostMapping
public ResponseEntity<OrderResponse> create(
        @Valid
        @RequestBody
        CreateOrderRequest request
) {
    OrderResponse response =
            orderService.create(request);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
}
```

Validation annotations handle structural rules such as:

- Required fields
- Length
- Format
- Numeric boundaries

Business validation belongs in the service layer:

```java
if (!inventoryService.isAvailable(
        request.productId(),
        request.quantity()
)) {
    throw new OutOfStockException(
            request.productId()
    );
}
```

---

## Question 13: How do you handle exceptions in a Spring Boot REST API?

Spring provides:

- `@ExceptionHandler`
- `@ControllerAdvice`
- `@RestControllerAdvice`
- `ResponseEntityExceptionHandler`
- `ProblemDetail`

Global example:

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            UserNotFoundException exception
    ) {
        ApiError error = new ApiError(
                "USER_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
}
```

```java
public record ApiError(
        String code,
        String message
) {
}
```

Avoid returning:

- Stack traces
- Internal exception names
- SQL details
- Database identifiers
- Internal service URLs
- Secrets or access tokens

---

# Pagination

## Question 14: How do you implement pagination in Spring Boot?

Spring Data supports pagination through `Pageable`, `Page`, `Slice`, and `PageRequest`.

Repository:

```java
public interface UserRepository
        extends JpaRepository<UserEntity, Long> {

    Page<UserEntity> findByStatus(
            String status,
            Pageable pageable
    );
}
```

Controller:

```java
@GetMapping
public Page<UserResponse> list(
        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "20")
        int size,

        @RequestParam(defaultValue = "name")
        String sortBy
) {
    Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100),
            Sort.by(sortBy).ascending()
    );

    return userService.list(pageable);
}
```

Example request:

```http
GET /api/v1/users?page=0&size=10&sortBy=name
```

### Production considerations

- Enforce a maximum page size.
- Validate or whitelist sort fields.
- Use stable ordering.
- Avoid exposing JPA entities directly.
- Consider keyset pagination for very deep datasets.

---

# Authentication and Security

## Question 15: What authentication methods are commonly used for APIs?

### Basic authentication

```http
Authorization: Basic <base64-credentials>
```

Base64 is encoding, not encryption. Basic authentication must be used only over HTTPS.

---

### Bearer token

```http
Authorization: Bearer <access-token>
```

The token may be opaque or structured.

---

### JWT

A JWT is a token format consisting of:

```text
Header.Payload.Signature
```

JWT can be used as a bearer token, but JWT and bearer authentication are not the same concept.

---

### OAuth 2.0

OAuth 2.0 is an authorization framework that allows a client to obtain delegated access to protected resources.

OAuth 2.0 is not itself an authentication protocol. OpenID Connect adds authentication and identity capabilities on top of OAuth 2.0.

---

### API key

```http
X-API-Key: client-key
```

API keys commonly identify a calling application rather than an individual human user.

---

### Mutual TLS

Both client and server authenticate using certificates.

Common in:

- Internal services
- Banking integrations
- Partner APIs
- High-trust machine-to-machine communication

---

## Question 16: How do you secure a REST API?

A production security strategy should include several layers.

### Transport security

Use HTTPS to protect data in transit.

### Authentication

Verify the caller’s identity using:

- Session
- OAuth 2.0 access token
- JWT
- API key
- Mutual TLS

### Authorization

Verify that the authenticated principal may perform the requested action.

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(long userId) {
}
```

### Request validation

Validate:

- Field formats
- Size limits
- Allowed values
- File types
- Query parameters

### Database safety

Use parameterized queries rather than string concatenation.

### Rate limiting

Protect the service from:

- Abuse
- Brute-force attempts
- Accidental request loops
- Resource exhaustion

### Sensitive-data protection

Do not log:

- Passwords
- Tokens
- Full payment information
- Private customer data

### CORS

CORS controls which browser origins may read cross-origin responses. It is not an authentication or authorization mechanism.

### CSRF

Cookie-authenticated browser applications may still require CSRF protection. CORS and CSRF solve different problems.

---

# Rate Limiting

## Question 17: What is API rate limiting?

Rate limiting restricts how frequently a client may call an API within a defined window.

Example policy:

```text
1,000 requests per hour per client
```

Possible identifiers include:

- User ID
- API key
- IP address
- Tenant
- Endpoint
- Subscription tier

Common algorithms include:

- Fixed window
- Sliding window
- Token bucket
- Leaky bucket

Response:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
```

### Rate limiting vs throttling

The terms are often used interchangeably, but throttling may also mean deliberately slowing requests instead of rejecting them immediately.

---

# API Gateway

## Question 18: What is an API gateway?

An API gateway is an entry point placed between clients and backend services.

```text
Client
   ↓
API Gateway
   ├── Authentication
   ├── Rate limiting
   ├── Routing
   ├── Request transformation
   ├── Logging
   └── Observability
        ↓
Backend services
```

Common responsibilities include:

- Routing
- Authentication
- TLS termination
- Rate limiting
- Request and response transformation
- Cross-origin configuration
- Logging and tracing
- Load balancing
- Canary routing

Examples include:

- Spring Cloud Gateway
- Kong
- Apigee
- AWS API Gateway
- Azure API Management
- NGINX
- Traefik

### Important rule

Do not move core business rules entirely into the gateway.

The backend must still enforce:

- Authorization
- Validation
- Business invariants
- Data ownership

A client may reach a service through another internal route, and gateway configuration can be misconfigured.

---

## Question 19: How does Spring Cloud Gateway route requests?

A conceptual route can be configured as:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8080
          predicates:
            - Path=/users/**
```

Request flow:

```text
GET /users/101
      ↓
Spring Cloud Gateway
      ↓
http://user-service:8080/users/101
```

Filters can be applied for:

- Authentication
- Header modification
- Logging
- Rate limiting
- Path rewriting
- Retry
- Circuit breaking

Retries must be used carefully because retrying non-idempotent operations can duplicate side effects.

---

# HATEOAS

## Question 20: What is HATEOAS?

**HATEOAS** means **Hypermedia as the Engine of Application State**.

The server includes links describing available next actions.

```json
{
  "id": 101,
  "status": "CREATED",
  "_links": {
    "self": {
      "href": "/api/v1/orders/101"
    },
    "cancel": {
      "href": "/api/v1/orders/101/cancellation"
    },
    "payment": {
      "href": "/api/v1/orders/101/payment"
    }
  }
}
```

The links available may depend on the resource state.

For example, a completed order may no longer include a cancellation action.

HATEOAS reduces the client’s need to construct every URL manually, although many practical APIs use only limited hypermedia.

---

# API Documentation

## Question 21: How do you document a Spring Boot REST API?

Common approaches include:

### OpenAPI

OpenAPI describes:

- Endpoints
- Methods
- Parameters
- Request schemas
- Response schemas
- Status codes
- Authentication requirements

Spring applications commonly generate OpenAPI descriptions through Springdoc.

Example annotation:

```java
@Operation(
        summary = "Find an order",
        description = "Returns an order by its identifier"
)
@GetMapping("/{id}")
public OrderResponse findById(
        @PathVariable long id
) {
    return orderService.findById(id);
}
```

---

### Swagger UI

Swagger UI provides interactive documentation based on an OpenAPI specification.

The exact URL depends on project configuration and library version, commonly under a Swagger UI path such as:

```text
/swagger-ui/index.html
```

---

### Spring REST Docs

Spring REST Docs generates documentation from tests.

Benefit:

```text
Test passes
→ documented request and response are real
```

This reduces the risk of documentation drifting from actual behavior.

---

### Postman

Postman collections can provide:

- Example requests
- Environment variables
- Test scripts
- Shared API collections

They are useful operational aids but should not be the only source of the formal API contract.

---

# Why REST Is Popular

REST is popular because it provides:

- Familiar HTTP semantics
- Broad client compatibility
- Lightweight JSON integration
- Stateless horizontal scaling
- HTTP caching
- Clear resource-based URLs
- Simple tooling
- Easy browser and mobile integration
- Language-independent communication

However, REST is not always the best option.

Alternatives may be better for certain requirements:

| Requirement                             | Possible choice    |
| --------------------------------------- | ------------------ |
| Strongly typed service-to-service calls | gRPC               |
| Flexible client-selected queries        | GraphQL            |
| Asynchronous event processing           | Kafka or messaging |
| Formal legacy enterprise contract       | SOAP               |
| Real-time bidirectional communication   | WebSocket          |

---

# Common Mistakes

## Mistake 1: Saying REST supports exactly seven methods

REST can use any suitable HTTP method. The seven listed methods are simply the most common for resource APIs.

---

## Mistake 2: Saying every `PATCH` request is non-idempotent

A PATCH operation can be idempotent or non-idempotent depending on its semantics.

---

## Mistake 3: Saying repeated idempotent requests return the same response

Idempotency concerns the final intended state, not necessarily an identical status code or body.

---

## Mistake 4: Treating JWT as an authentication protocol

JWT is a token format. OAuth 2.0 is an authorization framework, while OpenID Connect provides an authentication layer.

---

## Mistake 5: Treating CORS as API security

CORS is enforced by browsers and does not replace backend authentication or authorization.

---

## Mistake 6: Returning JPA entities directly

Expose DTOs rather than persistence entities to avoid:

- Internal field leakage
- Lazy-loading errors
- Recursive serialization
- Tight API-database coupling

---

# Quick Interview Cheat Sheet

```text
REST:
Architectural style based on resources and HTTP semantics.

Core constraints:
Client-server
Stateless
Cacheable
Uniform interface
Layered system
Optional code on demand

Common methods:
GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS

Safe:
GET, HEAD, OPTIONS

Idempotent:
GET, HEAD, PUT, DELETE, OPTIONS
PATCH depends on design
POST is not inherently idempotent

PUT:
Usually full replacement.

PATCH:
Partial modification.

REST vs SOAP:
Architectural style vs XML messaging framework.

Spring REST:
@RestController
@GetMapping
@PostMapping
@PathVariable
@RequestParam
@RequestBody
@Valid

Security:
HTTPS
Authentication
Authorization
Validation
Rate limiting
Secure logging
Database constraints

Gateway:
Routing, authentication, rate limiting, observability.

Documentation:
OpenAPI, Swagger UI, Spring REST Docs, Postman.
```

# Short Interview Answer

> A REST API models domain concepts as resources identified by URIs and uses HTTP methods such as GET, POST, PUT, PATCH and DELETE to interact with them. A properly RESTful API is stateless, cache-aware, layered and consistent in its use of HTTP semantics. In Spring Boot, I build APIs using thin `@RestController` classes, validated DTOs, service-layer business logic, centralized exception handling, pagination, authentication and authorization, rate limiting, idempotency for retried side effects, and OpenAPI-based documentation.
