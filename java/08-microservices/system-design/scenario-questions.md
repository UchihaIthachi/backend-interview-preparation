# Scenario-based Questions

**Scenario:** Your microservice depends on another service for user data. That service is intermittently failing, causing your API to break.
**Questions:** How would you handle this failure gracefully? Would you implement circuit breaker? How? What fallback strategies would you design? How do you ensure system resilience?

**Scenario:** Your system needs to process orders asynchronously using messaging.
**Questions:** How would you design this using an event-driven approach? How would you use Azure Service Bus queues/topics? What happens if message processing fails? How do you ensure idempotency?

**Scenario:** You need to design an API for a system like an e-commerce checkout handling millions of users.
**Questions:** How would you design the architecture? How would you ensure high availability and fault tolerance? How would you scale microservices?

**Scenario:** Your API calls a third-party service which sometimes takes too long to respond.
**Questions:** How do you handle timeouts in Spring Boot? Would you implement retries? If yes, with what strategy (fixed, exponential backoff)? How do you avoid retry storms?

**Scenario:** Your API needs to process large JSON payloads (10MB+).
**Questions:** How would you optimize performance? Would you use streaming or chunking? How do you prevent memory issues?

**Scenario:** A frequently accessed API endpoint hits the database every time.
**Questions:** Where would you add caching (API, service, DB)? Would you use Redis or in-memory cache? How do you handle cache invalidation?

**Scenario:** Your service memory usage keeps increasing over time.
**Questions:** How would you identify a memory leak? What tools would you use (heap dump, profiling)? Common causes in Java applications?

**Scenario:** Build a notification system (email/SMS/push) using microservices.
**Questions:** How would you design the system? How would you use queues/topics in Azure Service Bus? How do you ensure scalability and reliability?

**Scenario:** A client retries a payment API due to network failure, causing duplicate transactions.
**Questions:** What is idempotency and why is it critical here? How would you design an idempotent API? Would you use idempotency keys or DB constraints?

**Scenario:** You need to return millions of records via an API.
**Questions:** Offset vs cursor-based pagination—what’s better and why? How do you handle consistency when data changes during pagination? How would you implement this in Spring Boot?

**Scenario:** Messages are failing repeatedly in Azure Service Bus and moving to DLQ.
**Questions:** What is a Dead Letter Queue? How do you process and debug DLQ messages? How do you prevent messages from ending up there?

**Scenario:** An API takes 2–5 minutes to process a request.
**Questions:** Should this be synchronous or async? How would you design polling or callback mechanisms? Would you use messaging queues?

**Scenario/Question:** Your API has high latency - what optimizations will you apply?

**Scenario/Question:** How will you handle millions of records efficiently?

**Scenario/Question:** How will you implement pagination?

**Scenario/Question:** How will you ensure data integrity?

**Scenario/Question:** How will you optimize write-heavy systems?

**Scenario/Question:** How will you mentor junior developers?

**Scenario/Question:** How will you design a highly concurrent system handling millions of requests?

**Scenario/Question:** How will you design idempotent consumers?

**Scenario/Question:** How will you design a resilient system?

**Scenario/Question:** How will you design a system for high availability?

**Scenario/Question:** How will you design service isolation?



# Advanced Interview Questions & Answers

**Scenario/Question:** 91. Your bank is migrating from Java 11 to Java 21. How would you minimize business risk during migration?

**Answer:** Perform comprehensive automated testing (unit, integration, contract). Run parallel environments. Deploy via canary releases. Use tools like JMH to ensure performance hasn't degraded with the new JVM.

**Scenario/Question:** 92. A core banking dependency is available only during business hours, but your service must operate 24×7. How would you design around this limitation?

**Answer:** Implement asynchronous processing. Accept requests 24x7, store them in a persistent queue, and process them as soon as the core dependency comes online. Provide users with a "Pending" status.

**Scenario/Question:** 93. How would you implement a "freeze account" feature so that no debit transactions are accepted from any channel once the freeze request is confirmed?

