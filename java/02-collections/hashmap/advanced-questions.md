# Advanced Questions — `==` vs `equals()` in Java

## Question 1: What is the difference between `==` and `equals()`?

The `==` operator and the `equals()` method both compare values, but they serve different purposes.

| Feature              | `==`                         | `equals()`                                         |
| -------------------- | ---------------------------- | -------------------------------------------------- |
| Type                 | Operator                     | Method                                             |
| Primitive comparison | Compares primitive values    | Not applicable directly                            |
| Object comparison    | Compares reference identity  | Usually compares logical content when overridden   |
| Defined by           | Java language                | `java.lang.Object`                                 |
| Can be overridden    | No                           | Yes                                                |
| Null-safe            | Yes for reference comparison | Calling it on `null` throws `NullPointerException` |

---

## 1. Using `==` with primitive values

For primitive types, `==` compares the actual values.

```java
int first = 10;
int second = 10;

System.out.println(first == second); // true
```

```java
char first = 'A';
char second = 'A';

System.out.println(first == second); // true
```

Numeric promotion may occur before comparison:

```java
int number = 10;
long value = 10L;

System.out.println(number == value); // true
```

---

## 2. Using `==` with objects

For object references, `==` checks whether both references point to the **same object**.

```java
Person first = new Person("Alice");
Person second = new Person("Alice");

System.out.println(first == second); // false
```

Although both objects contain the same data, they are separate objects.

```java
Person first = new Person("Alice");
Person second = first;

System.out.println(first == second); // true
```

Both references point to the same object.

### Important correction

It is common to say that `==` compares memory addresses, but Java does not expose object memory addresses as part of the language contract.

A more accurate statement is:

> For reference types, `==` compares reference identity—whether two references refer to the same object.

---

## 3. What does `equals()` do?

The `equals()` method is declared in `java.lang.Object`, not only in the `String` class.

```java
public boolean equals(Object object)
```

Every Java object inherits it.

The default implementation in `Object` behaves like identity comparison:

```java
public boolean equals(Object object) {
    return this == object;
}
```

Therefore, unless a class overrides `equals()`, it does not automatically compare field values.

---

## 4. `String` comparison

`String` overrides `equals()` to compare character content.

```java
String first = new String("Java");
String second = new String("Java");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

- `==` checks whether the references identify the same object.
- `equals()` checks whether the strings contain the same sequence of characters.

### Case sensitivity

`String.equals()` is case-sensitive:

```java
System.out.println(
        "Java".equals("JAVA")
); // false
```

For case-insensitive comparison:

```java
System.out.println(
        "Java".equalsIgnoreCase("JAVA")
); // true
```

Case sensitivity is not a universal property of every `equals()` implementation. It depends on how the class defines logical equality.

---

## 5. String Pool trap

String literals may share the same pooled object:

```java
String first = "Java";
String second = "Java";

System.out.println(first == second); // true
```

This can make `==` appear to work for strings.

However:

```java
String first = "Java";
String second = new String("Java");

System.out.println(first == second); // false
```

Therefore, always use `equals()` for string-content comparison.

```java
if ("Java".equals(language)) {
    // Content comparison
}
```

---

## 6. Custom `equals()` implementation

Consider this class:

```java
import java.util.Objects;

public final class Employee {

    private final long id;
    private final String name;

    public Employee(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Employee other)) {
            return false;
        }

        return id == other.id
                && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
```

Usage:

```java
Employee first =
        new Employee(101L, "Alice");

Employee second =
        new Employee(101L, "Alice");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

The objects are different instances but logically equal.

---

## 7. Null-safe comparison

This can throw `NullPointerException`:

```java
String value = null;

System.out.println(value.equals("Java"));
```

### Constant-first comparison

```java
"Java".equals(value);
```

This safely returns `false`.

### Using `Objects.equals()`

```java
Objects.equals(first, second);
```

Its behavior is conceptually:

```java
first == second
        || first != null
        && first.equals(second)
```

Examples:

```java
Objects.equals(null, null);       // true
Objects.equals(null, "Java");     // false
Objects.equals("Java", "Java");   // true
```

---

## 8. Wrapper-class comparison

Wrapper objects can produce surprising results because some values are cached.

```java
Integer first = 100;
Integer second = 100;

System.out.println(first == second); // often true
```

```java
Integer first = 1000;
Integer second = 1000;

System.out.println(first == second); // often false
```

Do not rely on wrapper caching.

Use:

```java
System.out.println(
        first.equals(second)
);
```

Or unbox deliberately:

```java
System.out.println(
        first.intValue() == second.intValue()
);
```

Be careful because unboxing `null` throws `NullPointerException`.

---

## 9. Enum comparison

Using `==` for enum constants is correct and recommended.

```java
OrderStatus status =
        OrderStatus.COMPLETED;

if (status == OrderStatus.COMPLETED) {
    System.out.println("Completed");
}
```

Each enum constant is a single canonical instance.

`==` is also null-safe:

```java
if (status == OrderStatus.COMPLETED) {
}
```

If `status` is `null`, the expression simply evaluates to `false`.

---

## 10. Arrays and `equals()`

Arrays do not override `Object.equals()` for element-by-element comparison.

