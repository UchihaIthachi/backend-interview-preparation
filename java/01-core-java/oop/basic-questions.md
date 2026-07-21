# Basic Questions

## Question 1: What are the main principles of Object-Oriented Programming (OOP)?

1. Encapsulation: Wrapping data and methods in a single unit (class). 
2. Abstraction: Hiding implementation details and showing only the functionality. 
3. Inheritance: Allowing a class to inherit properties and methods from another class. 
4. Polymorphism: Using a single interface to represent different forms (overloading and 
overriding).

## Question 2: What are constructors in Java? How are they different from methods?

• Constructors: Special methods to initialize objects. 
• Name matches the class. 
• No return type. 
• Difference from methods: Methods perform actions; constructors initialize objects.

## Question 3: Explain method overloading and method overriding with examples.

• Overloading: Same method name, different parameters (compile-time 
polymorphism). 
java 

class Example { 
void display(int a) { } 
void display(String b) { } 
} 
• Overriding: Subclass provides a new implementation for a method in the superclass 
(runtime polymorphism). 
java 

class Parent { 
void display() { } 
} 
class Child extends Parent { 
@Override 
void display() { } 
}

## Question 4: What is inheritance in Java? Discuss its types.

Inheritance allows a class to acquire the properties and methods of another class using the 
extends keyword. Types: 
1. Single: One class inherits from another. 
2. Multilevel: A chain of inheritance. 
3. Hierarchical: Multiple classes inherit from one superclass. 
4. Multiple (via interfaces): A class implements multiple interfaces.

## Question 5: Define polymorphism and its types in Java.

Polymorphism allows methods to perform different tasks based on the object. Types: 
1. Compile-time (Method Overloading). 
2. Runtime (Method Overriding).

## Question 6: What is encapsulation? How is it implemented in Java?

Encapsulation is bundling data (variables) and methods into a single unit (class). It's 
implemented using: 
1. Private access modifiers for fields. 
2. Public getter and setter methods for access.

## Question 7: What are inner classes? Differentiate between static and non-static inner classes.

Classes defined within another class. Types: static, non-static, local, and anonymous.

## Question 8: What is an object-oriented programming language?

Object-oriented programming (OOP) is a programming language that uses objects to bind data and the functions that operate on it. The goal of OOP is to implement real-world entities like inheritance, hiding, and polymorphism. OOP concepts include:
❖
Class
❖
Object
❖
Inheritance
❖
polymorphism
❖
encapsulation
❖
abstraction

## Question 9: What are the pillars of oops ?

The four pillars of OOPS (object-oriented programming) are Inheritance, Polymorphism, Encapsulation and Data Abstraction.

## Question 10: What is an class in java ?

➢
A class in Java is a set of objects which shares common characteristics/ behavior and common properties/attributes.
➢
It is a user-defined blueprint or prototype from which objects are created.
➢
For example, Student is a class while a particular student named Ravi is an object

## Question 11: What is an object in java ?

➢
An object in Java is a basic unit of Object-Oriented Programming (OOP) and represents real-life entities.
➢
Objects are the instances of a class that are created to use the attributes and methods of a class.
➢
Java objects are very similar to the objects we can observe in the real world. A cat, a lighter, a pen, or a car areall objects.

## Question 12: How do you create an object in java ?

To create an object in Java, you use the new keyword, followed by the class name and parentheses. For example, to create an object of the class MyClass, you would write:
MyClass myObject = new MyClass();
Different ways to create objects in Java
❖
Using new keyword.
❖
Using new instance.
❖
Using clone() method.
❖
Using deserialization.
❖
Using newInstance() method of Constructor class.

## Question 13: What is differ b/w class and object in java ?

Class
•
Class is a user-defined datatype that has its own data members and member functions.
•
Class is a blueprint or prototype from which objects are created.
•
It is a logical entity.
•
It does not occupy any memory space.
•
Class is declared using the class keyword.
Object
•
Object is an instance of a class.
•
Object is a real-world entity such as book, car, etc.
•
It is a physical entity.
•
It occupies memory space.
•
Object is created using the new keyword.

## Question 14: Explain the concept of inheritance ?

