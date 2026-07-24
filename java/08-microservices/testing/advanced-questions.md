# Advanced Testing Questions — Microservices and Spring Boot

## Important Corrections

- The **70% unit / 20% integration / 10% end-to-end** distribution is a guideline, not a mandatory formula.
- Contract tests do not necessarily form a fixed percentage or a separate pyramid layer. They protect communication boundaries and often replace many expensive end-to-end tests.
- `@SpringBootTest` does not always start a real HTTP server. Its default web environment is mocked; use `RANDOM_PORT` for a real embedded server. ([Home][1])
- Embedded Kafka is useful for focused tests, but Testcontainers generally provides better production fidelity.
- Avoid using `Thread.sleep()` to wait for asynchronous Kafka or Saga processing. Poll for an observable condition with a deadline.
- Testcontainers should use pinned image versions rather than floating tags such as `latest`.
- An end-to-end Saga test alone is insufficient. Each transition, event contract, idempotency rule, compensation, and recovery path should be tested separately.

---

# Q89: Explain the Test Pyramid for Microservices

The Test Pyramid recommends having many fast, isolated tests and progressively fewer slow, broad tests.

```text
                  /\
                 /  \
                / E2E\
               /------\
              /Contract\
             /----------\
            /Integration \
           /--------------\
          /   Unit Tests   \
         /__________________\
```

A practical microservice test strategy may include:

| Layer           | Scope                                               |     Speed |  Quantity |
| --------------- | --------------------------------------------------- | --------: | --------: |
| Unit            | One class or small collaboration                    | Very fast | Very high |
| Slice/component | One application layer or one service boundary       |      Fast |      High |
| Integration     | Real database, broker, cache, or filesystem         |  Moderate |    Medium |
| Contract        | Consumer-provider communication assumptions         |  Moderate |    Medium |
| End-to-end      | Complete business workflow across deployed services |      Slow |       Low |

The exact percentages depend on the system. A commonly quoted target is:

```text
Unit and narrow component tests:  60–75%
Integration and contract tests:   20–35%
End-to-end tests:                  5–10%
```

The important principle is not the precise number. It is to avoid an **ice-cream-cone strategy** where most validation depends on slow, fragile end-to-end tests.

---

## 1. Unit Tests

Unit tests verify business logic without starting Spring or external infrastructure.

Typical targets:

- Domain services
- Validators
- Calculators
- State machines
- Retry classification
- Mapping logic
- Idempotency decisions
- Saga transition rules

Example:

```java
class TransferFeeCalculatorTest {

    private final TransferFeeCalculator calculator =
            new TransferFeeCalculator();

    @Test
    void shouldApplyInternationalTransferFee() {
        Money fee = calculator.calculate(
                Money.of("USD", 1_000),
                TransferType.INTERNATIONAL
        );

        assertThat(fee)
                .isEqualTo(Money.of("USD", 15));
    }
}
```

Characteristics:

- No Spring context
- No network
- No database
- Deterministic
- Millisecond execution
- Clear failure location

Mock only true collaborators. Do not mock simple value objects or the class being tested.

---

## 2. Slice and Component Tests

Slice tests load a focused part of Spring.

Examples:

- `@WebMvcTest`
- `@WebFluxTest`
- `@DataJpaTest`
- `@JsonTest`
- `@RestClientTest`
- `@WebClientTest`

Spring Boot provides test slices because loading only the required application section is faster and more focused than loading all auto-configuration. ([Home][1])

A broader **component test** may run one entire microservice while replacing external services with controlled stubs.

```text
HTTP request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Testcontainer database

External payment provider
    ↓
WireMock
```

This tests the service as a deployable unit without starting the complete microservice platform.

---

## 3. Integration Tests

Integration tests verify interaction with real technology boundaries:

- PostgreSQL
- Oracle
- Kafka
- Redis
- S3-compatible storage
- Elasticsearch
- Filesystem
- Security provider
- Database migrations

Examples:

```text
Repository + real PostgreSQL
Kafka producer + broker + consumer
Redis cache + serialization
Flyway migration + production database engine
```

Testcontainers is often preferred because it starts real backend services in Docker and integrates with JUnit and Spring Boot. ([Home][2])

