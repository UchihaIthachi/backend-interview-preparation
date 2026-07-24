# Service Discovery and Centralized Configuration

## Important Corrections

- **Service discovery and load balancing are related but different.** Discovery returns available service instances; load balancing chooses which instance receives a request.
- A service registry is not necessarily a traditional centralized database. It may be implemented through Eureka, Consul, Kubernetes Services and DNS, or another control plane.
- Health checks are not instantaneous. Clients may temporarily receive stale instance information, so calls still require timeouts, retries, circuit breakers, and idempotency.
- A Config Server centralizes configuration, but it does not automatically guarantee that every running instance has refreshed to the same version.
- Not every configuration value is safe to refresh dynamically. Connection pools, thread pools, logging systems, security infrastructure, and startup-only properties may require controlled restart or redeployment.
- Credentials should not normally be stored as plain text in a Git configuration repository. Use a secret manager or encrypted configuration with strict key management.
- In modern Spring Cloud applications, adding a `DiscoveryClient` implementation is usually enough; `@EnableDiscoveryClient` is no longer required. ([Home][1])

---

# 1. What Is Service Discovery?

Service discovery allows one service to find available instances of another service using a logical name instead of a hard-coded host and port.

Without discovery:

```text
Order Service
→ http://10.20.1.14:8082
```

This address becomes invalid when the instance:

- Restarts
- Moves to another host
- Scales horizontally
- Receives a new container IP
- Is removed because it is unhealthy

With discovery:

```text
Order Service
→ inventory-service
```

The discovery mechanism resolves `inventory-service` to one or more available instances:

```text
inventory-service
├── 10.20.1.14:8082
├── 10.20.1.19:8082
└── 10.20.2.04:8082
```

The caller or an intermediary then selects an instance.

---

# 2. Why Is Service Discovery Needed?

Microservice instances are dynamic:

```text
Morning:
payment-service → 2 instances

Peak period:
payment-service → 10 instances

After scaling down:
payment-service → 3 instances
```

Hard-coded addresses create several problems:

- Configuration must change whenever an instance moves.
- Clients may continue calling terminated instances.
- Scaling requires manual updates.
- Failover becomes slow and error-prone.
- Environment-specific addresses become difficult to manage.
- Deployment topology leaks into application logic.

Service discovery separates the **logical identity** of a service from its current physical locations.

---

# 3. Core Components

## Service provider

The service exposing functionality:

```text
inventory-service
```

## Service consumer

The service calling the provider:

```text
order-service
```

## Service registry

Maintains information about available service instances:

```text
service name
instance ID
host
port
status
zone
metadata
last heartbeat
```

## Discovery client or load balancer

Queries the registry or platform and chooses an instance.

---

# 4. Service Registration and Lookup Flow

```text
Inventory Service Instance
        ↓ register
Service Registry
        ↓
Order Service asks for inventory-service
        ↓
Registry returns available instances
        ↓
Load balancer chooses one instance
        ↓
Order Service calls selected instance
```

Detailed flow:

1. An Inventory Service instance starts.
2. It registers its service name, host, port, and metadata.
3. It periodically sends heartbeats or renews a lease.
4. Order Service requests the instances registered as `inventory-service`.
5. Discovery returns one or more candidates.
6. A load balancer selects an instance.
7. The request is sent with appropriate timeout and resilience controls.
8. Unhealthy or expired instances are eventually removed.

Eureka clients register metadata and send heartbeats; if renewals stop for the configured period, the instance is normally removed from the registry. ([Home][2])

---

# 5. Service Discovery vs Service Registry vs Load Balancer

| Component         | Responsibility                                        |
| ----------------- | ----------------------------------------------------- |
| Service Registry  | Stores or exposes available service instances         |
| Service Discovery | Looks up instances by logical service name            |
| Load Balancer     | Chooses one instance for the request                  |
| Health checking   | Determines whether an instance should remain eligible |
| API Gateway       | Routes external requests and applies edge policies    |

Example:

```text
Discovery:
Which Inventory Service instances exist?

Load balancing:
Which of those instances should receive this request?
```

