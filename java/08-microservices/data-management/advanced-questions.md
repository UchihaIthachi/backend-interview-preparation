# Advanced Questions

## Q41: What is the Database per Service pattern? How do you handle cross-service queries?
API composition, CQRS read models, event-driven denormalization — no joins across DBs.

## Q42: How do you implement distributed locking across microservices using Redis?
Redlock algorithm, SETNX + TTL, fencing tokens to prevent split-brain issues.

## Q43: Explain the difference between optimistic and pessimistic locking. When do you use each?
Optimistic = version column + retry; Pessimistic = SELECT FOR UPDATE; contention levels.

## Q44: How do you handle schema migrations in a microservice with zero downtime?
Expand-contract pattern; backward compatible migrations; Flyway/Liquibase phased rollout.

## Q45: What is the N+1 query problem? How do you detect and fix it in Spring Data JPA?
@EntityGraph, JOIN FETCH, @BatchSize, Hibernate statistics, Hypersistence Utilities.

## Q46: Explain connection pool sizing in a microservice. How do you calculate optimal pool size?
Little's Law: pool = (core × 2) + spindles; HikariCP settings; avoid pool starvation.

## Q47: How would you implement multi-tenancy in a microservice? Compare schema vs row-level isolation.
Separate schema (strong isolation), row-level (RLS, tenant_id filter), shared DB risks.

## Q48: What is eventual consistency? Give a concrete example in an e-commerce system.
Order placed -> inventory reserved -> email sent; intermediate states are inconsistent.

## Q49: How do you implement CQRS read model synchronization using Debezium CDC?
Debezium captures WAL, publishes to Kafka, consumers update read store (Elasticsearch/Redis).

## Q50: How do you manage database secrets rotation in microservices without downtime?
Dual credentials, gradual rollover, Vault dynamic secrets, connection pool refresh.
