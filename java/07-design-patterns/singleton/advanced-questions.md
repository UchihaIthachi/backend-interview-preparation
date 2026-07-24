# Java Design Patterns — Advanced Questions

## Q31: What problem does the Factory Pattern solve?

The Factory Pattern centralizes object creation and hides concrete implementation details from the caller.

Without a factory:

```java
NotificationSender sender;

if (channel == Channel.EMAIL) {
    sender = new EmailNotificationSender();
} else if (channel == Channel.SMS) {
    sender = new SmsNotificationSender();
} else {
    sender = new PushNotificationSender();
}
```

The caller is tightly coupled to every implementation.

With a factory:

```java
public interface NotificationSender {

    void send(Notification notification);
}
```

```java
public class NotificationSenderFactory {

    public NotificationSender create(Channel channel) {
        return switch (channel) {
            case EMAIL -> new EmailNotificationSender();
            case SMS -> new SmsNotificationSender();
            case PUSH -> new PushNotificationSender();
        };
    }
}
```

Usage:

```java
NotificationSender sender = factory.create(channel);
sender.send(notification);
```

### Problems it solves

- Hides construction logic
- Reduces coupling to concrete classes
- Centralizes implementation selection
- Makes creation rules easier to change
- Supports polymorphism
- Simplifies testing when combined with interfaces

### Interview answer

> The Factory Pattern separates object creation from object usage. The caller depends on an abstraction, while the factory decides which concrete implementation to create.

---

## Q32: Factory Pattern vs Abstract Factory Pattern

“Factory Pattern” is often used broadly, but interviews commonly compare **Factory Method** with **Abstract Factory**.

| Factory Method                          | Abstract Factory                                  |
| --------------------------------------- | ------------------------------------------------- |
| Creates one type of product             | Creates a family of related products              |
| Usually exposes one creation method     | Usually exposes multiple related creation methods |
| Focuses on selecting one implementation | Ensures related objects are compatible            |
| Simpler                                 | More structural complexity                        |

### Factory Method example

```java
public interface PaymentGatewayFactory {

    PaymentGateway createGateway();
}
```

```java
public class StripeGatewayFactory
        implements PaymentGatewayFactory {

    @Override
    public PaymentGateway createGateway() {
        return new StripePaymentGateway();
    }
}
```

### Abstract Factory example

Suppose an application supports AWS and Azure.

```java
public interface CloudResourceFactory {

    StorageClient createStorageClient();

    QueueClient createQueueClient();

    DatabaseClient createDatabaseClient();
}
```

```java
public class AwsResourceFactory
        implements CloudResourceFactory {

    @Override
    public StorageClient createStorageClient() {
        return new S3StorageClient();
    }

    @Override
    public QueueClient createQueueClient() {
        return new SqsQueueClient();
    }

    @Override
    public DatabaseClient createDatabaseClient() {
        return new DynamoDatabaseClient();
    }
}
```

The AWS factory creates a compatible family of AWS-specific resources.

### Interview answer

> Factory Method creates one product through an abstraction. Abstract Factory creates a family of related products, such as storage, messaging, and database clients for the same cloud provider.

---

## Q33: When should you use the Builder Pattern?

Use Builder when an object:

- Has many constructor parameters
- Has several optional properties
- Must be immutable
- Requires validation before creation
- Is constructed in several steps
- Would otherwise need multiple telescoping constructors

Problematic constructor:

```java
Order order = new Order(
        customerId,
        shippingAddress,
        billingAddress,
        items,
        currency,
        discount,
        priority,
        note
);
```

Builder:

```java
Order order = Order.builder()
        .customerId(customerId)
        .shippingAddress(shippingAddress)
        .currency(Currency.USD)
        .priority(OrderPriority.NORMAL)
        .addLineItem(item)
        .build();
```

### Avoid Builder when

- The object has only two or three obvious fields.
- Construction is already simple.
- The builder allows invalid combinations.
- A Java record or static factory method is sufficient.

---

