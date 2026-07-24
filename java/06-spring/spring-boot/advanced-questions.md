# Advanced Questions — Spring Boot and Microservices

The uploaded question bank contains strong material, but several questions repeat across auto-configuration, Actuator, testing, transactions, and microservices. I consolidated them below and corrected version-sensitive or misleading claims.

## Recommended Repository Structure

```text
spring/
├── spring-boot/
│   ├── advanced-questions.md
│   ├── auto-configuration.md
│   └── startup-and-performance.md
├── spring-core/
│   ├── aop.md
│   ├── bean-lifecycle.md
│   └── transactions.md
├── spring-actuator/
│   └── observability.md
├── spring-testing/
│   └── advanced-questions.md
└── spring-microservices/
    ├── communication.md
    ├── consistency.md
    └── resilience.md
```

## Important Corrections

- `@SpringBootApplication` precisely combines `@SpringBootConfiguration`, `@EnableAutoConfiguration`, and `@ComponentScan`.
- Modern custom auto-configurations use `@AutoConfiguration` and `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- `spring.factories` still has framework uses, but it is no longer the registration mechanism for modern Spring Boot auto-configuration.
- Current Redis connection properties use the `spring.data.redis.*` namespace; cache behavior uses `spring.cache.redis.*`.
- `WebFlux` is not limited to Netty, although Reactor Netty is a common default.
- `@Async` does not automatically create safe, unlimited background capacity. It requires a properly sized executor.
- Local Spring events and AOP do not communicate across microservice processes.
- Redis alone does not guarantee idempotency unless the design handles atomicity, expiration, crashes, and durable business state.
- In current Spring Framework documentation, `RestClient` is the modern synchronous client, while `WebClient` is intended for reactive, asynchronous, and streaming scenarios. `RestTemplate` is legacy and is deprecated in Spring Framework 7. ([Home][1])

---

# Part 1: Spring Boot Internals

## Q1: What is Spring Boot, and how is it different from Spring Framework?

Spring Framework provides the fundamental programming model:

- IoC and dependency injection
- AOP
- Transactions
- Spring MVC and WebFlux
- Validation
- Data-access integration
- Testing infrastructure

Spring Boot builds on those features and adds:

- Conditional auto-configuration
- Starter dependencies
- Embedded server support
- Externalized configuration
- Executable packaging
- Actuator and observability integration

Spring Boot does not replace Spring. It configures Spring applications using conventions and conditional defaults. ([Home][2])

---

## Q2: What does `@SpringBootApplication` do internally?

It enables three main capabilities:

```text
@SpringBootApplication
=
@SpringBootConfiguration
+
@EnableAutoConfiguration
+
@ComponentScan
```

- `@SpringBootConfiguration` identifies the primary Boot configuration class.
- `@EnableAutoConfiguration` imports matching auto-configuration classes.
- `@ComponentScan` discovers application-owned Spring components.

The application class should normally be placed in a root package above controllers, services, repositories, and configuration classes. ([Home][3])

---

## Q3: What happens inside Spring Boot during startup?

A practical startup flow is:

```text
main()
  ↓
SpringApplication.run()
  ↓
Prepare listeners and bootstrap state
  ↓
Create and configure Environment
  ↓
Resolve profiles and configuration
  ↓
Determine application type
  ↓
Create ApplicationContext
  ↓
Register application bean definitions
  ↓
Import and evaluate auto-configurations
  ↓
Refresh the context
  ↓
Instantiate non-lazy singleton beans
  ↓
Start the embedded web server
  ↓
Run ApplicationRunner and CommandLineRunner
  ↓
Publish ApplicationReadyEvent
```

The process is not completely linear internally. Configuration processing, condition evaluation, post-processors, bean creation, and server initialization happen as part of context preparation and refresh.

### Interview answer

> `SpringApplication.run()` prepares the environment, resolves profiles, creates the appropriate application context, registers application and auto-configuration bean definitions, refreshes the context, creates beans, starts the web server, runs startup runners, and publishes the ready event.

---

## Q4: How does Spring Boot auto-configuration work internally?

Spring Boot loads a known collection of auto-configuration candidates. Each candidate is a regular configuration class constrained by conditions.

Typical conditions include:

```java
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnBean
@ConditionalOnProperty
@ConditionalOnWebApplication
@ConditionalOnResource
```

Conceptually:

```text
Auto-configuration candidate
        ↓
Evaluate classpath conditions
        ↓
Evaluate bean conditions
        ↓
Evaluate property conditions
        ↓
