# Scenario-Based Questions — Spring Boot Internals and Production Design

The two circular-dependency questions have been merged because they cover the same scenario.

---

# Scenario 1: The application fails to start because of a circular dependency. How do you identify and fix it?

A circular dependency occurs when two or more beans depend on each other in a cycle.

```text
OrderService
    ↓
PaymentService
    ↓
NotificationService
    ↓
OrderService
```

## Example

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

```java
@Service
public class PaymentService {

    private final OrderService orderService;

    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

Spring cannot construct either bean first:

```text
Creating OrderService requires PaymentService.
Creating PaymentService requires OrderService.
```

## How to identify it

Start by reading the complete startup failure.

Typical evidence includes:

```text
BeanCurrentlyInCreationException
UnsatisfiedDependencyException
The dependencies of some of the beans form a cycle
```

The startup report usually shows the dependency path:

```text
orderService
┌─────┐
| paymentService
↑     ↓
└─────┘
```

Then inspect:

- Constructor parameters
- Injected fields
- Setter dependencies
- `@Bean` method parameters
- Event listeners
- Configuration classes
- Newly introduced dependencies

## Preferred solution: redesign the responsibilities

A dependency cycle often means that two services have mixed responsibilities.

For example, introduce an orchestrator:

```java
@Service
public class CheckoutCoordinator {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public CheckoutCoordinator(
            OrderService orderService,
            PaymentService paymentService
    ) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    public void checkout(CreateOrderCommand command) {
        Order order = orderService.create(command);
        paymentService.processPayment(order);
    }
}
```

Now the dependencies become:

```text
CheckoutCoordinator
├── OrderService
└── PaymentService
```

Neither service needs to depend on the other.

## Other redesign options

- Extract shared logic into a third service.
- Move orchestration into an application service.
- Depend on a smaller interface.
- Reverse the dependency direction.
- Publish an application event.
- Separate command handling from event handling.

## Using events to break direct coupling

```java
public record OrderCreatedEvent(
        long orderId,
        long totalAmount
) {
}
```

```java
@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public Order create(CreateOrderCommand command) {
        Order order = saveOrder(command);

        eventPublisher.publishEvent(
                new OrderCreatedEvent(
                        order.getId(),
                        order.getTotalAmount()
                )
        );

        return order;
    }
}
```

```java
@Component
public class PaymentEventHandler {

    private final PaymentService paymentService;

    public PaymentEventHandler(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @EventListener
    public void handle(OrderCreatedEvent event) {
        paymentService.process(
                event.orderId(),
                event.totalAmount()
        );
    }
}
```

Local Spring events are still in-process. They are not a replacement for Kafka or another durable messaging mechanism between independent services.

## Should `@Lazy` be used?

`@Lazy` delays dependency resolution:

```java
public OrderService(
        @Lazy PaymentService paymentService
) {
    this.paymentService = paymentService;
}
```

It may break the immediate construction cycle, but it usually hides the design problem.

Use it only when:

- The relationship is genuinely lazy.
- Refactoring is temporarily impossible.
- The dependency is an infrastructure concern.
- The limitation is documented.

## Should setter injection be used?

Setter injection can technically resolve some singleton cycles by allowing one bean to be created before the second dependency is assigned.

```java
@Autowired
public void setPaymentService(
        PaymentService paymentService
) {
    this.paymentService = paymentService;
}
```

This is normally not the best fix because it:

- Hides mandatory dependencies
- Allows partially initialized objects
- Makes the design harder to understand
- Preserves the underlying cycle

### Interview-ready answer

> I inspect the startup dependency graph to identify the exact cycle. My preferred solution is to redesign the responsibilities by introducing an orchestrator, extracting shared behavior, reversing the dependency, or publishing an event. I avoid using setter injection, `@Lazy`, or circular-reference configuration as permanent fixes because they usually hide a design problem.

---

# Scenario 2: An API response fails because of a Jackson serialization error. How do you debug it?

Serialization errors commonly appear as:

```text
HttpMessageNotWritableException
JsonMappingException
InvalidDefinitionException
StackOverflowError
LazyInitializationException
```

## Step 1: Inspect the complete exception path

Jackson exceptions often identify the field path that failed:

```text
OrderResponse["customer"]
→ Customer["orders"]
→ List[0]
→ Order["customer"]
```

This indicates a recursive relationship:

```text
Order → Customer → Orders → Customer → Orders...
```

## Step 2: Reproduce serialization directly

Create a focused test:

```java
@Test
void serializesOrderResponse() throws Exception {
    OrderResponse response =
            createSampleOrderResponse();

    String json =
            objectMapper.writeValueAsString(response);

    assertThat(json).contains("orderId");
}
```

This separates serialization failure from:

- HTTP routing
- Database access
- Authentication
- Controller logic
- Network behavior

## Step 3: Check common causes

### Bidirectional relationships

```java
@Entity
public class CustomerEntity {

