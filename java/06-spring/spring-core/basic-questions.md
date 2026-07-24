# Basic Questions — Spring Dependency Injection

## Question 1: What is dependency injection?

**Dependency Injection**, or **DI**, is a design technique in which an object receives its required dependencies from outside instead of creating them itself.

Without dependency injection:

```java
public class OrderService {

    private final PaymentGateway paymentGateway =
            new StripePaymentGateway();
}
```

`OrderService` is directly coupled to `StripePaymentGateway`.

With dependency injection:

```java
@Service
public class OrderService {

    private final PaymentGateway paymentGateway;

    public OrderService(
            PaymentGateway paymentGateway
    ) {
        this.paymentGateway = paymentGateway;
    }
}
```

Spring creates the appropriate `PaymentGateway` bean and supplies it to `OrderService`.

### Benefits

- Reduces tight coupling
- Makes unit testing easier
- Allows implementations to be replaced
- Makes dependencies explicit
- Centralizes object creation and configuration
- Supports cleaner separation of responsibilities

### Unit-test example

```java
PaymentGateway mockGateway =
        mock(PaymentGateway.class);

OrderService orderService =
        new OrderService(mockGateway);
```

The test can provide a mock without starting the Spring container.

### Interview-ready answer

> Dependency injection means that a class receives its dependencies from an external source instead of constructing them itself. In Spring, the IoC container creates beans, resolves their dependencies, and injects them through constructors, setters, fields, or methods.

---

## Question 2: What is the difference between constructor injection and setter injection?

### Constructor injection

Dependencies are supplied when the object is created.

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

Constructor injection is best for **required dependencies**.

### Advantages

- Required dependencies are explicit.
- Fields can be `final`.
- The object cannot be created without its dependencies.
- Easy to test without Spring.
- Circular dependencies fail clearly.
- Prevents partially initialized objects.

---

### Setter injection

Dependencies are supplied after the object has been constructed.

```java
@Service
public class NotificationService {

    private AuditService auditService;

    @Autowired
    public void setAuditService(
            AuditService auditService
    ) {
        this.auditService = auditService;
    }
}
```

Setter injection is generally more appropriate for **optional dependencies** or values that genuinely need to be reconfigured.

### Disadvantages

- The object can temporarily exist without the dependency.
- Fields cannot normally be `final`.
- Mandatory dependencies are less obvious.
- The dependency can potentially be replaced later.

---

### Comparison

| Constructor injection                   | Setter injection                    |
| --------------------------------------- | ----------------------------------- |
| Best for required dependencies          | Better for optional dependencies    |
| Supports `final` fields                 | Usually uses mutable fields         |
| Object is fully initialized immediately | Object may be partially initialized |
| Easy to unit test                       | Requires setter calls during tests  |
| Constructor cycles fail fast            | Some cycles may be hidden           |
| Preferred in most application code      | Used selectively                    |

### Interview-ready answer

> Constructor injection supplies required dependencies when the object is created, allowing final fields and guaranteeing a fully initialized object. Setter injection supplies dependencies after construction and is more appropriate for optional dependencies. I normally prefer constructor injection.

---

## Question 3: What happens if you do not use `@Autowired` in Spring?

It depends on the number of constructors.

### Case 1: The class has exactly one constructor

Spring automatically uses it for dependency injection. `@Autowired` is unnecessary.

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

This works without:

```java
@Autowired
```

---

### Case 2: The class has multiple constructors

Spring may not know which constructor to use.

```java
@Service
public class OrderService {

    public OrderService(
            OrderRepository repository
    ) {
    }

    public OrderService(
            OrderRepository repository,
            AuditService auditService
    ) {
    }
}
```

In this case, identify the intended injection constructor:

```java
@Autowired
public OrderService(
        OrderRepository repository,
        AuditService auditService
) {
}
```

Alternatively, redesign the class so that it has one clear constructor for required dependencies.

---

### Case 3: The class has a no-argument constructor

Spring can create the object using the no-argument constructor, but dependencies will not be injected unless another injection mechanism is configured.

```java
@Service
public class OrderService {

    private OrderRepository orderRepository;

    public OrderService() {
    }
}
```

Here, `orderRepository` remains `null` unless it is injected through a field, setter, or another mechanism.

### Interview-ready answer

> If a Spring bean has exactly one constructor, Spring uses it automatically, so `@Autowired` is not required. If several constructors exist, Spring may need `@Autowired` or another clear constructor-selection rule. Without a valid injection point, required dependencies may not be supplied or bean creation may fail.

---

## Question 4: How do you perform constructor injection without `@Autowired`?

There are two requirements:

1. The class must be registered as a Spring bean.
2. It should have one constructor accepting its required dependencies.

```java
public interface PaymentGateway {

    PaymentResult charge(
            PaymentRequest request
    );
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

Spring performs these steps:

```text
1. Detect PaymentService as a bean.
2. Inspect its constructor.
3. Find a PaymentGateway bean.
4. Create or retrieve that bean.
5. Pass it to the PaymentService constructor.
6. Register the completed PaymentService bean.
```

The same approach works with beans declared using `@Bean`:

```java
@Configuration
public class PaymentConfiguration {

    @Bean
    public PaymentService paymentService(
            PaymentGateway paymentGateway
    ) {
        return new PaymentService(
                paymentGateway
        );
    }
}
```

### Interview-ready answer

> To perform constructor injection without `@Autowired`, I declare the class as a Spring bean and provide a single constructor containing its required dependencies. Spring detects that constructor, resolves matching beans by type, and passes them when creating the object.

---

# Common Mistakes

## Using `new` for Spring-managed services

```java
OrderService service =
        new OrderService();
```

This object is not managed by Spring and will not automatically receive:

- Injected dependencies
- Transactional proxies
- Caching proxies
- Security advice
- Lifecycle callbacks

---

## Using field injection for every dependency

```java
@Autowired
private PaymentGateway paymentGateway;
```

Although it works, field injection:

- Hides dependencies
- Prevents final fields
- Makes direct unit testing harder
- Allows partially initialized objects

---

## Treating `final` as full immutability

```java
private final PaymentGateway paymentGateway;
```

`final` prevents the reference from being reassigned. It does not guarantee that the `PaymentGateway` object itself is immutable.

---

## Hiding circular dependencies with setter injection

```text
OrderService → PaymentService
PaymentService → OrderService
```

A circular dependency usually indicates a design problem. Consider introducing:

- A coordinator service
- An event
- A smaller interface
- A third component containing shared logic

---

# Quick Interview Summary

> Dependency injection means that Spring supplies a bean with its required collaborators instead of the bean creating them itself. Constructor injection is preferred for mandatory dependencies because it makes them explicit, supports final references, enables simple unit testing, and guarantees complete initialization. When a bean has exactly one constructor, Spring automatically uses it, so `@Autowired` is not required.
