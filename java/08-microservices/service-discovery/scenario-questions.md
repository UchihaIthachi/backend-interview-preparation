# Scenario-Based Questions — Service Discovery with Eureka

# Q1: How Will You Implement Service Discovery?

## Problem

In a microservices system, service instances are dynamic:

```text
inventory-service
├── instance 1 → 10.0.1.14:8080
├── instance 2 → 10.0.1.22:8080
└── instance 3 → 10.0.2.07:8080
```

Instances may:

- Restart
- Scale up or down
- Move to another host
- Receive new IP addresses
- Become unhealthy

Therefore, callers should not use hard-coded addresses such as:

```text
http://10.0.1.14:8080
```

They should use a logical service identity:

```text
http://inventory-service
```

---

## High-Level Design

```text
Service Provider
    ↓ registers instance
Service Registry
    ↓ exposes available instances
Service Consumer
    ↓ performs lookup
Load Balancer
    ↓ chooses one instance
Selected Service Instance
```

The registry answers:

> Which instances of `inventory-service` are currently available?

The load balancer answers:

> Which available instance should receive this request?

Service discovery and load balancing are related, but they are separate responsibilities.

---

## Implementation Steps

## Step 1: Choose a Discovery Model

### Client-side discovery

```text
Order Service
    ↓ lookup
Service Registry
    ↓ list of instances
Order Service Load Balancer
    ↓
Inventory instance
```

The calling application:

1. Queries the registry.
2. Receives candidate instances.
3. Selects an instance.
4. Sends the request directly.

Examples include Eureka with Spring Cloud LoadBalancer and Consul-based discovery.

### Server-side discovery

```text
Order Service
    ↓
Platform Service / Proxy / Load Balancer
    ↓
Inventory instance
```

The caller uses a stable platform endpoint while the intermediary performs discovery and routing.

Examples include:

- Kubernetes Service and DNS
- Cloud load balancers
- Service-mesh proxies
- Internal reverse proxies

Kubernetes Services expose changing Pod backends through a stable service abstraction and DNS name, so a separate Eureka registry is often unnecessary when every workload already runs inside Kubernetes. ([Kubernetes][1])

---

## Step 2: Register Service Instances

Each service instance provides:

```text
service name
instance ID
host
port
health status
metadata
zone or region
```

Example:

```text
serviceName = inventory-service
instanceId  = inventory-service-8081
host        = 10.0.1.14
port        = 8081
status      = UP
zone        = zone-a
```

Registration can be:

- **Self-registration:** the application registers itself.
- **Platform registration:** Kubernetes, an orchestrator, or another agent maintains the endpoints.

---

## Step 3: Track Health and Availability

The registry or platform must eventually remove unavailable instances using mechanisms such as:

- Heartbeats
- Lease renewal
- Health checks
- Readiness status
- Graceful deregistration
- Lease expiration

Eureka clients send instance metadata and heartbeats. When heartbeats stop beyond the configured period, the instance is normally removed from the registry. ([Home][2])

Discovery is not instantaneous. A caller may temporarily select an instance that has just failed.

Therefore, service discovery must still be combined with:

```text
Connection timeout
+
response timeout
+
circuit breaker
+
bounded retry
+
idempotency
```

---

## Step 4: Perform Client-Side Load Balancing

The caller retrieves available instances and selects one using an algorithm such as:

- Round robin
- Random
- Zone preference
- Weighted selection
- Metadata-based filtering

Spring Cloud LoadBalancer can obtain available instances through a `DiscoveryClient` implementation and use them for client-side selection. ([Home][3])

---

## Step 5: Secure the Discovery Infrastructure

Protect the registry using:

- TLS
- Authentication
- Network policies
- Least-privilege access
- Restricted administrative endpoints
- Credential rotation
- Audit logs

A compromised registry could redirect service traffic to a malicious endpoint.

---

## Step 6: Monitor Discovery

Track:

- Number of registered services
- Instances per service
- Registration failures
- Heartbeat failures
- Expired instances
- Lookup latency
- No-instance-available errors
- Requests sent to stale instances
- Load-balancer successes and failures

---

## General Interview Answer

