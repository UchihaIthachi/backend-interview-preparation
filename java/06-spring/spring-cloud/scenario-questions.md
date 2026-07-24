# Scenario-Based Question

## How would you implement centralized configuration using Spring Cloud Config?

I would use **Spring Cloud Config Server** as a centralized configuration service and store environment-specific configuration in a version-controlled repository such as Git.

```text
Configuration repository
        ↓
Spring Cloud Config Server
        ↓
Order service instances
Payment service instances
Inventory service instances
```

The Config Server reads configuration from the repository and exposes it according to:

```text
Application name
+
Active profile
+
Repository label or branch
```

Clients import the remote configuration into their Spring `Environment` during startup. ([Home][1])

---

# 1. Create a Central Configuration Repository

A configuration repository could be organized like this:

```text
central-config/
├── application.yml
├── application-dev.yml
├── application-prod.yml
├── order-service.yml
├── order-service-dev.yml
├── order-service-prod.yml
├── payment-service.yml
└── payment-service-prod.yml
```

## Shared configuration

```yaml
# application.yml

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
```

This configuration applies to all services.

## Service-specific configuration

```yaml
# order-service.yml

order:
  maximum-items: 20
  reservation-timeout: 30s
```

## Environment-specific configuration

```yaml
# order-service-prod.yml

order:
  maximum-items: 100
  reservation-timeout: 15s

logging:
  level:
    com.example.orders: INFO
```

The effective configuration for `order-service` with the `prod` profile can combine shared, service-specific, and profile-specific files.

---

# 2. Create the Config Server

## Maven dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

## Enable the Config Server

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                ConfigServerApplication.class,
                args
        );
    }
}
```

## Connect the Config Server to Git

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server

  cloud:
    config:
      server:
        git:
          uri: https://git.example.com/platform/central-config.git
          default-label: main
          clone-on-start: true
```

The Config Server acts as an HTTP interface over its configured `EnvironmentRepository`. A Git repository is common, although Spring Cloud Config supports other repository implementations as well. ([Home][1])

---

# 3. Retrieve Configuration from the Server

Configuration is commonly addressed using:

```text
/{application}/{profile}
/{application}/{profile}/{label}
```

For example:

```text
/order-service/prod
/order-service/prod/main
```

Conceptually, the Config Server responds with property sources assembled from matching configuration files.

```json
{
  "name": "order-service",
  "profiles": ["prod"],
  "label": "main",
  "propertySources": [
    {
      "name": "order-service-prod.yml",
      "source": {
        "order.maximum-items": 100
      }
    }
  ]
}
```

The exact response structure is primarily useful for diagnostics; application clients normally consume it automatically.

---

# 4. Configure Each Microservice as a Config Client

## Client dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

## Client configuration

```yaml
spring:
  application:
    name: order-service

  profiles:
    active: prod

  config:
    import: configserver:http://config-server:8888
```

The value of:

```yaml
spring:
  application:
    name: order-service
```

must correspond to the service-specific file name:

```text
order-service.yml
order-service-prod.yml
```

Using `spring.config.import` can cause multiple Config Server requests during startup. Spring Boot first resolves default configuration and then loads configuration for any profiles activated during that process. This is expected behavior. ([Home][1])

---

## Optional Config Server

Configuration can be made optional:

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

With `optional:`, the application may start using its remaining local configuration when the Config Server is unavailable.

Without `optional:`, an unavailable Config Server normally causes startup failure.

### Which approach should be used?

For configuration required for correct operation:

```yaml
spring:
  config:
    import: configserver:http://config-server:8888
```

Failing startup may be safer than running with incomplete or incorrect configuration.

For noncritical external configuration:

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

may be acceptable.

---

# 5. Read the Centralized Configuration

For related configuration values, prefer `@ConfigurationProperties` over many separate `@Value` fields.

```java
@ConfigurationProperties(prefix = "order")
public class OrderProperties {

    private int maximumItems;
    private Duration reservationTimeout;

    public int getMaximumItems() {
        return maximumItems;
    }

    public void setMaximumItems(int maximumItems) {
        this.maximumItems = maximumItems;
    }

    public Duration getReservationTimeout() {
        return reservationTimeout;
    }

    public void setReservationTimeout(
            Duration reservationTimeout
    ) {
        this.reservationTimeout = reservationTimeout;
    }
}
```