➢
Inheritance is a mechanism in object-oriented programming that allows a class to inherit the properties andbehaviors of another class.
➢
In Java, inheritance is implemented using the extends keyword. When a class inherits from another class, it iscalled a subclass or child class, and the class it inherits from is called a superclass or parent class. The subclassinherits all of the public and protected members of the superclass, including its fields, methods, andconstructors.
Eg :
class Animal {
String name;
int age;
void eat() {
System.out.println("Animal is eating");
}
}
class Dog extends Animal {
String breed;
void bark() {
System.out.println("Dog is barking");
}
}
public class Main {
public static void main(String[] args) {
Dog dog = new Dog();
dog.name = "Fido";
dog.age = 5;
dog.breed = "Labrador";
dog.eat(); // Prints "Animal is eating"
dog.bark(); // Prints "Dog is barking"
}
}

## Question 15: What is the purpose of super keyword ?

The super keyword can be used to call the superclass constructor from the subclass constructor. This is done by using the super() keyword followed by the arguments to the superclass constructor.
For example,
class Superclass {
public Superclass(int x) {
System.out.println("Superclass constructor called with x = " + x);
}
}
class Subclass extends Superclass {
public Subclass(int x, int y) {
super(x); // Call the superclass constructor with the value of x
System.out.println("Subclass constructor called with y = " + y);
}
}
public class Main {
public static void main(String[] args) {
Subclass subclass = new Subclass(10, 20);
}
}
OUTPUT :
Superclass constructor called with x = 10
Subclass constructor called with y = 20

## Question 16: What is Polymorphism in java ?

➢
Polymorphism is derived from two Greek words, “poly” and “morph”, which mean “many” and “forms”,respectively.
➢
Hence, polymorphism meaning in Java refers to the ability of objects to take on many forms.
➢
In other words, it allows different objects to respond to the same message or method call in multiple ways.
For Example:
public class Animal {
public void makeSound() {
System.out.println("Animal sound");
}
}
public class Dog extends Animal {
@Override
public void makeSound() {
System.out.println("Woof!");
}
}
public class Cat extends Animal {
@Override
public void makeSound() {
System.out.println("Meow!");
}
}
public class Main {
public static void main(String[] args) {
Animal animal = new Animal();
animal.makeSound(); // Prints "Animal sound"
Dog dog = new Dog();
dog.makeSound(); // Prints "Woof!"
Cat cat = new Cat();
cat.makeSound(); // Prints "Meow!"
}
}

## Question 17: Explain the difer b/w method overloading & method overriding in java ?

Method overloading is when a class has two or more methods with the same name but different parameters. This allows us to have multiple methods with the same name that perform different tasks, depending on the arguments passed to them.
For example, we could have a calculateArea() method that takes a single argument (the radius of a circle) and returns the area of the circle, and we could also have a calculateArea() method that takes two arguments (the length and width of a rectangle) and returns the area of the rectangle.
Method overriding is when a subclass provides its own implementation of a method that is already defined in the superclass. This allows us to customize the behavior of a method in a subclass, without having to break the code that relies on the superclass method.
For example, we could have a draw() method in a Shape class that simply prints the message "Drawing a shape". We could then override the draw() method in a Circle subclass to print the message "Drawing a circle". Feature Method overloading Method overriding Number of methods Two or more methods with the same name One method in the subclass and one method in the superclass Parameters Different parameters Same parameters Return type Same or different return type Same return type Inheritance Not required Required Purpose To provide multiple methods with the same name that perform different tasks To customize the behavior of a method in a subclass

## Question 18: What is Encapsulation in java ?

Encapsulation in Java is the process by which data (variables) and the code that acts upon them (methods) are integrated as a single unit. By encapsulating a class's variables, other classes cannot access them, and only the methods of the class can access them.
public class Person {
private String name;
private int age;
// getters and setters
public String getName() {
return name;
}
public void setName(String name) {
this.name = name;
}
public int getAge() {
return age;
}
public void setAge(int age) {
this.age = age;
}
}
In this example, the data members name and age are declared as private, and public getter and setter methods are provided to access and modify them. This way, the internal implementation of the Person class is hidden from the outside world, and the data members can only be accessed or modified through the public methods.

