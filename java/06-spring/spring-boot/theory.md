# Basic Questions — Spring Boot, Spring Framework, Maven, and Build

## Q1: What is Spring Boot, and why is it used?

Spring Boot is a framework built on top of the Spring Framework that simplifies the creation of stand-alone, production-ready Java applications.

It reduces manual setup through:

- Auto-configuration
- Starter dependencies
- Embedded web servers
- Externalized configuration
- Executable JAR packaging
- Actuator monitoring
- Testing support

Spring Boot is commonly used for REST APIs, microservices, batch applications, and event-driven services.

### Interview-ready answer

> Spring Boot builds on Spring and reduces configuration using auto-configuration, starters, embedded servers, externalized properties, and production monitoring. It allows developers to create and deploy Spring applications more quickly without removing the ability to customize the framework.

---

## Q2: What are the key features of Spring Boot?

The main features are:

| Feature                    | Purpose                                                                                      |
| -------------------------- | -------------------------------------------------------------------------------------------- |
| Auto-configuration         | Configures beans based on dependencies, properties, and existing beans                       |
| Starters                   | Provide curated dependency sets                                                              |
| Embedded servers           | Run web applications without deploying to a separate server                                  |
| Actuator                   | Exposes health, metrics, and operational information                                         |
| Externalized configuration | Reads configuration from properties, YAML, environment variables, and command-line arguments |
| Executable packaging       | Supports running applications with `java -jar`                                               |
| DevTools                   | Provides development-time restart and LiveReload support                                     |
| Testing support            | Provides full-context and focused test utilities                                             |
| Spring Initializr          | Generates a starter project structure                                                        |

Spring Boot CLI exists, but it is not a central requirement for normal Spring Boot development.

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

A common simplified answer says it combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. More precisely, it uses `@SpringBootConfiguration`, which is a specialized Spring configuration annotation.

### Responsibilities

- `@SpringBootConfiguration` — identifies the main configuration class.
- `@EnableAutoConfiguration` — enables conditional auto-configuration.
- `@ComponentScan` — discovers controllers, services, repositories, and other components.

---

## Q4: What are Spring Boot starters?

Starters are dependency descriptors that group the libraries normally required for a specific capability.

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

A starter selects related dependencies. Auto-configuration contains the runtime logic that creates beans based on those dependencies.

### Interview-ready answer

> Spring Boot starters are curated dependency sets for common features such as web development, JPA, security, validation, and testing. They simplify dependency selection and version compatibility.

---

## Q5: What is Spring Boot auto-configuration?

Auto-configuration creates default Spring beans when specific conditions are satisfied.

Spring Boot evaluates conditions such as:

- Is a required class available?
- Does a bean already exist?
- Is a feature enabled by a property?
- Is this a servlet, reactive, or non-web application?
- Is a required resource available?

Common conditional annotations include:

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

When the application defines its own `PaymentClient`, the default bean is not created because `@ConditionalOnMissingBean` no longer matches.

---

## Q6: How does Spring Boot differ from the Spring Framework?

| Spring Framework                                                      | Spring Boot                                                    |
| --------------------------------------------------------------------- | -------------------------------------------------------------- |
| Provides IoC, DI, MVC, AOP, transactions, validation, and data access | Simplifies building and deploying Spring applications          |
| Supplies the core programming model                                   | Adds conventions and opinionated defaults                      |
| Configuration can be Java, annotations, or XML                        | Adds conditional auto-configuration                            |
| Dependencies may be selected individually                             | Provides starter dependencies                                  |
| Supports many deployment approaches                                   | Commonly creates executable applications with embedded servers |
| Can be used without Spring Boot                                       | Uses Spring Framework internally                               |

It is inaccurate to say that Spring always requires XML or an external server. Spring supports Java-based configuration, and deployment choices depend on the application.

---

## Q7: What is Spring Boot Actuator?

Spring Boot Actuator provides production-oriented monitoring and management endpoints.

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
- JVM metrics
- HTTP requests
- Database connection pools
- Registered beans
- Request mappings
- Auto-configuration conditions
- Log levels

Sensitive Actuator endpoints should be protected and should not all be exposed publicly.

---

## Q8: What is the purpose of `application.properties` or `application.yml`?

These files provide externalized application configuration.

Typical settings include:

- Server port
- Database connection
- Logging levels
- Cache settings
- Messaging configuration
- Feature flags
- Timeouts
- Actuator configuration

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

