# Advanced Questions — Kubernetes Deployment, Autoscaling, Service Mesh, and Networking

## Important Corrections

- HPA is not limited to CPU and memory. It can use resource, custom, and external metrics.
- VPA is installed separately from the core Kubernetes API and primarily adjusts or recommends Pod resource requests.
- KEDA complements HPA by supplying event-driven metrics such as Kafka lag and queue depth; it does not replace every autoscaling mechanism.
- A `PodDisruptionBudget` protects against supported voluntary evictions such as node drains. It does not replace the rollout strategy configured on a Deployment or StatefulSet.
- Resource requests and limits must account for the **entire Java process**, not only the heap.
- Current Spring Boot versions enable graceful shutdown by default, but Kubernetes timing, readiness, message consumers, and background work still require explicit coordination. ([Home][1])
- NetworkPolicy controls Layer 3/4 traffic. It does not authorize HTTP paths, methods, users, or JWT scopes.
- Istio is no longer sidecar-only; it supports both sidecar and ambient data-plane modes. ([Istio][2])

---

# Q73: Deployment vs StatefulSet vs DaemonSet

## Deployment

A Deployment manages replicated Pods, usually for workloads that do not maintain identity-bound state. It manages ReplicaSets and provides declarative rolling updates and rollback support. ([Kubernetes][3])

Typical use cases:

- Spring Boot REST APIs
- API gateways
- Kafka consumers without Pod-specific identity
- Stateless workers
- Web frontends
- Services whose state is stored in an external database

```text
Deployment
    ↓
ReplicaSet
    ├── order-service-7f6b9-abc
    ├── order-service-7f6b9-def
    └── order-service-7f6b9-ghi
```

Pods are generally interchangeable:

```text
Any healthy Order Service Pod
can process the next request.
```

Example:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3

  selector:
    matchLabels:
      app: order-service

  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: example/order-service:2.1.0
          ports:
            - containerPort: 8080
```

---

## StatefulSet

A StatefulSet manages Pods that require stable, unique identities, ordered lifecycle operations, or persistent storage associated with individual replicas. Each Pod receives a stable ordinal name such as `database-0`, `database-1`, and `database-2`. ([Kubernetes][4])

```text
StatefulSet
    ├── database-0
    ├── database-1
    └── database-2
```

Typical use cases:

- Distributed databases
- Consensus-based systems
- Kafka brokers
- ZooKeeper-like coordination services
- Stateful clustered applications
- Workloads requiring one persistent volume per replica

A StatefulSet commonly works with:

- A headless Service
- Stable DNS identity
- `volumeClaimTemplates`
- Ordered startup and termination
- Ordered or partitioned rolling updates

Example:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: account-store
spec:
  serviceName: account-store
  replicas: 3

  selector:
    matchLabels:
      app: account-store

  template:
    metadata:
      labels:
        app: account-store
    spec:
      containers:
        - name: account-store
          image: example/account-store:4.0
          volumeMounts:
            - name: data
              mountPath: /var/lib/account-store

  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes:
          - ReadWriteOnce
        resources:
          requests:
            storage: 50Gi
```

## Important distinction

A Spring Boot service is not necessarily stateful merely because it accesses a database.

```text
Spring Boot API
    ↓
External PostgreSQL database
```

The application Pods are still usually interchangeable and should generally run as a Deployment.

Use StatefulSet only when the individual application Pod itself requires stable identity, storage, or ordered lifecycle behavior.

---

## DaemonSet

A DaemonSet ensures that every eligible node—or every selected group of nodes—runs a copy of a Pod. When nodes are added, corresponding DaemonSet Pods are added automatically. ([Kubernetes][5])

```text
Node 1 → Log Agent
Node 2 → Log Agent
Node 3 → Log Agent
```

Typical use cases:

- Log collectors
- Node monitoring agents
- Network plugins
- Security agents
- Storage drivers
- Node-local DNS
- Hardware or GPU monitoring

Example:

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: node-log-agent
spec:
  selector:
    matchLabels:
      app: node-log-agent

  template:
    metadata:
      labels:
        app: node-log-agent
    spec:
      containers:
        - name: agent
          image: example/log-agent:1.5
