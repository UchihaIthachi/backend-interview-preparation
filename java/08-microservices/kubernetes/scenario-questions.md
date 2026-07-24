# Scenario-Based Questions — Docker, Kubernetes, Cloud, Auto-Scaling, and Deployments

## Important Corrections

- “AWS auto-scaling” can refer to an EC2 Auto Scaling Group, ECS Service Auto Scaling, Kubernetes HPA on EKS, or node-level scaling. Debug the correct layer first.
- A container being in the `Running` state only means its primary process has not exited. It does not prove that the application is ready, responsive, or healthy.
- Auto-scaling cannot compensate for an unhealthy application, a saturated database, insufficient cluster capacity, or an incorrect scaling metric.
- Zero-downtime deployment requires application-level compatibility, sufficient capacity, readiness checks, and graceful shutdown—not only a rolling-update configuration.
- Environment variables are suitable for ordinary configuration, but long-lived secrets are better obtained through a secret-management system.
- A Kubernetes Service provides stable discovery and routing, but it does not guarantee that the selected backend remains healthy throughout a request. Timeouts and resilience patterns are still required.

---

# 1. AWS Auto-Scaling Is Not Triggering

## Step 1: Identify the scaling layer

Determine what is expected to scale:

| Platform                     | Scaled resource                 |
| ---------------------------- | ------------------------------- |
| EC2 Auto Scaling             | Virtual-machine instances       |
| ECS Service Auto Scaling     | ECS task count                  |
| Kubernetes HPA               | Pod replicas                    |
| KEDA                         | Event-driven Pod replicas       |
| Cluster Autoscaler/Karpenter | Kubernetes nodes                |
| Application Auto Scaling     | Supported AWS resource capacity |

Do not inspect EC2 scaling alarms when the real problem is an EKS HPA that cannot create Pods.

---

## EC2 Auto Scaling Group investigation

Start with the group state and scaling activity history:

```bash
aws autoscaling describe-auto-scaling-groups \
  --auto-scaling-group-names production-api-asg
```

```bash
aws autoscaling describe-scaling-activities \
  --auto-scaling-group-name production-api-asg \
  --max-items 20
```

AWS records the scaling activity status and error message, which can reveal launch-template errors, capacity problems, health-check failures, quota issues, or suspended scaling processes. ([AWS Documentation][1])

### Verify capacity boundaries

Check:

```text
Minimum capacity
Desired capacity
Maximum capacity
```

Example:

```text
Current desired capacity: 10
Maximum capacity:         10
```

Even if the alarm requests scale-out, the group cannot exceed its configured maximum.

### Check scaling-policy state

Verify:

- The scaling policy is enabled.
- The correct Auto Scaling Group is targeted.
- The policy is not attached to an old group.
- Dynamic-scaling processes are not suspended.
- A scheduled action is not continually overriding desired capacity.
- Scale-in protection is not blocking removal when investigating scale-in.

AWS allows individual policies or Auto Scaling processes such as alarm notifications and scheduled actions to be suspended, which can make an otherwise valid policy appear ineffective. ([AWS Documentation][2])

### Inspect CloudWatch alarms and metrics

```bash
aws cloudwatch describe-alarms \
  --state-value ALARM
```

Check:

- Is the alarm entering `ALARM`?
- Is the metric being published?
- Is the metric in the correct region?
- Are dimensions correct?
- Is the statistic correct—average, sum, maximum, percentile?
- Is the evaluation window too long?
- Is the data delayed or missing?
- Does the metric move proportionally with capacity?

For target tracking, AWS creates and manages the underlying CloudWatch alarms and attempts to keep the chosen metric near its configured target. The metric should represent average load or throughput per unit of capacity. ([AWS Documentation][3])

### Check warm-up and cooldown behavior

A new instance may need time to:

- Boot
- Download dependencies
- Join the load balancer
- Start the JVM
- Warm caches
- Pass health checks

If instance warm-up is too long, further scale-out may appear delayed. If it is too short, partially initialized instances may distort the scaling metric.

### Check instance-launch failures

Possible causes include:

- Invalid launch-template version
- Missing AMI
- Subnet IP exhaustion
- Security-group problems
- IAM instance-profile failure
- EC2 quota
- Unavailable instance type
- Capacity shortage in an Availability Zone
- Failed user-data script
- Failed load-balancer registration

Scaling activity history is usually the first place to find these failures. ([AWS Documentation][1])

---

## ECS Service Auto Scaling investigation

Check:

```text
Service desired count
Running task count
Pending task count
Minimum and maximum capacity
Scaling policy
CloudWatch metric
Task-placement failures
Capacity provider
```

ECS Service Auto Scaling changes the service’s desired task count through Application Auto Scaling. Target-tracking policies use metrics such as average CPU or memory and manage the related CloudWatch alarms. ([AWS Documentation][4])

A desired count may increase while no new tasks become `RUNNING` because of:

- No EC2 capacity
- Fargate capacity constraints
- Insufficient CPU or memory
- Missing subnets or IP addresses
- Image-pull failure
- Task execution-role failure
- Failing health checks
- Invalid task definition

Distinguish:

```text
Scaling decision failed
```

from:

```text
Scaling decision succeeded,
but new capacity could not start
```

---

## EKS or Kubernetes HPA investigation

Start with:

```bash
kubectl get hpa -A
kubectl describe hpa <hpa-name> -n <namespace>
kubectl get deployment <deployment> -n <namespace>
kubectl top pods -n <namespace>
```

Check HPA conditions such as:

```text
AbleToScale
ScalingActive
ScalingLimited
```

Common causes:

- Metrics Server unavailable
- Custom-metrics adapter unavailable
- CPU requests missing
- Wrong target resource
- `maxReplicas` already reached
- Current metric below threshold
- Stabilization window delaying scale-down
- New Pods are unready and excluded from calculations
- A sidecar distorts Pod-level CPU or memory metrics

For utilization-based HPA scaling, Kubernetes calculates utilization relative to resource requests. Missing or unrealistic requests can prevent meaningful scaling decisions. ([Kubernetes][5])

### HPA scales Pods, not nodes

The HPA may increase the desired replica count while Pods remain `Pending`:

```bash
kubectl get pods -n <namespace>
kubectl describe pod <pending-pod> -n <namespace>
```

Possible reasons:

- Insufficient node CPU or memory
- Node-selector mismatch
- Taints without tolerations
- Pod anti-affinity
- Storage not available
- Maximum node-group size reached
- Cluster node autoscaler not functioning

This is a two-layer issue:

```text
HPA requests more Pods
        ↓
Scheduler needs node capacity
        ↓
Node autoscaler must provide nodes
```

---

## Metrics to track

### Demand metrics

- Requests per second
- Concurrent requests
- ALB requests per target
- Queue depth
- Kafka lag
- Oldest-message age
- Scheduled-job backlog

### Resource metrics

- CPU utilization
- Memory working set
- CPU throttling
- Network throughput
- Disk I/O
- Connection-pool utilization

### Scaling-state metrics

- Minimum, desired, and maximum capacity
- Running and pending capacity
- Scaling activity failures
- HPA desired versus current replicas
- Unschedulable Pods
- Node-provisioning duration

### Application metrics

- P95 and P99 latency
- Error rate
- Rejection rate
- Thread-pool queue depth
- Database connection waiting
- Downstream timeouts

## Interview-ready answer

> I first identify whether the failing layer is EC2, ECS, Kubernetes Pods, or Kubernetes nodes. I then verify the scaling signal, policy status, minimum and maximum capacity, activity history, cooldown or warm-up, and whether newly requested capacity can actually launch. I correlate the scaling metric with user demand and monitor pending capacity, health-check failures, scheduling failures, latency, errors, and downstream saturation.

---

# 2. Cold Start — Service Takes Too Long to Start

Measure startup by phase rather than treating it as one number:

```text
Pod scheduled
→ image pulled
→ container created
→ JVM started
→ Spring context initialized
→ migrations completed
→ dependencies connected
→ readiness becomes healthy
```

## Common causes

- Large container image
- Slow image registry or cross-region pull
- Insufficient startup CPU
- Large Spring application context
- Excessive classpath scanning
- Unnecessary auto-configurations
- Thousands of beans
- Synchronous cache warm-up
- Database migration at every replica startup
- Slow DNS or secret retrieval
- Blocking calls in constructors or `@PostConstruct`
- External-service health checks during startup
- Entropy or certificate-loading delays

## Investigation

Record:

- Pod scheduling time
- Image-pull duration
- Container start time
- JVM start time
- `ApplicationStartedEvent`
- `ApplicationReadyEvent`
- Time until readiness succeeds

Inspect events:

```bash
kubectl describe pod <pod> -n <namespace>
kubectl get events -n <namespace> --sort-by=.lastTimestamp
```

Spring Boot exposes application lifecycle and Kubernetes availability states through Actuator, including readiness and liveness information. ([Home][6])

## Optimizations

### Reduce the image

Use:

- Multi-stage builds
- Runtime-only base image
- `.dockerignore`
- No Maven cache or source code in the runtime image
- Layered application dependencies
- Registry close to the cluster

Docker multi-stage builds allow build tools and intermediate files to remain outside the final image. ([Docker Documentation][7])

### Give startup sufficient CPU

A Pod with a very low CPU request or limit may experience slow class loading, bean creation, JIT compilation, and initialization.

Compare:

```text
Startup CPU throttling
Startup duration
Time until readiness
```

### Remove blocking initialization

Avoid:

```java
@PostConstruct
public void warmEverything() {
    loadEntireDatabase();
    callFiveExternalServices();
}
```

Instead:

- Load only critical data before readiness.
- Warm optional caches asynchronously.
- Apply a bounded timeout.
- Expose a degraded state where safe.
- Separate heavy initialization from request-serving capacity.