    @OneToMany(mappedBy = "customer")
    private List<OrderEntity> orders;
}
```

```java
@Entity
public class OrderEntity {

    @ManyToOne
    private CustomerEntity customer;
}
```

Serializing both sides can create infinite recursion.

### Lazy-loading proxies

An entity may contain a lazy relationship that cannot be loaded when Jackson accesses it.

```text
Controller returns entity
→ transaction closes
→ Jackson accesses lazy field
→ LazyInitializationException
```

### Unsupported object structure

Possible causes include:

- No usable getter
- Non-serializable proxy
- Unsupported date/time type configuration
- Incorrect custom serializer
- Invalid polymorphic configuration
- Self-referencing object graph
- Unexpected null value
- Getter throwing an exception

## Preferred solution: return DTOs

Do not expose JPA entities directly.

```java
public record OrderResponse(
        long id,
        String status,
        CustomerSummary customer
) {
}
```

```java
public record CustomerSummary(
        long id,
        String name
) {
}
```

Mapper:

```java
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(
            OrderEntity order
    ) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                new CustomerSummary(
                        order.getCustomer().getId(),
                        order.getCustomer().getName()
                )
        );
    }
}
```

This provides an explicit, finite response graph.

## Alternative Jackson annotations

### `@JsonManagedReference` and `@JsonBackReference`

```java
public class Customer {

    @JsonManagedReference
    private List<Order> orders;
}
```

```java
public class Order {