```

---

## Comparison

| Feature                       | Deployment              | StatefulSet                         | DaemonSet              |
| ----------------------------- | ----------------------- | ----------------------------------- | ---------------------- |
| Primary purpose               | Replicated application  | Identity-bound stateful application | Node-local service     |
| Pod identity                  | Interchangeable         | Stable and ordered                  | Tied to eligible nodes |
| Persistent volume per replica | Not automatic           | Common through templates            | Usually node-oriented  |
| Scaling                       | Arbitrary replica count | Ordered replica set                 | Based on node count    |
| Rolling updates               | ReplicaSet based        | Ordered or partitioned              | Node-by-node           |
| Common Java use               | REST API                | Cluster member requiring identity   | Monitoring agent       |
| Typical microservice          | Yes                     | Only when truly stateful            | Rarely                 |

## Interview-ready answer

> I use a Deployment for interchangeable stateless application replicas, a StatefulSet when replicas need stable identity, persistent storage, or ordered lifecycle guarantees, and a DaemonSet when one Pod must run on every eligible node. Most Spring Boot microservices use Deployments because their state belongs in external databases or messaging systems.

---

# Q74: Blue-Green vs Canary Deployments

## Blue-green deployment

Blue-green maintains two complete versions:

```text
Blue environment
Version 1 — currently active

Green environment
Version 2 — prepared and tested
```

Deployment flow:

```text
1. Deploy version 2 to Green.
2. Run readiness and verification tests.
3. Switch production traffic from Blue to Green.
4. Observe.
5. Keep Blue temporarily for rollback.
```

```text
Before promotion:

Production Service
        ↓
Blue v1

Preview Service
        ↓
Green v2
```

```text
After promotion:

Production Service
        ↓
Green v2
```

### Advantages

- Clear separation between versions
- Fast traffic switch
- Fast application rollback
- Full-capacity validation before promotion
- Simple concept for operators

### Disadvantages

- Temporarily requires nearly double application capacity
- Database rollback may not be instant
- Long-running connections may remain on the old version
- Both versions must be compatible with shared APIs, events, and data
- A full switch exposes all traffic at once

Argo Rollouts supports blue-green through separate active and preview Services. ([Argo Rollouts][6])

Example:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: order-service
spec:
  replicas: 4

  selector:
    matchLabels:
      app: order-service

  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: example/order-service:2.0

  strategy:
    blueGreen:
      activeService: order-service-active
      previewService: order-service-preview
      autoPromotionEnabled: false
```

---

## Canary deployment

A canary exposes the new version to a small amount of traffic and increases traffic gradually.

```text
Step 1:
v1 → 95%
v2 → 5%

Step 2:
v1 → 75%
v2 → 25%

Step 3:
v1 → 50%
v2 → 50%

Step 4:
v2 → 100%
```

At each stage, evaluate:

- Error rate
- P95/P99 latency
- Saturation
- Business success metrics
- Security failures
- Reconciliation results

Argo Rollouts defines canary delivery as releasing the new version to a limited portion of production traffic and supports automated analysis, pauses, and rollback. ([Argo Rollouts][7])

Example:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: payment-service
spec:
  replicas: 10

  selector:
    matchLabels:
      app: payment-service

  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
        - name: payment-service
          image: example/payment-service:3.0

  strategy:
    canary:
      steps:
        - setWeight: 5
        - pause:
            duration: 5m

        - setWeight: 25
        - pause:
            duration: 10m

        - setWeight: 50
        - pause:
            duration: 10m

        - setWeight: 100
```

Precise traffic weighting normally requires integration with a traffic manager such as Istio, an ingress controller, or another supported routing system. Without one, canary behavior may be based primarily on replica ratios rather than exact request percentages. ([Argo Rollouts][8])

---

## Comparison

| Blue-green                                      | Canary                                   |
| ----------------------------------------------- | ---------------------------------------- |
| Switches between complete environments          | Introduces new version gradually         |
| Fast promotion and application rollback         | Limits initial blast radius              |
| Requires additional full capacity               | Can use a smaller canary capacity        |
| All traffic changes at promotion                | Traffic shifts in steps                  |
| Simpler release model                           | Requires stronger metrics and automation |
| Suitable for manual pre-production verification | Suitable for production-risk validation  |

## Database warning

Neither strategy automatically rolls back a destructive database migration.

Use:

```text
Expand
→ deploy compatible application
→ migrate/backfill
→ switch behavior
→ remove old schema later
```

Old and new versions must coexist safely during the deployment.

## Interview-ready answer

> Blue-green runs two complete environments and switches traffic after the new environment is verified. Canary exposes the new version to a small percentage of production traffic and increases that percentage only when technical and business metrics remain healthy. I use blue-green when a rapid full switch and rollback are valuable, and canary when reducing blast radius is more important.

---

# Q75: Resource Requests and Limits for a Java Pod

## Requests

A resource request is used primarily for scheduling and represents the resource amount Kubernetes reserves when selecting a node.

```text
CPU request:    500m
Memory request: 768Mi
```

## Limits

A limit sets the enforced upper boundary.

```text
CPU limit:    2 cores
Memory limit: 1536Mi
```

CPU limits are enforced through throttling. Memory limits are enforced reactively, and a container may be terminated with an out-of-memory kill when it exceeds available constrained memory. ([Kubernetes][9])

---

## Java memory is more than heap

The total container memory includes:

```text
Java heap
+
Metaspace
+
Code cache
+
Thread stacks
+
Direct ByteBuffers
+
GC structures
+
JNI and native libraries
+
Memory-mapped files
+
JVM internal allocations
```

Therefore:

```text
Container limit ≠ -Xmx
```

Unsafe:

```text
Container memory limit = 1Gi
-Xmx1g
```

The heap alone can consume the complete container allowance, leaving no room for native JVM memory.

---

## Example memory budget

For a `1536Mi` container:

```text
Heap maximum                 950 MiB
Metaspace                    120 MiB
Direct/native buffers        150 MiB
Thread stacks                100 MiB
Code cache and JVM internals  70 MiB
Safety margin                146 MiB
```

These values are illustrative. Actual sizing must come from load tests, Native Memory Tracking, heap metrics, thread count, direct-buffer use, and production behavior.

---

## `MaxRAMPercentage`

Modern JVMs can calculate heap size as a percentage of the memory available to the JVM, including applicable container constraints. Oracle documents `-XX:MaxRAMPercentage` as controlling the maximum percentage used for heap sizing when an explicit maximum heap is not set. ([Oracle Docs][10])

Example:

```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: >-
      -XX:MaxRAMPercentage=65.0
      -XX:InitialRAMPercentage=40.0
      -XX:+ExitOnOutOfMemoryError
      -XX:+HeapDumpOnOutOfMemoryError
      -XX:HeapDumpPath=/var/dumps
