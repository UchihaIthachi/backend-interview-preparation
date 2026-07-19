# Strings Questions

### What is the difference between String, StringBuilder, and StringBuffer?

• String: Immutable. 
• StringBuilder: Mutable, non-thread-safe. 
• StringBuffer: Mutable, thread-safe.

### What is the purpose of the toString() method in java ?

The toString() method in Java is a pre-existing method found in the Object class. It serves the purpose of returning a string representation of an object. By default, it produces a string comprising the object's class name, followed by an "@" symbol and hash code.
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
In this example, the toString() method is overridden to return a string that contains the person's name and age. This string can then be printed to the console using the System.out.println() method:
Person person = new Person("John Doe", 30);
System.out.println(person);
This will print the following output to the console:
Person{name='John Doe', age=30}

### What is java string ?

A Java string is a sequence of characters that exists as an object of the class java. lang. Java strings are created and manipulated through the string class. Once created, a string is immutable -- its value cannot be changed.
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

### How do you create and manipulate strings in java ?

Strings in Java are immutable, which means that they cannot be changed once they are created. To create a string, you can use the new keyword and the String constructor. For example, the following code creates a string object named str and assigns it the value "Hello World":
String str = new String("Hello World");
You can also create strings using string literals. String literals are sequences of characters enclosed in double quotes. For example, the following code creates a string object named str and assigns it the value "Hello World":
String str = "Hello World";
Once you have created a string object, you can manipulate it using the methods provided by the String class. Some of the most common string manipulation methods include:
length() : Returns the length of the string.
charAt() : Returns the character at a specified index in the string.
substring() : Returns a new string that is a substring of the original string.
indexOf() : Returns the index of the first occurrence of a specified character or substring in the string.
lastIndexOf() : Returns the index of the last occurrence of a specified character or substring in the string.
replace() : Returns a new string that is a copy of the original string with all occurrences of a specified character or substring replaced with another character or substring.
toUpperCase(): Returns a new string that is a copy of the original string with all characters converted to uppercase.
toLowerCase(): Returns a new string that is a copy of the original string with all characters converted to lowercase.
For example, the following code uses the length() method to print the length of the string str:
System.out.println(str.length());
The following code uses the charAt() method to print the character at index 0 in the string str:
System.out.println(str.charAt(0));
The following code uses the substring() method to print the substring of the string str that starts at index 0 and ends at index 4:
System.out.println(str.substring(0, 4));
The following code uses the indexOf() method to print the index of the first occurrence of the character 'o' in the string str:
System.out.println(str.indexOf('o'));
The following code uses the lastIndexOf() method to print the index of the last occurrence of the character 'o' in the string str:
System.out.println(str.lastIndexOf('o'));
The following code uses the replace() method to print a new string that is a copy of the string str with all occurrences of the character 'o' replaced with the character 'a':
System.out.println(str.replace('o', 'a'));
The following code uses the toUpperCase() method to print a new string that is a copy of the string str with all characters converted to uppercase:
System.out.println(str.toUpperCase());
The following code uses the toLowerCase() method to print a new string that is a copy of the string str with all characters converted to lowercase:
System.out.println(str.toLowerCase());
These are just a few of the many string manipulation methods that are available in Java. For more information, please see the Java documentation for the String class.

### What is difference b/w String, StringBuilder, and StringBuffer in java ?

String :
➢
String is immutable, meaning that it cannot be changed after it is created. This is because String objects are stored in a String pool, which is a shared area of memory that is used to store all String objects in a Java program.
StringBuilder :
➢
StringBuilder is a mutable class, meaning that it can be changed after it is created. This is because StringBuilder objects are not stored in the String pool.
➢
StringBuilder is also thread-safe, meaning that it can be safely used by multiple threads at the same time. This is because StringBuilder methods are synchronized.
StringBuffer :
➢
StringBuffer is also a mutable class, meaning that it can be changed after it is created. This is because StringBuffer objects are not stored in the String pool.
➢
StringBuffer is also thread-safe, meaning that it can be safely used by multiple threads at the same time. This is because StringBuffer methods are synchronized.

### Why is `String` immutable?

No answer provided yet.

### What is the String Pool?

No answer provided yet.

### What is Strings?

String is a collection/set of characters.
String is a immutable object.
case1:
Once if we create a String object we can't perform any changes.If we perform any changes then for every change a new object will be created such behaviour is called immutability of an object.

### Difference between StringBuffer and StringBuilder?

StringBuffer StringBuilder
All the methods present in All the methods present in StringBuilder are
StringBuffe are synchronized. not synchronized.
At a time only one thread Multiple threads are allowed to access
is allowed to access Hence we can StringBuilder object.
StringBuffer object. achieve thread safety.
Hence we can't achieve
achieve thread safety.
Waiting time of a thread will There is no waiting thread effectively
increase effectively performance is high.
performance is low.
StringBuffer introduced in 1.0v. StringBuilder introduced in 1.5v.