## Question 19: What is abstract class in java ?

Abstract class: is a restricted class that cannot be used to create objects (to access it, it must be inherited from another class). Abstract method: can only be used in an abstract class, and it does not have a body. The body is provided by the subclass (inherited from).
abstract class Shape {
// abstract method
abstract void draw();
}
// subclass of Shape
class Circle extends Shape {
// overriding the draw() method
@Override
void draw() {
System.out.println("Drawing a circle");
}
}
// subclass of Shape
class Rectangle extends Shape {
// overriding the draw() method
@Override
void draw() {
System.out.println("Drawing a rectangle");
}
}
// main class
public class Main {
public static void main(String[] args) {
// creating an object of Circle class
Circle circle = new Circle();
// calling the draw() method on circle object
circle.draw();
// creating an object of Rectangle class
Rectangle rectangle = new Rectangle();
// calling the draw() method on rectangle object
rectangle.draw();
}
}
Output:
Drawing a circle
Drawing a rectangle

## Question 20: How do you achieve abstraction in java ?

In Java, abstraction is achieved by interfaces and abstract classes. We can achieve 100% abstraction using interfaces. Data Abstraction may also be defined as the process of identifying only the required characteristics of an object ignoring the irrelevant details.
// Java Program to implement
// Java Abstraction
// Abstract Class declared
abstract class Animal {
private String name;
public Animal(String name) { this.name = name; }
public abstract void makeSound();
public String getName() { return name; }
}
// Abstracted class
class Dog extends Animal {
public Dog(String name) { super(name); }
public void makeSound()
{
System.out.println(getName() + " barks");
}
}
// Abstracted class
class Cat extends Animal {
public Cat(String name) { super(name); }
public void makeSound()
{
System.out.println(getName() + " meows");
}
}
// Driver Class
public class AbstractionExample {
// Main Function
public static void main(String[] args)
{
Animal myDog = new Dog("Buddy");
Animal myCat = new Cat("Fluffy");
myDog.makeSound();
myCat.makeSound();
}
}
OUTPUT :
Buddy barks
Fluffy meows

## Question 21: What is differ b/w default & parameterized constructor in java ?

Default Constructor:
•
A default constructor is a constructor that has no parameters.
•
It is implicitly provided by the compiler if no constructor is explicitly defined in a class.
•
A default constructor initializes all instance variables to their default values.
class Student {
int id;
String name;
// Default constructor
Student() {
id = 0;
name = "null";
}
}
To create an instance of the Student class using the default constructor, you would simply write:
Student student = new Student();
Parameterized Constructor :
•
A parameterized constructor is a constructor that has one or more parameters.
•
It is explicitly defined by the programmer.
•
A parameterized constructor allows you to initialize instance variables to specific values at the time of objectcreation.
class Student {
int id;
String name;
// Parameterized constructor
Student(int id, String name) {
this.id = id;
this.name = name;
}
}
To create an instance of the Student class using the parameterized constructor, you would write:
Student student = new Student(1, "John Doe");

## Question 22: What is method overloading in java ?

Method overloading in Java means having two or more methods (or functions) in a class with the same name and different arguments (or parameters). It can be with a different number of arguments or different data types of arguments.
class Adder{
static int add(int a, int b){return a+b;}
static double add(double a, double b){return a+b;}
}
class TestOverloading2{
public static void main(String[] args){
System.out.println(Adder.add(11,11));
System.out.println(Adder.add(12.3,12.6));
}}
Output:
24.9

## Question 23: What is java interface ?

An interface is an abstract type that is used to declare a behavior that classes must implement. They are similar to protocols in other languages.
Here are some of the key features of interfaces in Java:
❖
Interfaces can contain only abstract methods.
❖
Interfaces cannot contain concrete methods.
❖
Interfaces cannot contain instance variables.
❖
Interfaces can be used to achieve multiple inheritance.
❖
Interfaces can be extended by other interfaces.
❖
Interfaces can be implemented by classes.
class Dog implements Animal {
@Override
public void animalSound() {
// The body of animalSound() is provided here
}
@Override
public void run() {
// The body of run() is provided here
}
}

