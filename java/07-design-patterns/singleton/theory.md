# Design Patterns Used in Backend Systems

## 1. Definition

Design patterns are reusable approaches to recurring software-design problems. They provide a shared vocabulary for discussing structure and behavior without prescribing one fixed implementation.

In Java backend systems, frequently encountered patterns include:

| Category               | Common patterns                                              |
| ---------------------- | ------------------------------------------------------------ |
| Creational             | Factory, Builder, Singleton                                  |
| Structural             | Adapter, Decorator, Proxy, Facade                            |
| Behavioral             | Strategy, Template Method, Observer, Chain of Responsibility |
| Enterprise/application | Repository, Dependency Injection, Service Layer              |
| Distributed systems    | Circuit Breaker, Saga, Transactional Outbox                  |

An important distinction is that **Repository** and **Dependency Injection** are not classic Gang of Four patterns, but they are widely used architectural and enterprise patterns.

---

# 2. Why Design Patterns Are Useful

Patterns help developers:

- Communicate design decisions clearly
- Separate changing behavior from stable behavior
- Reduce coupling between components
- Improve testability
- Encapsulate object creation
- Add cross-cutting behavior without changing business logic
- Avoid repeating previously solved design mistakes

For example:

```text
“Use Strategy for payment selection”
```

communicates more clearly than:

```text
“Create an interface, add multiple implementations,
inject one depending on runtime conditions, and delegate to it.”
```

Patterns should solve an actual design problem. Adding layers only because a pattern exists usually makes the system harder to understand.

---

# 3. Strategy Pattern

## Definition

The Strategy pattern places interchangeable algorithms behind a common interface.

Use it when:

- Several implementations perform the same responsibility differently.
- Behavior must be selected at runtime.
- A large `if-else` or `switch` chooses an algorithm.
- Each algorithm should be tested independently.

## Example: payment processing

```java
public interface PaymentStrategy {

    PaymentResult pay(
            PaymentRequest request
    );
}
```

```java
@Component("card")
public class CreditCardPaymentStrategy
        implements PaymentStrategy {

    @Override
    public PaymentResult pay(
            PaymentRequest request
    ) {
        return new PaymentResult(
                "CARD_PAYMENT_COMPLETED"
        );
    }
}
```

```java
@Component("bankTransfer")
public class BankTransferPaymentStrategy
        implements PaymentStrategy {

    @Override
    public PaymentResult pay(
            PaymentRequest request
    ) {
        return new PaymentResult(
                "BANK_TRANSFER_INITIATED"
        );
    }
}
```

Spring can inject all implementations as a map:

```java
@Service
public class PaymentService {

    private final Map<String, PaymentStrategy> strategies;

    public PaymentService(
            Map<String, PaymentStrategy> strategies
    ) {
        this.strategies = strategies;
    }

    public PaymentResult pay(
            String paymentType,
            PaymentRequest request
    ) {
        PaymentStrategy strategy =
                strategies.get(paymentType);

        if (strategy == null) {
            throw new UnsupportedPaymentTypeException(
                    paymentType
            );
        }

        return strategy.pay(request);
    }
}
```

## Without Strategy

```java
public PaymentResult pay(
        String type,
        PaymentRequest request
) {
    if ("card".equals(type)) {
        // Card processing
    } else if ("bankTransfer".equals(type)) {
        // Bank-transfer processing
    } else if ("wallet".equals(type)) {
        // Wallet processing
    }

    throw new UnsupportedPaymentTypeException(type);
}
```

As the number of implementations grows, the conditional method becomes harder to extend and test.

## Trade-offs

**Advantages**

- Easy to add new algorithms
- Each implementation is independently testable
- Reduces complex conditional logic
- Supports runtime selection

**Disadvantages**

- Introduces more classes
- The caller needs a strategy-selection mechanism
- Unnecessary when only one implementation will ever exist

---

# 4. Template Method Pattern

## Definition

Template Method defines the overall algorithm in a base class while allowing subclasses to customize selected steps.

