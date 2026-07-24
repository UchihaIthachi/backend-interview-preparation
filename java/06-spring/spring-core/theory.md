# Spring Core: IoC, Dependency Injection, and Bean Lifecycle

## 1. What Are IoC and Dependency Injection?

### Inversion of Control

**Inversion of Control**, or **IoC**, means application code does not directly control the creation, configuration, and wiring of its collaborating objects.

Instead, the Spring container:

1. Reads bean definitions.
2. Creates objects.
3. Resolves their dependencies.
4. Injects those dependencies.
5. Applies lifecycle callbacks.
6. Wraps beans with proxies when necessary.
7. Manages their configured scopes.

### Dependency Injection

**Dependency Injection**, or **DI**, is the mechanism Spring commonly uses to implement IoC.

A class declares what it needs, and the container supplies those dependencies.

Without DI:

```java
public final class OrderService {

    private final PaymentGateway paymentGateway =
            new StripePaymentGateway();

    public void createOrder(Order order) {
        paymentGateway.charge(order);
    }
}
```

`OrderService` is tightly coupled to `StripePaymentGateway`.

With DI:

```java
@Service
public class OrderService {

    private final PaymentGateway paymentGateway;

    public OrderService(
            PaymentGateway paymentGateway
    ) {
        this.paymentGateway = paymentGateway;
    }

    public void createOrder(Order order) {
        paymentGateway.charge(order);
    }
}
```

The service depends on the `PaymentGateway` abstraction rather than creating a concrete implementation.

Spring’s documentation recommends constructor injection for mandatory dependencies and setter or configuration-method injection for optional dependencies. ([Home][1])

---

## 2. What Is a Spring Bean?

A **Spring bean** is an object whose creation and lifecycle are managed by a Spring IoC container.

Beans may be registered through:

- Component scanning
- `@Bean` methods
- XML configuration
- Programmatic registration
- Auto-configuration supplied by Spring Boot

```text
Bean definition
      ↓
Spring container
      ↓
Bean instance
```

A bean definition is effectively a recipe containing information such as:

- Bean class
- Bean name
- Scope
- Dependencies
- Constructor arguments
- Initialization method
- Destruction method
- Lazy initialization
- Qualifiers

---

# Spring Containers

## 3. `BeanFactory` vs `ApplicationContext`

### `BeanFactory`

`BeanFactory` is the foundational interface for:

- Bean creation
- Dependency resolution
- Scope management
- Bean retrieval

### `ApplicationContext`

`ApplicationContext` builds on the bean-factory functionality and adds features such as:

- Automatic detection of post-processors
- Application events
- Message resolution
- Environment and property access
- Resource loading
- Integration with web application contexts

Most Spring and Spring Boot applications work with an `ApplicationContext` rather than directly using a plain `BeanFactory`.

```java
ApplicationContext context =
        new AnnotationConfigApplicationContext(
                ApplicationConfiguration.class
        );

OrderService service =
        context.getBean(OrderService.class);
```

Application classes should normally receive dependencies through injection instead of repeatedly calling `getBean()`.

---

# Dependency-Injection Types

## 4. Constructor Injection

```java
@Service
public class CheckoutService {

    private final PaymentGateway paymentGateway;
    private final OrderRepository orderRepository;

    public CheckoutService(
            PaymentGateway paymentGateway,
            OrderRepository orderRepository
    ) {
        this.paymentGateway = paymentGateway;
        this.orderRepository = orderRepository;
    }
}
```

When a Spring bean has only one constructor, that constructor does not need to be annotated with `@Autowired`. Spring uses it as the injection point. ([Home][2])

### Advantages

- Mandatory dependencies are explicit.
- References can be declared `final`.
- The object cannot be created without required dependencies.
- Unit testing does not require starting Spring.
- Constructor cycles fail clearly.
- The class communicates its requirements through its API.

Example test:

```java
PaymentGateway gateway =
        mock(PaymentGateway.class);

OrderRepository repository =
        mock(OrderRepository.class);

CheckoutService service =
        new CheckoutService(
                gateway,
                repository
        );
```

### Important nuance

Declaring a dependency reference `final` prevents that reference from being reassigned:

```java
private final PaymentGateway paymentGateway;
```

