# Design Patterns Used in Backend Systems

## 1. Definition

Reusable solutions to common design problems. In backend Java, a small set of
patterns account for most real usage: Singleton, Factory, Builder, Strategy,
Template Method, Observer, Decorator, Proxy, Repository, and Dependency
Injection.

## 2. Why it exists

Patterns give a shared vocabulary for design decisions ("just use Strategy
here") and encode trade-offs that have already been worked out by the
community, instead of reinventing a solution each time.

## 3. How it works — the patterns that show up most in interviews

```java
// Strategy: interchangeable algorithms behind one interface
interface PaymentStrategy { void pay(BigDecimal amount); }
class CreditCardPayment implements PaymentStrategy { /* ... */ }
class PaypalPayment implements PaymentStrategy { /* ... */ }

// Builder: construct complex objects step by step
Order order = Order.builder()
    .customerId(id)
    .addLineItem(item)
    .build();

// Proxy: Spring's @Transactional, @Cacheable etc. all work via dynamic proxies
// wrapping your bean and adding behavior before/after the real method call.

// Repository: abstracts persistence behind a domain-focused interface
interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
}
```

## 4. Code example — Decorator vs Proxy distinction

Decorator adds *new behavior/responsibility* (e.g., wrapping a stream with
buffering). Proxy controls *access* to an object (e.g., lazy loading, security
checks, transaction boundaries) while preserving the same interface — this is
exactly how Spring AOP implements `@Transactional`.

## 5. Production use case

Nearly every Spring feature is a proxy under the hood: `@Transactional`,
`@Cacheable`, `@Async` all work by Spring wrapping your bean in a dynamic proxy
that intercepts method calls to add behavior — which is also *why*
self-invocation (calling another `@Transactional` method from within the same
class) bypasses the proxy and the annotation has no effect.

## 6. Common mistakes

- Applying Singleton via a static field manually in a Spring app — Spring
  beans are singletons by default already; manual singletons fight the
  framework and complicate testing.
- Confusing Decorator with Proxy in interviews — both wrap an object with the
  same interface, but Decorator is about adding behavior, Proxy is about
  controlling access.
- Not recognizing that `@Transactional` failing on self-invocation is a direct
  consequence of the proxy pattern, not a "bug."

## 7. Trade-offs

Patterns add a layer of indirection — worth it when the flexibility (swapping
implementations, adding cross-cutting behavior) is actually needed; overkill
for a single, never-changing implementation.

## 8. Interview questions

1. Strategy vs Template Method — what's the difference?
2. Decorator vs Proxy — what's the difference?
3. How does Spring implement AOP, concretely?
4. Why does `@Transactional` fail on self-invocation?
5. Why is manually implementing Singleton usually unnecessary in a Spring app?

## 9. Short interview answer

"Spring's cross-cutting features like @Transactional and @Cacheable are all
implemented as dynamic proxies wrapping your bean — which is exactly why
calling another @Transactional method on `this` from inside the same class
bypasses the proxy and the annotation silently does nothing. Recognizing that
connects the proxy pattern directly to a very real, very common production bug."

## 10. Related topics

- [Spring Core / DI](../06-spring/spring-core.md)