```

With a `1536Mi` memory limit, a 65% maximum percentage gives the heap approximately 998 MiB before other JVM ergonomics and rounding.

Do not blindly use:

```text
-XX:MaxRAMPercentage=100
```

The JVM still requires substantial non-heap memory.

---

## Example Pod configuration

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3

  selector:
    matchLabels:
      app: order-service

  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: example/order-service:2.1.0

          resources:
            requests:
              cpu: 500m
              memory: 768Mi

            limits:
              cpu: "2"
              memory: 1536Mi

          env:
            - name: JAVA_TOOL_OPTIONS
              value: >-
                -XX:MaxRAMPercentage=65.0
                -XX:InitialRAMPercentage=40.0
                -XX:+ExitOnOutOfMemoryError
```

---

## CPU considerations

A low CPU limit can cause throttling even when the node has spare CPU capacity. Throttling can increase:

- P99 latency
- Garbage-collection duration
- Request queueing
- Kafka processing lag
- Startup time

Monitor:

```text
container CPU usage
container CPU throttling
request latency
GC CPU and pause time
executor queue depth
Kafka lag
```

A Java service with short bursty CPU demand may benefit from a realistic request and a carefully tested limit rather than an extremely restrictive limit.

---

## Memory considerations

Monitor:

- Heap used after GC
- Allocation rate
- Metaspace
- Direct-buffer memory
- Thread count
- Process RSS
- Container working set
- OOM kills
- Heap-dump storage capacity

`kubectl top` provides metrics intended for autoscaling decisions and may not exactly match operating-system tools. ([Kubernetes][11])

## Interview-ready answer

> I determine requests from sustained observed usage and use them to ensure reliable scheduling. I set limits using load tests and capacity policy, remembering that CPU limits throttle while memory pressure can terminate the container. For Java, I budget heap plus metaspace, direct buffers, thread stacks, code cache, native libraries, and a safety margin. I never set `Xmx` equal to the container limit.

---

# Q76: HPA vs VPA vs KEDA

## Horizontal Pod Autoscaler

HPA changes the number of Pod replicas.

```text
Increased demand
→ increase replicas

Reduced demand
→ decrease replicas
```

It can scale a Deployment or StatefulSet using:

- CPU utilization
- Memory utilization
- Custom metrics
- External metrics
- Multiple metrics combined

HPA is part of the Kubernetes API and periodically adjusts replica counts according to observed metrics. ([Kubernetes][12])

Example:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service

  minReplicas: 3
  maxReplicas: 20

  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 65

  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 60

    scaleDown:
      stabilizationWindowSeconds: 300
```

CPU percentage targets depend on CPU requests. Missing or inaccurate requests can make utilization-based scaling misleading.

---

## Vertical Pod Autoscaler

VPA changes or recommends CPU and memory requests for existing workload Pods.

```text
Current request:
CPU 250m, memory 512Mi

VPA recommendation:
CPU 600m, memory 900Mi
```

VPA can be used for:

- Rightsizing
- Reducing repeated OOM risk
- Improving scheduling accuracy
- Workloads that cannot scale horizontally effectively
- Recommendation-only analysis

Unlike HPA, VPA must be installed separately and is represented through a CRD. ([Kubernetes][13])

Conceptual example:

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: report-service
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: report-service

  updatePolicy:
    updateMode: Auto
```