```java
public abstract class FileImportTemplate {

    public final ImportResult importFile(
            Path path
    ) {
        validateFile(path);

        List<String> rows = readRows(path);

        List<Record> records = parseRows(rows);

        validateRecords(records);

        persist(records);

        return new ImportResult(records.size());
    }

    protected abstract List<Record> parseRows(
            List<String> rows
    );

    protected void validateFile(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    "File does not exist"
            );
        }
    }

    protected void validateRecords(
            List<Record> records
    ) {
        // Default validation
    }

    protected abstract void persist(
            List<Record> records
    );

    private List<String> readRows(Path path) {
        // Read common file content
        return List.of();
    }
}
```

Implementation:

```java
public class CsvOrderImporter
        extends FileImportTemplate {

    @Override
    protected List<Record> parseRows(
            List<String> rows
    ) {
        return parseCsv(rows);
    }

    @Override
    protected void persist(
            List<Record> records
    ) {
        saveOrders(records);
    }
}
```

---

## Strategy vs Template Method

| Strategy                           | Template Method                             |
| ---------------------------------- | ------------------------------------------- |
| Uses composition                   | Uses inheritance                            |
| Replaces the complete algorithm    | Customizes selected algorithm steps         |
| Strategy can change at runtime     | Base-class structure is fixed               |
| Implementations share an interface | Implementations extend a base class         |
| Usually more flexible              | Useful when workflow order must be enforced |

### Practical guideline

Prefer Strategy when implementations are independent and interchangeable.

Use Template Method when:

- Every implementation must follow the same sequence.
- Some steps are fixed.
- Only selected steps should be customized.

---

# 5. Builder Pattern

## Definition

Builder constructs a complex object incrementally instead of requiring a large constructor.

## Problematic constructor

```java
Order order = new Order(
        customerId,
        shippingAddress,
        billingAddress,
        lineItems,
        discount,
        currency,
        priority,
        note
);
```

It is difficult to understand which argument represents which field.

## Builder example

```java
Order order = Order.builder()
        .customerId(customerId)
        .shippingAddress(shippingAddress)
        .currency(Currency.USD)
        .priority(OrderPriority.NORMAL)
        .addLineItem(lineItem)
        .build();
```

Manual implementation:

```java
public final class Order {

    private final Long customerId;
    private final List<LineItem> lineItems;
    private final Currency currency;

    private Order(Builder builder) {
        this.customerId = builder.customerId;
        this.lineItems = List.copyOf(
                builder.lineItems
        );
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

        public Builder addLineItem(LineItem item) {
            this.lineItems.add(item);
            return this;
        }

        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Order build() {
            Objects.requireNonNull(
                    customerId,
                    "customerId"
            );

            if (lineItems.isEmpty()) {
                throw new IllegalStateException(
                        "Order requires a line item"
                );
            }

            return new Order(this);
        }
    }
}
```

## Good uses

- Immutable objects
- Many optional parameters
- Complex validation
- Test-data construction
- Objects created in several stages

## Common mistake

A builder should not allow the creation of an invalid object. Required-field and invariant validation should happen in `build()` or the domain constructor.

---

# 6. Factory Pattern

## Definition

A Factory centralizes object-creation decisions.

```java
public interface NotificationSender {

    void send(Notification notification);
}
```

```java
@Component
public class NotificationSenderFactory {

    private final Map<Channel, NotificationSender> senders;

    public NotificationSenderFactory(
            EmailNotificationSender emailSender,
            SmsNotificationSender smsSender
    ) {
        this.senders = Map.of(
                Channel.EMAIL,
                emailSender,
                Channel.SMS,
                smsSender
        );
    }

    public NotificationSender getSender(
            Channel channel
    ) {
        NotificationSender sender =
                senders.get(channel);

        if (sender == null) {
            throw new UnsupportedChannelException(
                    channel
            );
        }

        return sender;
    }
}
```

Usage:

```java
NotificationSender sender =
        senderFactory.getSender(channel);

sender.send(notification);
```

## Factory vs Strategy

- **Factory** chooses or creates the implementation.
- **Strategy** performs the selected behavior.

They frequently work together:

```text
Factory selects PaymentStrategy
        ↓
PaymentStrategy processes payment
```

---

# 7. Singleton Pattern in Spring

## Classic Singleton

A manually implemented singleton controls its own instance creation:

```java
public final class ConfigurationRegistry {

    private static final ConfigurationRegistry INSTANCE =
            new ConfigurationRegistry();

    private ConfigurationRegistry() {
    }

    public static ConfigurationRegistry getInstance() {
        return INSTANCE;
    }
}
```

## Spring Singleton Scope

Spring beans are singleton-scoped by default, but a Spring singleton is **one instance per bean definition per container**, not necessarily one instance per JVM, application, cluster, or classloader. ([Home][1])

```java
@Service
public class PricingService {
}
```

There is normally no need to add:

```java
private static PricingService instance;
```

## Important limitations

A Spring singleton is not:

- Automatically thread-safe
- Globally unique across multiple application contexts
- Unique across multiple JVMs
- A distributed singleton
- One instance per class in every situation

Two bean definitions can create two instances of the same class:

```java
@Configuration
public class ClientConfiguration {

    @Bean
    PaymentClient primaryPaymentClient() {
        return new PaymentClient("primary");
    }

    @Bean
    PaymentClient backupPaymentClient() {
        return new PaymentClient("backup");
    }
}
```

## Thread-safety concern

Because singleton beans are commonly shared across request threads, avoid mutable request-specific fields:

```java
@Service
public class OrderService {

    // Unsafe shared mutable request state
    private Order currentOrder;
}
```

Prefer method-local state:

```java
@Service
public class OrderService {

    public OrderResponse process(
            OrderRequest request
    ) {
        Order order = createOrder(request);

        return map(order);
    }
}
```

---

# 8. Proxy Pattern

## Definition

A Proxy exposes the same interface as another object but controls or intercepts access to it.

A proxy can add:

- Transaction boundaries
- Security checks
- Lazy loading
- Caching
- Remote communication
- Logging
- Metrics

```java
public interface PaymentService {

    PaymentResult pay(PaymentRequest request);
}
```

```java
public class SecuredPaymentServiceProxy
        implements PaymentService {

    private final PaymentService target;
    private final AuthorizationService authorization;

    public SecuredPaymentServiceProxy(
            PaymentService target,
            AuthorizationService authorization
    ) {
        this.target = target;
        this.authorization = authorization;
    }

    @Override
    public PaymentResult pay(
            PaymentRequest request
    ) {
        authorization.verifyPermission(
                "payments.write"
        );

        return target.pay(request);
    }
}
```

---

# 9. How Spring Uses Proxies

Spring AOP can create proxies using JDK dynamic proxies or class-based CGLIB proxies. JDK proxies implement interfaces, while CGLIB creates a generated subclass of the target class. Final classes, final methods, and private methods impose limitations on class-based proxy advice. ([Home][2])

```text
Caller
   ↓
Spring proxy
   ├── Before behavior
   ├── Call target method
   └── After behavior
```

Many Spring method-level cross-cutting features are proxy-based by default, including:

- `@Transactional`
- `@Cacheable`
- `@CachePut`
- `@CacheEvict`
- `@Async`
- Method-security advice
- Custom Spring AOP aspects

It is too broad to say that **every Spring feature** uses proxies. Component scanning, configuration-property binding, MVC request mapping, bean creation, and many other framework facilities use different mechanisms.

---

## `@Transactional` example

```java
@Service
public class TransferService {

    @Transactional
    public void transfer(
            long sourceId,
            long destinationId,
            BigDecimal amount
    ) {
        debit(sourceId, amount);
        credit(destinationId, amount);
    }
}
```

Conceptually:

```text
Caller
  ↓
Transaction proxy
  ↓
Start or join transaction
  ↓
Invoke transfer()
  ↓
Commit or roll back
```

Spring’s default transaction advice mode is proxy-based, meaning only calls entering through the proxy are intercepted. ([Home][3])

---

## Why self-invocation fails

```java
@Service
public class PaymentService {

    public void processPayment() {
        savePayment();
    }

    @Transactional
    public void savePayment() {
        // Database operations
    }
}
```

