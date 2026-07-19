# Arrays Questions

### Differentiate between ArrayList and LinkedList.

• ArrayList: Backed by a dynamic array, faster for indexing. 
• LinkedList: Backed by a doubly-linked list, better for insertions/deletions.

### What is a java array ?

In Java, an array is a data structure that can store a fixed-size sequence of elements of the same data type. An array is an object in Java, which means it can be assigned to a variable, passed as a parameter to a method, and returned as a value from a method.

### How do you declare and initialize array in java ?

For eg;
String[] cars = {"Volvo", "BMW", "Ford", "Mazda"};
int[] myNum = {10, 20, 30, 40};
you can use and iterate over the elements in array
for (int i = 0; i < myArray.length; i++) {
System.out.println(myArray[i]);
}

### What is the length of an array in java ?

The theoretical maximum Java array size is 2,147,483,647 elements. To find the size of a Java array, query an array's length property. The Java array size is set permanently when the array is initialized. The size or length count of an array in Java includes both null and non-null characters.
The following code prints length of the array:
int[] arr = new int[5];
System.out.println(arr.length); // Prints 5

### What is diff b/w Array and ArrayList in java ?

Array
•
An array is a data structure that stores a collection of items. The size of an array is fixed, meaning that it cannot be changed once it is created.
•
Arrays can store both primitive and object types.
•
Arrays are accessed using indexes. The index of an element is its position in the array, starting from 0.
•
Elements can be inserted into and deleted from arrays, but this can be slow and inefficient.
•
Arrays can be sorted using the Arrays.sort() method.
•
Arrays can be multidimensional, meaning that they can store arrays of arrays.
•
Arrays cannot store null values.
•
Arrays are generally faster than ArrayLists, but they are also less flexible.
ArrayList
•
An ArrayList is a resizable array. This means that its size can be changed as needed.
•
ArrayLists can only store object types.
•
ArrayLists are accessed using methods, such as get(), add(), and remove().
•
Elements can be inserted into and deleted from ArrayLists efficiently.
•
ArrayLists can be sorted using the Collections.sort() method.
•
ArrayLists can only be single-dimensional.
•
ArrayLists can store null values.
•
ArrayLists are generally slower than arrays, but they are also more flexible.

### What is a java Arraylist ?

An ArrayList class is a resizable array, which is present in the “java. util package”. While built-in arrays have a fixed size, ArrayLists can change their size dynamically. Elements can be added and removed from an ArrayList whenever there is a need, helping the user with memory management.

### How do you create and manipulate ArrayLists in java ?

Here are the steps on how to create and manipulate ArrayLists in Java:
To create an ArrayList, you can:
Import the java.util.ArrayList package.
Create an ArrayList. For example, to create an ArrayList of strings, you can use:
ArrayList<String> languages = new ArrayList<>();
Here are some other methods of the ArrayList class:
❖
get() : Returns an element from the ArrayList.
❖
set() : Changes an element of the ArrayList.
❖
remove() : Removes an element from the ArrayList.
❖
size() : Returns the number of elements in the ArrayList.
❖
isEmpty() : Checks if the ArrayList is empty.
❖
contains() : Checks if the ArrayList contains a specific element.
❖
clear() : Removes all elements from the ArrayList.

### What is the purpose of the ToArray() method in java collections ?

The toArray() method of the ArrayList is used to convert an ArrayList to an array in Java. This function either takes in no parameter or takes in an array of Type T(T[] a) in which the element of the list will be stored. The toArray() function returns an Object array if no argument is passed.
SOURCE CODE
import java.util.*;
public class Main {
public static void main(String[] args) {
List<String> list = new ArrayList<>();
list.add("Hello");
list.add("World");
// Convert the list to an array
String[] array = list.toArray(new String[0]);
// Print the array
for (String s : array) {
System.out.println(s);
}
}
}
Output:
Hello
World

### `ArrayList` vs `LinkedList`?

No answer provided yet.

### Why is `ArrayDeque` often preferred over `Stack`?

No answer provided yet.

### what is Array?

In a normal variable we can store only one value at a time.
To store more then one value in a single variable then we need to use arrays.
Definition:
Array is a collection of homogeneous data elements.
The main advantages of array are.

### Performance point of view array is recommanded to use.

The main disadvantages of array are.

### Arrays are fixed in size.Once if we create an array there is no chance of increasing and decreasing the size of an array.



### To use array concept in advanced we should know what is the size of an array which is always not possible.

In java, arrays are categorised into three types.
1)Single Dimensional Array
2)Two Dimensional Array / Double Dimensional Array
3)Multi-Dimensional Array / Three Dimensional Array

### Difference between Arrays and Collections?

Arrays Collections
It is a collection of homogenous data It is a collection of homogenous and
elements. hetrogenous data elements.
Arrays are fixed in size. Collections are growable in nature.
Performance point of view array is Memory point of view Collection is
recommanded to use. recommanded to use.
It is typesafe. It is not typesafe.
Arrays are not implemented based on Collections are implemented based on
data structure concept so we can't data structure concept so we can expect
expect any ready made methods.For readymade methods.
every logic we need to write the code
explicitly.
It holds primitive and object types. It holds only object type.

### What is the Difference between ArrayList vs Vector?

ArrayList Vector
The underlying data structure is The underlying data structure is resizable or
doublyArrayList. growable array in Vector.
Insertion order is preserved. Insertion order is preserved.
Duplicate objects are allowed. Duplicate objects are allowed.
Hetrogeneous objects are allowed. Hetrogeneous objects are allowed.
Null insertion is possible. Null insertion is possible.
It implements Serializable It implements Serializable
and Cloneable, Random Access and Cloneable, Random Access interface.
interface.

### What is the Difference between ArrayList vs LinkedList?

ArrayList LinkedList
The underlying data structure is The underlying data structure is
resize or growable ArrayList. doubly LinkedList.
Insertion order is preserved. Insertion order is preserved.
Duplicate objects are allowed. Duplicate objects are allowed.
Hetrogeneous objects are allowed. Hetrogeneous objects are allowed.
Null insertion is possible. Null insertion is possible.
It implements Serializable It implements Serializable
and Cloneable, Random Access and Cloneable, Random Access interface.
interface.
If our frequent operation is If our frequent operation is
retrieval or select operation then insert or delete in the middle then
we need to use ArrayList. LinkedList is a best choice.