YAML can be easier to read for hierarchical configuration, but neither format is inherently superior.

Spring Boot can also read values from:

- Environment variables
- JVM system properties
- Command-line arguments
- External configuration files
- Secret-management systems

---

## Q9: How do you create a REST API in Spring Boot?

Add the web starter and define a REST controller.

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

A production-ready API should also include:

- DTOs
- Request validation
- Service-layer business logic
- Global exception handling
- Authentication and authorization
- Pagination
- Logging and tracing
- Database constraints
- Idempotency for retried side effects

---

## Q10: What is dependency injection in Spring Boot?

Dependency injection means that an object receives its dependencies from the Spring container instead of constructing them itself.

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

Spring resolves the `OrderRepository` bean and passes it to the constructor.

Constructor injection is generally preferred for required dependencies because:

- Dependencies are explicit.
- Fields can be `final`.
- The object is fully initialized.
- Direct unit testing is simple.
- Constructor cycles fail clearly.

---

## Q11: What is `spring-boot-starter-parent`?

`spring-boot-starter-parent` is an optional Maven parent POM that provides:

- Managed dependency versions
- Plugin defaults
- Java compilation configuration
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

It is not mandatory. Projects with a corporate parent POM can import the Spring Boot dependency-management BOM instead.

---

## Q12: How do you connect a Spring Boot application to a database?

### Step 1: Add JPA or JDBC support

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### Step 2: Add the database driver

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Step 3: Configure the connection

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders
    username: orders_app
    password: ${DB_PASSWORD}
```

### Step 4: Create a repository

```java
public interface OrderRepository
        extends JpaRepository<OrderEntity, Long> {
}
```

Use a migration tool such as Flyway or Liquibase for production database schema changes.

---

## Q13: What is Spring Boot DevTools?

Spring Boot DevTools provides development-time features such as:

- Automatic application restart
- LiveReload support
- Development-oriented property defaults
- Faster feedback during local development

It should normally be included only for development.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

DevTools is not a production monitoring or deployment tool.

---

## Q14: How do you handle exceptions in Spring Boot?

Use centralized exception handling with:

- `@ExceptionHandler`
- `@ControllerAdvice`
- `@RestControllerAdvice`
- `ResponseEntityExceptionHandler`
- `ProblemDetail`

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

Do not expose stack traces, SQL statements, secrets, or internal implementation details to clients.

---

## Q15: What is `@EnableAutoConfiguration`?

`@EnableAutoConfiguration` tells Spring Boot to evaluate its registered auto-configuration classes.

It conditionally registers infrastructure based on:

- Classpath dependencies
- Existing beans
- Configuration properties
- Application type
- Active conditions

Most applications do not add it separately because it is already included in:

```java
@SpringBootApplication
```

---

## Q16: How do you implement security in Spring Boot?

Add Spring Security and configure a `SecurityFilterChain`.

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

Authentication may use:

- Session-based login
- HTTP Basic
- OAuth 2.0
- OpenID Connect
- JWT bearer tokens
- API keys
- Mutual TLS

Security requires both:

```text
Authentication → Who are you?
Authorization  → What may you do?
```

---

## Q17: What is the difference between `@Controller` and `@RestController`?

### `@Controller`

Typically used for MVC pages that return view names.

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

Combines:

```text
@Controller
@ResponseBody
```

It writes return values directly into the HTTP response body.

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public UserResponse find(@PathVariable long id) {
        return userService.find(id);
    }
}
```

A normal `@Controller` method can also return JSON when annotated with `@ResponseBody`.

---

## Q18: How do you create a Spring Boot executable JAR?

Use the Spring Boot Maven or Gradle plugin.

For Maven:

```bash
mvn clean package
```

Run the generated application:

```bash
java -jar target/order-service.jar
```

The Spring Boot plugin packages the application classes and dependencies into an executable archive.

---

## Q19: What does the Actuator health endpoint show?

The health endpoint is normally available at:

```text
GET /actuator/health
```

A basic response might be:

```json
{
  "status": "UP"
}
```

Health contributors may report information about:

- Database connectivity
- Disk space
- Redis
- Messaging systems
- Custom dependencies

Detailed health information may be hidden depending on configuration and authorization.

A health response of `UP` means the configured health contributors consider the application healthy. It does not guarantee that every business workflow is functioning correctly.

