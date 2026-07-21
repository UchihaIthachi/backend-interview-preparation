# Basic Questions

## Q1: What is Spring Boot, and why is it used?
Spring Boot is an extension of the Spring framework designed to simplify building production-ready Java applications. It offers auto-configuration, embedded servers, and “starter” dependencies to reduce setup time and boilerplate code. It's popular for rapid development, microservices, and out-of-the-box production readiness.

## Q2: What are the key features of Spring Boot?
Auto-Configuration, Starters, Embedded Servers (Tomcat, Jetty), Spring Boot Actuator, Spring Boot CLI, and Spring Initializr.

## Q3: What is the @SpringBootApplication annotation?
It combines three key annotations: @EnableAutoConfiguration, @ComponentScan, and @Configuration. It's the entry point for most projects.

## Q4: What are Spring Boot Starters?
Starters are dependency descriptors that bundle related libraries for specific functionality (e.g., `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`). They simplify dependency management.

## Q5: What is Spring Boot Auto-Configuration?
It's Spring Boot’s ability to automatically configure beans based on the dependencies in your classpath (e.g., setting up a DataSource if `spring-boot-starter-data-jpa` and a DB driver are present).

## Q6: How does Spring Boot differ from Spring Framework?
Spring Boot builds on Spring, simplifying setup with auto-configuration and embedded servers. Spring requires manual configuration (XML or Java config) and external servers.

## Q7: What is the Spring Boot Actuator?
Provides production-ready endpoints for monitoring and managing the app, such as `/health` and `/metrics`.

## Q8: What is the purpose of application.properties or application.yml?
These files store configuration settings like DB connections, server ports, and logging levels. YAML is preferred for complex hierarchical configs.

## Q9: How do you create a REST API in Spring Boot?
Use `@RestController` with annotations like `@GetMapping`, `@PostMapping` to define endpoints. Include `spring-boot-starter-web`.

## Q10: What is Dependency Injection in Spring Boot?
Dependency Injection (DI) is injecting dependencies into a class rather than creating them inside it, managed by the Spring IoC container (e.g., using `@Autowired` or constructor injection).

## Q11: What is Spring Boot Starter Parent?
A parent POM file in Maven that provides default configurations, dependency versions, and plugins for Spring Boot projects.

## Q12: How do you connect a Spring Boot application to a database?
Use `spring-boot-starter-data-jpa` and configure the database URL, username, and password in `application.properties`.

## Q13: What is Spring Boot DevTools?
Enhances development by enabling features like automatic restarts when code changes and live reload.

## Q14: How do you handle exceptions in Spring Boot?
Use `@ControllerAdvice` and `@ExceptionHandler` to handle exceptions globally across your application.

## Q15: What is the @EnableAutoConfiguration annotation?
Enables Spring Boot’s auto-configuration, setting up beans based on classpath dependencies. Part of `@SpringBootApplication`.

## Q16: How do you implement security in Spring Boot?
Use `spring-boot-starter-security` and configure a `SecurityFilterChain` bean to define access rules, login methods, etc.

## Q17: What is the difference between @Controller and @RestController?
`@Controller` returns view names for MVC apps. `@RestController` combines `@Controller` and `@ResponseBody` to return data (like JSON) directly.

## Q18: How do you create a Spring Boot executable JAR?
Use the Spring Boot Maven/Gradle plugin, run `mvn package`, and execute the resulting JAR with `java -jar`.

## Q19: What is Spring Boot Actuator’s /health endpoint?
Shows the application’s health status, including dependencies like databases. Returns `{"status":"UP"}` if healthy.

## Q20: How do you implement caching in Spring Boot?
Use `spring-boot-starter-cache`, enable it with `@EnableCaching`, and annotate methods with `@Cacheable`.


Spring Framework & Spring Boot:
22. Spring Container internal working.
23. Explain Bean Lifecycle.
24. What is @EnableAutoConfiguration?
25. What is @Qualifier and what is the difference between @Primary and @Qualifier?
26. Diff b/w IOC and Dependency Injection.
27. What is Dependency Injection and its types?
28. What is a spring profile, and how did you use it?
29. What is the difference between Path Variable and Request Params?
30. How does the @Transactional annotation work?
31. What is fetch type (Lazy and Eager Loading)?

Maven and Build
 - What is a Maven build?
 - What does mvn clean install do?
 - How do you push code to production?
 - What is code coverage and line coverage?
 - How do you improve code coverage when build fails?