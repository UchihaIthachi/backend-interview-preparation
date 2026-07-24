# Java Streams: `map`, `flatMap`, `reduce`, and `collect`

## 1. Definition

A Java `Stream<T>` represents a sequence of elements processed through a pipeline of operations.

A stream:

- Does not normally store data itself.
- Reads data from a source such as a collection, array, file, or generator.
- Is evaluated lazily.
- Can usually be consumed only once.
- May execute sequentially or in parallel.

```java
Stream<String> stream = names.stream();
```

A stream pipeline contains:

```text
Source
  → intermediate operations
  → terminal operation
```

Example:

```java
List<String> adultNames = people.stream()
        .filter(person -> person.getAge() >= 18)
        .map(Person::getName)
        .sorted()
        .toList();
```

---

## 2. Why Do Streams Exist?

Streams allow data-processing logic to be expressed declaratively.

Instead of describing every mutation and loop step:

```java
List<String> result = new ArrayList<>();

for (Person person : people) {
    if (person.getAge() >= 18) {
        result.add(person.getName());
    }
}

Collections.sort(result);
```

a stream describes the required transformations:

```java
List<String> result = people.stream()
        .filter(person -> person.getAge() >= 18)
        .map(Person::getName)
        .sorted()
        .toList();
```

Streams are particularly useful for:

- Filtering
- Transforming
- Flattening nested data
- Aggregating values
- Grouping records
- Removing duplicates
- Sorting
- Building DTO collections

Streams should improve clarity. A loop is preferable when the pipeline becomes harder to understand than the equivalent imperative code.

---

# Stream Pipeline Execution

## 3. Intermediate Operations

Intermediate operations return another stream.

Common examples include:

```java
filter()
map()
flatMap()
distinct()
sorted()
limit()
skip()
peek()
```

They are normally lazy:

```java
Stream<String> pipeline = names.stream()
        .filter(name -> {
            System.out.println("Filtering: " + name);
            return name.startsWith("A");
        });
```

No filtering occurs until a terminal operation is invoked.

---

## 4. Terminal Operations

Terminal operations consume the stream and produce a result or side effect.

Examples include:

```java
toList()
collect()
reduce()
count()
forEach()
findFirst()
anyMatch()
allMatch()
noneMatch()
```

```java
List<String> result = pipeline.toList();
```

After a terminal operation, the same stream cannot normally be reused.

```java
Stream<String> stream = names.stream();

long count = stream.count();

// Throws IllegalStateException
List<String> values = stream.toList();
```

Create a new stream from the source when another traversal is required:

```java
long count = names.stream().count();
List<String> values = names.stream().toList();
```

---

## 5. Why Are Streams Lazy?

Laziness allows the stream runtime to avoid unnecessary work.

```java
Optional<String> firstMatch = names.stream()
        .filter(name -> {
            System.out.println("Filtering: " + name);
            return name.startsWith("A");
        })
        .map(String::toUpperCase)
        .findFirst();
```

Once the first matching element is found, the stream can stop processing.

Laziness enables:

- Short-circuiting
- Operation fusion
- Reduced intermediate allocation
- Processing of large or potentially unbounded sources

### Important nuance

Not every operation can process one element independently.

Stateful operations such as:

```java
sorted()
distinct()
```

may need to buffer or track multiple elements.

---

# `map()`

## 6. What Does `map()` Do?

`map()` transforms each input element into one output element.

Conceptually:

```text
T → R
```

Example:

```java
List<String> names = people.stream()
        .map(Person::getName)
        .toList();
```

Input:

```text
Person, Person, Person
```

Output:

```text
String, String, String
```

Another example:

```java
List<Integer> lengths = names.stream()
        .map(String::length)
        .toList();
```

Use `map()` when each input produces one transformed result.

---

# `flatMap()`

## 7. What Does `flatMap()` Do?

`flatMap()` transforms each input element into a stream of zero or more elements and combines those streams into one stream.

Conceptually:

```text
T → Stream<R>
```

followed by:

```text
Stream<Stream<R>> → Stream<R>
```