    @JsonBackReference
    private Customer customer;
}
```

The managed side is serialized, while the back-reference is omitted.

### `@JsonIgnore`

```java
@JsonIgnore
private Customer customer;
```

Use this when the relationship should never appear in that JSON representation.

### `@JsonIdentityInfo`

```java
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class Customer {
}
```

After the first serialization, related objects can be represented by identity instead of recursively repeating the full object.

## Avoid using annotations as the only API design

Jackson annotations on entities can create coupling between:

```text
Database model
+
API response model
+
serialization behavior
```

DTOs are usually safer for public APIs.

### Interview-ready answer

> I inspect the Jackson exception path and reproduce the failure directly with the configured `ObjectMapper`. Common causes are recursive bidirectional relationships, lazy-loading proxies, unsupported types, or faulty custom serializers. I normally fix the problem by returning purpose-built DTOs. For controlled internal models, `@JsonManagedReference`, `@JsonBackReference`, `@JsonIgnore`, or identity-based serialization are alternatives.

---

# Scenario 3: A bug appears randomly. How do you investigate an intermittent bug?

A bug that appears random is usually dependent on a condition that has not yet been identified.

Possible hidden dimensions include:

- Concurrency
- Request order
- Timing
- Specific production data
- Instance or application version
- Cache state
- Time zone
- Network delay
- Retry behavior
- Database isolation
- Resource exhaustion
- External-service response

## Step 1: Define the symptom precisely

Instead of:

```text
The API occasionally fails.
```

Capture:

```text
Endpoint: POST /orders
Observed status: 500
Expected status: 201
Frequency: approximately 2 out of 10,000 requests
First observed: after release 3.8.1
Affected tenant: tenant-17
Affected instances: pod-4 and pod-7
```

## Step 2: Add correlation information

Every request should be traceable across services.

Capture:

- Trace ID
- Span ID
- Request ID
- Application version
- Instance or pod ID
- Tenant ID
- Safe user identifier
- Feature-flag version
- Cache hit or miss
- Database source
- Downstream dependency

Example structured log:

```json
{
  "level": "ERROR",
  "traceId": "792a12f",
  "service": "order-service",
  "version": "3.8.1",
  "instance": "order-service-7",
  "route": "/orders",
  "errorCode": "ORDER_CREATION_FAILED"
}
```

## Step 3: Compare successful and failed executions

Find what differs:

| Dimension            | Successful | Failed  |
| -------------------- | ---------- | ------- |
| Application instance | Pod 2      | Pod 7   |
| Cache                | Miss       | Hit     |
| Database node        | Primary    | Replica |
| Payload type         | Standard   | Large   |
| Concurrent updates   | No         | Yes     |
| Feature flag         | Disabled   | Enabled |
| Downstream latency   | 100 ms     | 4.8 s   |

## Step 4: Preserve evidence

Depending on the symptom, collect:

- Structured logs
- Distributed traces
- Metrics
- Database logs
- Thread dumps
- Heap dump
- Java Flight Recorder
- GC logs
- Request samples
- Queue depths
- Connection-pool metrics

For a concurrency or blocked-thread problem:

```bash
jcmd <pid> Thread.print -l
```

Capture several thread dumps a few seconds apart rather than relying on a single snapshot.

## Step 5: Reproduce the conditions

Use:

- The sanitized failing request
- Production-like data volume
- Concurrent requests
- The same JDK and dependency versions
- The same feature flags
- The same database engine
- Similar timeouts
- Controlled network delay
- Dependency failure simulation

## Step 6: Form and test one hypothesis at a time

Example:

```text
Hypothesis:
Two requests update the same order simultaneously.

Test:
Run 100 concurrent updates against one order.

Evidence:
Version conflict reproduced.

Fix:
Add optimistic locking and conflict handling.
```

Avoid changing logging, timeouts, thread pools, database configuration, and code simultaneously. That makes it difficult to identify which change fixed the issue.

### Interview-ready answer

> I first turn the random symptom into a measurable pattern using trace IDs, instance IDs, versions, timing, input characteristics, and resource metrics. I compare successful and failed requests, preserve runtime evidence, and reproduce the production conditions with concurrency or fault injection. Then I test one hypothesis at a time instead of making several speculative changes.

---

# Scenario 4: How would you implement graceful shutdown?

Graceful shutdown means:

```text
Stop accepting new work
→ allow active work to finish within a limit
→ stop background consumers and schedulers
→ release resources
→ terminate the process
```

Current Spring Boot documentation states that graceful web-server shutdown is enabled by default for supported embedded web servers. It occurs while the application context closes, and active requests receive a configured grace period. `SpringApplication` also registers a JVM shutdown hook so standard lifecycle callbacks run when the context is closed. ([Home][1])

## Configure the shutdown timeout

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

For applications that must support older Spring Boot versions, graceful shutdown may also need to be enabled explicitly:

```yaml
server:
  shutdown: graceful
```

Verify the behavior against the version used by the project.

## Release owned resources

```java
@Component
public class ReportExecutor {

    private final ExecutorService executor =
            Executors.newFixedThreadPool(4);

    @PreDestroy
    public void shutdown() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(
                    20,
                    TimeUnit.SECONDS
            )) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

## Stop message consumption carefully

During shutdown:

1. Stop fetching new messages.
2. Allow currently executing handlers to finish.
3. Commit or acknowledge only successfully processed messages.
4. Leave unfinished work available for redelivery.
5. Close the consumer connection.

Do not acknowledge a message before the business operation is durably completed.

## Manage lifecycle phases with `SmartLifecycle`

For components requiring ordered startup and shutdown:

