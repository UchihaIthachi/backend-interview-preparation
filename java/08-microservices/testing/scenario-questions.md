# Scenario-Based Questions — CI/CD, Jenkins, Rollbacks, and Testing

## Important Corrections

- A failed deployment is not always a compilation failure. The artifact may build successfully and then fail during packaging, image creation, configuration loading, database migration, infrastructure provisioning, or health verification.
- Rolling back application code does **not** automatically roll back database or event-schema changes.
- The safest pipeline builds an immutable artifact once and promotes the same artifact digest through environments instead of rebuilding separately for test, staging, and production.
- Caching can accelerate builds, but stale or incorrectly keyed caches can produce invalid results.
- Parallel execution improves only independent stages. Parallelizing tests that share databases, ports, files, or static state can increase flakiness.
- High line coverage does not prove strong tests. Assertions, boundary cases, failure paths, and mutation resistance matter more than achieving an arbitrary percentage.
- Blue-green and rolling deployments support availability only when readiness checks, capacity, graceful shutdown, and backward-compatible data changes are correctly designed.

---

# 1. Jenkins Deployment Fails After a Merge

## Scenario

A Jenkins pipeline fails after your code is merged. You need to determine whether the failure is caused by:

- Source code or compilation
- Dependency resolution
- Tests
- Docker image construction
- Configuration
- Infrastructure
- Deployment
- Runtime health verification

## First Response

Do not immediately rerun the job repeatedly.

Start by recording:

```text
Pipeline name
Build number
Commit SHA
Branch
Artifact version
Target environment
Failed stage
First failing timestamp
Previous successful build
Application/configuration version
```

Then determine whether production was affected:

```text
Did deployment start?
Did any traffic reach the new version?
Did a database migration execute?
Did configuration change?
Are old instances still healthy?
```

---

## Step 1: Find the First Real Failure

Jenkins often displays multiple later failures caused by one earlier error.

Example:

```text
Compile failed
→ JAR not created
→ Docker build failed
→ deployment stage skipped
```

The Docker failure is not the root cause.

Inspect:

- The first failed stage
- The first meaningful exception
- Exit code
- Test report
- Archived artifacts
- Jenkins agent logs
- Deployment events

A Jenkins Pipeline is normally organized into stages and steps, which makes it possible to isolate where the process failed. Declarative Pipelines also support sequential, parallel, and matrix execution structures. ([Jenkins][1])

---

## Build Issue vs Dependency Issue vs Environment Issue

| Evidence                                            | Likely category                                         |
| --------------------------------------------------- | ------------------------------------------------------- |
| Java compilation error                              | Build/source issue                                      |
| Unit-test assertion failure                         | Code or test issue                                      |
| Maven dependency cannot be downloaded               | Dependency/repository issue                             |
| Dependency checksum or authentication failure       | Repository/configuration issue                          |
| Build passes locally but fails on every clean agent | Missing undeclared dependency                           |
| Docker build cannot locate JAR                      | Packaging or workspace issue                            |
| Deployment manifest validation fails                | Deployment configuration issue                          |
| Pod starts but crashes                              | Runtime/configuration issue                             |
| Pod remains `Pending`                               | Kubernetes capacity/scheduling issue                    |
| New version starts but readiness fails              | Application or dependency issue                         |
| Only production fails                               | Environment, secret, data, network, or scale difference |

---

## Step 2: Reproduce the Exact Build

Use the same:

- Commit
- JDK version
- Maven or Gradle version
- Container image
- Build command
- Dependency repository
- Environment-independent build arguments

Example:

```bash
git checkout <failed-commit>

java -version
./mvnw -version

./mvnw clean verify
```

Prefer the project wrapper:

```bash
./mvnw
./gradlew
```

rather than depending on whatever Maven or Gradle happens to be installed on the Jenkins agent.

For Maven dependency diagnosis:

```bash
./mvnw -U -e -X clean verify
```

Check:

- Repository authentication
- Proxy configuration
- Expired credentials
- Missing internal artifact
- Snapshot replacement
- Dependency conflict
- Network or DNS failure
- Corrupted local cache
- Repository outage

Do not delete the complete Jenkins dependency cache as the first response. That can hide evidence and slow every subsequent build.

