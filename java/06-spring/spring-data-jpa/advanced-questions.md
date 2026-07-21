# Advanced Questions

## Question 1: How does `@Transactional` work (proxies)?

No answer provided yet.

## Q16: What is the difference between @Transactional in a monolith vs a microservice?
No distributed ACID; local transactions only; use Saga for cross-service atomicity.

## Q33: What is Spring Data JPA? How is it different from Hibernate?
Spring Data JPA is an abstraction layer providing repositories to reduce boilerplate. Hibernate is the underlying ORM implementation.

## Q34: What are the different types of repository interfaces in Spring Data?
Repository, CrudRepository (basic operations), PagingAndSortingRepository (pagination), JpaRepository (adds JPA specific methods like flush).

## Q35: What is the difference between @Query with JPQL and Native Query?
JPQL operates on entity objects and is DB-agnostic. Native Query uses actual SQL (table/column names) and is DB-specific.

## Q36: What are JPA entity relationships? Explain with examples.
@OneToOne (User-Profile), @OneToMany/@ManyToOne (Department-Employee), @ManyToMany (Student-Course, requires join table).

## Q37: What is the N+1 Select problem and how do you solve it?
Fetching N parents triggers N child queries. Solve with JPQL JOIN FETCH, @EntityGraph, or @BatchSize.

## Q38: What is the difference between EntityManager.persist(), merge(), and save() in Spring Data?
`persist()` makes transient entity managed. `merge()` updates detached entity. Spring Data `save()` calls persist if new, else merge.

## Q39: What is @Transactional? What are its propagation levels?
Manages transactions declaratively. Levels: REQUIRED (default), REQUIRES_NEW, SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER, NESTED.

## Q40: What is the difference between FetchType.LAZY and FetchType.EAGER?
EAGER loads associations immediately. LAZY loads them on demand (proxy). Default is LAZY for collections, EAGER for singular associations.

## Q41: What are Spring Data Projections? Explain interface-based and DTO projections.
Fetch only required columns. Interface-based defines getters. DTO-based uses constructor expression in @Query.

## Q42: What is Flyway / Liquibase? How does Spring Boot integrate with them?
Database migration tools for versioned schema evolution. Auto-configured when on classpath by reading scripts (e.g., `V1__init.sql`).

## Q43: What is Optimistic vs Pessimistic locking in JPA?
Optimistic uses `@Version` to detect concurrent mods. Pessimistic acquires DB lock on read (`PESSIMISTIC_WRITE`).
