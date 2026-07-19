# Core Java General Questions

### What is Java? Explain its features.

Java is a high-level, object-oriented programming language developed by Sun Microsystems 
(now Oracle) in 1995. 
Key features: 
• Platform Independent: Write Once, Run Anywhere (WORA). 
• Object-Oriented: Follows OOP principles like encapsulation and inheritance. 
• Robust: Strong memory management and exception handling. 
• Multithreaded: Supports concurrent execution of threads. 
• Secure: No explicit pointers and runs in a virtual machine.

### Explain the concept of platform independence in Java.

Java programs are compiled into bytecode, which is platform-independent. Bytecode is 
executed by the JVM, which is platform-specific, ensuring the same Java program runs on 
any OS with a compatible JVM.

### What is the significance of the main method in Java?

The main method is the entry point of a Java application. Its signature is: 
java 

public static void main(String[] args) 
• public: Accessible globally. 
• static: Allows the JVM to call it without object instantiation. 
• void: Returns no value. 
• String[] args: Accepts command-line arguments.

### Describe the access modifiers in Java.

• Public: Accessible everywhere. 
• Protected: Accessible within the same package and subclasses. 
• Default: Accessible within the same package only. 
• Private: Accessible within the same class only.

### Explain the concept of packages in Java.

Packages are namespaces used to group related classes and interfaces. They help avoid name 
conflicts and improve organization.

### Explain the concept of Java annotations.

Annotations provide metadata about code, such as @Override, @Deprecated, and 
custom annotations.

### Explain serialization and deserialization in Java.

• Serialization: Converts an object to a byte stream. 
• Deserialization: Converts a byte stream back to an object.

### Explain the concept of immutability in Java.

Immutable objects cannot be modified after creation, e.g., String.

### What is the enum type in Java? How is it used?

Used to define a set of named constants. 
Example: 
java 

enum Day { MONDAY, TUESDAY }

### Explain the concept of reflection in Java.

Allows inspection and modification of classes, methods, and fields at runtime.

### What are modules in Java? Discuss their significance.

Introduced in Java 9, modules allow better packaging, encapsulation, and dependency 
management.

### What is java ?

➢
Java is a high-level, class-based, object-oriented programming language that is designed to have as fewimplementation dependencies as possible.
➢
It is a general-purpose programming language intended to let programmers write once, run anywhere,meaning that compiled Java code can run on all platforms that support Java without the need to recompile.
➢
it is used to develop a wide variety of applications, including:
➢
Web applications, Mobile applications, Desktop applications, Enterprise software, Scientific applications, andEmbedded systems.

### What are the main features of java ?

1.
Simple and Easy to Learn. Java is easy to learn and simple to use as a programming language.
2.
Object-Oriented Programming.
3.
Platform Independence.
4.
Automatic Memory Management.
5.
Security.
6.
Rich API.
7.
Multithreading.
8.
High Performance
9.
Scalability

### What is differ b/w java and javascript ?

➢
Java is an OOP programming language while Java Script is an OOP scripting language.
➢
Java creates applications that run in a virtual machine or browser while JavaScript code is run on a browseronly.
➢
Java code needs to be compiled while JavaScript code are all in text.

### What are the purpose of access modifiers in java ?

➢
Access modifiers in Java are used to control the access level of classes, methods, variables, and constructors.
➢
They are used to restrict access to certain parts of a program, which can help to improve security and makethe code more maintainable.

### Differ b/w public, private, protected & default in java ?

There are four access modifiers in Java:
Public:
Public members are accessible from anywhere in the program.
Private:
Private members are only accessible from within the class in which they are declared.
Protected:
Protected members are accessible from within the class in which they are declared, as well as from any subclasses of that class.
Default:
Default members are accessible from within the package in which they are declared.

### What is static method in java ?

A static method in Java is a method that belongs to a class rather than an instance of a class. Static methods are used to access and change static variables and other non-object-based static methods.
Here are some features of static methods:
•
Static methods are called using the class name, not the instance name.
•
Static methods can only access static variables and other static methods.
•
Static methods cannot access non-static variables or non-static methods.
•
Static methods are typically used for utility functions, such as mathematical functions or string manipulationfunctions.

### What is method overriding in java ?

➢
In Java, method overriding occurs when a subclass (child class) has the same method as the parent class. Inother words, method overriding occurs when a subclass provides a particular implementation of a methoddeclared by one of its parent classes.
➢
To override a method, the subclass must have the same method name, return type, and parameter list as themethod in the parent class. The overriding method can also return a subtype of the type returned by theoverridden method.
Here is an example of method overriding:
class Animal {
public void move() {
System.out.println("Animals can move");
}
}
class Dog extends Animal {
@Override
public void move() {
System.out.println("Dogs can walk and run");
}
}
public class TestDog {
public static void main(String[] args) {
Animal a = new Animal();
Animal b = new Dog();
a.move(); // prints "Animals can move"
b.move(); // prints "Dogs can walk and run"
}
}
In this example, the Dog class overrides the move() method from the Animal class. The Dog class's move() method prints a different message than the Animal class's move() method.
When you call the move() method on a Dog object, the Dog class's move() method is called, even though the Dog object is an instance of the Animal class. This is because the Dog class overrides the move() method.
Method overriding is a powerful feature that allows you to customize the behavior of classes in Java. It is one of the key features that makes Java an object-oriented programming language.

### What is a package in java ?

➢
A java package is a group of similar types of classes, interfaces and sub-packages.
➢
Package in java can be categorized in two form, built-in package and user-defined package.
There are many built-in packages such as java, lang, awt, javax, swing, net, io, util, sql etc.

### How do you import packages in java ?