## Question 24: Explain the differ b/w abstract classes and interfaces in java ?Feature Abstract Class Interface Definition A class that is declared abstract cannot be instantiated, but it can be subclassed. An interface is a reference type that is used to declare a set of abstract methods. Methods Abstract classes can have both abstract and non-abstract methods. Interfaces can only have abstract methods. Variables Abstract classes can have variables. Interfaces cannot have variables. Inheritance Abstract classes can only be extended by one class. Interfaces can be implemented by multiple classes. Implementation Abstract classes can provide default implementations for abstract methods. Interfaces cannot provide implementations for abstract methods.

## Question 25: Can you implement multiple interfaces in java ?

our class can implement more than one interface, so the implements keyword is followed by a comma-separated list of the interfaces implemented by the class. By convention, the implements clause follows the extends clause, if there is one.

## Question 26: What is a java Abstract class ?

Abstract class: is a restricted class that cannot be used to create objects (to access it, it must be inherited from another class). Abstract method: can only be used in an abstract class, and it does not have a body.

## Question 27: What is the purpose of the getClass() method in java ?

getClass() in Java is a method of the Object class present in java. lang package. getClass() returns the runtime class of the object "this". This returned class object is locked by static synchronized method of the represented class.
Object obj = new String("Hello, world!");
Class c = obj.getClass();
System.out.println(c.getName());

## Question 28: Explain try, catch and finally blocks in java ? 1. try Block Enclose the code that might throw an exception within a try block. If an exception occurs within the try block, that exception is handled by an exception handler associated with it. The try block contains at least one catch block or finally block. The syntax of the try-catch block: try{ //code that may throw exception }catch(Exception_class_Name ref){} The syntax of a try-finally block: try{ //code that may throw exception }finally{} 2. catch Block Java catch block is used to handle the Exception. It must be used after the try block only. You can use multiple catch blocks with a single try. Syntax: try { //code that cause exception; } catch(Exception_type e) { //exception handling code } 3 finally Block