Register it:

```java
@Configuration
@EnableConfigurationProperties(OrderProperties.class)
public class OrderConfiguration {
}
```

Use it:

```java
@Service
public class OrderPolicy {

    private final OrderProperties properties;

    public OrderPolicy(OrderProperties properties) {
        this.properties = properties;
    }

    public void validateItemCount(int itemCount) {
        if (itemCount > properties.getMaximumItems()) {
            throw new IllegalArgumentException(
                    "Order exceeds maximum item count"
            );
        }
    }
}
```

This gives related configuration a typed structure and supports validation more cleanly than scattering string-based property lookups throughout the application.

---

# 6. Refresh One Application Instance Without Restarting

Changing the Git repository does not automatically recreate every bean in a running application.

A bean that should be recreated after configuration refresh can use `@RefreshScope`.

```java
@Component
@RefreshScope
public class OrderPolicy {

    private final int maximumItems;

    public OrderPolicy(
            @Value("${order.maximum-items}")
            int maximumItems
    ) {
        this.maximumItems = maximumItems;
    }

    public int getMaximumItems() {
        return maximumItems;
    }
}
```

Expose the refresh endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - refresh
```

After updating and committing the configuration:

```bash
curl -X POST \
  http://order-service:8080/actuator/refresh
```

The refresh operation clears refresh-scoped bean targets so they can be recreated using the latest configuration. The endpoint affects only the application instance receiving the request. ([Home][2])

```text
POST instance-1/actuator/refresh

Instance 1 → refreshed
Instance 2 → unchanged
Instance 3 → unchanged
```

---

# 7. Refresh All Instances Using Spring Cloud Bus

In a horizontally scaled deployment, manually calling every instance is impractical.

Spring Cloud Bus broadcasts configuration-refresh events through:

- RabbitMQ
- Kafka

```text
Configuration changed in Git
        ↓
POST /actuator/busrefresh
        ↓
Spring Cloud Bus
        ↓
RabbitMQ or Kafka
        ├── Order instance 1
        ├── Order instance 2
        ├── Order instance 3
        └── Other targeted services
```

Spring Cloud Bus provides dedicated starters for RabbitMQ and Kafka transports. ([Home][3])

## RabbitMQ dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

## Kafka dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-kafka</artifactId>
</dependency>
```

Expose the Bus endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - busrefresh
```

Trigger refresh:

```bash
curl -X POST \
  http://order-service:8080/actuator/busrefresh
```

The Bus refresh endpoint clears `RefreshScope` caches and rebinds configuration-property beans across the targeted applications. ([Home][4])

---

# 8. Automate Refresh with a Git Webhook

A more automated flow can be:

```text
Developer updates configuration
        ↓
Pull request and review
        ↓
Configuration merged
        ↓
Git webhook calls Config Server monitor endpoint
        ↓
Config Server publishes refresh event
        ↓
Spring Cloud Bus
        ↓