Packages are used to avoid naming conflicts, and to control access to classes. For example, the java.lang package contains classes such as String, Object, and Math, which are fundamental to the Java language.
Packages are declared using the package keyword.
Package mypackage;
the following code imports the String class from the java.lang package:
import java.lang.String;
the following code declares a nested package named mypackage.subpackage:
package mypackage.subpackage;

### What is the purpose of the instance of the operator in java ?

The instanceof operator in Java is used to check whether an object is an instance of a particular class or not. objectName instanceOf className; Here, if objectName is an instance of className , the operator returns true . Otherwise, it returns false .
For Eg :
if (myObject instanceof Dog) {
// myObject is a Dog object
} else if (myObject instanceof Animal) {
// myObject is an Animal object, but not a Dog object
} else {
// myObject is not an Animal object
}

### What is the purpose of the clone() method in java ?

clone() is a method in the Java programming language for object duplication. In Java, objects are manipulated through reference variables, and there is no operator for copying an object—the assignment operator duplicates the reference, not the object. The clone() method provides this missing functionality.
public class MyClass {
private int myField;
public MyClass(int myField) {
this.myField = myField;
}
public MyClass clone() {
MyClass newMyClass = new MyClass(myField);
return newMyClass;
}
}
public class Main {
public static void main(String[] args) {
MyClass myClass = new MyClass(10);
MyClass clonedMyClass = myClass.clone();
System.out.println(myClass.myField); // 10
System.out.println(clonedMyClass.myField); // 10
}
}

### What is the purpose of the equals() methods in java ?

equals() method is primarily used to compare the 'value' of two objects. It's an instance method that's part of the Object class, which is the parent class of all classes in Java. This means you can use . equals() to compare any two objects in Java.
For Eg:
String str1 = "Hello, World!";
String str2 = "Hello, World!";
// Compare the two strings for equality
boolean areEqual = str1.equals(str2);
// Print the result
System.out.println(areEqual);

### What is the purpose of the hashcode() method in java ?

The hashCode() method in Java is a built-in function used to return an integer hash code representing the value of the object, used with the syntax, int hash = targetString. hashCode(); . It plays a crucial role in data retrieval, especially when dealing with Java collections like HashMap and HashSet.
SOURCE CODE :
String str = "Hello, world!";
int hashCode = str.hashCode();
System.out.println(hashCode);
OUTPUT
"Hello, world!"

### What is the purpose of the compareTo() method in java ?

The compareTo() method returns an integer value that represents the comparison result. If the result is less than 0, str1 comes before str2 in lexicographical order. If the result is greater than 0, str1 comes after str2. If the result is 0, it means that both strings are equal.
SOURCE CODE:
String str1 = "Hello";
String str2 = "World";
int result = str1.compareTo(str2);
if (result > 0) {
System.out.println("str1 is greater than str2");
} else if (result < 0) {
System.out.println("str1 is less than str2");
} else {
System.out.println("str1 is equal to str2");
}
OUTPUT :
"str1 is less than str2"

### What is the purpose of the equals() method in java ?

equals() method is primarily used to compare the 'value' of two objects. It's an instance method that's part of the Object class, which is the parent class of all classes in Java. This means you can use . equals() to compare any two objects in Java.
Source code :
String str1 = "Hello, World!";
String str2 = "Hello, World!";
// Compare the two strings for equality.
boolean isEqual = str1.equals(str2);
// Print the result.
System.out.println(isEqual);
OUTPUT:
True //Because two strings are equal

### What is JDBC? How is it used in Java applications?

JDBC (Java Database Connectivity) is an API for connecting to databases. 
Steps: 
1. Load driver. 
2. Establish connection. 
3. Execute SQL queries. 
4. Close connection.

### Is Java pass-by-value or pass-by-reference?

No answer provided yet.

### What is type erasure?

No answer provided yet.

### What is garbage collection in Java? How does it work?

Garbage collection automatically deallocates memory for objects no longer in use, reclaiming 
memory in the heap.

### What is java HashMap ?

HashMap in Java stores the data in (Key, Value) pairs, and you can access them by an index of another type (e.g. an Integer). One object is used as a key (index) to another object (value). If you try to insert the duplicate key in HashMap, it will replace the element of the corresponding key.
Here are some of the key features of HashMaps:
❖
HashMaps store elements in key-value pairs.
❖
HashMaps are unsorted, which means that the order in which elements are added to the map is not preserved.
❖
HashMaps allow for one null key and multiple null values.
❖
HashMaps provide efficient access and manipulation of data based on unique keys.
❖
HashMaps are widely used in Java applications.
Here is an example of how to use a HashMap in Java:
import java.util.HashMap;
public class HashMapExample {
public static void main(String[] args) {
HashMap<String, Integer> hashMap = new HashMap<>();
// Add elements to the HashMap
hashMap.put("one", 1);
hashMap.put("two", 2);
hashMap.put("three", 3);
// Get the value for a key
Integer value = hashMap.get("two");
// Remove an element from the HashMap
hashMap.remove("three");
// Check if the HashMap contains a key
boolean containsKey = hashMap.containsKey("one");
// Print the contents of the HashMap
System.out.println(hashMap);
}
}
Output:
{one=1, two=2}

### How do you create and manipulate Hashmaps in java ?

