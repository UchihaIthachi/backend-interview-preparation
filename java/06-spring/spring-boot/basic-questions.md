# Spring Boot, Spring Framework, Maven, and Build — Cleaned Question Set

This set contains several duplicate questions. A cleaner repository structure would separate it into three files:

```text
spring/
├── spring-boot/
│   └── basic-questions.md
├── spring-core/
│   └── basic-questions.md
└── maven/
    └── basic-questions.md
```

## Duplicate Questions to Merge

| Existing questions                  | Recommended action                                                     |
| ----------------------------------- | ---------------------------------------------------------------------- |
| Q5 and Q15/Q24 — Auto-Configuration | Keep one detailed question                                             |
| Q7 and Q19 — Actuator/health        | Keep Actuator overview and health as separate subquestion              |
| Q10, Q26 and Q27 — IoC/DI           | Keep IoC vs DI and DI types separately                                 |
| Q22 and Q23                         | Keep container internals and bean lifecycle separately                 |
| Q6                                  | Correct the claim that Spring always requires XML and external servers |

The original numbering also skips **Q21**. A useful addition is:

> **Q21: What is Spring Initializr?**

---

# Part 1: Spring Boot Basic Questions

## Q1: What is Spring Boot, and why is it used?

Spring Boot is a framework built on top of the Spring Framework that simplifies the creation of standalone, production-oriented Java applications.

It reduces configuration and setup through:

- Auto-configuration
- Starter dependencies
- Embedded servers
- Externalized configuration
- Executable JAR packaging
- Actuator monitoring
- Testing support

### Interview answer

> Spring Boot builds on the Spring Framework and simplifies application development through auto-configuration, starters, embedded servers, externalized configuration, and production monitoring. It reduces boilerplate while still allowing developers to override the defaults.

---

## Q2: What are the main features of Spring Boot?

The main features are:

| Feature                    | Purpose                                                         |
| -------------------------- | --------------------------------------------------------------- |
| Auto-configuration         | Configures beans based on dependencies and properties           |
| Starters                   | Group compatible dependencies                                   |
| Embedded servers           | Run applications without separately deploying a server          |
| Actuator                   | Provides health, metrics, and operational endpoints             |
| Externalized configuration | Supports properties, YAML, environment variables, and arguments |
| Executable packaging       | Supports running applications using `java -jar`                 |
| DevTools                   | Provides restart and development-time features                  |
| Testing support            | Supports full-context and focused tests                         |
| Spring Initializr          | Generates starter projects                                      |

---

## Q3: What is `@SpringBootApplication`?

`@SpringBootApplication` is normally placed on the main application class.

```java
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

It combines:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

A simplified interview answer often says that it combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. More precisely, it uses `@SpringBootConfiguration`, which is based on `@Configuration`.

### Responsibilities

- `@SpringBootConfiguration` identifies the main configuration class.
- `@EnableAutoConfiguration` enables conditional Boot configuration.
- `@ComponentScan` discovers components in the application packages.

---

## Q4: What are Spring Boot starters?

Starters are dependency descriptors that group libraries commonly required for a particular capability.

Examples:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Important distinction

- A **starter** selects related dependencies.
- **Auto-configuration** creates beans based on those dependencies.

---

## Q5: What is Spring Boot auto-configuration?

Auto-configuration allows Spring Boot to register default infrastructure beans when certain conditions are satisfied.

Common condition annotations include:

```java
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnBean
@ConditionalOnProperty
@ConditionalOnWebApplication
```

Example:

```java
@AutoConfiguration
@ConditionalOnClass(PaymentClient.class)
public class PaymentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PaymentClient paymentClient(PaymentProperties properties) {
        return new PaymentClient(properties.getBaseUrl());
    }
}
```

If the application defines its own `PaymentClient`, the default bean is skipped because `@ConditionalOnMissingBean` no longer matches.

### Interview answer

> Spring Boot loads known auto-configuration classes and evaluates conditions based on the classpath, properties, application type, and existing beans. Matching configurations register default beans, while user-defined beans can cause those defaults to back off.

---

## Q6: How does Spring Boot differ from the Spring Framework?

| Spring Framework                                          | Spring Boot                                                |
| --------------------------------------------------------- | ---------------------------------------------------------- |
| Provides IoC, DI, MVC, AOP, transactions, and data access | Simplifies the setup and deployment of Spring applications |
| Supplies the core programming model                       | Adds conventions and defaults                              |
| Supports Java, annotations, and XML configuration         | Adds conditional auto-configuration                        |
| Dependencies can be selected manually                     | Provides starters                                          |
| Supports several deployment models                        | Commonly uses executable JARs and embedded servers         |
| Can be used without Spring Boot                           | Uses Spring internally                                     |

### Correction

It is inaccurate to say that the Spring Framework always requires XML or an external server. Modern Spring applications can use Java configuration and can be packaged in different ways.

---

## Q7: What is Spring Boot Actuator?

Spring Boot Actuator provides production monitoring and management capabilities.

Common endpoints include:

```text
/actuator/health
/actuator/metrics
/actuator/info
/actuator/conditions
/actuator/beans
/actuator/mappings
/actuator/loggers
/actuator/threaddump
/actuator/heapdump
```

Actuator can expose information about:

- Application health
- JVM memory
- HTTP requests
- Database connection pools
- Registered beans
- Request mappings
- Auto-configuration conditions
- Log levels

Sensitive endpoints should be authenticated and should not all be publicly exposed.

---

## Q8: What is the purpose of `application.properties` and `application.yml`?

These files hold externalized application configuration.

Properties example:

```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/orders
logging.level.com.example=DEBUG
```

YAML example:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders

logging:
  level:
    com.example: DEBUG
```

