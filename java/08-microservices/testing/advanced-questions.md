# Advanced Questions

## Q89: Explain the Test Pyramid for microservices. What types of tests live at each layer?
Unit (70%) -> Integration (20%) -> Contract (Pact) -> E2E (10%); avoid ice cream cone.

## Q90: How do you test a Spring Boot microservice with @SpringBootTest vs @WebMvcTest?
Full context vs slice test; test slices: @DataJpaTest, @WebFluxTest, @JsonTest.

## Q91: How do you write integration tests for a Kafka consumer in Spring Boot?
Embedded Kafka (@EmbeddedKafka), TestContainers with real Kafka, await + polling assertions.

## Q92: What is TestContainers? How do you use it for database integration tests?
Docker-based real dependencies; @Container lifecycle; singleton container pattern for speed.

## Q93: How do you mock external HTTP dependencies in microservice integration tests?
WireMock, MockServer; stub by request matching; verify interaction counts.

## Q94: Explain mutation testing. How does it differ from code coverage?
PIT mutates bytecode; surviving mutants = weak tests; coverage != quality assurance.

## Q95: How do you test a distributed Saga flow across multiple microservices?
Contract tests per step, dedicated integration env, event assertion with Kafka consumer.