Here's how to create and manipulate a HashMap in Java:
Create a HashMap instance using the syntax HashMap<KeyType, ValueType>. KeyType specifies the type of keys, and ValueType specifies the type of map will hold. For example, to create a HashMap called numberMapping that stores key-value pairs of strings and integers, you can use the following code:
Map<String, Integer> numberMapping = new HashMap<>();
Add key-value pairs to the HashMap using the put() function. For example, to add the key-value pairs "One" to 1, "Two" to 2, and "Three" to 3, you can use the following code:
numberMapping.put("One", 1); numberMapping.put("Two", 2); numberMapping.put("Three", 3);
Access elements in the HashMap using the get() function. For example, to print the value associated with the key "John", you can use the following code:
System.out.println(hashMap.get("John"));
Remove an element from the HashMap using the remove() function. For example, to remove the element associated with the key "Jim", you can use the following code:
hashMap.remove("Jim");
Check if an element is present in the HashMap using the containsKey() function. For example, to check if the element associated with the key "Jim" is present, you can use the following code:
System.out.println(hashMap.containsKey("Jim"));

### What is the purpose of the put() and get() methods in java HashMap ?

➢
The put()and get() methods are two of the most important methods in the Java HashMap class.
➢
The put() method is used to add a new key-value pair to the map,
➢
the get() method is used to retrieve the value associated with a given key.
➢
The put() method returns the previous value associated with the key, or null if there was no previous value.
➢
The get() method returns the value associated with the key, or null if there is no value associated with the key.
Here is an example of how to use the put() and get() methods:
HashMap<String, Integer> map = new HashMap<>();
// Add a new key-value pair to the map
map.put("name", "John Doe");
// Get the value associated with the key "name"
String name = map.get("name");
// Print the value
System.out.println(name);
OUTPUT:
John Doe

### What is the purpose of the remove() methods in java HashMap ?

The remove() method removes the mapping and returns: the previous value associated with the specified key. true if the mapping is removed.
The remove() method can be used to delete an item from a HashMap. This can be useful for a variety of reasons, such as:
❖
To remove an item that is no longer needed.
❖
To remove an item that is causing problems.
❖
To remove an item that is outdated.
Here is an example of how to use the remove() method:
SOURCE CODE:
HashMap<String, Integer> hashMap = new HashMap<>();
hashMap.put("one", 1);
hashMap.put("two", 2);
hashMap.put("three", 3);
// Remove the item with the key "two"
Integer removedValue = hashMap.remove("two");
// Print the removed value
System.out.println(removedValue); // 2

### What is a java HashSet ?

➢
HashSet in Java is a class from the Collections Framework.
➢
It allows you to store multiple values in a collection using a hash table.
➢
The hash table stores the values in an unordered method with the help of hashing mechanism.
Here are some of the methods of HashSet in Java:
❖
add(E e) : Adds the specified element to the HashSet.
❖
contains(Object o) : Returns true if the specified element is present in the HashSet.
❖
remove(Object o) : Removes the specified element from the HashSet.
❖
size() : Returns the number of elements in the HashSet.
❖
isEmpty() : Returns true if the HashSet is empty.
❖
clear() : Removes all elements from the HashSet.

### How do you create and manipulate HashSets in java ?

We add elements to the HashSet using the add() method, and then we print the HashSet using the println() method. We demonstrate checking if an element exists in the HashSet using the contains() method and removing an element using the remove() method.
SOURCE CODE:
import java.util.HashSet;
public class HashSetExample {
public static void main(String[] args) {
/ Create a HashSet
HashSet<String> names = new HashSet<>();
// Add elements to the HashSet
names.add("John");
names.add("Alice");
names.add("Bob");
// Print the HashSet
System.out.println("HashSet: " + names);
// Check if an element exists in the HashSet
boolean containsAlice = names.contains("Alice");
System.out.println("Contains 'Alice': " + containsAlice);
// Remove an element from the HashSet
names.remove("Bob");
System.out.println("After removal: " + names);
}
}
OUTPUT :
HashSet: [Alice, Bob, John]
Contains 'Alice': true
After removal: [Alice, John]

### What is the purpose of the add () method in java Hashset ?

add() method in Java HashSet is used to add a specific element into a HashSet. This method will add the element only if the specified element is not present in the HashSet else the function will return False if the element is already present in the HashSet.
HashSet<String> hashSet = new HashSet<>();
// Add a new element to the HashSet
hashSet.add("Element 1");
// Check if an element is present in the HashSet
boolean isPresent = hashSet.contains("Element 1");
// Print the result
System.out.println(isPresent); // true
In this example, we create a new HashSet and add the element "Element 1" to it. We then check if the element "Element 1" is present in the HashSet. The contains() method returns true, which means that the element is present in the HashSet.
The add() method is a very useful method for adding new elements to a HashSet and for checking if an element is already present in a HashSet.

### What is the purpose of the remove () method in java HashSet ?

remove() method is present in the HashSet class inside the java. util package. The HashSet remove() method is used to remove only the specified element from the HashSet .
SOURCE CODE:
import java.util.HashSet;
public class Main {
public static void main(String[] args) {
HashSet<String> names = new HashSet<>();
names.add("John");
names.add("Mary");
names.add("Bob");
// Remove the element "Mary" from the HashSet
names.remove("Mary");
// Print the remaining elements in the HashSet
for (String name : names) {
System.out.println(name);
}
}
}
OUTPUT :
John
Bob

### What is a java LinkedList ?

A linked list in Java is a dynamic data structure whose size increases as you add the elements and decreases as you remove the elements from the list. The elements in the linked list are stored in containers. The list holds the link to the first container.

### How do you create and manipulate LinkedList in java ?