```java
@Component
public class MessageConsumerLifecycle
        implements SmartLifecycle {

    private volatile boolean running;

    @Override
    public void start() {
        startConsumer();
        running = true;
    }

    @Override
    public void stop() {
        stopReceivingNewMessages();
        waitForCurrentMessages();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 100;
    }
}
```

Lifecycle phases can control the relative order in which managed components stop.

## Container deployment considerations

The termination grace period provided by the deployment platform should be longer than the application’s internal shutdown timeout.

```text
Application shutdown timeout: 30 seconds
Platform termination limit: greater than 30 seconds
```

The instance should also be removed from traffic before it is forcibly terminated.

### Interview-ready answer

> I stop accepting new traffic, allow in-flight requests to complete within a bounded grace period, stop consumers and schedulers from accepting new work, and release owned executors and connections through lifecycle callbacks. I ensure the deployment platform’s termination period is longer than the application shutdown timeout and verify that unfinished messages remain retryable.

---

# Scenario 5: How would you design a scalable Spring Boot microservice?

A scalable design starts by identifying:

- Expected requests per second
- Peak traffic
- Read-to-write ratio
- Latency objective
- Payload size
- Consistency requirements
- Dependency capacity
- Failure behavior

## High-level architecture

```text
Clients
   ↓
Load balancer / API gateway
   ↓
Stateless Spring Boot instances
   ├── Local bounded cache
   ├── Distributed cache
   ├── Business logic
   └── Bounded executors
   ↓
Database / read models
   ↓
Message broker
   ↓
Background workers
```

## 1. Keep instances stateless

Avoid storing request-specific state in singleton fields.

Unsafe:

```java
@Service
public class OrderService {

    private Order currentOrder;
}
```

Spring singleton beans are shared across request threads.

Use:

- Method-local state
- Database state
- Distributed cache
- Message broker
- Object storage

## 2. Scale horizontally

Run multiple application instances behind a load balancer.

```text
Request
   ↓
Load balancer
   ├── Instance 1
   ├── Instance 2
   └── Instance 3
```

Do not rely on one instance’s local memory for correctness.

## 3. Protect the database

Use:

- Correct indexes
- Query projections
- Pagination
- Bounded connection pools
- Batch writes
- Optimistic locking
- Read replicas where stale reads are acceptable
- Database constraints
- Partitioning only when justified

Increasing application threads does not increase database capacity.

## 4. Add caching where staleness is acceptable

Possible layers:

- HTTP cache
- CDN
- Local Caffeine cache
- Distributed Redis cache

Every cache needs:

- TTL
- Size limit
- Eviction policy
- Invalidation strategy
- Failure behavior
- Cache-key design
- Stale-data policy

Do not use cached data for correctness-critical decisions unless its consistency model supports the business requirement.

## 5. Use asynchronous messaging

Kafka or another broker can decouple long-running work.

```text
Create order
→ save order and outbox record
→ publish event
→ inventory and notification consumers process it
```

Consumers should be idempotent because messages may be delivered again.

## 6. Apply backpressure

Use bounded:

- Executor queues
- Connection pools
- Request sizes
- Batch sizes
- Message queues
- Concurrent operations

When capacity is exhausted, fail predictably rather than allowing unlimited queue growth.

## 7. Configure resilience

For remote dependencies use:

- Connect timeout
- Response timeout
- Retry with backoff
- Circuit breaker
- Bulkhead
- Rate limiting
- Fallback only when semantically valid

## 8. Add observability

Measure:

- Request throughput
- P95 and P99 latency
- Error rate
- Thread-pool utilization
- Queue depth
- Connection-pool waiting
- Cache hit ratio
- Database latency
- Downstream latency
- JVM memory and GC
- Consumer lag

### Interview-ready answer

> I design the service as stateless instances behind a load balancer, protect the database with bounded pools and optimized queries, use caches only with an explicit consistency policy, and move suitable long-running work to durable messaging. I add backpressure, timeouts, idempotency, observability, and load testing so the system fails predictably instead of accumulating unlimited work.

---

# Scenario 6: How would you handle bean lifecycle issues?