## Q34: Why is Builder preferred for immutable objects?

An immutable object should be completely initialized during construction and should not expose setters.

Builder provides a mutable construction object while keeping the final object immutable.

```java
public final class Order {

    private final Long customerId;
    private final List<LineItem> lineItems;
    private final Currency currency;

    private Order(Builder builder) {
        this.customerId = builder.customerId;
        this.lineItems = List.copyOf(builder.lineItems);
        this.currency = builder.currency;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Long customerId;
        private final List<LineItem> lineItems =
                new ArrayList<>();
        private Currency currency;

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder addLineItem(LineItem lineItem) {
            this.lineItems.add(lineItem);
            return this;
        }

        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Order build() {
            Objects.requireNonNull(
                    customerId,
                    "customerId is required"
            );

            Objects.requireNonNull(
                    currency,
                    "currency is required"
            );

            if (lineItems.isEmpty()) {
                throw new IllegalStateException(
                        "An order must contain at least one item"
                );
            }

            return new Order(this);
        }
    }
}
```

### Benefits

- Final fields can be used.
- No partially initialized final object is exposed.
- Validation occurs before creation.
- Defensive copies protect mutable collections.
- Creation code remains readable.

### Interview answer

> Builder is useful for immutable objects because the builder collects mutable construction state, validates it, and then creates one fully initialized object with final fields and no setters.

---

## Q35: How do you implement a thread-safe Singleton?

### Option 1: Enum Singleton

```java
public enum ApplicationRegistry {

    INSTANCE;

    public void register(String key, Object value) {
        // Registration logic
    }
}
```

Advantages:

- Thread-safe initialization
- Serialization-safe
- Resistant to normal reflection-based construction
- Simple

---

### Option 2: Initialization-on-demand holder

```java
public final class ApplicationRegistry {

    private ApplicationRegistry() {
    }

    private static class Holder {
        private static final ApplicationRegistry INSTANCE =
                new ApplicationRegistry();
    }

    public static ApplicationRegistry getInstance() {
        return Holder.INSTANCE;
    }
}
```

This is:

- Lazy
- Thread-safe
- Free from explicit synchronization
- Based on JVM class-initialization guarantees

---

### Option 3: Eager initialization

```java
public final class ApplicationRegistry {

    private static final ApplicationRegistry INSTANCE =
            new ApplicationRegistry();

    private ApplicationRegistry() {
    }

    public static ApplicationRegistry getInstance() {
        return INSTANCE;
    }
}
```

Simple and thread-safe, but not lazy.

---

### Option 4: Double-checked locking

```java
public final class ApplicationRegistry {

    private static volatile ApplicationRegistry instance;

    private ApplicationRegistry() {
    }

    public static ApplicationRegistry getInstance() {
        if (instance == null) {
            synchronized (ApplicationRegistry.class) {
                if (instance == null) {
                    instance = new ApplicationRegistry();
                }
            }
        }

        return instance;
    }
}
```

The `volatile` field is necessary for safe publication and ordering.

### Recommended answer

> I normally prefer an enum Singleton or the initialization-on-demand holder idiom. Both are thread-safe and simpler than double-checked locking.

---

## Q36: What are the drawbacks of Singleton?

Singleton can introduce:

### Hidden global state

Any part of the application can access and modify the same object.

### Tight coupling

Classes may call the Singleton directly instead of declaring dependencies.

```java
ConfigurationRegistry.getInstance()
        .getConfiguration();
```

This hides the dependency.

### Difficult testing

Tests may share state and influence one another.

### Concurrency problems

A single instance with mutable fields must still be made thread-safe.

```java
public class CounterSingleton {

    private int count;

    public int increment() {
        return ++count;
    }
}
```

One instance does not mean thread-safe.

### Lifecycle problems

The Singleton controls its own construction and may be difficult to initialize, replace, or destroy properly.

### Class-loader limitation

There can be one instance per class loader rather than one instance for the entire JVM.

### Not distributed