---

## Step 3: Compare with the Last Successful Build

Compare:

```text
Failed commit
Previous successful commit

JDK version
Build-tool version
Dependency lock or POM
Docker base image digest
Jenkins agent image
Environment configuration
Secrets
Deployment manifests
Database migration scripts
```

Useful commands:

```bash
git diff <last-good-commit>..<failed-commit>
```

```bash
git log --oneline <last-good-commit>..<failed-commit>
```

Also compare the Jenkinsfile itself. A build can fail because the pipeline changed even when application code did not.

---

## Step 4: Determine Whether It Is an Environment Issue

Check whether the same immutable artifact works in another environment.

```text
Same artifact fails everywhere
→ likely artifact/application issue

Same artifact works in staging but fails in production
→ likely configuration, data, permissions, network, capacity, or dependency difference
```

Inspect:

- Environment variables
- Secret versions
- IAM or service-account permissions
- Security groups and NetworkPolicies
- Database schema version
- Available CPU and memory
- DNS resolution
- External endpoints
- Certificate trust
- File-system permissions
- Feature flags
- Production data characteristics

Do not print all environment variables or secrets into Jenkins logs.

---

# Safe Rollback Procedure

## 1. Stop Further Promotion

- Disable automatic promotion.
- Pause the rollout.
- Prevent additional production stages from running.
- Preserve logs and artifacts.

## 2. Assess What Changed

```text
Application image
Configuration
Secrets
Database schema
Feature flags
Kafka/event schemas
Infrastructure
```

## 3. Restore the Last Known-Good Application

For Kubernetes:

```bash
kubectl rollout history deployment/order-service
```

```bash
kubectl rollout undo deployment/order-service
```

```bash
kubectl rollout status deployment/order-service
```

Kubernetes Deployments retain rollout revisions and support rolling updates and rollback to a prior revision. ([Kubernetes][2])

## 4. Verify Recovery

Check:

- Ready replicas
- Error rate
- P95 and P99 latency
- Business transaction success
- Database health
- Consumer lag
- Failed jobs
- Reconciliation state

## 5. Handle Database Changes Carefully

Application rollback may fail when the new deployment has already:

- Deleted a column
- Renamed a table
- Changed data representation
- Published an incompatible event
- Written data the old version cannot read

Use **expand–migrate–contract**:

```text
Release 1:
Add backward-compatible schema.

Release 2:
Deploy code supporting old and new schema.

Release 3:
Migrate data and switch reads.

Later:
Remove obsolete schema.
```

## Interview-Ready Answer

> I identify the first failed stage and classify the failure as source, test, dependency, packaging, deployment, configuration, or runtime. I reproduce the exact commit with the same toolchain and compare it with the previous successful build. If production is affected, I stop further promotion, determine whether data or schema changed, and restore the last immutable known-good artifact. I verify technical and business health after rollback and avoid destructive database changes that make application rollback impossible.

---

# 2. CI/CD Pipeline Takes 20 Minutes

## Step 1: Measure the Critical Path

Do not optimize from intuition.

Record the duration of:

```text
Checkout
Dependency download
Compilation
Unit tests
Static analysis
Integration tests
Docker build
Security scan
Artifact upload
Deployment
Smoke tests
```

Example:

| Stage                 |    Duration |
| --------------------- | ----------: |
| Checkout              |  30 seconds |
| Dependency resolution |   4 minutes |
| Compilation           |   2 minutes |
| Unit tests            |   3 minutes |
| Integration tests     |   7 minutes |
| Docker build          |   2 minutes |
| Security scan         | 1.5 minutes |

The critical path is the longest sequence of dependent stages—not necessarily the stage with the highest total CPU consumption.

---

## Use Parallel Execution