### Handle migrations carefully

For multiple replicas, avoid every Pod racing to perform the same large migration. Depending on the migration tooling and architecture, use:

- A dedicated migration Job
- Leader-controlled migration
- Backward-compatible migrations
- Separate deployment stage

### Use a startup probe

A startup probe prevents liveness checks from killing an application that is still legitimately initializing. Kubernetes does not begin ordinary liveness and readiness processing until the startup probe succeeds. ([Kubernetes][8])

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  periodSeconds: 5
  failureThreshold: 30
  timeoutSeconds: 2
```

## Interview-ready answer

> I break cold-start time into scheduling, image pull, JVM startup, Spring initialization, migrations, dependency setup, and readiness. I optimize the slowest phase rather than guessing. Common improvements are a smaller runtime image, sufficient startup CPU, reduced bean and classpath scanning, removal of blocking startup calls, controlled migrations, asynchronous cache warming, and a correctly sized startup probe.

---

# 3. Docker Container Keeps Crashing

## Initial Docker runbook

```bash
docker ps -a
docker logs --timestamps <container>
docker inspect <container>
docker stats <container>
```

`docker ps -a` includes stopped containers, `docker logs` retrieves container output, `docker inspect` displays detailed state, and `docker stats` streams live resource usage. ([Docker Documentation][9])

## Inspect termination state

```bash
docker inspect <container> \
  --format='Exit={{.State.ExitCode}} OOM={{.State.OOMKilled}} Error={{.State.Error}}'
```

Check:

- Exit code
- `OOMKilled`
- Restart count
- Health-check result
- Start and finish timestamps
- Entrypoint and command
- Environment
- Mounts
- Published ports
- Resource limits

An exit code such as `137` indicates termination by `SIGKILL`, but it is not proof by itself that the kernel killed the process for memory. Check `OOMKilled`, container events, and host kernel logs.

## Common causes

### Application failure

- Unhandled startup exception
- Invalid configuration
- Database unavailable
- Authentication failure
- Port conflict
- Missing file
- Java version mismatch
- Migration failure

### Container configuration

- Incorrect entrypoint
- Incorrect working directory
- Relative path failure
- Required environment variable missing
- Volume mounted over application files
- Read-only path used for writes
- Wrong CPU architecture

### Resource failure

- Memory limit exceeded
- Native-memory growth
- Excessive thread count
- Disk full
- File-descriptor exhaustion
- CPU starvation

### External termination

- Deployment replacement
- Host restart
- Container-health policy
- Orchestrator action
- Operator command

## Reproduce interactively

Override the entrypoint when the image contains a shell:

```bash
docker run --rm -it \
  --entrypoint sh \
  example/order-service:2.1.0
```

A minimal or distroless production image may not contain a shell. In that case, use a debug image, inspect the filesystem externally, or reproduce with a development variant rather than modifying production permanently.

## Verify the application is listening

```bash
docker exec <container> ps -ef
docker exec <container> ss -lntp
```

`docker exec` can run diagnostics only while the container’s primary process is still running. ([Docker Documentation][10])

Check that the service binds to:

```text
0.0.0.0:8080
```

rather than only:

```text
127.0.0.1:8080
```

## Interview-ready answer

> I inspect the stopped container, exit code, OOM flag, logs, health state, resource limits, command, environment, and mounts. I distinguish an application exception from an external kill or container misconfiguration. I then reproduce with the exact image and configuration, verify the process and listening port, and inspect host-level memory, disk, and runtime events.

---

# 4. Kubernetes Pod Keeps Restarting but Logs Are Empty

`CrashLoopBackOff` means the container repeatedly exits and Kubernetes is delaying subsequent restart attempts with backoff. It is a condition around repeated failure, not the actual root cause. ([Kubernetes][11])

## First commands

```bash
kubectl get pod <pod> -n <namespace> -o wide
kubectl describe pod <pod> -n <namespace>
kubectl logs <pod> -n <namespace> --previous
kubectl get events -n <namespace> --sort-by=.lastTimestamp
```

Kubernetes provides separate guidance for debugging failed and crashing Pods, including inspection of Pod state, events, and previous container logs. ([Kubernetes][12])

For multiple containers:

```bash
kubectl logs <pod> \
  -n <namespace> \
  -c <container> \
  --previous
```

## Inspect termination details

```bash
kubectl get pod <pod> -n <namespace> \
  -o jsonpath='{.status.containerStatuses[*].lastState.terminated}'