---

## Q20: How do you implement caching in Spring Boot?

Enable Spring’s caching abstraction:

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
}
```

Cache method results:

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
- Simple in-memory cache

A production cache should define:

- Maximum size
- Time to live
- Eviction
- Invalidation
- Stale-data tolerance
- Failure behavior
- Cache-key design

---

# Spring Framework and Spring Boot

## Q22: How does the Spring container work internally?

A simplified process is:

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

Bean definitions can come from:

- Component scanning
- `@Bean` methods
- Imported configuration
- Auto-configuration
- XML
- Programmatic registration

The container manages:

- Object creation
- Dependency resolution
- Bean scopes
- Lifecycle callbacks
- AOP proxying
- Destruction callbacks

---

## Q23: Explain the Spring bean lifecycle

A practical bean lifecycle is:

```text
1. Bean definition registered
2. Bean instantiated
3. Dependencies populated
4. Aware callbacks invoked
5. BeanPostProcessor before-initialization callbacks
6. @PostConstruct
7. InitializingBean.afterPropertiesSet()
8. Custom init method
9. BeanPostProcessor after-initialization callbacks
10. Bean ready for use
11. @PreDestroy during shutdown
12. DisposableBean.destroy()
13. Custom destroy method
```

Example:

```java
@Component
public class PaymentClient {

    @PostConstruct
    void initialize() {
        // Validate configuration or initialize resources.
    }

    @PreDestroy
    void shutdown() {
        // Close resources owned by this bean.
    }
}
```

### Important points

- Singleton is the default scope.
- Singleton means one instance per bean definition per Spring container.
- Singleton beans are not automatically thread-safe.
- Prototype beans are initialized by Spring, but their destruction is normally managed by the client.

---

## Q24: What is `@EnableAutoConfiguration`?

It enables the Spring Boot auto-configuration import mechanism.

Spring Boot:

1. Finds known auto-configuration candidates.
2. Evaluates their conditions.
3. Imports matching configurations.
4. Registers their beans.
5. Backs off when custom configuration replaces a default.

In normal applications, use:

```java
@SpringBootApplication
```

instead of declaring `@EnableAutoConfiguration` separately.

---

## Q25: What is `@Qualifier`, and what is the difference between `@Primary` and `@Qualifier`?

Suppose two beans implement the same interface:

```java
public interface PaymentGateway {
}
```

```java
@Component
public class CardPaymentGateway
        implements PaymentGateway {
}
```

```java
@Component
public class BankPaymentGateway
        implements PaymentGateway {
}
```

### `@Primary`

Marks one bean as the preferred default.

```java
@Primary
@Component
public class CardPaymentGateway
        implements PaymentGateway {
}
```

### `@Qualifier`

Selects a specific bean at an injection point.

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

| `@Primary`                                         | `@Qualifier`                                       |
| -------------------------------------------------- | -------------------------------------------------- |
| Defines the preferred default bean                 | Selects a specific bean                            |
| Usually placed on the implementation               | Usually placed at the injection point              |
| Used when one implementation is normally preferred | Used when consumers need different implementations |
| Can be overridden by a qualifier                   | More explicit than primary selection               |

---

## Q26: What is the difference between IoC and dependency injection?

### Inversion of Control

IoC is the broader principle that the framework controls object creation, configuration, and lifecycle.

### Dependency Injection

DI is the mechanism through which objects receive their dependencies from outside.

```text
IoC:
Spring controls bean construction and lifecycle.

DI:
Spring supplies each bean with the collaborators it requires.
```

### Interview-ready answer

> IoC is the broader principle of transferring control to the framework. Dependency injection is the technique Spring uses to implement IoC by supplying objects with their dependencies.

---

## Q27: What is dependency injection, and what are its types?

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

Best for mandatory dependencies.

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

Suitable for genuinely optional or reconfigurable dependencies.

### Field injection

```java
@Autowired
private OrderRepository repository;
```

Supported, but generally discouraged because:

- Dependencies are hidden.
- Fields cannot easily be final.
- Unit testing is harder.
- The object can be partially initialized.

---

## Q28: What is a Spring profile, and how do you use it?

Profiles activate environment-specific beans and configuration.

```java
@Configuration
@Profile("prod")
public class ProductionConfiguration {
}
```

Configuration files:

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

or:

```bash
export SPRING_PROFILES_ACTIVE=prod
```

Profiles are commonly used for:

- Environment-specific endpoints
- Development-only components
- Test databases
- Logging configuration
- Cloud-specific beans

Profiles should not be used as a substitute for secret management.

---

## Q29: What is the difference between `@PathVariable` and `@RequestParam`?

### `@PathVariable`

Extracts a value from the URI path.

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

Use it when the value identifies a resource.

### `@RequestParam`

Extracts a query-string parameter.

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

Use query parameters for:

- Filtering
- Pagination
- Sorting
- Search criteria
- Optional modifiers

---

## Q30: How does `@Transactional` work?

Spring normally implements `@Transactional` using AOP proxies.

```java
@Service
public class TransferService {