Independent checks can run concurrently:

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './mvnw -B -DskipTests package'
                stash name: 'application-jar',
                      includes: 'target/*.jar'
            }
        }

        stage('Quality Checks') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh './mvnw -B test'
                    }
                }

                stage('Static Analysis') {
                    steps {
                        sh './mvnw -B verify -DskipTests'
                    }
                }

                stage('Dependency Scan') {
                    steps {
                        sh './scripts/dependency-scan.sh'
                    }
                }
            }
        }
    }
}
```

Jenkins supports parallel stages, but shared state and limited agents can prevent real speed gains. Jenkins also documents `stash` as appropriate for relatively small files within one Pipeline run; large or reusable artifacts should normally go to an external artifact repository. ([Jenkins][3])

---

## Dependency Caching

Cache:

- Maven repository
- Gradle dependency cache
- Node package cache
- Docker build layers
- Static-analysis databases where supported
- Downloaded tool distributions

Cache keys should include relevant inputs:

```text
Operating system
JDK version
Build-tool version
pom.xml or lock-file hash
Architecture
```

Bad cache key:

```text
maven-cache
```

Better:

```text
maven-linux-jdk21-<pom-hash>
```

Do not cache:

- Generated secrets
- Environment-specific outputs
- Unverified compiled artifacts across unrelated commits
- Mutable dependencies without suitable invalidation

---

## Improve Docker Build Caching

Arrange stable layers before frequently changing files:

```dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests
```

Changing one Java source file should not force dependency resolution to rerun.

---

## Split the Pipeline by Feedback Speed

### Pull-request pipeline

Run:

- Compilation
- Unit tests
- Focused integration tests
- Formatting
- Static analysis
- Secret and dependency scanning

### Main-branch pipeline

Run:

- Full integration suite
- Contract tests
- Image build
- Artifact publication
- Staging deployment

### Scheduled pipeline

Run:

- Long performance tests
- Mutation tests
- Full security scans
- Large compatibility matrix
- Extended soak tests

Do not remove important tests merely to reduce duration. Move expensive tests to the earliest appropriate feedback point.

---

## Other Optimizations

- Reuse one compiled artifact across later stages.
- Avoid repeated `clean` operations when workspace isolation already guarantees correctness.
- Split large test suites across agents.
- Fix slow tests instead of only adding agents.
- Start Testcontainers concurrently where safe.
- Use pre-warmed agents.
- Keep agents near artifact repositories.
- Avoid downloading the same tools in every stage.
- Fail fast on compilation, formatting, or unit-test failures.
- Cancel obsolete builds after a newer commit arrives.

## Interview-Ready Answer

> I measure every stage and optimize the pipeline’s critical path. Independent tests and scans run in parallel, dependencies and Docker layers use correctly keyed caches, and the artifact is compiled once and promoted rather than rebuilt. Fast checks run on pull requests, broader integration tests run on the main branch, and long-running performance or mutation tests run on an appropriate scheduled pipeline.

---

# 3. How Do You Identify Which Commit Broke the Build?

## First Narrow the Range

Find:

```text
Last known-good commit
First known-bad commit
```

Review commits:

```bash
git log --oneline <good>..<bad>
```

For a small range, run the pipeline checks against each candidate commit.

For a large range, use `git bisect`.

```bash
git bisect start
git bisect bad <bad-commit>
git bisect good <good-commit>
```

Git checks out a midpoint commit. Test it and mark it:

```bash
git bisect good
```

or:

```bash
git bisect bad
```

Continue until Git identifies the first bad commit:

```bash
git bisect reset
```

`git bisect` performs a binary search between a known-good and known-bad revision to locate the commit that introduced a regression. ([Git][4])

---

## Automate the Search

Create a script that returns:

- `0` when the commit is good
- Non-zero when bad

```bash
#!/usr/bin/env bash
set -euo pipefail