```

Look for:

```text
reason
exitCode
signal
startedAt
finishedAt
message
```

## Common reasons with no useful application logs

### `OOMKilled`

The kernel terminates the process before Java writes a normal exception.

Check:

```bash
kubectl top pod <pod> -n <namespace>
kubectl describe pod <pod> -n <namespace>
```

Also inspect:

- Container memory limit
- JVM heap sizing
- Direct buffers
- Thread count
- Metaspace
- Native libraries
- Process RSS

### Liveness-probe failure

The application may be alive but temporarily slow.

Inspect Pod events for:

```text
Liveness probe failed
Readiness probe failed
Startup probe failed
```

Check whether the probe:

- Starts too early
- Has too short a timeout
- Calls a slow dependency
- Uses an incorrect path or port
- Requires authentication
- Is affected by GC pauses

### Entrypoint failure

The process may fail before logging initializes:

- Executable not found
- Permission denied
- Invalid shell syntax
- Missing dynamic library
- Wrong architecture
- Invalid JVM option

### Config or secret mounting failure

Inspect:

```bash
kubectl describe pod <pod> -n <namespace>
kubectl get configmap -n <namespace>
kubectl get secret -n <namespace>
```

### Node eviction or disruption

A Pod may disappear because of:

- Node memory pressure
- Disk pressure
- Node shutdown
- Deployment replacement
- Taint or drain
- Preemption

## Improve future evidence

- Write fatal startup errors to standard error.
- Configure termination messages.
- Preserve previous container logs centrally.
- Enable automatic heap dumps for justified OOM investigations.
- Store dumps on persistent or external storage.
- Emit deployment and configuration versions.
- Capture Kubernetes events.

Kubernetes termination messages provide a dedicated mechanism for surfacing fatal container information through Pod status and monitoring tools. ([Kubernetes][13])

---

# 5. Optimize Docker Image Size

## Use a multi-stage build

```dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /application

COPY --from=build \
  /workspace/target/order-service.jar \
  application.jar

USER 10001

ENTRYPOINT ["java", "-jar", "application.jar"]
```

The final stage contains the runtime and application artifact, not Maven, source code, test files, or the complete build cache. ([Docker Documentation][7])

## Add `.dockerignore`

```text
.git
.idea
.vscode
target
build
*.log
*.hprof
README.md
docker-compose.yml
```

## Additional techniques

- Use a runtime image rather than a JDK image.
- Remove package-manager caches.
- Avoid unnecessary OS packages.
- Copy only the required artifact.
- Use layered Spring Boot images where useful for cache reuse.
- Pin and update trusted base images.
- Avoid adding debugging tools to every production image.
- Build separate debug images when operationally required.

Image size is not the only objective. A tiny but unsupported, unpatchable, or operationally opaque image may be a worse production choice.

---

# 6. Manage Environment Variables in Containers

## Classify configuration

### Non-sensitive configuration

Examples:

```text
LOG_LEVEL
PAYMENT_TIMEOUT
FEATURE_ENABLED
DATABASE_HOST
KAFKA_TOPIC
```

Use:

- Environment variables
- Kubernetes ConfigMaps
- Spring Cloud Config
- Versioned deployment configuration

### Sensitive configuration

Examples:

```text
DATABASE_PASSWORD
OAUTH_CLIENT_SECRET
API_KEY
PRIVATE_KEY
```

Use:

- External secret manager
- Vault
- Cloud secret manager
- Secrets Store CSI
- External Secrets Operator
- Kubernetes Secret with encryption and strict RBAC

## Kubernetes example

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: production

  - name: PAYMENT_TIMEOUT
    valueFrom:
      configMapKeyRef:
        name: order-service-config
        key: payment-timeout

  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: order-service-database
        key: password
```

## Good practices

- Validate required configuration at startup.
- Use typed configuration objects.
- Define safe defaults only where appropriate.
- Never log the complete environment.
- Separate configuration by environment.
- Record configuration version, not values.
- Avoid putting secrets in Docker `ARG` or image layers.
- Restart or safely refresh applications after configuration changes.

Docker recommends build-secret mounts rather than build arguments or environment variables for secrets required during the image build. ([Docker Documentation][14])

---

# 7. How Will You Scale Services in Kubernetes?

Use the demand signal that best represents the workload.

## Request-driven API

Possible signals:

- CPU
- Request rate
- Concurrent requests
- P95 latency
- Active connections

## Event consumer

Possible signals:

- Kafka lag
- Queue depth
- Oldest-message age
- Pending jobs

## HPA example

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
      policies:
        - type: Percent
          value: 25
          periodSeconds: 60