**Answer:** Update the account state to FROZEN in a central data store. Use a distributed cache with near-real-time invalidation to ensure all API gateways and core services check this state before authorizing debits.

**Scenario/Question:** 94. A nationwide festival causes transaction volume to increase by 15× for six hours. What architectural changes would you make to keep services responsive?

**Answer:** Implement aggressive auto-scaling (HPA in Kubernetes). Pre-scale databases and caches. Use circuit breakers to protect overloaded dependencies. Downgrade non-critical features (like reward points calculation).

**Scenario/Question:** 95. You must introduce a new fraud-check service into an existing payment flow without increasing customer response time noticeably. How would you achieve this?

**Answer:** Run the fraud check asynchronously. If the transaction completes before the check, hold the settlement. Alternatively, execute the fraud check in parallel with other validations using `CompletableFuture`.

**Scenario/Question:** 96. A regulatory rule changes overnight, affecting interest calculation for all savings accounts. How would you roll out the change safely and verify correctness?

**Answer:** Implement the new rule behind a feature toggle. Run "Shadow Testing" where both old and new logic run concurrently, compare their outputs, and log discrepancies without affecting the actual user data until verified.

**Scenario/Question:** 97. How would you design a customer profile service that is the single source of truth while allowing other systems to continue operating during temporary outages?

**Answer:** Use Event Sourcing and CQRS. The profile service publishes updates to an event bus. Dependent services consume these events and maintain their own local read-optimized projections of the profile data.

**Scenario/Question:** 98. Your mobile banking app and internet banking portal submit conflicting profile updates within seconds of each other. How would you resolve the conflict?

**Answer:** Use Optimistic Concurrency Control (ETag or Version numbers). The second request is rejected due to a version mismatch, prompting the user to refresh and see the latest state before reapplying the change.

**Scenario/Question:** 99. An external credit bureau becomes intermittently unavailable during loan processing. How would you maintain a good customer experience while preserving business correctness?

**Answer:** Implement Circuit Breakers and Fallbacks. If the bureau is down, queue the request, return a "Processing Application" status to the user, and use Webhooks/Push notifications to inform them once completed.

**Scenario/Question:** 100. If you were the technical owner of HDFC Bank's fund transfer platform, what architectural, operational, and security improvements would you prioritize over the next 12 months, and why?

**Answer:** Prioritize zero-trust security (mTLS, tighter IAM), transition to event-driven architectures to improve resilience during peak loads, enhance observability (distributed tracing), and improve deployment speed via CI/CD maturity.



# AWS Cloud Scenarios for Java Engineers

**Scenario/Question:** 1. You need to deploy a long-running Spring Boot payment API on AWS. Would you choose AWS Lambda, Amazon ECS on Fargate, or Amazon EKS?

**Answer:** For a conventional, continuously available Spring Boot payment API, Amazon ECS on AWS Fargate behind an Application Load Balancer is usually the strongest default. Lambda is a good fit for short, event-driven units of work with variable traffic. EKS is appropriate when the organization has a real Kubernetes requirement, not simply because Kubernetes is popular.

*Why:* ECS on Fargate removes EC2 host management while preserving the normal operational model of a Java service (continuously running JVM, predictable connection pools).

**Scenario/Question:** 2. How would you design a highly available Java payment API across Availability Zones?

**Answer:** Use a regional, Multi-AZ architecture with no single-AZ dependency. Place an Application Load Balancer across at least two Availability Zones, run multiple ECS tasks in private subnets across those zones, use a Multi-AZ relational database or an appropriately replicated data store, and make the application stateless at the compute layer.

*Why:* Availability Zones are separate failure domains. Running all application tasks or the only database writer path in one zone leaves the payment service vulnerable to a zonal failure.

**Scenario/Question:** 3. How do you make a payment-creation API idempotent?

