# Basic Questions — Web and Spring Frameworks

## Question 1: How do you validate request bodies in Spring Boot?

Spring Boot commonly uses **Jakarta Bean Validation** to validate request DTOs. Add validation constraints to the DTO and place `@Valid` before `@RequestBody`. When a Bean Validation implementation is available through `spring-boot-starter-validation`, Spring MVC can apply those constraints to controller arguments. ([Home][1])

### Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Request DTO

```java
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Age is required")
        @Positive(message = "Age must be positive")
        Integer age
) {
}
```

### Controller

```java
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @Valid
            @RequestBody
            CreateUserRequest request
    ) {
        return userService.create(request);
    }
}
```

Spring deserializes the JSON request into the DTO and validates it before normal controller processing continues. Invalid input normally results in a client error response rather than allowing malformed data to reach the service layer. ([Home][2])

---

### Common validation annotations

| Annotation          | Purpose                                           |
| ------------------- | ------------------------------------------------- |
| `@NotNull`          | Value cannot be `null`                            |
| `@NotBlank`         | String cannot be null, empty, or whitespace       |
| `@NotEmpty`         | Collection, array, map, or string cannot be empty |
| `@Size`             | Controls string or collection size                |
| `@Min` / `@Max`     | Numeric boundaries                                |
| `@Positive`         | Number must be greater than zero                  |
| `@Email`            | Checks email structure                            |
| `@Pattern`          | Validates against a regular expression            |
| `@Past` / `@Future` | Validates date relationships                      |

---

### Nested-object validation

Adding constraints to a nested DTO is not enough by itself. Apply `@Valid` to cascade validation into the nested object.

```java
public record AddressRequest(

        @NotBlank
        String lineOne,

        @NotBlank
        String city,

        @NotBlank
        String country
) {
}
```

```java
public record CreateCustomerRequest(

        @NotBlank
        String name,

        @NotNull
        @Valid
        AddressRequest address
) {
}
```

---

### Collection-element validation

```java
public record CreateOrderRequest(

        @NotEmpty
        List<@Valid OrderItemRequest> items
) {
}
```

This validates both:

- The collection itself
- Every item inside it

---

### Global validation error handling

```java
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse>
    handleValidationFailure(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors =
                new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ValidationErrorResponse response =
                new ValidationErrorResponse(
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        fieldErrors
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
```

```java
public record ValidationErrorResponse(
        String code,
        String message,
        Map<String, String> errors
) {
}
```

Example response:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "errors": {
    "name": "Name is required",
    "email": "Email must be valid"
  }
}
```

---

### Service method validation

Use `@Validated` when constraints are placed directly on service method parameters or return values. Spring Boot enables method validation when a Bean Validation provider is on the classpath and the target class is marked appropriately. ([Home][1])

```java
import jakarta.validation.constraints.Positive;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PaymentService {

    public void refund(
            @Positive long paymentId,

            @Positive long amountCents
    ) {
        // Business logic
    }
}
```

---

### Request validation vs business validation

Bean Validation should handle request shape:

```text
Email format
Required fields
String length
Positive quantity
```

The service layer should handle business rules:

```text
Does the customer exist?
Is inventory available?
Can this order be cancelled?
Does the user own this resource?
```

A valid request can still be invalid according to business rules.

### Interview-ready answer

> I create a request DTO with Jakarta Validation constraints and annotate the controller parameter with `@Valid @RequestBody`. Spring validates the DTO before executing the controller logic. I use `@RestControllerAdvice` to return consistent field-level errors, cascade nested validation with `@Valid`, and keep database-dependent business validation in the service layer.

---

# Question 2: What is the difference between REST and SOAP?

REST and SOAP are not equivalent concepts:

- **REST** is an architectural style.
- **SOAP** is an XML-based messaging framework with defined message-processing rules.

REST defines constraints such as client-server separation, stateless communication, caching, a uniform interface, and layered architecture. SOAP defines an extensible XML message structure containing elements such as an envelope, optional headers, a body, and faults, and it can be bound to different underlying protocols. ([UC Irvine ICS][3])

| REST                                         | SOAP                                             |
| -------------------------------------------- | ------------------------------------------------ |
| Architectural style                          | Messaging framework/protocol                     |
| Usually uses HTTP                            | Can be bound to HTTP or other transports         |
| Commonly exchanges JSON                      | Uses an XML-based message format                 |
| Resource-oriented                            | Often operation or message-oriented              |
| Uses HTTP methods and status codes naturally | Uses SOAP envelopes and SOAP faults              |
| Generally lightweight                        | More structured and verbose                      |
| Contract may be documented with OpenAPI      | Often associated with formal service contracts   |
| Common for web and mobile APIs               | Common in legacy and contract-heavy integrations |

### REST example

```http
GET /api/v1/orders/101
Accept: application/json
```

```json
{
  "id": 101,
  "status": "CONFIRMED"
}
```

### SOAP message example

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
    <soap:Header/>
    <soap:Body>
        <GetOrderRequest xmlns="https://example.com/orders">
            <OrderId>101</OrderId>
        </GetOrderRequest>
    </soap:Body>
</soap:Envelope>
```