```

Kubernetes HPA supports resource, container, custom, and external metrics. For resource-utilization targets, resource requests are part of the utilization calculation. ([Kubernetes][15])

## Capacity planning

Before increasing replicas, verify downstream capacity:

```text
20 Pods
× 20 DB connections each
=
400 possible DB connections
```

Scaling the application can overload:

- Database
- Redis
- Third-party API
- Kafka partitions
- File storage
- Authentication server

Use:

- Per-Pod connection limits
- Global capacity budgets
- Bulkheads
- Rate limiting
- Backpressure
- Load shedding

## Scale-up latency

Account for:

```text
Metric collection
+ HPA evaluation
+ Pod scheduling
+ node provisioning
+ image pull
+ application startup
+ readiness delay
```

For sudden traffic, keep a suitable minimum replica count or use predictive/scheduled capacity when appropriate.

---

# 8. Handle Pod Failures

A resilient service assumes individual Pods will fail.

## Platform controls

- Deployment or StatefulSet controller
- Multiple replicas
- Readiness probe
- Liveness probe
- Startup probe
- PodDisruptionBudget
- Topology spread
- Anti-affinity
- Node health management

Kubernetes probes let the kubelet restart unhealthy containers or stop routing traffic to Pods that are not ready. ([Kubernetes][11])

## Application controls

- Stateless request handling
- External durable state
- Idempotent writes
- Message-redelivery safety
- Graceful shutdown
- Durable checkpoints
- Timeouts and retries
- No process-local singleton assumptions

## Failure example

```text
Pod processes payment
→ provider completes charge
→ Pod crashes before returning response
```

The replacement Pod cannot determine the result from memory. Use:

- Stable payment reference
- Idempotency key
- Durable state
- Provider-status lookup
- Reconciliation

## Interview-ready answer

> Kubernetes restarts or replaces failed Pods, but the application must make replacement safe. I run multiple replicas, use readiness, liveness and startup probes, spread replicas across failure domains, and protect voluntary disruption with a PDB. Application state is externalized, writes are idempotent, consumers tolerate redelivery, and incomplete workflows are recoverable from durable state.

---

# 9. Implement Rolling Deployments

## Deployment configuration

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
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
      app: order-service

  template:
    metadata:
      labels:
        app: order-service
    spec:
      terpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63: 45

      containers:
        - name: order-service
          image: registry.example.com/order-service:2.4.0

          ports:
            - containerPort: 8080

          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 5
            failureThreshold: 30

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
            failureThreshold: 2

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
            failureThreshold: 3

          lifecycle:
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - sleep 5
```

A Deployment rolling update incrementally replaces old Pods with new Pods, and `maxUnavailable` and `maxSurge` control availability and temporary extra capacity. ([Kubernetes][16])

## Required conditions for zero downtime

### Sufficient replicas

One replica cannot provide meaningful availability while it is being replaced unless temporary surge capacity starts successfully before termination.

### Readiness must represent real serving capability

Do not mark the Pod ready before:

- Server is listening
- Required initialization is complete
- Routing-critical local state is ready

### Graceful termination

Old Pods must stop taking new traffic and finish bounded in-flight work.

### Backward-compatible API

Old and new versions run simultaneously.

### Backward-compatible database migration

Use:

```text
Expand
→ deploy compatible code
→ migrate/backfill
→ switch usage
→ contract later
```

Do not drop or rename a column before all old Pods are gone.

### Backward-compatible events

Consumers must tolerate old and new event schemas during rollout.

### Cluster surge capacity

`maxSurge: 1` is ineffective when no node can schedule the additional Pod.

## Observe rollout

```bash
kubectl rollout status deployment/order-service
kubectl get replicasets
kubectl get pods -w
```

Rollback:

```bash
kubectl rollout undo deployment/order-service
```

Application rollback remains safe only when data, event, and configuration changes are backward-compatible.

---

# 10. In-Flight Requests During Pod Termination

Kubernetes begins termination, executes `preStop`, sends `SIGTERM`, waits for the termination grace period, and eventually uses `SIGKILL` if the process has not exited. Spring Boot can move readiness to refusing traffic and process existing requests during graceful shutdown when configured appropriately. ([Home][17])

## Recommended flow

```text
Pod marked terminating
        ↓
Endpoint removal begins
        ↓
preStop delay allows routing to converge
        ↓
SIGTERM reaches JVM
        ↓
Spring stops accepting new work
        ↓
Bounded in-flight requests complete
        ↓
Resources close
        ↓
Process exits before grace period
```

## Spring Boot configuration

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

management:
  endpoint:
    health:
      probes:
        enabled: true
```

Kubernetes:

```yaml
spec:
  terpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63: 45
```

Ensure:

```text
terpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63
>
preStop duration
+
Spring shutdown duration
+
operational margin
```

Spring Boot’s current Kubernetes deployment guidance notes that traffic removal and shutdown happen concurrently, creating a window in which traffic might still reach a terminating Pod; a suitable `preStop` delay can reduce that risk. ([Home][18])

## Long-running operations

Do not depend solely on graceful shutdown because:

- Node failure may be immediate.
- The grace period can expire.
- The process can be OOM-killed.
- An operator can force deletion.
- The underlying host can fail.

Therefore, long-running and side-effecting work must be:

- Idempotent
- Checkpointed
- Resumable
- Reconciled
- Safe under duplicate delivery

---

# 11. Monitor Container and Kubernetes Health

## Probe health

- Startup-probe status
- Readiness failures
- Liveness failures
- Restart count
- Time to readiness

## Resource health

- CPU usage
- CPU throttling
- Memory working set
- RSS
- OOM kills
- Filesystem usage
- Network errors
- File-descriptor usage

## JVM health

- Heap after GC
- Allocation rate
- GC pause
- Thread count
- Deadlocks
- Direct-buffer memory
- Metaspace
- Process CPU

## Service health

Use RED:

```text
Rate
Errors
Duration
```

## Resource health

Use USE:

```text
Utilization
Saturation
Errors
```

## Kubernetes health

- Desired versus available replicas
- Pending Pods
- Unschedulable Pods
- Node pressure
- Deployment rollout progress
- Endpoint availability
- HPA desired replica count
- Container restarts
- Kubernetes events

## Business health

- Successful orders
- Payment success rate
- Stuck transactions
- Consumer backlog
- Failed compensations
- Reconciliation mismatches

Spring Boot Actuator provides production-management endpoints and metrics that can be connected to an external monitoring system. ([Home][6])

---

# 12. Debug Kubernetes Networking

Use a layered approach.

```text
Application
→ local listener
→ Pod network
→ DNS
→ Service
→ EndpointSlice
→ NetworkPolicy
→ ingress/mesh/load balancer
→ external dependency
```

## Step 1: Verify the application listener

Inside the Pod:

```bash
kubectl exec -it <pod> -n <namespace> -- \
  sh
