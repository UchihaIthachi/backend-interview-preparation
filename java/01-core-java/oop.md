# OOP in Java: Encapsulation, Inheritance, Polymorphism, Abstraction

## 1. Definition

- **Encapsulation** — hiding internal state behind a controlled interface.
- **Inheritance** — a class acquiring fields/methods from a superclass.
- **Polymorphism** — the same method call behaving differently depending on the
  runtime type of the object.
- **Abstraction** — exposing *what* an object does without exposing *how*.

## 2. Why it exists

These principles let large codebases stay maintainable: encapsulation limits the
blast radius of changes, inheritance/polymorphism enable code reuse and flexible
extension points, abstraction lets callers depend on a stable contract rather than
a concrete implementation.

## 3. How it works — interface vs abstract class

```java
interface PaymentStrategy {
    void pay(BigDecimal amount);
}

abstract class BaseNotifier {
    void send(String message) {
        log(message);
        doSend(message);
    }
    protected abstract void doSend(String message);
    private void log(String message) { System.out.println("Sending: " + message); }
}
```

Use an **interface** when unrelated classes need to share a contract with no shared
state (`PaymentStrategy` implemented by `CreditCardPayment`, `PaypalPayment`).

Use an **abstract class** when related classes share both a contract *and* some
common implementation (`BaseNotifier` shares the `send()` template but each
subclass implements `doSend()` differently).

## 4. Code example — composition over inheritance

```java
// Inheritance-heavy (fragile): OrderProcessor extends Logger extends Validator ...
// Prefer composition:
class OrderProcessor {
    private final Validator validator;
    private final Logger logger;

    OrderProcessor(Validator validator, Logger logger) {
        this.validator = validator;
        this.logger = logger;
    }

    void process(Order order) {
        validator.validate(order);
        logger.log("Processing order " + order.getId());
    }
}
```

## 5. Production use case

Strategy + composition is the backbone of most Spring service layers: a
`PaymentService` depends on a `PaymentStrategy` interface, and Spring injects the
concrete implementation — this is why constructor injection and interfaces show up
constantly in backend interviews.

## 6. Common mistakes

- Deep inheritance chains (3+ levels) that are hard to reason about.
- Using inheritance purely for code reuse when composition would be more flexible.
- Overloading vs overriding confusion — overloading is compile-time (same name,
  different params), overriding is runtime (subclass replaces superclass behavior).

## 7. Trade-offs

| Interface | Abstract class |
|---|---|
| Multiple inheritance of type | Single inheritance only |
| No shared state | Can hold shared fields/state |
| Pure contract | Contract + partial implementation |

## 8. Interview questions

1. Interface vs abstract class — when do you choose each?
2. Overloading vs overriding — what's the difference?
3. Why is composition often preferred over inheritance?
4. What is polymorphism, concretely, at the bytecode/dispatch level?

## 9. Short interview answer

"I reach for an interface when I need a pure contract shared by unrelated types,
and an abstract class when subclasses share both a contract and common
implementation. In practice I lean toward composition over deep inheritance
because it keeps classes independently testable and avoids fragile base-class
problems."

## 10. Related topics

- [Design patterns](../07-design-patterns/patterns.md)
- [Spring dependency injection](../06-spring/spring-core.md)