---

## 4. Contract Tests

Contract tests verify communication assumptions between a consumer and provider.

```text
Order Service expects:
GET /inventory/P-100

Response:
{
  "productId": "P-100",
  "available": true
}
```

The consumer publishes its expected interaction. The provider verifies that its current implementation still satisfies that interaction.

Pact is a code-first consumer-driven contract-testing tool. Its contracts are generated from consumer tests and verified by providers, allowing teams to detect incompatible changes without running the complete distributed environment. ([Pact Docs][3])

Contract tests can cover:

- HTTP requests and responses
- Message payloads
- Required headers
- Status codes
- Optional and mandatory fields
- Error contracts
- Event schema expectations

They do **not** prove:

- Database correctness
- Complete business workflow correctness
- Production networking
- Authentication infrastructure
- Performance
- Eventual consistency across all services

---

## 5. End-to-End Tests

End-to-end tests exercise a complete user or business flow.

```text
Create order
→ reserve inventory
→ authorize payment
→ confirm order
→ send notification
```

Use them for a small number of critical paths:

- Successful purchase
- Payment rejection
- Inventory shortage
- Refund
- User registration
- High-value approval workflow

End-to-end tests are expensive because they require:

- Multiple deployed services
- Stable test data
- Broker and database coordination
- Asynchronous waiting
- Environment management
- Complex diagnosis when a test fails

### Interview-ready answer

> The Test Pyramid recommends many fast unit and component tests, fewer integration and contract tests, and a small number of end-to-end tests. Unit tests validate business logic, slice tests validate focused Spring layers, integration tests use real infrastructure, contract tests protect service interfaces, and end-to-end tests validate only the most important deployed workflows. The 70/20/10 ratio is a heuristic rather than a rule.

---

# Q90: `@SpringBootTest` vs `@WebMvcTest`

## `@SpringBootTest`

`@SpringBootTest` loads the complete Spring Boot application context.

It can include:

- Controllers
- Services
- Repositories
- Security configuration
- Cache configuration
- Messaging configuration
- Application properties
- Auto-configuration

Use it when testing interactions across several layers.

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "C-100",
                                  "productId": "P-200",
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status")
                        .value("CREATED"));
    }
}
```

By default, `@SpringBootTest` uses a mock web environment and does not start a real embedded server. With `WebEnvironment.RANDOM_PORT`, it starts a real server on an available port. ([Home][1])

```java
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class OrderApiLiveServerTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldExposeHealthEndpoint() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        ResponseEntity<String> response = client.get()
                .uri("/actuator/health")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
```

Use `RANDOM_PORT` when you need to test:

- Real HTTP serialization
- Servlet container behavior
- Filters
- Network client configuration
- Actual server startup
- Full security chain
- Error handling across the stack

---

## `@WebMvcTest`

`@WebMvcTest` loads only the Spring MVC layer.

It normally includes:

- Selected controller
- `@ControllerAdvice`
- JSON conversion
- MVC configuration
- Filters
- Interceptors
- Argument resolvers
- Spring Security web configuration

It does not normally load:

- Service implementations
- Repositories
- Database
- Kafka consumers
- Entire application configuration

Spring Boot documents `@WebMvcTest` as a focused controller slice that auto-configures MVC infrastructure and `MockMvc`; collaborators are generally supplied as Mockito beans. ([Home][1])

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldReturnOrder() throws Exception {
        given(orderService.findById("O-100"))
                .willReturn(new OrderResponse(
                        "O-100",
                        "CONFIRMED"
                ));

        mockMvc.perform(get("/api/orders/O-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId")
                        .value("O-100"))
                .andExpect(jsonPath("$.status")
                        .value("CONFIRMED"));
    }
}
```

Current Spring versions use `@MockitoBean` for overriding Spring beans with Mockito mocks. Older Spring Boot projects commonly use `@MockBean`; use the annotation supported by the project’s managed Spring version. ([Home][1])

---

## Comparison