```

Then:

```bash
ss -lntp
```

Verify:

- Correct port
- Binding to `0.0.0.0`
- Correct protocol
- Process is running

For an image without debugging tools, use an ephemeral debug container:

```bash
kubectl debug -it <pod> \
  -n <namespace> \
  --image=nicolaka/netshoot
```

Kubernetes documents `kubectl debug` and ephemeral containers as tools for debugging running Pods, including images that lack a shell or diagnostic utilities. ([Kubernetes][12])

## Step 2: Test Pod-to-Pod connectivity

```bash
curl -v http://<pod-ip>:8080/actuator/health
```

If direct Pod connectivity fails, inspect:

- CNI
- Node routing
- Security groups
- Host firewall
- NetworkPolicy
- Service mesh

## Step 3: Verify DNS

```bash
nslookup inventory-service
nslookup inventory-service.inventory.svc.cluster.local
```

Check:

- Service name
- Namespace
- CoreDNS health
- `/etc/resolv.conf`
- DNS egress policy

## Step 4: Inspect the Service

```bash
kubectl get service inventory-service \
  -n inventory \
  -o yaml
```

Verify:

- Selector
- `port`
- `targetPort`
- Protocol
- Service type

A Kubernetes Service provides a stable endpoint over one or more changing backend Pods and is a core service-discovery mechanism. ([Kubernetes][19])

## Step 5: Inspect EndpointSlices

```bash
kubectl get endpointslices \
  -n inventory \
  -l kubernetes.io/service-name=inventory-service
```

No endpoints usually means:

- Service selector does not match Pod labels.
- Pods are not ready.
- Workload is absent.
- Endpoint controller issue.

## Step 6: Inspect NetworkPolicy

```bash
kubectl get networkpolicy -A
kubectl describe networkpolicy \
  <policy> \
  -n <namespace>
```

Check both:

```text
Source egress
AND
destination ingress
```

Also ensure DNS is allowed when default-deny egress is enabled.

## Step 7: Inspect ingress, proxy, or service mesh

Check:

- Ingress route
- Gateway listener
- TLS certificate
- Host header
- Path rewrite
- Backend port
- Proxy timeout
- Mesh authorization policy
- mTLS mode
- Sidecar readiness

## Interview-ready answer

> I debug networking from the application outward. I verify the process is listening on the expected interface and port, test Pod IP connectivity, validate DNS, inspect the Service selector and target port, confirm EndpointSlices contain ready backends, and review ingress and egress NetworkPolicies. I then inspect the ingress controller, service mesh, CNI, node routing, and cloud security rules.

---

# 13. Secure Containerized Applications

## Build security

- Use trusted base images.
- Pin and update dependencies.
- Scan images for vulnerabilities.
- Generate an SBOM.
- Sign and verify images.
- Do not embed secrets.
- Keep build and runtime stages separate.
- Restrict registry access.

## Runtime security

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 10001
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true

  capabilities:
    drop:
      - ALL

  seccompProfile:
    type: RuntimeDefault
```

Kubernetes security contexts control runtime privilege and access settings, while the Restricted Pod Security Standard favors non-root execution, limited capabilities, and restricted privilege escalation. ([Kubernetes][20])

Avoid privileged containers. Kubernetes warns that privileged mode overrides or weakens multiple kernel-security controls and grants broad capabilities. ([Kubernetes][21])

## Kubernetes controls

- Least-privilege ServiceAccount
- Disable automatic token mounting when unnecessary
- Namespace isolation
- Pod Security Admission
- Default-deny NetworkPolicies
- Secret encryption
- Resource requests and limits
- Read-only root filesystem
- Restricted volume mounts
- Admission policy
- Runtime detection

## Application security

- OAuth 2.0/OIDC
- mTLS for workload identity
- Local service authorization
- Input validation
- Output encoding
- Rate limiting
- SSRF protection
- Idempotency
- Safe structured logging
- Actuator endpoint protection