It does not make the injected `PaymentGateway` object itself immutable.

---

## 5. Setter Injection

Setter injection is suitable for optional or reconfigurable dependencies.

```java
@Service
public class NotificationService {

    private AuditPublisher auditPublisher;

    @Autowired(required = false)
    public void setAuditPublisher(
            AuditPublisher auditPublisher
    ) {
        this.auditPublisher = auditPublisher;
    }
}
```

Another approach is to represent optionality explicitly:

```java
@Service
public class NotificationService {

    private final Optional<AuditPublisher> auditPublisher;

    public NotificationService(
            Optional<AuditPublisher> auditPublisher
    ) {
        this.auditPublisher = auditPublisher;
    }
}
```

Setter injection has some disadvantages:

- The object may temporarily exist without the dependency.
- The dependency cannot normally be `final`.
- Mandatory dependencies are less obvious.
- Mutability increases.

---

## 6. Field Injection

```java
@Service
public class PaymentService {

    @Autowired
    private PaymentGateway paymentGateway;
}
```

Field injection works, but it is usually a weaker design for application code.

Problems include:

- Dependencies are hidden from the constructor.
- Fields cannot normally be `final`.
- Direct unit testing is harder.
- Reflection or a Spring context may be needed to inject test values.
- Classes can accumulate too many dependencies without an obvious constructor smell.
- Instances created with `new` contain uninitialized fields.

Prefer:

```java
@Service
public class PaymentService {

    private final PaymentGateway paymentGateway;

    public PaymentService(
            PaymentGateway paymentGateway
    ) {
        this.paymentGateway = paymentGateway;
    }
}
```

---

## 7. Method Injection

Spring can also inject dependencies through arbitrary methods:

```java
@Component
public class ReportCoordinator {

    private ReportWriter reportWriter;
    private AuditPublisher auditPublisher;

    @Autowired
    public void configure(
            ReportWriter reportWriter,
            AuditPublisher auditPublisher
    ) {
        this.reportWriter = reportWriter;
        this.auditPublisher = auditPublisher;
    }
}
```

This is less common than constructor injection but can be useful for grouped configuration.

---

# How Autowiring Works

## 8. Dependency Resolution

Spring generally resolves dependencies by type.

```java
public interface PaymentGateway {
    PaymentResult charge(PaymentRequest request);
}
```

```java
@Component
public class CardPaymentGateway
        implements PaymentGateway {

    @Override
    public PaymentResult charge(
            PaymentRequest request
    ) {
        return new PaymentResult(true);
    }
}
```

```java
@Service
public class PaymentService {

    private final PaymentGateway paymentGateway;

    public PaymentService(
            PaymentGateway paymentGateway
    ) {
        this.paymentGateway = paymentGateway;
    }
}
```

Spring searches the application context for a compatible `PaymentGateway` bean.

Conceptually:

```text
Injection point requests PaymentGateway
             ↓
Find candidate beans by type
             ↓
Apply qualifiers and priority rules
             ↓
Resolve one candidate
             ↓
Inject bean
```

---

## 9. Multiple Implementations

Suppose two implementations exist:

```java
@Component
public class CardPaymentGateway
        implements PaymentGateway {
}
```

```java
@Component
public class BankTransferGateway
        implements PaymentGateway {
}
```

Spring cannot automatically choose one for a single-valued injection point unless more selection information is provided.

### Use `@Qualifier`

```java
@Service
public class PaymentService {

    private final PaymentGateway paymentGateway;

    public PaymentService(
            @Qualifier("cardPaymentGateway")
            PaymentGateway paymentGateway
    ) {
        this.paymentGateway = paymentGateway;
    }
}
```

### Use `@Primary`

```java
@Primary
@Component
public class CardPaymentGateway
        implements PaymentGateway {
}
```

`@Primary` gives one candidate preference when several beans match a single-valued dependency. ([Home][3])

### Inject all implementations

```java
@Service
public class PaymentRouter {

    private final List<PaymentGateway> gateways;

    public PaymentRouter(
            List<PaymentGateway> gateways
    ) {
        this.gateways = gateways;
    }
}
```

Spring can inject collections containing all compatible beans.

---

# Registering Beans