They can configure:

- Server ports
- Database connections
- Logging levels
- Feature flags
- Timeouts
- Cache settings
- Messaging
- Actuator

YAML is often easier to read for hierarchical configuration, but it is not automatically better than properties.

---

## Q9: How do you create a REST API in Spring Boot?

Add the web starter and create a REST controller.

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable long id) {
        return orderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.create(request);
    }
}
```

A production API should also include:

- DTOs
- Validation
- Service-layer business logic
- Global exception handling
- Authentication and authorization
- Pagination
- Logging and tracing
- Database constraints
- Idempotency where required

---

## Q10: What is dependency injection in Spring Boot?

Dependency injection means that a class receives its collaborators from the Spring container instead of constructing them itself.

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

Constructor injection is preferred for required dependencies because:

- Dependencies are explicit.
- Fields can be `final`.
- The object is fully initialized.
- Unit testing is easier.
- Circular dependencies fail clearly.

---

## Q11: What is `spring-boot-starter-parent`?

`spring-boot-starter-parent` is an optional Maven parent POM that supplies:

- Managed dependency versions
- Plugin configuration
- Java compiler defaults
- Resource-processing defaults
- Spring Boot build conventions

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>${spring-boot.version}</version>
    <relativePath/>
</parent>
```

It is optional. Projects with an existing corporate parent can import the Spring Boot BOM instead.

---

## Q12: How do you connect a Spring Boot application to a database?

### Add JPA support

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### Add a database driver

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Configure the connection

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders
    username: orders_app
    password: ${DB_PASSWORD}
```

### Create a repository

```java
public interface OrderRepository
        extends JpaRepository<OrderEntity, Long> {
}
```

Use Flyway or Liquibase for production schema migrations.

---

## Q13: What is Spring Boot DevTools?

Spring Boot DevTools provides development-time features such as:

- Automatic application restart
- LiveReload
- Development-oriented configuration defaults
- Faster local feedback

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

DevTools should not normally be used as part of the production runtime.

---

## Q14: How do you handle exceptions globally?

Use `@RestControllerAdvice` with `@ExceptionHandler`.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleNotFound(
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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
            Exception exception
    ) {
        log.error("Unexpected request failure", exception);

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }
}
```

Do not expose stack traces, SQL queries, credentials, or internal implementation details to API clients.

---

## Q15: What is `@EnableAutoConfiguration`?

`@EnableAutoConfiguration` tells Spring Boot to evaluate registered auto-configuration classes.

It considers:

- Classpath dependencies
- Existing beans
- Properties
- Application type
- Conditional annotations

It is already included in:

```java
@SpringBootApplication
```

Therefore, normal Boot applications do not need to declare it separately.

---

## Q16: How do you implement security in Spring Boot?