### Important corrections

REST does not require JSON, and SOAP is not restricted to HTTP. SOAP’s specification explicitly defines an XML messaging framework that can be exchanged over different underlying protocols. ([W3C][4])

### When might SOAP be chosen?

- Existing enterprise integrations already use it
- Strict formal contracts are required
- Standardized message-level features are needed
- Compatibility with legacy financial or government systems matters

### When might REST be chosen?

- Browser, mobile, or public APIs
- Resource-oriented CRUD operations
- Lightweight payloads
- Easy HTTP integration
- Broad client interoperability

### Interview-ready answer

> REST is an architectural style based on resources, stateless communication, a uniform interface, and other constraints. SOAP is a structured XML messaging framework with envelopes, headers, bodies, and faults. REST is usually simpler for modern HTTP APIs, while SOAP remains useful in formal contract-driven and legacy enterprise integrations.

---

# Question 3: What is Spring?

Spring is a modular Java application framework that provides infrastructure for building enterprise applications. Its core capabilities include dependency injection, validation, data binding, AOP, transaction support, testing, data access, Spring MVC, and Spring WebFlux. ([Home][5])

At its center is the Spring container:

```text
Configuration
     ↓
Spring container
     ↓
Creates beans
     ↓
Injects dependencies
     ↓
Manages bean lifecycle
```

Example:

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(
            OrderRepository orderRepository
    ) {
        this.orderRepository = orderRepository;
    }
}
```

`OrderService` does not create its own repository:

```java
new JdbcOrderRepository();
```

Instead, Spring supplies a compatible bean.

### Major Spring capabilities

- Inversion of Control and dependency injection
- Declarative transaction management
- Aspect-oriented programming
- Web applications through Spring MVC or WebFlux
- Database and ORM integration
- Validation and data binding
- Event publishing
- Testing support
- Integration with messaging and security projects

### Interview-ready answer

> Spring is a modular Java framework and IoC container. It creates and manages application objects called beans, injects their dependencies, and provides infrastructure for transactions, web development, validation, data access, AOP, events, and testing.

---

# Question 4: Why do we use Spring?

Spring reduces infrastructure boilerplate and allows application components to depend on abstractions rather than creating concrete dependencies directly. Its official framework modules cover dependency injection, transactions, data access, web development, validation, AOP, and testing. ([Home][6])

## Main benefits

### Loose coupling

```java
public interface PaymentGateway {

    PaymentResult charge(
            PaymentRequest request
    );
}
```

```java
@Service
public class CheckoutService {

    private final PaymentGateway paymentGateway;

    public CheckoutService(
            PaymentGateway paymentGateway
    ) {
        this.paymentGateway = paymentGateway;
    }
}
```

The checkout logic is not tightly coupled to one payment provider.

---

### Easier testing

```java
PaymentGateway gateway =
        mock(PaymentGateway.class);

CheckoutService service =
        new CheckoutService(gateway);