| `@SpringBootTest`                     | `@WebMvcTest`                         |
| ------------------------------------- | ------------------------------------- |
| Loads full application context        | Loads MVC slice                       |
| Slower                                | Faster                                |
| Tests multiple layers                 | Tests controller behavior             |
| Can use real server                   | Uses mock MVC infrastructure          |
| Real service beans by default         | Service dependencies are mocked       |
| Suitable for integration tests        | Suitable for controller slice tests   |
| Can include DB and broker connections | Does not load repositories by default |

---

## Other Test Slices

| Annotation        | Primary target                          |
| ----------------- | --------------------------------------- |
| `@DataJpaTest`    | JPA entities and repositories           |
| `@JdbcTest`       | JDBC access                             |
| `@DataMongoTest`  | MongoDB repositories                    |
| `@DataRedisTest`  | Redis repositories                      |
| `@WebFluxTest`    | Reactive controllers                    |
| `@JsonTest`       | JSON serialization/deserialization      |
| `@RestClientTest` | `RestClient` and `RestTemplate` clients |
| `@WebClientTest`  | Reactive HTTP clients                   |
| `@GraphQlTest`    | GraphQL controllers                     |

Spring Boot provides dedicated test modules and slices for MVC, WebFlux, JPA, JDBC, Redis, JSON, REST clients, and other technologies. ([Home][4])

### Interview-ready answer

> I use `@WebMvcTest` for fast controller tests where service dependencies are mocked. I use `@SpringBootTest` when I need the complete Spring configuration and interactions between controllers, services, repositories, security, messaging, or infrastructure. If I need a real HTTP server, I use `@SpringBootTest(webEnvironment = RANDOM_PORT)`.

---

# Q91: How Do You Write Integration Tests for a Kafka Consumer?

A useful Kafka consumer test should verify more than method invocation.

It should validate:

```text
Producer serialization
→ Kafka topic
→ Consumer deserialization
→ Listener execution
→ Database or business side effect
→ Offset and retry behavior
```

Two common options are:

1. Embedded Kafka
2. Testcontainers Kafka

---

## Option 1: Embedded Kafka

Spring Kafka provides test support, including an embedded KRaft broker and `KafkaTestUtils`. Current Spring Kafka versions use the KRaft-based embedded broker. ([Home][5])

```java
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = "payment-authorized"
)
class PaymentAuthorizedConsumerTest {

    @Autowired
    private KafkaTemplate<String, PaymentAuthorizedEvent>
            kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldMarkPaymentAsAuthorized() {
        PaymentAuthorizedEvent event =
                new PaymentAuthorizedEvent(
                        "PAY-100",
                        "ORDER-200"
                );

        kafkaTemplate.send(
                "payment-authorized",
                event.paymentId(),
                event
        );

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Payment payment =
                            paymentRepository.findById("PAY-100")
                                    .orElseThrow();

                    assertThat(payment.getStatus())
                            .isEqualTo(
                                    PaymentStatus.AUTHORIZED
                            );
                });
    }
}
```

Use polling assertions because Kafka processing is asynchronous.

Avoid:

```java
Thread.sleep(5_000);
```

A fixed sleep is either:

- Too short and flaky
- Longer than necessary and slow

---

## What to Verify

### Successful processing

```text
Valid event
→ correct business state
→ correct offset progression
```

### Invalid message

```text
Malformed event
→ classified correctly
→ retry or dead-letter path
```

### Duplicate event

```text
Same event delivered twice
→ business side effect occurs once
```

### Transient failure

```text
Database temporarily unavailable
→ bounded retry
→ eventual success
```

### Permanent failure

```text
Invalid business payload
→ no infinite retry
→ dead-letter record
```

### Ordering

```text
Events with same aggregate key
→ processed in expected order
```

---

## Option 2: Kafka Testcontainer

Use Testcontainers when production fidelity matters:

```java
@Testcontainers
@SpringBootTest
class PaymentConsumerContainerTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka:4.1.0"
                    )
            );

    @Autowired
    private KafkaTemplate<String, PaymentAuthorizedEvent>
            kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldConsumePaymentEvent() {
        kafkaTemplate.send(
                "payment-authorized",
                "PAY-100",
                new PaymentAuthorizedEvent(
                        "PAY-100",
                        "ORDER-200"
                )
        );

        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() ->
                        assertThat(
                                paymentRepository
                                        .findById("PAY-100")
                        ).isPresent()
                );
    }
}
```