Condition matches?
   ├── Yes → register bean definitions
   └── No  → skip configuration
```

Auto-configuration attempts to provide defaults but commonly backs off when application code defines a competing bean. ([Home][4])

---

## Q5: How does `@ConditionalOnClass` work?

`@ConditionalOnClass` activates configuration only when specified classes are available to the application class loader.

```java
@AutoConfiguration
@ConditionalOnClass(RedisConnectionFactory.class)
public class CustomRedisAutoConfiguration {
}
```

This allows an optional integration to remain inactive when its dependency is absent.

It is commonly combined with `@ConditionalOnMissingBean`:

```java
@Bean
@ConditionalOnMissingBean
public CacheService cacheService() {
    return new RedisCacheService();
}
```

This means:

```text
Redis class exists
+
No application-defined CacheService exists
=
Create the default CacheService
```

Spring Boot recommends class and missing-bean conditions for custom auto-configuration. ([Home][4])

---

## Q6: How do you create custom auto-configuration?

### Auto-configuration class

```java
@AutoConfiguration
@EnableConfigurationProperties(PaymentProperties.class)
@ConditionalOnClass(PaymentClient.class)
public class PaymentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "payment.client",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    PaymentClient paymentClient(PaymentProperties properties) {
        return new PaymentClient(
                properties.getBaseUrl(),
                properties.getTimeout()
        );
    }
}
```

### Registration file

Create:

```text
META-INF/spring/
org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Add:

```text
com.example.payment.PaymentAutoConfiguration
```

Custom auto-configuration should be listed in the imports file and should generally not be discovered through normal component scanning. ([Home][4])

---

## Q7: Is `spring.factories` still used?

For modern Spring Boot auto-configuration, use:

```text
META-INF/spring/
org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Do not register new auto-configuration classes through the old `EnableAutoConfiguration` entry in `spring.factories`.

However, the Spring factories-loading mechanism still exists for selected extension points such as property-source loaders and other framework strategies. Therefore, it is incorrect to say that `spring.factories` has disappeared completely. ([Home][4])

---

## Q8: What is `@ConditionalOnProperty`?

It creates configuration only when a property satisfies a condition.

```java
@Bean
@ConditionalOnProperty(
        prefix = "notification.sms",
        name = "enabled",
        havingValue = "true"
)
SmsNotificationService smsNotificationService() {
    return new SmsNotificationService();
}
```

Configuration:

```yaml
notification:
  sms:
    enabled: true
```

Common uses include:

- Feature toggles
- Optional external integrations
- Choosing mock versus real clients
- Enabling scheduled jobs
- Enabling an alternative implementation

Feature flags that must change dynamically at runtime usually require more than a startup condition because conditional beans are generally decided when the application context is created.

---

## Q9: What is the difference between `@Import` and `@ImportResource`?

### `@Import`

Imports Java configuration classes, selectors, or registrars.

```java
@Configuration
@Import({
        PaymentConfiguration.class,
        AuditConfiguration.class
})
public class ApplicationConfiguration {
}
```

### `@ImportResource`

Imports XML bean definitions.

```java
@Configuration
@ImportResource("classpath:legacy-context.xml")
public class LegacyIntegrationConfiguration {
}
```

Use `@ImportResource` mainly when integrating legacy Spring XML configuration.

---

## Q10: How do profiles and externalized configuration work?

Profiles activate selected beans and profile-specific configuration.

```java
@Configuration
@Profile("prod")
public class ProductionConfiguration {
}
```

```yaml
# application-prod.yml
payment:
  base-url: https://payments.example.com
```

Activate it with:

```bash
java -jar application.jar --spring.profiles.active=prod
```

Configuration can come from properties files, YAML, environment variables, system properties, command-line arguments, and external configuration sources. Higher-precedence sources can override lower-precedence values.

Profiles select configurations; they should not be used as secret storage.

---

# Part 2: Spring Core, DI, AOP, and Transactions

## Q11: What is the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`?

All are Spring component stereotypes.

| Annotation        | Intended responsibility                                 |
| ----------------- | ------------------------------------------------------- |
| `@Component`      | General Spring-managed component                        |
| `@Service`        | Business or application service                         |
| `@Repository`     | Persistence component                                   |
| `@Controller`     | Spring MVC controller                                   |
| `@RestController` | Controller whose methods normally write response bodies |

`@Repository` also participates in Spring’s persistence-exception translation mechanism when the relevant infrastructure is configured.