Spring Cloud LoadBalancer provides client-side load-balancing abstractions and supports discovery-backed instance suppliers with implementations such as round-robin and random selection. ([Home][3])

---

# 6. Client-Side Service Discovery

In client-side discovery, the calling service obtains the instance list and chooses a destination itself.

```text
Order Service
    ↓ query
Service Registry
    ↓ instance list
Order Service Load Balancer
    ↓ selected instance
Inventory Service
```

## Advantages

- No additional network hop through a central proxy
- Callers can use custom instance-selection rules
- Zone, version, or metadata-aware routing is possible
- Failure handling can be customized per dependency

## Disadvantages

- Discovery and load-balancing libraries must be integrated into clients.
- Multi-language platforms may require different client implementations.
- Clients may cache stale registry data.
- Every caller must be configured and monitored correctly.
- Load-balancing behavior can become inconsistent across teams.

## Example technologies

- Eureka with Spring Cloud LoadBalancer
- Consul client discovery
- Zookeeper discovery
- Spring Cloud Kubernetes `DiscoveryClient`

---

# 7. Server-Side Service Discovery

In server-side discovery, the client calls a stable intermediary.

```text
Client
   ↓
Load Balancer / Proxy / Kubernetes Service
   ↓
Service instance
```

The intermediary selects an available backend.

## Advantages

- Clients remain simple.
- Discovery implementation is centralized.
- Works well across multiple languages.
- Routing policy can be managed by the platform.
- Applications may use ordinary DNS or HTTP clients.

## Disadvantages

- The intermediary becomes critical infrastructure.
- It adds another network component.
- Routing and health behavior must be highly available.
- Troubleshooting may span client, proxy, and backend layers.

## Examples

- Kubernetes Service
- Cloud load balancer
- Reverse proxy
- Service mesh data plane
- API Gateway for appropriate traffic

---

# 8. Client-Side vs Server-Side Discovery

| Client-side                    | Server-side                             |
| ------------------------------ | --------------------------------------- |
| Client retrieves instances     | Proxy/load balancer retrieves instances |
| Client selects destination     | Intermediary selects destination        |
| Fewer proxy hops               | Simpler application clients             |
| Client needs discovery library | Platform owns discovery logic           |
| Flexible per-client routing    | Consistent centralized routing          |
| More application coupling      | More infrastructure dependency          |

A platform may use both:

```text
External client
→ API Gateway
→ Order Service
→ client-side discovery
→ Inventory Service
```

Or in Kubernetes:

```text
Order Service
→ inventory-service DNS name
→ Kubernetes Service
→ Inventory Pod
```

---

# 9. Service Registry Responsibilities

A registry typically supports:

## Registration

```text
serviceName = payment-service
instanceId  = payment-service-7f8c
host        = 10.10.4.19
port        = 8080
status      = UP
zone        = zone-a
```

## Deregistration

An instance should be removed when:

- It shuts down gracefully.
- Its lease expires.
- Health monitoring marks it unavailable.
- The platform removes the workload.

## Lookup

Clients query:

```text
Find all available instances of payment-service
```

## Health and lease management

Registries may use:

- Client heartbeats
- Server-side health checks
- Lease expiration
- Platform readiness state

## Metadata

Metadata can support:

- Availability zone
- Application version
- Secure port
- Traffic weight
- Region
- Deployment group

Metadata-based routing should be controlled carefully because callers can become coupled to infrastructure details.

---

# 10. Stale Registry Information

Discovery information is not perfectly instantaneous.

Consider:

```text
1. Inventory instance crashes.
2. Registry has not yet expired its lease.
3. Client still has the instance in its local cache.
4. Client selects the dead instance.
5. Connection fails.
```

Therefore, discovery must be combined with:

```text
Connection timeout
+
response timeout
+
bounded safe retry
+
circuit breaker
+
load-balancer feedback
```

Service discovery reduces location management problems; it does not remove network failure.

---

# 11. Spring Cloud Eureka Server

## Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

## Application

```java
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                DiscoveryServerApplication.class,
                args
        );
    }
}
```

Spring Cloud Netflix currently provides `spring-cloud-starter-netflix-eureka-server`, with `@EnableEurekaServer` enabling the server application. ([Home][2])