```

Constructor injection allows dependencies to be replaced with test doubles without starting the complete Spring context.

---

### Declarative transactions

```java
@Transactional
public void transfer(
        long sourceAccountId,
        long destinationAccountId,
        long amount
) {
    debit(sourceAccountId, amount);
    credit(destinationAccountId, amount);
}
```

---

### Reusable cross-cutting behavior

Spring can apply infrastructure concerns such as:

- Transactions
- Security
- Caching
- Logging
- Metrics
- Retry handling

without placing all related code directly inside each business method.

### Interview-ready answer

> We use Spring to reduce boilerplate, manage object creation and dependencies, support loose coupling, simplify testing, provide declarative transactions, and integrate common web, persistence, validation, security, and messaging concerns.

---

# Question 5: What is the difference between Spring and Spring Boot?

Spring Boot is built on top of the Spring ecosystem. Spring provides the underlying framework capabilities, while Spring Boot adds conventions and automation for creating stand-alone, production-oriented Spring applications. Spring Boot provides auto-configuration, starter dependencies, embedded server support, externalized configuration, and operational features. ([Home][7])

| Spring Framework                            | Spring Boot                                   |
| ------------------------------------------- | --------------------------------------------- |
| Core application framework                  | Tooling and conventions built around Spring   |
| Provides IoC, DI, MVC, AOP and transactions | Simplifies configuration and startup          |
| May require more explicit configuration     | Uses auto-configuration                       |
| Dependencies selected individually          | Provides starter dependencies                 |
| Deployment model configured by application  | Commonly runs with an embedded server         |
| Infrastructure capabilities                 | Faster project bootstrap and production setup |

### Traditional Spring configuration

```java
@Configuration
@ComponentScan("com.example")
@EnableWebMvc
public class ApplicationConfiguration {
}
```

### Spring Boot application

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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

`@SpringBootApplication` combines common configuration behavior, including component scanning and auto-configuration participation. Boot auto-configuration then examines available dependencies and existing beans before configuring suitable infrastructure. ([Home][7])

### Interview-ready answer

> Spring is the underlying framework that provides dependency injection, web, transactions, data access, and other infrastructure. Spring Boot builds on Spring and reduces configuration through starters, auto-configuration, embedded servers, externalized configuration, and production-oriented features.

---

# Question 6: How does autowiring work in Spring?

Autowiring is Spring’s mechanism for resolving and injecting a bean’s dependencies.

Spring commonly resolves an injection point by type:

```java
@Service
public class NotificationService {

    private final MessageSender messageSender;

    public NotificationService(
            MessageSender messageSender
    ) {
        this.messageSender = messageSender;
    }
}
```

When only one constructor exists, Spring can use it without requiring `@Autowired`. Spring also supports constructor, setter, field, and arbitrary method injection. ([Home][8])

## Candidate resolution

Suppose two beans implement the same interface:

```java
public interface MessageSender {

    void send(String message);
}
```

```java
@Component
public class EmailMessageSender
        implements MessageSender {

    @Override
    public void send(String message) {
    }
}
```

```java
@Component
public class SmsMessageSender
        implements MessageSender {

    @Override
    public void send(String message) {
    }
}
```

Spring cannot choose automatically when both are equally valid candidates.

### Resolve with `@Qualifier`

```java
@Service
public class NotificationService {

    private final MessageSender messageSender;