**Answer:** Require or generate a stable idempotency key for the business operation, persist it atomically with the payment result, and return the original result for repeated requests with the same key and same request semantics. Reject reuse of the same key with materially different request data.

*Why:* HTTP transport retries, load balancer retries, and operator replays can repeat an operation. Correctness requires idempotency at the business boundary. Use a unique constraint or transactional write in the database.

**Scenario/Question:** 4. When would you use an Amazon SQS Standard queue versus an SQS FIFO queue for payment processing?

**Answer:** Use a Standard queue when maximum throughput and horizontal concurrency matter more than strict ordering, and make every consumer idempotent. Use a FIFO queue when ordering must be preserved within a business key, such as all state transitions for one payment or one account.

*Why:* Standard queues give very high throughput but duplicate delivery is possible. FIFO queues support ordered processing within a message group, though application-level idempotency is still required.

**Scenario/Question:** 5. A payment service must update its database and publish a PaymentAuthorized event. How do you avoid losing the event or publishing it without the database change?

**Answer:** Use the transactional outbox pattern. Write the payment state change and an outbox record in the same local database transaction. A separate publisher then sends the outbox event to SQS, SNS, EventBridge, or MSK and marks it as published. Consumers remain idempotent.

*Why:* Writing to the database and broker as two independent operations creates a dual-write failure risk.

**Scenario/Question:** 6. Would you use Aurora PostgreSQL or DynamoDB for the core payment data model?

**Answer:** For a strongly relational payment domain with multi-row invariants, accounting relationships, complex reconciliation queries, and familiar transactional semantics, Aurora PostgreSQL is often the safer core system of record. DynamoDB is an excellent choice for access patterns around keys and conditional writes, like idempotency records, high-scale status lookups, or deduplication.

*Why:* Aurora PostgreSQL supports relational constraints and SQL transactions natural for ledgers. DynamoDB requires table design starting from known access patterns.

**Scenario/Question:** 7. Why might a Java payment service use Amazon RDS Proxy, and how should it interact with HikariCP?

**Answer:** RDS Proxy can pool and reuse database connections across application instances, absorb connection storms, and improve resilience during database failover. The Java service should still use a bounded local pool such as HikariCP, but the pool size must be chosen from database capacity and total task count.

*Why:* Autoscaling can multiply local connection pools and exhaust the database before CPU becomes the bottleneck. RDS Proxy reduces the cost of repeatedly opening physical connections.

**Scenario/Question:** 8. How would you implement a multi-step payment workflow such as authorize, fraud check, ledger posting, notification, and compensation?

**Answer:** Model it as a durable state machine or saga rather than one distributed database transaction. AWS Step Functions Standard Workflows can orchestrate long-running, auditable steps, while Java services execute domain operations. Each step must be idempotent and the workflow must define retries, terminal failures, and compensating actions.

*Why:* Workflows cross systems that cannot participate in one ACID transaction. A durable orchestrator records progress and makes timeout and retry behavior explicit.

**Scenario/Question:** 9. How should retries be implemented when the Java service calls AWS services or an external payment processor?

**Answer:** Retry only transient failures, use bounded attempts with exponential backoff and jitter, apply strict timeouts, and retry only idempotent operations or operations protected by an idempotency key. Permanent validation or business-decline responses should not be retried.

*Why:* Immediate synchronized retries can create a retry storm and make a partial outage worse. A timeout is an ambiguous outcome; the downstream processor might have completed the charge.

**Scenario/Question:** 10. How should card data, API credentials, and encryption keys be protected in an AWS payment platform?

**Answer:** Minimize the card-data environment, tokenize card details whenever possible, encrypt data in transit and at rest, keep secrets in AWS Secrets Manager, use AWS KMS for key control, and grant access through least-privilege IAM roles. Never place secrets in source code, container images, environment files, or logs.

*Why:* The safest sensitive data is data the platform never stores. Secrets Manager protects values with KMS-backed encryption and rotation workflows.

**Scenario/Question:** 11. What is the difference between an ECS task role and an ECS task execution role?