Spring Boot supports automatic Testcontainers connection configuration through `@ServiceConnection`, including Kafka connection details. ([Home][2])

---

## Embedded Kafka vs Testcontainers

| Embedded Kafka                   | Kafka Testcontainer                   |
| -------------------------------- | ------------------------------------- |
| Usually faster                   | More startup overhead                 |
| Runs within test process support | Runs real broker distribution         |
| Good for focused listener tests  | Better production fidelity            |
| Easy for small test suites       | Better for broker configuration tests |
| May differ from deployed broker  | Can use matching Kafka image          |
| Useful for rapid feedback        | Useful for CI integration suites      |

### Interview-ready answer

> I start a broker using Embedded Kafka or a Kafka Testcontainer, publish a real serialized event, and poll until the observable business result appears. I test successful processing, deserialization failure, retries, dead-letter behavior, duplicate delivery, idempotency, and ordering. I prefer Testcontainers when broker fidelity matters and Embedded Kafka for faster focused tests.

---

# Q92: What Is Testcontainers?

Testcontainers is a Java library that starts disposable Docker containers for integration tests.

It can provide real instances of:

- PostgreSQL
- MySQL
- Oracle
- Kafka
- Redis
- RabbitMQ
- MongoDB
- Elasticsearch
- LocalStack
- WireMock

It integrates with JUnit and Spring Boot and is intended for tests against real backend technology rather than simplified in-memory substitutes. ([Home][2])

---

## PostgreSQL Integration Test

```java
@Testcontainers
@SpringBootTest
class AccountRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17.5"
            )
                    .withDatabaseName("account_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldPersistAndRetrieveAccount() {
        Account account = new Account(
                "ACC-100",
                new BigDecimal("5000.00")
        );

        accountRepository.save(account);

        Account stored =
                accountRepository.findById("ACC-100")
                        .orElseThrow();

        assertThat(stored.getBalance())
                .isEqualByComparingTo("5000.00");
    }
}
```

`@ServiceConnection` allows Spring Boot to obtain connection details automatically from supported Testcontainers types. ([Home][2])

---

## Using `@DynamicPropertySource`

For projects without service-connection support:

```java
@Testcontainers
@SpringBootTest
class AccountRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17.5");

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }
}
```

Spring Boot supports `@DynamicPropertySource` as a flexible way to register container-generated connection values. ([Home][2])

---

## Container Lifecycle

With the JUnit extension:

```java
@Container
static PostgreSQLContainer<?> postgres = ...
```

The static container is shared by test methods in that class.

```java
@Container
PostgreSQLContainer<?> postgres = ...
```

The instance container is restarted for each test method.

Testcontainers documents static containers as shared for a class and instance fields as restarted per test method. Its JUnit extension has primarily been tested with sequential execution, so parallel execution must be approached carefully. ([Testcontainers for Java][6])

---

## Singleton Container Pattern

For a large integration suite, starting one broker or database for every class can be slow.

```java
abstract class PostgreSqlIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(
                "postgres:17.5"
        );

        POSTGRES.start();
    }

    @DynamicPropertySource
    static void rpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
    }
}
```

A singleton container improves speed, but tests must still isolate their data.

Use:

- Transaction rollback where applicable
- Table cleanup
- Unique tenant or test identifiers
- Schema-per-test where justified
- Truncation in a controlled order

Spring Boot warns that container lifecycle and cached application contexts must be aligned. A context must not be reused after a container it depends on has already stopped. Managing containers as Spring beans or through supported imported configuration can provide safer lifecycle coordination. ([Home][2])

---

## Why Not Always Use H2?

H2 may differ from production PostgreSQL or Oracle in:

- SQL dialect
- Date behavior
- Locking
- Index behavior
- JSON support
- Constraint handling
- Transaction isolation
- Query planning
- Database-specific functions

For repository integration tests, use the same database engine as production where practical.

### Interview-ready answer