    public NotificationService(
            @Qualifier("emailMessageSender")
            MessageSender messageSender
    ) {
        this.messageSender = messageSender;
    }
}
```

### Resolve with `@Primary`

```java
@Primary
@Component
public class EmailMessageSender
        implements MessageSender {
}
```

### Constructor injection is generally preferable

Benefits include:

- Required dependencies are explicit
- Fields can be `final`
- Easier unit testing
- No partially initialized bean
- Circular dependencies become easier to identify

### Optional dependencies

```java
public NotificationService(
        Optional<AuditPublisher> auditPublisher
) {
    this.auditPublisher = auditPublisher;
}
```

Spring also supports injecting collections of all matching beans. ([Home][8])

```java
public NotificationService(
        List<MessageSender> senders
) {
    this.senders = senders;
}
```

### Interview-ready answer

> Spring autowires dependencies by resolving matching beans from the application context, normally by type. If one constructor exists, it does not need `@Autowired`. For multiple candidates, I use `@Qualifier` or `@Primary`. I prefer constructor injection because dependencies are explicit, testable, and can be immutable.

---

# Question 7: What is Spring Security?

Spring Security is the Spring ecosystem’s framework for authentication, authorization, and protection against common web exploits. In servlet applications, requests pass through Spring Security’s filter infrastructure, where a matching `SecurityFilterChain` determines which security filters apply. ([Home][9])

## Authentication vs authorization

### Authentication

```text
Who is the user?
```

Examples:

- Username and password
- Session cookie
- JWT access token
- OAuth 2.0 login
- Client certificate

### Authorization

```text
What is that user allowed to do?
```

Examples:

- Only administrators may delete users.
- A customer may view only their own orders.
- A support agent may refund but not change prices.

Spring Security supports request-based and method-based authorization. ([Home][10])

---

## Basic configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/users/**"
                        ).hasRole("ADMIN")
                        .anyRequest()
                        .authenticated()
                );

        return http.build();
    }
}
```

## Method security example

```java
@PreAuthorize(
        "hasRole('ADMIN') or #customerId == authentication.name"
)
public OrderResponse getOrder(
        String customerId,
        long orderId
) {
    return loadOrder(orderId);
}
```

### Important features

- Authentication
- Request authorization
- Method authorization
- Password encoding
- Session management
- CSRF protection
- Security headers
- OAuth 2.0 and OpenID Connect integration
- Resource-server support
- Security testing support

### Interview-ready answer

> Spring Security provides authentication, authorization, and protection against common web attacks. In servlet applications, requests pass through an ordered security filter chain. I configure endpoint rules through `SecurityFilterChain` and use method security for business-level authorization.

---

# Question 8: What is a RESTful API?

A RESTful API is an API designed according to REST architectural constraints.

Important REST constraints include:

1. **Client-server separation**
2. **Stateless communication**
3. **Cacheable responses**
4. **Uniform interface**
5. **Layered system**
6. **Optional code-on-demand**

These constraints are defined by the REST architectural style rather than by a particular JSON library or framework. ([UC Irvine ICS][3])

## Resource-oriented design

Prefer:

```text
GET    /orders/101
POST   /orders
PUT    /orders/101
PATCH  /orders/101
DELETE /orders/101
```

Avoid operation-heavy URLs where normal HTTP semantics are sufficient:

```text
/getOrder
/createOrder
/deleteOrder
```

## HTTP methods

| Method   | Typical purpose              |            Idempotent? |
| -------- | ---------------------------- | ---------------------: |
| `GET`    | Retrieve a representation    |                    Yes |
| `POST`   | Create or trigger processing |         Not inherently |
| `PUT`    | Replace a resource           |                    Yes |
| `PATCH`  | Partially update             |      Depends on design |
| `DELETE` | Remove a resource            | Yes in intended effect |

HTTP defines method semantics independently of Spring, and a REST API should use those semantics consistently. ([RFC Editor][11])

## Statelessness

Each request should contain the information required for the server to understand it.

```http
GET /api/v1/orders/101
Authorization: Bearer <token>
```

Statelessness does not mean the application has no database state. It means the server should not depend on hidden conversational state from a previous request to interpret the current request.

## Appropriate status codes

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Content
429 Too Many Requests
500 Internal Server Error
```

### Interview-ready answer

> A RESTful API models domain concepts as resources, uses HTTP methods and status codes consistently, keeps requests stateless, supports cacheability where appropriate, and exposes a uniform interface. REST is an architectural style and does not require JSON specifically.

---

# Question 9: What is the difference between HTTP and HTTPS?

HTTP is a stateless application-level request-response protocol. HTTPS uses HTTP semantics over a secure transport protected by TLS. ([IETF][12])

| HTTP                                        | HTTPS                                                 |
| ------------------------------------------- | ----------------------------------------------------- |
| Traffic is not protected by TLS             | Traffic is protected by TLS                           |
| Data may be readable in transit             | Provides confidentiality                              |
| Messages may be modified undetected         | Provides integrity protection                         |
| Server identity is not authenticated by TLS | Server identity is authenticated through certificates |
| Unsuitable for passwords and tokens         | Required for sensitive production traffic             |

## What TLS provides

### Confidentiality

Other parties should not be able to read the transmitted application data.

### Integrity

Modification of protected traffic can be detected.

### Authentication

The client can authenticate the server through its certificate. TLS can also support client authentication when mutual TLS is configured. ([RFC Editor][13])

## Simplified HTTPS connection

```text
Client connects to server
        ↓