Three service replicas create three Singleton instances:

```text
Service instance 1 → Singleton A
Service instance 2 → Singleton B
Service instance 3 → Singleton C
```

### Spring-specific concern

Spring beans are already singleton-scoped by default. A manual static Singleton usually interferes with dependency injection and testing.

---

## Q37: Explain the Strategy Pattern with a real-world example

Strategy encapsulates interchangeable algorithms behind a common interface.

### Example: discount calculation

```java
public interface DiscountStrategy {

    BigDecimal calculate(
            BigDecimal originalPrice
    );
}
```

```java
@Component("regular")
public class RegularCustomerDiscount
        implements DiscountStrategy {

    @Override
    public BigDecimal calculate(
            BigDecimal originalPrice
    ) {
        return originalPrice;
    }
}
```

```java
@Component("premium")
public class PremiumCustomerDiscount
        implements DiscountStrategy {

    @Override
    public BigDecimal calculate(
            BigDecimal originalPrice
    ) {
        return originalPrice.multiply(
                new BigDecimal("0.90")
        );
    }
}
```

```java
@Component("employee")
public class EmployeeDiscount
        implements DiscountStrategy {

    @Override
    public BigDecimal calculate(
            BigDecimal originalPrice
    ) {
        return originalPrice.multiply(
                new BigDecimal("0.80")
        );
    }
}
```

Selection:

```java
@Service
public class PricingService {

    private final Map<String, DiscountStrategy> strategies;

    public PricingService(
            Map<String, DiscountStrategy> strategies
    ) {
        this.strategies = strategies;
    }

    public BigDecimal calculatePrice(
            String customerType,
            BigDecimal originalPrice
    ) {
        DiscountStrategy strategy =
                strategies.get(customerType);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported customer type"
            );
        }

        return strategy.calculate(originalPrice);
    }
}
```

### Benefits

- Removes large conditional blocks
- Supports the Open/Closed Principle
- Allows runtime behavior selection
- Makes each algorithm independently testable

### Interview answer

> Strategy is useful when several algorithms solve the same problem differently. For example, regular, premium, and employee discount calculations can implement one interface and be selected at runtime.

---

## Q38: When would you use the Observer Pattern?

Use Observer when one event should notify multiple independent listeners without the publisher depending directly on each one.

Examples:

- Order created
- Payment completed
- User registered
- Inventory changed
- Configuration updated
- Domain event emitted

```java
public record OrderCreatedEvent(
        Long orderId,
        Long customerId
) {
}
```

Publisher:

```java
@Service
public class OrderService {

    private final ApplicationEventPublisher publisher;

    public OrderService(
            ApplicationEventPublisher publisher
    ) {
        this.publisher = publisher;
    }

    public Order createOrder(
            CreateOrderCommand command
    ) {
        Order order = saveOrder(command);

        publisher.publishEvent(
                new OrderCreatedEvent(
                        order.getId(),
                        order.getCustomerId()
                )
        );

        return order;
    }
}
```

Observers:

```java
@Component
public class OrderEmailListener {

    @EventListener
    public void sendEmail(
            OrderCreatedEvent event
    ) {
        // Send confirmation email
    }
}
```

```java
@Component
public class OrderAuditListener {

    @EventListener
    public void audit(
            OrderCreatedEvent event
    ) {
        // Record audit event
    }
}
```

### Appropriate when

- Multiple components react independently
- Loose coupling is useful
- The event is meaningful to the domain
- The publisher should not know every consumer

### Avoid when

- The caller requires an immediate response from every listener.
- Execution order must be tightly controlled.
- Failure handling must be atomic.
- Durable cross-service delivery is required.

---

## Q39: How is Observer used in event-driven systems?

In event-driven systems, the Observer idea is extended through a broker or event bus.

```text
Order service
    ↓ publishes OrderCreated
Kafka topic
    ├── Inventory consumer
    ├── Notification consumer
    ├── Analytics consumer
    └── Billing consumer
```

The producer does not call each consumer directly.