Use Spring Security and configure a `SecurityFilterChain`.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health")
                .permitAll()
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated()
        );

        return http.build();
    }
}
```

Possible authentication mechanisms include:

- Session authentication
- HTTP Basic
- OAuth 2.0
- OpenID Connect
- JWT bearer tokens
- API keys
- Mutual TLS

---

## Q17: What is the difference between `@Controller` and `@RestController`?

### `@Controller`

Normally used to return MVC view names.

```java
@Controller
public class PageController {

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
```

### `@RestController`

Equivalent to:

```text
@Controller
+
@ResponseBody
```

It writes returned values directly to the HTTP response body.

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public UserResponse find(@PathVariable long id) {
        return userService.find(id);
    }
}
```

---

## Q18: How do you create an executable Spring Boot JAR?

Using Maven:

```bash
mvn clean package
```

Run it using:

```bash
java -jar target/order-service.jar
```

The Spring Boot Maven or Gradle plugin packages the application and its dependencies into an executable archive.

---

## Q19: What does `/actuator/health` show?

Example request:

```text
GET /actuator/health
```

Basic response:

```json
{
  "status": "UP"
}
```

Health contributors can check:

- Database connectivity
- Disk space
- Redis
- Messaging systems
- Custom dependencies

Detailed health information may be hidden depending on security and configuration.

An `UP` result does not guarantee that every business workflow is functioning correctly. It only reflects the configured health checks.

---

## Q20: How do you implement caching in Spring Boot?

Enable caching:

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
}
```

Cache service results:

```java
@Service
public class ProductService {

    @Cacheable(
            cacheNames = "products",
            key = "#productId"
    )
    public ProductResponse findById(long productId) {
        return loadFromDatabase(productId);
    }

    @CacheEvict(
            cacheNames = "products",
            key = "#productId"
    )
    public void update(
            long productId,
            UpdateProductRequest request
    ) {
        updateDatabase(productId, request);
    }
}
```

Possible cache providers include:

- Caffeine
- Redis
- Hazelcast
- JCache implementations
- Simple in-memory caching

Production caching requires policies for:

- Maximum size
- TTL
- Eviction
- Invalidation
- Stale-data tolerance
- Failure behavior

---

## Q21: What is Spring Initializr?

Spring Initializr is a project-generation service used to create an initial Spring Boot project.

It allows developers to select:

- Build tool
- Java version
- Spring Boot version
- Packaging type
- Project metadata
- Dependencies

It generates a project containing files such as:

```text
pom.xml
src/main/java/
src/main/resources/application.properties
src/test/java/
```

It generates the initial structure but does not design the application architecture or business logic.

---

# Part 2: Spring Framework Internals

## Q22: How does the Spring container work internally?

A simplified sequence is:

```text
1. Discover and register bean definitions
2. Run BeanFactoryPostProcessor implementations
3. Register BeanPostProcessor implementations
4. Resolve constructor dependencies
5. Instantiate beans
6. Populate setter and field dependencies
7. Invoke lifecycle callbacks
8. Create proxies where required
9. Store and expose managed beans
```

The container manages:

- Bean construction
- Dependency resolution
- Bean scopes
- Lifecycle callbacks
- AOP proxies
- Destruction callbacks

---

## Q23: Explain the Spring bean lifecycle

A practical lifecycle is:

```text
1. Bean definition registered
2. Bean instantiated
3. Dependencies populated
4. Aware callbacks invoked
5. BeanPostProcessor before-initialization
6. @PostConstruct
7. InitializingBean.afterPropertiesSet()
8. Custom init method
9. BeanPostProcessor after-initialization
10. Bean ready for use
11. @PreDestroy
12. DisposableBean.destroy()
13. Custom destroy method
```

Example:

```java
@Component
public class PaymentClient {

    @PostConstruct
    void initialize() {
        // Initialize resources.
    }

    @PreDestroy
    void shutdown() {
        // Release owned resources.
    }
}
```

A singleton bean is not automatically thread-safe. Singleton means one instance per bean definition per Spring container.

---

## Q24: What is the difference between `@Primary` and `@Qualifier`?

Suppose multiple beans implement the same interface.

```java
public interface PaymentGateway {
}
```

### `@Primary`

Marks one implementation as the preferred default.

```java
@Primary
@Component
public class CardPaymentGateway
        implements PaymentGateway {
}
```

### `@Qualifier`

Selects a specific implementation at an injection point.

```java
@Service
public class RefundService {

