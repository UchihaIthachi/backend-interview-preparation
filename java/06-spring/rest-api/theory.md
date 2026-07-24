# Building Production-Ready REST APIs with Spring Boot

## 1. Definition

A production-ready REST API separates responsibilities across clear layers:

```text
HTTP request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Each layer has a distinct role:

| Layer      | Responsibility                                             |
| ---------- | ---------------------------------------------------------- |
| Controller | HTTP mapping, validation trigger, status codes and headers |
| Service    | Business rules, transaction boundaries and orchestration   |
| Repository | Persistence and database queries                           |
| DTO        | External request and response contract                     |
| Entity     | Internal persistence model                                 |
| Mapper     | Conversion between DTOs and domain objects                 |

This prevents HTTP concerns, business logic and persistence details from becoming tightly coupled.

---

# 2. Recommended Project Structure

```text
com.example.orders
├── controller
│   └── OrderController.java
├── dto
│   ├── CreateOrderRequest.java
│   ├── OrderResponse.java
│   └── PageResponse.java
├── entity
│   └── OrderEntity.java
├── exception
│   ├── ApiExceptionHandler.java
│   ├── OrderNotFoundException.java
│   └── DuplicateRequestException.java
├── mapper
│   └── OrderMapper.java
├── repository
│   └── OrderRepository.java
└── service
    └── OrderService.java
```

The exact package arrangement can vary, but dependencies should generally flow inward:

```text
Controller → Service → Repository
```

A repository should not call a controller, and domain logic should not depend on HTTP-specific classes such as `ResponseEntity`.

---

# 3. Request and Response DTOs

## Request DTO

```java
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotBlank
        String customerId,

        @NotBlank
        String productId,

        @NotNull
        @Min(1)
        Integer quantity
) {
}
```

## Response DTO

```java
import java.time.Instant;

public record OrderResponse(
        Long id,
        String customerId,
        String productId,
        int quantity,
        String status,
        Instant createdAt
) {
}
```

DTOs define the API contract independently of the database model.

They help prevent:

- Internal columns being exposed accidentally
- Bidirectional entity relationships causing recursive JSON
- Lazy-loaded associations being accessed during serialization
- Persistence annotations leaking into the API contract
- Database refactoring becoming an API-breaking change
- Clients setting fields they should not control

---

# 4. Why You Should Not Return JPA Entities Directly

Consider this entity:

```java
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String productId;
    private int quantity;
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    private CustomerEntity customer;

    private String internalRiskDecision;
}
```

Returning it directly may expose:

- `internalRiskDecision`
- Database relationships
- Internal identifiers
- Lazy-loading behavior
- Fields later added for persistence only

Instead, map it explicitly:

```java
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(
            OrderEntity order
    ) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
```

The mapping code adds some boilerplate, but it protects the external contract.

---

# 5. Thin Controller

A controller should focus on HTTP concerns rather than business decisions.

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            CreateOrderRequest request
    ) {
        OrderResponse response =
                orderService.create(
                        idempotencyKey,
                        request
                );

        URI location = URI.create(
                "/api/v1/orders/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(
            @PathVariable Long id
    ) {
        return orderService.findById(id);
    }
}
```

Spring MVC can apply Bean Validation to an `@RequestBody` parameter when it is annotated with `@Valid` or `@Validated`; invalid input normally produces a `400 Bad Request`. ([Home][1])

### Controller responsibilities

- Map URLs and HTTP methods
- Read headers, path variables and query parameters
- Trigger request validation
- Select response status and headers
- Delegate to the service
- Convert domain exceptions into API errors through central handling

### Avoid in controllers

- Complex business rules
- Database queries
- Transaction management
- Large mapping logic
- Remote-service orchestration
- Repeated exception handling

---