## Example ServiceAccount configuration

```yaml
spec:
  serviceAccountName: order-service
  automountServiceAccountToken: false
```

Kubernetes ServiceAccounts provide identities for Pod processes; grant only the API permissions actually required. ([Kubernetes][22])

---

# 14. Deploy Microservices in the Cloud

## Reference deployment pipeline

```text
Developer commit
        ↓
CI tests
        ↓
SAST/SCA/secret scan
        ↓
Build immutable image
        ↓
Image vulnerability scan
        ↓
Push registry
        ↓
Deploy to staging
        ↓
Integration and load tests
        ↓
Canary or rolling deployment
        ↓
Automated health analysis
        ↓
Production promotion
```

## Cloud architecture

```text
DNS
 ↓
CDN/WAF
 ↓
Load Balancer / API Gateway
 ↓
Kubernetes or ECS
 ↓
Microservice replicas
 ↓
Managed databases, caches, queues
```

## Required platform capabilities

- Infrastructure as code
- Multiple Availability Zones
- Managed load balancing
- Container registry
- Centralized configuration
- Secret management
- Auto-scaling
- Centralized logs, metrics, and traces
- Backups and restore tests
- Disaster-recovery plan
- Network segmentation
- IAM and workload identity
- Cost and quota monitoring

## Deployment decision

| Requirement                        | Suitable strategy                       |
| ---------------------------------- | --------------------------------------- |
| Simple, low-risk update            | Rolling update                          |
| Fast full cutover and rollback     | Blue-green                              |
| Limit initial blast radius         | Canary                                  |
| Request-by-request experimentation | Weighted or targeted routing            |
| Database-breaking change           | Redesign as compatible staged migration |

---

# 15. Kubernetes Service Discovery

A Kubernetes Service selects backend Pods and exposes a stable virtual endpoint and DNS name. The individual Pod IP addresses may change as Pods restart or scale. ([Kubernetes][19])

## Service example

```yaml
apiVersion: v1
kind: Service
metadata:
  name: inventory-service
  namespace: inventory
spec:
  selector:
    app: inventory-service

  ports:
    - name: http
      port: 8080
      targetPort: 8080
```

Call from the same namespace:

```text
http://inventory-service:8080
```

Call from another namespace:

```text
http://inventory-service.inventory:8080
```

Fully qualified:

```text
http://inventory-service.inventory.svc.cluster.local:8080
```

## Flow

```text
Order Service
        ↓ DNS
inventory-service.inventory
        ↓
Kubernetes Service
        ↓
Ready Inventory Pod
```

Readiness is important because unready Pods should not receive ordinary Service traffic.

## Service discovery does not replace resilience

The selected Pod can fail immediately after selection. Use:

- Connection timeout
- Response timeout
- Circuit breaker
- Safe bounded retry
- Idempotency for writes

---

# 16. Manage Configuration and Secrets

## ConfigMap for ordinary configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-service-config
data:
  PAYMENT_TIMEOUT: "2s"
  FEATURE_NEW_CHECKOUT: "false"
```

## Secret reference

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: order-service-database
type: Opaque
stringData:
  username: order_app
  password: replace-through-secret-management
```

## Deployment usage

```yaml
envFrom:
  - configMapRef:
      name: order-service-config

env:
  - name: DATABASE_USERNAME
    valueFrom:
      secretKeyRef:
        name: order-service-database
        key: username

  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: order-service-database
        key: password
```

## Production recommendations

- Keep ConfigMaps for non-sensitive values.
- Keep secrets in a dedicated secret-management system where possible.
- Use workload identity.
- Rotate credentials automatically.
- Do not log configuration values.
- Validate configuration before readiness.
- Version important configuration.
- Roll back bad configuration.
- Avoid making every property dynamically refreshable.
- Rebuild clients or connection pools when rotated credentials require it.

---

# Consolidated Scenario Answers

## How will you implement auto-scaling?

> I choose a metric proportional to demand, configure realistic resource requests, define minimum and maximum capacity, and tune scale-up and scale-down behavior. For request-driven services, HPA may use CPU or request metrics. For consumers, I prefer queue depth or Kafka lag through KEDA or external metrics. I also ensure node capacity can grow and verify that scaling the application will not overload the database or downstream systems.

## How will you handle Pod crashes?

> I inspect Pod status, termination reason, previous logs, events, probe failures, memory limits, and node state. I then correct the root cause rather than simply increasing restart limits. The service runs multiple replicas, externalizes state, uses idempotent processing, and exposes appropriate startup, readiness, and liveness probes.

## How will you design rolling updates?

> I run multiple replicas, set `maxUnavailable: 0` and controlled surge capacity, use startup and readiness probes, and require new Pods to remain ready before progression. Old Pods use graceful shutdown. APIs, events, configuration, and database schemas remain compatible while old and new versions coexist.

## How will you monitor Kubernetes applications?