    private final PaymentGateway gateway;

    public RefundService(
            @Qualifier("bankPaymentGateway")
            PaymentGateway gateway
    ) {
        this.gateway = gateway;
    }
}
```

| `@Primary`                                           | `@Qualifier`                                            |
| ---------------------------------------------------- | ------------------------------------------------------- |
| Defines a preferred default                          | Explicitly selects a bean                               |
| Usually placed on the bean                           | Usually placed at the injection point                   |
| Useful when one implementation is normally preferred | Useful when consumers require different implementations |
| Less explicit                                        | More explicit                                           |

---

## Q25: What is the difference between IoC and dependency injection?

### Inversion of Control

IoC is the broader principle that the framework controls object creation, configuration, and lifecycle.

### Dependency Injection

DI is the mechanism through which dependencies are supplied to an object.

```text
IoC:
Spring controls bean construction and lifecycle.

DI:
Spring supplies beans with the collaborators they require.
```

### Interview answer

> IoC is the broader principle of transferring control to the framework. Dependency injection is the mechanism Spring uses to implement IoC by supplying each object with its dependencies.

---

## Q26: What are the types of dependency injection?

### Constructor injection

```java
@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

Best for required dependencies.

### Setter injection

```java
@Component
public class ReportService {

    private AuditPublisher auditPublisher;

    @Autowired
    public void setAuditPublisher(
            AuditPublisher auditPublisher
    ) {
        this.auditPublisher = auditPublisher;
    }
}
```

Suitable for optional or reconfigurable dependencies.

### Field injection

```java
@Autowired
private OrderRepository repository;
```

Supported, but generally discouraged because dependencies are hidden and testing is more difficult.

---

## Q27: What is a Spring profile?

Profiles allow selected configuration and beans to be activated for a particular environment.

```java
@Configuration
@Profile("prod")
public class ProductionConfiguration {
}
```

Profile files:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Activate a profile:

```bash
java -jar application.jar \
  --spring.profiles.active=prod
```

Profiles are useful for environment-specific configuration, but they are not a replacement for secret management.

---

## Q28: What is the difference between `@PathVariable` and `@RequestParam`?

### `@PathVariable`

Reads a value from the URI path.

```java
@GetMapping("/orders/{orderId}")
public OrderResponse find(
        @PathVariable long orderId
) {
    return orderService.find(orderId);
}
```

Request:

```text
GET /orders/101
```

Use it to identify a resource.

### `@RequestParam`

Reads a query-string parameter.

```java
@GetMapping("/orders")
public List<OrderResponse> search(
        @RequestParam(required = false)
        String status,

        @RequestParam(defaultValue = "20")
        int size
) {
    return orderService.search(status, size);
}
```

Request:

```text
GET /orders?status=CONFIRMED&size=20
```

Use query parameters for filtering, sorting, pagination, and optional modifiers.

---

## Q29: How does `@Transactional` work?

Spring normally implements `@Transactional` using AOP proxies.

```java
@Service
public class TransferService {

    @Transactional
    public void transfer(
            long sourceId,
            long destinationId,
            long amount
    ) {
        debit(sourceId, amount);
        credit(destinationId, amount);
    }
}
```

Conceptual flow:

```text
Caller
  ↓
Transaction proxy
  ↓
Start or join transaction
  ↓
Invoke target method
  ↓
Commit on success
or
Roll back on configured failure
```

Important points:

- Transaction boundaries normally belong in services.
- Self-invocation can bypass proxy interception.
- Runtime exceptions and errors normally cause rollback by default.
- Checked exceptions require explicit rollback configuration where needed.
- Database transactions do not automatically include remote HTTP calls or message brokers.

---

## Q30: What is lazy and eager loading in JPA?

Fetch type controls when associated entities are loaded.

### Lazy loading

```java
@OneToMany(
        mappedBy = "order",
        fetch = FetchType.LAZY
)
private List<OrderItemEntity> items;
```

The relationship is loaded when accessed.

Advantages:

- Avoids unnecessary loading
- Reduces initial query size

