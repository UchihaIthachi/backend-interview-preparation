# Spring Core: IoC, Dependency Injection, Bean Lifecycle

## 1. Definition

Inversion of Control (IoC) means the framework, not your code, controls object
creation and wiring. Dependency Injection (DI) is the specific mechanism Spring
uses to implement IoC — dependencies are provided ("injected") rather than
constructed by the class that needs them.

## 2. Why it exists

Manually wiring dependencies (`new PaymentService(new PaymentGateway())`)
couples classes tightly and makes testing hard. DI lets you inject a mock
`PaymentGateway` in tests and a real one in production without changing
`PaymentService` at all.

## 3. How it works — constructor injection preferred

```java
@Service
class OrderService {
    private final PaymentGateway paymentGateway; // final -> immutable, forces injection

    // Constructor injection: Spring calls this automatically if there's only
    // one constructor (no @Autowired needed since Spring 4.3+)
    OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
}
```

Why constructor injection over field injection (`@Autowired private ...`):
- Dependencies can be `final` — guaranteed immutable, no partially-constructed state.
- Makes circular dependencies fail fast at startup instead of silently working
  around them.
- Trivial to unit test without a Spring context — just call `new OrderService(mockGateway)`.

## 4. Code example — bean lifecycle

```text
1. Bean definitions loaded (from @Component scan or @Bean methods)
2. Constructor called (dependencies injected)
3. Setter/field injection applied (if used)
4. @PostConstruct method called
5. Bean is ready for use
6. ... application runs ...
7. @PreDestroy method called on shutdown
```

## 5. Production use case

Every Spring service, repository, and controller relies on this lifecycle.
Understanding it is essential for debugging circular dependency errors at
startup and for correctly using `@PostConstruct` to run initialization logic
that needs injected dependencies already set.

## 6. Common mistakes

- Using field injection (`@Autowired` on a field) — works, but harder to test
  and hides mandatory dependencies.
- Creating circular dependencies between beans (Spring can sometimes resolve
  these for setter injection but not constructor injection — a sign of a
  design problem, not something to work around).
- Assuming all beans are singletons by default without knowing you can
  override scope with `@Scope("prototype")` etc.

## 7. Trade-offs

| Constructor injection | Field injection |
|---|---|
| Testable without Spring context | Requires Spring context or reflection to test |
| Dependencies can be `final` | Dependencies are mutable |
| Fails fast on circular deps | Can silently "work" with circular deps (bad sign) |

## 8. Interview questions

1. Why is constructor injection preferred over field injection?
2. Walk through what happens when Spring creates a bean.
3. What's the difference between `@Component` and `@Bean`?
4. How does Spring implement AOP (proxies)?
5. Why does self-invocation sometimes bypass `@Transactional`?

## 9. Short interview answer

"IoC means Spring controls object creation and wiring instead of my code doing
it manually. I prefer constructor injection because dependencies can be final
and immutable, it's trivially testable without spinning up a Spring context,
and circular dependencies fail fast at startup instead of being silently
worked around."

## 10. Related topics

- [Spring Boot auto-configuration](spring-boot.md)
- [Design patterns — Dependency Injection](../07-design-patterns/patterns.md)