> I implement service discovery by giving each service a logical name and registering its available instances in a registry or platform control plane. Consumers resolve that logical name through a discovery client or server-side proxy, and a load balancer selects an instance. I combine discovery with health monitoring, graceful deregistration, timeouts, circuit breakers, bounded retries, security, and observability because registry information can temporarily become stale.

---

# Q2: How Will You Implement Service Discovery Using Eureka?

## Architecture

Consider three applications:

```text
Eureka Server
     ↑
     ├── Inventory Service instance 1
     ├── Inventory Service instance 2
     └── Order Service
```

Request flow:

```text
Order Service
    ↓ asks Eureka for inventory-service
Eureka registry
    ↓ returns available instances
Spring Cloud LoadBalancer
    ↓ selects one instance
Inventory Service
```

Eureka acts as both the discovery server and client ecosystem. It supports peer-aware server deployments for higher availability. ([Home][4])

---

# 1. Project Structure

```text
service-discovery-demo/
├── discovery-server/
├── inventory-service/
└── order-service/
```

Use a Spring Cloud release train compatible with the selected Spring Boot version rather than assigning unrelated Spring Cloud dependency versions manually.

---

# 2. Create the Eureka Server

## Maven dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

The official Eureka server starter is `spring-cloud-starter-netflix-eureka-server`. ([Home][2])

## Main class

```java
package com.example.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

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

## `application.yml`

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
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

For a standalone Eureka server:

- `register-with-eureka: false` prevents the server from registering itself.
- `fetch-registry: false` prevents it from trying to retrieve a peer registry.
- Port `8761` is the conventional local Eureka server port.

Spring’s current Eureka documentation uses this standalone configuration and recommends peer-aware servers for higher availability in production. ([Home][2])

After startup, the Eureka dashboard is available at:

```text
http://localhost:8761
```

---

# 3. Create the Inventory Service

## Maven dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Adding the Eureka client starter makes the Spring Boot application register itself and enables it to query the registry. The default service ID comes from `spring.application.name`. ([Home][4])

## Main class

```java
package com.example.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                InventoryServiceApplication.class,
                args
        );
    }
}
```

`@EnableDiscoveryClient` is not required in current Spring Cloud when a compatible `DiscoveryClient` implementation is present on the classpath. ([Home][5])

## `application.yml`

```yaml
server:
  port: ${SERVER_PORT:8081}

spring:
  application:
    name: inventory-service

eureka:
  instance:
    instance-id: ${spring.application.name}:${server.port}

  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
```

The `defaultZone` key is case-sensitive because Eureka models `serviceUrl` as a map, so use `defaultZone`, not `default-zone`. ([Home][2])

## Response model

```java
package com.example.inventory.api;

public record InventoryResponse(
        String productId,
        boolean available,
        String servedBy
) {
}
```

## Controller

```java
package com.example.inventory.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final String port;

    public InventoryController(
            @Value("${server.port}") String port
    ) {
        this.port = port;
    }

    @GetMapping("/{productId}")
    public InventoryResponse checkInventory(
            @PathVariable String productId
    ) {
        return new InventoryResponse(
                productId,
                true,
                "inventory-service:" + port
        );
    }
}
```

---

# 4. Run Multiple Inventory Instances

Run the first instance:

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

Run the second instance:

```bash
SERVER_PORT=8082 ./mvnw spring-boot:run
```

Eureka should display:

```text
INVENTORY-SERVICE

inventory-service:8081
inventory-service:8082
```

---

# 5. Create the Order Service

## Maven dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Spring Cloud LoadBalancer’s starter provides client-side load balancing over instances supplied by the discovery client. ([Home][6])

## `application.yml`

```yaml
server:
  port: 8090

spring:
  application:
    name: order-service

eureka:
  instance:
    instance-id: ${spring.application.name}:${server.port}

  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
```

---

# 6. Configure a Load-Balanced `RestClient`

```java
package com.example.order.config;