Here are the steps on how to create and manipulate a Java LinkedList:
Import the LinkedList class :
The LinkedList class is part of the java.util package, so you need to import it before you can use it.
Create a new LinkedList object :
You can do this by using the new keyword, followed by the LinkedList class name.
Add elements to the LinkedList :
You can add elements to the LinkedList using the add() method. The add() method takes an element as an argument and adds it to the end of the LinkedList.
Remove elements from the LinkedList :
You can remove elements from the LinkedList using the remove() method. The remove() method takes an element as an argument and removes it from the LinkedList.
Get the size of the LinkedList :
You can get the size of the LinkedList using the size() method. The size() method returns the number of elements in the LinkedList.
Check if the LinkedList is empty :
You can check if the LinkedList is empty using the isEmpty() method. The isEmpty() method returns true if the LinkedList is empty, and false otherwise.
Iterate over the LinkedList:
You can iterate over the LinkedList using a for-each loop. The for-each loop will iterate over each element in the LinkedList and print it to the console.
Here is an example of how to create and manipulate a Java LinkedList:
import java.util.LinkedList;
public class Main {
public static void main(String[] args) {
// Create a new LinkedList object
LinkedList<String> names = new LinkedList<>();
// Add elements to the LinkedList
names.add("John");
names.add("Mary");
names.add("Bob");
// Remove an element from the LinkedList
names.remove("Bob");
// Get the size of the LinkedList
int size = names.size();
// Check if the LinkedList is empty
boolean isEmpty = names.isEmpty();
// Iterate over the LinkedList
for (String name : names) {
System.out.println(name);
}
}
}
OUTPUT:
John
Mary

### What is the purpose of the add () and remove ()methods in java LinkedList ?

The add() and remove() methods in Java LinkedList are used to add and remove elements from a linked list. The add() method takes an element as a parameter and adds it to the end of the list. The remove() method takes an element as a parameter and removes the first occurrence of that element from the list.
Here is an example of how to use the add() and remove() methods:
import java.util.LinkedList;
public class Main {
public static void main(String[] args) {
LinkedList<String> list = new LinkedList<>();
// Add elements to the list
list.add("Hello");
list.add("World");
// Print the list
System.out.println(list);
// Remove an element from the list
list.remove("Hello");
// Print the list again
System.out.println(list);
}
}
OUTPUT
[Hello, World]
[World]

### What is java TreeSet?

Published in the Java Collections group. Java provides a vast set of data structures for efficiently working with element collections. One such data structure is TreeSet, an implementation of a red-black tree in Java. TreeSet maintains a sorted order for storing unique elements.
Source Code :
import java.util.*;
public class TreeSetExample {
public static void main(String[] args) {
// Create a TreeSet
TreeSet<String> names = new TreeSet<>();
// Add some elements to the TreeSet
names.add("Alice");
names.add("Bob");
names.add("Carol");
names.add("Dave");
// Print the elements of the TreeSet
for (String name : names) {
System.out.println(name);
}
// Check if the TreeSet contains an element
System.out.println(names.contains("Alice")); // true
// Remove an element from the TreeSet
names.remove("Bob");
// Print the elements of the TreeSet
for (String name : names) {
System.out.println(name);
}
}
}
OUTPUT :
Alice
Carol
Dave
true
Alice
Carol
Dave

### What is the purpose of add() and remove() methods in java TreeSets ?

The add() method in a Java TreeSet is used to add a new element to the set. If the element is already present in the set, the add() method will do nothing and return false. Otherwise, the element will be added to the set and the method will return true.
The remove() method in a Java TreeSet is used to remove an element from the set. If the element is not present in the set, the remove() method will do nothing and return false. Otherwise, the element will be removed from the set and the method will return true.
Here is an example of how to use the add() and remove() methods in a Java TreeSet:
import java.util.*;
public class TreeSetExample {
public static void main(String[] args) {
TreeSet<Integer> treeSet = new TreeSet<>();
// Add elements to the TreeSet
treeSet.add(1);
treeSet.add(2);
treeSet.add(3);
treeSet.add(4);
treeSet.add(5);
// Remove an element from the TreeSet
treeSet.remove(3);
// Print the elements in the TreeSet
for (Integer element : treeSet) {
System.out.println(element);
}
}
}
OUTPUT:

### What is the purpose of the contains() method in java Collections ?

The contains() method of Java AbstractCollection is used to check whether an element is present in a Collection or not. It takes the element as a parameter and returns True if the element is present in the collection.
SOURCE CODE:
import java.util.ArrayList;
public class Main {
public static void main(String[] args) {
ArrayList<String> names = new ArrayList<>();
names.add("John");
names.add("Mary");
names.add("Bob");
// Check if the collection contains the element "John"
boolean containsJohn = names.contains("John");
// Print the result
System.out.println(containsJohn);
}
}
OUTPUT :
True

### What is the purpose of the isEmpty() method in java collections ?

The isEmpty() method is a convenient way to check if a collection is empty. It is available in all collection interfaces, so you can use it with any type of collection.
Source code :
import java.util.*;
public class Example {
public static void main(String[] args) {
// Create a new list
List<String> names = new ArrayList<>();
// Check if the list is empty
if (names.isEmpty()) {
System.out.println("The list is empty.");
} else {
System.out.println("The list is not empty.");
}
// Add an element to the list
names.add("John Doe");
// Check if the list is empty
if (names.isEmpty()) {
System.out.println("The list is empty.");
} else {
System.out.println("The list is not empty.");
}
}
}
Output :
The list is empty.
The list is not empty.

### What is the purpose of size() method in java collections ?

The size() method simply retrieves the value of the internal variable that tracks the number of elements in the ArrayList. This variable is updated every time an element is added or removed from the ArrayList.
The size() method in Java collections is used to get the number of elements in a collection. It is a very useful method for determining the length of a collection and for iterating over its elements. The size() method is available in all collection interfaces, including List, Set, and Map.
Here is an example of how to use the size() method:
import java.util.*;
public class Example {
public static void main(String[] args) {
// Create a list of elements
List<String> names = new ArrayList<>();
names.add("John");
names.add("Mary");
names.add("Bob");
// Get the size of the list
int size = names.size();
// Print the size of the list
System.out.println("The size of the list is: " + size);
}
}
OUTPUT:
The size of the list is: 3