## Standalone configuration

```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-server

eureka:
  instance:
    hostname: localhost

  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

In standalone mode, registration and registry fetching are disabled because this server has no Eureka peer. For production, multiple peer-aware registry instances are preferable to one manually managed registry process. ([Home][2])

---

# 12. Eureka Client Registration

## Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

## Configuration

```yaml
server:
  port: 8081

spring:
  application:
    name: order-service

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

Having the current Eureka client starter on the classpath makes the application both a registered Eureka instance and a client capable of querying the registry. The default service ID is based on `spring.application.name`. ([Home][2])

A normal Spring Boot application is sufficient:

```java
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                OrderServiceApplication.class,
                args
        );
    }
}
```

`@EnableDiscoveryClient` can still be used for explicitness or configuration, but modern Spring Cloud discovery implementations are activated through classpath auto-configuration. ([Home][1])

---

# 13. Calling a Discovered Service

## Load-balanced `WebClient`

```java
@Configuration
public class HttpClientConfiguration {

    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
```

```java
@Service
public class InventoryClient {

    private final WebClient webClient;

    public InventoryClient(
            @LoadBalanced WebClient.Builder builder
    ) {
        this.webClient = builder
                .baseUrl("http://inventory-service")
                .build();
    }

    public Mono<InventoryResponse> check(String productId) {
        return webClient.get()
                .uri("/inventory/{productId}", productId)
                .retrieve()
                .bodyToMono(InventoryResponse.class);
    }
}
```

Here, `inventory-service` is a service ID rather than a DNS host. Spring Cloud LoadBalancer obtains candidate instances through the available `DiscoveryClient` and selects one. Current Spring Cloud also provides the `spring-cloud-starter-loadbalancer` starter and supports `@LoadBalanced WebClient.Builder`. ([Home][3])

## Do not forget resilience

```text
Discovery
→ select an instance
→ connection timeout
→ response timeout
→ circuit breaker
→ safe retry where applicable
```

Load balancing does not guarantee that the selected instance will remain healthy for the duration of the call.

---

# 14. Service Discovery in Kubernetes

Kubernetes Pods are ephemeral and can receive different IP addresses as workloads scale, restart, or move. A Kubernetes `Service` provides a stable abstraction over a changing set of Pod endpoints. ([Kubernetes][4])

Example:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: inventory-service
spec:
  selector:
    app: inventory-service
  ports:
    - port: 8080
      targetPort: 8080
```

A caller can use:

```text
http://inventory-service:8080
```

Within another namespace:

```text
http://inventory-service.inventory:8080
```

Kubernetes provides DNS records for Services, allowing workloads to discover them using stable names rather than individual Pod addresses. ([Kubernetes][5])

## Do you need Eureka inside Kubernetes?

Often, Kubernetes Service and DNS already provide the required server-side discovery and load-balancing behavior.

Use Eureka or another application-level registry in Kubernetes only when there is a concrete requirement such as:

- Hybrid Kubernetes and non-Kubernetes workloads
- Application-level metadata routing
- Existing platform dependency
- Discovery behavior not provided by normal Services
- Cross-platform service registration requirements

Avoid operating two discovery control planes without a clear reason.

---

# 15. What Is a Config Server?

A Config Server provides external configuration to distributed applications from a central source.

```text
Git / Vault / Configuration Repository
                ↓
          Config Server
        ┌───────┼────────┐
        ↓       ↓        ↓
 Order Service Payment  Inventory
```

Spring Cloud Config provides server-side and client-side support for distributed external configuration, mapping remote properties into Spring’s `Environment` and `PropertySource` abstractions. Git is its default repository style, although multiple alternative repositories are supported. ([Home][6])

---

# 16. Why Centralized Configuration Is Needed

Without central configuration:

```text
order-service/application-prod.yml
payment-service/application-prod.yml
inventory-service/application-prod.yml
```

Problems include:

- Duplicated values
- Environment mismatch
- Manual server edits
- No clear audit history
- Difficult rollback
- Secret sprawl
- Inconsistent feature settings
- Configuration tied to application binaries

With externalized configuration:

```text
Application artifact
+
environment configuration
=
deployable service
```

The same image can be promoted through environments with different external values.

---

# 17. Configuration Dimensions

Spring Cloud Config identifies configuration using:

```text
application
profile
label
```

## Application

Maps to:

```yaml
spring:
  application:
    name: order-service
