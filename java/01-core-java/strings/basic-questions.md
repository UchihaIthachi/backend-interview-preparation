# Basic Questions

## Question 1: What is the purpose of the `toString()` method in Java?

The `toString()` method returns a string representation of an object. It is defined in the `Object` class, so every Java class inherits it.

The default implementation typically returns:

```text
fully.qualified.ClassName@hexadecimalHashCode
```

For example:

```text
Person@4e25154f
```

Classes commonly override `toString()` to return more meaningful information about their objects.

```java
public class Person {

    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
```

The object can then be printed directly:

```java
Person person = new Person("John Doe", 30);
System.out.println(person);
```

Output:

```text
Person{name='John Doe', age=30}
```

Java automatically calls `toString()` when an object is passed to methods such as `System.out.println()` or used in string concatenation.

---

## Question 2: What is a Java String?

A Java string is an object of the `java.lang.String` class that represents a sequence of characters.

```java
String message = "Hello, Java!";
```

Strings in Java are **immutable**. This means that the contents of a `String` object cannot be changed after the object has been created.

For example:

```java
String value = "Hello";
value = value + " World";
```

The original `"Hello"` object is not modified. Instead, Java creates a new string containing `"Hello World"` and assigns its reference to `value`.

Common string operations include:

```java
String first = "Hello";
String second = "World";

String combined = first + " " + second;
int length = combined.length();
int index = combined.indexOf('o');
String substring = combined.substring(0, 5);
boolean equal = first.equals(second);
```

---

## Question 3: How do you create and manipulate strings in Java?

### Creating strings

The recommended way to create a string is by using a string literal:

```java
String message = "Hello World";
```

A string can also be created using the `String` constructor:

```java
String message = new String("Hello World");
```

However, using `new String(...)` is usually unnecessary because it explicitly creates an additional object.

### Common String methods

Given the following string:

```java
String text = "Hello World";
```

#### `length()`

Returns the number of characters in the string.

```java
int length = text.length();
System.out.println(length); // 11
```

#### `charAt()`

Returns the character at a specified zero-based index.

```java
char firstCharacter = text.charAt(0);
System.out.println(firstCharacter); // H
```

#### `substring()`

Returns a portion of the string. The ending index is exclusive.

```java
String result = text.substring(0, 5);
System.out.println(result); // Hello
```

#### `indexOf()`

Returns the index of the first occurrence of a character or substring.

```java
int index = text.indexOf('o');
System.out.println(index); // 4
```

It returns `-1` when the value cannot be found.

#### `lastIndexOf()`

Returns the index of the last occurrence of a character or substring.

```java
int index = text.lastIndexOf('o');
System.out.println(index); // 7
```

#### `replace()`

Returns a new string in which matching characters or substrings are replaced.

```java
String result = text.replace('o', 'a');
System.out.println(result); // Hella Warld
```

#### `toUpperCase()`

Returns a new uppercase string.

```java
System.out.println(text.toUpperCase());
// HELLO WORLD
```

#### `toLowerCase()`

Returns a new lowercase string.

```java
System.out.println(text.toLowerCase());
// hello world
```

#### `equals()`

Compares the contents of two strings.

```java
String first = "Java";
String second = "Java";

System.out.println(first.equals(second)); // true
```

Use `equals()` rather than `==` when comparing string values. The `==` operator compares object references, not character content.

Because strings are immutable, these methods do not modify the original string. They return new strings when a changed value is required.

---

## Question 4: Why is `String` immutable?

`String` is immutable for several important reasons.

### 1. Security

Strings are frequently used to represent sensitive values such as:

- File paths
- Database URLs
- Network addresses
- Class names
- Usernames
- Configuration values

If strings were mutable, a value could be changed after it had already passed a security validation.

### 2. Thread safety

An immutable object cannot be changed after creation. Therefore, the same `String` object can safely be shared by multiple threads without synchronization.

### 3. String Pool safety

String literals may be shared through the String Pool.

```java
String first = "Java";
String second = "Java";
```

Both variables may refer to the same pooled object. Immutability prevents one reference from changing a value that is also being used by another reference.

### 4. Stable hash codes

Strings are frequently used as keys in hash-based collections such as `HashMap` and `HashSet`.

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
```

A key's hash code must remain stable while the key is stored in the collection. String immutability guarantees that its character content—and therefore its hash code—will not unexpectedly change.

### 5. Caching and performance

Because a string does not change, Java can reuse pooled strings and may cache a string's calculated hash code. This can reduce memory consumption and improve performance.

---

## Question 5: What is the String Pool?

The String Pool, also called the **String Intern Pool**, is a special area managed by the JVM for reusing string instances.

When a string literal is created, the JVM checks whether an equal string already exists in the pool.

```java
String first = "Hello";
String second = "Hello";
```

Because both values are identical literals, `first` and `second` normally reference the same pooled object.

```java
System.out.println(first == second); // true
```

When the `new` keyword is used, Java explicitly creates another `String` object:

```java
String first = "Hello";
String second = new String("Hello");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

The literal `"Hello"` is available in the String Pool, while `new String("Hello")` creates a separate object.

The `intern()` method returns the pooled representation of a string:

```java
String first = "Hello";
String second = new String("Hello").intern();

System.out.println(first == second); // true
```

The String Pool reduces memory consumption by allowing identical string values to share the same instance.