### What is the purpose of the clear() method in java collections ?

The clear() method of Java Collection Interface removes all of the elements from this collection. It returns a Boolean value 'true', if it successfully empties the collection.
SOURCE CODE :
import java.util.ArrayList;
public class Main {
public static void main(String[] args) {
ArrayList<String> list = new ArrayList<>();
list.add("Hello");
list.add("World");
// Print the contents of the list
System.out.println(list);
// Clear the list
list.clear();
// Print the contents of the list again
System.out.println(list);
}
}
OUTPUT
[Hello, World]
[]

### What is the purpose of the iterator () method in java collections ?

Java Iterator Interface of java collections allows us to access elements of the collection and is used to iterate over the elements in the collection(Map, List or Set). It helps to easily retrieve the elements of a collection and perform operations on each element.
The Iterator interface provides three methods: hasNext(), next(), and remove().
1.
The hasNext() method returns true if there are more elements in the collection, and false otherwise.
2.
The next() method returns the next element in the collection
3.
The remove() method removes the current element from the collection.
The iterator() method is a very useful method for iterating over collections in Java. It allows you to easily access and manipulate the elements of a collection.
SOURCE CODE:
import java.util.ArrayList;
import java.util.Iterator;
public class Main {
public static void main(String[] args) {
ArrayList<String> names = new ArrayList<>();
names.add("Alice");
names.add("Bob");
names.add("Charlie");
Iterator<String> iterator = names.iterator();
while (iterator.hasNext()) {
String name = iterator.next();
System.out.println(name);
}
}
}
OUTPUT :
Alice
Bob
Charlie

### What is the java Comparator ?

A comparator interface is used to order the objects of user-defined classes. A comparator object is capable of comparing two objects of the same class.
The Comparator interface in Java is used to order the objects of a user-defined class. It provides a single method, compare(), which takes two objects as input and returns an integer value indicating whether the first object is less than, equal to, or greater than the second object. The compare() method has the following signature:
int compare(T o1, T o2);
where T is the type of object being compared.
The Comparator interface can be used to sort collections of objects using the Collections.sort() or Arrays.sort() methods. It can also be used to create sorted sets and maps.
Here is an example of how to use the Comparator interface to sort a collection of objects:
SOURCE CODE :
import java.util.*;
public class ComparatorExample {
public static void main(String[] args) {
// Create a list of objects
List<Integer> numbers = new ArrayList<>();
numbers.add(10);
numbers.add(5);
numbers.add(20);
// Create a comparator to compare the objects by their values
Comparator<Integer> comparator = new Comparator<Integer>() {
@Override
public int compare(Integer o1, Integer o2) {
return o1.compareTo(o2);
}
};
// Sort the list using the comparator
Collections.sort(numbers, comparator);
// Print the sorted list
for (Integer number : numbers) {
System.out.println(number);
}
}
}
OUTPUT

### How do you implement custom sorting using a comparator in java ?

To sort a collection of objects by multiple criteria, first define the class representing your objects. Then, create a custom comparator class that implements the Comparator interface. Override the compare method to define how objects should be compared based on different criteria
SOURCE CODE:
import java.util.Comparator;
public class StudentComparator implements Comparator<Student> {
@Override
public int compare(Student student1, Student student2) {
int nameComparison = student1.getName().compareTo(student2.getName());
if (nameComparison == 0) {
return student1.getAge() - student2.getAge();
} else {
return nameComparison;
}
}
}
To use this comparator, you would simply pass it to the sort() method of a list of students:
SOURCE CODE:
List<Student> students = new ArrayList<>();
students.add(new Student("Alice", 12));
students.add(new Student("Bob", 10));
students.add(new Student("Carol", 11));
students.sort(new StudentComparator());
After sorting, the students list will be in the following order:
[Bob, Carol, Alice]
You can use custom comparators to sort any collection of objects, regardless of their type. This can be useful for sorting objects based on multiple criteria or for sorting objects in a specific order that is not defined by the object's class.

### Comparable vs Comparator — what's the difference and when do you use each?

No answer provided yet.

### What is fail-fast iteration?

No answer provided yet.

### Discuss the Stream API in Java.

The Stream API processes collections of objects in a functional style, supporting operations 
like filter, map, and reduce.

### Intermediate vs terminal operations?

No answer provided yet.

### `reduce()` vs `collect()`?

No answer provided yet.

### Discuss the lifecycle of a thread in Java.

1. New: Thread is created. 
2. Runnable: Thread is ready to run. 
3. Running: Thread is executing. 
4. Blocked/Waiting: Thread is waiting for a resource. 
5. Terminated: Thread execution is complete.

### Explain the concept of synchronization in Java.

Synchronization prevents thread interference by allowing only one thread to access a critical 
section at a time, using the synchronized keyword.

### What is java thread ?

A thread in Java is the direction or path that is taken while a program is being executed. Generally, all the programs have at least one thread, known as the main thread, that is provided by the JVM or Java Virtual Machine at the starting of the program's execution.

### How do you create and start a thread in java ?