• Java finally block is a block that is used to execute important code such as closing connection, stream, etc.
• Java finally block is always executed whether an exception is handled or not.
• Java finally block follows try or catch block.
• For each try block, there can be zero or more catch blocks, but only one finally block.
• The finally block will not be executed if the program exits(either by calling System.exit() or by causing a fatal error that causes the process to abort). Syntax: try { // Code that might throw an exception } catch (ExceptionType1 e1) { // Code to handle ExceptionType1 } catch (ExceptionType2 e2) { // Code to handle ExceptionType2 } // ... more catch blocks if necessary ... finally { // Code to be executed always, whether an exception occurred or not } Example 1: In this example, we have used FileInputStream to read the simple.txt file. After reading a file the resource FileInputStream should be closed by using finally block. public class FileInputStreamExample { public static void main(String[] args) { FileInputStream fis = null; try { File file = new File("sample.txt"); fis = new FileInputStream(file); int content; while ((content = fis.read()) != -1) { // convert to char and display it System.out.print((char) content); } } catch (IOException e) { e.printStackTrace(); } finally { if (fis != null) { try { fis.close(); } catch (IOException e) { // TODO Auto-generated catch block e.printStackTrace(); } } } } }

## Question 29: What is the purpose of import statement in java ?An import statement tells Java which class you mean when you use a short name (like List ). It tells Java where to find the definition of that class. You can import just the classes you need from a package as shown below. Just provide an import statement for each class that you want to use. Import statement in Java is helpful to take a class or all classes visible for a program specified under a package, with the help of a single statement.

import java.util.List;
import java.util.*;
import static java.lang.Math.PI;

## Question 30: Interface vs abstract class?

No answer provided yet.

## Question 31: Overloading vs overriding?

No answer provided yet.

## Question 32: Composition vs inheritance?

No answer provided yet.

## Question 33: What happens when a mutable object is used as a `HashMap` key?

No answer provided yet.

## Question 34: What is a record and when would you use one over a class?

No answer provided yet.

## Question 35: What are sealed classes and what problem do they solve?

No answer provided yet.

## Question 36: What is the Java Collections Framework? Name its main interfaces.

A unified architecture for storing and manipulating groups of objects, including interfaces 
like List, Set, and Map.

## Question 37: What is the purpose of iterator interface in java ?

Java Iterator Interface of java collections allows us to access elements of the collection and is used to iterate over the elements in the collection(Map, List or Set). It helps to easily retrieve the elements of a collection and perform operations on each element.
The Iterator interface has three methods:
❖
hasNext() - Returns true if the iterator has more elements.
❖
next() - Returns the next element in the iteration.
❖
remove() - Removes the last element returned by the next() method from the collection.
Eg :
import java.util.ArrayList;
import java.util.Iterator;
public class Main {
public static void main(String[] args) {
ArrayList<String> names = new ArrayList<>();
names.add("John");
names.add("Doe");
names.add("Jane");
Iterator<String> iterator = names.iterator();
while (iterator.hasNext()) {
String name = iterator.next();
System.out.println(name);
}
}
}
Output :
John
Doe
Jane

## Question 38: What is the purpose of the Optional class?

Optional prevents NullPointerException by representing optional values.

## Question 39: What are functional interfaces? Provide examples.

Interfaces with a single abstract method, e.g., Runnable.

## Question 40: Discuss the role of the default keyword in interfaces.

Allows adding methods to interfaces without breaking existing implementations.

## Question 41: When should a normal loop be preferred over a stream?

No answer provided yet.

## Question 42: What happens after `javac` compiles a `.java` file — walk through class loading?

No answer provided yet.

## Question 43: What is the class-loading lifecycle (loading, linking, initialization)?

No answer provided yet.

## Question 44: How are Spring beans created (lifecycle)?

No answer provided yet.

## Question 45: `@Component` vs `@Bean`?

No answer provided yet.

## Question 46: Why prefer constructor injection?

No answer provided yet.

## Question 47: Types of classloaders in java?

We have three types of predefined classloaders.
1)Bootstrap classloader (loads rt.jar file)
2)Extension classloader (loads all the jar files from ext folder)
3)System/Application classloader(it loads .class file from classpath).

## Question 48: What .class file contains ?

A .class file contains byte code instructions.

## Question 49: What is class?

A class is a blue print of an object.
A class is a collection of variables and methods.
A class is a reusable component.
A class will accept following modifiers.
ex:
default, public, final, abstract
We can declare a class as follow.
syntax:
optional
|
modifier class class_name <extends> Parent_classname
<implements> Interface_name
{
- // variables and methods
}
2)What is the difference between default class and public class?
default class
We can access default class within the package.
ex: class A
{
}
public class
We can access public class within the package and outside of the package.
ex: public class A
{
}
What is final class?
If we declare any class as final then creating child class is not possible.
If we declare any class as final then extending some other class is not possible.
ex:
final class A
{
}
class B extends A // invalid
{
}
What is abstract class ?
If we declare any class as abstract then creating object of that class is not possible.
ex: abstract class A
{
}

## Question 50: What is object ?

It is a outcome of a blue print.
It is a instance of a class.
Instance means allocating memory for our data members.
Object class
Object class present in java.lang package.
Object class consider as a parent class for every java program.
Object class contains following methods.
toString()
It is a method present in Object class.
Whenever we are trying to display any object reference directly or indirectly toString()
method will be executed.
Data Hiding
It is used to hide the data from the outsider.
It means outside person must not access our data directly.
Using private modifier we can implement data hiding concept.
ex: class Account
{
private double balance;
}

## Question 51: Types of objects?

We have two types of objects.
1)Immutable object
After object creation if we perform any changes then for every change a new object
will be created such behaviour is called immutable object.
ex: String
Wrapper classes.
2)Mutable object
After object creation if we perform any changes then all the changes will reflect to single
object such behaviour is called mutable object.
ex: StringBuffer
StringBuilder

## Question 52: What is singleton class?

A class which allows us to create only one object is called singleton class.
To declare a singleton class we required private constructor and static method.
ex: class Singleton
{
private static Singleton singleton=null;
private Singleton()
{
}
public static Singleton getInstance()
{
if(singleton==null)
{
singleton=new Singletone();
}
return singleton;
}
}