### Differences from in-memory Observer

| In-memory Observer                              | Event-driven messaging                                  |
| ----------------------------------------------- | ------------------------------------------------------- |
| Same process                                    | Separate processes or services                          |
| Usually synchronous unless configured otherwise | Usually asynchronous                                    |
| Event may be lost on process crash              | Broker can provide durable storage                      |
| Simple failure handling                         | Requires retries, dead-letter handling, and idempotency |
| Shared memory model                             | Network and serialization involved                      |

### Production concerns

- Duplicate delivery
- Idempotent consumers
- Event ordering
- Schema evolution
- Retry policies
- Dead-letter topics
- Consumer lag
- Transactional outbox
- Monitoring and tracing

Observer is the conceptual basis, but distributed event-driven systems require additional reliability patterns.

---

## Q40: Adapter Pattern vs Decorator Pattern

| Adapter                             | Decorator                              |
| ----------------------------------- | -------------------------------------- |
| Converts one interface into another | Adds behavior to an existing interface |
| Solves interface incompatibility    | Extends responsibility dynamically     |
| Client sees the expected interface  | Client sees the original interface     |
| Common for third-party integrations | Common for logging, compression, retry |
| Focuses on compatibility            | Focuses on behavior extension          |

### Adapter example

External API:

```java
public class LegacyPaymentClient {

    public LegacyResponse execute(
            LegacyRequest request
    ) {
        return new LegacyResponse();
    }
}
```

Application interface:

```java
public interface PaymentGateway {

    PaymentResult charge(
            PaymentRequest request
    );
}
```

Adapter:

```java
public class LegacyPaymentAdapter
        implements PaymentGateway {

    private final LegacyPaymentClient client;

    public LegacyPaymentAdapter(
            LegacyPaymentClient client
    ) {
        this.client = client;
    }

    @Override
    public PaymentResult charge(
            PaymentRequest request
    ) {
        LegacyRequest legacyRequest =
                mapRequest(request);

        LegacyResponse legacyResponse =
                client.execute(legacyRequest);

        return mapResponse(legacyResponse);
    }
}
```

### Decorator example

```java
public class LoggingPaymentGateway
        implements PaymentGateway {

    private final PaymentGateway delegate;

    public LoggingPaymentGateway(
            PaymentGateway delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    public PaymentResult charge(
            PaymentRequest request
    ) {
        log.info("Starting payment");

        PaymentResult result =
                delegate.charge(request);

        log.info("Payment completed");

        return result;
    }
}
```

---

## Q41: What problem does the Proxy Pattern solve?

Proxy places an intermediary in front of a target object to control or intercept access.

It can provide:

- Authorization
- Transactions
- Caching
- Lazy loading
- Remote access
- Logging
- Metrics
- Rate limiting

```text
Caller
   ↓
Proxy
   ├── Perform checks or setup
   ├── Invoke target
   └── Perform cleanup
```

### Spring example

```java
@Service
public class TransferService {

    @Transactional
    public void transfer(
            long sourceId,
            long destinationId,
            BigDecimal amount
    ) {
        // Database operations
    }
}
```

Conceptually:

```text
Caller
   ↓
Spring transaction proxy
   ↓
Start transaction
   ↓
Call TransferService
   ↓
Commit or roll back
```

The proxy preserves the target’s interface while adding cross-cutting behavior.

### Self-invocation consequence

```java
this.transactionalMethod();
```

does not pass through the proxy again, so the inner method’s transactional configuration is not newly applied.

---

## Q42: How is Template Method different from Strategy?

| Template Method                    | Strategy                                   |
| ---------------------------------- | ------------------------------------------ |
| Uses inheritance                   | Uses composition                           |
| Base class defines workflow        | Interface defines interchangeable behavior |
| Subclasses override selected steps | Entire algorithm is replaceable            |
| Workflow order is fixed            | Strategy can change at runtime             |
| Stronger coupling to base class    | More flexible and loosely coupled          |

### Template Method example

