# Advanced Questions

## Question 1: What is the difference between String, StringBuilder, and StringBuffer?

### String

- `String` is **immutable**, meaning its value cannot be changed after creation.
- Operations such as concatenation, replacement, or appending create a new `String` object.
- String literals can be stored in the **String Pool**, allowing identical literals to share the same object.
- `String` is suitable for values that do not require frequent modification.
- Its immutability makes it inherently safe to share between threads.

### StringBuilder

- `StringBuilder` is **mutable**, so its contents can be modified without creating a new object for every operation.
- Its methods are **not synchronized**, so it is not thread-safe when the same instance is modified by multiple threads.
- It is generally faster than `StringBuffer` because it has no synchronization overhead.
- It is the preferred option for repeated string modifications in single-threaded code.

### StringBuffer

- `StringBuffer` is also **mutable**.
- Its methods are synchronized, making it thread-safe for individual operations.
- Synchronization introduces additional overhead, so it is generally slower than `StringBuilder`.
- It should be used when the same mutable character sequence must be shared and modified across multiple threads.

### Summary

| Feature                | `String`                     | `StringBuilder`                        | `StringBuffer`                          |
| :--------------------- | :--------------------------- | :------------------------------------- | :-------------------------------------- |
| Mutability             | Immutable                    | Mutable                                | Mutable                                 |
| Thread-safe            | Yes, because it is immutable | No                                     | Yes, through synchronized methods       |
| Synchronization        | Not applicable               | No                                     | Yes                                     |
| Repeated modifications | Inefficient                  | Most efficient in single-threaded code | Useful when synchronization is required |
| Typical use case       | Fixed or rarely changed text | String manipulation within one thread  | Shared mutable text across threads      |

---

## Question 2: What is the difference between StringBuffer and StringBuilder?

| Feature             | `StringBuffer`                                         | `StringBuilder`                                     |
| :------------------ | :----------------------------------------------------- | :-------------------------------------------------- |
| **Thread safety**   | Thread-safe for individual method calls                | Not thread-safe                                     |
| **Synchronization** | Methods are synchronized                               | Methods are not synchronized                        |
| **Performance**     | Generally slower because of synchronization overhead   | Generally faster                                    |
| **Recommended use** | When the same instance is modified by multiple threads | For most single-threaded string-building operations |
| **Introduced in**   | Java 1.0                                               | Java 1.5                                            |

### Example

```java
StringBuilder builder = new StringBuilder();
builder.append("Hello");
builder.append(" World");

String result = builder.toString();
System.out.println(result);
```

`StringBuilder` is normally the default choice for constructing or repeatedly modifying strings. Use `StringBuffer` only when synchronized access to the same mutable instance is genuinely required.

> Note: `StringBuffer` synchronizes individual method calls, but a sequence of multiple operations may still require external synchronization when the entire sequence must be atomic.