import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(
            RestClientBuilderConfigurer configurer
    ) {
        return configurer.configure(RestClient.builder());
    }
}
```

The `@LoadBalanced` builder allows a logical service name to be used as the URI host. Spring Cloud LoadBalancer resolves that virtual host to a physical service instance. Using Spring Boot’s builder configurer also preserves Boot-provided client customizations such as observability support. ([Home][7])

---

# 7. Implement the Inventory Client

```java
package com.example.order.client;

import com.example.order.dto.InventoryResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(
            @LoadBalanced RestClient.Builder builder
    ) {
        this.restClient = builder
                .baseUrl("http://inventory-service")
                .build();
    }

    public InventoryResponse checkInventory(
            String productId
    ) {
        return restClient.get()
                .uri("/api/inventory/{productId}", productId)
                .retrieve()
                .body(InventoryResponse.class);
    }
}
```

The URI contains:

```text
inventory-service
```

It does not contain:

```text
localhost:8081
localhost:8082
10.0.1.14:8080
```

At runtime:

```text
http://inventory-service
        ↓
Eureka DiscoveryClient
        ↓
available Inventory instances
        ↓
Spring Cloud LoadBalancer
        ↓
selected host and port
```

---

# 8. Order Controller

```java
package com.example.order.api;

import com.example.order.client.InventoryClient;
import com.example.order.dto.InventoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final InventoryClient inventoryClient;

    public OrderController(
            InventoryClient inventoryClient
    ) {
        this.inventoryClient = inventoryClient;
    }

    @GetMapping("/inventory/{productId}")
    public InventoryResponse checkInventory(
            @PathVariable String productId
    ) {
        return inventoryClient.checkInventory(productId);
    }
}
```

Response DTO:

```java
package com.example.order.dto;

public record InventoryResponse(
        String productId,
        boolean available,
        String servedBy
) {
}
```

---

# 9. Test the Application

Start applications in this order:

```text
1. discovery-server
2. inventory-service on port 8081
3. inventory-service on port 8082
4. order-service
```

Call the Order Service repeatedly:

```bash
curl http://localhost:8090/api/orders/inventory/P-100
```

Possible first response:

```json
{
  "productId": "P-100",
  "available": true,
  "servedBy": "inventory-service:8081"
}
```

Possible next response:

```json
{
  "productId": "P-100",
  "available": true,
  "servedBy": "inventory-service:8082"
}
```

This demonstrates:

```text
Service registration
+
service lookup
+
client-side load balancing
```

---

# 10. What Happens When an Inventory Instance Fails?

Suppose the instance on port `8081` stops.

```text
Inventory 8081 stops
        ↓
heartbeats stop
        ↓
Eureka eventually expires the registration
        ↓
clients refresh their registry caches
        ↓
new requests use Inventory 8082
```

Eureka uses server and client-side caches, so removal is not instantaneous. Spring’s documentation notes that metadata convergence can take multiple heartbeat cycles. ([Home][2])

During that interval, Order Service may still select the failed instance.

Therefore, add:

- Connect timeout
- Response timeout
- Circuit breaker
- Retry on another instance only when safe
- Idempotency for write requests

Do not assume Eureka alone provides fault tolerance.

---

# 11. Adding Resilience

Conceptual call flow:

```text
Order Service
    ↓
Timeout
    ↓
Circuit Breaker
    ↓
Spring Cloud LoadBalancer
    ↓
Inventory Service
```

For a safe `GET`, a bounded retry may choose another instance.

For a write such as:

```http
POST /payments
```

do not retry blindly. The first instance may have completed the operation before its response was lost. Use an idempotency key and reconciliation.

---

# 12. Production Eureka Design

## Use multiple Eureka servers

A single registry is a failure risk.

```text
Eureka peer 1
    ↔
Eureka peer 2
    ↔
Eureka peer 3
```

Eureka servers can register with peers and replicate registry state between them. ([Home][2])

Example concept:

```yaml
eureka:
  client:
    service-url:
      defaultZone: https://eureka-2.internal/eureka/