```java
public abstract class ReportGenerator {

    public final Report generate() {
        Data data = loadData();
        Data validated = validate(data);
        return format(validated);
    }

    protected abstract Data loadData();

    protected Data validate(Data data) {
        return data;
    }

    protected abstract Report format(Data data);
}
```

### Strategy example

```java
public interface ReportFormatter {

    Report format(Data data);
}
```

```java
public class ReportService {

    private final ReportFormatter formatter;

    public ReportService(
            ReportFormatter formatter
    ) {
        this.formatter = formatter;
    }

    public Report generate(Data data) {
        return formatter.format(data);
    }
}
```

### Interview answer

> Template Method fixes an algorithm’s structure in a base class and lets subclasses override selected steps. Strategy uses composition and replaces the complete algorithm through an interface.

---

## Q43: What is Dependency Injection, and which pattern does it use?

Dependency Injection means that an object receives its dependencies from outside instead of constructing them itself.

Without DI:

```java
public class OrderService {

    private final PaymentClient paymentClient =
            new PaymentClient();
}
```

With DI:

```java
@Service
public class OrderService {

    private final PaymentClient paymentClient;

    public OrderService(
            PaymentClient paymentClient
    ) {
        this.paymentClient = paymentClient;
    }
}
```

### Is DI a GoF pattern?

Dependency Injection is not one of the classic Gang of Four patterns. It is an architectural technique used to implement **Inversion of Control**.

A DI container often uses several design patterns internally:

- Factory for object creation
- Singleton or other scopes for lifecycle
- Proxy for cross-cutting behavior
- Strategy for selecting implementations
- Registry for storing bean definitions

### Interview answer

> Dependency Injection is an IoC technique where dependencies are supplied from outside the object. It is not a single GoF pattern, although DI containers commonly use Factory, Registry, Singleton scope, Strategy, and Proxy internally.

---

## Q44: Which design patterns are commonly used in Spring Framework?

| Spring feature                                 | Related pattern                  |
| ---------------------------------------------- | -------------------------------- |
| `BeanFactory` and `ApplicationContext`         | Factory and Registry             |
| Default bean scope                             | Singleton scope                  |
| Dependency injection                           | Dependency Injection / IoC       |
| `@Transactional`                               | Proxy                            |
| `@Cacheable`                                   | Proxy                            |
| `@Async`                                       | Proxy                            |
| Spring AOP                                     | Proxy                            |
| `JdbcTemplate`                                 | Template Method                  |
| `RestTemplate`                                 | Template-style abstraction       |
| `ApplicationEventPublisher`                    | Observer                         |
| `HandlerAdapter`                               | Adapter                          |
| `DispatcherServlet`                            | Front Controller                 |
| Spring MVC                                     | Model–View–Controller            |
| Spring Data repositories                       | Repository                       |
| Servlet and security filters                   | Chain of Responsibility          |
| `BeanPostProcessor`                            | Extension/interception mechanism |
| Multiple implementations injected by interface | Strategy                         |
| `@Bean` methods                                | Factory Method                   |

### Important nuance

A Spring feature may combine several patterns. These mappings describe the dominant design intent rather than a strict one-to-one implementation.

---

## Q45: Which design patterns are most commonly used in microservices?

Microservice patterns are mostly architectural and distributed-system patterns rather than classic GoF patterns.

## 1. API Gateway

Provides one entry point for clients.

Responsibilities may include:

- Routing
- Authentication
- Rate limiting
- TLS termination
- Request transformation

```text
Clients
   ↓
API Gateway
   ├── Order service
   ├── Payment service
   └── Inventory service
```

---

## 2. Service Discovery

Allows services to locate dynamically changing instances.

```text
Order service
    ↓
Service registry or platform DNS
    ↓
Inventory service instances
```

---

## 3. Circuit Breaker

Stops repeatedly calling a failing dependency.

```text
CLOSED
→ calls allowed

OPEN
→ calls rejected temporarily

HALF_OPEN
→ limited test calls
```