    @Transactional
    public void transfer(
            long sourceAccountId,
            long destinationAccountId,
            long amount
    ) {
        debit(sourceAccountId, amount);
        credit(destinationAccountId, amount);
    }
}
```

Conceptually:

```text
Caller
  ↓
Spring transaction proxy
  ↓
Start or join transaction
  ↓
Call target method
  ↓
Commit on success
or
Roll back on configured failure
```

### Important points

- Transaction boundaries usually belong in service methods.
- Proxy interception normally occurs when another bean calls the method.
- Self-invocation can bypass the transactional proxy.
- Runtime exceptions and errors normally cause rollback by default.
- Checked exceptions require explicit rollback configuration when needed.
- Propagation controls whether a method joins or creates a transaction.
- A database transaction does not include remote HTTP calls or Kafka automatically.

Example custom rollback rule:

```java
@Transactional(rollbackFor = Exception.class)
public void performOperation() throws Exception {
}
```

---

## Q31: What is fetch type in JPA?

Fetch type controls when related entities are loaded.

### Lazy loading

```java
@OneToMany(
        mappedBy = "order",
        fetch = FetchType.LAZY
)
private List<OrderItemEntity> items;
```

The association is loaded when it is accessed.

Benefits:

- Avoids loading unnecessary data
- Reduces the initial query size

Risks:

- Access outside the persistence context may fail.
- Iteration can trigger the N+1 query problem.

### Eager loading

```java
@ManyToOne(fetch = FetchType.EAGER)
private CustomerEntity customer;
```

The association is loaded with the owning entity.

Risks:

- Loads data even when it is not required.
- Can generate expensive joins or additional queries.
- Makes query performance less predictable.

### Traditional defaults

| Relationship  | Default fetch type |
| ------------- | ------------------ |
| `@OneToMany`  | `LAZY`             |
| `@ManyToMany` | `LAZY`             |
| `@ManyToOne`  | `EAGER`            |
| `@OneToOne`   | `EAGER`            |

Do not solve every lazy-loading issue by making all relationships eager. Prefer explicit query strategies such as:

- Fetch joins
- Entity graphs
- DTO projections
- Batch fetching
- Repository-specific queries

---

# Maven and Build

## Q32: What is a Maven build?

A Maven build executes lifecycle phases and plugin goals based on a project’s `pom.xml`.

The POM defines:

- Project coordinates
- Dependencies
- Plugins
- Packaging
- Modules
- Build configuration
- Profiles
- Repositories

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

When a phase is executed, Maven also executes the preceding phases in that lifecycle.

---

## Q33: What does `mvn clean install` do?

```bash
mvn clean install
```

### `clean`

Removes generated build output, normally:

```text
target/
```

### `install`

Runs the default lifecycle through:

```text
validate
compile
test
package
verify
install
```

The built artifact is then installed into the local Maven repository:

```text
~/.m2/repository
```

It does not:

- Deploy the application to production
- Upload the artifact to a remote repository
- Release a new production version

Remote artifact publication normally happens during the Maven `deploy` phase.

---

## Q34: How do you push code to production?

Production deployment should occur through a controlled CI/CD pipeline.

```text
Developer branch
→ pull request
→ code review
→ unit tests
→ integration tests
→ static analysis
→ security scans
→ build versioned artifact
→ publish artifact or image
→ deploy to staging
→ smoke tests
→ approval or automated promotion
→ production deployment
→ health verification
→ monitoring
→ rollback or roll-forward if required
```

### Important practices

- Build once and promote the same artifact.
- Keep environment configuration outside the binary.
- Use an artifact repository or container registry.
- Apply database migrations safely.
- Use readiness and health checks.
- Prefer rolling, canary, or blue-green deployments.
- Monitor error rate, latency, traffic, and saturation.
- Maintain a tested rollback or roll-forward strategy.
- Avoid deploying directly from a developer laptop.

---

## Q35: What is code coverage?

Code coverage measures which parts of the code were executed during tests.

Common metrics include:

- Instruction coverage
- Line coverage
- Branch coverage
- Method coverage
- Class coverage

Coverage can identify untested code, but it does not prove that tests contain meaningful assertions.

```text
High coverage
does not automatically mean
high-quality tests.
```

A test can execute every line without correctly verifying behavior.

---

## Q36: What is line coverage?

Line coverage measures whether executable source-code lines were executed during tests.

```java
if (account.isActive()) {
    approve(account);
} else {
    reject(account);
}
```

One test may execute the `if` statement but cover only one outcome.

Therefore, line coverage alone may miss untested branches.

---

## Q37: What is branch coverage?

Branch coverage measures whether every possible branch of a decision was executed.

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

Branch coverage is especially important for business logic containing:

- `if` conditions
- `switch` statements
- Boolean combinations
- Validation branches
- Exception paths

---

## Q38: How do you improve code coverage when a build fails?

First determine why the build failed.

### Case 1: Functional tests failed

```text
Read the failing assertion
→ reproduce locally
→ fix the implementation or incorrect expectation
→ rerun the affected tests
→ run the complete build
```

Do not add meaningless tests simply to increase coverage while functional tests remain broken.

### Case 2: Coverage threshold failed

Open the coverage report, commonly:

```text
target/site/jacoco/index.html
```

Look for untested:

- Business branches
- Boundary conditions
- Failure paths
- Invalid input
- Empty collections
- Authorization failures
- Retry logic
- Transaction rollback
- Mapping behavior

### Case 3: Code is difficult to test

Improve the design:

- Move business logic out of controllers.
- Use constructor injection.
- Extract large methods.
- Separate calculations from I/O.
- Wrap external services behind interfaces.
- Remove hidden global state.
- Avoid static dependencies.

### Case 4: Infrastructure or generated code lowers coverage

Exclude code only when the project has a documented reason, such as:

- Generated source
- Framework bootstrap classes
- Generated clients
- Pure configuration metadata

Do not exclude important business logic merely to satisfy the percentage.

### Case 5: Coverage tooling fails

Check:

- JaCoCo compatibility with the JDK
- Maven Surefire and Failsafe configuration
- Test fork settings
- `argLine` overrides
- Unit and integration report aggregation
- Whether tests are actually running
- Whether debug information is available

---

# Quick Interview Cheat Sheet

```text
Spring Boot:
Simplifies Spring application setup and deployment.