Depending on the cluster version, VPA mode, and runtime support, applying changed resources may recreate Pods or use supported in-place resource resizing.

## HPA and VPA conflict

Avoid letting both autoscalers control the same CPU or memory signal without a deliberate design.

Example conflict:

```text
HPA:
High CPU → create more Pods

VPA:
High CPU → increase CPU request
```

Changing the request also changes HPA’s utilization calculation.

A common combination is:

```text
HPA scales replicas using request rate or queue depth.
VPA recommends CPU and memory without applying them automatically.
```

---

## KEDA

KEDA is a Kubernetes event-driven autoscaler. It integrates with standard Kubernetes autoscaling components and can activate or scale workloads based on external event sources. ([KEDA][14])

Common KEDA triggers include:

- Kafka lag
- RabbitMQ queue depth
- Cloud queues
- Prometheus queries
- Database queries
- Scheduled triggers
- Event-stream backlog

For supported event triggers, KEDA can scale workloads from zero when no work exists and activate them when new events arrive. ([KEDA][15])

Example Kafka scaler:

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: invoice-consumer-scaler
spec:
  scaleTargetRef:
    name: invoice-consumer

  minReplicaCount: 0
  maxReplicaCount: 20
  pollingInterval: 15
  cooldownPeriod: 300

  triggers:
    - type: kafka
      metadata:
        bootstrapServers: kafka.messaging.svc:9092
        consumerGroup: invoice-service
        topic: invoice-created
        lagThreshold: "100"
```

Kafka consumers cannot gain useful active parallelism beyond the number of assignable partitions for that consumer group. Partition skew must also be considered; total lag alone can hide one hot partition. KEDA’s Kafka scaler documentation explicitly considers Kafka partition and consumer behavior in scaling decisions. ([KEDA][16])

---

## Comparison

| Autoscaler | Changes                                              | Typical signal                      | Scale to zero                             | Main purpose         |
| ---------- | ---------------------------------------------------- | ----------------------------------- | ----------------------------------------- | -------------------- |
| HPA        | Number of Pods                                       | CPU, memory, custom/external metric | Normally requires suitable external setup | Horizontal capacity  |
| VPA        | Resource requests                                    | Historical CPU/memory usage         | No                                        | Rightsizing          |
| KEDA       | Number of Pods through event metrics/HPA integration | Queue depth, Kafka lag, events      | Yes, for supported triggers               | Event-driven scaling |

## When to use KEDA over ordinary HPA

Use KEDA when the real demand signal is:

```text
Kafka consumer lag
RabbitMQ queue length
Pending cloud events
Scheduled workload
External event backlog
```

Use ordinary HPA when the service is request-driven and signals such as CPU, memory, or an exposed custom metric represent demand adequately.

## Interview-ready answer

> HPA scales the number of Pods using resource, custom, or external metrics. VPA adjusts or recommends resource requests for individual Pods. KEDA extends horizontal autoscaling with event-driven triggers such as Kafka lag and queue depth and can activate some workloads from zero. I use KEDA for asynchronous consumers whose backlog is a better demand signal than CPU.

---

# Q77: Graceful Shutdown in Spring Boot on Kubernetes

## Termination flow

When Kubernetes terminates a Pod, it tracks a grace period, invokes any configured `preStop` hook, requests graceful process termination, and eventually forcefully terminates containers that do not exit within the allowed period. The default Pod termination grace period is 30 seconds. ([Kubernetes][17])

Conceptual flow:

```text
Pod enters termination
        ↓
Stop routing new traffic
        ↓
Run preStop hook
        ↓
Send SIGTERM
        ↓
Spring closes ApplicationContext
        ↓
Stop accepting new work
        ↓
Drain bounded in-flight work
        ↓
Close pools and clients
        ↓
Exit before grace period
```

Current Spring Boot enables graceful shutdown by default for supported embedded servers. It allows existing requests to finish within the configured shutdown phase while refusing new requests. ([Home][1])

---

## Spring Boot configuration

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 25s

management:
  endpoint:
    health:
      probes:
        enabled: true
```

A JVM shutdown hook closes the Spring `ApplicationContext` and invokes lifecycle callbacks such as `@PreDestroy`. ([Home][18])

Example:

```java
@Component
public class PaymentWorkerLifecycle {

    private final AtomicBoolean acceptingWork =
            new AtomicBoolean(true);

    @PreDestroy
    public void stop() {
        acceptingWork.set(false);

        // Stop pulling new work.
        // Drain bounded in-flight work.
        // Flush durable checkpoints.
        // Close clients and executors.
    }
}
```

---

## Kubernetes configuration

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3

  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1

  selector:
    matchLabels:
      app: order-service

  template:
    metadata:
      labels:
        app: order-service
    spec:
      terpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63: 40

      containers:
        - name: order-service
          image: example/order-service:2.4.0

          lifecycle:
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - sleep 5

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
            timeoutSeconds: 2
            failureThreshold: 2

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
            timeoutSeconds: 2
            failureThreshold: 3