## 10. `@Component` and Stereotype Annotations

`@Component` marks a class as a candidate for component scanning.

```java
@Component
public class ExchangeRateProvider {
}
```

Spring provides specialized component stereotypes:

| Annotation        | Typical layer          |
| ----------------- | ---------------------- |
| `@Component`      | General component      |
| `@Service`        | Business/service layer |
| `@Repository`     | Persistence layer      |
| `@Controller`     | MVC controller         |
| `@RestController` | REST controller        |
| `@Configuration`  | Configuration class    |

`@Service`, `@Repository`, and `@Controller` are specialized forms of `@Component`. ([Home][4])

---

## 11. `@Bean`

`@Bean` is placed on a factory method that registers the returned object as a Spring bean.

```java
@Configuration
public class PaymentConfiguration {

    @Bean
    public PaymentGateway paymentGateway(
            HttpClient httpClient
    ) {
        return new ExternalPaymentGateway(
                httpClient
        );
    }
}
```

The method parameters are resolved as dependencies by the container.

`@Bean` is especially useful when:

- The class belongs to a third-party library.
- You cannot annotate its source code.
- Construction requires custom configuration.
- You need several differently configured instances.
- Bean creation depends on external properties.

Spring documents `@Bean` as the Java-configuration equivalent of declaring bean metadata, with the method’s return value registered in the application context. ([Home][5])

---

## 12. `@Component` vs `@Bean`

| `@Component`                               | `@Bean`                                       |
| ------------------------------------------ | --------------------------------------------- |
| Applied to a class                         | Applied to a method                           |
| Discovered through component scanning      | Declared explicitly in configuration          |
| Best for application-owned classes         | Useful for third-party or custom construction |
| Container invokes the constructor          | Configuration method creates the instance     |
| Bean name normally derives from class name | Bean name normally derives from method name   |

Example:

```java
@Service
public class OrderService {
}
```

versus:

```java
@Configuration
public class AppConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules();
    }
}
```

---

# Bean Lifecycle

## 13. Complete Lifecycle Overview

A simplified but interview-ready lifecycle is:

```text
1. Bean definition registered
2. Bean instantiated
3. Dependencies and properties populated
4. Aware callbacks invoked
5. BeanPostProcessor before-initialization callbacks
6. @PostConstruct
7. InitializingBean.afterPropertiesSet()
8. Custom init method
9. BeanPostProcessor after-initialization callbacks
10. Bean is ready for use
11. Application runs
12. @PreDestroy
13. DisposableBean.destroy()
14. Custom destroy method
```

Spring uses `BeanPostProcessor` implementations before and after initialization. Post-processors can modify bean instances or wrap them with proxies, and proxy-producing infrastructure commonly operates after initialization. ([Home][6])

---

## 14. Instantiation

Spring first creates the raw bean instance using:

- A constructor
- A static factory method
- An instance factory method
- A `@Bean` method
- Framework-specific factory infrastructure

```java
@Service
public class OrderService {

    public OrderService() {
        System.out.println("Constructor");
    }
}
```

At this point, the object exists, but the complete Spring lifecycle has not necessarily finished.

---

## 15. Dependency Population

After construction, Spring performs any applicable:

- Field injection
- Setter injection
- Configuration-property population

Constructor dependencies are already supplied during instantiation.

```java
@Component
public class ExampleBean {

    private final RequiredDependency required;
    private OptionalDependency optional;

    public ExampleBean(
            RequiredDependency required
    ) {
        this.required = required;
    }

    @Autowired(required = false)
    public void setOptional(
            OptionalDependency optional
    ) {
        this.optional = optional;
    }
}
```

---

## 16. Aware Callbacks

A bean may implement Spring “Aware” interfaces to receive container infrastructure.

Examples include:

- `BeanNameAware`
- `BeanFactoryAware`
- `ApplicationContextAware`
- `EnvironmentAware`
- `ResourceLoaderAware`

```java
@Component
public class ContextAwareBean
        implements BeanNameAware {

    @Override
    public void setBeanName(
            String beanName
    ) {
        System.out.println(
                "Bean name: " + beanName
        );
    }
}
```

These interfaces create direct coupling to Spring and should be used only when the component genuinely requires container infrastructure.