```

## Profile

Represents an environment or configuration group:

```text
dev
test
staging
production
```

## Label

Represents a versioned configuration set, commonly a Git:

- Branch
- Tag
- Commit ID

Spring Cloud Config’s environment resources are parameterized by `{application}`, `{profile}`, and `{label}`. ([Home][7])

Example request conceptually:

```text
/order-service/production/main
```

---

# 18. Git Repository Structure

A simple configuration repository might contain:

```text
config-repository/
├── application.yml
├── application-production.yml
├── order-service.yml
├── order-service-production.yml
├── payment-service.yml
└── payment-service-production.yml
```

## Shared configuration

`application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - prometheus
```

## Service configuration

`order-service.yml`:

```yaml
order:
  cancellation:
    enabled: true
```

## Environment-specific configuration

`order-service-production.yml`:

```yaml
order:
  cancellation:
    enabled: false
```

More-specific application and profile properties override general defaults according to Spring property precedence. ([Home][7])

---

# 19. Spring Cloud Config Server Implementation

## Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

## Application

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

## Configuration

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
          uri: https://git.example.com/platform/config-repository.git
          cloneOnStart: true
```

The default `EnvironmentRepository` implementation uses a Git backend, and the repository location is configured through `spring.cloud.config.server.git.uri`. ([Home][8])

`cloneOnStart` can detect an invalid repository configuration during Config Server startup rather than waiting until the first client requests configuration. ([Home][8])

---

# 20. Spring Cloud Config Client

## Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

## Required local configuration

```yaml
spring:
  application:
    name: order-service

  profiles:
    active: production

  config:
    import: configserver:http://config-server:8888
```

The modern default integration uses `spring.config.import`. A separate `bootstrap.yml` is not required for this approach. ([Home][9])

## Required vs optional Config Server

Required:

```yaml
spring:
  config:
    import: configserver:http://config-server:8888
```

The service fails startup if configuration cannot be obtained.

Optional:

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

The service may start using its remaining local configuration.

Removing `optional:` creates fail-fast behavior for the Config Data import mechanism. ([Home][9])

## Which should be used?

### Required configuration

Appropriate when missing configuration could cause:

- Connection to the wrong database
- Incorrect security policy
- Invalid business behavior
- Data corruption
- An unusable service

### Optional configuration

Appropriate when:

- Safe local defaults exist.
- The feature is non-critical.
- The service can deliberately operate in a documented degraded mode.

Do not mark Config Server optional merely to hide startup problems.

---

# 21. Config First vs Discovery First

## Config first

The Config Server address is known locally:

```yaml
spring:
  config:
    import: configserver:http://config-server:8888
```

Advantages:

- Simple startup path
- No dependency on a registry before configuration loads
- Fewer startup network operations

Disadvantage:

- Config Server location must be supplied through local configuration, DNS, or environment variables.

## Discovery first

The application finds Config Server through service discovery.

```yaml
spring:
  cloud:
    config:
      discovery:
        enabled: true
        serviceId: config-server
```

Advantages:

- Config Server coordinates can change.
- Multiple Config Server instances can be discovered.

Disadvantages:

- Startup depends on the discovery service.
- An additional lookup is required.
- Bootstrap dependency chains become more complex.

Spring Cloud Config supports discovery-first lookup through an available `DiscoveryClient`, at the cost of an additional startup round trip. ([Home][9])

---

# 22. Dynamic Refresh

Changing Git configuration does not automatically mean every running bean immediately receives the new value.

Possible flow:

```text
Commit configuration
        ↓
Config Server sees new version
        ↓
Refresh signal
        ↓
Client reloads environment
        ↓
Refresh-scoped bean recreated
```

## Refresh one instance

A client can expose the refresh actuator operation where appropriate:

```text
POST /actuator/refresh
```

This affects the selected instance, not automatically every replica.

## Refresh multiple instances

Spring Cloud Bus can distribute refresh events through RabbitMQ or Kafka:

```text
Configuration update
        ↓
POST /actuator/busrefresh
        ↓
Spring Cloud Bus
        ↓
All targeted service instances refresh
```

Spring Cloud Bus provides `/busrefresh`, which reloads configuration across listening application nodes as though their individual refresh operations had been invoked. ([Home][10])

---

# 23. `@RefreshScope`

Example:

```java
@Component
@RefreshScope
@ConfigurationProperties(prefix = "order.cancellation")
public class CancellationProperties {

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

When refreshed, the scoped target is cleared and recreated when accessed again.

## Dynamic refresh is suitable for

- Feature toggles
- Display messages
- Bounded numeric thresholds
- Some endpoint addresses
- Non-structural business configuration

## Dynamic refresh is risky for

- Database pool credentials
- Connection-pool size
- Kafka bootstrap infrastructure
- Thread-pool structure
- Security filter-chain design
- Server ports
- Serialization contracts
- Schema assumptions
- Complex interdependent values

A controlled rolling restart is often safer for structural configuration.

---

# 24. Dynamic Configuration Consistency

A Config Server does not provide one instantaneous distributed transaction across every application instance.

Consider:

```text
Instance A refreshed to configuration version 12.
Instance B still uses version 11.
Instance C is restarting.
```

During rollout, multiple versions may coexist.

For important changes:

1. Add a configuration version.
2. Validate the complete configuration set.
3. Make the application compatible with old and new values.
4. Roll out gradually.
5. Monitor adoption.
6. Roll back when health criteria fail.
7. Remove obsolete settings later.

Avoid configurations where one instance using the old value and another using the new value causes data corruption.

---

# 25. Feature Flags vs General Configuration

A Config Server can store feature settings, but a dedicated feature-flag platform may be better when requirements include:

- Per-user targeting
- Percentage rollout
- Tenant-specific activation
- Experiment tracking
- Immediate kill switches
- Audit of evaluation decisions

Config Server is strongest for application configuration. It should not automatically become a complete experimentation system.

---

# 26. Secrets Management

## Avoid plain secrets in Git

Bad:

```yaml
spring:
  datasource:
    password: production-password
```

Even if the repository is private:

- Developers may clone it.
- CI logs may expose values.
- Git history retains deleted secrets.
- Backups replicate the secret.
- Access may be broader than necessary.

## Prefer

- HashiCorp Vault
- AWS Secrets Manager
- Google Secret Manager
- Azure Key Vault
- Kubernetes Secrets with appropriate encryption and access controls
- Short-lived dynamic credentials where possible

Spring Cloud Config supports several repository backends, including dedicated secret-manager integrations, but access control, rotation, and key management still require deliberate design. ([Home][7])

## Separate configuration from secrets

```text
Config repository:
payment.timeout=2s
payment.retry.maxAttempts=2