The specialized annotations communicate architectural intent even when the container can discover all of them through component scanning.

---

## Q12: What is the difference between `@Bean` and `@Component`?

### `@Component`

Placed on a class and normally discovered through component scanning.

```java
@Service
public class OrderService {
}
```

### `@Bean`

Placed on a factory method.

```java
@Configuration
public class ClientConfiguration {

    @Bean
    PaymentClient paymentClient() {
        return new PaymentClient();
    }
}
```

Use `@Bean` when:

- Configuring a third-party class
- Construction requires custom logic
- Several differently configured instances are needed
- The source class cannot be annotated

`@Bean` is a method-level declaration analogous to registering an object explicitly in the container. ([Home][5])

---

## Q13: How does dependency injection work internally?

Spring first stores bean definitions describing how objects should be created. During context refresh, it resolves dependencies for each injection point.

Conceptually:

```text
OrderService constructor requests PaymentGateway
        ↓
Find candidate beans assignable to PaymentGateway
        ↓
Apply qualifiers, primary status, names, and priorities
        ↓
Resolve one candidate
        ↓
Create dependency if necessary
        ↓
Pass it to the constructor
```

Spring recommends constructor injection for required dependencies. ([Home][6])

---

## Q14: Two beans have the same type. How does Spring choose one?

Suppose two beans implement `PaymentGateway`.

```java
@Component
public class CardGateway implements PaymentGateway {
}
```

```java
@Component
public class BankGateway implements PaymentGateway {
}
```

Spring requires additional selection information.

### `@Primary`

```java
@Primary
@Component
public class CardGateway implements PaymentGateway {
}
```

### `@Qualifier`

```java
public PaymentService(
        @Qualifier("bankGateway")
        PaymentGateway gateway
) {
    this.gateway = gateway;
}
```

A qualifier is more explicit at the injection point and normally takes precedence over the general primary preference.

---

## Q15: What is AOP?

Aspect-Oriented Programming separates cross-cutting concerns from business logic.

Common cross-cutting concerns include:

- Transactions
- Security
- Logging
- Metrics
- Caching
- Auditing
- Retry behavior

### Terms

| Term       | Meaning                                             |
| ---------- | --------------------------------------------------- |
| Aspect     | Module containing cross-cutting behavior            |
| Join point | A point in execution, commonly a method invocation  |
| Pointcut   | Expression selecting join points                    |
| Advice     | Code executed before, after, or around a join point |
| Weaving    | Connecting aspects to target execution              |
| Proxy      | Wrapper intercepting calls to the target bean       |

Spring AOP commonly uses proxies around managed beans.

---

## Q16: How does `@Transactional` work internally?

Spring usually creates a proxy for the transactional bean.

```text
Caller
  ↓
Transaction proxy
  ↓
TransactionInterceptor
  ↓
Start or join transaction
  ↓
Invoke target method
  ↓
Commit or roll back
```

Example:

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

The default propagation is `REQUIRED`, meaning the method joins an existing compatible transaction or starts one when none exists. ([Home][7])

---

## Q17: Where can `@Transactional` silently fail?

### Private method

```java
@Transactional
private void savePayment() {
}
```

A private method cannot normally be intercepted through Spring’s proxy-based AOP mechanism.

### Self-invocation

```java
public void process() {
    savePayment();
}

@Transactional
public void savePayment() {
}
```

`process()` calls `this.savePayment()`, bypassing the proxy.

### Object created with `new`

```java
TransferService service = new TransferService();
```

The instance is not Spring-managed and has no transactional proxy.

### Incorrect rollback assumptions

By default, runtime exceptions and errors commonly trigger rollback. A checked exception does not automatically cause rollback unless configured.

### Final or non-proxyable method concerns

Proxy type and method visibility can affect interception. The safest common design is a non-final public transactional service method called from another managed bean.

Spring’s proxy documentation explicitly explains that self-invocation bypasses proxy advice. ([Home][8])

---

## Q18: How does Spring handle circular dependencies?

Constructor cycle:

```text
OrderService → PaymentService → OrderService
```

Neither bean can be constructed first.

The preferred solution is redesign:

- Extract an orchestration service
- Move shared behavior into another component
- Publish an event
- Reverse one dependency
- Depend on a smaller interface

Switching to field injection, setter injection, or `@Lazy` may hide the structural problem without fixing ownership.

---

## Q19: What is the difference between `BeanFactory` and `ApplicationContext`?

`BeanFactory` provides fundamental bean registration, creation, and dependency-resolution functionality.