Do not use `ApplicationContextAware` merely as a service locator to avoid normal dependency injection.

---

## 17. `@PostConstruct`

`@PostConstruct` runs after dependency injection and before the bean is placed into normal service.

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ReferenceDataCache {

    private final ReferenceDataRepository repository;

    public ReferenceDataCache(
            ReferenceDataRepository repository
    ) {
        this.repository = repository;
    }

    @PostConstruct
    void initialize() {
        System.out.println(
                "Loading reference data"
        );

        repository.loadAll();
    }

    @PreDestroy
    void destroy() {
        System.out.println(
                "Releasing resources"
        );
    }
}
```

Modern Spring applications use the annotations from `jakarta.annotation`. Spring considers `@PostConstruct` and `@PreDestroy` standard lifecycle callback mechanisms. ([Home][7])

### Suitable initialization work

- Validate configuration.
- Build an immutable lookup structure.
- Initialize a client-owned resource.
- Verify required files or directories.
- Precompute lightweight local state.

### Avoid

- Long-running batch processing
- Large database migrations
- Starting unmanaged threads
- Calling remote services without timeouts
- Depending on proxy-based self-invocation
- Hiding business workflows in bean initialization

---

## 18. Other Initialization Mechanisms

### `InitializingBean`

```java
@Component
public class ExampleBean
        implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        System.out.println("Initialized");
    }
}
```

This works but couples the class to Spring.

### Custom initialization method

```java
public class ExternalClient {

    public void connect() {
        // Initialization
    }
}
```

```java
@Configuration
public class ClientConfiguration {

    @Bean(initMethod = "connect")
    public ExternalClient externalClient() {
        return new ExternalClient();
    }
}
```

When several mechanisms are configured on the same bean, the normal initialization order is:

```text
@PostConstruct
→ InitializingBean.afterPropertiesSet()
→ custom init method
```

Spring generally recommends `@PostConstruct` or a custom POJO initialization method over implementing Spring-specific lifecycle interfaces. ([Home][7])

---

## 19. `BeanPostProcessor`

A `BeanPostProcessor` can inspect or modify beans around initialization.

```java
@Component
public class LoggingBeanPostProcessor
        implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(
            Object bean,
            String beanName
    ) {
        System.out.println(
                "Before initialization: " + beanName
        );

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(
            Object bean,
            String beanName
    ) {
        System.out.println(
                "After initialization: " + beanName
        );

        return bean;
    }
}
```

Spring infrastructure uses post-processors for capabilities such as:

- Annotation processing
- Dependency injection
- Lifecycle annotations
- AOP proxy creation
- Transactional proxies
- Async proxies
- Caching proxies
- Security proxies

Custom post-processors are advanced container-extension tools and should not be used for ordinary business logic.

---

## 20. Destruction Lifecycle

When the application context closes, Spring invokes destruction callbacks for eligible managed beans.

```text
@PreDestroy
→ DisposableBean.destroy()
→ custom destroy method
```

Example:

```java
@Component
public class MessageClient {

    private final ExecutorService executor =
            Executors.newFixedThreadPool(4);

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
```

Use destruction callbacks to release resources owned directly by the bean:

- Executor services
- Client connections
- File handles
- Native resources
- Background workers
- Local buffers

Do not manually close a shared dependency owned by another bean unless ownership is explicitly assigned to the current bean.

---

# Bean Scopes

## 21. Common Scopes

| Scope         | Lifecycle                                      |
| ------------- | ---------------------------------------------- |
| `singleton`   | One instance per bean definition per container |
| `prototype`   | New instance each time the bean is requested   |
| `request`     | One instance per HTTP request                  |
| `session`     | One instance per HTTP session                  |
| `application` | One instance per `ServletContext`              |
| `websocket`   | One instance per WebSocket lifecycle           |

The default scope is `singleton`. A Spring singleton is **per bean definition and per IoC container**, not necessarily one object per JVM or class loader. ([Home][8])

---

## 22. Singleton Scope

```java
@Service
public class PricingService {
}
```

All normal injections of that bean definition receive the same managed instance.

Therefore, singleton services should normally be stateless or manage shared state with proper concurrency controls.

Unsafe:

```java
@Service
public class OrderService {

