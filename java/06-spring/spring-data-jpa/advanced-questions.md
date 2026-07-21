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


Spring @Transactional 
 
16. How does @Transactional work internally? 
17. Why does @Transactional sometimes not work? 
18. What exceptions trigger transaction rollback? 
19. What is the difference between REQUIRED and REQUIRES_NEW? 
20. Explain all transaction propagation types. 
21. What is transaction isolation? 
22. Explain all isolation levels. 
23. What causes dirty reads? 
24. What causes phantom reads? 
25. Why should API calls be avoided inside transactions? 
26. What happens during nested transactions? 
27. What is transaction synchronization? 
28. How do you debug transaction issues? 
29. What is the self-invocation problem? 
30. How does Spring use proxies for transaction management?

𝗗𝗮𝘁𝗮𝗯𝗮𝘀𝗲 𝗮𝗻𝗱 𝗝𝗣𝗔
 - What is the N+1 problem? How do you detect and fix it?
 - Two transactions updating same row simultaneously. One silently overwrites. How do you prevent this?
 - First level vs second level cache in Hibernate what is the difference?
 - How does indexing work? When does an index hurt performance?
 - What are ACID properties? Explain with a banking transaction example.

DATABASE AND JPA:
1. First-level vs second-level cache.
2. The N+1 problem.
3. How you find and fix a slow query.
4. How you handle concurrent updates.

Database and JPA
13. Lazy loading throws LazyInitializationException in production but not in dev. Why?
14. Your JPA query works fine with 100 rows. Takes 45 seconds with 1 million rows. What is wrong?
15. Two transactions updating same row simultaneously. One silently overwrites the other. How do you prevent it?

𝗝𝗣𝗔 / 𝗛𝗶𝗯𝗲𝗿𝗻𝗮𝘁𝗲
 - Difference between get() and load() in Hibernate.
 - How does Hibernate manage caching (1st level vs 2nd level cache)?
 - How do you handle optimistic vs pessimistic locking in JPA?
 - Explain dirty checking in Hibernate.
 - Entity lifecycle states (Transient, Persistent, Detached, Removed)
 - Scenario Based: You have a product catalog service where multiple users can update stock quantity at the same time. How would you use JPA locking to prevent inconsistent data?

Database and JPA
1. First level vs second level cache in Hibernate what is the difference?
2. Your JPA query works fine locally. Times out in production. Why?
3. Two transactions updating same row simultaneously. One silently overwrites. How do you prevent this?
4. N+1 problem how do you detect it and fix it without rewriting everything?