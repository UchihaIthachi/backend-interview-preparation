# Exceptions Questions

### What is exception handling? How is it implemented in Java?

Exception handling manages runtime errors using try, catch, throw, throws, and 
finally.

### Differentiate between throw and throws keywords.

• throw: Used to explicitly throw an exception. 
• throws: Declares exceptions a method might throw.

### What are checked and unchecked exceptions? Give examples.

• Checked: Checked at compile-time (e.g., IOException). 
• Unchecked: Occur at runtime (e.g., NullPointerException).

### What is the difference between final, finally, and finalize()?

• final: Prevents modification of variables, methods, or classes. 
• finally: Ensures execution of code after a try-catch. 
• finalize(): Called by the garbage collector before destroying an object.

### Explain differ b/w final, finally, and finalize in java ?

1.
final is a keyword used in Java to restrict the modification of a variable, method, or class.
2.
finally is a block used in Java to ensure that a section of code is always executed, even if an exception isthrown.
3.
finalize is a method in Java used to perform cleanup processing on an object before it is garbage collected.

### What is Exception handling in java ?

The Java programming language uses exceptions to handle errors and other exceptional events. the process of responding to unwanted or unexpected events when a computer program runs. There are 5 keywords.
❖
Try
❖
Catch
❖
Throw
❖
Throws
❖
Finally

### What is the purpose of throw and throws keyword in java ?

throw Keyword The throw keyword is used to explicitly throw an exception from a method or any block of code. We can throw either checked or unchecked exceptions using the throw keyword. The throw keyword is followed by an instance of the exception. Syntax: throw exception_instance; public class ThrowExample { private int age; public void setAge(int age) { if (age < 0) { throw new IllegalArgumentException("Age cannot be negative!"); } this.age = age; } public static void main(String[] args) { ThrowExample person = new ThrowExample(); try { person.setAge(-5); // This will cause an exception } catch (IllegalArgumentException e) { System.out.println("Error: " + e.getMessage()); } } } Output: Error: Age cannot be negative! In the setAge method, if the age provided is negative, we throwan IllegalArgumentException with a relevant message. Throws keyword The throws keyword is used to declare exceptions. It doesn’t throw an exception but specifies that a method might throw exceptions. It's typically used to inform callers of the exceptions they might encounter. Syntax: return_type method_name() throws exception_class_name{ //method code } Example:
public class ExceptionHandlingWorks { public static void main(String[] args) { exceptionHandler(); } private static void exceptionWithoutHandler() throws IOException { try (BufferedReader reader = new BufferedReader(new FileReader(new File("/invalid/file/location")))) { int c; // Read and display the file. while ((c = reader.read()) != -1) { System.out.println((char) c); } } } private static void exceptionWithoutHandler1() throws IOException { exceptionWithoutHandler(); } private static void exceptionWithoutHandler2() throws IOException { exceptionWithoutHandler1(); } private static void exceptionHandler() { try { exceptionWithoutHandler2(); } catch (IOException e) { System.out.println("IOException caught!"); } } }

### What is difference between checked and un checked Exceptions in java ?

Checked Exceptions
•
They occur at compile time.
•
The compiler checks for a checked exception.
•
These exceptions can be handled at the compilation time.
•
It is a sub-class of the exception class.
•
The JVM requires that the exception be caught and handled.
•
Example of Checked exception- ‘File Not Found Exception’
Unchecked Exceptions
•
These exceptions occur at runtime.
•
The compiler doesn’t check for these kinds of exceptions.
•
These kinds of exceptions can’t be caught or handled during compilation time.
•
This is because the exceptions are generated due to the mistakes in the program.
•
These are not a part of the ‘Exception’ class since they are runtime exceptions.
•
The JVM doesn’t require the exception to be caught and handled.
•
Example of Unchecked Exceptions- ‘No Such Element Exception’

### Explain the try-with-resources statement.

Manages resources (like files) automatically, ensuring they are closed after use. 
Example: 
java 

try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) { 
// Read file 
}

### What is the purpose of the try-with-resources statement in java ?

In Java, the try-with-resources statement is a try statement that declares one or more resources. The resource is as an object that must be closed after finishing the program. The try-with-resources statement ensures that each resource is closed at the end of the statement execution.
try (BufferedReader br = new BufferedReader(new FileReader("myfile.txt"))) {
String line;
while ((line = br.readLine()) != null) {
System.out.println(line);
}
}

### Checked vs unchecked exceptions?

No answer provided yet.

### Why prefer try-with-resources over manual `finally` blocks?

No answer provided yet.

### What is the purpose of entrySet() method in java Hashmap ?

The Java HashMap. entrySet() method is used to convert the elements within a HashMap into a Set. This provides a convenient way to access and manipulate the elements stored in the HashMap.
The entrySet() method is useful for iterating over the entries in a map. For example, the following code iterates over the entries in a map and prints the keys and values:
HashMap<String, Integer> map = new HashMap<>();
map.put("one", 1);
map.put("two", 2);
for (Map.Entry<String, Integer> entry : map.entrySet()) {
System.out.println(entry.getKey() + " : " + entry.getValue());
}
The entrySet() method can also be used to remove entries from a map. For example, the following code removes the entry with the key "one" from the map:
map.entrySet().removeIf(entry -> entry.getKey().equals("one"));
The entrySet() method is a powerful tool for working with maps in Java. It can be used to iterate over the entries in a map, remove entries from a map, and more.

### What causes `StackOverflowError`?

No answer provided yet.

### What causes `OutOfMemoryError`?

No answer provided yet.

### How do you handle exceptions globally in a Spring app?

No answer provided yet.

### What is the difference between Exception and Error?

Exception
Exception is a problem for which we can provide solution programmatically.
Exception will raise due to syntax errors.
ex:
ArithmeticException
FileNotFoundException
IllegalArgumentException
Error:
Error is a problem for which we can't provide solution programmatically.
Error will raise due to lack of system resources.
ex:
OutOfMemoryError
StackOverFlowErrr
LinkageError and etc
As a part of application development it is a responsibility of a programmer to provide smooth termination for every java program.
We have two types of terminations.
1)Smooth termination / Graceful termination
2)Abnormal termination
1)Smooth termination
During the program execution suppose if we are getting any interruption in the middle of the program such type of termination is called smooth termination.
ex:
class Test
{
public static void main(String[] args)
{
System.out.println("Hello World");
}
}
2)Abnormal termination
During the program execution suppose if we are getting any interruption in the middle of the program such type of termination is called abnormal termination.
ex:
class Test
{
public static void main(String[] args)
{
System.out.println(10/0);
}
}

### What is Exception?

It is a unwanted, unexpected event which disturbs normal flow of our program.
Exception always raised at runtime so they are also known as runtime events.
The main objective of exception handling is to provide gaceful termination.
In java , exceptions are divided into two types.
1)Predefined exceptions
2)Userdefined exceptions
1)Predefined exceptions
Built-In exceptions are called predefined exceptions.
It is categories into two types.
i)Checked exception:
Exceptions which are checked by a compiler at the time of compilation
is called checked exception.
ex: IOException
InterruptedException
ii)Unchecked exceptions:
Exceptions which are checked by a JVM at the time of runtime is called
unchecked exceptions.
ex: ArithmeticException
IllegalArgumentException
ClassCastException and etc
If any checked exception raised in our program we must and should handle that exception by using try and catch block.
2)Userdefined exceptions
Exceptions which are created by the user based on the application requirement are called customized exceptions.
ex: StudentsNotPracticingException
NoInterestInJavaException
InsufficientFeeException