A simplified Spring bean lifecycle is:

```text
Bean definition registered
→ object instantiated
→ dependencies injected
→ aware callbacks
→ BeanPostProcessor before initialization
→ @PostConstruct
→ initialization callbacks
→ BeanPostProcessor after initialization
→ bean ready
→ @PreDestroy during shutdown
```

## Common lifecycle issues

### Accessing dependencies in the constructor

Constructor-injected dependencies are available, but field- or setter-injected dependencies are not yet populated during construction.

Unsafe:

```java
@Component
public class ReportLoader {

    @Autowired
    private ReportRepository repository;

    public ReportLoader() {
        repository.loadReports();
    }
}
```

`repository` is still `null` when the constructor executes.

Prefer constructor injection:

```java
@Component
public class ReportLoader {

    private final ReportRepository repository;

    public ReportLoader(
            ReportRepository repository
    ) {
        this.repository = repository;
    }
}
```

## Heavy work in `@PostConstruct`

Avoid:

```java
@PostConstruct
void initialize() {
    loadTenMillionRows();
    callThreeRemoteServices();
}
```

This can:

- Delay startup
- Block readiness
- Cause startup failure
- Make retry difficult
- Hold large object graphs
- Depend on unavailable remote services

Use an explicit startup runner, asynchronous job, or application-ready workflow when the work is substantial.

## Proxy-dependent annotations during initialization

Calling an internally annotated method from `@PostConstruct` may bypass proxies.

```java
@PostConstruct
void initialize() {
    loadDataTransactionally();
}

@Transactional
public void loadDataTransactionally() {
}
```

The internal call does not necessarily pass through the transactional proxy.

Move the transactional operation to another bean.

## Resource ownership

Only close resources owned by the bean.

```java
@Component
public class FileProcessor {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @PreDestroy
    void stop() {
        executor.shutdown();
    }
}
```

Do not close a shared `DataSource` or shared executor injected from another managed component unless lifecycle ownership is explicitly assigned.

## Diagnosing lifecycle issues

Inspect:

- Constructor logs
- Bean creation stack trace
- `@PostConstruct` failures
- Circular dependency reports
- Active profiles
- Conditional bean registration
- Bean post-processors
- Proxy type
- Application startup steps

Actuator can expose startup-step information when the application uses an appropriate startup recorder, and Spring Boot exposes application-started and application-ready timing metrics. ([Home][2])

### Interview-ready answer

> I identify which lifecycle phase is failing: construction, dependency injection, initialization, proxy creation, or destruction. I use constructor injection, avoid heavy remote or database work in `@PostConstruct`, move proxy-dependent calls to another bean, and manage only resources owned by the component. I use startup metrics and bean-creation logs to locate slow or failing initialization.

---

# Scenario 7: How do you implement a custom Spring MVC interceptor?

A `HandlerInterceptor` runs around Spring MVC controller handling.

Its main callbacks are:

```text
preHandle()
→ controller executes
→ postHandle()
→ response completion
→ afterCompletion()
```

Spring MVC supports interceptors for common request-processing behavior, but Spring’s documentation advises against using them as the main security layer. Authentication and authorization should normally be implemented with Spring Security or an equivalent filter-chain mechanism. ([Home][3])

## Example interceptor

```java
@Component
public class RequestTimingInterceptor
        implements HandlerInterceptor {

    private static final String START_TIME =
            "requestStartTime";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        request.setAttribute(
                START_TIME,
                System.nanoTime()
        );

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        Long started =
                (Long) request.getAttribute(
                        START_TIME
                );

        if (started == null) {
            return;
        }

        long durationNanos =
                System.nanoTime() - started;

        log.info(
                "method={} path={} status={} durationMs={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationNanos / 1_000_000
        );
    }
}
```

## Register it

```java
@Configuration
public class WebConfiguration
        implements WebMvcConfigurer {

    private final RequestTimingInterceptor interceptor;

    public WebConfiguration(
            RequestTimingInterceptor interceptor
    ) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/public/**"
                );
    }
}
```