    private Order currentOrder;

    public void process(Order order) {
        currentOrder = order;
    }
}
```

Multiple request threads can overwrite `currentOrder`.

Prefer:

```java
@Service
public class OrderService {

    public void process(Order order) {
        Order validated = validate(order);
        save(validated);
    }
}
```

Method-local state is isolated per invocation.

---

## 23. Prototype Scope

```java
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReportBuilder {
}
```

Each request to the container creates a new instance.

```java
ReportBuilder first =
        context.getBean(ReportBuilder.class);

ReportBuilder second =
        context.getBean(ReportBuilder.class);

System.out.println(first == second); // false
```

Spring creates, configures, and initializes prototype beans, but it does not automatically invoke their configured destruction callbacks. The client that receives a prototype bean must manage its later cleanup. ([Home][8])

---

## 24. Prototype Injected into a Singleton

This does not create a fresh prototype for every method call:

```java
@Service
public class ReportService {

    private final ReportBuilder reportBuilder;

    public ReportService(
            ReportBuilder reportBuilder
    ) {
        this.reportBuilder = reportBuilder;
    }
}
```

The prototype is resolved when the singleton is created, so the singleton keeps that one injected instance.

Use `ObjectProvider` when a fresh instance is required:

```java
@Service
public class ReportService {

    private final ObjectProvider<ReportBuilder> builders;

    public ReportService(
            ObjectProvider<ReportBuilder> builders
    ) {
        this.builders = builders;
    }

    public Report createReport() {
        ReportBuilder builder =
                builders.getObject();

        return builder.build();
    }
}
```

---

# Circular Dependencies

## 25. Constructor Circular Dependency

```text
OrderService
    ↓ needs
PaymentService
    ↓ needs
OrderService
```

Example:

```java
@Service
public class OrderService {

    public OrderService(
            PaymentService paymentService
    ) {
    }
}
```

```java
@Service
public class PaymentService {

    public PaymentService(
            OrderService orderService
    ) {
    }
}
```

Neither bean can be constructed first, so the container cannot satisfy the cycle.

### Correct response

Do not immediately switch to field injection or add `@Lazy` merely to hide the cycle.

The cycle often indicates:

- Responsibilities are incorrectly divided.
- Two services know too much about each other.
- A missing domain service or coordinator is needed.
- One dependency should become an event.
- Shared logic should move into a third component.

Refactor:

```text
OrderService ───────┐
                    ↓
             CheckoutCoordinator
                    ↑
PaymentService ─────┘
```

---

## 26. Why Setter Injection Is Not a Good Circular-Dependency Fix

Some singleton property-injection cycles can be resolved through early bean references, but this can expose partially initialized objects and interact badly with proxy creation.

A technically resolvable cycle is still usually a design smell.

Preferred solutions include:

- Extracting a third service
- Reversing a dependency
- Publishing an event
- Depending on a smaller interface
- Moving orchestration to an application service

---

# AOP and Proxies

## 27. How Spring Implements AOP

Spring AOP commonly creates a proxy around a bean.

```text
Caller
   ↓
Spring proxy
├── transaction advice
├── security advice
├── cache advice
└── target method
       ↓
Actual bean
```

Depending on configuration and the target type, Spring can use:

- JDK dynamic proxies
- Class-based proxies

Proxy-based features include:

- `@Transactional`
- `@Async`
- `@Cacheable`
- Method security
- Custom aspects

Spring’s proxy documentation explains that calls entering through the proxy can be advised, while calls made directly on `this` bypass that proxy. ([Home][9])

---

## 28. Why Self-Invocation Bypasses `@Transactional`

```java
@Service
public class PaymentService {

    public void process() {
        savePayment();
    }

    @Transactional
    public void savePayment() {
        // Database operations
    }
}
```

The call:

```java
savePayment();
```

is effectively:

```java
this.savePayment();
```

It does not pass through the Spring proxy.

```text
External caller
→ proxy
→ process()
→ this.savePayment()
→ target method directly
```

Therefore, the transaction advice on `savePayment()` is not newly applied through that internal call.

Spring’s default transaction mode is proxy-based, so only calls that enter through the proxy are intercepted. Spring also advises against relying on transactional proxy behavior from initialization code such as `@PostConstruct`. ([Home][10])

### Better design

Move the transactional method to another bean:

```java
@Service
public class PaymentWriter {