./mvnw -B clean test
```

Then:

```bash
git bisect start <bad-commit> <good-commit>
git bisect run ./verify-build.sh
```

## Important Limitation

A commit may appear bad because of:

- External repository outage
- Expired secret
- Mutable dependency
- Changed Jenkins agent
- Time-dependent test
- Environment drift

Before blaming a commit, confirm the test is reproducible in a controlled environment.

---

# 4. Dev and Production Behave Differently

This is an environment-drift problem.

## Target Model

```text
Same source commit
+
same immutable artifact
+
versioned environment configuration
+
versioned infrastructure
=
predictable promotion
```

## Controls

### Build Once, Promote the Same Artifact

```text
Commit abc123
→ image digest sha256:...
→ test
→ staging
→ production
```

Do not rebuild the image for production after testing another image in staging.

### Infrastructure as Code

Manage through version control:

- Networks
- Kubernetes resources
- Load balancers
- IAM roles
- Databases
- Queues
- Autoscaling rules
- Monitoring
- Secrets references

### Configuration as Code

Version:

- Environment variables
- ConfigMaps
- Feature flags
- Deployment settings
- JVM flags

Keep secret values in a secret manager, while references and access policies remain version controlled.

### Production-Like Integration Tests

Use the same:

- Database engine
- Kafka version family
- Redis configuration
- Java runtime
- Container runtime assumptions
- Migrations
- Authentication flow

Spring Boot supports Testcontainers for integration tests that communicate with real backend services in Docker rather than simplified substitutes. ([Home][5])

### Drift Detection

Periodically compare declared and actual infrastructure.

```text
Declared configuration
vs
running cloud resources
```

Prevent manual production changes or require them to be captured back into code immediately.

---

# 5. Designing a Rollback Strategy

Rollback must cover more than the application image.

| Change type       | Rollback strategy                      |
| ----------------- | -------------------------------------- |
| Application image | Redeploy previous immutable artifact   |
| Configuration     | Restore previous version               |
| Feature           | Disable with controlled feature flag   |
| Database schema   | Prefer forward-compatible migration    |
| Data migration    | Restore/reconcile using explicit plan  |
| Event schema      | Maintain backward-compatible consumers |
| Infrastructure    | Apply prior IaC version carefully      |
| Secret rotation   | Maintain bounded overlap               |

## Pipeline Requirements

- Version every artifact.
- Retain deployment history.
- Record commit, image digest, configuration version, and migration version.
- Provide one controlled rollback action.
- Require health verification after rollback.
- Preserve incident evidence.
- Support automated rollback for clearly defined technical failures.
- Require human review when rollback could damage business data.

## Automatic Rollback Signals

Possible signals:

```text
Readiness failure
Error-rate threshold
P99 latency regression
Crash-loop increase
Business success-rate decline
Critical smoke-test failure
```

Do not auto-rollback solely because one non-critical metric briefly changes.

---

# 6. Stabilizing Flaky Tests

A flaky test produces different results without a meaningful code change.

## Common Causes

- Shared mutable state
- Fixed ports
- Reused database records
- Uncontrolled clock
- Random values without fixed seed
- Thread races
- Incorrect asynchronous waiting
- Test-order dependency
- External network dependency
- Resource exhaustion
- Leftover containers or files
- Overly strict timing assertion

---

## Investigation Process

1. Record the test name and failure signature.
2. Run it repeatedly in isolation.
3. Run the complete suite with the test.
4. Change test order.
5. Repeat under CI resource limits.
6. Capture random seed, timestamps, logs, thread dumps, and environment details.
7. Identify whether the product code or test itself contains a race.

Example repetition:

```bash
for i in $(seq 1 100); do
  ./mvnw -Dtest=PaymentServiceTest test || break
done
```

---

## Avoid Fixed Sleeps

Bad:

```java
Thread.sleep(5_000);

assertThat(repository.findById(id)).isPresent();
```

Better:

```java
await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(() ->
                assertThat(repository.findById(id))
                        .isPresent()
        );
```

## Make Time Deterministic

Inject `Clock`:

```java
public final class TokenService {

    private final Clock clock;

    public TokenService(Clock clock) {
        this.clock = clock;
    }

    public boolean isExpired(Instant expiry) {
        return expiry.isBefore(clock.instant());
    }
}
```

Test with:

```java
Clock fixedClock = Clock.fixed(
        Instant.parse("2026-07-24T08:00:00Z"),
        ZoneOffset.UTC
);
```

## Isolation Rules

- Give each test unique data.
- Reset databases predictably.
- Do not depend on test execution order.
- Close executors and clients.
- Avoid shared static mocks.
- Use random available ports.
- Stub external APIs.
- Pin container and dependency versions.

Quarantine should be temporary and visible. A quarantined test needs an owner and resolution deadline.

---

# 7. Improving Test Coverage Meaningfully

Do not begin with:

```text
We need 100% line coverage.
```

Begin with:

```text
Which failures would be expensive, dangerous, or difficult to detect?
```

## Prioritize

- Financial calculations
- Authorization
- Validation boundaries
- State transitions
- Retry classification
- Idempotency
- Error mapping
- Database constraints
- Kafka duplicate delivery
- Compensation logic
- Security-sensitive code

## Test Categories

For each behavior, test:

```text
Happy path
Boundary values
Invalid input
Dependency failure
Timeout
Duplicate request
Concurrent update
Authorization failure
Recovery path
```

## Coverage Metrics

Use:

- Line coverage
- Branch coverage
- Condition coverage
- Mutation score
- Critical requirement coverage

A test that executes a line without asserting its outcome can increase coverage while providing little defect protection.

---

# 8. How Will You Design a CI/CD Pipeline?

## Reference Pipeline

```text
Commit
  ↓