# 6. Service Layer

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;

    public OrderService(
            OrderRepository orderRepository,
            InventoryService inventoryService,
            IdempotencyService idempotencyService
    ) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public OrderResponse create(
            String idempotencyKey,
            CreateOrderRequest request
    ) {
        return idempotencyService.execute(
                idempotencyKey,
                request,
                () -> createOrder(request)
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        OrderEntity order = orderRepository
                .findById(id)
                .orElseThrow(
                        () -> new OrderNotFoundException(id)
                );

        return OrderMapper.toResponse(order);
    }

    private OrderResponse createOrder(
            CreateOrderRequest request
    ) {
        inventoryService.reserve(
                request.productId(),
                request.quantity()
        );

        OrderEntity order = new OrderEntity();
        order.setCustomerId(request.customerId());
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setStatus("PENDING");
        order.setCreatedAt(Instant.now());

        OrderEntity saved =
                orderRepository.save(order);

        return OrderMapper.toResponse(saved);
    }
}
```

The service layer is where the application should enforce business invariants such as:

```text
Quantity must be available.
A cancelled order cannot be paid.
The same checkout must not create two orders.
A customer may not access another customer's order.
```

---

# 7. Repository Layer

```java
public interface OrderRepository
        extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdempotencyKey(
            String idempotencyKey
    );

    Page<OrderEntity> findByCustomerId(
            String customerId,
            Pageable pageable
    );
}
```

Spring Data provides pagination abstractions such as `Pageable`, `Page`, `Sort` and `PageRequest`; repository interfaces can expose `findAll(Pageable)` and related paged queries. ([Home][2])

---

# Validation

## 8. Validate at Multiple Levels

### Request-shape validation

```java
public record CreateOrderRequest(
        @NotBlank
        String customerId,

        @NotBlank
        String productId,

        @Min(1)
        int quantity
) {
}
```

This handles structural rules such as:

- Required fields
- Minimum and maximum values
- String length
- Email format
- Basic patterns

### Business validation

```java
if (!customer.isActive()) {
    throw new InactiveCustomerException(
            customer.getId()
    );
}
```

Business rules belong in the service or domain layer, not only in annotations.

### Database validation

Use constraints as the final consistency boundary:

```sql
ALTER TABLE orders
ADD CONSTRAINT uk_orders_idempotency_key
UNIQUE (idempotency_key);
```

```sql
ALTER TABLE inventory
ADD CONSTRAINT chk_inventory_non_negative
CHECK (available_quantity >= 0);
```

Validation annotations alone cannot protect against every concurrent database update.

---

# Error Handling

## 9. Centralized Exception Handling

Do not repeat `try-catch` blocks in every controller.

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(
            OrderNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Order not found");
        problem.setProperty(
                "errorCode",
                "ORDER_NOT_FOUND"
        );

        return problem;
    }

    @ExceptionHandler(OutOfStockException.class)
    public ProblemDetail handleOutOfStock(
            OutOfStockException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle("Insufficient inventory");
        problem.setProperty(
                "errorCode",
                "OUT_OF_STOCK"
        );

        return problem;
    }
}
```

Current Spring MVC supports RFC 9457-style responses through `ProblemDetail`, `ErrorResponse`, `@ExceptionHandler` and `ResponseEntityExceptionHandler`. ([Home][3])

Example response:

```json
{
  "type": "about:blank",
  "title": "Order not found",
  "status": 404,
  "detail": "Order 101 was not found",
  "instance": "/api/v1/orders/101",
  "errorCode": "ORDER_NOT_FOUND"
}
```

### Do not expose

- Stack traces
- SQL statements
- Database table names
- Internal exception class names
- Secrets or tokens
- Internal hostnames

Log the technical details internally and return a stable client-facing error.

---

# Pagination and Sorting

## 10. Paginated Endpoint

The controller should delegate pagination to the service instead of accessing the repository directly.

```java
@GetMapping
public PageResponse<OrderResponse> list(
        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "20")
        int size,

        @RequestParam(defaultValue = "createdAt")
        String sortBy,

        @RequestParam(defaultValue = "desc")
        String direction
) {
    return orderService.list(
            page,
            size,
            sortBy,
            direction
    );
}
```