`ApplicationContext` builds on the container and provides additional application-level capabilities, including:

- Event publishing
- Message resolution
- Environment and profile access
- Resource loading
- Automatic registration of many framework post-processors
- Web-specific context types

Most applications use `ApplicationContext`.

---

# Part 3: Actuator and Observability

## Q20: What is Spring Boot Actuator?

Actuator provides production-oriented management and monitoring capabilities through HTTP endpoints or JMX.

Common endpoint IDs include:

```text
health
info
metrics
prometheus
conditions
beans
mappings
loggers
threaddump
heapdump
```

The usual HTTP path is:

```text
/actuator/{endpoint-id}
```

For example:

```text
/actuator/health
```

Endpoints must be enabled, exposed, and secured deliberately. ([Home][9])

---

## Q21: How do you create a custom health indicator?

```java
@Component
public class PaymentGatewayHealthIndicator
        implements HealthIndicator {

    private final PaymentGatewayClient client;

    public PaymentGatewayHealthIndicator(
            PaymentGatewayClient client
    ) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            client.checkConnection();

            return Health.up()
                    .withDetail(
                            "provider",
                            "payment-gateway"
                    )
                    .build();
        } catch (Exception exception) {
            return Health.down(exception)
                    .withDetail(
                            "provider",
                            "payment-gateway"
                    )
                    .build();
        }
    }
}
```

Spring detects the indicator and includes it in the health system.

Health checks should:

- Have strict timeouts
- Avoid expensive operations
- Avoid changing application state
- Avoid exposing sensitive information
- Distinguish liveness from readiness

---

## Q22: What is the difference between liveness and readiness?

### Liveness

Answers:

> Is the application process internally broken and likely to require a restart?

Liveness should not usually fail merely because a shared database or external API is unavailable. Otherwise, every application instance may restart simultaneously and amplify the outage.

### Readiness

Answers:

> Should this instance currently receive traffic?

Readiness may consider selected dependencies required for serving requests.

Spring Boot represents Kubernetes liveness and readiness as health groups. ([Home][10])

---

## Q23: How do you create custom metrics with Micrometer?

```java
@Component
public class PaymentMetrics {

    private final Counter successfulPayments;
    private final Timer paymentDuration;

    public PaymentMetrics(MeterRegistry registry) {
        this.successfulPayments =
                Counter.builder(
                        "payments.success.total"
                ).register(registry);

        this.paymentDuration =
                Timer.builder(
                        "payments.duration"
                ).register(registry);
    }

    public void recordSuccess(Duration duration) {
        successfulPayments.increment();
        paymentDuration.record(duration);
    }
}
```

Use low-cardinality tags such as:

```text
status=success
method=card
```

Avoid high-cardinality values such as:

```text
userId
orderId
requestId
complete URL
```

---

## Q24: How do Prometheus and Grafana integrate with Spring Boot?

1. Add the Prometheus Micrometer registry.
2. Expose the Prometheus Actuator endpoint.
3. Configure Prometheus to scrape each application instance.
4. Configure Grafana to query Prometheus.
5. Create dashboards and alerts.

Spring Boot exposes Prometheus-formatted metrics through:

```text
/actuator/prometheus
```

when the registry is available and the endpoint is exposed. ([Home][11])

---

## Q25: What is Spring Boot Admin?

Spring Boot Admin is a separate community project that provides a UI over Actuator information from multiple applications.

Actuator provides the application-side endpoints. Spring Boot Admin aggregates and visualizes data such as:

- Health
- Metrics
- Log levels
- Environment information
- Thread dumps
- Application status

It should be secured because it can expose sensitive operational functionality.

---

# Part 4: Caching, Async, Scheduling, and Performance

## Q26: How do you implement caching?

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
}
```

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

    @CachePut(
            cacheNames = "products",
            key = "#result.id"
    )
    public ProductResponse update(
            UpdateProductRequest request
    ) {
        return updateDatabase(request);
    }

    @CacheEvict(
            cacheNames = "products",
            key = "#productId"
    )
    public void delete(long productId) {
        deleteFromDatabase(productId);
    }
}
```

`@Cacheable` may also be bypassed by self-invocation because Spring’s default caching implementation is proxy-based. ([Home][12])

---

## Q27: How do you integrate Redis caching?

Typical configuration:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

  cache:
    type: redis
    redis:
      time-to-live: 10m
      cache-null-values: false