Checkout
  ↓
Compile
  ↓
Unit tests
  ↓
Static analysis and formatting
  ↓
Dependency/secret/security scan
  ↓
Package immutable artifact
  ↓
Integration and contract tests
  ↓
Publish artifact and SBOM
  ↓
Deploy to test/staging
  ↓
Smoke and migration verification
  ↓
Canary or blue-green production deployment
  ↓
Automated health analysis
  ↓
Promote or roll back
```

## Example Jenkinsfile

```groovy
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds(abortPrevious: true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                sh './mvnw -B -DskipTests package'
            }
        }

        stage('Quality') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh './mvnw -B test'
                    }
                }

                stage('Static Analysis') {
                    steps {
                        sh './scripts/static-analysis.sh'
                    }
                }

                stage('Security Scan') {
                    steps {
                        sh './scripts/security-scan.sh'
                    }
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh './mvnw -B verify -Pintegration'
            }
        }

        stage('Build Image') {
            steps {
                sh '''
                    docker build \
                      -t registry.example.com/order-service:${GIT_COMMIT} .
                '''
            }
        }

        stage('Publish') {
            steps {
                sh '''
                    docker push \
                      registry.example.com/order-service:${GIT_COMMIT}
                '''
            }
        }

        stage('Deploy Staging') {
            steps {
                sh './scripts/deploy.sh staging ${GIT_COMMIT}'
            }
        }

        stage('Smoke Test') {
            steps {
                sh './scripts/smoke-test.sh staging'
            }
        }

        stage('Deploy Production') {
            when {
                branch 'main'
            }

            steps {
                input message: 'Promote verified artifact?'
                sh './scripts/deploy.sh production ${GIT_COMMIT}'
            }
        }
    }

    post {
        always {
            junit 'target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true
        }
    }
}
```

Store credentials in Jenkins credentials management or an external secret manager and bind them only to the required stage. Never hard-code them in the Jenkinsfile.

---

# 9. Blue-Green Deployment

## Design

```text
Blue:
current production version

Green:
new version under validation
```

```text
Production Service
        ↓
Blue Pods

Preview Service
        ↓
Green Pods
```

Deployment process:

1. Deploy Green.
2. Wait for readiness.
3. Run smoke, integration, and business checks.
4. Switch production traffic to Green.
5. Monitor technical and business metrics.
6. Keep Blue temporarily available.
7. Roll back traffic to Blue when necessary.

## Advantages

- Fast traffic switch
- Fast application rollback
- Full environment available for validation
- Clear separation of versions

## Risks

- Extra capacity
- Shared database incompatibility
- Background consumers from both environments
- Duplicate scheduled jobs
- Long-lived connections
- Data written by Green may not be readable by Blue

## Kubernetes Concept

```yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
    deployment-color: green
  ports:
    - port: 8080
      targetPort: 8080
```

Changing the selector switches traffic, but a production-grade process should use controlled promotion tooling, verification, and auditability rather than casual manual edits.

---

# 10. Ensuring Zero-Downtime Deployment

## Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 4
  minReadySeconds: 15

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

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
```

A Kubernetes rolling update gradually replaces existing Pods and supports availability controls through `maxUnavailable` and `maxSurge`. ([Kubernetes][2])

## Additional Requirements