Suppose every order contains multiple line items:

```java
public class Order {

    private List<LineItem> lineItems;

    public List<LineItem> getLineItems() {
        return lineItems;
    }
}
```

Using `map()`:

```java
List<Stream<LineItem>> nestedStreams = orders.stream()
        .map(order -> order.getLineItems().stream())
        .toList();
```

This produces nested streams.

Using `flatMap()`:

```java
List<LineItem> allItems = orders.stream()
        .flatMap(order -> order.getLineItems().stream())
        .toList();
```

This produces one flat list of line items.

---

## 8. `map()` vs `flatMap()`

| `map()`                       | `flatMap()`                    |
| ----------------------------- | ------------------------------ |
| One output per input          | Zero or more outputs per input |
| Keeps nested structures       | Flattens one level             |
| `T → R`                       | `T → Stream<R>`                |
| Extracts or transforms values | Expands nested collections     |
| May produce `Stream<List<T>>` | Can produce `Stream<T>`        |

### Sentence-to-word example

```java
List<String> sentences = List.of(
        "Java Streams",
        "Functional Programming"
);

List<String> words = sentences.stream()
        .flatMap(sentence ->
                Arrays.stream(sentence.split("\\s+"))
        )
        .toList();
```

Result:

```text
[Java, Streams, Functional, Programming]
```

---

## 9. `Optional.map()` vs `Optional.flatMap()`

The same concept applies to `Optional`.

Suppose this method already returns an `Optional`:

```java
Optional<Address> findAddress(User user) {
    return Optional.ofNullable(user.getAddress());
}
```

Using `map()` creates a nested optional:

```java
Optional<Optional<Address>> address =
        optionalUser.map(this::findAddress);
```

Using `flatMap()` removes the nesting:

```java
Optional<Address> address =
        optionalUser.flatMap(this::findAddress);
```

---

# `reduce()`

## 10. What Does `reduce()` Do?

`reduce()` combines stream elements into one result.

Common examples include:

- Sum
- Product
- Minimum
- Maximum
- Concatenation
- Combining immutable values

### Sum example

```java
int total = numbers.stream()
        .reduce(0, Integer::sum);
```

The first argument is the identity value:

```text
0 + x = x
```

### Product example

```java
int product = numbers.stream()
        .reduce(1, (left, right) -> left * right);
```

### `BigDecimal` total

```java
BigDecimal total = lineItems.stream()
        .map(LineItem::getPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

---

## 11. Reduction Without an Identity

```java
Optional<Integer> maximum = numbers.stream()
        .reduce(Integer::max);
```

The result is `Optional<Integer>` because an empty stream has no maximum.

```java
int maximumValue = maximum.orElseThrow(
        () -> new IllegalArgumentException("No numbers provided")
);
```

---

## 12. Associativity Requirement

A reduction operation should be associative, especially for parallel execution.

An operation is associative when grouping does not change the result.

Addition:

```text
(a + b) + c = a + (b + c)
```

Subtraction is not associative:

```text
(10 - 5) - 2 ≠ 10 - (5 - 2)
```

Problematic:

```java
int result = numbers.parallelStream()
        .reduce(0, (left, right) -> left - right);
```

The result may depend on how the parallel stream partitions and combines the data.

---

## 13. Identity Requirements

The identity must be neutral for the accumulator.

Correct for addition:

```java
numbers.stream()
        .reduce(0, Integer::sum);
```

Correct for multiplication:

```java
numbers.stream()
        .reduce(1, (left, right) -> left * right);
```

Incorrect identity:

```java
numbers.stream()
        .reduce(10, Integer::sum);
```

This adds an unexpected initial value of `10`.

In parallel reductions, a wrong identity may be applied to multiple partial reductions and produce even more surprising results.

---

# `collect()`

## 14. What Does `collect()` Do?

`collect()` performs a mutable reduction.

It accumulates elements into a mutable result container such as:

- `List`
- `Set`
- `Map`
- `StringBuilder`
- Grouped data structure
- Custom result object

Example:

```java
List<String> names = people.stream()
        .map(Person::getName)
        .collect(Collectors.toList());