```

If Redis is available and configured, Spring Boot can auto-configure a `RedisCacheManager`. Cache defaults such as TTL are configured through `spring.cache.redis.*`. ([Home][13])

Every Redis cache should define:

- TTL
- Cache key structure
- Prefixing
- Serialization
- Invalidation
- Maximum memory policy
- Behavior when Redis is unavailable

---

## Q28: What does `@Async` do?

`@Async` submits the method invocation to a Spring `TaskExecutor`.

```java
@Configuration
@EnableAsync
public class AsyncConfiguration {
}
```

```java
@Async
public CompletableFuture<Report> generateReport() {
    Report report = buildReport();
    return CompletableFuture.completedFuture(report);
}
```

Important limitations:

- Self-invocation bypasses the proxy.
- Thread-local context may not propagate automatically.
- Transactions do not automatically move to the asynchronous thread.
- Executor queues and thread counts must be bounded.
- Returning `CompletableFuture` does not automatically make blocking code non-blocking.

Spring provides `TaskExecutor` abstractions for asynchronous execution. ([Home][14])

---

## Q29: What happens when an `@Async` method throws an exception?

### Future-returning method

The exception is represented through the returned `Future` or `CompletableFuture`.

```java
@Async
public CompletableFuture<Result> execute() {
    throw new IllegalStateException("Failed");
}
```

The caller observes the failure when consuming the future.

### `void` method

A `void` asynchronous method cannot return the exception to the caller. Configure an `AsyncUncaughtExceptionHandler`.

```java
@Configuration
@EnableAsync
public class AsyncConfiguration
        implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler
    getAsyncUncaughtExceptionHandler() {

        return (exception, method, parameters) ->
                log.error(
                        "Async failure in {}",
                        method.getName(),
                        exception
                );
    }
}
```

Spring documents this distinction explicitly. ([Home][14])

---

## Q30: What is `@Scheduled`?

`@Scheduled` runs a method according to a fixed delay, fixed rate, or cron expression.

```java
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
```

```java
@Scheduled(fixedDelay = 30_000)
public void pollAfterPreviousCompletion() {
}
```

```java
@Scheduled(fixedRate = 30_000)
public void triggerOnRate() {
}
```

```java
@Scheduled(
        cron = "0 0 2 * * *",
        zone = "UTC"
)
public void nightlyJob() {
}
```

- `fixedDelay` waits after the previous execution finishes.
- `fixedRate` uses the scheduled start rate.
- `cron` follows calendar-based scheduling.

For multiple application instances, a local scheduled method may execute on every instance. Use a distributed scheduler lock, leader election, or external scheduler when only one cluster-wide execution is allowed.

---

## Q31: What is HikariCP?

HikariCP is a JDBC connection-pool implementation commonly selected by Spring Boot when it is available.

Example:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3s
      idle-timeout: 10m
      max-lifetime: 30m
```

Pool size should be based on:

- Database capacity
- Query duration
- Request concurrency
- Number of service instances
- Workload type

A larger connection pool is not automatically faster. It can overload the database and increase contention.

---

## Q32: What is Spring WebFlux, and when should it be used?

Spring WebFlux is Spring’s reactive web framework built around Reactive Streams and Reactor types such as `Mono` and `Flux`.

Use it when:

- The request path is non-blocking end-to-end
- The service has high concurrent I/O
- Streaming is required
- Reactive libraries are already used
- Backpressure matters

Avoid choosing WebFlux solely because an application has high traffic. Blocking JDBC calls or blocking external libraries can remove much of its benefit unless isolated correctly.

`WebClient` is fully non-blocking and supports streaming. ([Home][15])

---

## Q33: How do you improve Spring Boot performance?

Start with measurement rather than annotations.

Investigate:

- Database query time and plans
- Connection-pool waiting
- External calls
- Thread-pool queues
- Lock contention
- Allocation rate
- GC pauses
- Serialization
- Cache hit ratio
- Container CPU throttling

Common improvements include:

- Correct indexes and query projections
- Pagination and bounded batch sizes
- Appropriate caching
- Bounded executors
- HTTP and database timeouts
- Avoiding N+1 queries
- Reducing unnecessary allocation
- Asynchronous processing for suitable work
- Load testing with production-like conditions

`@Async`, WebFlux, lazy initialization, and caching are not universal performance fixes.

---

## Q34: Spring Boot startup takes 45 seconds. How do you investigate?

1. Measure startup phases.
2. Enable the auto-configuration condition report.
3. Inspect slow bean initialization.
4. Check database connection attempts.
5. Check network calls during constructors or `@PostConstruct`.
6. Check schema migrations.
7. Check classpath and component scanning.
8. Inspect runners and ready-event listeners.
9. Compare active profiles and configuration.
10. Use JFR when CPU or class-loading cost is suspected.

