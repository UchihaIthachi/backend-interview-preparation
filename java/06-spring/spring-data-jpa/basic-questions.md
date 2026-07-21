# Basic Questions

## Question 1: Why might `@Transactional` fail on self-invocation?

`@Transactional` only works on public methods when applied via a Spring AOP proxy. When a method calls another method within the same class (self-invocation), it bypasses the proxy, meaning the transactional advice is not applied.


Hibernate
 - How do you define relationships between tables in Hibernate?
 - What are the types of relationships in Hibernate?