```

In newer Java versions, this can often be written as:

```java
List<String> names = people.stream()
        .map(Person::getName)
        .toList();
```

`Stream.toList()` returns an unmodifiable list. `Collectors.toList()` does not guarantee a particular list implementation or mutability contract.

When a mutable `ArrayList` is specifically required:

```java
List<String> names = people.stream()
        .map(Person::getName)
        .collect(Collectors.toCollection(ArrayList::new));
```

---

## 15. Common Collectors

### Collect into a set

```java
Set<String> uniqueNames = people.stream()
        .map(Person::getName)
        .collect(Collectors.toSet());
```

### Join strings

```java
String names = people.stream()
        .map(Person::getName)
        .collect(Collectors.joining(", "));
```

### Group values

```java
Map<Department, List<Employee>> employeesByDepartment =
        employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment
                ));
```

### Count grouped values

```java
Map<Department, Long> employeeCounts =
        employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        Collectors.counting()
                ));
```

### Partition values

```java
Map<Boolean, List<Employee>> partitioned =
        employees.stream()
                .collect(Collectors.partitioningBy(
                        Employee::isActive
                ));
```

### Convert to a map

```java
Map<Long, User> usersById = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                Function.identity()
        ));
```

---

## 16. Handling Duplicate Keys in `toMap()`

This may fail when duplicate keys exist:

```java
Map<String, User> usersByEmail = users.stream()
        .collect(Collectors.toMap(
                User::getEmail,
                Function.identity()
        ));
```

If two users share the same email, `toMap()` throws `IllegalStateException`.

Define a merge function:

```java
Map<String, User> usersByEmail = users.stream()
        .collect(Collectors.toMap(
                User::getEmail,
                Function.identity(),
                (existing, replacement) -> existing
        ));
```

Or group duplicate values:

```java
Map<String, List<User>> usersByEmail = users.stream()
        .collect(Collectors.groupingBy(
                User::getEmail
        ));