Main features:
Auto-configuration
Starters
Embedded servers
Actuator
Externalized configuration
Executable packaging

@SpringBootApplication:
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan

Starter:
Curated dependency set.

Auto-configuration:
Conditional default bean registration.

Actuator:
Health, metrics, mappings, conditions, and diagnostics.

Dependency injection:
The container supplies required collaborators.

@Primary:
Preferred default bean.

@Qualifier:
Selects a specific bean.

Profile:
Activates environment-specific configuration.

@PathVariable:
Reads a value from the URI path.

@RequestParam:
Reads a value from the query string.

@Transactional:
Proxy-based transaction boundary.

Lazy:
Association is loaded when needed.

Eager:
Association is loaded immediately.

Maven build:
Lifecycle-driven build defined by pom.xml.

mvn clean install:
Clean, compile, test, package, verify, and install locally.

Code coverage:
Measures executed code.

Line coverage:
Measures executed source lines.

Branch coverage:
Measures alternative decision paths.
```

# Short Interview Summary

> Spring Boot builds on Spring and simplifies application development through starters, conditional auto-configuration, embedded servers, externalized configuration, executable packaging, and Actuator. The Spring container registers bean definitions, resolves dependencies, manages lifecycle callbacks, and creates proxies for features such as transactions. I use constructor injection for required dependencies, profiles for environment-specific configuration, and explicit fetch strategies for JPA relationships. In Maven, `mvn clean install` cleans the build, runs tests and verification, packages the application, and installs it into the local repository. Code coverage is useful for finding untested areas, but meaningful branch and behavior coverage is more important than achieving a percentage alone.