Useful techniques include:

```bash
java -jar application.jar --debug
```

and application startup instrumentation.

Common causes include:

- Remote calls during startup
- Database timeouts
- Large component scans
- Flyway or Liquibase migrations
- Heavy cache loading
- Large numbers of beans
- DNS delays
- DevTools accidentally enabled
- Blocking startup runners

---

# Part 5: HTTP Clients and Microservices

## Q35: `RestTemplate` vs `RestClient` vs `WebClient`

| Client         | Model                     | Best use                                        |
| -------------- | ------------------------- | ----------------------------------------------- |
| `RestClient`   | Synchronous, fluent       | Normal blocking Spring MVC applications         |
| `WebClient`    | Reactive and non-blocking | Reactive flows, streaming, high-concurrency I/O |
| `RestTemplate` | Synchronous, older API    | Existing legacy applications                    |

For new synchronous code, prefer `RestClient`. For reactive or streaming requirements, use `WebClient`. In Spring Framework 7, `RestTemplate` is deprecated in favor of `RestClient`. ([Home][1])

Regardless of client, configure:

- Connect timeout
- Read/response timeout
- Connection pool
- Maximum response size
- Retry policy
- Observability
- Authentication
- TLS validation

---

## Q36: How should microservices communicate?

### REST or synchronous HTTP

Use when:

- The caller requires an immediate response
- The operation is query-like
- Failure must be known immediately
- The workflow is short

Trade-offs:

- Temporal coupling
- Timeout risk
- Cascading failure
- Retry complexity

### Kafka or asynchronous messaging

Use when:

- Eventual consistency is acceptable
- Producers and consumers should be decoupled
- Work can continue asynchronously
- Multiple consumers need the event
- Replay or durable event processing is useful

Trade-offs:

- More complex debugging
- Duplicate delivery
- Ordering constraints
- Event schema evolution
- Eventual consistency

Kafka acceptance does not prove that the final business action succeeded. Consumers must usually be idempotent. Spring Boot provides configuration for Spring Kafka through `spring.kafka.*`. ([Home][16])

---

## Q37: How do you handle transactions across microservices?

A local database transaction cannot atomically include independent service databases without a distributed transaction mechanism.

Common patterns include:

### Saga

A sequence of local transactions with compensating actions.

```text
Create order
→ reserve inventory
→ authorize payment
→ generate invoice
```

Failure:

```text
Payment fails
→ release inventory
→ mark order failed
```

### Transactional outbox

Write the business state and outgoing event into the same local transaction.

```text
Business row
+
Outbox row
=
One database commit
```

A publisher later sends the outbox event.

### Idempotent consumer

The consumer records a message or business-operation identifier before applying the side effect.

Avoid saying that “Kafka gives exactly once” without defining which business side effect is protected.

---

## Q38: How do you implement an idempotent REST API?

Require a unique key:

```http
POST /loan-applications
Idempotency-Key: loan-request-7291
```

Store:

- Idempotency key
- Request fingerprint
- Processing status
- Created resource ID
- Response
- Expiration information

Conceptual flow:

```text
Receive key
  ↓
Atomically reserve key
  ├── New → execute operation
  └── Existing
        ├── Same request, completed → return stored result
        ├── Same request, processing → return processing status
        └── Different request → reject key reuse
```

For critical financial operations, connect the idempotency record and business write through a durable atomic boundary where possible. A Redis-only entry can disappear or expire while the durable business record remains.

---

## Q39: What is an API gateway?

An API gateway is an entry point for client traffic.

Responsibilities can include:

- Routing
- TLS termination
- Authentication
- Rate limiting
- Request-size enforcement
- Header transformation
- Observability
- Canary routing

The backend must still enforce authorization and business rules. Gateway security alone is insufficient.

---

## Q40: How do you implement rate limiting?

Common algorithms include:

- Token bucket
- Leaky bucket
- Fixed window
- Sliding window

Possible implementations:

- API gateway with Redis
- Bucket4j
- Redis scripts
- Managed cloud gateway
- Service mesh or ingress controls

Choose the key carefully:

```text
user ID
tenant ID
API key
IP address
endpoint
operation cost
```

A payment endpoint may need a different limit from a product-search endpoint. Distributed rate limiting should use an atomic shared operation rather than separate read-and-increment calls.

---