Spring Boot allows MVC customization through `WebMvcConfigurer` without requiring `@EnableWebMvc`; adding `@EnableWebMvc` takes more complete control and can disable parts of Boot’s MVC auto-configuration. ([Home][4])

## Appropriate interceptor use cases

- Request timing
- Correlation-ID setup
- Locale selection
- Request audit metadata
- Controller-specific logging
- Shared response headers

## Interceptor vs filter

### Servlet filter

Runs earlier in the Servlet chain and can handle:

- Raw requests and responses
- Security
- CORS
- Compression
- Request wrapping
- All servlet resources

### Handler interceptor

Runs after Spring MVC has selected a handler and can access:

- Controller method metadata
- Handler information
- MVC request lifecycle

## Avoid

- Complex business logic
- Database transactions
- Authorization rules that belong in Spring Security
- Reading a large request body repeatedly
- Storing request data in shared fields

### Interview-ready answer

> I implement `HandlerInterceptor`, use `preHandle` for pre-processing and `afterCompletion` for final logging or cleanup, and register it through `WebMvcConfigurer`. I use interceptors for MVC-aware cross-cutting behavior, but I use Spring Security or Servlet filters for authentication, authorization, and early request security.

---

# Scenario 8: How would you design a multi-module Spring Boot application?

A multi-module project should reflect dependency and domain boundaries rather than simply splitting files into many JARs.

## Example structure

```text
order-platform/
├── pom.xml
├── order-domain/
│   └── pom.xml
├── order-application/
│   └── pom.xml
├── order-infrastructure/
│   └── pom.xml
├── order-web/
│   └── pom.xml
└── order-boot/
    └── pom.xml
```

## Suggested responsibilities

### `order-domain`

Contains:

- Domain entities
- Value objects
- Domain services
- Domain events
- Repository interfaces
- Business rules

It should have minimal framework dependencies.

### `order-application`

Contains:

- Use cases
- Application services
- Commands
- Queries
- Transaction orchestration
- Interfaces for external systems

### `order-infrastructure`

Contains:

- JPA implementations
- External HTTP clients
- Kafka adapters
- Redis implementations
- File storage
- Framework-specific configuration

### `order-web`

Contains:

- Controllers
- Request DTOs
- Response DTOs
- Exception handlers
- Web mapping

### `order-boot`

Contains:

- Main Spring Boot class
- Runtime assembly
- Configuration
- Executable packaging

## Dependency direction

```text
order-domain
      ↑
order-application
      ↑
order-infrastructure
      ↑
order-web
      ↑
order-boot
```

A more precise adapter-based design may allow infrastructure and web modules to depend on application interfaces without depending on each other.

## Parent Maven POM

```xml
<packaging>pom</packaging>

<modules>
    <module>order-domain</module>
    <module>order-application</module>
    <module>order-infrastructure</module>
    <module>order-web</module>
    <module>order-boot</module>
</modules>
```

Maven’s reactor collects modules, sorts them according to dependencies, and builds them in the required order. ([Apache Maven][5])

## Package structure

Place the main application class in a root package above the application components:

```text
com.example.orders
├── OrderApplication
├── domain
├── application
├── infrastructure
└── web
```

Spring Boot recommends a root package because the main application package becomes a default search location for component scanning and entity discovery. ([Home][6])

## Rules

- Avoid cyclic module dependencies.
- Keep one executable Boot module unless multiple deployables are required.
- Keep domain logic out of controllers and repositories.
- Avoid exposing implementation classes across modules.
- Use interfaces at boundaries.
- Centralize dependency versions in dependency management.
- Test modules independently.
- Do not confuse Maven modules with microservices.

A multi-module application may still be one deployable monolith.

### Interview-ready answer

> I divide the project by architectural or domain responsibility, enforce one-way dependencies, keep the domain independent of infrastructure, and use one Boot module to assemble the runtime. Maven’s parent POM aggregates the modules and builds them in dependency order. I avoid cyclic module dependencies and do not treat every module as a separate microservice.

---

