# Basic Questions

## Question 1: What is the purpose of the toString() method in java ?

The `toString()` method in Java is a pre-existing method found in the Object class. It serves the purpose of returning a string representation of an object. By default, it produces a string comprising the object's class name, followed by an "@" symbol and hash code.
```java
public class Person {
    private String name;
    private int age;
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
In this example, the `toString()` method is overridden to return a string that contains the person's name and age. This string can then be printed to the console using the `System.out.println()` method:
```java
Person person = new Person("John Doe", 30);
System.out.println(person);
```
This will print the following output to the console:
`Person{name='John Doe', age=30}`

## Question 2: What is java string ?

A Java string is a sequence of characters that exists as an object of the class `java.lang.String`. Java strings are created and manipulated through the String class. Once created, a string is immutable -- its value cannot be changed.
```java
String str1 = "Hello";
String str2 = "World";
// Concatenate two strings
String str3 = str1 + str2;
// Get the length of a string
int length = str3.length();
// Find the index of a character in a string
int index = str3.indexOf('o');
// Substring a string
String substring = str3.substring(0, 5);
// Compare two strings
boolean isEqual = str1.equals(str2);
```

## Question 3: How do you create and manipulate strings in java ?

Strings in Java are immutable, which means that they cannot be changed once they are created. To create a string, you can use the `new` keyword and the String constructor. For example, the following code creates a string object named `str` and assigns it the value "Hello World":
```java
String str = new String("Hello World");
```
You can also create strings using string literals. String literals are sequences of characters enclosed in double quotes. For example, the following code creates a string object named `str` and assigns it the value "Hello World":
```java
String str = "Hello World";
```
Once you have created a string object, you can manipulate it using the methods provided by the String class. Some of the most common string manipulation methods include:
- `length()` : Returns the length of the string.
- `charAt()` : Returns the character at a specified index in the string.
- `substring()` : Returns a new string that is a substring of the original string.
- `indexOf()` : Returns the index of the first occurrence of a specified character or substring in the string.
- `lastIndexOf()` : Returns the index of the last occurrence of a specified character or substring in the string.
- `replace()` : Returns a new string that is a copy of the original string with all occurrences of a specified character or substring replaced with another character or substring.
- `toUpperCase()`: Returns a new string that is a copy of the original string with all characters converted to uppercase.
- `toLowerCase()`: Returns a new string that is a copy of the original string with all characters converted to lowercase.

## Question 4: Why is `String` immutable?

Strings in Java are immutable for several key reasons:
1.  **Security:** Strings are widely used as parameters for network connections, database URLs, file paths, etc. If String were mutable, a reference to a string could be changed after security checks have passed, leading to severe vulnerabilities.
2.  **Thread Safety:** Because a String cannot be changed once created, it is inherently thread-safe. Multiple threads can safely share a single String object without the need for synchronization.
3.  **Caching (String Pool):** The JVM optimizes memory by caching String literals in the String Pool. If strings were mutable, changing the value of one shared string literal would inadvertently affect all other references pointing to the same memory location.
4.  **Performance (Hashcode):** The `hashCode()` of a String is frequently used (e.g., as keys in a `HashMap`). Because the string is immutable, its hash code can be calculated once and cached for future use, improving performance.

## Question 5: What is the String Pool?

The String Pool (or String Intern Pool) is a special storage area in the Java Heap memory. 
When a String literal is created (e.g., `String s = "Hello";`), the JVM checks the String Pool. 
- If the string "Hello" already exists in the pool, a reference to the pooled instance is returned.
- If it does not exist, a new String object is created and placed in the pool.

This caching mechanism significantly saves memory space because it guarantees that there is only one instance of any given string literal in the heap. Note that strings created using the `new` keyword (e.g., `new String("Hello")`) are always created in the main heap memory outside of the String Pool (unless `.intern()` is explicitly called).

## Question 6: What is Strings?

String is a collection/set of characters.
String is an immutable object.
**Case 1:**
Once we create a String object, we can't perform any changes on the original object itself. If we perform any changes, a new object will be created for that changed value. Such behavior is called immutability.
