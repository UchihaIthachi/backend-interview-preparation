# Advanced Questions

## Question 1: What is a HashMap? How does it work internally?

HashMap stores key-value pairs using a hash table. Keys are hashed to determine the index,
and collisions are handled using linked lists or trees.

## Question 2: How does `HashMap` work internally?

No answer provided yet.

## Question 3: How does `ConcurrentHashMap` achieve thread safety without locking the whole map?

No answer provided yet.

## Question 4: What is the Difference between List vs Set

LIST SET
It is a child interface of Collection interface. It is a child interface of Collection interface.
If we want to represent group of individual objects If we want to represent group of individual objects
in a single entity where duplicates are allowed. where duplicates are not allowed and order is not preserved then we need
order is preserved. to use Set interface.

## Question 5: What is the Difference between HashSet vs LinkedHashSet?

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

## Question 6: What is the Difference between HashSet vs TreeSet?

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

## Question 7: What is the Difference between HashMap vs LinkedHashMap?

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

## Question 8: What is the Difference between HashMap vs TreeMap

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

## Question 9: What is the Difference between TreeMap vs Hashtable?

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