Secret manager:
payment.provider.client-secret
database.password
signing.private-key
```

---

# 27. Securing Discovery and Config Infrastructure

Protect Eureka, Config Server, Consul, or equivalent infrastructure with:

- TLS
- Authentication
- Network policies
- Least-privilege authorization
- Restricted administrative endpoints
- Audit logging
- Credential rotation
- High availability
- Backup and recovery procedures

Do not expose:

```text
Eureka dashboard
Config Server API
Actuator environment endpoints
Registry administrative API
```

directly to the public Internet.

A compromised registry could redirect callers to a malicious instance. A compromised Config Server could distribute malicious endpoints, credentials, or feature settings across the platform.

---

# 28. Availability Design

## Service registry unavailable

Existing callers may continue temporarily using cached instance lists, but:

- New services may not register.
- Terminated instances may remain cached.
- Newly scaled instances may not be discovered.
- Recovery behavior depends on the implementation.

Clients must still apply timeouts and safe retries.

## Config Server unavailable during startup

Choose explicitly:

```text
Fail fast
or
start with safe local configuration
```

## Config Server unavailable at runtime

Already loaded configuration normally remains in memory, but:

- New instances may fail startup.
- Refresh cannot retrieve changes.
- Emergency configuration changes cannot propagate.
- Configuration drift may continue.

## Avoid bootstrap dependency cycles

Problematic design:

```text
Config Server needs Eureka to start.
Eureka needs Config Server to start.
```

Keep foundational infrastructure dependencies simple and acyclic.

---

# 29. Monitoring Service Discovery

Monitor:

- Registered service count
- Instances per service
- Registration failures
- Heartbeat or lease-renewal failures
- Expired instances
- Registry lookup latency
- Registry availability
- Load-balancer selection failures
- Requests where no instance was found
- Calls to stale or dead instances
- Zone distribution

Spring Cloud LoadBalancer can expose Micrometer statistics for active, successful, failed, and discarded load-balanced requests when its metrics integration is enabled. ([Home][3])

---

# 30. Monitoring Configuration

Track:

- Config Server availability
- Repository fetch failures
- Git commit currently served
- Configuration version by client
- Refresh success and failure
- Instances still using an old version
- Invalid configuration
- Secret access failures
- Configuration rollback
- Startup failures caused by missing configuration

Log a safe configuration version:

```text
service=order-service
configVersion=3b76c4a
applicationVersion=2.8.1
```

Do not log secret values.

---

# 31. Common Mistakes

## Hard-coding instance addresses

```java
String inventoryUrl = "http://10.10.1.14:8080";
```

Use service identity and external configuration.

## Confusing discovery with load balancing

A registry returns candidates; another component selects a candidate.

## No timeout after discovery

A registered instance can fail immediately after lookup.

## One Eureka server in production

This creates a critical dependency unless failure behavior and recovery are acceptable.

## Adding Eureka unnecessarily in Kubernetes

Kubernetes DNS and Services may already solve the discovery problem.

## Treating the registry as the source of business truth

The registry stores service locations, not business data.

## Making liveness depend on the registry

A temporary registry outage should not necessarily restart every healthy service.

## Storing all secrets in Git

Encryption at rest is useful, but dedicated secret management generally offers stronger rotation and access controls.

## Refreshing every property dynamically

Some values require bean reconstruction, new connections, or full application restart.

## Exposing refresh endpoints publicly

An attacker could alter or repeatedly reload runtime configuration.

## Assuming central configuration guarantees uniformity

Instances can run different configuration versions during rollout, failure, or partial refresh.

## No rollback plan

Every configuration change should be traceable and reversible.

---

# Interview Questions and Answers

## Q1: What is service discovery?

> Service discovery allows a service to locate available instances of another service by logical name instead of hard-coded IP addresses. Instances register or are represented by the platform, and clients or load balancers resolve the name to current endpoints.

## Q2: Why is service discovery necessary?

> Microservice instances scale, restart, and receive changing addresses. Discovery separates service identity from physical location and enables dynamic routing and failover.

## Q3: What is the difference between service discovery and a registry?

> The registry maintains or exposes service-instance information. Discovery is the lookup process that uses the registry or platform to obtain those instances.

## Q4: What is the difference between discovery and load balancing?

> Discovery answers which instances exist. Load balancing chooses which discovered instance receives a request.

## Q5: Client-side vs server-side discovery?

> In client-side discovery, the caller obtains the instance list and selects an instance. In server-side discovery, the caller sends the request to a stable proxy or platform Service, which discovers and selects the backend.

## Q6: How does Eureka know an instance is unavailable?

> Eureka clients send heartbeats or lease renewals. If renewals stop beyond the configured expiration period, the instance is normally removed. Because detection is not instantaneous, callers still require timeouts and retries.

## Q7: Is `@EnableDiscoveryClient` mandatory?

> No. In current Spring Cloud, placing a compatible `DiscoveryClient` implementation on the classpath is generally sufficient. The annotation remains available but is not required.

## Q8: Do you need Eureka in Kubernetes?

> Not necessarily. Kubernetes Services and DNS already provide stable naming and server-side routing to Pods. I would add application-level discovery only for a clear requirement such as hybrid workloads or metadata-aware routing.

## Q9: What is Spring Cloud Config?

> Spring Cloud Config externalizes configuration and provides a central server from which applications load environment- and service-specific properties. The default repository model is Git-based, with application, profile, and label identifying a configuration set.

## Q10: How does a Config Client connect?

> Modern clients use `spring.config.import=configserver:...`. Adding `optional:` allows startup without the server; removing it causes startup to fail when configuration cannot be loaded.

## Q11: How do you refresh configuration across replicas?

> A refresh endpoint affects one instance. Spring Cloud Bus can broadcast a refresh event through Kafka or RabbitMQ to targeted application instances. I still monitor configuration versions because refresh is not one atomic distributed transaction.

## Q12: Should every configuration change be refreshed dynamically?

> No. Simple values such as feature toggles may be refreshable. Structural settings such as connection pools, security infrastructure, and startup properties are usually safer to deploy through a rolling restart.

## Q13: How should secrets be managed?

> I keep ordinary configuration in version control and credentials in a dedicated secret manager. Access is least-privileged, audited, encrypted, and designed for rotation. Secrets are never logged or committed as plain text.

---

# Scenario-Based Answer

## Scenario

Order Service must call multiple Inventory Service instances. Instances scale dynamically, and configuration differs across development, staging, and production.

## Design

```text
Inventory instances
    ↓ register or become Kubernetes endpoints