Service:

```java
@Transactional(readOnly = true)
public PageResponse<OrderResponse> list(
        int page,
        int size,
        String sortBy,
        String direction
) {
    int safeSize = Math.min(
            Math.max(size, 1),
            100
    );

    Sort.Direction sortDirection =
            "asc".equalsIgnoreCase(direction)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

    Sort sort = Sort.by(
            sortDirection,
            validateSortField(sortBy)
    );

    Pageable pageable =
            PageRequest.of(
                    Math.max(page, 0),
                    safeSize,
                    sort
            );

    Page<OrderResponse> result =
            orderRepository
                    .findAll(pageable)
                    .map(OrderMapper::toResponse);

    return PageResponse.from(result);
}
```

Response wrapper:

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(
            Page<T> page
    ) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
```

### Production considerations

- Enforce a maximum page size.
- Whitelist sortable fields.
- Do not pass arbitrary field names directly into queries.
- Use stable ordering.
- Consider cursor or keyset pagination for very deep result sets.
- Avoid returning millions of rows from one request.

---

# Idempotency

## 11. Why Idempotency Matters

Consider this sequence:

```text
Client sends create-order request.
Server creates the order.
Response is lost because of a timeout.
Client retries.
Server creates a second order.
```

An idempotency key identifies one logical operation:

```http
POST /api/v1/orders
Idempotency-Key: checkout-73f8818d
```

Repeated requests using the same key should return the same logical outcome rather than execute the side effect twice.

---

## 12. Database-Backed Idempotency

A simplified table:

```sql
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    resource_id BIGINT,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL
);
```

Possible states:

```text
PROCESSING
COMPLETED
FAILED
```

Conceptual workflow:

```text
Receive request and key
        ↓
Calculate request hash
        ↓
Attempt unique insert
        ├── Insert succeeds → process request
        └── Key exists
              ├── Same request → return stored result
              └── Different request → reject key reuse
```

### Important requirements

- Enforce uniqueness in a durable store.
- Associate the key with a request fingerprint.
- Reject the same key used for different payloads.
- Store the outcome or resource identifier.
- Handle a process crashing while the key is `PROCESSING`.
- Define key expiration according to business requirements.
- Do not rely only on an in-memory map in a multi-instance deployment.

For payments or orders, a database constraint is normally a stronger correctness boundary than a best-effort cache entry.

---

# HTTP Semantics

## 13. Common Status Codes

| Situation                                |                      Status |
| ---------------------------------------- | --------------------------: |
| Resource created                         |               `201 Created` |
| Successful retrieval or update           |                    `200 OK` |
| Successful request with no response body |            `204 No Content` |
| Invalid request structure                |           `400 Bad Request` |
| Missing or invalid authentication        |          `401 Unauthorized` |
| Authenticated but not permitted          |             `403 Forbidden` |
| Resource not found                       |             `404 Not Found` |
| Business or concurrent-state conflict    |              `409 Conflict` |
| Validation rule not processable          | `422 Unprocessable Content` |
| Payload exceeds limit                    |     `413 Content Too Large` |
| Rate limit exceeded                      |     `429 Too Many Requests` |
| Temporary server overload                |   `503 Service Unavailable` |

For creation:

```java
return ResponseEntity
        .created(location)
        .body(response);