# Scenario 9: Spring Boot startup takes too long. How do you diagnose and optimize it?

Do not enable lazy initialization immediately. First measure where startup time is spent.

Spring Boot exposes `application.started.time` and `application.ready.time`, and its startup endpoint can expose recorded startup steps when startup tracking is configured. ([Home][2])

## Step 1: Establish a baseline

Measure:

```text
JVM start
→ context started
→ runners completed
→ application ready
```

Compare:

- Local startup
- Container startup
- Cold startup
- Warm startup
- Development profile
- Production profile

## Step 2: Enable condition diagnostics

```bash
java -jar application.jar --debug
```

Inspect:

- Unexpected auto-configurations
- Missing conditions
- Duplicate infrastructure
- Unnecessary starters
- Excessive component scanning

## Step 3: Look for common causes

### Database initialization

- Slow connection attempts
- DNS delays
- Flyway or Liquibase migrations
- Schema validation
- Connection retries

### Heavy bean initialization

- Remote calls in constructors
- Remote calls in `@PostConstruct`
- Large cache preloading
- File scanning
- Reflection-heavy libraries
- Generating large metadata structures

### Classpath problems

- Too many unused dependencies
- Duplicate libraries
- Very large component-scan area
- Unnecessary JPA entities
- Development tools included in production

### Startup runners

Inspect:

- `ApplicationRunner`
- `CommandLineRunner`
- `ApplicationReadyEvent` listeners
- Scheduler initialization

## Step 4: Move heavy work out of the critical path

Instead of loading all reference data before startup:

```text
Start application
→ become ready
→ load optional data asynchronously
```

Only do this when the application can safely serve requests without that data.

## Step 5: Consider lazy initialization carefully

Lazy initialization can reduce initial startup time by delaying bean creation until first use, but it can also:

- Move failures from startup to the first request
- Increase first-request latency
- Hide configuration errors
- Delay detection of missing beans

Spring Boot documentation explicitly notes this trade-off. ([Home][7])

## Step 6: Verify the improvement

Compare:

- Startup time
- Ready time
- First-request latency
- Memory
- CPU
- Error detection
- Container scaling behavior

### Interview-ready answer

> I measure the difference between application-started and application-ready time, inspect recorded startup steps, and use the condition report to find unnecessary configuration. Common causes are database connection retries, migrations, heavy constructors or `@PostConstruct` methods, broad scanning, remote calls, and startup runners. I remove or defer non-essential work and use lazy initialization only after considering first-request latency and delayed failures.

---

# Scenario 10: How do you handle timeout issues in REST calls?

A timeout is not one setting. Different stages can time out independently.

```text
DNS lookup
→ connection acquisition
→ TCP connection
→ TLS handshake
→ request write
→ server processing
→ response headers
→ response body read
→ overall operation
```

## Important timeout types

### Connection-pool acquisition timeout

How long the caller waits for an available HTTP connection.

### Connect timeout

How long establishing the network connection may take.

### Response timeout

How long to wait for the remote server to begin or complete its response, depending on the client configuration.

### Read timeout

How long a read operation can remain without receiving data.

### Overall business timeout

Maximum duration allowed for the complete operation, including retries.

## Choose the correct HTTP client

Current Spring documentation describes:

- `RestClient` as the modern synchronous client
- `WebClient` for reactive, asynchronous, and streaming scenarios
- `RestTemplate` as an older synchronous client whose new feature development is no longer the focus ([Home][8])

## WebClient timeout example

```java
HttpClient httpClient =
        HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        2_000
                )
                .responseTimeout(
                        Duration.ofSeconds(5)
                );

WebClient webClient =
        WebClient.builder()
                .clientConnector(
                        new ReactorClientHttpConnector(
                                httpClient
                        )
                )
                .build();
```

Spring’s WebClient documentation demonstrates configuring a response timeout on the underlying Reactor Netty client. ([Home][9])

## Per-operation timeout

```java
return webClient.get()
        .uri("/inventory/{id}", productId)
        .retrieve()
        .bodyToMono(InventoryResponse.class)
        .timeout(Duration.ofSeconds(3));
```