There are two ways to create threads in Java:
1.
By extending the Thread class and implementing the run() method.
2.
By implementing the Runnable interface and passing an instance of the class to the Thread constructor.
Once a thread is created, it can be started by calling the start() method. The thread will then run concurrently with the main thread until it finishes executing its run() method.
To create a thread by extending the Thread class, you need to create a subclass of Thread and override the run() method. The run() method contains the code that will be executed by the thread.
To start a thread, you need to call the start() method on the thread object. The start() method causes the thread to begin executing the run() method.
Here is an example of how to create and start a thread by extending the Thread class:
SOURCE CODE :
public class MyThread extends Thread {
@Override
public void run() {
System.out.println("Hello from MyThread!");
}
}
public class Main {
public static void main(String[] args) {
MyThread thread = new MyThread();
thread.start();
}
}
OUTPUT
Hello from MyThread!
create a thread by implementing the Runnable interface, you need to create a class that implements the Runnable interface. The Runnable interface has a single method, run(), which contains the code that will be executed by the thread.
To start a thread, you need to create a Thread object and pass the Runnable object to the constructor. Then, you need to call the start() method on the Thread object.
Here is an example of how to create and start a thread by implementing the Runnable interface:
SOURCE CODE :
public class MyRunnable implements Runnable {
@Override
public void run() {
System.out.println("Hello from MyRunnable!");
}
}
public class Main {
public static void main(String[] args) {
MyRunnable runnable = new MyRunnable();
Thread thread = new Thread(runnable);
thread.start();
}
}
OUTPUT :
Hello from MyRunnable!

### What is difference between a thread and a process in java ?

Process :
✓
A process is an independent unit of execution in Java.
✓
It has its own memory space, which contains its code, data, and stack.
✓
A process can have multiple threads.
✓
Processes are created and destroyed by the operating system.
✓
Processes communicate with each other through inter-process communication (IPC) mechanisms, such as pipes, sockets, and shared memory.
Thread :
✓
A thread is a lightweight unit of execution within a process.
✓
It shares the process's memory space, but it has its own stack.
✓
A thread can be created and destroyed by the Java Virtual Machine (JVM).
✓
Threads communicate with each other through shared memory. Feature Thread Process Memory space Shares the process's memory space Has its own memory space Stack Has its own stack Has its own stack Creation Created and destroyed by the JVM Created and destroyed by the operating system Communication Communicates with other threads through shared memory Communicates with other processes through IPC mechanisms

### What is synchronization in java ?

Synchronization in java is the capability to control the access of multiple threads to any shared resource. In the Multithreading concept, multiple threads try to access the shared resources at a time to produce inconsistent results. The synchronization is necessary for reliable communication between threads.

### How do you achieve synchronization in java ?

In Java, synchronization can be achieved using the synchronized keyword and the volatile modifier. The synchronized Keyword: The synchronized keyword is used to define critical sections of code that should be accessed by only one thread at a time. It can be applied to methods or code blocks.
Eg :
// Synchronize a method
public synchronized void increment() {
// ...
}
// Synchronize a code block
synchronized (obj) {
// ...
}

### What are the states of a thread in java ?

A thread in Java can exist in one of the following six states at any given time:
1.
New: When a thread object is created, it is in the new state. It is not yet runnable.
2.
Runnable: A thread that is ready to run is in the runnable state. It is waiting to be scheduled by the operating system.
3.
Running: A thread that is currently executing is in the running state.
4.
Blocked: A thread that is waiting for an event to occur, such as a lock to become available, is in the blocked state.
5.
Waiting: A thread that is waiting for another thread to finish executing is in the waiting state.
6.
Terminated: A thread that has finished executing is in the terminated state
Example :
Thread thread = new Thread();
Thread.State state = thread.getState();

### What is deadlock in java ?

Deadlock in Java is a condition where two or more threads are blocked forever, waiting for each other. This usually happens when multiple threads need the same locks but obtain them in different orders. Multithreaded Programming in Java suffers from the deadlock situation because of the synchronized keyword.

### How do you prevent deadlock in java ?

Here are some ways to prevent deadlock in Java:
Avoid unnecessary locking :
Only lock resources when absolutely necessary, and release them as soon as possible.
Acquire locks in a consistent order :
This means that all threads should acquire locks in the same order, to avoid situations where two threads are waiting for each other to release locks.
Avoid nested locks :
This means that a thread should not acquire a second lock while it is already holding another lock.
Use timeouts when acquiring locks.
This will help to prevent situations where a thread is waiting indefinitely for a lock to be released.
Use lock-free data structures :
These data structures are designed to be used by multiple threads without the need for locks.
Use a deadlock detection and avoidance algorithm :
This can be a complex solution, but it can be effective in preventing deadlocks in situations where other methods are not feasible.
Here are some additional tips for preventing deadlocks in Java:
Use proper synchronization techniques :
When multiple threads are accessing the same data, it is important to use synchronization techniques to ensure that the data is not corrupted.
Be aware of the potential for deadlocks :
When designing your code, be aware of the potential for deadlocks and take steps to avoid them.
Test your code thoroughly :
Test your code thoroughly to ensure that it is free of deadlocks.
These tips, you can help to prevent deadlocks in your Java code.

### What is the purpose of the Wait(), notify(), notifyAll() methods in java ?

The wait(), notify(), and notifyAll() methods in Java are used to coordinate actions of multiple threads. They are part of the Object class and are used to implement inter-thread communication.
❖
wait(): Causes the current thread to wait indefinitely until another thread invokes notify() or notifyAll() on the same object.
❖
notify(): Wakes up a single thread that is waiting on that object's monitor.
❖
notifyAll(): Wakes up all threads that are waiting on that object's monitor.
These methods are typically used in conjunction with synchronized blocks to ensure that only one thread can access a shared resource at a time. For example, a producer-consumer problem could be solved using wait() and notify() as follows:
SOURCE CODE :
class Producer {
private Object lock;
public Producer(Object lock) {
this.lock = lock;
}
public void produce() {
synchronized (lock) {
// Produce an item
// Notify the consumer that an item is available
lock.notify();
}
}
}
class Consumer {
private Object lock;
public Consumer(Object lock) {
this.lock = lock;
}
public void consume() {
synchronized (lock) {
// Wait for an item to be produced
lock.wait();
// Consume the item
}
}
}
The wait(), notify(), and notifyAll() methods are powerful tools for coordinating the actions of multiple threads. However, they must be used carefully to avoid race conditions and deadlocks.