```

The `Location` header tells the client where the newly created resource can be retrieved.

---

# API Versioning

## 14. Versioning Strategies

### URI versioning

```text
/api/v1/orders
/api/v2/orders
```

Advantages:

- Visible
- Easy to route
- Easy to document
- Easy for clients to understand

Disadvantage:

- Version becomes part of the resource URL.

### Header versioning

```http
X-API-Version: 2
```

Advantages:

- Cleaner resource URL

Disadvantages:

- Less visible
- Harder to test manually
- More complicated caching and routing

### Media-type versioning

```http
Accept: application/vnd.example.orders-v2+json
```

Advantages:

- Explicit representation version

Disadvantages:

- More operational and client complexity

### Best practice

Do not create a new version for every additive field.

Usually version only when introducing a genuine breaking change, such as:

- Renaming or removing a field
- Changing field meaning
- Changing required input
- Changing response structure
- Changing business semantics

---

# CORS

## 15. What Is CORS?

**Cross-Origin Resource Sharing** is a browser-enforced protocol that allows a server to state which cross-origin browser requests are permitted.

An origin is determined by:

```text
Scheme + host + port
```

These are different origins:

```text
https://app.example.com
http://app.example.com
https://api.example.com
https://app.example.com:8443
```

CORS primarily affects browser-based JavaScript requests. It is not a general restriction on server-to-server HTTP clients, command-line tools or backend services. Spring MVC validates cross-origin requests against configured policies and adds the required response headers when a request is allowed. ([Home][4])

---

## 16. CORS Is Not Authentication

A common misconception is:

> Configuring CORS secures the API.

It does not.

CORS controls whether browser JavaScript may access a cross-origin response. The API still requires normal security controls:

- Authentication
- Authorization
- Input validation
- CSRF protection where applicable
- Rate limiting
- Audit logging

A request blocked by the browser may still reach the server. The server must enforce authorization independently.

---

## 17. Important CORS Headers

### Request headers

| Header                           | Meaning                                 |
| -------------------------------- | --------------------------------------- |
| `Origin`                         | Origin initiating the request           |
| `Access-Control-Request-Method`  | Intended method in a preflight          |
| `Access-Control-Request-Headers` | Intended non-safelisted request headers |

### Response headers

| Header                             | Meaning                                             |
| ---------------------------------- | --------------------------------------------------- |
| `Access-Control-Allow-Origin`      | Origin allowed to access the response               |
| `Access-Control-Allow-Methods`     | Allowed methods                                     |
| `Access-Control-Allow-Headers`     | Allowed request headers                             |
| `Access-Control-Allow-Credentials` | Whether credentialed access is allowed              |
| `Access-Control-Expose-Headers`    | Extra response headers readable by JavaScript       |
| `Access-Control-Max-Age`           | How long the browser may cache preflight permission |

---

# Simple and Preflighted Requests

## 18. Simple CORS Request

A CORS-safelisted request can be sent without a preflight.

Conceptual flow:

```text
Browser sends actual request with Origin
        ↓
Server processes request
        ↓
Server returns response with CORS headers
        ↓
Browser decides whether JavaScript may read it
```

Example:

```http
GET /api/v1/products HTTP/1.1
Origin: https://shop.example.com
```

Response:

```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: https://shop.example.com
Content-Type: application/json
```

A “simple” request is not necessarily read-only or harmless. A safelisted `POST` may be sent without a preflight.

---

## 19. Preflight Request

A preflight is normally used when the intended request uses a non-safelisted method or non-safelisted headers.

For example:

```http
OPTIONS /api/v1/orders/10 HTTP/1.1
Origin: https://shop.example.com
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: Authorization, Content-Type
```

Server response:

```http
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://shop.example.com
Access-Control-Allow-Methods: GET, POST, PUT
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Max-Age: 3600
```

The browser sends the actual request only after the preflight policy succeeds. The Fetch Standard bases preflight behavior on factors such as whether the method and headers are CORS-safelisted—not on whether the operation is considered “important” or changes user data. ([Fetch Standard][5])

Actual request:

```http
PUT /api/v1/orders/10 HTTP/1.1
Origin: https://shop.example.com
Authorization: Bearer ...
Content-Type: application/json
```

The actual response must also contain the appropriate CORS headers.

---

# Spring Boot CORS Configuration

## 20. Controller-Level Configuration

```java
@CrossOrigin(
        origins = "https://shop.example.com",
        methods = {
                RequestMethod.GET,
                RequestMethod.POST
        }
)
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
}
```

This can be useful for a small isolated endpoint, but distributing CORS configuration across many controllers becomes difficult to audit.

---

## 21. Global Spring MVC Configuration

```java
@Configuration
public class WebConfiguration
        implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(
            CorsRegistry registry
    ) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "https://shop.example.com",
                        "https://admin.example.com"
                )
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE"
                )
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "Idempotency-Key"
                )
                .exposedHeaders(
                        "Location",
                        "X-Request-Id"
                )
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