- At least two healthy replicas
- Enough capacity for surge Pods
- Correct readiness checks
- Graceful shutdown
- Bounded in-flight requests
- Backward-compatible APIs
- Backward-compatible event schemas
- Expand–migrate–contract database changes
- Externalized session state
- Idempotent write operations
- Safe scheduled-job ownership
- Rollback-compatible configuration

Kubernetes configuration alone cannot guarantee zero downtime when the application or database contract is incompatible.

---

# 11. Unit Tests for a Service Layer

## Production Code

```java
public final class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final NotificationService notificationService;

    public OrderService(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.notificationService = notificationService;
    }

    public Order createOrder(CreateOrderRequest request) {
        boolean available = inventoryClient.isAvailable(
                request.productId(),
                request.quantity()
        );

        if (!available) {
            throw new InsufficientInventoryException(
                    request.productId()
            );
        }

        Order order = new Order(
                request.customerId(),
                request.productId(),
                request.quantity()
        );

        Order saved = orderRepository.save(order);
        notificationService.orderCreated(saved.id());

        return saved;
    }
}
```

## Unit Test with Mockito

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderWhenInventoryIsAvailable() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "C-100",
                        "P-200",
                        2
                );

        when(inventoryClient.isAvailable("P-200", 2))
                .thenReturn(true);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.assignId("O-100");
                    return order;
                });

        Order result = orderService.createOrder(request);

        assertThat(result.id()).isEqualTo("O-100");

        verify(inventoryClient)
                .isAvailable("P-200", 2);

        verify(orderRepository)
                .save(any(Order.class));

        verify(notificationService)
                .orderCreated("O-100");

        verifyNoMoreInteractions(
                inventoryClient,
                orderRepository,
                notificationService
        );
    }

    @Test
    void shouldRejectOrderWhenInventoryIsUnavailable() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        "C-100",
                        "P-200",
                        2
                );

        when(inventoryClient.isAvailable("P-200", 2))
                .thenReturn(false);

        assertThatThrownBy(
                () -> orderService.createOrder(request)
        ).isInstanceOf(
                InsufficientInventoryException.class
        );

        verify(orderRepository, never())
                .save(any());

        verify(notificationService, never())
                .orderCreated(anyString());
    }
}
```

## What to Test

- Return value
- State changes
- Collaboration with dependencies
- Exception behavior
- No interaction when validation fails
- Boundary cases
- Duplicate/idempotent behavior

Avoid testing only whether mocks were called. Assert the meaningful business outcome as well.

---

# 12. How Will You Mock Dependencies Using Mockito?

## Stubbing

```java
when(repository.findById("O-100"))
        .thenReturn(Optional.of(order));
```

## Exception

```java
when(paymentClient.authorize(any()))
        .thenThrow(new PaymentProviderException());
```

## Verify Exactly Twice

```java
verify(notificationService, times(2))
        .sendNotification(anyString());
```

## Argument Capture

```java
ArgumentCaptor<Order> captor =
        ArgumentCaptor.forClass(Order.class);

verify(orderRepository).save(captor.capture());

assertThat(captor.getValue().customerId())
        .isEqualTo("C-100");
```

## `doThrow` for Void Methods

```java
doThrow(new NotificationException())
        .when(notificationService)
        .sendNotification("O-100");