### Concurrency vs parallelism?

No answer provided yet.

### What is happens-before?

No answer provided yet.

### What is CAS (compare-and-swap)?

No answer provided yet.

### `execute()` vs `submit()`?

No answer provided yet.

### Differentiate between JDK, JRE, and JVM.

• JDK (Java Development Kit): Provides tools for development (compiler, debugger). 
• JRE (Java Runtime Environment): Includes libraries and JVM for running Java 
applications. 
• JVM (Java Virtual Machine): Converts bytecode into machine code and executes it.

### How does Java achieve memory management?

Java uses automatic garbage collection to manage memory. Objects are allocated in the 
heap memory, and when they are no longer referenced, the garbage collector deallocates 
them.

### Describe the Java Memory Model (JMM).

Defines how threads interact through memory, ensuring visibility and ordering of variable 
accesses.

### How does Java handle memory leaks?

Java uses garbage collection but memory leaks can occur if references to unused objects are 
maintained.

### Explain the differ b/w JDK, JRE, JVM.

JDK :
Java Development Kit is a software development environment that includes JRE and development tools. It's used to create Java applications and applets. JDK includes tools like a compiler, debugger, and documentation generator.
JRE :
Java Runtime Environment is a set of software tools that provides a runtime environment for running other software. It's used to run Java applications. JRE contains class libraries, supporting files, and the JVM.
JVM :
Java Virtual Machine is the foundation of Java programming language and ensures the program's Java source code will be platform-agnostic. It's used to run Java bytecode. JVM is included in both JDK and JRE, and Java programs won't run without it.

### Interpreter vs JIT compiler?

No answer provided yet.

### How would you investigate a memory leak?

No answer provided yet.

### How would you investigate high CPU usage in a running JVM?

No answer provided yet.

### How would you secure a REST API end to end?

No answer provided yet.

### What are design patterns? Name a few commonly used ones in Java.

Design patterns are reusable solutions to common software design problems. Examples: 
Singleton, Factory, Observer.

### What is Java?

Java is a object oriented, platform independent, case sensitive, strongly typed checking ,
high level , open source programming language developed by James Gosling in the
year of 1995.

### Features of Java?

1)Simple
2)Object oriented
3)Platform independent
4)Portable
5)Architecture Neutral
6)Highly secured
7)Robust
8)Multithreaded
9)Distributed
10)Dynamic

### Differences betweend JDK , JRE and JVM ?

JDK
JDK stands for Java Development Kit.
It is a installable software which contains compiler (javac) , interpreter (java),
Java virtual machine (JVM), archiever (.jar) , document generator (javadoc) and
other tools needed for java application development.
JRE
JRE stands for Java Runtime Environment.
It provides very good environment to run java applications only.
JVM
JVM stands for Java Virtual Machine.
It is an interpreter which is used to execute our program line by line procedure.

### Is it possible to execute java program without main methods?

Till 1.6 version it is possible to execute java program without main method
using static block. But from 1.7 version onwards it is not possible to execute
java program without main method.
ex: class A
{
static
{
System.out.println("Hello World");
System.exit(0);
}
}

### What is static import in java?

Using static import we can access static members directly.
Ex: import static java.lang.System.*;
class Test
{
public static void main(String[] args)
{
out.println("Hello World");
}
}
8)Which is a default package in java?
java.lang package

### What is JIT compiler?

IT is a part of a JVM which is used to increase the execution speed of our program.

### How many memories are there in java?

We have five memories in java.
1) Method area
2) Heap
3) JAva Stack
4) PC Register
5) Native method stack

### What is native method in java?

A Method which is developed by using some other language is called native method.

### What is Garbage Collector ?

Garbage collector is responsible to destroy unused or useless objects in java.
There are two ways to call garbage collector in java.
1) System.gc()
2) Runtime.getRuntime().gc()

### Is java support access specifiers?

Java does not support access specifiers.
Java support access modifiers.
1) default
2) public
3) private
4) protected

### What is program?

Program is a collection of instructions (or) Program is a set of instructions.

### Types of blocks in java?

We have three types of blocks in java.
1) Instance block
2) Static block
3) Local block

### Explain main method ?

public:
JVM wants to call this method from any where that's why main method is public.
static:
JVM wants to call this method without using object reference.
void:
Main method does not return anything to JVM.
main:
It is an identifier given to a main method.
String[] args:
It is a command line argument.
20)Is java purely object oriented or not?
No, java will not consider as purely object oriented because
it does not support many OOPS concepts like multiple inheritance, operator overloading
and more ever we depends upon primitive datatypes which are non-objects.

### Enhancement becomes more easy without effecting enduser they can perform any

changes in our internal system.
3)It provides flexibility to the enduser to use the system.
4)It improves maintainability of an application.

### What is Method Overriding?

Having same method name with same parameters in two different classes is called
method overriding.
Methods which are present in parent class are called overridden methods.
Methods which are present in child class are called overriding methods.
Method resolution will taken care by JVM based on runtime objects.
ex: class A
{
public void m1()
{
System.out.println("ITALENT");
}
}
class B extends A
{
public void m1()
{
System.out.println("IIHUB TALENT");
}
}

### What is Method Hiding?

