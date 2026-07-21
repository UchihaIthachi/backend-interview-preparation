# Java Strings, the String Pool and StringBuilder

## 1. Definition

`String` in Java is immutable — once created, its internal character data never
changes. Any operation that appears to "modify" a string actually returns a new one.

## 2. Why it exists

Immutability gives three benefits:
- **Safety**: strings can be freely shared across threads with no synchronization.
- **Security**: class loaders, file paths, and network hosts are often passed as
  strings; immutability prevents them from being altered after validation.
- **Caching**: the JVM can intern identical literals in a String Pool, saving memory,
  because it's guaranteed they'll never change underneath a shared reference.

## 3. How it works

String literals are stored in a **String Pool** inside the heap. Writing
`"hello"` twice reuses the same pooled object. Using `new String("hello")` forces a
new object outside the pool.

```java
String a = "hello";
String b = "hello";
String c = new String("hello");

a == b;          // true  (same pooled reference)
a == c;          // false (different object)
a.equals(c);     // true  (same content)
```

Every `+` concatenation, `.substring()`, `.replace()`, etc. allocates a **new**
`String` object — the original is untouched.

## 4. Code example

```java
String result = "";
for (int i = 0; i < 10_000; i++) {
    result += i; // creates 10,000 intermediate String objects — O(n^2) total cost
}
```

Prefer `StringBuilder` for repeated concatenation:

```java
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10_000; i++) {
    sb.append(i); // mutates an internal char array — O(n) total cost
}
String result = sb.toString();
```

## 5. Production use case

Building large log lines, SQL query strings, or JSON payloads in a loop should
always use `StringBuilder` (or a proper serializer). Naive `+=` concatenation
inside a loop is one of the most common performance bugs found in code review.

## 6. Common mistakes

- Concatenating strings with `+` inside loops.
- Comparing strings with `==` instead of `.equals()`.
- Assuming `new String("x") == "x"` is true.
- Using `StringBuffer` by default — it's synchronized and slower; prefer
  `StringBuilder` unless multiple threads truly share the same builder instance.

## 7. Trade-offs

| String | StringBuilder |
|---|---|
| Immutable, thread-safe, poolable | Mutable, faster for repeated edits |
| Bad for loops of concatenation | Not thread-safe by default |

## 8. Interview questions

1. Why is `String` immutable?
2. What is the String Pool and when does a string get added to it?
3. `StringBuilder` vs `StringBuffer` — when would you use each?
4. Why is repeated `+=` concatenation in a loop a performance problem?

## 9. Short interview answer

"Strings are immutable so the JVM can safely pool and share literals across the
whole application without synchronization, and so strings used for security-
sensitive values like file paths can't be altered after being validated. Because
every modification creates a new object, repeated concatenation should use
StringBuilder instead of `+=`."

## 10. Related topics

- [Data types & pass-by-value](data-types.md)
- [HashMap internals](../02-collections/hashmap-internals.md) (string hashing)