## Question 53: What is Interfaces?

Interface is a collection of zero or more abstract methods.
Abstract method is a incomplete method which ends with semicolon and does not have any body.
ex:
public abstract void m1();
It is not possible to create object for interfaces.
To write the implementation of abstract methods of an interface we will use implementation class.
It is possible to create object for implementation class because it contains method with body.
By default , every abstract method is a public and abstract.
Interface contains only constants i.e public static final.
Syntax: interface interface_name
{
- //constants
- //abstract method
}
In java, a class can't extends more then one class.
But interface can extends more then one interface.

## Question 54: What is Abstract class?

Abstract class is a collection of zero or more abstract methods and zero or more concrete methods.
A "abstract" keyword is applicable only for class and method but not for variable.
It is not possible to create object for abstract class.
To write the implementation of abstract methods of an abstract class we will use sub classes.
By default every abstract method is a public and abstract.
Abstract class contains only instance variables.
syntax:
abstract class class_name
{ - //instance variables
- //abstract methods
- //concrete methods
}

## Question 55: What is Abstraction?

Hiding the internal implementation and high lighting the set of services is called abstraction.
Best example of abstraction is ATM machine, coffee machine,calcular, phone and etc.
The main advantages of abstraction are.
1)It gives security because it will hide internal implementation from the outsider.

## Question 56: What is Encapsulation?

The process of encapsulating or grouping variables and it's associate methods in a
single entity is called encapsulation.
In encapsulation for every variable we need to declare setter and getter methods.
A class is said to be encapsulated class if it supports data hiding + abstraction.
The main advantages of encapsulation are.
1)It gives security.
2)Enhancement becomes more easy.
3)It provides flexibility to the enduser to use the system.
4)It improves maintainability of an application.
The main disadvantage of encpasulation is ,it will increase the length of our code and
slow down the execution process.
ex: class Employee
{
private int empId;
//setter
public void setEmpId(int empId)
{
this.empId=empId;
}
//getter
public int getEmpId()
{
return empId;}
}

## Question 57: What is Constructor?

Constructor is a special method which is used to initialize an object.
ex: Test t=new Test();
Having same name as class name is called constructor.
A constructor will execute when we create an object.
A constructor does not allowed any returntype.
A constructor will accept following modifiers.
ex:default, public, private , protected
ex:class A
{
A()
{
System.out.println("0-arg const");
}
}
ex: class A
{
A(int i)
{
System.out.println("parameterized const");
}
}
1)Userdefined constructor
A constructor which is created by the user based on the application requirement
is called userdefined constructor.
It is categories into two types.
i)Zero Argument constructor
ii) Parameterized constructor
i)Zero Argument constructor
Suppose if we are not passing atleast one argument to userdefined constructor is
called zero argument constructor.
2) Parameterized constructor
Suppose if we are passing atleast one argument to userdefined constructor is
called parameterized argument constructor.
2)What is default constructor?
It is a compiler generated constructor for every java program where we are not
defining any constructor.
Default constructor is a empty implementation.
To see the default constructor we need to use below command.
ex: javap -c Test

## Question 58: What is Constructor Overloading:

Having same constructor name with difference parameters in a single class is
called constructor overloading.

## Question 59: What is Method Overloading?

Having same method name with different parameters in a single class is called
method overloading.
All the methods present in a class are called overloaded methods.
Method resolution will taken care by compiler based on reference type.
ex: class A
{
public void m1()
{
System.out.println("0-arg method");
}
public void m1(int i)
{
System.out.println("int-arg method");
}
}

## Question 60: Can we overload main method in java?

Yes, we can overload main method in java.But JVM always execute main method
with String[] parameter only.

## Question 61: Can we override final methods in java?

ans) No , we can't override final methods in java.

## Question 62: Can we override static methods in java?

No , we can't override static methods in java.

## Question 63: Can we override main method in java?

No , we can't override main method because it is static.

## Question 64: What is this keyword?

A "this" keyword is a java keyword which is used to refer current class
object reference.
We can utility this keyword in following ways.
i)To refer current class variables
ii)To refer current class methods
iii)To refer current class constructors

