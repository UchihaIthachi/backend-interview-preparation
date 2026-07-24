# Basic Questions — Lambdas, Streams, Parallel Streams, and `Optional`

## Question 1: What are lambda expressions? Provide a use case.

A lambda expression is a concise way to provide an implementation of a **functional interface**.

A functional interface has exactly one abstract method.

```java
@FunctionalInterface
public interface Calculator {
    int calculate(int first, int second);
}
```

Lambda implementation:

```java
Calculator addition =
        (first, second) -> first + second;

int result = addition.calculate(10, 20);

System.out.println(result); // 30
```

Before lambdas, the same behavior required an anonymous class:

```java
Calculator addition = new Calculator() {
    @Override
    public int calculate(
            int first,
            int second
    ) {
        return first + second;
    }
};
```

### Common syntax forms

No parameters:

```java
Runnable task =
        () -> System.out.println("Running");
```

One parameter:

```java
Consumer<String> printer =
        value -> System.out.println(value);
```

Multiple parameters:

```java
Comparator<String> comparator =
        (first, second) ->
                first.compareTo(second);
```

Multiple statements:

```java
BiFunction<Integer, Integer, Integer> addition =
        (first, second) -> {
            int result = first + second;
            return result;
        };
```

### Collection use case

```java
List<Integer> numbers =
        Arrays.asList(1, 2, 3);

numbers.forEach(
        number -> System.out.println(number)
);
```

This can be shortened using a method reference:

```java
numbers.forEach(System.out::println);
```

### Production use case

Lambdas are commonly used with:

- Streams
- Collection sorting
- Event listeners
- `CompletableFuture`
- Retry policies
- Spring callbacks
- Functional configuration

Example:

```java
List<User> users =
        new ArrayList<>();

users.sort(
        Comparator.comparing(User::name)
);
```

### Captured variables

A lambda may capture local variables only when they are final or effectively final.

```java
String prefix = "USER-";

users.forEach(user ->
        System.out.println(prefix + user.id())
);
```

This is invalid because `prefix` is reassigned:

```java
String prefix = "USER-";
prefix = "ACCOUNT-";

// Compilation error
users.forEach(user ->
        System.out.println(prefix + user.id())
);
```

### Interview-ready answer

> A lambda expression provides a concise implementation of a functional interface. It is commonly used with streams, collection sorting, callbacks, and asynchronous APIs. Lambdas reduce anonymous-class boilerplate while keeping Java statically typed.

---

## Question 2: Why are streams lazy?

Most intermediate stream operations are **lazy**.

They do not process elements immediately. Instead, they describe a pipeline that begins only when a terminal operation is invoked.

```java
Stream<String> stream =
        names.stream()
             .filter(name -> {
                 System.out.println(
                         "Filtering " + name
                 );
                 return name.startsWith("A");
             });
```

At this point, no filtering has occurred.

Processing starts here:

```java
List<String> result =
        stream.toList();
```

### Intermediate operations

Examples include:

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

These return another stream and normally do not execute the pipeline by themselves.

### Terminal operations

Examples include:

```java
toList()
collect()
forEach()
reduce()
count()
findFirst()
findAny()
anyMatch()
allMatch()
noneMatch()
```

A terminal operation triggers evaluation.

---

### Example of lazy execution

```java
List<String> names =
        List.of(
                "Alice",
                "Bob",
                "Alex",
                "Andrew"
        );

Optional<String> result =
        names.stream()
             .filter(name -> {
                 System.out.println(
                         "Filtering: " + name
                 );

                 return name.startsWith("A");
             })
             .map(name -> {
                 System.out.println(
                         "Mapping: " + name
                 );

                 return name.toUpperCase();
             })
             .findFirst();
```

Possible output:

```text
Filtering: Alice
Mapping: Alice
```

The stream stops after finding the first matching result. It does not process every element.

---

### Benefits of laziness

#### 1. Short-circuiting

Operations such as the following can stop early:

```java
findFirst()
findAny()
anyMatch()
allMatch()
noneMatch()
limit()
```

Example:

```java
boolean containsAdmin =
        users.stream()
             .anyMatch(User::isAdmin);
```

Once an administrator is found, remaining elements do not need to be examined.

#### 2. Operation fusion

Instead of filtering the entire collection and then mapping the entire result, streams process elements through the combined pipeline.

Conceptually:

```text
element 1 → filter → map
element 2 → filter → map
element 3 → filter → map
```