```

The grace period should be greater than:

```text
preStop duration
+
Spring shutdown timeout
+
small operational margin
```

---

## Why use `preStop`?

Service endpoint updates and external load-balancer propagation are not always instantaneous. A short `preStop` delay is sometimes used to allow traffic routing to converge before SIGTERM begins the application shutdown.

Do not use a large arbitrary sleep as the only shutdown mechanism. The application still needs genuine draining and idempotent processing.

---

## HTTP requests

For HTTP workloads:

- Stop accepting new traffic.
- Allow bounded in-flight requests to complete.
- Ensure client timeouts are shorter than termination duration.
- Make write requests idempotent.
- Do not start a long operation after shutdown begins.

## Kafka consumers

During shutdown:

```text
Stop polling new records
→ finish or safely abandon current bounded batch
→ commit only completed offsets
→ close consumer
```

A record may still be delivered again after abrupt termination, so handlers must remain idempotent.

## Scheduled and background jobs

- Stop accepting new jobs.
- Store checkpoints durably.
- Interrupt only cancellation-safe work.
- Use distributed ownership for singleton jobs.
- Ensure another instance can resume incomplete work.

## Interview-ready answer

> Kubernetes executes the termination lifecycle within a configured grace period and sends SIGTERM to the container. Spring Boot closes its application context and gracefully drains supported web requests. I combine that with readiness probes, a short `preStop` delay where needed, a grace period longer than the Spring shutdown timeout, safe consumer shutdown, durable checkpoints, and idempotency because forced termination can still occur.

---

# Q78: What Is a Service Mesh? Istio vs Linkerd

A service mesh is an infrastructure layer that manages service-to-service networking independently of application code.

It commonly provides:

- mTLS
- Workload identity
- Traffic routing
- Load balancing
- Retries and timeouts
- Access policy
- Service-level telemetry
- Multi-cluster connectivity

Istio describes a service mesh as an infrastructure layer for security, observability, and advanced traffic management without requiring those capabilities to be coded independently into every service. ([Istio][19])

---

## Architecture

```text
Application container
        ↓
Mesh data plane
        ↓
Network
        ↓
Destination mesh data plane
        ↓
Destination application
```

The control plane configures the data plane:

```text
Control plane
    ↓ configuration and identity
Data-plane proxies/tunnels
    ↓
Service traffic
```

---

## Istio

Istio provides:

- Advanced Layer 7 traffic management
- mTLS and workload identity
- Authentication and authorization policy
- Canary and weighted routing
- Fault injection
- Multi-cluster features
- Envoy-based sidecar mode
- Ambient mode using a shared Layer 4 tunnel and optional Layer 7 proxies

Istio currently supports both sidecar and ambient data-plane models. ([Istio][20])

Istio VirtualServices and DestinationRules provide programmable service routing and destination policies. ([Istio][21])

### Strengths

- Broad traffic-management capabilities
- Fine-grained routing
- Strong security policy model
- Mature Envoy ecosystem
- Sidecar and ambient deployment options
- Suitable for complex multi-cluster and policy-heavy platforms

### Trade-offs

- Larger feature and configuration surface
- Requires platform expertise
- Misconfiguration can be difficult to diagnose
- Advanced Layer 7 features add operational and data-plane complexity

---

## Linkerd

Linkerd inserts lightweight transparent proxies beside workloads. Its `linkerd2-proxy` is a purpose-built Rust proxy. ([Linkerd][22])

It provides capabilities such as:

- Automatic mTLS
- Workload authorization policy
- Request success rate
- Request throughput
- Latency metrics
- Retries and timeouts
- Traffic splitting
- Multi-cluster connectivity

Linkerd exposes golden metrics including request success rate, request volume, and latency. ([Linkerd][23])

### Strengths

- Focused operational model
- Purpose-built lightweight Rust proxy
- Automatic service telemetry
- Straightforward mTLS setup
- Often simpler for teams needing core mesh capabilities

### Trade-offs

- Less extensive policy and advanced Layer 7 routing surface than a fully configured Istio deployment
- Smaller ecosystem around Envoy-specific extensions
- Teams must verify that required gateway, egress, traffic, and multi-cluster features match their exact needs

---

## Comparison

| Istio                                   | Linkerd                                          |
| --------------------------------------- | ------------------------------------------------ |
| Broad traffic and policy platform       | Focused service-mesh platform                    |
| Envoy sidecars or ambient data plane    | Rust micro-proxy sidecars                        |
| Strong advanced Layer 7 routing         | Emphasis on core reliability and observability   |
| Rich security and traffic configuration | More constrained configuration surface           |
| Suitable for complex policy needs       | Often suitable for teams prioritizing simplicity |
| Greater operational surface             | Generally narrower operational surface           |

## Important warning

A mesh does not replace:

- Application authorization
- Idempotency
- Database transactions
- Business-level tracing
- Secure coding
- NetworkPolicy
- Rate limits
- Correct timeouts in every protocol

Mesh retries can duplicate non-idempotent writes unless retry policies and application semantics are coordinated.

## Interview-ready answer

> A service mesh moves common service-networking concerns—such as mTLS, workload identity, traffic control, and telemetry—into a platform-managed data plane. Istio offers a broad, powerful policy and routing model with sidecar and ambient modes. Linkerd uses purpose-built Rust micro-proxies and generally focuses on a simpler set of reliability, security, and observability capabilities. I select based on concrete requirements and operational capacity rather than feature count alone.

---

# Q79: Zero-Downtime Updates for a Stateful Java Service

## First ask: Is the application itself stateful?

A service that stores state in PostgreSQL, Redis, Kafka, or object storage can still be stateless at the Pod level.

```text
Spring Boot instances
    ↓