Risks:

- Lazy-loading exceptions outside the persistence context
- N+1 queries

### Eager loading

```java
@ManyToOne(fetch = FetchType.EAGER)
private CustomerEntity customer;
```

The association is loaded with the owning entity.

Risks:

- Unnecessary data loading
- Large joins
- Additional queries
- Unpredictable performance

Traditional JPA defaults are:

| Relationship  | Default |
| ------------- | ------- |
| `@OneToMany`  | `LAZY`  |
| `@ManyToMany` | `LAZY`  |
| `@ManyToOne`  | `EAGER` |
| `@OneToOne`   | `EAGER` |

Prefer explicit fetch joins, entity graphs, DTO projections, or batch fetching rather than marking every relationship eager.

---

# Part 3: Maven and Build

## Q31: What is a Maven build?

A Maven build executes lifecycle phases and plugin goals based on the project’s `pom.xml`.

The POM defines:

- Dependencies
- Plugins
- Packaging
- Modules
- Repositories
- Build profiles
- Project metadata

Important default lifecycle phases include:

```text
validate
→ compile
→ test
→ package
→ verify
→ install
→ deploy
```

Calling a phase also executes the preceding phases in that lifecycle.

---

## Q32: What does `mvn clean install` do?

```bash
mvn clean install
```

### `clean`

Removes previous build output, normally:

```text
target/
```

### `install`

Executes the default lifecycle through:

```text
validate
compile
test
package
verify
install
```

It then installs the built artifact into the local Maven repository:

```text
~/.m2/repository
```

It does not deploy the application to production or upload the artifact to a remote repository.

---

## Q33: How do you push code to production?

A typical production process is:

```text
Developer branch
→ pull request
→ code review
→ unit tests
→ integration tests
→ static analysis
→ security scanning
→ build versioned artifact
→ publish artifact or image
→ deploy to staging
→ smoke tests
→ production promotion
→ health verification
→ monitoring
→ rollback or roll-forward if necessary
```

Important practices include:

- Build once and promote the same artifact.
- Keep environment configuration outside the artifact.
- Use an artifact repository or container registry.
- Apply database migrations safely.
- Use readiness and health checks.
- Prefer rolling, canary, or blue-green deployments.
- Avoid deploying directly from a developer machine.

---

## Q34: What is code coverage?

Code coverage measures which parts of the application were executed during tests.

Common metrics include:

- Instruction coverage
- Line coverage
- Branch coverage
- Method coverage
- Class coverage

Coverage identifies untested code, but it does not prove that the tests contain meaningful assertions.

---

## Q35: What is line coverage?

Line coverage measures whether executable source-code lines were executed during tests.

```java
if (account.isActive()) {
    approve(account);
} else {
    reject(account);
}
```

A test may execute the condition but still cover only one possible outcome.

Therefore, high line coverage does not necessarily mean that all behavior has been tested.

---

## Q36: What is branch coverage?

Branch coverage measures whether each possible outcome of a decision has been executed.

For:

```java
if (balance >= amount) {
    approve();
} else {
    reject();
}
```

Tests should cover:

- `balance >= amount`
- `balance < amount`

Branch coverage is particularly important for validation and business rules.

---

## Q37: How do you improve code coverage when a build fails?

First determine the reason for the failure.

### Functional test failure

```text
Read the failed assertion
→ reproduce locally
→ fix the implementation or test
→ rerun affected tests
→ run the full build
```

### Coverage threshold failure

Open the JaCoCo report, commonly:

```text
target/site/jacoco/index.html
```

Add meaningful tests for:

- Business branches
- Boundary values
- Invalid input
- Empty collections
- Exception paths
- Authorization failures
- Retry logic
- Transaction rollback
- Mapping behavior

### Code is difficult to test

Improve the design:

- Move business logic out of controllers.
- Use constructor injection.
- Extract large methods.
- Separate calculations from I/O.
- Wrap external services behind interfaces.
- Remove static global state.

### Coverage tooling failure

Check:

- JaCoCo and JDK compatibility
- Surefire and Failsafe configuration
- Test fork settings
- `argLine` overrides
- Unit and integration report aggregation
- Whether tests are actually running

Do not write meaningless tests merely to increase the percentage.
