# Advanced Questions

## Question 1: What is the difference between String, StringBuilder, and StringBuffer?

• String: Immutable. Slow for repeated modifications as each change creates a new object.
• StringBuilder: Mutable, non-thread-safe. Extremely fast (approx 95% faster than String for repeated edits) but has no synchronization overhead.
• StringBuffer: Mutable, thread-safe. All methods are synchronized, preventing race conditions in multi-threaded environments, but this comes with a performance cost (approx 65% faster than String, but slower than StringBuilder).

## Question 2: What is difference b/w String, StringBuilder, and StringBuffer in java ?

**String:**
- String is immutable, meaning that it cannot be changed after it is created. Any modification (like appending) creates a brand-new object in memory.
- String literals are stored in a String pool, which is a shared area of memory used to optimize storage.

**StringBuilder:**
- `StringBuilder` is a mutable class, meaning it modifies the same object directly in memory without creating new objects.
- It is **not** thread-safe. Multiple threads accessing it concurrently can cause data corruption or race conditions.
- Because it lacks synchronization overhead, it is the fastest option for string manipulation in single-threaded applications.

**StringBuffer:**
- `StringBuffer` is also a mutable class.
- It is **thread-safe**. Every method in `StringBuffer` is synchronized, meaning it can be safely used by multiple threads at the same time without race conditions.
- Because of the synchronization overhead, it is slower than `StringBuilder`.

## Question 3: Difference between StringBuffer and StringBuilder?

| Feature | `StringBuffer` | `StringBuilder` |
| :--- | :--- | :--- |
| **Thread Safety** | Thread-safe. | Not thread-safe. |
| **Synchronization** | All methods are synchronized. | Methods are not synchronized. |
| **Performance** | Slower due to synchronization overhead. | Faster because there is no waiting time for threads. |
| **Use Case** | Use when multiple threads need to read and write strings concurrently, or when a single string object is shared across threads. | Use in single-threaded applications or when performance is the priority. Default choice over StringBuffer. |
| **Introduced In** | Java 1.0 | Java 1.5 |

