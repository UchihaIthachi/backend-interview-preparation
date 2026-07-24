# Java Strings, the String Pool, and `StringBuilder`

## 1. Definition

`String` is an immutable class representing a sequence of UTF-16 code units.

```java
String message = "Hello, Java!";
```

Once a `String` object is created, its content cannot change. Operations that produce different text return another `String` rather than modifying the existing object.

```java
String original = "Hello";
String updated = original.concat(" World");

System.out.println(original); // Hello
System.out.println(updated);  // Hello World
```

`StringBuilder` and `StringBuffer` are mutable character-sequence classes intended for constructing or repeatedly modifying text.

---

## 2. Why Is `String` Immutable?

String immutability provides several important benefits.

### Safe sharing

The same `String` instance can be shared across different components without one component changing its contents unexpectedly.

### Thread safety

Because its state cannot change after construction, a `String` can be safely read by multiple threads without synchronization.

### String Pool safety

Identical string literals may share one pooled instance.

```java
String first = "Java";
String second = "Java";
```

Immutability ensures that modifying one reference cannot affect every other reference to the pooled value.

### Stable hash codes

Strings are frequently used as keys in hash-based collections:

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
```

Because the content cannot change, the string’s equality and hash code remain stable while it is stored in the map.

### Security and predictability

Strings commonly represent:

- File paths
- URLs
- Class names
- Configuration properties
- Usernames
- Protocol values

Immutability prevents the same object from being altered after validation.

However, secrets such as passwords may be better stored in mutable arrays when controlled clearing is required:

```java
char[] password = readPassword();

try {
    authenticate(password);
} finally {
    Arrays.fill(password, '\0');
}
```

---

# The String Pool

## 3. What Is the String Pool?

The String Pool is a JVM-managed table of canonical string instances.

When Java encounters identical string literals, they normally refer to the same pooled object.

```java
String first = "hello";
String second = "hello";

System.out.println(first == second); // true
```

```text
first  ─┐
        ├──> pooled "hello"
second ─┘
```

Using `new String(...)` explicitly creates a separate object:

```java
String first = "hello";
String second = new String("hello");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

- `==` compares object references.
- `equals()` compares string content.

---

## 4. What Does `intern()` Do?

The `intern()` method returns the canonical pooled representation of a string.

```java
String first = "hello";
String second = new String("hello").intern();

System.out.println(first == second); // true
```

When an equal pooled string already exists, `intern()` returns that instance.

Manual interning should usually be avoided unless memory profiling shows that an application holds a very large number of repeated string values. Interning many unique values may increase memory retention and lookup overhead.

---

## 5. Compile-Time and Runtime Concatenation

### Compile-time constant concatenation

The compiler can combine constant string expressions:

```java
String message = "Hello" + " " + "Java";
```

This can be compiled as the single constant:

```java
String message = "Hello Java";
```

Therefore, this does not necessarily create several runtime string objects.

### Runtime concatenation

```java
String name = "Alice";
String message = "Hello " + name;
```

The compiler and JVM use optimized concatenation mechanisms. The final result is still a new immutable string, but implementation details should not be assumed from the source expression alone.

---

## 6. Do All String Methods Always Allocate a New Object?

No.

String methods cannot modify the original object, but they are not required to create a distinct object when the result is unchanged.

For example, an implementation may return the original object when:

- `substring()` selects the complete string.
- `replace()` finds nothing to replace.
- A case-conversion operation makes no changes.

Therefore, the accurate rule is:

> String operations never mutate the existing object. They return a string representing the result, which may be the same instance or a new instance.

---

# Repeated Concatenation

## 7. Why Is `+=` Inside a Loop Expensive?

Consider:

```java
String result = "";

for (int index = 0; index < 10_000; index++) {
    result += index;
}
```

Because `String` is immutable, each iteration must conceptually:

1. Create space for the new result.
2. Copy the previous content.
3. Append the new value.
4. Reassign the reference.

As the string grows, increasingly large amounts of existing text are copied.

For repeatedly appending similarly sized values, the total copying cost can approach quadratic behavior relative to the final output length.

---

## 8. Use `StringBuilder` for Repeated Construction

```java
StringBuilder builder = new StringBuilder();

for (int index = 0; index < 10_000; index++) {
    builder.append(index);
}

String result = builder.toString();
```

`StringBuilder` modifies a resizable internal buffer and expands it when required.

Appending is typically amortized efficient, so building a large string is generally much cheaper than repeatedly creating immutable intermediate strings.

Avoid depending on a particular internal representation such as `char[]`; that is an implementation detail that may vary between Java implementations and versions.