```

Each server points to one or more peers rather than disabling Eureka client behaviour as in standalone mode.

## Secure the registry

Use:

- Internal network access
- TLS
- Authentication
- Spring Security
- Restricted dashboard access
- Protected `/eureka/**` endpoints
- Credential rotation

Current Eureka clients also support TLS-related client properties for key stores and trust stores. ([Home][4])

## Monitor

Track:

```text
registered instances
heartbeat failures
expired leases
registry fetch failures
lookup latency
no-instance errors
load-balancer failures
Eureka server availability
```

Spring Cloud LoadBalancer can publish Micrometer measurements for active, successful, failed, and discarded load-balanced requests when its metrics integration is enabled. ([Home][6])

---

# Common Mistakes

## Mistake 1: Hard-coding service URLs

```java
String inventoryUrl =
        "http://localhost:8081/api/inventory";
```

This prevents dynamic scaling and failover.

---

## Mistake 2: Calling the service name without a load-balanced client

A normal `RestClient` interprets:

```text
inventory-service
```

as an ordinary DNS hostname.

Use a builder marked with:

```java
@LoadBalanced
```

and include Spring Cloud LoadBalancer.

---

## Mistake 3: Forgetting `spring.application.name`

Eureka uses it as the default service ID.

```yaml
spring:
  application:
    name: inventory-service
```

---

## Mistake 4: Using the wrong Eureka URL

Correct:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

Include the `/eureka/` path.

---

## Mistake 5: Assuming registration is immediate

Clients and servers maintain cached registry information. Allow for registration and cache-refresh delay during startup and failover.

---

## Mistake 6: Using Eureka as the load balancer

Eureka supplies the instance list. Spring Cloud LoadBalancer chooses the destination.

---

## Mistake 7: Adding Eureka unnecessarily in Kubernetes

When services are entirely inside Kubernetes, a normal Kubernetes Service and DNS name may already provide the required discovery and routing. Adding Eureka creates another control plane that must be operated and secured. ([Kubernetes][1])

---

# Interview-Ready Answers

## How will you implement service discovery?

> I assign every service a logical name and register its running instances in a service registry or platform control plane. The consumer resolves that service name through a discovery client or server-side proxy, and a load balancer selects an available instance. I add health monitoring, lease expiration, graceful deregistration, timeouts, circuit breakers, bounded retries, and monitoring because discovery data can temporarily become stale.

## How will you implement it with Eureka?

> I create a Eureka Server using `spring-cloud-starter-netflix-eureka-server` and `@EnableEurekaServer`. Each microservice includes the Eureka client starter, defines `spring.application.name`, and configures the server under `eureka.client.service-url.defaultZone`. The services automatically register and periodically renew their leases. A consuming service uses Spring Cloud LoadBalancer with a `@LoadBalanced RestClient.Builder` or `WebClient.Builder` and calls the provider through a logical URL such as `http://inventory-service`. Eureka returns candidate instances, and the load balancer selects one.

## What happens if one service instance goes down?

> Its heartbeats stop and Eureka eventually removes it after lease expiration. Because registries and clients cache instance data, callers may temporarily receive a stale address. I therefore configure connection timeouts, circuit breakers, and safe bounded retries instead of depending only on registry health.

## Is `@EnableDiscoveryClient` required?

> No. In current Spring Cloud, adding a compatible discovery-client implementation to the classpath is normally sufficient for auto-registration and discovery. The annotation may still be used for explicitness, but it is not mandatory. ([Home][5])

## Eureka or Kubernetes Service Discovery?

> I use Kubernetes Services and DNS when the workloads are completely managed by Kubernetes and ordinary platform routing satisfies the requirements. I consider Eureka when I need application-level discovery, hybrid Kubernetes and non-Kubernetes workloads, or Eureka-specific metadata and routing behaviour.

[1]: https://kubernetes.io/docs/concepts/services-networking/service/?utm_source=chatgpt.com "Service"
[2]: https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html "Spring Cloud Netflix Features :: Spring Cloud Netflix"
[3]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html?utm_source=chatgpt.com "Spring Cloud LoadBalancer"
[4]: https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html?utm_source=chatgpt.com "Spring Cloud Netflix Features"
[5]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html?utm_source=chatgpt.com "Spring Cloud Commons: Common Abstractions"
[6]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html "Spring Cloud LoadBalancer :: Spring Cloud Commons"
[7]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html "Spring Cloud Commons: Common Abstractions :: Spring Cloud Commons"
