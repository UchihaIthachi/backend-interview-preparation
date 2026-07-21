# Building Production-Ready REST APIs with Spring Boot

## 1. Definition

A REST API structure separates concerns: controllers handle HTTP, services hold
business logic, repositories handle persistence — with DTOs at the boundary
instead of exposing entities directly.

## 2. Why it exists

Keeping business logic out of controllers makes it testable independent of the
web layer, and using DTOs instead of entities prevents accidentally leaking
internal database structure (or lazy-loading exceptions) through the API.

## 3. How it works — layered structure

```text
Controller (HTTP concerns: status codes, request/response mapping)
    v
Service (business logic, transactions)
    v
Repository (persistence)
    v
Database
```

```java
@RestController
@RequestMapping("/orders")
class OrderController {
    private final OrderService orderService;

    OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping
    ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderMapper.toResponse(order));
    }
}
```

## 4. Code example — pagination and idempotency

```java
@GetMapping
Page<OrderResponse> list(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
    return orderRepository.findAll(PageRequest.of(page, size)).map(OrderMapper::toResponse);
}

@PostMapping
ResponseEntity<OrderResponse> create(@RequestHeader("Idempotency-Key") String key,
                                      @Valid @RequestBody CreateOrderRequest request) {
    // check if this idempotency key was already processed before creating
}
```

## 5. Production use case

Idempotency keys on POST endpoints prevent duplicate order/payment creation
when clients retry after a timeout — a very common production requirement for
payment and order APIs.

## 6. Common mistakes

- Returning JPA entities directly from controllers (leaks lazy-loading
  exceptions and internal fields, tightly couples API shape to DB schema).
- Skipping request validation (`@Valid`) and letting bad data reach the service layer.
- Not versioning APIs, making breaking changes painful for existing clients.
- Ignoring idempotency on POST endpoints that create resources.

## 7. Trade-offs

DTOs add mapping boilerplate but decouple your API contract from your database
schema — almost always worth it for anything beyond a throwaway prototype.

## 8. Interview questions

1. Why avoid putting business logic directly in controllers?
2. Why use DTOs instead of returning entities directly?
3. How would you implement idempotency for a POST endpoint?
4. How do you handle pagination and sorting in a Spring REST API?
5. How would you version a REST API?

## 9. Short interview answer

"I keep controllers thin — HTTP concerns only — with business logic in a
service layer and DTOs at the boundary so the API contract doesn't leak
database schema details or lazy-loading exceptions. For any POST that creates
a resource with real-world side effects, like payments or orders, I add
idempotency key support so client retries don't create duplicates."

## 10. Related topics

- [Spring Boot](spring-boot.md)
- [JPA / N+1 problem](spring-data-jpa.md)


During Web Development did you get this error?

Cross-Origin Resource Sharing (CORS) is a security feature that lets a web page from one domain request resources from a different domain:

➤ What is CORS?

CORS allows a web page to request resources (like CSS files or API data) from a different domain. For ex: if your web page is hosted on domain-A.com, it can request resources from domain-B.com.

➤ How Does CORS Work?

CORS uses HTTP headers to tell browsers whether different domains are allowed to access resources. Here are some key headers:

- Access-Control-Allow-Origin: Specifies which domains can access the response.
- Access-Control-Allow-Credentials: Indicates if the response can include credentials (like cookies).
- Access-Control-Allow-Headers: Lists which headers can be used in the request.
- Access-Control-Allow-Methods: Lists which HTTP methods (GET, POST, etc.) can be used.
- Access-Control-Expose-Headers: Lists which headers are safe to expose to the browser.
- Access-Control-Request-Headers: Used in preflight requests to list headers that will be used.
- Access-Control-Request-Method: Used in preflight requests to indicate the HTTP method that will be used.

➤ Types of CORS Requests

1. Simple Requests:
 - Do not trigger a preflight check.
 - Use only safelisted headers.
 - Methods: GET, POST, HEAD.

2. Preflight Requests:
 - Send an "OPTIONS" request before the actual request.
 - The server checks if the actual request is safe to allow.
 - Necessary for requests that may affect user data or make significant changes on the server.

➤ Example Flows

Simple Request:
1. Client: domain-A.com makes a GET request to domain-B.com/resource.
2. Server: domain-B.com responds with the resource and `Access-Control-Allow-Origin: domain-A.com`.
3. Browser: Checks the `Access-Control-Allow-Origin` header. If domain-A.com matches, it allows the resource to be accessed by the web page.

Preflight Request:
1. Client: domain-A.com wants to send a PUT request to domain-B.com/resource. Sends an OPTIONS request first.
2. Server: domain-B.com responds to OPTIONS request with `Access-Control-Allow-Methods: PUT` and `Access-Control-Allow-Headers: Content-Type`.
3. Client: Sends the actual PUT request if allowed.
4. Server: Responds to the PUT request and includes `Access-Control-Allow-Origin: domain-A.com`.
5. Browser: Checks the `Access-Control-Allow-Origin` header. If domain-A.com matches, it allows the resource to be accessed by the web page.

By setting up CORS correctly, your web pages can safely request resources from other domains, keeping your web applications secure.