> I monitor infrastructure, Kubernetes, JVM, service, dependency, and business signals. This includes Pod restarts, availability, pending Pods, CPU throttling, memory, GC, request rate, error rate, P99 latency, connection-pool waiting, Kafka lag, circuit states, and business workflow outcomes. Logs, metrics, and traces share deployment and trace context.

## How will you manage secrets?

> Services authenticate through workload identity and retrieve only the secrets they require from a secret manager. Kubernetes Secrets are protected with encryption and least-privilege RBAC when used. Credentials are not placed in Git, images, logs, or build arguments, and rotation is automated and tested without downtime.

---

# Production Investigation Runbook

```text
1. Define user impact and time window.
2. Identify the affected image and deployment version.
3. Check workload, Pod, and container status.
4. Read current and previous logs.
5. Inspect termination reason and events.
6. Check probes and resource limits.
7. Inspect CPU, memory, disk, and network.
8. Verify Service, EndpointSlice, DNS, and NetworkPolicy.
9. Check configuration, secrets, and dependencies.
10. Compare healthy and unhealthy instances.
11. Mitigate using rollback, traffic control, or scaling.
12. Preserve evidence and complete root-cause analysis.
```

# Senior Interview Summary

> I treat containers and Pods as disposable execution units and keep business state durable outside them. Every service has resource requests and limits, startup, readiness, and liveness probes, structured logs, metrics, traces, and graceful shutdown. Kubernetes Services provide stable discovery, while NetworkPolicies, workload identity, mTLS, and service-level authorization secure communication.
>
> Auto-scaling uses a demand metric that reflects actual work rather than blindly relying on CPU. I distinguish scaling decisions from capacity-launch failures and ensure that Pod scaling, node scaling, and downstream capacity are aligned. Deployments use multiple replicas, controlled surge, readiness-based promotion, graceful termination, and backward-compatible data and contracts.
>
> During failures, I inspect container termination state, previous logs, Kubernetes events, probe results, resource pressure, DNS, Services, EndpointSlices, NetworkPolicies, and cloud infrastructure. I avoid repeated restarts or arbitrary resource increases until evidence identifies the actual failure boundary.

[1]: https://docs.aws.amazon.com/autoscaling/ec2/userguide/CHAP_Troubleshooting.html?utm_source=chatgpt.com "Troubleshoot issues in Amazon EC2 Auto Scaling"
[2]: https://docs.aws.amazon.com/autoscaling/ec2/userguide/CHAP_Troubleshooting.html "Troubleshoot issues in Amazon EC2 Auto Scaling - Amazon EC2 Auto Scaling"
[3]: https://docs.aws.amazon.com/autoscaling/ec2/userguide/as-scaling-target-tracking.html?utm_source=chatgpt.com "Target tracking scaling policies for Amazon EC2 Auto Scaling"
[4]: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html?utm_source=chatgpt.com "Automatically scale your Amazon ECS service"
[5]: https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/ "Horizontal Pod Autoscaling | Kubernetes"
[6]: https://docs.spring.io/spring-boot/reference/actuator/index.html?utm_source=chatgpt.com "Production-ready Features"
[7]: https://docs.docker.com/build/building/multi-stage/?utm_source=chatgpt.com "Multi-stage builds"
[8]: https://v1-33.docs.kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/?utm_source=chatgpt.com "Configure Liveness, Readiness and Startup Probes"
[9]: https://docs.docker.com/reference/cli/docker/container/ls/?utm_source=chatgpt.com "docker container ls"
[10]: https://docs.docker.com/reference/cli/docker/container/exec/?utm_source=chatgpt.com "docker container exec"
[11]: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/?utm_source=chatgpt.com "Pod Lifecycle"
[12]: https://kubernetes.io/docs/tasks/debug/debug-application/debug-running-pod/?utm_source=chatgpt.com "Debug Running Pods"
[13]: https://kubernetes.io/docs/tasks/debug/debug-application/determine-reason-pod-failure/?utm_source=chatgpt.com "Determine the Reason for Pod Failure"
[14]: https://docs.docker.com/build/building/secrets/?utm_source=chatgpt.com "Build secrets"
[15]: https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/?utm_source=chatgpt.com "Horizontal Pod Autoscaling"
[16]: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/?utm_source=chatgpt.com "Deployments"
[17]: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html "Endpoints :: Spring Boot"
[18]: https://docs.spring.io/spring-boot/how-to/deployment/cloud.html "Deploying to the Cloud :: Spring Boot"
[19]: https://kubernetes.io/docs/concepts/services-networking/service/?utm_source=chatgpt.com "Service"
[20]: https://kubernetes.io/docs/tasks/configure-pod-container/security-context/?utm_source=chatgpt.com "Configure a Security Context for a Pod or Container"
[21]: https://kubernetes.io/docs/concepts/security/linux-kernel-security-constraints/?utm_source=chatgpt.com "Linux kernel security constraints for Pods and containers"
[22]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/?utm_source=chatgpt.com "Configure Service Accounts for Pods"