    @Transactional
    public void savePayment() {
        // Database operations
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

    public void process() {
        paymentWriter.savePayment();
    }
}
```

Now the call enters the `PaymentWriter` proxy.

---

# Production Example

## 29. Complete Bean with Lifecycle Management

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

    private final PaymentProperties properties;
    private ExternalPaymentConnection connection;

    public PaymentClient(
            PaymentProperties properties
    ) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        validateConfiguration();

        connection =
                new ExternalPaymentConnection(
                        properties.baseUrl(),
                        properties.timeout()
                );

        connection.connect();
    }

    public PaymentResult charge(
            PaymentRequest request
    ) {
        if (connection == null) {
            throw new IllegalStateException(
                    "Payment connection is not initialized"
            );
        }

        return connection.charge(request);
    }

    @PreDestroy
    void shutdown() {
        if (connection != null) {
            connection.close();
        }
    }

    private void validateConfiguration() {
        if (properties.baseUrl() == null
                || properties.baseUrl().isBlank()) {

            throw new IllegalStateException(
                    "Payment base URL is required"
            );
        }
    }
}
```

This bean has clear ownership:

```text
PaymentClient creates connection
→ PaymentClient uses connection
→ PaymentClient closes connection
```

---

# Common Mistakes

## 30. Creating Spring Components with `new`

```java
OrderService service =
        new OrderService();
```

This instance is not managed by the Spring container. Therefore, it does not automatically receive:

- Injected dependencies
- Lifecycle callbacks
- Transaction proxies
- Async proxies
- Cache proxies
- Security advice

Use constructor injection from another managed component instead.

---

## 31. Putting Request State in Singleton Beans

```java
@Service
public class UserService {

    private String currentUser;
}
```

Singleton beans are shared across request threads. Mutable request-specific data should normally remain:

- In method-local variables
- In request-scoped objects
- In explicitly passed context
- In properly managed framework security context

---

## 32. Performing Heavy Work in `@PostConstruct`

Heavy startup work can:

- Delay readiness
- Fail application startup
- Hold the context-refresh thread
- Trigger remote dependencies before the service is ready
- Complicate retries and observability

Use explicit startup components, migrations, scheduled jobs, or asynchronous workflows when the task is substantial.

---

## 33. Assuming a Spring Singleton Is Globally Unique

A singleton is not necessarily unique across:

- Multiple application contexts
- Multiple service instances
- Multiple JVMs
- Multiple Kubernetes pods

```text
Pod A → its singleton bean
Pod B → its singleton bean
Pod C → its singleton bean
```

A Spring singleton cannot enforce distributed uniqueness.

---

## 34. Calling Proxy-Based Methods in `@PostConstruct`

Annotations such as `@Transactional` or `@Async` may not behave as expected when invoked through `this` during initialization.

Move such operations into:

- Another managed bean
- An application-ready event handler
- A startup runner
- An explicit workflow after context initialization

---

## 35. Using Field Injection Everywhere

Field injection can make a class appear simple while hiding excessive coupling.

A constructor with ten parameters is uncomfortable because it exposes a genuine design problem:

```text
Too many responsibilities
→ too many dependencies
→ class should probably be split
```

---

# Constructor vs Field Injection

| Constructor injection                               | Field injection                          |
| --------------------------------------------------- | ---------------------------------------- |
| Dependencies are explicit                           | Dependencies are hidden                  |
| Supports `final` references                         | Fields are normally mutable              |
| Easy direct unit testing                            | Often requires reflection or Spring      |
| Fails clearly for constructor cycles                | Can conceal poor dependency design       |
| Prevents construction without required dependencies | Can create partially initialized objects |
| Preferred for mandatory dependencies                | Convenient but less maintainable         |

---

# Interview Questions

## Question 1: What is IoC?

> IoC means the Spring container controls the creation, configuration, wiring, and lifecycle of managed objects instead of application classes creating all collaborators directly.

## Question 2: What is dependency injection?

> Dependency injection is the mechanism through which dependencies are supplied to an object from outside. In Spring, the container resolves matching beans and injects them through constructors, setters, fields, or methods.

## Question 3: Why is constructor injection preferred?

> Constructor injection makes mandatory dependencies explicit, allows references to be final, makes direct unit testing easy, prevents creating an object without required collaborators, and exposes circular dependencies immediately.

## Question 4: Does `final` make an injected dependency immutable?

> No. It prevents the field reference from being reassigned. The referenced object may still be mutable.

## Question 5: What is a Spring bean?

> A Spring bean is an object whose construction, dependency wiring, scope, lifecycle callbacks, and optional proxying are managed by the Spring container.

## Question 6: What is the difference between `@Component` and `@Bean`?

> `@Component` marks a class for component scanning. `@Bean` marks a factory method whose returned object is registered in the container. I commonly use `@Component` for application-owned classes and `@Bean` for third-party classes or customized construction.

## Question 7: What is the default Spring bean scope?

> The default is singleton, meaning one instance per bean definition per Spring IoC container—not one instance per JVM.

## Question 8: Walk through the bean lifecycle

> Spring registers the bean definition, creates the instance, injects dependencies, invokes aware and pre-initialization callbacks, runs `@PostConstruct`, `afterPropertiesSet`, and any custom initialization method, then applies post-initialization processors that may create a proxy. During container shutdown, it invokes `@PreDestroy`, `destroy`, and custom destruction callbacks.

## Question 9: What is a `BeanPostProcessor`?

> It is a container extension point that can inspect, modify, or replace bean instances before and after initialization. Spring uses post-processors internally for annotation handling and AOP proxy creation.

## Question 10: Why are constructor circular dependencies a problem?

> Neither object can be created first because each constructor requires the other. The cycle usually indicates misplaced responsibilities and should be removed by redesigning ownership or introducing a coordinator or event boundary.

## Question 11: Why can self-invocation bypass `@Transactional`?

> Spring’s default transaction support is proxy-based. A method call made through `this` stays inside the target object and does not pass through the proxy, so the transaction interceptor does not get another opportunity to apply the annotation.

## Question 12: Is a singleton bean automatically thread-safe?

> No. Singleton describes instance count, not synchronization. A mutable singleton accessed by several threads must still be designed for concurrency.

## Question 13: Are destruction callbacks invoked for prototype beans?

> Spring initializes prototype beans but does not manage their complete destruction lifecycle. The code receiving the prototype is responsible for cleanup.

---

# Short Interview Answer

> IoC means Spring controls the creation, wiring, configuration, scope, and lifecycle of managed objects. Dependency injection is how Spring supplies those objects with their collaborators. I prefer constructor injection because mandatory dependencies are explicit, references can be final, direct unit testing is simple, and circular dependencies fail clearly. During bean creation, Spring instantiates the object, injects dependencies, applies lifecycle callbacks and bean post-processors, and may return an AOP proxy. I also remember that Spring singletons are per bean definition and container, are not automatically thread-safe, and proxy-based features such as `@Transactional` can be bypassed by self-invocation.

## Related Topics

- [Spring Boot Auto-Configuration](spring-boot.md)
- [Spring AOP and Proxies](spring-aop.md)
- [Spring Transactions](spring-transactions.md)
- [Bean Scopes](bean-scopes.md)
- [Dependency Injection Design Pattern](../07-design-patterns/dependency-injection.md)

[1]: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html?utm_source=chatgpt.com "Dependency Injection :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com "Using @Autowired :: Spring Framework"
[3]: https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-primary.html?utm_source=chatgpt.com "Fine-tuning Annotation-based Autowiring with @Primary or ..."
[4]: https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html?utm_source=chatgpt.com "Classpath Scanning and Managed Components"
[5]: https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com "Using the @Bean Annotation"
[6]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/config/BeanPostProcessor.html?utm_source=chatgpt.com "BeanPostProcessor (Spring Framework 7.0.7 API)"
[7]: https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html?utm_source=chatgpt.com "Customizing the Nature of a Bean"
[8]: https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html "Bean Scopes :: Spring Framework"
[9]: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html?utm_source=chatgpt.com "Proxying Mechanisms"
[10]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com "Using @Transactional"
