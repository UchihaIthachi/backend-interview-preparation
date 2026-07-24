# Basic Questions — `@Transactional` and Hibernate Relationships

## Question 1: Why might `@Transactional` fail on self-invocation?

Spring usually implements `@Transactional` using an AOP proxy around the managed bean.

```text
Caller
  ↓
Spring transaction proxy
  ↓
Start or join transaction
  ↓
Invoke target method
  ↓
Commit or roll back
```

When one method calls another method in the **same class**, the call is made through `this`, not through the Spring proxy.

```java
@Service
public class PaymentService {

    public void processPayment() {
        savePayment(); // Same as this.savePayment()
    }

    @Transactional
    public void savePayment() {
        // Database operations
    }
}
```

The call to `savePayment()` bypasses the proxy, so the transactional interceptor does not process the annotation on that method. Spring’s default proxy mode intercepts calls that enter through the proxy, not calls made internally within the target object. ([Home][1])

### Important nuance

Suppose the outer method is already transactional:

```java
@Service
public class PaymentService {

    @Transactional
    public void processPayment() {
        savePayment();
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void savePayment() {
        // Database operations
    }
}
```

`savePayment()` still runs inside the transaction started by `processPayment()`, but its own `REQUIRES_NEW` configuration is ignored because the internal call does not pass through the proxy.

Therefore:

```text
Self-invocation does not always mean
"there is no transaction."

It means:
"The called method's transactional advice is not newly applied."
```

### Preferred solution: move the method to another bean

```java
@Service
public class PaymentWriter {

    @Transactional
    public void savePayment() {
        // Database operations
    }
}
```

```java
@Service
public class PaymentService {

    private final PaymentWriter paymentWriter;

    public PaymentService(
            PaymentWriter paymentWriter
    ) {
        this.paymentWriter = paymentWriter;
    }

    public void processPayment() {
        paymentWriter.savePayment();
    }
}
```

The call now passes through the `PaymentWriter` proxy:

```text
PaymentService
    ↓
PaymentWriter proxy
    ↓
Transaction interceptor
    ↓
PaymentWriter.savePayment()
```

### What about private methods?

A private method cannot be overridden or invoked through a normal Spring proxy, so placing `@Transactional` on a private method does not provide normal proxy-based transaction interception.

However, saying that `@Transactional` works **only on public methods** is too broad. With modern Spring class-based proxies, protected and package-visible methods can also be transactional. Interface-based proxies generally require the method to be public and declared through the proxied interface. Public service methods remain the clearest and most portable design. ([Home][1])

### Other situations where `@Transactional` may not work as expected

- The object was created using `new` instead of by Spring.
- The annotated method is private.
- Self-invocation bypasses the proxy.
- The method runs in another thread, such as through `@Async`.
- A checked exception occurs, but rollback was not configured for it.
- The exception is caught and not rethrown.
- The database operation has not yet reached flush or commit.
- The configured transaction manager is not the one managing the resource.

### Interview-ready answer

> Spring normally implements `@Transactional` through an AOP proxy. When one method calls another method in the same object, the call goes through `this` rather than through the proxy, so the called method’s transactional advice is not applied. I normally fix this by moving the transactional operation into another Spring-managed service.

---

# Hibernate Relationships

## Question 2: How do you define relationships between tables in Hibernate?

In Hibernate/JPA, relationships are defined between **entity classes** using association annotations.

The main annotations are:

```java
@OneToOne
@OneToMany
@ManyToOne
@ManyToMany
```

Columns used to connect tables are commonly configured using:

```java
@JoinColumn
@JoinTable
```

Example database relationship:

```text
orders.customer_id
        ↓
customers.id
```

Entity mapping:

```java
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private CustomerEntity customer;
}
```

`@ManyToOne` commonly maps a foreign-key column using `@JoinColumn`. ([Jakarta EE][2])

---

## Owning side and inverse side

In a bidirectional relationship, one entity is the **owning side**. The owning side controls the foreign-key mapping.

For a one-to-many/many-to-one relationship, the many side normally owns the relationship because it contains the foreign key.

```java
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
}
```

The inverse side uses `mappedBy`:

```java
@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    private Long id;

    @OneToMany(mappedBy = "customer")
    private List<OrderEntity> orders =
            new ArrayList<>();
}
```

