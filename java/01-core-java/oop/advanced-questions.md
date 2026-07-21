# Advanced Questions

## Question 1: What is an interface in Java, and how does it differ from an abstract class?

• Interface: A collection of abstract methods and static constants.
• Can have default and static methods (since Java 8).

• A class can implement multiple interfaces.
Difference:
• Abstract class can have both abstract and concrete methods; an interface has abstract
methods by default (Java 7 and below).
• A class extends one abstract class but can implement multiple interfaces.

## Question 2: What is constructor in java ?A constructor in Java is a special method that is used to initialize objects. It is called when an object of a class is created. It can be used to set initial values for object attributes. Constructors are similar to methods, but they have some important differences:

❖
Constructors have the same name as the class they are in.
❖
Constructors do not have a return type.
❖
Constructors are called automatically when an object is created.
Here is an example of a constructor:
public class Person {
private String name;
private int age;
public Person(String name, int age) {
this.name = name;
this.age = age;
}
}
This constructor takes two parameters, a name and an age, and uses them to initialize the corresponding object attributes.
To create a new Person object, you would use the following code: Person person = new Person("John Doe", 30);

## Question 3: What is the difference between Comparable and Comparator interfaces?

• Comparable: Used to define natural ordering.
• Comparator: Defines custom ordering.

## Question 4: What is the difference between POJO class and Java Bean class?

POJO class
A class is said to be pojo class if it supports following two properties.
1)All variables must be private.
2)All variables must have setter and getter method.
Java Bean class:
A class is said to be java bean class if it supports following four properties.
1)A class must be public.
2)A class must have constructor.
3)All variables should be private.
4)All variables should have setter and getter method.
Note:
Every Java bean class is a pojo class.
But every pojo class is not a java bean class.

## Question 5: Differences between interface and abstract class?

Interface Abstract class
To declare interface we will use To declare abstract class we will use
interface keyword. abstract keyword.
Interface is a collection of abstract Abstract class is a collection of abstract
methods,default methods and static methods and concrete methods.
methods.
Interface contains constants. Abstract class contains instance variables.
We can achieve multiple inheritance. We can't achieve multiple inheritence.
It does not support constructor. It supports constructor.
It does not support blocks. It supports blocks.
To write the implementation of To write the implementation of abstract
abstract methods we need to use methods we need to use sub class.
implementation class.
If we know only specification then If we know partial implementation then
we need to use interface. we need to use abstract class.