## Q41: Monolith vs microservices

| Monolith                     | Microservices                              |
| ---------------------------- | ------------------------------------------ |
| One deployable application   | Multiple independently deployable services |
| Simpler local transactions   | Distributed consistency                    |
| Easier local debugging       | Better service isolation                   |
| Lower operational complexity | Higher infrastructure complexity           |
| In-process calls             | Network communication                      |
| One coordinated release      | Independent service releases               |

Microservices are appropriate when organizational and domain boundaries justify their operational cost. They are not automatically more scalable than a well-designed modular monolith.

---

## Q42: Can Spring events or AOP coordinate independent microservices?

No—not by themselves.

Spring application events and Spring AOP normally operate inside one application process and application context.

For independent services, use:

- Kafka
- RabbitMQ
- Transactional outbox
- Durable workflow engine
- Saga orchestrator
- REST or gRPC where synchronous coordination is required

For an order-management workflow:

```text
Order service commits order
+
writes OrderCreated outbox event
        ↓
Event published
        ↓
Inventory service reserves stock
Payment service authorizes payment
Invoice service creates invoice
```

AOP may be used locally for logging, transactions, metrics, or security, but it is not a distributed messaging mechanism.

---

# Part 6: Testing

## Q43: What is the difference between common Spring test annotations?

| Annotation        | Purpose                                      |
| ----------------- | -------------------------------------------- |
| `@SpringBootTest` | Loads a complete Boot application context    |
| `@WebMvcTest`     | Loads the Spring MVC web slice               |
| `@DataJpaTest`    | Loads JPA-related components                 |
| `@JsonTest`       | Tests JSON serialization and deserialization |

Test slices reduce startup time by loading only the relevant part of the application. ([Home][17])

---

## Q44: What is MockMvc?

MockMvc tests Spring MVC request handling without starting a real network server.

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void returnsOrder() throws Exception {
        mockMvc.perform(get("/orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }
}
```

It exercises:

- Request mapping
- JSON conversion
- Validation
- Controller advice
- Security filters when configured

---

## Q45: What is the difference between `@Mock`, `@MockitoBean`, and `@Spy`?

### `@Mock`

Creates a plain Mockito mock without requiring Spring.

```java
@Mock
private PaymentGateway gateway;
```

### `@MockitoBean`

Replaces or adds a bean in a Spring test application context.

```java
@MockitoBean
private PaymentGateway gateway;
```

### `@Spy`

Wraps a real object. Real methods execute unless stubbed.

```java
@Spy
private PriceCalculator calculator;
```

Current Spring testing documentation uses `@MockitoBean` and `@MockitoSpyBean` for context-level overrides. ([Home][18])

---

## Q46: What is `@DataJpaTest`?

`@DataJpaTest` is a JPA test slice intended for:

- Entities
- Repositories
- Mapping behavior
- Persistence queries

It commonly:

- Loads JPA infrastructure
- Configures transactional test behavior
- Rolls tests back by default
- Attempts to use an embedded test database when appropriate

Do not assume H2 accurately reproduces PostgreSQL, Oracle, or MySQL behavior. Use Testcontainers for database-specific queries, constraints, locking, JSON types, or dialect behavior.

---

## Q47: What is Testcontainers?

Testcontainers starts temporary Docker-based dependencies for integration tests.

```java
@Testcontainers
@SpringBootTest
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17"
            );
}
```

It is useful for testing against actual services such as PostgreSQL, Redis, Kafka, MongoDB, or RabbitMQ instead of replacing them with behaviorally different in-memory implementations. Spring Boot integrates Testcontainers with JUnit and supports service-connection configuration. ([Home][19])

---

## Q48: How do you test REST APIs with RestAssured?

Start the application on a random port:

```java
@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
class OrderApiTest {

    @LocalServerPort
    private int port;

    @Test
    void createsOrder() {
        given()
                .port(port)
                .contentType("application/json")
                .body("""
                    {
                      "productId": "P100",
                      "quantity": 2
                    }
                    """)
        .when()
                .post("/orders")
        .then()
                .statusCode(201);
    }
}
```

This is a full HTTP integration test because a real embedded server is started.

---

## Q49: What is `@TestConfiguration`?

`@TestConfiguration` defines beans intended only for tests.

```java
@TestConfiguration
public class StubPaymentConfiguration {

