# List Set Map Questions

### What is a HashMap? How does it work internally?

HashMap stores key-value pairs using a hash table. Keys are hashed to determine the index, 
and collisions are handled using linked lists or trees.

### How does `HashMap` work internally?

No answer provided yet.

### `HashMap` vs `ConcurrentHashMap`?

No answer provided yet.

### `HashSet` vs `TreeSet`?

No answer provided yet.

### When should immutable collections (`List.of()`, `Collections.unmodifiableList()`) be used?

No answer provided yet.

### `map()` vs `flatMap()`?

No answer provided yet.

### How does `ConcurrentHashMap` achieve thread safety without locking the whole map?

No answer provided yet.

### Minor vs major/full collection?

No answer provided yet.

### What is Collection?

Collection is an interface which is present in java.util package.
It is a root interface for entire collection framework.
If we want to represent group of individual objects in a single entity then we need to use Collections.

### What is the Difference between List vs Set

LIST SET
It is a child interface of Collection interface. It is a child interface of Collection interface.
If we want to represent group of individual objects If we want to represent group of individual objects
in a single entity where duplicates are allowed. where duplicates are not allowed and order is not preserved then we need
order is preserved. to use Set interface.

### What is the Difference between HashSet vs LinkedHashSet?

HashSet LinkedHashSet
The underlying data structure is The underlying data structure is
Hashtable. Hashtable and LinkedList.
Insertion order is not preserved. Insertion order is preserved.
bcoz objects are arrange based on
hashcode of an oject.
Duplicate objects are not allowed. Duplicate objects are not allowed.
Hetrogeneous objects are allowed. Hetrogeneous objects are allowed.
Null insertion is possible. Null insertion is possible.
It implements Serializable
and Cloneable interface.
Introduced in 1.4v. Introduced in 1.4v.

### What is the Difference between HashSet vs TreeSet?

HashSet TreeSet
The underlying data structure is The underlying data structure is
Hashtable. Balanced Tree.
Insertion order is not preserved. Insertion order is preserved.
bcoz objects are arrange based on bcoz it is based on sorting
hashcode of an oject. order of an object.
Duplicate objects are not allowed. Duplicate objects are not allowed.
Hetrogeneous objects are allowed. Hetrogeneous objects are allowed.
Null insertion is possible. Null insertion is possible only once.
It implements Serializable It implements Serializable
and Cloneable interface. Navigableset and Cloneable interface.
Map
It is not a child interface of Collection interface.
If we want to represent group of individual objects in a key-value pair then we need to use Map interface.
Both key and value are objects only.
Duplicate keys are not allowed but values can be duplicated.
Each key-value pair is called "one entry".

### What is the Difference between HashMap vs LinkedHashMap?

HashMap LinkedHashMap
The underlying data structure is The underlying data structure is
Hashtable. Hashtable. It is a child class of HashMap
class.
Insertion order is not preserved Insertion order is not preserved.
and it is based on hashcode of the
keys.
Duplicate keys are not allowed.
but values can be duplicated.
Hetrogeneous objects are allowed
for both keys and values.
Null insertion is possible
for keys(only once) and for values
(any number).
Introduced in 1.2v. Introduced in 1.4v.

### What is the Difference between HashMap vs TreeMap

HashMap TreeMap
The underlying data structure is The underlying data structure is
Hashtable. RED BLACK TREE.
Insertion order is not preserved Insertion order is not preserved.
and it is based on hashcode of the All entries will store in the sorting
keys. order of keys.
Duplicate keys are not allowed. Duplicate keys are not allowed but values
can be duplicated.
Hetrogeneous objects are allowed
for both keys and values
Null insertion is possible see below QUESTION AND ANSWER
for keys(only once) and for values
(any number).

### What is TreeMap?

The underlying data structure is RED BLACK TREE.
Duplicate keys are not allowed but values can be duplicated.
Insertion order is not preserved. All entries will store in the sorting order of keys.
If we depends upon natural sorting order then keys can be homogeneous
and Comparable.
If we depends upon customized sorting order then keys can be hetrogeneous and
non-comparable.
For empty TreeMap if we insert NULL as key then we will get NullPointerException.
After insertion elements if we are trying to insert NULL as key the we will
get NullPointerException.
But there is no restrictions on NULL values

### What is the Difference between TreeMap vs Hashtable?

TreeMap HashTable
The underlying data structure is The underlying data structure is doubly LinkedList.
RED BLACK TREE.
Insertion order is not preserved. Insertion order is not preserved.
Duplicate keys are not allowed Duplicate keys are not allowed but values are allowed.
but values can be duplicated.
Hetrogeneous keys and values are allowed.
SEE ABOVE QUESTION & ANS
and values can't be Null otherwise we willget
NullPointerException.

### Explain Enumeration vs Iterator vs ListIterator

Types of Cursors in java
Cursors are used to retrieve the objects one by one from Collection.
We have three types of cursors in java.
1)Enumeration
2)Iterator
3)ListIterator
1)Enumeration
Enumeration interface present in java.util package.
It is used to read objects one by one from Legacy Collection objects.
Enumeration object can be created by using elements() method.
ex: Enumeration e=v.elements();
Enumeration interface contains following two methods.
ex: public boolean hasMoreElements();
public Object nextElement();
2)Iterator
It is used to retrieve the objects one by one from any Collection object.Hence it is known as universal cursor.
Using Iterator interface we can perform read and remove operation.
We can create Iterator object by using iterator() method.
ex: Iterator itr=al.iterator();
Iterator interface contains following three methods.
ex: public boolean hasNext();
public Object next();
public void remove();
3)ListIterator
It is a child interface of Iterator interface.
It is used to read objects one by one from List Collection objects.
Using ListIterator we can read objects in forward direction and backward direction.Hence it is a bi-directional cursor.
Using ListIterator we can perform read, remove ,adding and replacement of new objects.
We can create ListIterator interface by using listIterator() method.
ex: ListIterator litr=al.listIterator();

### What is Setting and Getting Name of a thread?

In java, every thread has a name automatically generated by JVM or explictly provided by the programmer.
We have following methods to set and get name of a thread.
ex: public final void setName(String name)
public final String getName()
In how many ways we can prevent a thread from execution
There are three ways to prevent(stop) a thread from execution.
1)yield()
2)join()
3)sleep()
1)yield()
It will pause the current execution thread and gives chance to other threads having same priority.
If multiple threads having same priority then we can't expect any execution order.
If there is no waiting threads then same thread will continue the execution.
Ex: public static native void yield()
2)join()
If a thread wants to wait untill the completion of some other thread then we need to use join() method.
A join() method throws one checked exception called InterruptedException so we must and should handle that exception by using try and catch block or by using throws statement
ex: public final void join()throws InterruptedException
public final void join(long ms)throws InterruptedException
public final void join(long ms,int ns)throws InterruptedException.
3)SLEEP():
If a thread don't want to perform any operation on perticular amount of time then we need to use sleep() method.
A sleep() method will throw one checked exception called InterruptedException so must and should handle that exception by using try and catch block or by using throws statement.
ex: public static native void sleep()throws InterruptedException
public static native void sleep(long ms)throws InterruptedException
public static native void sleep(long ms,int ns)throws InterruptedException

### What is ResultSetMetaData?

ResultSetMetaData is an interface which is present in java.sql package.
ResultSetMetaData provides metadata of a table.
ResultSetMetaData gives information about number of columns, datatype of a columns, size of a column and etc.
We can create ResultSetMetaData object by using getMetaData() method of ResultSet obj.
ex:
ResultSetMetaData rsmd=rs.getMetaData();