TLS handshake
        ↓
Server presents certificate
        ↓
Cryptographic keys established
        ↓
HTTP messages sent through encrypted connection
```

### HTTPS does not secure everything

HTTPS does not automatically prevent:

- SQL injection
- Broken authorization
- Weak passwords
- Server-side data leaks
- Malicious application logic
- Sensitive data being logged
- Compromised client or server devices

### Interview-ready answer

> HTTP defines request-response semantics. HTTPS is HTTP carried over TLS, which provides confidentiality, integrity, and server authentication. HTTPS protects data while it travels across the network, but application-level authentication, authorization, validation, and secure coding are still required.

---

# Quick Interview Cheat Sheet

| Question           | Key answer                                                   |
| ------------------ | ------------------------------------------------------------ |
| Request validation | DTO constraints plus `@Valid @RequestBody`                   |
| REST vs SOAP       | Architectural style vs XML messaging framework               |
| Spring             | Java framework and IoC container                             |
| Why Spring?        | Loose coupling, testability and infrastructure support       |
| Spring vs Boot     | Framework capabilities vs conventions and auto-configuration |
| Autowiring         | Spring resolves and injects matching beans                   |
| Spring Security    | Authentication, authorization and exploit protection         |
| RESTful API        | Resource-oriented, stateless, uniform HTTP interface         |
| HTTP vs HTTPS      | HTTPS is HTTP protected by TLS                               |

# Short Interview Summary

> I validate Spring request DTOs with Jakarta Validation constraints and `@Valid @RequestBody`, then format errors centrally using `@RestControllerAdvice`. Spring manages application beans and their dependencies through dependency injection, while Spring Boot simplifies setup using starters, auto-configuration and embedded-server support. Spring Security handles authentication and authorization through an ordered filter chain. REST is a stateless, resource-oriented architectural style, while SOAP is a structured XML messaging framework. HTTPS preserves HTTP semantics but protects network traffic using TLS.

[1]: https://docs.spring.io/spring-boot/reference/io/validation.html?utm_source=chatgpt.com "Validation :: Spring Boot"
[2]: https://docs.spring.io/spring-framework/reference/core/validation.html?utm_source=chatgpt.com "Validation, Data Binding, and Type Conversion"

[3]: https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm "
Fielding Dissertation: CHAPTER 5: Representational State Transfer (REST)
"
[4]: https://www.w3.org/TR/soap12-part1/ "SOAP Version 1.2 Part 1: Messaging Framework (Second Edition)"
[5]: https://docs.spring.io/spring-framework/reference/overview.html?utm_source=chatgpt.com "Spring Framework Overview"
[6]: https://spring.io/projects/spring-framework?utm_source=chatgpt.com "Spring Framework"
[7]: https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com "Auto-configuration"
[8]: https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com "Using @Autowired :: Spring Framework"
[9]: https://docs.spring.io/spring-security/reference/features/index.html?utm_source=chatgpt.com "Features :: Spring Security"
[10]: https://docs.spring.io/spring-security/reference/features/authorization/index.html?utm_source=chatgpt.com "Authorization :: Spring Security"
[11]: https://www.rfc-editor.org/rfc/rfc9110.html "RFC 9110: HTTP Semantics"
[12]: https://www.ietf.org/rfc/rfc9112?utm_source=chatgpt.com "RFC 9112: HTTP/1.1"
[13]: https://www.rfc-editor.org/rfc/rfc8446.html "www.rfc-editor.org"