Affected application instances refresh
```

Spring Cloud Config can process repository push notifications and publish a `RefreshRemoteApplicationEvent`. Its default change-detection strategy can target services based on changed filenames, such as `order-service.yml`, while shared `application.yml` changes may target all applications. ([Home][5])

This avoids manually calling `busrefresh` after every approved configuration change.

---

# 9. Secure the Config Server

The Config Server should not be publicly accessible without protection.

I would apply:

- Spring Security authentication
- TLS
- Network restrictions
- Least-privilege repository credentials
- Audit logging
- Restricted Actuator exposure
- Configuration change reviews
- Branch protection
- Controlled rollback procedures

Example:

```java
@Configuration
public class ConfigServerSecurity {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

Config clients would then provide the corresponding credentials through a secure secret-management mechanism.

---

# 10. Do Not Store Plain Secrets in Git

The central configuration repository is appropriate for:

- Feature settings
- Service URLs
- Timeouts
- Logging levels
- Pool sizes
- Operational limits

It should not be treated as the only protection for:

- Database passwords
- OAuth client secrets
- Private keys
- Encryption keys
- Cloud credentials

For secrets, use a dedicated secret-management system such as:

- Vault
- AWS Secrets Manager
- Azure Key Vault
- Google Secret Manager
- Kubernetes secret-store integrations

Spring Cloud Config supports several repository types, including secret-manager-backed environments, but secret access and lifecycle still require strict authorization, rotation, and auditing. ([Home][6])

---

# 11. Not Every Property Is Safe to Refresh

Runtime refresh should be used carefully.

Properties that are often safer to refresh include:

- Feature flags
- Business thresholds
- Retry counts
- Selected timeouts
- Logging levels

Properties that may require controlled restart or special handling include:

- Database URLs
- Connection-pool implementation
- Kafka consumer group identity
- Server ports
- Encryption keys
- Security-filter configuration
- Thread-pool structure
- Bean-definition conditions

Changing a property value does not guarantee that every existing singleton or infrastructure component will reconstruct itself correctly.

For high-risk configuration changes, a rolling restart may be safer:

```text
Update configuration
→ deploy one instance
→ verify health and behavior
→ continue rolling deployment
```

---

# 12. Failure Handling

## Config Server unavailable during startup

Choose deliberately between:

```text
Fail fast
```

and:

```text
Start with local/default configuration
```

For correctness-critical configuration, fail-fast behavior is usually safer.

## Git repository unavailable

The Config Server should expose health information showing whether its `EnvironmentRepository` is working. Spring Cloud Config includes a Config Server health indicator for that repository. ([Home][7])

## Invalid configuration committed

Use:

- Pull-request review
- Schema or configuration validation
- Automated tests
- Protected branches
- Versioned releases
- Fast rollback to a previous Git commit
- Canary refresh or rolling deployment

## Partial refresh

A Bus event might reach some instances while another instance is temporarily unavailable. Configuration changes should therefore tolerate brief version skew, and monitoring should expose the configuration version or Git commit currently loaded by each instance.

---

# 13. Production Architecture

```text
                     Git configuration repository
                               │
                               ▼
                    Spring Cloud Config Server
                      ├── secured with TLS
                      ├── repository credentials
                      └── health monitoring
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
       order-service                    payment-service
       instance 1                       instance 1
       instance 2                       instance 2
       instance 3                       instance 3
                │                             │
                └───────────┬─────────────────┘
                            ▼
                  RabbitMQ or Kafka Bus
                            │
                            ▼
                 Configuration refresh events
```

For availability, the Config Server itself can run as multiple instances behind a load balancer. Clients should use explicit startup and failure policies rather than assuming the server is always reachable.

---

# Interview-Ready Answer

> I would create a Spring Cloud Config Server backed by a version-controlled Git repository. Configuration files would be organized by application, profile, and label, such as `order-service-prod.yml`. Each microservice would use `spring.config.import=configserver:...` and its `spring.application.name` to load the correct configuration during startup.
>
> For runtime updates, I would mark genuinely refreshable beans with `@RefreshScope` and use `/actuator/refresh` for one instance. In a scaled environment, I would use Spring Cloud Bus with RabbitMQ or Kafka and trigger `/actuator/busrefresh` so that all targeted instances clear their refresh scope and rebind configuration properties.
>
> I would secure the Config Server and management endpoints, keep secrets in a dedicated secret manager, validate configuration changes through pull requests and automated checks, and use rolling restarts for infrastructure properties that are unsafe to refresh dynamically.

[1]: https://docs.spring.io/spring-cloud-config/reference/client.html?utm_source=chatgpt.com "Spring Cloud Config Client"
[2]: https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/?utm_source=chatgpt.com "Cloud Native Applications"
[3]: https://docs.spring.io/spring-cloud-bus/docs/current/reference/html/?utm_source=chatgpt.com "Spring Cloud Bus"
[4]: https://docs.spring.io/spring-cloud-bus/reference/spring-cloud-bus/bus-endpoints.html?utm_source=chatgpt.com "Bus Endpoints"
[5]: https://docs.spring.io/spring-cloud-config/reference/server/push-notifications-and-bus.html?utm_source=chatgpt.com "Push Notifications and Spring Cloud Bus"
[6]: https://docs.spring.io/spring-cloud-config/reference/server/environment-repository/aws-secrets-manager.html?utm_source=chatgpt.com "AWS Secrets Manager :: Spring Cloud Config"
[7]: https://docs.spring.io/spring-cloud-config/reference/server/health-indicator.html?utm_source=chatgpt.com "Health Indicator :: Spring Cloud Config"