External shared database
```

Use a Deployment when Pods are interchangeable.

Use a StatefulSet when each Pod requires:

- Stable network identity
- Pod-specific persistent storage
- Ordered replacement
- Membership identity
- Leader/follower or quorum coordination

---

## Zero-downtime requirements

```text
Multiple healthy replicas
+
readiness checks
+
safe rollout policy
+
graceful shutdown
+
backward-compatible contracts
+
replicated state
+
capacity during rollout
+
rollback capability
```

---

## Deployment strategy for externally stored state

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: account-service
spec:
  replicas: 4
  minReadySeconds: 15
  progressDeadlineSeconds: 600

  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1

  selector:
    matchLabels:
      app: account-service

  template:
    metadata:
      labels:
        app: account-service
    spec:
      terpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63: 45

      containers:
        - name: account-service
          image: example/account-service:5.2.0

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
            failureThreshold: 2

          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 5
            failureThreshold: 30
```

A Deployment’s rolling update can control the maximum unavailable and additional surge Pods. ([Kubernetes][3])

---

## StatefulSet strategy

StatefulSets provide ordered identities and rolling updates. `minReadySeconds` can require a newly created Pod to remain ready and stable for a minimum duration before rollout progression. ([Kubernetes][4])

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: ledger-node
spec:
  serviceName: ledger-node
  replicas: 3
  minReadySeconds: 20

  updateStrategy:
    type: RollingUpdate
    rollingUpdate:
      partition: 0

  selector:
    matchLabels:
      app: ledger-node

  template:
    metadata:
      labels:
        app: ledger-node
    spec:
      containers:
        - name: ledger-node
          image: example/ledger-node:4.1
```

A partition can be used to update selected ordinals first, but the application’s replication and membership protocol must tolerate the sequence.

---

## Database migration strategy

Never combine the deployment with an immediately destructive migration.

Use **expand–migrate–contract**:

```text
Release A:
Add new nullable column or table.
Old and new code remain compatible.

Release B:
Backfill data.
Start writing both old and new representation where necessary.

Release C:
Switch reads to new representation.

Later:
Remove old field after every old application version is gone.
```

Avoid:

```text
Rename or delete a column
→ deploy application afterward
```

During rolling deployment, old and new versions coexist.

---

## API and event compatibility

New versions must tolerate:

- Old API clients
- Old event producers
- Old event consumers
- Missing newly introduced fields
- Additional unknown fields
- Mixed configuration versions

Prefer additive changes.

---

## PodDisruptionBudget

A PDB limits supported voluntary disruptions such as node maintenance and eviction. It does not guarantee availability during node failure, application failure, or an incorrectly configured rollout. ([Kubernetes][24])

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: account-service-pdb
spec:
  minAvailable: 3

  selector:
    matchLabels:
      app: account-service
```

For four replicas, this permits one supported voluntary eviction while retaining three available replicas.

---

## Additional controls

### Topology distribution

Place replicas across:

- Nodes
- Availability zones
- Failure domains

### Session state

Avoid process-local user sessions. Store them externally or design clients to reconnect safely.

### Distributed jobs

Use leases, fencing tokens, or durable ownership. Two Pods must not both believe they exclusively own the same job.

### Capacity

Ensure the cluster has enough room for `maxSurge`. Otherwise, the new Pod may remain pending and block the rollout.

### Rollback

Application rollback works only when:

- Database changes remain backward compatible.
- Events remain compatible.
- Configuration remains compatible.
- Old container images remain available.

## Interview-ready answer

> I first determine whether the Java Pods are actually stateful or merely use an external database. For interchangeable Pods, I use a Deployment with multiple replicas, `maxUnavailable: 0`, readiness probes, surge capacity, and graceful shutdown. For identity-bound replicas, I use a StatefulSet and respect its ordered update and replication semantics. Zero downtime also requires backward-compatible database, API, event, and configuration changes; Kubernetes rollout settings alone are insufficient.

