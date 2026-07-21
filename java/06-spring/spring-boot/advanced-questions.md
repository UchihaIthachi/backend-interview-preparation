# Advanced Questions

## Q11: Explain auto-configuration in Spring Boot. How does @ConditionalOnClass work internally?
spring.factories / META-INF/spring/auto-configuration, class-path scanning, bean conditions.

## Q12: What is the difference between @SpringBootApplication and manually combining its constituent annotations?
@ComponentScan + @EnableAutoConfiguration + @Configuration — implications of each.

## Q19: How does Spring Boot Actuator expose health checks? How would you write a custom HealthIndicator?
Implement HealthIndicator, return Health.up/down, expose via management.endpoints.

## Q81: How do you implement caching in Spring Boot?
Add spring-boot-starter-data-redis and spring-boot-starter-cache. Configure Redis host/port. Spring Boot auto-configures RedisCacheManager. Use @EnableCaching, @Cacheable, @CacheEvict, @CachePut.

## Q82: How do you integrate Redis cache with Spring Boot?
Configure spring.cache.type=redis and spring.redis.* properties. Provide a custom RedisCacheConfiguration bean if custom TTL or serialization is needed.

## Q83: What is @Async in Spring Boot? How do you enable it and use it?
Makes a method run in a separate thread asynchronously. Enable with @EnableAsync. Returns void or Future/CompletableFuture.

## Q84: What is @Scheduled in Spring Boot? How do you configure scheduled tasks?
Enables scheduling methods to run at fixed intervals or cron expressions. Enable with @EnableScheduling. Use fixedRate, fixedDelay, or cron.

## Q85: How do you improve Spring Boot application performance? List key strategies.
Use @Async, enable caching, use LAZY loading in JPA, use pagination, use connection pooling (HikariCP), enable GZip, use WebFlux for I/O bound tasks, optimize DB queries, lazy initialization, profile with Actuator.

## Q86: What is HikariCP and how do you configure it in Spring Boot?
Default connection pool in Spring Boot (fastest and most reliable). Configure properties like maximum-pool-size, minimum-idle, connection-timeout.

## Q87: What is Spring WebFlux? When would you use it over Spring MVC?
Reactive, non-blocking web framework built on Project Reactor (Mono/Flux). Uses Netty. Best for high concurrency I/O bound operations.

## Q88: How would you design a rate limiter for a REST API in Spring Boot?
Spring Cloud Gateway with Redis Rate Limiter, Bucket4j library, or a custom filter using Redis counters.

## Q89: How do you handle transactions across multiple microservices?
Avoid 2PC. Use Saga Pattern (choreography/orchestration) or Outbox Pattern with eventual consistency.

## Q90: How would you implement idempotency in a REST API?
Client sends unique Idempotency-Key. Server stores (key -> response) in Redis. Return cached response on duplicate requests.

## Q91: What are the key differences between Spring Boot 2.x and Spring Boot 3.x?
Java 17 baseline, Jakarta EE 9+ (jakarta.* namespace), Native image support via GraalVM, Micrometer Tracing (replaces Sleuth).

## Q92: How do you containerize a Spring Boot application with Docker?
Use a Dockerfile (multi-stage preferred) or Spring Boot Maven/Gradle plugin (buildpacks) with `spring-boot:build-image`.

## Q93: What is GraalVM Native Image support in Spring Boot 3? What are the limitations?
Compiles to native image using Spring AOT for fast startup and low memory footprint. Limitations: slower builds, no dynamic class loading, requires reflection hints.

## Q53: What is Spring Boot Actuator? What endpoints does it expose?
Actuator provides production-ready monitoring and management endpoints. Key endpoints: /health, /info, /metrics, /env, /beans, /mappings, /loggers, /threaddump, /heapdump.

## Q54: How do you create a custom Health Indicator in Spring Boot Actuator?
Implement the HealthIndicator interface and override the health() method. Spring Boot auto-detects and includes it in /actuator/health.