> Testcontainers starts real disposable dependencies in Docker for integration tests. I define the container with `@Container`, connect Spring using `@ServiceConnection` or `@DynamicPropertySource`, and test against the same database or broker technology used in production. Static or singleton containers improve execution speed, but test data isolation and container/ApplicationContext lifecycle must be handled carefully.

---

# Q93: How Do You Mock External HTTP Dependencies?

Use an HTTP stub server such as:

- WireMock
- MockServer

Unlike a Mockito mock, an HTTP stub exercises:

- URL construction
- HTTP method
- Headers
- Query parameters
- JSON serialization
- Status handling
- Timeouts
- Retry behavior
- Error mapping

---

## WireMock Example

```java
@SpringBootTest
class FraudClientIntegrationTest {

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(
                wireMockConfig()
                        .dynamicPort()
        );

        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void providerProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "fraud.provider.base-url",
                wireMock::baseUrl
        );
    }

    @Autowired
    private FraudClient fraudClient;

    @Test
    void shouldMapLowRiskResponse() {
        wireMock.stubFor(
                post(urlEqualTo("/risk/evaluate"))
                        .withHeader(
                                "Content-Type",
                                containing(
                                        "application/json"
                                )
                        )
                        .withRequestBody(
                                matchingJsonPath(
                                        "$.transactionId",
                                        equalTo("TX-100")
                                )
                        )
                        .willReturn(
                                okJson("""
                                        {
                                          "score": 12,
                                          "decision": "APPROVE"
                                        }
                                        """)
                        )
        );

        FraudDecision decision =
                fraudClient.evaluate(
                        new FraudRequest("TX-100")
                );

        assertThat(decision.status())
                .isEqualTo(
                        FraudStatus.APPROVED
                );

        wireMock.verify(
                1,
                postRequestedFor(
                        urlEqualTo("/risk/evaluate")
                )
        );
    }
}
```

WireMock supports matching requests by URL, headers, query parameters, and body content. Its request journal supports verifying exact interaction counts. ([WireMock][7])

---

## Test Failure Conditions

### Provider `500`

```java
wireMock.stubFor(
        post("/risk/evaluate")
                .willReturn(
                        serverError()
                )
);
```

### Timeout

```java
wireMock.stubFor(
        post("/risk/evaluate")
                .willReturn(
                        ok()
                                .withFixedDelay(5_000)
                )
);
```

### Rate limit

```java
wireMock.stubFor(
        post("/risk/evaluate")
                .willReturn(
                        aResponse()
                                .withStatus(429)
                                .withHeader(
                                        "Retry-After",
                                        "2"
                                )
                )
);
```

### Malformed JSON

```java
wireMock.stubFor(
        post("/risk/evaluate")
                .willReturn(
                        okJson("{invalid-json")
                )
);
```

### Connection fault

Simulate abrupt network failures where the tool supports them.

---

## What Should Be Verified?

- Correct URL and method
- Required authentication headers
- Correlation ID
- Idempotency key
- Request payload
- Retry count
- Timeout behavior
- Response mapping
- Error classification
- Circuit-breaker behavior
- No retry for non-retryable errors

MockServer also supports expectation-based request matching and verification of requests and responses. ([MockServer][8])

### Avoid Over-Specification

Do not verify every irrelevant HTTP detail.

Fragile:

```text
Require every header in an exact order
Require exact JSON whitespace
Require an unrelated User-Agent value
```

Prefer verifying contract-relevant behavior.

### Interview-ready answer

> I use WireMock or MockServer to start a local HTTP endpoint and point the service client to it. I stub responses based on method, path, headers, query parameters, and JSON body, then verify the received requests and interaction count. I test success, timeout, malformed response, rate limiting, authentication failure, and selected `5xx` conditions so retry and error-mapping behavior are validated.

---

# Q94: Explain Mutation Testing

Mutation testing deliberately modifies production code and checks whether the test suite detects the change.

Example original code:

```java
boolean isEligible(int age) {
    return age >= 18;
}
```

Possible mutant:

```java
boolean isEligible(int age) {
    return age > 18;
}
```

PIT runs the tests against modified bytecode. When a test fails, the mutant is **killed**. When all tests still pass, the mutant **survives**. ([Pitest][9])

