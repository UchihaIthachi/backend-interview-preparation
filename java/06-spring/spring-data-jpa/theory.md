# Spring Data JPA and the N+1 Query Problem

## 1. Definition

Spring Data JPA (built on Hibernate) maps Java objects to database rows. The
persistence context tracks loaded entities and their state changes ("dirty
checking") within a transaction.

## 2. Why it exists

JPA removes most boilerplate SQL/JDBC code, and dirty checking means you don't
need to explicitly call `save()` after modifying a managed entity within a
transaction — Hibernate detects the change and issues an UPDATE automatically
at flush time.

## 3. How it works — the N+1 problem

```text
1 query:   SELECT * FROM orders          (loads 100 orders)
100 queries: SELECT * FROM line_items WHERE order_id = ?   (once per order, if lazy)
= 101 queries total for what should often be 1-2 queries
```

This happens when an entity relationship is lazily loaded and then accessed in
a loop, triggering one query per iteration.

## 4. Code example — fixes

```java
// Fix 1: fetch join
@Query("SELECT o FROM Order o JOIN FETCH o.lineItems WHERE o.status = :status")
List<Order> findByStatusWithLineItems(@Param("status") String status);

// Fix 2: entity graph
@EntityGraph(attributePaths = {"lineItems"})
List<Order> findByStatus(String status);

// Fix 3: DTO projection (often best for read-heavy endpoints)
@Query("SELECT new com.app.OrderSummary(o.id, o.total) FROM Order o")
List<OrderSummary> findSummaries();
```

## 5. Production use case

Any endpoint that "loads a list, then something for each item" is a strong
signal to check for N+1 — it's one of the most common performance bugs found
during code review or slow-query investigation, and it doesn't show up in
tests with small datasets.

## 6. Common mistakes

- Defaulting relationships to eager loading to "avoid the problem" — this
  often makes it worse by always loading data that's not needed.
- Not enabling SQL logging (`spring.jpa.show-sql`) during development, missing
  the problem until production.
- Confusing optimistic locking (version column, fails on conflict at commit)
  with pessimistic locking (`SELECT ... FOR UPDATE`, blocks other transactions
  immediately).

## 7. Trade-offs

| Fetch join                         | Entity graph          | DTO projection                     |
| ---------------------------------- | --------------------- | ---------------------------------- |
| Simple, works for one relationship | Reusable, declarative | Best performance, most boilerplate |

## 8. Interview questions

1. What is the N+1 query problem and how do you detect it?
2. What is dirty checking and what triggers it?
3. Optimistic vs pessimistic locking — when do you use each?
4. Lazy vs eager loading — what are the risks of each default?
5. What's the difference between JPQL and native queries?

## 9. Short interview answer

"N+1 happens when a lazy relationship gets loaded once per item in a loop
instead of once up front. I fix it with a fetch join or entity graph when I
still need entities, or a DTO projection when I only need specific fields —
which is usually also the fastest option since it avoids loading full entity
graphs at all."

## 10. Related topics

- [REST API development](rest-api.md)
- [Database indexing](../../databases/sql.md)