## Question 65: what is Super Keyword?

A "super" keyword is a java keyword which is used to refer super class object
reference.
We can utility super keyword in following ways.
i)To refer super class variables
ii)To refer super class methods
iii)To refer super class constructors

## Question 66: What is polymorphism?

Polymorphism has taken from Greek Word.
Here poly means many and morphism means forms.
The ability to represent in different forms is called polymorphism.
In java, polymorphism is categories into two types.
1)Compile time polymorphism/ static polymorphism / early binding.
A polymorphism which exhibits at compile time is called compile time polymorphism.
ex: Method overloading
Method Hiding
2)Runtime polymorphism / dynamic polymorphism / late binding.
A polymorphism which exhibits at runtime is called run time polymorphism.
ex: Method overriding

## Question 67: What is Inheritance?

Inheritance is a mechanism where one class will derived from the properties from
another class.
Using "extends" keyword we can implements inheritance.
We have five types of inheritance.
1) Single level inheritance.
2) Multi level inheritance
3) Multiple inheritance
4) Hierarchical inheritance
5) Hybrid inheritance

## Question 68: What are the inheritance not support by java?

Java does not support multiple inheritance and hybrid inheritance.

## Question 69: Why java does not support multiple inheritance?

There is a chance of raising ambiguity problem that's why java does not
support multiple inheritance.
Ex:
P1.m1() p2.m1()
|-----------------------------|
|
C.m1();
But from java 1.8 version onwards java supports multiple inheritance through
default methods of interface.

## Question 70: What is marker interface?

An interface which does not have any constants and abstract methods is called marker interface.
In general, empty interface is called marker interface.
Marker interfaces are empty interfaces but by using those interfaces we get some ability to do.
ex: Serializable
Remote

## Question 71: What is inner class?

Sometimes we will declare a class inside another class such concept
is called inner class.
ex:class Outer_Class
{
class Inner_Class
{
- //code to be execute
}
}
Inner classes introduced as a part of event handling to remove GUI bugs.
But because of powerful features and benefits of inner classes, Programmers started
to use inner classes in regular programming code.
Inner class does not accept static members i.e static variable, static method and
static block.

## Question 72: what is Wrapper classes?

The main objective of wrapper class is used to wrap primitive to wrapper object
and vice versa.
To defined serveral utility methods.
Primitive type Wrapper class
byte Byte
short Short
int Integer
long Long
float Float
double Double
boolean Boolean
char Character
constructors
Every wrapper contains following two constructors.One will take corresponding primitive as an argument and another will take corresponding String as an argument.
wrapper class constructor
Byte byte or String
Short short or String
Integer int or String
Long long or String
Float float or String
Double double or String
Boolean boolean or String
Character char

## Question 73: Types of ResultSet objects?

We have two types of ResultSet objects in jdbc.
1)Non-Scrollable ResultSet object
2)Scrollable ResultSet object
1)Non-Scrollable ResultSet object
By default every ResultSet object is a non-scrollable ResultSet object.
It allows us to read the records sequentially or uni-directionally such type of ResultSet object is called non-scrollable ResultSet object.
2)Scrollable ResultSet object
It allows us to read the records non-sequentially , bi-directionally or randomly such type of ResultSet object is called scrollable ResultSet object.

## Question 74: Types of Jsp Implicit Objects

There are 9 implicit objects present in jsp.
Implicit objects are created by the web container that is available for every jsp program.
Object which can be used directly without any configuration is called implicit object.
The list of implicit objects are.
Object Type
out JspWriter
request HttpServletRequest
response HttpServletRespons
config ServletConfig
application ServletContext
session HttpSession
pageContext pageContext
page Object
exception Throwable



Interfaces & OOP:
10. Is it possible to write private methods in an interface?
11. How many types of methods can we write in an interface?
12. What happens if we write two identical default methods in two different interfaces and try to implement them in a class? How to fix the issue?
13. Explain the SOLID design principles.
14. What are Liskov's substitutional principles? If we can replace the parent class object with a subclass, then why do we need inheritance?
15. Explain and implement the Factory design pattern (with some modification).