---

## 9. Common `StringBuilder` Operations

```java
StringBuilder builder = new StringBuilder("Java");

builder.append(" Programming");
builder.insert(4, " Language");
builder.replace(0, 4, "Modern");
builder.delete(0, 7);
builder.reverse();

String result = builder.toString();
```

Important methods include:

| Method         | Purpose                            |
| -------------- | ---------------------------------- |
| `append()`     | Adds content to the end            |
| `insert()`     | Inserts content at an index        |
| `delete()`     | Removes a range                    |
| `replace()`    | Replaces a range                   |
| `reverse()`    | Reverses the current content       |
| `setCharAt()`  | Replaces one character             |
| `length()`     | Returns the current content length |
| `capacity()`   | Returns current internal capacity  |
| `setLength(0)` | Clears the logical content         |
| `toString()`   | Produces an immutable `String`     |

---

# `StringBuilder` vs `StringBuffer`

## 10. Comparison

| Feature                                  | `StringBuilder`                   | `StringBuffer`                            |
| ---------------------------------------- | --------------------------------- | ----------------------------------------- |
| Mutable                                  | Yes                               | Yes                                       |
| Method synchronization                   | No                                | Yes                                       |
| Thread-safe individual operations        | No                                | Yes                                       |
| Compound operations automatically atomic | No                                | No                                        |
| Typical performance                      | Usually faster                    | Usually slower because of synchronization |
| Recommended use                          | Thread-confined text construction | Legacy or genuinely shared mutable text   |

`StringBuilder` should normally be the default choice.

```java
public String formatOrder(Order order) {
    StringBuilder builder = new StringBuilder();

    builder.append(order.getId());
    builder.append(':');
    builder.append(order.getStatus());

    return builder.toString();
}
```

Even when many threads call this method, it is safe because each invocation creates its own builder.

---

## 11. Does `StringBuffer` Solve Every Thread-Safety Problem?

No.

`StringBuffer` synchronizes individual method calls, but a sequence of calls is not automatically atomic.

```java
if (buffer.length() > 0) {
    buffer.deleteCharAt(buffer.length() - 1);
}
```

Another thread could modify the buffer between the calls.

When the complete sequence must be atomic, external coordination is required:

```java
synchronized (buffer) {
    if (buffer.length() > 0) {
        buffer.deleteCharAt(buffer.length() - 1);
    }
}
```

In many designs, it is better to avoid sharing mutable text buffers entirely.

---

# Production Use Cases

## 12. Building Text in a Loop

```java
public String createReport(List<Order> orders) {
    StringBuilder report = new StringBuilder();

    for (Order order : orders) {
        report.append(order.getId())
              .append(',')
              .append(order.getStatus())
              .append(System.lineSeparator());
    }

    return report.toString();
}
```

---

## 13. Logging

Do not manually build expensive log strings when parameterized logging is available.

Avoid:

```java
String message = new StringBuilder()
        .append("Processing order ")
        .append(order.getId())
        .append(" for customer ")
        .append(order.getCustomerId())
        .toString();

log.debug(message);
```

Prefer:

```java
log.debug(
        "Processing order {} for customer {}",
        order.getId(),
        order.getCustomerId()
);
```

This is clearer and allows the logging framework to delay formatting when the log level is disabled.

---

## 14. SQL Construction

`StringBuilder` may be useful for building a dynamic query structure:

```java
StringBuilder sql = new StringBuilder(
        "SELECT id, name FROM users WHERE 1 = 1"
);

if (status != null) {
    sql.append(" AND status = ?");
}

if (createdAfter != null) {
    sql.append(" AND created_at >= ?");
}
```

Dynamic values must still be passed as prepared-statement parameters.

Never append untrusted values directly:

```java
// SQL-injection risk
sql.append(" AND username = '" + username + "'");
```

---

## 15. JSON Generation

Avoid manually constructing complex JSON:

```java
String json = "{"
        + "\"name\":\"" + user.getName() + "\","
        + "\"email\":\"" + user.getEmail() + "\""
        + "}";
```

Use a serialization library so escaping, null handling, and nested structures are handled correctly.

`StringBuilder` is suitable for text construction, but it is not a replacement for a domain-specific serializer.

---

# Common Mistakes

## 16. Comparing Strings Using `==`

Incorrect:

```java
if (username == "admin") {
}
```

Correct:

```java
if ("admin".equals(username)) {
}
```

Null-safe alternative:

```java
if (Objects.equals(username, expectedUsername)) {
}
```

---

## 17. Ignoring the Returned String

Incorrect:

```java
String name = "alice";

name.toUpperCase();

System.out.println(name); // alice
```