The internal call is equivalent to:

```java
this.savePayment();
```

The invocation remains inside the target object:

```text
External caller
      ↓
PaymentService proxy
      ↓
processPayment()
      ↓
this.savePayment()
      ↓
Proxy not crossed again
```

Therefore, the `savePayment()` method’s transactional advice is not applied. Spring explicitly documents that self-invocation bypasses proxy advice and recommends redesigning the code to avoid it. ([Home][2])

## Preferred correction

Move the transactional responsibility into another bean:

```java
@Service
public class PaymentWriter {

    @Transactional
    public void savePayment(
            Payment payment
    ) {
        // Persist payment
    }
}
```

```java
@Service
public class PaymentService {

    private final PaymentWriter paymentWriter;

    public PaymentService(
            PaymentWriter paymentWriter
    ) {
        this.paymentWriter = paymentWriter;
    }

    public void processPayment(
            Payment payment
    ) {
        paymentWriter.savePayment(payment);
    }
}
```

The call now crosses the proxy boundary:

```text
PaymentService
      ↓
PaymentWriter proxy
      ↓
Transactional advice
      ↓
PaymentWriter target
```

The same general self-invocation limitation applies to Spring’s default proxy mode for caching and `@Async`. ([Home][4])

---

# 10. Decorator Pattern

## Definition

Decorator wraps an object to add responsibilities while preserving its interface.

```java
public interface MessageSender {

    void send(Message message);
}
```

Base implementation:

```java
public class EmailMessageSender
        implements MessageSender {

    @Override
    public void send(Message message) {
        // Send email
    }
}
```

Logging decorator:

```java
public class LoggingMessageSenderDecorator
        implements MessageSender {

    private final MessageSender delegate;

    public LoggingMessageSenderDecorator(
            MessageSender delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    public void send(Message message) {
        log.info(
                "Sending message id={}",
                message.id()
        );

        delegate.send(message);

        log.info(
                "Message sent id={}",
                message.id()
        );
    }
}
```

Retry decorator:

```java
public class RetryMessageSenderDecorator
        implements MessageSender {

    private final MessageSender delegate;

    public RetryMessageSenderDecorator(
            MessageSender delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    public void send(Message message) {
        int attempts = 0;

        while (true) {
            try {
                delegate.send(message);
                return;
            } catch (TemporaryFailure exception) {
                attempts++;

                if (attempts >= 3) {
                    throw exception;
                }
            }
        }
    }
}
```

Decorators can be composed:

```java
MessageSender sender =
        new RetryMessageSenderDecorator(
                new LoggingMessageSenderDecorator(
                        new EmailMessageSender()
                )
        );
```

---

# 11. Decorator vs Proxy

Both patterns may wrap another object using the same interface, but their primary intent differs.

| Decorator                                       | Proxy                                                        |
| ----------------------------------------------- | ------------------------------------------------------------ |
| Adds responsibilities or behavior               | Controls access to the target                                |
| Often deliberately composed by application code | Often inserted transparently                                 |
| Multiple decorators may be stacked              | Often represents one controlled access boundary              |
| Examples: buffering, compression, logging       | Examples: security, transactions, lazy loading, remote proxy |
| Focuses on extending behavior                   | Focuses on mediation or access control                       |

## Important nuance

The distinction is based on **intent**, not only structure.

The same wrapper implementation could resemble both patterns structurally. You identify the pattern by asking:

```text
Why is this wrapper present?
```

- To extend responsibility → Decorator
- To control access or invocation → Proxy

---

# 12. Observer Pattern

## Definition

Observer allows one component to notify interested listeners when an event occurs.

Spring application events provide an in-process example.

```java
public record OrderCreatedEvent(
        long orderId,
        long customerId
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

    @Transactional
    public Order createOrder(
            CreateOrderCommand command
    ) {
        Order order = save(command);

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

Listener:

```java
@Component
public class OrderNotificationListener {