The reactive timeout limits the reactive sequence, but the underlying network client should still have its own connection and response timeout configuration.

## Retry only when safe

Do not automatically retry every failure.

Retry may be reasonable for:

- Temporary connection failure
- Selected `502`, `503`, or `504` responses
- Read-only requests
- Idempotent writes
- Operations protected by idempotency keys

Do not blindly retry:

```text
POST /payments
```

A timed-out response does not prove that payment processing failed. The server may have completed the operation while the response was lost.

## Use a timeout budget

Suppose the external API contract allows five seconds:

```text
Overall API budget: 5 seconds

Application processing: 500 ms
Database: 700 ms
Downstream call: 2 seconds
Retry/backoff reserve: 1 second
Response serialization/network reserve: 800 ms
```

Every nested timeout should fit within the caller’s remaining budget.

## Add resilience patterns

Use:

- Circuit breaker
- Bulkhead
- Bounded connection pool
- Retry with exponential backoff and jitter
- Rate limiting
- Fallback only when valid
- Deadline propagation
- Idempotency

## Diagnose timeout incidents

Inspect:

- Trace span duration
- DNS timing
- Connection-pool pending count
- Active HTTP connections
- Downstream latency
- Timeout type
- Retry count
- Thread dumps
- Server queue depth
- CPU throttling
- Network errors

Low CPU with high latency often indicates threads waiting for:

- Connections
- Sockets
- Locks
- Remote services
- Executor capacity

### Interview-ready answer

> I identify which timeout is occurring—connection acquisition, connect, response, read, or overall operation—and configure each explicitly. I trace the downstream call, inspect HTTP-pool metrics, and make sure nested timeouts fit inside the API’s total latency budget. I retry only transient and safe operations, protect writes with idempotency, and add circuit breakers and bulkheads to prevent one slow dependency from exhausting the service.

---

# Quick Interview Summary

```text
Circular dependency:
Identify the bean cycle and redesign ownership.
Use @Lazy or setter injection only as a temporary workaround.

Serialization:
Inspect the Jackson property path.
Prefer DTOs over serializing JPA entities.

Intermittent bug:
Correlate logs, traces, versions, inputs, instances, and resource metrics.
Reproduce with production-like concurrency and failures.

Graceful shutdown:
Stop new work, drain active work, close resources, and respect a deadline.

Scalable microservice:
Stateless instances, bounded resources, caching, durable messaging,
database protection, resilience, and observability.

Bean lifecycle:
Understand construction, injection, initialization, proxying, and destruction.

Interceptor:
Implement HandlerInterceptor and register through WebMvcConfigurer.
Use Spring Security for authentication and authorization.

Multi-module:
Enforce one-way module dependencies and separate domain,
application, infrastructure, web, and runtime assembly.

Startup optimization:
Measure first, inspect startup steps, remove heavy initialization,
and use lazy initialization carefully.

REST timeouts:
Configure pool, connection, response, and overall deadlines separately.
Retry only safe or idempotent operations.
```

[1]: https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html?utm_source=chatgpt.com "Graceful Shutdown :: Spring Boot"
[2]: https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com "Metrics :: Spring Boot"
[3]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html?utm_source=chatgpt.com "Interceptors :: Spring Framework"
[4]: https://docs.spring.io/spring-boot/reference/web/servlet.html?utm_source=chatgpt.com "Servlet Web Applications :: Spring Boot"
[5]: https://maven.apache.org/guides/mini/guide-multiple-modules.html?utm_source=chatgpt.com "Guide to Working with Multiple Modules - Apache Maven"
[6]: https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html?utm_source=chatgpt.com "Structuring Your Code :: Spring Boot"
[7]: https://docs.spring.io/spring-boot/docs/3.2.5/reference/htmlsingle/?utm_source=chatgpt.com "Spring Boot Reference Documentation"
[8]: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com "REST Clients :: Spring Framework"
[9]: https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html?utm_source=chatgpt.com "Configuration :: Spring Framework"