## Q55: How do you create custom metrics with Micrometer in Spring Boot?
Inject MeterRegistry and create counters, gauges, timers (e.g., `Counter.builder(...).register(registry)`).

## Q56: How do you integrate Spring Boot Actuator with Prometheus and Grafana?
Add micrometer-registry-prometheus. Expose /actuator/prometheus endpoint. Configure Prometheus to scrape it and connect Grafana.

## Q57: What is the difference between /health and /info endpoints?
/health reports application health status (UP/DOWN). /info provides arbitrary application info like version, build details, git commit.

## Q58: How do you use Spring Boot Actuator for Kubernetes liveness and readiness probes?
Spring Boot auto-configures liveness (/actuator/health/liveness) and readiness (/actuator/health/readiness) probes. Enable with management.health.probes.enabled=true.

## Q59: What is Spring Boot Admin? How is it different from Actuator?
Spring Boot Admin provides a web UI on top of Actuator JSON endpoints to aggregate and visualize data from multiple Spring Boot services.

## Q60: What testing libraries does Spring Boot provide out of the box?
spring-boot-starter-test includes: JUnit 5, Mockito, AssertJ, Hamcrest, Spring Test, JSONassert, JsonPath.

## Q61: What is the difference between @SpringBootTest, @WebMvcTest, @DataJpaTest, and @JsonTest?
@SpringBootTest loads full app context (integration). @WebMvcTest loads only web layer (controllers). @DataJpaTest loads only JPA layer with in-memory DB. @JsonTest tests JSON serialization.

## Q62: What is MockMvc and how do you use it?
Simulates HTTP requests to Spring MVC controllers without a real HTTP server to test the web layer in isolation using MockMvcRequestBuilders.

## Q63: What is the difference between @Mock and @MockBean?
@Mock creates a pure Mockito mock (no Spring context). @MockBean creates a Mockito mock AND registers it in the Spring ApplicationContext, replacing any existing bean.

## Q64: What is @DataJpaTest? What is its default configuration?
Loads only JPA components. Defaults to in-memory H2 database, enables SQL logging, wraps each test in a rolled-back transaction.

## Q65: What is Testcontainers and how do you use it in Spring Boot?
Provides throwaway Docker containers for integration testing (e.g., real MySQL instead of H2). Used with @ServiceConnection in Spring Boot 3.1+.

## Q66: How do you test Spring Boot REST APIs with RestAssured?
Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` and configure RestAssured with the injected `@LocalServerPort`.

## Q67: What is the use of @TestConfiguration and how is it different from @Configuration?
Defines test-specific beans. Unlike @Configuration, it is NOT auto-detected and must be explicitly imported using @Import.

## Q68: What is @Spy in Mockito? When would you use it over @Mock?
@Spy wraps a real object where real methods are called by default but can be stubbed. Use it to test real logic while stubbing a few specific methods.

## Q13: How does Spring Boot Auto-Configuration work internally?
Reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and conditionally registers beans using `@Conditional` annotations based on classpath and properties.

## Q14: What are Spring Boot Starter POMs? Name some commonly used starters.
Curated dependency descriptors eliminating version conflicts (e.g., `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-test`).

## Q15: How do you create a custom Spring Boot Auto-Configuration?
Create a `@Configuration` class with `@Conditional` annotations, and add its fully qualified name to `AutoConfiguration.imports`.

## Q16: What is @ConditionalOnProperty? Give a real use case.
Registers a bean only if a specific property is set (e.g., enable/disable a feature flag or mock service in non-prod).

## Q17: What is the use of spring.factories? Is it still used in Spring Boot 3.x?
Used in 2.x for auto-config registration. In 3.x, auto-config moved to `AutoConfiguration.imports`, but `spring.factories` is still used for listeners and initializers.

## Q18: What is the difference between @Import and @ImportResource?
`@Import` imports `@Configuration` classes programmatically. `@ImportResource` imports XML configuration files into Java config.

## Q20: What is Banner in Spring Boot? How do you customize or disable it?
ASCII art printed on startup. Customize with `banner.txt` in resources, or disable via `spring.main.banner-mode=off`.

## Q1: What is Spring Boot and how is it different from the Spring Framework?
Opinionated extension simplifying Spring app development via auto-configuration and embedded servers, minimizing boilerplate compared to bare Spring Framework.

## Q2: What does @SpringBootApplication do internally?
Combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`.