Discovery platform
    ↓
Order Service resolves inventory-service
    ↓
Load balancer selects instance
    ↓
Timeout + circuit breaker
    ↓
Inventory request
```

Configuration flow:

```text
Git configuration
        ↓
Config Server
        ↓
Order Service loads:
application + profile + label
```

Implementation decisions:

1. Use Kubernetes Service and DNS when all workloads run in Kubernetes.
2. Use Eureka or Consul when application-level or hybrid discovery is required.
3. Use Spring Cloud LoadBalancer for client-side instance selection where applicable.
4. Configure explicit connection and response timeouts.
5. Retry only safe transient failures.
6. Keep Config Server highly available.
7. Require Config Server at startup when missing values would be unsafe.
8. Store credentials in a secret manager.
9. Refresh only values known to be dynamically safe.
10. Record application and configuration versions in telemetry.
11. Secure registry, configuration, and actuator endpoints.
12. Test stale discovery, registry failure, and partial configuration refresh.

---

# Senior Interview Summary

> Service discovery separates a logical service name from dynamic instance locations. A registry or platform exposes available instances, and either the client or a server-side intermediary performs load balancing. In Spring Cloud, Eureka can provide registration and discovery while Spring Cloud LoadBalancer selects an instance. In Kubernetes, Services and DNS often remove the need for a separate application registry.
>
> Centralized configuration externalizes environment-specific properties from application artifacts. Spring Cloud Config identifies configuration by application, profile, and label and commonly reads it from a version-controlled Git repository. Modern clients use `spring.config.import`, with fail-fast or optional startup behavior selected according to business safety.
>
> I do not assume discovery information is perfectly fresh or configuration refresh is globally atomic. Every remote call still has timeouts and resilience controls, and every configuration rollout is versioned, observable, backward-compatible, and reversible. Ordinary configuration belongs in controlled version management, while sensitive credentials belong in a dedicated secret-management system.

[1]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html "Spring Cloud Commons: Common Abstractions :: Spring Cloud Commons"
[2]: https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html "Spring Cloud Netflix Features :: Spring Cloud Netflix"
[3]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html "Spring Cloud LoadBalancer :: Spring Cloud Commons"
[4]: https://kubernetes.io/docs/concepts/services-networking/service/ "Service | Kubernetes"
[5]: https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/?utm_source=chatgpt.com "DNS for Services and Pods"
[6]: https://docs.spring.io/spring-cloud-config/reference/index.html "Spring Cloud Config :: Spring Cloud Config"
[7]: https://docs.spring.io/spring-cloud-config/reference/server/environment-repository.html "Environment Repository :: Spring Cloud Config"
[8]: https://docs.spring.io/spring-cloud-config/reference/server/environment-repository/git-backend.html "Git Backend :: Spring Cloud Config"
[9]: https://docs.spring.io/spring-cloud-config/reference/client.html "Spring Cloud Config Client :: Spring Cloud Config"
[10]: https://docs.spring.io/spring-cloud-bus/reference/quickstart.html "Quickstart :: Spring Cloud Bus"