rather than:

```text
filter all elements
        ↓
store intermediate result
        ↓
map all filtered elements
```

#### 3. Avoiding unnecessary work

Combined with short-circuiting, laziness reduces processing and temporary allocations.

### Important nuance

Not every intermediate operation can operate on one element at a time.

Stateful operations such as:

```java
sorted()
distinct()
```

may need to buffer or inspect multiple elements before producing results.

### Interview-ready answer

> Streams are lazy because intermediate operations only describe a processing pipeline. Actual evaluation starts when a terminal operation is called. Laziness enables operation fusion, short-circuiting, and avoidance of unnecessary work.

---

## Question 3: Why should stream operations avoid shared mutable state?

Stream pipelines should avoid modifying shared mutable state because it can create:

- Race conditions
- Lost updates
- Nondeterministic results
- Ordering problems
- Difficult-to-debug behavior
- Failures when switching to parallel execution

### Problematic example

```java
List<Integer> result =
        new ArrayList<>();

numbers.parallelStream()
       .filter(number -> number % 2 == 0)
       .forEach(result::add);
```

`ArrayList` is not thread-safe. Multiple worker threads may call `add()` concurrently.

Possible consequences include:

- Missing elements
- Corrupted internal state
- Incorrect size
- Exceptions
- Results that vary between runs

---

### Better solution

Use a stream collector:

```java
List<Integer> result =
        numbers.parallelStream()
               .filter(number ->
                       number % 2 == 0
               )
               .toList();
```

The stream framework manages intermediate result containers and combines them safely.

---

### Another bad example

```java
int[] total = {0};

numbers.parallelStream()
       .forEach(number ->
               total[0] += number
       );
```

The update is not atomic.

Better:

```java
int total =
        numbers.parallelStream()
               .mapToInt(Integer::intValue)
               .sum();
```

---

### Side effects can also make sequential streams unclear

Even without parallel execution, this design mixes transformation with mutation:

```java
List<String> output =
        new ArrayList<>();

users.stream()
     .filter(User::active)
     .forEach(user ->
             output.add(user.name())
     );
```

Prefer:

```java
List<String> output =
        users.stream()
             .filter(User::active)
             .map(User::name)
             .toList();
```

The second version is:

- Declarative
- Easier to reason about
- Easier to test
- Safer to parallelize
- Less dependent on execution order

---

### Side effects are not always forbidden

Some terminal operations intentionally perform effects:

```java
users.forEach(notificationService::notify);
```

The important requirement is that the operation should be safe for the selected execution mode.

For parallel streams:

- The target service must be thread-safe.
- Ordering assumptions must be avoided.
- Shared mutable state must be coordinated.
- Blocking I/O may make parallel streams unsuitable.

### Interview-ready answer

> Stream operations should avoid shared mutable state because streams may execute elements independently or in parallel. Mutating the same object can cause race conditions and nondeterministic results. I prefer transformations and collectors such as `map`, `filter`, `reduce`, and `collect` rather than manually updating shared containers.

---

## Question 4: What problem does `Optional` solve, and what is a common misuse?

`Optional<T>` represents a value that may or may not be present.

```java
Optional<User> user =
        userRepository.findById(userId);
```

It makes absence explicit in the API instead of returning an undocumented `null`.

### Creating an `Optional`

Empty:

```java
Optional<User> user =
        Optional.empty();
```

Known non-null value:

```java
Optional<User> user =
        Optional.of(existingUser);
```

Possibly null value:

```java
Optional<User> user =
        Optional.ofNullable(possiblyNullUser);
```

`Optional.of(null)` throws `NullPointerException`.

---

### Reading an optional value

Using `orElseThrow()`:

```java
User user = userRepository
        .findById(userId)
        .orElseThrow(() ->
                new UserNotFoundException(userId)
        );
```

Using `map()`:

```java
String email = userRepository
        .findById(userId)
        .map(User::email)
        .orElse("unknown@example.com");
```

Using `ifPresent()`:

```java
userRepository
        .findById(userId)
        .ifPresent(
                notificationService::notify
        );
```

---

### Problem solved

Without `Optional`:

```java
User user = repository.findById(userId);

if (user != null) {
    process(user);
}
```

With `Optional`:

```java
repository.findById(userId)
          .ifPresent(this::process);
```

The return type communicates that absence is expected and must be handled.

---

### Common misuse 1: Calling `get()` without checking

Avoid:

```java
User user =
        optionalUser.get();
```

If the optional is empty, this throws `NoSuchElementException`.

Prefer:

```java
User user = optionalUser.orElseThrow(
        UserNotFoundException::new
);
```

---

### Common misuse 2: Using `isPresent()` followed by `get()`

Verbose:

```java
if (optionalUser.isPresent()) {
    User user = optionalUser.get();
    process(user);
}
```

Prefer:

```java
optionalUser.ifPresent(this::process);
```

Or:

```java
User user = optionalUser.orElseThrow();
```

---

### Common misuse 3: Using `Optional` as a field

Usually avoid:

```java
public class User {

    private Optional<String> middleName;
}
```

Prefer storing the underlying value:

```java
public class User {

    private String middleName;
}
```

and exposing absence at an API boundary where useful:

```java
public Optional<String> middleName() {
    return Optional.ofNullable(middleName);
}
```

`Optional` was primarily designed as a return type, not as a universal replacement for nullable fields.

---

### Common misuse 4: Using `Optional` as a parameter

Avoid:

```java
void sendEmail(
        Optional<String> subject
) {
}
```

This creates multiple ways to represent absence:

```java
sendEmail(null);
sendEmail(Optional.empty());
```

Prefer:

```java
void sendEmail(String subject) {
}
```

or overload the method when the distinction is meaningful.

---

### Common misuse 5: Returning `Optional<List<T>>`

Avoid:

```java
Optional<List<User>> findUsers();
```

A collection already represents zero or more values.

Prefer:

```java
List<User> findUsers();
```

Return an empty list when no users exist.

---

### `orElse()` vs `orElseGet()`

`orElse()` evaluates the fallback immediately:

```java
User user =
        optionalUser.orElse(
                createDefaultUser()
        );
```

`createDefaultUser()` runs even when the optional contains a value.

`orElseGet()` evaluates lazily:

```java
User user =
        optionalUser.orElseGet(
                this::createDefaultUser
        );
```

Use `orElseGet()` when fallback creation is expensive or has side effects.

### Interview-ready answer

> `Optional` makes an optional return value explicit and encourages callers to handle absence safely. A common misuse is calling `get()` directly or using `isPresent()` followed by `get()`. It is also usually inappropriate as an entity field, method parameter, or wrapper around a collection.

---

# Streams and Functional Programming

## Question 5: What is the difference between a sequential stream and a parallel stream?

A sequential stream processes its pipeline using one thread at a time.

```java
long count =
        users.stream()
             .filter(User::active)
             .count();
```

A parallel stream divides the source into smaller parts and processes them concurrently, commonly using the shared Fork/Join common pool.

```java
long count =
        users.parallelStream()
             .filter(User::active)
             .count();
```

A sequential stream can also be converted to parallel:

```java
users.stream()
     .parallel()
     .filter(User::active)
     .count();
```

---

### Comparison

| Feature               | Sequential stream                         | Parallel stream                                   |
| --------------------- | ----------------------------------------- | ------------------------------------------------- |
| Execution             | One processing thread                     | Multiple worker threads                           |
| Ordering              | Easier to reason about                    | Depends on operation and terminal method          |
| Overhead              | Low                                       | Splitting, coordination, and merging              |
| Best for              | Small or ordinary workloads               | Large CPU-intensive, easily partitioned workloads |
| Shared state risk     | Lower                                     | Much higher                                       |
| Blocking I/O          | Usually clearer with explicit concurrency | Can exhaust common-pool workers                   |
| Performance guarantee | No                                        | No                                                |

---

### When can a parallel stream help?

A parallel stream may help when:

- The input is large.
- Work per element is CPU-intensive.
- Operations are independent.
- The source splits efficiently.
- No shared mutable state exists.
- Ordering is unnecessary or inexpensive.
- Benchmarking proves improvement.

Example:

```java
long result =
        largeNumberList
                .parallelStream()
                .mapToLong(this::expensiveCalculation)
                .sum();
```

---

### When might it not help?

Avoid or carefully measure parallel streams when:

- The collection is small.
- Each operation is trivial.
- Encounter order must be preserved.
- Processing performs blocking network or database calls.
- The operation modifies shared state.
- The application already uses the common pool heavily.
- The source does not split efficiently.
- Per-element work has highly uneven cost.

Example to avoid:

```java
users.parallelStream()
     .forEach(user ->
             databaseRepository.save(user)
     );
```

Problems may include:

- Connection-pool exhaustion
- Uncontrolled database concurrency
- Transaction ambiguity
- Common-pool blocking
- Hard-to-predict throughput

Use explicit concurrency and bounded executors when operational control matters.

---

### Ordered terminal operations

This may not preserve encounter order:

```java
values.parallelStream()
      .forEach(System.out::println);
```

This preserves encounter order:

```java
values.parallelStream()
      .forEachOrdered(System.out::println);
```

However, preserving ordering can reduce parallel efficiency.

---

### Have you used parallel streams in a real project?

A strong interview answer should not claim that parallel streams are always better.

> I use parallel streams only after measuring a CPU-bound, independent workload. For backend I/O, database calls, or workflows requiring controlled concurrency, I prefer a bounded executor, `CompletableFuture`, or an application-specific worker pool because they provide clearer control over thread count, timeouts, and failure handling.

### Interview-ready answer

> A sequential stream runs the pipeline on one thread, while a parallel stream partitions the source and processes it across multiple workers. Parallel streams can help for large, CPU-intensive, stateless workloads, but they add coordination overhead and use a shared pool. I avoid them for blocking I/O, small collections, shared mutable state, and workloads requiring precise concurrency control.

---

## Question 6: What is the difference between `map()` and `flatMap()`?

The original phrase “Stream vs `flatMap`” should normally be written as:

> What is the difference between `map()` and `flatMap()`?

Both are intermediate stream operations.

---

### `map()`

`map()` transforms each input element into exactly one output value.

Conceptually:

```text
T → R
```

Example:

```java
List<String> names =
        List.of("alice", "bob");

List<String> uppercaseNames =
        names.stream()
             .map(String::toUpperCase)
             .toList();
```

Result:

```text
[ALICE, BOB]
```

Another example:

```java
List<Long> userIds =
        users.stream()
             .map(User::id)
             .toList();
```

---

### `flatMap()`

`flatMap()` transforms each input element into zero or more output elements and combines the nested streams into one stream.

Conceptually:

```text
T → Stream<R>
```

followed by:

```text
Stream<Stream<R>> → Stream<R>
```

Example:

```java
List<List<String>> groups =
        List.of(
                List.of("A", "B"),
                List.of("C", "D")
        );

List<String> values =
        groups.stream()
              .flatMap(List::stream)
              .toList();
```

Result:

```text
[A, B, C, D]
```

---

### `map()` producing a nested result

```java
List<Stream<String>> nested =
        groups.stream()
              .map(List::stream)
              .toList();
```

The result contains streams:

```text
[
    Stream<A, B>,
    Stream<C, D>
]
```

Using `flatMap()` removes the nesting.

---

### Splitting sentences into words

```java
List<String> sentences =
        List.of(
                "Java Streams",
                "Functional Programming"
        );

List<String> words =
        sentences.stream()
                 .flatMap(sentence ->
                         Arrays.stream(
                                 sentence.split(" ")
                         )
                 )
                 .toList();
```

Result:

```text
[Java, Streams, Functional, Programming]
```

---

### `Optional.flatMap()`

`flatMap()` is also useful with nested optionals.

Suppose:

```java
Optional<User> user =
        repository.findById(userId);
```

and:

```java
Optional<Address> address =
        user.address();
```

Using `map()`:

```java
Optional<Optional<Address>> result =
        user.map(User::address);
```

Using `flatMap()`:

```java
Optional<Address> result =
        user.flatMap(User::address);
```

`flatMap()` prevents nested containers.

---

### Comparison

| `map()`                        | `flatMap()`                               |
| ------------------------------ | ----------------------------------------- |
| One output value per input     | Zero or more output values per input      |
| Keeps nested containers        | Flattens one level                        |
| `T → R`                        | `T → Stream<R>`                           |
| Useful for property extraction | Useful for nested collections and streams |
| May produce `Stream<List<T>>`  | Can produce `Stream<T>`                   |

### Interview-ready answer

> `map()` converts each input element into one output value. `flatMap()` converts each input element into a stream or container of values and flattens those nested results into one stream. I use `map()` for property transformation and `flatMap()` for nested collections, tokenization, or avoiding nested optionals.

---

# Common Functional Interfaces