---

# Q80: Kubernetes Network Policies

NetworkPolicy controls permitted traffic to and from selected Pods at Layer 3 and Layer 4 using Pod selectors, namespace selectors, IP blocks, ports, and protocols.

The cluster must use a network plugin that enforces NetworkPolicy. Creating the resource has no effect if the CNI does not implement it. ([Kubernetes][25])

---

## Default behavior

Without applicable policies, Pod traffic is generally unrestricted by NetworkPolicy.

Use a default-deny policy and then add explicit allow rules.

---

## Default deny ingress and egress

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: payments
spec:
  podSelector: {}

  policyTypes:
    - Ingress
    - Egress
```

An empty `podSelector` selects all Pods in the namespace. With no allow rules, selected traffic is denied according to the listed policy types. Kubernetes documents this as the default-deny-all pattern. ([Kubernetes][25])

---

## Allow Order Service to call Payment Service

Assume:

```text
Namespace: orders
Pod label: app=order-service

Namespace: payments
Pod label: app=payment-service

Payment port: 8080
```

Policy in the `payments` namespace:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-orders-to-payment
  namespace: payments
spec:
  podSelector:
    matchLabels:
      app: payment-service

  policyTypes:
    - Ingress

  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: orders

          podSelector:
            matchLabels:
              app: order-service

      ports:
        - protocol: TCP
          port: 8080
```

Because the namespace and Pod selectors appear in the same peer entry, the source must match both:

```text
Namespace = orders
AND
Pod label = app=order-service
```

---

## Allow Payment Service to reach PostgreSQL

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: payment-egress
  namespace: payments
spec:
  podSelector:
    matchLabels:
      app: payment-service

  policyTypes:
    - Egress

  egress:
    - to:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: databases

          podSelector:
            matchLabels:
              app: payment-postgres

      ports:
        - protocol: TCP
          port: 5432
```

---

## Allow DNS

Default-deny egress also blocks DNS unless it is explicitly allowed. Kubernetes calls this out in its default-deny guidance. ([Kubernetes][25])

A common policy is:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns
  namespace: payments
spec:
  podSelector: {}

  policyTypes:
    - Egress

  egress:
    - to:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: kube-system

          podSelector:
            matchLabels:
              k8s-app: kube-dns

      ports:
        - protocol: UDP
          port: 53

        - protocol: TCP
          port: 53
```

DNS labels vary by cluster distribution, so confirm the labels used by the cluster’s DNS Pods.

---

## Policies are additive

When multiple policies select the same Pod, their permitted traffic is combined.

```text
Policy A allows Order Service.
Policy B allows Monitoring Service.

Result:
Both are allowed.
```

There is no ordered “first rule wins” behavior in standard Kubernetes NetworkPolicy.

---

## What NetworkPolicy cannot do

Standard NetworkPolicy cannot normally express:

```text
Allow GET /orders
but deny DELETE /orders

Require OAuth scope payments.write

Allow only valid JWTs

Limit requests to 100 per minute
```

It primarily operates on:

- IP addresses
- Pod and namespace identity through selectors
- Ports
- TCP, UDP, and optionally SCTP

Kubernetes documents NetworkPolicy as Layer 3/4 traffic filtering. ([Kubernetes][25])

Use a service mesh, gateway, or application security for Layer 7 authorization.

---

## Recommended design

```text
Default-deny NetworkPolicy
        +
Explicit service-to-service allow rules
        +
mTLS workload identity
        +
OAuth/JWT authorization
        +
Application-level object authorization
```

## Operational checklist

- Verify that the CNI supports enforcement.
- Start with observability before broad enforcement.
- Allow DNS and necessary platform services.
- Define ingress and egress separately.
- Use stable workload labels.
- Test policies in staging.
- Monitor denied connection attempts.
- Do not use broad namespace-wide allow rules unnecessarily.
- Restrict access to databases and message brokers.
- Pair policies with application-layer authorization.

## Interview-ready answer

> I begin with default-deny ingress and egress policies, then explicitly permit only the required Pod, namespace, protocol, and port combinations. I verify that the cluster’s CNI actually enforces NetworkPolicy and remember to allow required DNS traffic. NetworkPolicy gives Layer 3/4 segmentation; I still use mTLS and service-level authorization for workload identity and business permissions.

---

# Quick Interview Cheat Sheet