```

The correct behavior must follow the domain requirement rather than silently discarding data.

---

# `reduce()` vs `collect()`

## 17. Main Difference

| `reduce()`                         | `collect()`                                  |
| ---------------------------------- | -------------------------------------------- |
| Produces one combined value        | Accumulates into a mutable result            |
| Best for immutable scalar results  | Best for lists, maps, sets, and builders     |
| Uses identity and accumulator      | Uses supplier, accumulator, and combiner     |
| Example: sum or maximum            | Example: grouping or list creation           |
| Should avoid mutating the identity | Designed for controlled mutable accumulation |

### Use `reduce()` for scalar aggregation

```java
BigDecimal total = lineItems.stream()
        .map(LineItem::getPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

### Use `collect()` for containers

```java
Map<String, List<LineItem>> itemsByCategory =
        lineItems.stream()
                .collect(Collectors.groupingBy(
                        LineItem::getCategory
                ));
```

---

## 18. Do Not Use `reduce()` to Mutate a Collection

Avoid:

```java
List<String> names = people.stream()
        .map(Person::getName)
        .reduce(
                new ArrayList<>(),
                (list, name) -> {
                    list.add(name);
                    return list;
                },
                (left, right) -> {
                    left.addAll(right);
                    return left;
                }
        );
```

This mutates the reduction identity and can behave incorrectly in parallel pipelines.

Use `collect()`:

```java
List<String> names = people.stream()
        .map(Person::getName)
        .collect(Collectors.toCollection(ArrayList::new));
```

---

# Complete Production Example

## 19. Flatten and Aggregate Order Items

```java
List<LineItem> allItems = orders.stream()
        .flatMap(order -> order.getLineItems().stream())
        .toList();

BigDecimal total = allItems.stream()
        .map(LineItem::getPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

The pipeline can be combined:

```java
BigDecimal total = orders.stream()
        .flatMap(order -> order.getLineItems().stream())
        .map(LineItem::getPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

---

## 20. Group Totals by Category

```java
Map<String, BigDecimal> totalsByCategory = orders.stream()
        .flatMap(order -> order.getLineItems().stream())
        .collect(Collectors.groupingBy(
                LineItem::getCategory,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        LineItem::getPrice,
                        BigDecimal::add
                )
        ));
```

A clearer alternative may be:

```java
Map<String, BigDecimal> totalsByCategory = orders.stream()
        .flatMap(order -> order.getLineItems().stream())
        .collect(Collectors.toMap(
                LineItem::getCategory,
                LineItem::getPrice,
                BigDecimal::add
        ));
```

---

## 21. Entity-to-DTO Transformation

```java
List<UserDto> users = userEntities.stream()
        .filter(UserEntity::isActive)
        .map(entity -> new UserDto(
                entity.getId(),
                entity.getName(),
                entity.getEmail()
        ))
        .toList();
```

This is a common and appropriate service-layer use of streams when the mapping remains simple.

---

# Side Effects and Shared State

## 22. Avoid External Mutation

Problematic:

```java
List<String> names = new ArrayList<>();

people.stream()
        .map(Person::getName)
        .forEach(names::add);
```

Prefer:

```java
List<String> names = people.stream()
        .map(Person::getName)
        .toList();
```

External mutation:

- Makes the pipeline harder to reason about.
- Can fail with parallel execution.
- Introduces race conditions.
- Mixes transformation and side effects.

---

## 23. Side Effects Are Sometimes Intentional

A terminal operation may intentionally perform a side effect:

```java
notifications.forEach(notificationService::send);
```

However, for production operations consider:

- Failure handling
- Retry behavior
- Partial completion
- Idempotency
- Ordering
- Transaction boundaries
- Concurrency limits

A stream does not automatically provide these guarantees.

---

# Sequential and Parallel Streams

## 24. Parallel Streams Are Not Automatically Faster

```java
long total = values.parallelStream()
        .mapToLong(this::expensiveCalculation)
        .sum();
```

Parallel streams may help when:

- The data set is large.
- Work is CPU-intensive.
- Operations are independent.
- The source splits efficiently.
- Ordering requirements are limited.
- Shared mutable state is absent.
- Benchmarking demonstrates improvement.

They may perform worse because of:

- Task-splitting overhead
- Worker scheduling
- Result merging
- Shared common-pool contention
- Synchronization
- Ordering constraints
- Poor workload balance
- Additional allocation

Avoid using parallel streams by default for:

- Database calls
- Remote API calls
- Small collections
- Cheap transformations
- Request processing requiring bounded concurrency
- Code with shared mutable state

---

# Streams vs Loops

## 25. When Should a Loop Be Preferred?

Use a loop when:

- Complex branching is involved.
- Multiple variables change together.
- Early exits are central to the algorithm.
- Checked exceptions need careful handling.
- Detailed debugging is required.
- The stream pipeline becomes deeply nested.
- Side effects are the primary purpose.
- Performance profiling shows the loop is clearer or faster.

Example where a loop may be clearer:

```java
for (Order order : orders) {
    if (order.isCancelled()) {
        continue;
    }

    if (order.requiresManualReview()) {
        reviewQueue.add(order);
        continue;
    }

    try {
        paymentService.process(order);
    } catch (PaymentException exception) {
        failedOrders.add(order);
    }
}
```

Forcing this workflow into a stream may reduce readability.

---

# Common Mistakes

## 26. Forgetting That Streams Are Single-Use

```java
Stream<String> stream = names.stream();

stream.count();
stream.toList(); // IllegalStateException
```

---

## 27. Forgetting a Terminal Operation

```java
names.stream()
        .filter(name -> {
            System.out.println(name);
            return true;
        });
```

This pipeline is never executed.

---

## 28. Using `peek()` for Business Logic

Avoid:

```java
orders.stream()
        .peek(repository::save)
        .toList();
```

`peek()` is mainly intended for observation and debugging.

Use an explicit operation for essential side effects.

---

## 29. Returning `null` from `flatMap()`

Incorrect:

```java
stream.flatMap(value -> null);
```

Correct:

```java
stream.flatMap(value ->
        value == null
                ? Stream.empty()
                : Stream.of(value)
);
```

---

## 30. Assuming `sorted()` Changes the Source

```java
List<Integer> sorted = numbers.stream()
        .sorted()
        .toList();
```

The original list is not modified.

```java
System.out.println(numbers);
System.out.println(sorted);
```

---

## 31. Ignoring Null Values

This may throw `NullPointerException`:

```java
names.stream()
        .map(String::toUpperCase)
        .toList();
```

Filter nulls first:

```java
List<String> normalized = names.stream()
        .filter(Objects::nonNull)
        .map(name -> name.toUpperCase(Locale.ROOT))
        .toList();
```

---

## 32. Using Non-Associative Parallel Reduction

Avoid:

```java
int result = numbers.parallelStream()
        .reduce(0, (left, right) -> left - right);
```

Use an associative operation or process sequentially when order is essential.

---

## 33. Creating Unreadable Pipelines

Avoid excessively nested pipelines:

```java
Map<String, List<Result>> result = data.stream()
        .filter(...)
        .flatMap(...)
        .map(...)
        .collect(Collectors.groupingBy(
                value -> transform(
                        calculate(
                                value.getNested().getValue()
                        )
                )
        ));
```

Extract meaningful functions:

```java
Map<String, List<Result>> result = data.stream()
        .filter(this::isEligible)
        .flatMap(this::expand)
        .map(this::toResult)
        .collect(Collectors.groupingBy(
                this::groupingKey
        ));
```

---

# Trade-Offs

| Streams                           | Loops                         |
| --------------------------------- | ----------------------------- |
| Declarative                       | Imperative                    |
| Composable transformations        | Direct control flow           |
| Good for filtering and mapping    | Good for complex branching    |
| Built-in aggregation and grouping | Manual accumulation           |
| Lazy execution                    | Immediate execution           |
| Parallel option available         | Explicit concurrency required |
| Can become difficult to debug     | Straightforward breakpoints   |
| Side effects should be controlled | Side effects are explicit     |

---

# Interview Questions

## Question 1: What is the difference between `map()` and `flatMap()`?

> `map()` transforms each element into one result. `flatMap()` transforms each element into a stream of results and combines those streams into one flat stream.

## Question 2: Why are streams lazy?

> Intermediate operations only describe the pipeline. A terminal operation triggers execution. Laziness enables short-circuiting, operation fusion, and avoidance of unnecessary work.

## Question 3: What is the difference between `reduce()` and `collect()`?

> `reduce()` combines elements into one immutable-style result such as a number or value object. `collect()` performs mutable reduction into structures such as lists, sets, maps, or grouped results.

## Question 4: Why can parallel streams reduce performance?

> Parallel streams add splitting, scheduling, synchronization, and merging overhead. They also commonly share the Fork/Join common pool, so small, blocking, ordered, or contended workloads can perform worse.

## Question 5: When should a loop be preferred?

> A loop is often better when the logic has complex branching, multiple side effects, checked exceptions, early exits, or when the stream pipeline becomes harder to understand.

## Question 6: Can a stream be reused?

> No. After a terminal operation consumes a stream, create a new stream from the source for another traversal.

## Question 7: Is `Stream.toList()` mutable?

> No. `Stream.toList()` returns an unmodifiable list. Use an explicit collector such as `Collectors.toCollection(ArrayList::new)` when a mutable list is required.

---

# Short Interview Answer

> A stream is a lazy, single-use pipeline over a data source. Intermediate operations such as `filter`, `map`, and `flatMap` build the pipeline, while terminal operations such as `collect`, `reduce`, and `count` trigger execution. I use `map` for one-to-one transformations, `flatMap` for flattening nested values, `reduce` for scalar aggregation, and `collect` for mutable result containers such as lists and maps. I avoid shared mutable state and use parallel streams only when profiling proves they benefit a large CPU-bound workload.