**Answer:** The task role is assumed by the application container and grants the Java code permission to call AWS services (e.g., SQS, Secrets Manager). The task execution role is used by ECS and the Fargate agent for platform operations like pulling an image from ECR and sending container logs to CloudWatch. They should be separate and least-privileged.

**Scenario/Question:** 12. How would you protect a public payment API from abuse and traffic spikes?

**Answer:** Use layered controls: TLS, strong client authentication/authorization, AWS WAF managed and custom rules, rate-based rules, API Gateway or load-balancer protections, request-size limits, schema validation, per-merchant quotas, and backend concurrency controls. Treat cloud throttling as one layer, not the complete business control.

**Scenario/Question:** 13. What should be monitored in a Java payment platform running on AWS?

**Answer:** Monitor business correctness, customer impact, application behavior, Java runtime health, AWS service health, and dependency health. Use structured logs, metrics, and distributed traces with stable correlation identifiers, while excluding sensitive payment data (PII/card data).

*Why:* CPU and memory do not reveal whether merchants are being charged twice or authorizations are timing out.

**Scenario/Question:** 14. How should an ECS-based payment API autoscale?

**Answer:** Use target tracking or step scaling based on the actual bottleneck. CPU and memory are useful signals, but request rate, concurrent requests, queue backlog per task, response latency, thread-pool saturation, and downstream capacity can be better scaling metrics. Maintain a safe minimum capacity and cap scaling at what databases and processors can sustain.

**Scenario/Question:** 15. How would you deploy a new version of a Java payment service without causing downtime or unsafe mixed-version behavior?

**Answer:** Use immutable container images, automated tests, database compatibility rules, health checks, and a controlled rolling, blue/green, or canary deployment. Enable automatic rollback or an ECS deployment circuit breaker. Ensure old and new versions can coexist during the deployment window (backward-compatible database migrations).

**Scenario/Question:** 16. How would you design disaster recovery for a payment platform, and what do RTO and RPO mean?

**Answer:** RTO is the maximum acceptable time to restore the service. RPO is the maximum acceptable amount of data loss (in time). Select backup-and-restore, pilot light, warm standby, or multi-region active architecture based on business impact, regulatory requirements, and the ability to preserve payment correctness (reconciliation) during failover.

**Scenario/Question:** 17. What VPC and network design would you use for a payment API on ECS?

**Answer:** Place public load balancers or API endpoints in public subnets, run ECS tasks and databases in private subnets, restrict flows with security groups, and use VPC endpoints for supported AWS services (like KMS, Secrets Manager, ECR) to keep traffic off the public internet. Control outbound access explicitly.

**Scenario/Question:** 18. How do you size and tune a Java JVM inside an ECS Fargate task?

**Answer:** Set task CPU and memory from load tests, size the JVM for the container limit leaving native-memory headroom, use a modern JDK with container awareness, and monitor heap, non-heap, thread stacks, direct buffers, and GC. Do not set the Java heap equal to the task memory limit, as the JVM uses memory outside the heap (metaspace, direct buffers), causing OOMKills.

**Scenario/Question:** 19. When would you choose SQS, Amazon Kinesis Data Streams, or Amazon MSK for payment events?

**Answer:** Choose SQS for work queues and decoupled task processing (competing consumers). Choose Kinesis Data Streams for managed partitioned event streams with ordered records per partition and multiple near-real-time consumers. Choose Amazon MSK when the organization requires Apache Kafka compatibility, ecosystem tooling, or an existing Kafka operating model.

**Scenario/Question:** 20. How would you build auditability and reconciliation into an AWS payment platform?

**Answer:** Maintain an append-oriented history of payment state changes with immutable identifiers, timestamps, actor information, and processor references. Preserve operational logs in protected storage (e.g., S3 Object Lock) and implement automated reconciliation between internal records, processor reports, settlement files, and ledger entries to detect failures real-time processing missed.