---

## Mutation Score

```text
Mutation score =
killed mutants
──────────────────────────
total non-equivalent mutants
× 100
```

Example:

```text
Generated mutants: 100
Killed:             82
Survived:           15
No coverage:         3

Mutation score: approximately 82%
```

---

## Code Coverage vs Mutation Testing

### Code coverage asks:

> Did a test execute this line or branch?

### Mutation testing asks:

> Would the test fail if the behavior of this line changed?

Example:

```java
@Test
void executesCalculation() {
    calculator.add(2, 3);
}
```

This test may produce line coverage but has no assertion.

A mutant changing addition to subtraction may survive.

PIT explains that line and branch coverage measure execution but do not prove that tests detect faults. Mutation testing evaluates whether tests respond to behavioral changes. ([Pitest][9])

---

## Common Mutations

PIT may:

- Invert conditions
- Change boundary checks
- Replace return values
- Remove method calls
- Change arithmetic operations
- Negate values
- Remove increments
- Return `null`, `false`, zero, or an empty value

PIT performs mutations on compiled bytecode rather than directly rewriting Java source. ([Pitest][10])

---

## Example Surviving Mutant

Production code:

```java
public boolean canWithdraw(
        BigDecimal balance,
        BigDecimal amount
) {
    return balance.compareTo(amount) >= 0;
}
```

Weak test:

```java
@Test
void shouldAllowWithdrawal() {
    assertThat(
            service.canWithdraw(
                    new BigDecimal("100"),
                    new BigDecimal("50")
            )
    ).isTrue();
}
```

The test does not cover the boundary.

Add:

```java
@Test
void shouldAllowWithdrawalOfExactBalance() {
    assertThat(
            service.canWithdraw(
                    new BigDecimal("100"),
                    new BigDecimal("100")
            )
    ).isTrue();
}
```

This can kill a mutant changing `>=` to `>`.

---

## When to Use Mutation Testing

Best targets:

- Financial calculations
- Authorization logic
- Validation
- State machines
- Retry classification
- Pricing and fees
- Business-critical domain rules

Avoid spending excessive time mutating:

- Generated code
- Trivial getters
- Framework configuration
- DTO boilerplate
- Logging statements

Mutation testing is more expensive than ordinary tests, so teams commonly:

- Run a focused mutation set on pull requests.
- Run broader mutation testing nightly.
- Apply thresholds to critical modules.
- Investigate surviving mutants rather than chasing 100% blindly.

### Interview-ready answer

> Code coverage tells me which code was executed. Mutation testing changes the code and checks whether the tests detect that behavioral change. PIT mutates compiled Java bytecode, runs the relevant tests, and reports killed and surviving mutants. A surviving mutant often indicates a missing assertion, an uncovered boundary, or a weak test case.

---

# Q95: How Do You Test a Distributed Saga?

A Saga coordinates multiple local transactions across services.

Example:

```text
Create order
→ reserve inventory
→ authorize payment
→ confirm order
```

Failure path:

```text
Create order
→ reserve inventory
→ payment fails
→ release inventory
→ cancel order
```

Testing only the successful path is insufficient.

---

# Saga Testing Layers

## 1. State-Machine Unit Tests

Test the Saga coordinator or domain state transitions without infrastructure.

```java
class OrderSagaTest {

    @Test
    void shouldRequestInventoryAfterOrderCreation() {
        OrderSaga saga = new OrderSaga();

        List<SagaCommand> commands =
                saga.handle(
                        new OrderCreated(
                                "O-100"
                        )
                );

        assertThat(commands)
                .containsExactly(
                        new ReserveInventory(
                                "O-100"
                        )
                );

        assertThat(saga.state())
                .isEqualTo(
                        SagaState.RESERVING_INVENTORY
                );
    }

    @Test
    void shouldCompensateInventoryWhenPaymentFails() {
        OrderSaga saga =
                OrderSaga.awaitingPayment(
                        "O-100"
                );

        List<SagaCommand> commands =
                saga.handle(
                        new PaymentRejected(
                                "O-100"
                        )
                );

        assertThat(commands)
                .containsExactly(
                        new ReleaseInventory(
                                "O-100"
                        )
                );

        assertThat(saga.state())
                .isEqualTo(
                        SagaState.COMPENSATING
                );
    }
}
```

