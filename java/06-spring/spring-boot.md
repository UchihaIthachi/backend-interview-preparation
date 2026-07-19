# Spring Boot: Auto-Configuration and Startup

## 1. Definition

Spring Boot builds on Spring Core to remove manual configuration — it inspects
the classpath and application properties to automatically configure beans
(a `DataSource`, an embedded web server, etc.) that would otherwise require
manual XML/Java config.

## 2. Why it exists

Before Spring Boot, wiring a basic web app required substantial boilerplate
configuration. Auto-configuration applies sensible defaults conditionally,
only when relevant libraries are present and no conflicting bean already exists.

## 3. How it works — startup flow

```text
main() calls SpringApplication.run()
  -> Environment created (profiles, property sources resolved)
  -> ApplicationContext created
  -> Bean definitions loaded (component scan + auto-configuration classes)
  -> @Conditional checks run (ConditionalOnClass, ConditionalOnMissingBean, etc.)
  -> Beans instantiated in dependency order
  -> Embedded server (Tomcat/Netty) started
  -> ApplicationReadyEvent published
```

`@SpringBootApplication` is really three annotations combined:
`@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.

## 4. Code example — conditional auto-configuration

```java
@Configuration
@ConditionalOnClass(DataSource.class)          // only if a DB driver is on the classpath
@ConditionalOnMissingBean(DataSource.class)     // only if the user hasn't defined their own
class DataSourceAutoConfiguration {
    @Bean
    DataSource dataSource(DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }
}
```

## 5. Production use case

Debugging "why did my custom bean not get used" or "why is my app connecting
to the wrong database" almost always comes back to auto-configuration order
and `@ConditionalOnMissingBean` — defining your own bean of the same type
overrides Spring Boot's default.

## 6. Common mistakes

- Not understanding that defining your own `@Bean` of a type usually disables
  the auto-configured default for that type.
- Debugging startup failures without checking `--debug` output, which lists
  exactly which auto-configurations were applied/excluded and why.
- Assuming profile-specific properties (`application-prod.yml`) are loaded
  without setting `spring.profiles.active` correctly.

## 7. Trade-offs

Auto-configuration trades some "magic"/implicitness for drastically less
boilerplate — worth understanding the mechanism specifically so it doesn't
feel like magic during debugging.

## 8. Interview questions

1. How does Spring Boot auto-configuration work under the hood?
2. What is inside `@SpringBootApplication`?
3. How does Spring choose which auto-configuration to apply?
4. How do profiles work?
5. How would you debug an application that fails during startup?

## 9. Short interview answer

"Spring Boot's auto-configuration classes are conditionally applied based on
what's on the classpath and whether the user has already defined a competing
bean — via annotations like @ConditionalOnClass and @ConditionalOnMissingBean.
When debugging startup issues, I run with --debug to see the auto-configuration
report, which shows exactly what was applied, skipped, and why."

## 10. Related topics

- [Spring Core / DI](spring-core.md)
- [REST API development](rest-api.md)