---

## 4. Retry

Retries temporary failures using:

- Limited attempts
- Exponential backoff
- Jitter
- Idempotency

Retry should not be applied blindly to non-idempotent operations.

---

## 5. Bulkhead

Separates resources so one failing dependency does not exhaust the whole service.

Examples:

- Separate thread pools
- Separate connection pools
- Separate concurrency limits

---

## 6. Saga

Coordinates a business workflow through multiple local transactions.

```text
Create order
→ reserve inventory
→ authorize payment
→ confirm order
```

Failure may trigger compensation:

```text
Payment fails
→ release inventory
→ cancel order
```

---

## 7. Transactional Outbox

Stores a business change and outgoing event in one local transaction.

```text
Order table update
+
Outbox event insert
=
One database transaction
```

A separate publisher sends the event later.

---

## 8. Idempotent Consumer

Ensures duplicate message delivery does not duplicate the business effect.

```text
Message ID already processed?
├── Yes → ignore or return previous result
└── No  → process and record ID
```

---

## 9. CQRS

Separates write models from read models.

```text
Commands
→ Write model

Queries
→ Read-optimized model
```

Useful when reads and writes have significantly different requirements.

---

## 10. Event Sourcing

Stores state changes as an ordered sequence of events.

```text
AccountOpened
MoneyDeposited
MoneyWithdrawn
AccountFrozen
```

Current state is derived by replaying events or using snapshots.

It adds substantial complexity and should not be used only because the application uses events.

---

## 11. Strangler Fig

Gradually replaces a legacy system.

```text
Client traffic
     ↓
Routing layer
     ├── Legacy functionality
     └── New microservice functionality
```

More functionality moves to the new system over time.

---

## 12. Database per Service

Each service owns its data and exposes it through APIs or events.

```text
Order service → Order database
Payment service → Payment database
Inventory service → Inventory database
```

Services should not directly modify another service’s tables.

---

## 13. Sidecar

Runs supporting functionality beside the application instance.

Examples:

- Service-mesh proxy
- Log collector
- Secret agent
- Monitoring agent

---

## 14. Backend for Frontend

Creates client-specific backend APIs.

```text
Mobile app → Mobile BFF
Web app → Web BFF
Partner app → Partner BFF
```

This prevents one generic API from becoming overly complex for every client.

---

# Quick Comparison

| Pattern              | Main purpose                              |
| -------------------- | ----------------------------------------- |
| Factory              | Centralize object creation                |
| Abstract Factory     | Create families of related objects        |
| Builder              | Construct complex or immutable objects    |
| Singleton            | Restrict instance count                   |
| Strategy             | Swap algorithms                           |
| Observer             | Notify multiple listeners                 |
| Adapter              | Convert one interface into another        |
| Decorator            | Add responsibilities                      |
| Proxy                | Control or intercept access               |
| Template Method      | Fix workflow and customize steps          |
| Dependency Injection | Supply dependencies externally            |
| Repository           | Abstract persistence                      |
| Circuit Breaker      | Stop repeated failing calls               |
| Saga                 | Coordinate distributed transactions       |
| Outbox               | Reliably publish local transaction events |
| Bulkhead             | Isolate resource failure                  |
| Strangler Fig        | Incrementally replace legacy systems      |

# Interview Summary

> Factory hides concrete object creation, while Abstract Factory creates related families of objects. Builder is useful for complex immutable objects because it separates mutable construction from the final state. Strategy replaces algorithms through composition, while Template Method fixes the workflow through inheritance. Adapter solves interface incompatibility, Decorator adds responsibilities, and Proxy controls access or adds interception. Spring uses patterns such as Factory, Proxy, Template Method, Observer, Adapter, Repository, Front Controller, and Dependency Injection. In microservices, the most common architectural patterns include API Gateway, Circuit Breaker, Retry, Bulkhead, Saga, Transactional Outbox, Idempotent Consumer, CQRS, Database per Service, and Strangler Fig.