Test:

- Valid transitions
- Invalid transitions
- Duplicate events
- Out-of-order events
- Timeout transitions
- Compensation decisions
- Terminal states

---

## 2. Contract Tests for Every Boundary

For each command and event, verify:

```text
Producer serialization
+
consumer expectations
+
schema compatibility
```

Examples:

```text
Order Service → ReserveInventory command
Inventory Service → InventoryReserved event
Payment Service → PaymentRejected event
Order Service → ReleaseInventory command
```

Pact can represent consumer-provider contracts for both request-response interactions and message-based communication. ([Pact Docs][3])

Contract tests should verify:

- Event name/type
- Required fields
- Field types
- Version compatibility
- Error responses
- Optional-field handling
- Correlation and Saga identifiers

---

## 3. Service Integration Tests

Run each service with:

- Real database Testcontainer
- Real Kafka broker
- External HTTP dependencies stubbed
- Actual serialization
- Actual transaction management

Example inventory test:

```text
ReserveInventory command published
        ↓
Inventory consumer receives it
        ↓
Database reservation committed
        ↓
InventoryReserved event published
```

Verify both local state and emitted event.

---

## 4. Transactional Outbox Tests

Verify atomicity:

```text
Business state updated
+
outbox record inserted
=
same database transaction
```

Test rollback:

```text
Business update fails
→ no outbox event remains
```

Test relay duplication:

```text
Outbox publisher sends event
→ crashes before marking sent
→ event is sent again
→ consumer remains idempotent
```

---

## 5. Multi-Service Workflow Test

Deploy only the services required for the Saga:

```text
Order Service
Inventory Service
Payment Service
Kafka
Databases
```

Then trigger the flow through the public boundary.

```java
@Test
void shouldCancelOrderWhenPaymentIsRejected() {
    String orderId =
            orderClient.createOrder(
                    rejectedPaymentOrder()
            );

    await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                OrderView order =
                        orderClient.getOrder(orderId);

                assertThat(order.status())
                        .isEqualTo("CANCELLED");

                assertThat(
                        inventoryClient
                                .isReserved(orderId)
                ).isFalse();

                assertThat(
                        paymentClient
                                .status(orderId)
                ).isEqualTo("REJECTED");
            });
}
```

Assert business state, not only that a Kafka message appeared.

---

# Essential Saga Scenarios

## Successful Saga

```text
Order created
Inventory reserved
Payment authorized
Order confirmed
```

## Failure Before Any Side Effect

```text
Inventory rejects reservation
→ order cancelled
→ no payment attempted
```

## Compensation

```text
Inventory reserved
Payment rejected
→ inventory released
→ order cancelled
```

## Compensation Failure

```text
Payment rejected
Inventory release temporarily fails
→ COMPENSATION_PENDING
→ retry
→ eventually released
```

## Duplicate Event

```text
PaymentAuthorized delivered twice
→ one state transition
→ no duplicate confirmation
```

## Out-of-Order Event

```text
PaymentAuthorized arrives before expected state
→ reject, defer, or reconcile according to design
```

## Consumer Crash

```text
Database commit succeeds
Consumer crashes before offset commit
→ Kafka redelivers
→ idempotency prevents duplicate side effect
```

## Coordinator Crash

```text
Saga coordinator restarts
→ reconstruct state from durable storage
→ continue from pending transition
```

## Timeout

```text
No payment response before deadline
→ mark PAYMENT_TIMEOUT
→ begin compensation or reconciliation
```

## Lost or Delayed Event

```text
Event does not arrive
→ timeout scanner detects stuck Saga
→ retry, query status, or reconcile
```

---

# Idempotency Assertions

Every participant should have a stable message or operation identity.

```text
eventId
sagaId
aggregateId
commandId
```

Test the same message repeatedly:

```java
kafkaTemplate.send(topic, event);
kafkaTemplate.send(topic, event);

await()
        .untilAsserted(() ->
                assertThat(
                        reservationRepository
                                .countByOrderId("O-100")
                ).isEqualTo(1)
        );
```