    @EventListener
    public void handle(
            OrderCreatedEvent event
    ) {
        sendConfirmation(event.orderId());
    }
}
```

## Transaction-aware listener

```java
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
)
public void handle(
        OrderCreatedEvent event
) {
    sendConfirmation(event.orderId());
}
```

## Important limitation

Spring application events are normally:

- In-process
- In-memory
- Bound to one application instance

They are not a durable replacement for Kafka, RabbitMQ, or a transactional outbox when communication must survive process failure.

---

# 13. Repository Pattern

## Definition

Repository presents domain-focused persistence operations while hiding lower-level data-access details.

```java
public interface OrderRepository
        extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByCustomerId(
            Long customerId
    );
}
```

Spring Data’s repository abstraction is specifically designed to reduce data-access boilerplate, with `Repository` serving as its central marker interface. ([Home][5])

## Domain-oriented repository

A stronger domain boundary may avoid exposing JPA directly:

```java
public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    List<Order> findPendingOrders(
            CustomerId customerId
    );

    void save(Order order);
}
```

Infrastructure implementation:

```java
@Repository
public class JpaOrderRepository
        implements OrderRepository {

    private final SpringDataOrderRepository delegate;
    private final OrderEntityMapper mapper;

    @Override
    public Optional<Order> findById(
            OrderId id
    ) {
        return delegate.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public void save(Order order) {
        delegate.save(
                mapper.toEntity(order)
        );
    }
}
```

## Benefits

- Isolates persistence decisions
- Improves testability
- Gives domain-specific method names
- Reduces database coupling in business logic

## Common mistake

Do not place all business logic inside repository queries. Repositories retrieve and persist state; services or domain objects should enforce business rules.

---

# 14. Dependency Injection

## Definition

Dependency Injection supplies an object with its collaborators instead of letting it construct them internally.

Without DI:

```java
public class OrderService {

    private final PaymentClient paymentClient =
            new PaymentClient(
                    "https://payments.example.com"
            );
}
```

With constructor injection:

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

## Benefits

- Makes dependencies explicit
- Allows implementations to be replaced
- Improves unit testing
- Separates construction from behavior
- Supports Strategy and Adapter patterns naturally

Unit test:

```java
@Test
void rejectsFailedPayment() {
    PaymentClient paymentClient =
            mock(PaymentClient.class);

    OrderService service =
            new OrderService(paymentClient);

    when(paymentClient.charge(any()))
            .thenThrow(
                    new PaymentRejectedException()
            );

    assertThrows(
            PaymentRejectedException.class,
            () -> service.placeOrder(request)
    );
}
```

---

# 15. Adapter Pattern

## Definition

Adapter converts one interface into another interface expected by the application.

Suppose an external provider exposes:

```java
public class ExternalPaymentSdk {

    public ExternalResult executePayment(
            ExternalRequest request
    ) {
        return new ExternalResult();
    }
}
```

The application expects:

```java
public interface PaymentGateway {

    PaymentResult charge(
            PaymentRequest request
    );
}
```

Adapter:

```java
@Component
public class ExternalPaymentGatewayAdapter
        implements PaymentGateway {

    private final ExternalPaymentSdk sdk;

    public ExternalPaymentGatewayAdapter(
            ExternalPaymentSdk sdk
    ) {
        this.sdk = sdk;
    }

    @Override
    public PaymentResult charge(
            PaymentRequest request
    ) {
        ExternalRequest externalRequest =
                mapRequest(request);

        ExternalResult result =
                sdk.executePayment(externalRequest);

        return mapResult(result);
    }
}
```

This prevents third-party types from spreading through the business layer.

---

# 16. Facade Pattern

## Definition

Facade provides a simplified entry point over multiple internal components.

```java
@Service
public class CheckoutFacade {

    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public CheckoutResult checkout(
            CheckoutRequest request
    ) {
        InventoryReservation reservation =
                inventoryService.reserve(request.items());

        Payment payment =
                paymentService.authorize(
                        request.payment(),
                        request.total()
                );

        Order order =
                orderService.create(
                        request,
                        reservation,
                        payment
                );

        notificationService.sendConfirmation(order);

        return new CheckoutResult(order.getId());
    }
}
```

The caller interacts with one operation instead of coordinating four services directly.

## Risk

A facade can become a large “god service” if it absorbs:

- Every business rule
- Every integration
- Every data transformation
- Every workflow

Keep it focused on orchestration.

---

# 17. Chain of Responsibility

## Definition

Chain of Responsibility passes a request through a sequence of handlers.

This is useful for:

- Validation pipelines
- Security filters
- Request enrichment
- Fraud checks
- Approval workflows

```java
public interface OrderValidator {

    void validate(
            CreateOrderRequest request
    );
}
```

```java
@Component
@Order(1)
public class CustomerValidator
        implements OrderValidator {

    @Override
    public void validate(
            CreateOrderRequest request
    ) {
        // Validate customer
    }
}
```

```java
@Component
@Order(2)
public class InventoryValidator
        implements OrderValidator {

    @Override
    public void validate(
            CreateOrderRequest request
    ) {
        // Validate inventory
    }
}
```

```java
@Service
public class OrderValidationService {

    private final List<OrderValidator> validators;

    public OrderValidationService(
            List<OrderValidator> validators
    ) {
        this.validators = validators;
    }

    public void validate(
            CreateOrderRequest request
    ) {
        validators.forEach(
                validator -> validator.validate(request)
        );
    }
}
```

Spring Security’s filter chain is also conceptually related to Chain of Responsibility: each filter processes a request and may continue or stop the chain.

---

# 18. Patterns Commonly Visible in Spring

| Spring feature                    | Related pattern                  |
| --------------------------------- | -------------------------------- |
| Dependency injection container    | Dependency Injection / IoC       |
| `@Bean` factory methods           | Factory Method                   |
| `ApplicationEventPublisher`       | Observer                         |
| `HandlerInterceptor` and filters  | Chain of Responsibility          |
| `JdbcTemplate`                    | Template Method                  |
| Spring Data repositories          | Repository                       |
| `@Transactional`                  | Proxy                            |
| `@Cacheable`                      | Proxy                            |
| `@Async`                          | Proxy                            |
| `RestClient` around external APIs | Adapter or Facade                |
| Bean scopes                       | Object lifecycle management      |
| `BeanPostProcessor`               | Extension/interception mechanism |

The mapping is conceptual. A framework feature may combine several patterns internally rather than representing exactly one textbook pattern.

---

# 19. Common Mistakes

## Mistake 1: Manually implementing Singleton in Spring

```java
public class PaymentService {

    private static PaymentService instance;
}
```

This bypasses container-managed construction and makes dependency injection, lifecycle management, and testing more difficult.

Use a normal Spring singleton bean unless the class has a specific non-Spring lifecycle requirement. Remember that Spring singleton scope is per bean and per container. ([Home][1])

---

## Mistake 2: Assuming singleton means thread-safe

```java
@Service
public class CounterService {

    private int count;

    public int increment() {
        return ++count;
    }
}
```

A singleton scope controls instance count. It does not synchronize mutable state.

---

## Mistake 3: Calling proxied methods through `this`

```java
this.transactionalMethod();
this.cachedMethod();
this.asyncMethod();
```

With the default proxy modes, these internal calls bypass method interception for transactions, caching, and asynchronous execution. ([Home][3])

---

## Mistake 4: Using a pattern before it is needed

Creating:

```text
interface
→ abstract class
→ factory
→ registry
→ implementation
```

for one simple implementation may add more complexity than value.

Introduce abstraction when there is genuine variation, isolation, or testability value.

---

## Mistake 5: Confusing Spring singleton with distributed singleton

With three service instances:

```text
Pod 1 → one Spring singleton
Pod 2 → one Spring singleton
Pod 3 → one Spring singleton
```

There are three instances across the deployment.

A Spring singleton cannot enforce cluster-wide uniqueness for:

- Scheduled jobs
- ID generation
- Leader election
- Resource ownership

Those problems require distributed coordination or an external authoritative system.

---

## Mistake 6: Confusing Decorator and Proxy

The code structure may look similar. Explain the difference through intent:

```text
Decorator:
What responsibility is being added?

Proxy:
What access or invocation is being controlled?
```

---

# 20. Trade-Offs

| Benefit                                    | Cost                                 |
| ------------------------------------------ | ------------------------------------ |
| Clear separation of responsibilities       | More classes and interfaces          |
| Easy implementation replacement            | Additional indirection               |
| Better testability                         | More navigation through code         |
| Cross-cutting behavior without duplication | Proxy and lifecycle limitations      |
| Cleaner extension points                   | Risk of premature abstraction        |
| Shared design vocabulary                   | Patterns may be applied mechanically |

Use a pattern when its flexibility or separation is worth the additional complexity.

---

# 21. Interview Questions and Answers

## Q1: Strategy vs Template Method—what is the difference?

> Strategy uses composition and places complete interchangeable algorithms behind an interface. Template Method uses inheritance and fixes the workflow in a base class while subclasses customize selected steps. I normally prefer Strategy when implementations need to vary independently or at runtime.

## Q2: Decorator vs Proxy—what is the difference?

> Both wrap another object behind the same interface. Decorator primarily adds responsibilities, while Proxy primarily controls or mediates access. The structural code can look similar, so the difference is mainly the design intent.

## Q3: How does Spring implement AOP?

> Spring AOP commonly creates a proxy around a managed bean using either JDK dynamic proxies or CGLIB. Calls entering through the proxy can be intercepted by advice before or after the target method. ([Home][2])

## Q4: Why does `@Transactional` fail on self-invocation?

> Spring’s default transaction mode intercepts calls entering through the proxy. An internal call uses `this`, remains inside the target object, and does not cross the proxy again, so the called method’s own transactional advice is not applied. ([Home][3])

## Q5: Does self-invocation always mean there is no transaction?

> No. The outer method may already have started a transaction. Self-invocation means that the inner method’s own transaction settings—such as `REQUIRES_NEW`, isolation, timeout, or rollback rules—are not newly intercepted and applied.

## Q6: Why is a manual Singleton usually unnecessary in Spring?

> Spring beans use singleton scope by default. The container manages one instance per bean definition per container, including its dependencies and lifecycle. A manually coded static singleton usually works against dependency injection and makes testing more difficult. ([Home][1])

## Q7: Factory vs Builder?

> Factory decides which object or implementation to create. Builder assembles one complex object step by step. A factory can internally use a builder.

## Q8: Repository vs DAO?

> Both isolate persistence logic. DAO often mirrors lower-level database operations, while Repository is normally expressed in domain terms and behaves like a collection of aggregates. In many Spring applications, the terms overlap.

## Q9: When would you use Observer in Spring?

> I use in-process Spring events when multiple components need to react to a domain event without direct coupling. For durable communication across services, I use a message broker and usually a transactional outbox rather than relying only on application events.

## Q10: Are `@Transactional`, `@Cacheable`, and `@Async` always implemented with proxies?

> Proxy mode is the normal default for these annotation-driven features, but Spring also supports AspectJ-based interception in selected cases. It is safer to say that they are proxy-based by default rather than claiming they can only work through proxies. ([Home][3])

---

# 22. Short Interview Answer

> The backend patterns I use most are Strategy for interchangeable business rules, Factory for implementation selection, Builder for creating complex immutable objects, Repository for isolating persistence, Adapter for external integrations, Observer for in-process events, and Proxy for cross-cutting concerns. Spring applies features such as `@Transactional`, `@Cacheable`, and `@Async` through proxies by default. That is why self-invocation is important: a call through `this` does not cross the proxy, so the called method’s annotation is not intercepted. I also avoid manual static singletons in Spring because beans already use a per-container, per-bean singleton scope by default.

[1]: https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html?utm_source=chatgpt.com "Bean Scopes"
[2]: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html?utm_source=chatgpt.com "Proxying Mechanisms"
[3]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com "Using @Transactional"
[4]: https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com "Declarative Annotation-based Caching"
[5]: https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html?utm_source=chatgpt.com "Core concepts :: Spring Data JPA"