The value:

```java
mappedBy = "customer"
```

refers to the `customer` field in `OrderEntity`, not to the database column name.

Jakarta Persistence specifies that the many side owns a bidirectional one-to-many/many-to-one relationship, while the inverse one-to-many side identifies the owner through `mappedBy`. ([Jakarta EE][3])

---

## Keep both sides synchronized

For a bidirectional relationship, update both Java object references:

```java
@Entity
public class CustomerEntity {

    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderEntity> orders =
            new ArrayList<>();

    public void addOrder(OrderEntity order) {
        orders.add(order);
        order.setCustomer(this);
    }

    public void removeOrder(OrderEntity order) {
        orders.remove(order);
        order.setCustomer(null);
    }
}
```

Without helper methods, the in-memory object graph may become inconsistent:

```text
Customer contains Order
but
Order.customer is null
```

---

# Question 3: What are the types of relationships in Hibernate?

There are four principal relationship types.

## 1. One-to-One

One record is associated with one other record.

Example:

```text
User 1 ───── 1 UserProfile
```

```java
@Entity
public class UserEntity {

    @Id
    private Long id;

    @OneToOne(
            cascade = CascadeType.ALL
    )
    @JoinColumn(
            name = "profile_id",
            unique = true
    )
    private UserProfileEntity profile;
}
```

```java
@Entity
public class UserProfileEntity {

    @Id
    private Long id;

    @OneToOne(mappedBy = "profile")
    private UserEntity user;
}
```

Typical use cases:

- User and profile
- Employee and parking space
- Account and account settings

---

## 2. One-to-Many

One parent is associated with multiple children.

```text
Customer 1 ───── * Orders
```

```java
@Entity
public class CustomerEntity {

    @Id
    private Long id;

    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderEntity> orders =
            new ArrayList<>();
}
```

The corresponding many-to-one side is:

```java
@Entity
public class OrderEntity {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private CustomerEntity customer;
}
```

A one-to-many association commonly maps through a foreign key in the child table. ([Jakarta EE][4])

Typical use cases:

- Customer and orders
- Order and line items
- Department and employees

---

## 3. Many-to-One

Many child records are associated with one parent record.

```text
Many Orders ───── 1 Customer
```

```java
@Entity
public class OrderEntity {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
}
```

`@ManyToOne` is usually placed on the entity containing the foreign key.

Database representation:

```text
orders
--------------------------------
id
order_number
customer_id  → customers.id
```

---

## 4. Many-to-Many

Many records on each side can be associated with many records on the other side.

```text
Students * ───── * Courses
```

This normally requires a join table:

```text
students
courses
student_courses
```

Owning side:

```java
@Entity
public class StudentEntity {

    @Id
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "student_courses",
            joinColumns = @JoinColumn(
                    name = "student_id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "course_id"
            )
    )
    private Set<CourseEntity> courses =
            new HashSet<>();
}
```

Inverse side:

```java
@Entity
public class CourseEntity {

    @Id
    private Long id;

    @ManyToMany(mappedBy = "courses")
    private Set<StudentEntity> students =
            new HashSet<>();
}
```

### When to replace `@ManyToMany` with a join entity

Suppose the join table contains additional data:

```text
student_courses
--------------------------------
student_id
course_id
enrolled_at
grade
status
```

Model it as a separate entity:

```text
Student
   ↓ one-to-many
Enrollment
   ↑ many-to-one
Course
```

```java
@Entity
public class EnrollmentEntity {

    @EmbeddedId
    private EnrollmentId id;

    @ManyToOne
    @MapsId("studentId")
    private StudentEntity student;

    @ManyToOne
    @MapsId("courseId")
    private CourseEntity course;

    private LocalDate enrolledAt;

    private String status;
}
```

A join entity is usually clearer when the relationship has its own attributes, lifecycle, or business rules.

---

# Relationship Direction

Relationships can also be classified by navigation direction.

## Unidirectional relationship

Only one entity references the other.

```java
@Entity
public class OrderEntity {

    @ManyToOne
    private CustomerEntity customer;
}
```

You can navigate:

```text
Order → Customer
```

but not:

```text
Customer → Orders
```

unless you perform a separate query.

---

## Bidirectional relationship

Both entities reference each other.

```text
Customer → Orders
Order → Customer
```

Example:

```java
@OneToMany(mappedBy = "customer")
private List<OrderEntity> orders;
```

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private CustomerEntity customer;
```

Bidirectional mappings are useful when both navigation directions are required, but they introduce additional concerns:

- Keeping both sides synchronized
- Recursive JSON serialization
- Larger object graphs
- Accidental lazy loading
- More complicated equality and `toString()` behavior

Do not make every association bidirectional automatically.

---

# Common Relationship Attributes

## `mappedBy`

Identifies the field that owns a bidirectional relationship.

```java
@OneToMany(mappedBy = "customer")
```

It contains a Java field or property name—not a database column name.

---

## `cascade`

Controls whether an operation is propagated to related entities.

```java
@OneToMany(
        mappedBy = "order",
        cascade = {
                CascadeType.PERSIST,
                CascadeType.MERGE
        }
)
```

Common cascade types include:

```text
PERSIST
MERGE
REMOVE
REFRESH
DETACH
ALL
```

Do not apply `CascadeType.ALL` automatically. For example, cascading removal from a many-to-many association can delete more data than intended.

---

## `orphanRemoval`

Removes a child entity when it is removed from the parent collection.

```java
@OneToMany(
        mappedBy = "order",
        orphanRemoval = true
)
private List<LineItemEntity> lineItems;
```

```java
order.getLineItems().remove(lineItem);
```

With orphan removal, the removed line item can be deleted from the database during synchronization.

This is different from cascade removal:

```text
Cascade REMOVE:
Delete the parent → delete related children.

orphanRemoval:
Remove a child from the relationship → delete that child.
```

---

## `fetch`

Controls when an association is fetched.

```java
@ManyToOne(fetch = FetchType.LAZY)
private CustomerEntity customer;
```

The two options are:

```text
FetchType.LAZY
FetchType.EAGER
```

Fetch strategy should be selected carefully because lazy loading can produce N+1 queries, while eager loading can retrieve unnecessary data.

---

# Common Mistakes

## Returning bidirectional entities directly from REST controllers

```java
@GetMapping("/{id}")
public CustomerEntity findCustomer(
        @PathVariable long id
) {
    return repository.findById(id)
            .orElseThrow();
}
```

This may cause recursive serialization:

```text
Customer
→ Orders
→ Customer
→ Orders
→ ...
```

Prefer API DTOs:

```java
public record CustomerResponse(
        Long id,
        String name
) {
}
```

---

## Placing `mappedBy` on the wrong side

Incorrect:

```java
@ManyToOne
private CustomerEntity customer;
```

The many-to-one side owns the foreign key and does not normally use `mappedBy`.

The inverse one-to-many side uses it:

```java
@OneToMany(mappedBy = "customer")
private List<OrderEntity> orders;
```

---

## Using cascade remove carelessly

Deleting one entity should not accidentally delete a shared entity.

For example:

```text
Delete one student
≠
Delete every course attended by that student
```

Therefore, `CascadeType.REMOVE` is often inappropriate for many-to-many relationships.

---

# Quick Comparison

| Relationship | Annotation    | Example              |
| ------------ | ------------- | -------------------- |
| One-to-one   | `@OneToOne`   | User and profile     |
| One-to-many  | `@OneToMany`  | Customer and orders  |
| Many-to-one  | `@ManyToOne`  | Orders and customer  |
| Many-to-many | `@ManyToMany` | Students and courses |

# Short Interview Summary

> `@Transactional` may fail on self-invocation because Spring normally applies it through a proxy, while an internal method call uses `this` and bypasses that proxy. I usually solve it by moving the transactional operation to another Spring-managed service. In Hibernate, relationships are mapped using `@OneToOne`, `@OneToMany`, `@ManyToOne`, and `@ManyToMany`. The owning side controls the foreign key, while the inverse side uses `mappedBy`. I also define cascade, fetch strategy, orphan removal, and relationship direction carefully rather than applying the same settings to every association.

[1]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com "Using @Transactional :: Spring Framework"
[2]: https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/manytoone?utm_source=chatgpt.com "ManyToOne (Jakarta Persistence API documentation)"
[3]: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2?utm_source=chatgpt.com "Jakarta Persistence"
[4]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/onetomany?utm_source=chatgpt.com "OneToMany (Jakarta Persistence API documentation)"