```

## Principles

- Mock external collaborators, not value objects.
- Prefer constructor injection.
- Avoid mocking the class under test.
- Do not over-specify irrelevant interactions.
- Avoid deep stubs.
- Use real domain objects when inexpensive.
- Test observable behavior, not private implementation details.

---

# 13. Designing Integration Tests

Integration tests verify real boundaries such as:

- Database mappings
- Transactions
- Migrations
- Kafka serialization
- Redis behavior
- HTTP clients
- Security filters
- External-system contracts

## PostgreSQL Testcontainers Example

```java
@Testcontainers
@SpringBootTest
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17.5"
            );

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldPersistOrder() {
        Order order = new Order(
                "C-100",
                "P-200",
                2
        );

        Order saved = orderRepository.save(order);

        Order loaded = orderRepository
                .findById(saved.id())
                .orElseThrow();

        assertThat(loaded.customerId())
                .isEqualTo("C-100");
    }
}
```

Spring Boot integrates with Testcontainers and can obtain service connection details from supported containers using `@ServiceConnection`. ([Home][5])

## Integration Test Principles

- Use the production database engine where practical.
- Run real migrations.
- Use isolated test data.
- Pin container versions.
- Test constraints and transaction behavior.
- Stub only dependencies outside the boundary being tested.
- Poll asynchronous outcomes instead of sleeping.
- Keep a smaller number than unit tests.
- Clean up or uniquely namespace data.

---

# 14. Ensuring High Test Coverage

## Coverage Strategy

### Unit tests

Cover:

- Domain rules
- Calculations
- Validation
- Failure classification
- State transitions

### Slice tests

Cover:

- Controller status codes
- Request validation
- Serialization
- Security rules
- Exception mapping

### Integration tests

Cover:

- Database mappings
- Constraints
- Transactions
- Kafka
- Redis
- External HTTP client behavior

### Contract tests

Cover:

- Request and response schemas
- Event schemas
- Required headers
- Backward compatibility

### End-to-end tests

Cover only critical business workflows.

## Quality Gates

Example policy:

```text
No reduction in critical-module coverage
Minimum branch coverage for domain logic
No surviving high-risk mutation
Required tests for every production defect
No ignored test without issue and owner
```

Avoid forcing every generated DTO and configuration class to meet the same threshold as financial or authorization logic.

---

# Consolidated Interview Answers

## How do you debug a broken Jenkins deployment?

> I locate the first failing stage, reproduce the exact commit with the same toolchain, and classify the failure as compilation, dependency resolution, testing, packaging, deployment, configuration, or runtime. I compare it with the last successful build and verify whether the exact artifact behaves differently across environments.

## How do you optimize a slow CI/CD pipeline?

> I measure stage durations and the critical path, parallelize independent checks, cache dependencies and Docker layers with correct invalidation keys, compile the artifact once, split tests intelligently, and run long-running tests at the appropriate pipeline level.

## How do you isolate the breaking commit?

> I determine the last good and first bad commit, reproduce the failure, and use `git bisect`, preferably with an automated verification script, to locate the first commit that introduces the regression.

## How do you design rollback?

> Every deployment records an immutable artifact, commit, configuration version, and migration version. I support controlled rollback to the last known-good application and configuration, but database and event changes remain backward compatible so the old version can still operate.

## How do you stabilize flaky tests?

> I remove uncontrolled time, randomness, external systems, shared state, fixed ports, and arbitrary sleeps. I reproduce the failure repeatedly, capture diagnostic evidence, use polling assertions for asynchronous behavior, and ensure every test owns isolated data and resources.

## How do you improve coverage?

> I prioritize high-risk behavior, boundaries, failure paths, authorization, concurrency, and business invariants. I combine line and branch coverage with mutation testing and defect-based tests rather than pursuing 100% line coverage as an end in itself.

## Senior Interview Summary

> My CI/CD pipeline builds an immutable artifact once, runs fast unit and static checks early, executes integration and contract tests against realistic dependencies, scans the artifact, and promotes the exact same digest through staging and production. Independent stages run in parallel, while dependencies and Docker layers use correctly keyed caches.
>
> Production deployment uses rolling, canary, or blue-green promotion with readiness checks, graceful shutdown, backward-compatible database and event changes, technical and business verification, and a tested rollback path. A failed rollout is diagnosed from the first failing stage rather than from later cascading errors.
>
> Testing follows the pyramid: service logic is covered by focused JUnit and Mockito tests, framework boundaries by Spring slices, infrastructure by Testcontainers integration tests, and only critical workflows by end-to-end tests. Coverage is evaluated through behavior, branches, failure paths, and mutation resistance—not only a percentage.

[1]: https://www.jenkins.io/doc/book/pipeline/jenkinsfile/?utm_source=chatgpt.com "Using a Jenkinsfile"
[2]: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/?utm_source=chatgpt.com "Deployments"
[3]: https://www.jenkins.io/doc/pipeline/steps/workflow-basic-steps/?utm_source=chatgpt.com "Pipeline: Basic Steps"
[4]: https://git-scm.com/docs/git-bisect?utm_source=chatgpt.com "Git - git-bisect Documentation"
[5]: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html?utm_source=chatgpt.com "Testcontainers :: Spring Boot"