### What is try block?

It is a block which contains risky code.
A try block always associate with catch block.
A try block is used to throw the exception to catch block.
If any exception raised in try block then try block won't be executed.
If any exception raised in the middle of the try block then reset of code won't be executed.

### what is catch block?

It is a block which contains Error handling code.
A catch block always associate with try block
A catch block is used to catch the exception which is thrown by try block
A catch block will take exception name as a parameter and that name must match with exception class name.
If there is no exception in try block then catch block won't be executed.
Syntax:
try
{
- //Risky code
}
catch(ArithmeticException ae)
{
- //Error handling code
}
How to display exception details
Throwable class defines following three method to display exception details.
1)printStackTrace()
It will display name of the exception, description of the exception and line number of the exception.
2)toString()
It will display name of the exception and description of the exception.
3)getMessage()
It will display description of the exception.

### What is finally block?

It is never recommanded to maintain cleanup code inside try block because if any exception raised in try block then try won't be executed.
It is never recommanded to maintain cleanup code inside catch block because if no exception raised in try block then catch won't be executed.
But we need a place where we can maintain cleanup code and it should execute irrespective of exception raised or not.Such block is called finally block.
syntax
try
{
- // Risky Code
}
catch(Exception e)
{
- // Errorhandling code
}
finally
{
- //cleanup code
}

### What is the difference between final, finally and finalized method?

final
A final is a modifier which is applicable for variables,methods and classes.
If we declare any variable as final then reassignment of that variable is not possible.
If we declare any method as final then overriding of that method is not possible.
If we declare any class as final then creating child class is not possible.
finally
It is a block which contains cleanup code and it will execute irrespective of exception raised or not.
finalized method
It is a method called by garbage collector just before destroying an object for cleanup activity.

### What is throw statement?

Sometimes we will create exception object explicitly and handover to JVM manually by using throw statement.

### What is throws statement?

If any checked exception raised in our program we must and should handle that exception by using try and catch block or by using throws statement.