| Interface             | Abstract method                  | Purpose                         |
| --------------------- | -------------------------------- | ------------------------------- |
| `Predicate<T>`        | `boolean test(T value)`          | Test a condition                |
| `Function<T, R>`      | `R apply(T value)`               | Transform a value               |
| `Consumer<T>`         | `void accept(T value)`           | Consume a value                 |
| `Supplier<T>`         | `T get()`                        | Produce a value                 |
| `UnaryOperator<T>`    | `T apply(T value)`               | Transform value to same type    |
| `BinaryOperator<T>`   | `T apply(T first, T second)`     | Combine two values of same type |
| `BiFunction<T, U, R>` | `R apply(T first, U second)`     | Transform two inputs            |
| `Comparator<T>`       | `int compare(T first, T second)` | Define ordering                 |

Examples:

```java
Predicate<User> active =
        User::active;

Function<User, String> userName =
        User::name;

Consumer<User> notifyUser =
        notificationService::notify;

Supplier<UUID> idGenerator =
        UUID::randomUUID;
```

---

# Stream Pipeline Example

```java
List<String> activeAdminEmails =
        users.stream()
             .filter(User::active)
             .filter(user ->
                     user.roles()
                         .contains("ADMIN")
             )
             .map(User::email)
             .filter(Objects::nonNull)
             .map(email ->
                     email.toLowerCase(
                             Locale.ROOT
                     )
             )
             .distinct()
             .sorted()
             .toList();
```

Pipeline stages:

```text
Source
  ↓
Filter active users
  ↓
Filter administrators
  ↓
Extract email
  ↓
Remove null values
  ↓
Normalize case
  ↓
Remove duplicates
  ↓
Sort
  ↓
Collect result
```

---

# Common Mistakes

## 1. Reusing a stream

A stream can be consumed only once.

```java
Stream<String> stream =
        names.stream();

long count = stream.count();

// IllegalStateException
List<String> values =
        stream.toList();
```

Create a new stream for each traversal:

```java
long count = names.stream().count();
List<String> values = names.stream().toList();
```

---

## 2. Forgetting the terminal operation

This does not execute the filter:

```java
names.stream()
     .filter(name -> {
         System.out.println(name);
         return true;
     });
```

A terminal operation is required.

---

## 3. Using `peek()` for essential business logic

Avoid:

```java
orders.stream()
      .peek(order ->
              repository.save(order)
      )
      .toList();
```

`peek()` is mainly useful for debugging and observation. It may not execute if the stream is not consumed, and optimization or short-circuiting can affect which elements reach it.

Use explicit operations for important side effects.

---

## 4. Mutating source elements unexpectedly

```java
users.stream()
     .map(user -> {
         user.setActive(false);
         return user;
     })
     .toList();
```

This surprises callers because the source objects are changed.

Prefer immutable transformations where practical:

```java
users.stream()
     .map(User::deactivate)
     .toList();
```

---

## 5. Using parallel streams automatically

Do not assume:

```java
parallelStream()
```

is faster than:

```java
stream()
```

Always consider:

- Workload size
- Per-element cost
- Splitting quality
- Ordering
- Pool contention
- Blocking calls
- Benchmark results

---

## 6. Returning `null` from `flatMap()`

Incorrect:

```java
stream.flatMap(value -> null);
```

Return an empty stream:

```java
stream.flatMap(value ->
        value == null
                ? Stream.empty()
                : Stream.of(value)
);
```

---

## 7. Using `Optional.get()` directly

Avoid:

```java
optional.get();
```

Prefer:

```java
optional.orElseThrow();
optional.orElseGet(this::createDefault);
optional.ifPresent(this::process);
```

---

# Short Interview Answers

## What is a lambda?

> A lambda is a concise implementation of a functional interface. It is commonly used with streams, callbacks, comparators, and asynchronous APIs.

## Why are streams lazy?

> Intermediate operations only describe the pipeline. Execution starts with a terminal operation, enabling short-circuiting, operation fusion, and avoidance of unnecessary work.

## Why avoid shared mutable state?

> Stream operations may run independently or concurrently. Shared mutation creates race conditions and nondeterministic results, so collectors and reductions are preferred.

## What problem does `Optional` solve?

> `Optional` makes an absent return value explicit. A common misuse is calling `get()` directly or using it as a field, parameter, or wrapper around collections.

## Sequential vs parallel stream

> Sequential streams use one processing thread. Parallel streams partition work across multiple workers and are appropriate only for large, CPU-bound, independent workloads that benchmark well.

## `map()` vs `flatMap()`

> `map()` transforms one input into one output. `flatMap()` transforms one input into zero or more outputs and flattens the nested results.