Spring MVC supports both handler-level `@CrossOrigin` configuration and global URL-pattern-based configuration. ([Home][4])

---

## 22. Spring Security Integration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()
                        .anyRequest()
                        .authenticated()
                );

        return http.build();
    }
}
```

CORS processing must occur before normal Spring Security authentication because a preflight request may not contain the cookies or credentials expected by the authentication flow. Spring Security can reuse the Spring MVC CORS configuration when CORS integration is enabled. ([Home][6])

Do not disable authentication merely to make a preflight error disappear.

---

# Credentialed CORS

## 23. Cookies and Credentials

Frontend:

```javascript
fetch("https://api.example.com/api/v1/orders", {
  method: "GET",
  credentials: "include",
});
```

Backend:

```java
.allowCredentials(true)
.allowedOrigins("https://shop.example.com")
```

Do not combine credentialed requests with an unrestricted origin:

```text
Access-Control-Allow-Origin: *
Access-Control-Allow-Credentials: true
```

Spring requires specific origins or controlled origin patterns when credentials are enabled because credentialed access establishes greater trust with the permitted origins. ([Home][4])

---

# Common CORS Problems

## 24. Preflight Returns `401` or `403`

Likely cause:

```text
OPTIONS request
→ security filter expects authentication
→ request rejected before CORS processing
```

Fix the CORS and Spring Security integration rather than adding authentication data manually to the preflight request.

---

## 25. Allowed Origin Does Not Match Exactly

These are not the same origin:

```text
http://localhost:3000
http://localhost:4200
https://localhost:3000
```

Include the exact development frontend origin.

---

## 26. `Authorization` Is Not Allowed

Frontend request:

```http
Authorization: Bearer ...
```

Preflight:

```http
Access-Control-Request-Headers: authorization
```

The backend must permit that header:

```java
.allowedHeaders(
        "Authorization",
        "Content-Type"
)
```

---

## 27. Postman Works but the Browser Fails

Postman is not enforcing the browser’s same-origin and CORS rules.

Therefore:

```text
Postman succeeds
+
browser fails
→ likely browser CORS configuration issue
```

It does not necessarily indicate that the endpoint or authentication is correct in every other respect.

---

# Production Readiness Checklist

## 28. API Design

- Use nouns for resources.
- Use HTTP methods consistently.
- Return meaningful status codes.
- Keep request and response contracts stable.
- Apply maximum page sizes.
- Version genuine breaking changes.
- Document optional and required fields.

## 29. Correctness

- Validate requests.
- Enforce business rules in the service layer.
- Use database constraints.
- Define transaction boundaries.
- Support idempotency for retried side effects.
- Handle concurrent updates safely.

## 30. Security

- Authenticate every protected endpoint.
- Authorize access to each resource.
- Configure CORS narrowly.
- Do not expose internal error details.
- Avoid logging secrets or complete sensitive payloads.
- Apply request-size limits.
- Add rate and abuse controls.

## 31. Reliability

- Configure connection and read timeouts.
- Use bounded retries.
- Prevent retry amplification.
- Bound executor queues.
- Add overload handling.
- Define graceful shutdown.
- Avoid holding database transactions across slow remote calls.

## 32. Observability

Capture:

- Request count
- Error count
- P95 and P99 latency
- Status-code distribution
- Database query duration
- Connection-pool waiting
- Downstream-call latency
- Executor queue depth
- Idempotency conflicts
- Correlation or trace IDs

---

# Common Mistakes

## Returning repositories directly from controllers

```java
@GetMapping
public Page<OrderEntity> list(Pageable pageable) {
    return orderRepository.findAll(pageable);
}
```

This mixes persistence, HTTP and API-contract concerns.

---

## Using entities as request DTOs

```java
@PostMapping
public OrderEntity create(
        @RequestBody OrderEntity entity
) {
}
```

This may let callers set fields such as:

- Database ID
- Internal status
- Ownership
- Audit fields
- Relationships

---

## Trusting `@Valid` as complete business validation

```java
@Min(1)
int quantity
```

checks request shape but cannot determine whether inventory is actually available.

---

## Implementing idempotency only with a check-then-insert

Unsafe:

```text
Check key does not exist.
Two requests both pass.
Both create an order.
```

Use a unique constraint or equivalent atomic operation.

---

## Allowing every CORS origin in production

```java
.allowedOrigins("*")
```

This may be appropriate for intentionally public, non-credentialed resources, but it should not be the default answer for authenticated business APIs.

---

## Treating CORS as CSRF protection

CORS and CSRF address different concerns. Proper cookie-authenticated applications may still require CSRF defenses.

---

# Interview Questions

## Question 1: Why keep controllers thin?

> Controllers should handle HTTP concerns and delegate business decisions to services. This makes business logic easier to test and reuse without constructing HTTP requests.

## Question 2: Why use DTOs rather than entities?

> DTOs keep the external API contract independent of persistence. They prevent accidental exposure of internal fields, lazy-loading problems and tight coupling between the database schema and clients.

## Question 3: Where should transactions be placed?

> Transaction boundaries normally belong in service methods that represent complete business operations, not in controllers.

## Question 4: How do you implement idempotency?

> I require an idempotency key, atomically store it in a durable system, associate it with a request hash and persist the result. A repeated identical request returns the original result, while reuse with different input is rejected.

## Question 5: How do you implement pagination?

> I accept bounded page or cursor parameters, build a `Pageable` or keyset query in the service, apply stable validated sorting and return pagination metadata with DTOs.

## Question 6: What is CORS?

> CORS is a browser-enforced protocol that lets a server declare which cross-origin browser applications may access its responses. It is not authentication or authorization.

## Question 7: When does a browser send a preflight request?

> A preflight is generally required when the intended request uses a non-safelisted method, header or request configuration. The browser sends `OPTIONS` to verify the server’s policy before sending the actual request.

## Question 8: Why does Postman work while the browser reports CORS errors?

> Postman is not subject to browser same-origin enforcement. The server may accept the request while failing to return the CORS headers required by browser JavaScript.

## Question 9: Can `Access-Control-Allow-Origin: *` be used with credentials?

> Credentialed access should use an explicitly trusted origin or controlled origin pattern rather than an unrestricted wildcard.

## Question 10: How would you version an API?

> I prefer path versioning when simplicity and visibility matter, but header or media-type versioning are alternatives. I introduce a new version only for breaking contract or behavior changes.

---

# Short Interview Answer

> I structure Spring Boot APIs with thin controllers, service-layer business logic and transaction boundaries, repositories for persistence, and DTOs at the API boundary. Requests are validated with Bean Validation, while business and consistency rules are enforced in the service and database. I use centralized RFC 9457-style error responses, bounded pagination, stable versioning and idempotency keys for retried order or payment requests. For browser clients, I configure CORS with explicit trusted origins and integrate it correctly with Spring Security, remembering that CORS is not a replacement for authentication, authorization or CSRF protection.

[1]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html "@RequestBody :: Spring Framework"
[2]: https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html "Core concepts :: Spring Data Commons"
[3]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html "Error Responses :: Spring Framework"
[4]: https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html "CORS :: Spring Framework"
[5]: https://fetch.spec.whatwg.org/ "Fetch Standard"
[6]: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html "CORS :: Spring Security"