```java
int[] first = {1, 2, 3};
int[] second = {1, 2, 3};

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // false
```

Use `Arrays.equals()`:

```java
System.out.println(
        Arrays.equals(first, second)
); // true
```

For nested arrays:

```java
Object[] first = {
        new int[]{1, 2}
};

Object[] second = {
        new int[]{1, 2}
};

System.out.println(
        Arrays.deepEquals(first, second)
); // true
```

---

## 11. Collections and `equals()`

Most collection implementations define logical equality based on their contents.

### Lists

Order matters:

```java
List<Integer> first =
        List.of(1, 2, 3);

List<Integer> second =
        List.of(1, 2, 3);

System.out.println(
        first.equals(second)
); // true
```

```java
List<Integer> third =
        List.of(3, 2, 1);

System.out.println(
        first.equals(third)
); // false
```

### Sets

Order does not matter:

```java
Set<Integer> first =
        Set.of(1, 2, 3);

Set<Integer> second =
        Set.of(3, 2, 1);

System.out.println(
        first.equals(second)
); // true
```

### Maps

Maps are equal when they contain equal key-value mappings.

---

## 12. Records and equality

Records automatically generate content-based `equals()` and `hashCode()` methods using their components.

```java
public record UserId(long value) {
}
```

```java
UserId first = new UserId(10L);
UserId second = new UserId(10L);

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

---

# `equals()` Contract

A correct `equals()` implementation must be:

## Reflexive

```java
x.equals(x) == true
```

## Symmetric

```java
x.equals(y) == y.equals(x)
```

## Transitive

If:

```java
x.equals(y)
```

and:

```java
y.equals(z)
```

then:

```java
x.equals(z)
```

must be true.

## Consistent

Repeated calls should produce the same result while equality-related state remains unchanged.

## Non-null

```java
x.equals(null) == false
```

---

# `equals()` and `hashCode()`

Whenever `equals()` is overridden, `hashCode()` should normally also be overridden.

The required contract is:

```text
If two objects are equal,
they must have the same hash code.
```

This is required for:

- `HashMap`
- `HashSet`
- `Hashtable`
- `ConcurrentHashMap`

Incorrect:

```java
@Override
public boolean equals(Object object) {
    // Logical equality
}

// hashCode() not overridden
```

This can cause logically equal objects to be stored in different hash buckets.

---

# Comparison Summary

| Example                                         |     `==` result |                     `equals()` result |
| ----------------------------------------------- | --------------: | ------------------------------------: |
| Same primitive value                            |          `true` |                        Not applicable |
| Same object reference                           |          `true` |                        Usually `true` |
| Different objects with same fields, no override |         `false` |                               `false` |
| Different strings with same content             | Usually `false` |                                `true` |
| Same pooled string literal                      |    Often `true` |                                `true` |
| Equal record instances                          |         `false` |                                `true` |
| Equal array contents                            |         `false` | `false` with array’s inherited method |
| Equal list contents                             |         `false` |                                `true` |

---

# Common Mistakes

## 1. Saying `equals()` belongs only to `String`

Incorrect:

> `equals()` is a method in the `String` class.

Correct:

> `equals()` is declared in `Object`. `String` overrides it to compare character content.

---

## 2. Comparing strings with `==`

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

---

## 3. Assuming all `equals()` methods compare content

Classes that do not override `equals()` retain identity-based behavior inherited from `Object`.

---

## 4. Calling `equals()` on a nullable reference

Risky:

```java
input.equals(expected);
```

Safer:

```java
Objects.equals(input, expected);
```

---

## 5. Comparing wrapper objects using `==`

Avoid:

```java
if (firstInteger == secondInteger) {
}
```

Prefer:

```java
if (Objects.equals(
        firstInteger,
        secondInteger
)) {
}
```

---

## 6. Using array `equals()` for content

Incorrect:

```java
firstArray.equals(secondArray);
```

Correct:

```java
Arrays.equals(
        firstArray,
        secondArray
);
```

---

# Interview Questions

## Question 1: Does `equals()` always compare object content?

No. The default implementation inherited from `Object` compares identity. Content comparison happens only when a class overrides `equals()` accordingly.

## Question 2: Can `==` be used for strings?

It can compile, but it compares references rather than content. Use `equals()` for string values.

## Question 3: Why can two strings with equal content produce `true` for `==`?

String literals may refer to the same pooled object. This is an implementation optimization and should not be used for content comparison.

## Question 4: When is `==` appropriate for objects?

Use it when object identity matters, such as checking whether two references point to the exact same object. It is also recommended for enum constants.

## Question 5: Is `equals()` case-sensitive?

`String.equals()` is case-sensitive. Other classes define equality according to their own contracts.

## Question 6: Why should `hashCode()` be overridden with `equals()`?

Hash-based collections use `hashCode()` to locate a bucket and `equals()` to find the exact object. Equal objects must therefore produce the same hash code.

---

# Short Interview Answer

> `==` compares primitive values or reference identity. For objects, it checks whether two references point to the same instance. `equals()` is declared in `Object` and can be overridden to define logical equality. `String`, records, and most collections provide content-based equality, while classes that do not override it retain identity comparison. For string values, use `equals()`; for enums or intentional identity checks, use `==`.