Correct:

```java
name = name.toUpperCase();

System.out.println(name); // ALICE
```

String methods cannot modify the original object.

---

## 18. Using `new String()` Without a Reason

Avoid:

```java
String language = new String("Java");
```

Prefer:

```java
String language = "Java";
```

---

## 19. Reusing a Builder Without Resetting It

```java
StringBuilder builder = new StringBuilder();

builder.append("First");
String first = builder.toString();

builder.append("Second");
String second = builder.toString();
```

`second` contains:

```text
FirstSecond
```

Clear the builder before reuse:

```java
builder.setLength(0);
builder.append("Second");
```

A new local builder is often clearer than reusing one.

---

## 20. Sharing a Static `StringBuilder`

Unsafe:

```java
public final class Formatter {

    private static final StringBuilder BUILDER =
            new StringBuilder();

    public static String format(Order order) {
        BUILDER.setLength(0);
        BUILDER.append(order.getId());
        BUILDER.append(':');
        BUILDER.append(order.getStatus());

        return BUILDER.toString();
    }
}
```

Multiple threads can modify the same builder.

Prefer a local instance:

```java
public static String format(Order order) {
    return new StringBuilder()
            .append(order.getId())
            .append(':')
            .append(order.getStatus())
            .toString();
}
```

For a simple expression, direct concatenation may be clearer:

```java
return order.getId() + ":" + order.getStatus();
```

---

## 21. Assuming Every Concatenation Requires a Builder

This is perfectly reasonable:

```java
String fullName = firstName + " " + lastName;
```

Using a builder manually would add noise:

```java
String fullName = new StringBuilder()
        .append(firstName)
        .append(' ')
        .append(lastName)
        .toString();
```

Use `StringBuilder` mainly for:

- Loops
- Many conditional appends
- Large dynamically constructed text
- Repeated modifications

---

# Trade-Offs

| `String`                                  | `StringBuilder`                                |
| ----------------------------------------- | ---------------------------------------------- |
| Immutable                                 | Mutable                                        |
| Safe to share                             | Requires thread confinement or synchronization |
| Literals may be pooled                    | Not pooled                                     |
| Stable hash code                          | Content can change                             |
| Best for normal text values               | Best for repeated construction                 |
| Simple concatenations are often optimized | Avoids repeated intermediate strings           |
| Suitable as a map key                     | Usually unsuitable as a mutable map key        |

---

# Interview Questions

## Question 1: Why is `String` immutable?

Immutability allows strings to be safely shared, pooled, used across threads, and used as stable keys in hash-based collections. It also prevents values from changing after validation.

---

## Question 2: What is the String Pool?

The String Pool stores canonical string instances so identical literals can reuse one object. Calling `intern()` returns the pooled representation.

---

## Question 3: Why does `new String("hello")` create another object?

The literal `"hello"` is eligible for pooling, while the `new` expression explicitly requests a distinct `String` instance with the same content.

---

## Question 4: Why should `equals()` be used instead of `==`?

`equals()` compares string contents. `==` checks whether two references point to the same object.

---

## Question 5: Why is repeated `+=` concatenation expensive?

Each iteration may copy all previously constructed content into a new immutable string, producing many temporary objects and potentially quadratic copying cost.

---

## Question 6: When should `StringBuilder` be used?

Use it for repeated, loop-based, or conditional string construction when one mutable builder can accumulate the result efficiently.

---

## Question 7: When should `StringBuffer` be used?

Use it only when the same mutable sequence genuinely requires synchronized method access or when a legacy API requires it. Avoid sharing mutable builders where possible.

---

## Question 8: Does every string operation allocate a new object?

No. String operations never mutate the original object, but an operation may return the same instance when its result is unchanged. Compile-time concatenation may also be folded into one constant.

---

# Short Interview Answer

> `String` is immutable, which makes it safe to share, pool, use across threads, and use as a stable `HashMap` key. Identical literals can share a canonical object in the String Pool, while `new String()` creates a separate instance. Simple concatenation is usually fine, but repeated `+=` operations in a loop may repeatedly copy growing content, so `StringBuilder` is preferred for iterative construction. `StringBuffer` synchronizes individual operations but is rarely needed when builders are properly confined to one thread.

---

## Related Topics

- [Data Types and Pass-by-Value](data-types.md)
- [String, StringBuilder, and StringBuffer](advanced-questions.md)
- [HashMap Internals](../02-collections/hashmap-internals.md)
- [Java Memory Management](../05-jvm/memory-management.md)
- [Thread Safety](../04-concurrency/thread-safety.md)