Method hiding is exactly the same as method overriding with following differences.
Method overriding Method hiding
All the methods present in method overriding All the methods present in method
must be non-static. hiding must be static.
Method resolution will taken care by JVM Method resolution will taken care
based on runtime object. by compiler based on reference type.
It is also known as runtime polymorphism, It is also known as compile time
dynamic polymorphism, late binding. polymorphism, static polymorphism,early binding.

### What is API?

API stands for application programming interface.
It is a base for the programmer to develop software applications.
API is a collection of packages.
We have three types of API's.
1)Predefined API:
Built-In API is called predefined API.
2)Userdefined API:
API which is created by the user based on the requirement is
called userdefined API.
3)Third party API:
API which is given by third party vendor.
ex: JAVAZOOM API , Text API and etc.

### What is Has-A relationship?

Has-A relationship is also known as composition and aggregation.
There is no specific keyword to implement Has-A relationship but mostly we will use new operator.
The main objective of Has-A relationship is to provide reusability.
Has-A relationship will increase dependency between two components.
ex:
class Engine
{
- //engine specific functionality
}
class Car
{
Engine e=new Engine();
}
Composition
Without existing container object there is a no chance of having contained object then the relationship between
container object and contained object is called composition which is strongly association.
Aggregation
Without existing container object there is a chance of having contained object then the relationship
between container object and contained object is called aggregation which is loosely association.

### What is package?

Package is a collection of classes, interfaces, enums ,Annotations, Exceptions and Errors.
Enum,Exception and Error is a special class and Annotation is a special interface.
In general, a package is a collection of classes and interfaces.
Package is also known as folder or a directory.
In java, packages are divided into two types.
All package names we need to declare under lower case letters only.
1)Predefined packages:
Built-In packages are called predefined packages.
ex: java.lang , java.io, java.util
2)Userdefined package:
Packages which are created by ther user are called userdefined packages.
It is highly recommanded to use package name in the reverse order of url.
ex: com.ihubtalent.www

### What is the difference b/w length and length() ?

Length length()
It is a final variable which is It is a final method which is applicable
applicable only for arrays only for String objects.
It will return size of an array. It will return number of character present in String.
Ex: ex:
class Test class Test
{ {
public static void main(String[] args) public static void main(String[] args)
{ {
int[] arr=new int[3]; String str="hello";
System.out.println(arr.length);//3 System.out.println(str.length());//5
System.out.println(arr.length()); //CTE System.out.println(str.length);//C.T.E
} }
} }

### What is Recursion?

A method is called self for many number of times is called Recursion.
Recursion is similar to loopings.
In Recursion post Increment and Decrement are used.

### What is Enum?

Enum is a group of named constants.
Enum concept is introduced in 1.5 version.
Using enum we can create our own datatype called enumerated datatype.
When compare to old language enum, java enum is more powerful.
Enum is a special class.
To declare enum we need to use enum keyword.
Syntax:
enum enum_type_name
{
val1,val2,....,valN
}
Ex:
enum Months
{
JAN,FEB,MAR
}

### what is Generics?

Array is a typesafe.
We can provide guarantee that what type of elements are present in array.
If requirement to store String values then we need to use String[] array.
ex:
String[] str=new String[100];
str[0]="hi";
str[2]=10; // invalid
At the time of retrieving the data from array , we don't need to perform any typecasting
ex:
String[] str=new String[100];
str[0]="hi";

### Comparable vs Comparator?

OR
What is the difference between Comparable and Comparator interface?
Comparable
Comparable interface present in java.lang package.
Comparable interface contains following one method i.e compareTo() method.
Ex:public int compareTo(Object o)
If we depend upon default natural sorting(ascending order) order then we need to use Comparable interface.
Comparator
Comparator interface present in java.util package.
Comparator interface contains following two methods.
1) public int compare(Object obj1,Object obj2)
2) public boolean equals(Object obj)
Whenever we are using Comparator interface we should write
implementation only for compare() method.
Implementation for equals() method is optional because equals() method is available by default by Object class throw inheritence.
If we depend upon customized sorting order then we need to use Comparator interface.

### Type of Datastructure in java?



### What is Multi-tasking?

Executing several task simultenously such concept is called multi-tasking.
Multi-tasking is divided into two types.
1)Thread based multi-tasking
Executing several task simultenously where each task is a same part of a program.
It is best suitable for programmatic level.
2)Process based multi-tasking
Executing several task simultenously where each task is a independent process.
It is best suitable for OS level.

### What is Lock Mechanism in Java?

synchronization is build around an entity called lock.
Whenever a thread wants to access any object. First it has to acquire the lock of it and release the lock once thread complete its task.

### What is Synchronized block?

If we want to perform synchronization on specific resource of a program then we need to use
synchronization.
ex: If we have 100 lines of code and if we want to perform synchronization only for
10 lines then we need to use synchronized block.
If we keep all the logic in synchronized block then it will act as a synchronized method.
3)Static Synchronization:
In static synchronization the lock will be on class but not on object.
If we declare any static method as synchronized then it is called static synchronization method.

### What is DeadLock in java?

DeadLock will occur in a suitation when one thread is waiting to access
object lock which is acquired by another thread and that thread is waiting
to access object lock which is acquired by first thread.
Here both the threads are waiting release the thread but no body will
release such situation is called DeadLock.

### What is web application?

Web application is a collection of web resource programs having the capability to
generate web pages.
We have two types of web pages.
1)Static web page
2)Dynamic web page

### What is web resource program?

We have two types of web resource programs.
1)Static web resource program
It is responsible to generate static web pages.
Ex: HTML program
CSS program
Bootstrap program
Angular program and etc.
2)Dynamic web resource program
It is responsible to generate dynamic web pages.
ex: Servlet program
JSP program and etc.