    @Bean
    PaymentGateway paymentGateway() {
        return new StubPaymentGateway();
    }
}
```

Import it explicitly:

```java
@Import(StubPaymentConfiguration.class)
```

Unlike normal application configuration, it is designed not to be discovered accidentally as part of regular application startup.

---

# Version-Specific Question

## Q50: What are the major Spring Boot 2 to Spring Boot 3 changes?

Major migration themes include:

- Java 17 minimum baseline
- Migration from `javax.*` to `jakarta.*`
- Spring Framework 6
- Updated Jakarta EE specifications
- AOT processing and improved GraalVM native-image integration
- Micrometer Tracing replacing Spring Cloud Sleuth-based approaches
- Dependency and configuration changes across the ecosystem

This is a migration question, not a complete list. Each project must inspect:

- Removed APIs
- Deprecated configuration properties
- Spring Security changes
- Hibernate changes
- Servlet and persistence imports
- Test annotation changes
- Third-party dependency compatibility

The uploaded source correctly identifies the Java 17, Jakarta namespace, native-image, and Micrometer Tracing themes.

---

# Native Images and Containers

## Q51: How do you containerize a Spring Boot application?

### Dockerfile approach

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/application.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

For production, consider:

- Non-root user
- Read-only filesystem where possible
- JVM container awareness
- Health checks
- Graceful shutdown
- Layer caching
- Minimal base image
- Explicit resource limits

### Buildpacks

```bash
mvn spring-boot:build-image
```

Buildpacks can create an OCI image without maintaining a custom Dockerfile.

---

## Q52: What is GraalVM Native Image support?

A native image compiles the application ahead of time into a platform-specific executable.

Potential benefits:

- Faster startup
- Lower memory usage in some workloads
- Useful for short-lived or scale-to-zero workloads

Trade-offs:

- Longer build process
- Platform-specific binary
- Closed-world analysis
- Reflection and proxy metadata requirements
- More complex debugging
- Not every library behaves identically
- Throughput may differ from a warmed-up JIT-compiled JVM

Avoid claiming that dynamic class loading is universally impossible in every form; the practical limitation is that runtime behavior must fit what the native-image build can analyze or what is described through metadata and hints.

---

# Final Interview Summary

> Spring Boot startup begins with `SpringApplication.run()`, which prepares the environment, creates the application context, evaluates conditional auto-configurations, creates beans, starts the server, executes runners, and publishes readiness events. Auto-configuration uses registered candidates and conditions such as `@ConditionalOnClass`, `@ConditionalOnProperty`, and `@ConditionalOnMissingBean`. Spring features such as transactions, caching, asynchronous methods, and method security are commonly proxy-based, so private methods and self-invocation are important failure cases. For production systems, I combine Actuator, Micrometer, health groups, bounded executors, properly configured connection pools, explicit caching policies, and realistic integration testing. Across microservices, I use durable messaging, idempotent consumers, outbox processing, and Saga-style workflows rather than assuming local Spring events or database transactions cross process boundaries.

[1]: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com "REST Clients :: Spring Framework"
[2]: https://spring.io/projects/spring-boot?utm_source=chatgpt.com "Spring Boot"
[3]: https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html?utm_source=chatgpt.com "Using the @SpringBootApplication Annotation"
[4]: https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com "Creating Your Own Auto-configuration"
[5]: https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com "Using the @Bean Annotation"
[6]: https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html?utm_source=chatgpt.com "Spring Beans and Dependency Injection :: Spring Boot"
[7]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com "Using @Transactional"
[8]: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html?utm_source=chatgpt.com "Proxying Mechanisms"
[9]: https://docs.spring.io/spring-boot/reference/actuator/index.html?utm_source=chatgpt.com "Production-ready Features"
[10]: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html?utm_source=chatgpt.com "Endpoints :: Spring Boot"
[11]: https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com "Metrics :: Spring Boot"
[12]: https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com "Declarative Annotation-based Caching"
[13]: https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com "Caching :: Spring Boot"
[14]: https://docs.spring.io/spring-framework/reference/integration/scheduling.html?utm_source=chatgpt.com "Task Execution and Scheduling :: Spring Framework"
[15]: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html?utm_source=chatgpt.com "WebClient :: Spring Framework"
[16]: https://docs.spring.io/spring-boot/reference/messaging/kafka.html?utm_source=chatgpt.com "Apache Kafka Support :: Spring Boot"
[17]: https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com "Testing Spring Boot Applications"
[18]: https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html?utm_source=chatgpt.com "@MockitoBean and @MockitoSpyBean :: Spring Framework"
[19]: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html?utm_source=chatgpt.com "Testcontainers :: Spring Boot"