| Topic             | Core answer                                              |
| ----------------- | -------------------------------------------------------- |
| Deployment        | Interchangeable application replicas                     |
| StatefulSet       | Stable identity, storage, ordered lifecycle              |
| DaemonSet         | One Pod on every eligible node                           |
| Blue-green        | Two environments, switch traffic                         |
| Canary            | Gradually increase new-version traffic                   |
| Resource request  | Scheduling and reserved capacity                         |
| CPU limit         | Throttles excess CPU                                     |
| Memory limit      | Can result in OOM termination                            |
| Java memory       | Heap plus all native/non-heap memory                     |
| HPA               | Changes replica count                                    |
| VPA               | Recommends or changes Pod resources                      |
| KEDA              | Event-driven horizontal scaling                          |
| Graceful shutdown | Stop traffic, drain work, exit before grace ends         |
| Service mesh      | Platform-managed service networking                      |
| Istio             | Broad traffic/security platform                          |
| Linkerd           | Focused mesh using Rust micro-proxies                    |
| PDB               | Limits supported voluntary disruptions                   |
| NetworkPolicy     | Layer 3/4 traffic isolation                              |
| Zero downtime     | Rollout controls plus compatible application/data design |

# Senior Interview Summary

> I use Deployments for interchangeable Java microservices, StatefulSets only for replicas requiring stable identity or storage, and DaemonSets for node-level agents. Releases use rolling, blue-green, or canary strategies according to risk, with readiness checks and automated analysis deciding progression.
>
> Java Pod resources are sized from measured heap, native memory, thread, direct-buffer, and CPU behavior. HPA handles replica scaling, VPA supports rightsizing, and KEDA scales event consumers using backlog-oriented metrics such as Kafka lag.
>
> Zero-downtime delivery requires more than Kubernetes configuration. The service must shut down gracefully, database migrations must follow expand–migrate–contract, APIs and events must remain compatible, state must be replicated, and rollback must remain possible.
>
> Network security begins with default-deny NetworkPolicies and explicit communication paths, while mTLS and application authorization provide workload identity and business-level protection.

[1]: https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html?utm_source=chatgpt.com "Graceful Shutdown :: Spring Boot"
[2]: https://istio.io/latest/docs/overview/dataplane-modes/?utm_source=chatgpt.com "Sidecar or ambient?"
[3]: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/?utm_source=chatgpt.com "Deployments"
[4]: https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/?utm_source=chatgpt.com "StatefulSets"
[5]: https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/?utm_source=chatgpt.com "DaemonSet"
[6]: https://argo-rollouts.readthedocs.io/en/stable/features/bluegreen/?utm_source=chatgpt.com "BlueGreen Deployment Strategy - Argo Rollouts"
[7]: https://argo-rollouts.readthedocs.io/en/stable/features/canary/?utm_source=chatgpt.com "Canary Deployment Strategy - Argo Rollouts"
[8]: https://argo-rollouts.readthedocs.io/en/stable/concepts/?utm_source=chatgpt.com "Concepts - Kubernetes Progressive Delivery Controller"
[9]: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/?utm_source=chatgpt.com "Resource Management for Pods and Containers"
[10]: https://docs.oracle.com/en/java/javase/24/docs/specs/man/java.html?utm_source=chatgpt.com "The java Command"
[11]: https://kubernetes.io/docs/reference/kubectl/generated/kubectl_top/?utm_source=chatgpt.com "kubectl top"
[12]: https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/?utm_source=chatgpt.com "Horizontal Pod Autoscaling"
[13]: https://kubernetes.io/docs/concepts/workloads/autoscaling/vertical-pod-autoscale/?utm_source=chatgpt.com "Vertical Pod Autoscaling"
[14]: https://keda.sh/?utm_source=chatgpt.com "KEDA | Kubernetes Event-driven Autoscaling"
[15]: https://keda.sh/docs/2.20/concepts/scaling-deployments/?utm_source=chatgpt.com "Scaling Deployments, StatefulSets & Custom Resources"
[16]: https://keda.sh/docs/2.20/scalers/apache-kafka/?utm_source=chatgpt.com "Apache Kafka"
[17]: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/?utm_source=chatgpt.com "Pod Lifecycle"
[18]: https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com "SpringApplication :: Spring Boot"
[19]: https://istio.io/latest/about/service-mesh/?utm_source=chatgpt.com "The Istio service mesh"
[20]: https://istio.io/?utm_source=chatgpt.com "Istio"
[21]: https://istio.io/latest/docs/concepts/traffic-management/?utm_source=chatgpt.com "Traffic Management"
[22]: https://linkerd.io/docs/overview/?utm_source=chatgpt.com "Overview"
[23]: https://linkerd.io/2-edge/features/telemetry/?utm_source=chatgpt.com "Telemetry and Monitoring"
[24]: https://kubernetes.io/docs/concepts/workloads/pods/disruptions/?utm_source=chatgpt.com "Disruptions"
[25]: https://kubernetes.io/docs/concepts/services-networking/network-policies/?utm_source=chatgpt.com "Network Policies"