---

# Do Not Assert Only the Final State

A test that checks only:

```text
Order status = CANCELLED
```

may miss:

- Inventory still reserved
- Duplicate refund
- Outbox event missing
- Audit trail missing
- Saga stuck in an inconsistent internal state

Verify invariants:

```text
Order is cancelled.
Inventory is released.
No successful charge exists.
Compensation is recorded.
No duplicate side effect occurred.
Saga reached a terminal state.
```

---

# Saga Test Environment

Use:

- Isolated topic names or test identifiers
- Unique Saga IDs
- Independent database schemas
- Deterministic provider stubs
- Bounded polling
- Cleanup strategy
- Trace and correlation IDs
- Observable Saga status endpoint or read model

Avoid depending on message arrival timing.

### Interview-ready answer

> I test a Saga at multiple levels. Unit tests validate the coordinator state machine, contract tests validate every command and event, and service-level integration tests use real databases and brokers. A smaller number of multi-service tests validate success, failure, compensation, timeout, duplicate delivery, out-of-order events, consumer crashes, and coordinator recovery. I assert all business invariants and eventual states, not merely that messages were published.

---

# Quick Interview Cheat Sheet

| Topic               | Core answer                                   |
| ------------------- | --------------------------------------------- |
| Test Pyramid        | Many narrow tests, few broad tests            |
| Unit test           | Business logic without infrastructure         |
| Slice test          | Focused Spring layer                          |
| Integration test    | Real database, broker, or cache               |
| Contract test       | Consumer-provider compatibility               |
| E2E test            | Complete deployed workflow                    |
| `@SpringBootTest`   | Full Spring Boot context                      |
| `@WebMvcTest`       | MVC/controller slice                          |
| Embedded Kafka      | Fast focused Kafka test                       |
| Kafka Testcontainer | Better broker fidelity                        |
| Testcontainers      | Disposable real dependencies                  |
| WireMock            | HTTP stub and request verification            |
| Mutation testing    | Tests must detect code changes                |
| Saga testing        | State, contracts, infrastructure and recovery |
| Async assertion     | Poll observable state with deadline           |
| Idempotency test    | Deliver same event more than once             |

# Senior Interview Summary

> I structure microservice testing around fast feedback. Domain behavior is covered by unit tests, Spring layers by focused slices, technology boundaries by Testcontainers-based integration tests, and service communication by consumer-driven contracts. Only a small number of critical workflows depend on full end-to-end environments.
>
> For Spring Boot, I choose the narrowest context that proves the behavior: `@WebMvcTest` for controller contracts, `@DataJpaTest` for persistence, and `@SpringBootTest` when multiple application layers must collaborate. Kafka tests publish real serialized records and poll for durable outcomes rather than sleeping.
>
> Distributed Sagas require testing beyond the happy path. I verify state transitions, outbox atomicity, event contracts, duplicate delivery, retries, compensation, compensation failure, timeout, process crashes, and reconciliation. The final test assertion covers all business invariants so a superficially successful status cannot hide inconsistent downstream state.

[1]: https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html "Testing Spring Boot Applications :: Spring Boot"
[2]: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html?utm_source=chatgpt.com "Testcontainers :: Spring Boot"
[3]: https://docs.pact.io/ "Introduction | Pact Docs"
[4]: https://docs.spring.io/spring-boot/reference/testing/test-modules.html?utm_source=chatgpt.com "Test Modules :: Spring Boot"
[5]: https://docs.spring.io/spring-kafka/reference/testing.html "Testing Applications :: Spring Kafka"
[6]: https://java.testcontainers.org/test_framework_integration/junit_5/ "Jupiter / JUnit 5 - Testcontainers for Java"
[7]: https://wiremock.org/docs/request-matching/?utm_source=chatgpt.com "Request Matching"
[8]: https://www.mock-server.com/mock_server/verification.html?utm_source=chatgpt.com "Verifying Requests & Responses"
[9]: https://pitest.org/ "PIT Mutation Testing"
[10]: https://pitest.org/quickstart/mutators/?utm_source=chatgpt.com "Mutation operators"