## Q3: Explain the Spring Boot startup process step by step.
`SpringApplication.run()` -> create environment -> load initializers -> prepare context -> execute auto-config -> refresh context -> start embedded server.

## Q4: What is the difference between @Component, @Service, @Repository, and @Controller?
All are `@Component`s. `@Service` marks business logic, `@Repository` adds persistence exception translation, `@Controller` handles HTTP requests.

## Q5: What is Spring IoC and Dependency Injection? What types of DI does Spring support?
Inversion of Control (container manages objects). Types: Constructor (preferred), Setter, Field.

## Q6: What is a Spring Bean? What are the different Bean scopes?
Object managed by IoC container. Scopes: Singleton, Prototype, Request, Session, Application, WebSocket.

## Q7: What is the Bean lifecycle in Spring Boot?
Instantiation -> Populate Properties -> Aware Callbacks -> PostProcessBeforeInit -> PostConstruct -> PostProcessAfterInit -> Ready -> PreDestroy.

## Q8: What is Spring AOP? Explain key AOP terminologies.
Separates cross-cutting concerns (logging, security). Terms: Aspect, Join Point, Pointcut, Advice, Weaving.

## Q9: What are Profiles in Spring Boot and how do you use them?
Group configurations for different environments (dev, prod). Active with `spring.profiles.active`.

## Q10: What is application.properties vs application.yml? Which is preferred?
Both externalize config. `.properties` is flat, `.yml` supports hierarchical structure and is often preferred for readability.


𝗦𝗽𝗿𝗶𝗻𝗴 𝗕𝗼𝗼𝘁 𝗮𝗻𝗱 𝗠𝗶𝗰𝗿𝗼𝘀𝗲𝗿𝘃𝗶𝗰𝗲𝘀
 - How does @Transactional work internally? Where does it silently fail?
 - What happens inside Spring Boot during startup?
 - What is the difference between @Component, @Service and @Repository?
 - How does Dependency Injection work internally in Spring?
 - RestTemplate vs WebClient when do you choose which?
 - How do you implement Global Exception Handling in Spring Boot?
 - What is the difference between monolithic and microservices architecture?
 - How do microservices communicate? When REST, when Kafka?

Spring Boot
9. @Transactional on a private method. Why does it silently fail?
10. Two beans of same type in Spring context. How does Spring decide which to inject?
11. Your @Async method throws an exception. Nobody catches it. What happens?
12. Spring Boot startup taking 45 seconds. How do you diagnose and fix it?

𝗦𝗽𝗿𝗶𝗻𝗴 & 𝗦𝗽𝗿𝗶𝗻𝗴 𝗕𝗼𝗼𝘁
 - Difference between @Bean and @Component.
 - What is AOP (Aspect-Oriented Programming)? Real-world use case?
 - How does Spring handle circular dependencies?
 - How to secure REST APIs (JWT, OAuth2)?
 - How does Spring Boot auto-configuration work?
 - How does Spring Boot manage externalized configuration (application .properties vs application .yml)?
 - What is the difference between ApplicationContext and BeanFactory?
 - Scenario Based: You are designing an order management system where inventory update, payment deduction, and invoice generation must happen as independent services. How would you use Spring events/AOP to achieve this?

Spring Boot and Microservices
1. How does @Transactional work internally? Where does it silently fail
2. RestTemplate vs WebClient when do you choose which?
3. How do microservices communicate? When REST, when Kafka?
4. What is API Gateway and why is it required?
5. How do you implement Global Exception Handling in Spring Boot?