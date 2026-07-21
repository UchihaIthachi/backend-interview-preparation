# Basic Questions

## Question 1: What is exception handling? How is it implemented in Java?

Exception handling manages runtime errors using try, catch, throw, throws, and 
finally.

## Question 2: Differentiate between throw and throws keywords.

• throw: Used to explicitly throw an exception. 
• throws: Declares exceptions a method might throw.

## Question 3: What are checked and unchecked exceptions? Give examples.

• Checked: Checked at compile-time (e.g., IOException). 
• Unchecked: Occur at runtime (e.g., NullPointerException).

## Question 4: Explain differ b/w final, finally, and finalize in java ?

1.
final is a keyword used in Java to restrict the modification of a variable, method, or class.
2.
finally is a block used in Java to ensure that a section of code is always executed, even if an exception isthrown.
3.
finalize is a method in Java used to perform cleanup processing on an object before it is garbage collected.

## Question 5: What is Exception handling in java ?

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

## Question 6: What is the purpose of throw and throws keyword in java ?

throw Keyword The throw keyword is used to explicitly throw an exception from a method or any block of code. We can throw either checked or unchecked exceptions using the throw keyword. The throw keyword is followed by an instance of the exception. Syntax: throw exception_instance; public class ThrowExample { private int age; public void setAge(int age) { if (age < 0) { throw new IllegalArgumentException("Age cannot be negative!"); } this.age = age; } public static void main(String[] args) { ThrowExample person = new ThrowExample(); try { person.setAge(-5); // This will cause an exception } catch (IllegalArgumentException e) { System.out.println("Error: " + e.getMessage()); } } } Output: Error: Age cannot be negative! In the setAge method, if the age provided is negative, we throwan IllegalArgumentException with a relevant message. Throws keyword The throws keyword is used to declare exceptions. It doesn’t throw an exception but specifies that a method might throw exceptions. It's typically used to inform callers of the exceptions they might encounter. Syntax: return_type method_name() throws exception_class_name{ //method code } Example:
public class ExceptionHandlingWorks { public static void main(String[] args) { exceptionHandler(); } private static void exceptionWithoutHandler() throws IOException { try (BufferedReader reader = new BufferedReader(new FileReader(new File("/invalid/file/location")))) { int c; // Read and display the file. while ((c = reader.read()) != -1) { System.out.println((char) c); } } } private static void exceptionWithoutHandler1() throws IOException { exceptionWithoutHandler(); } private static void exceptionWithoutHandler2() throws IOException { exceptionWithoutHandler1(); } private static void exceptionHandler() { try { exceptionWithoutHandler2(); } catch (IOException e) { System.out.println("IOException caught!"); } } }

## Question 7: Explain the try-with-resources statement.

Manages resources (like files) automatically, ensuring they are closed after use. 
Example: 
java 

try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) { 
// Read file 
}

## Question 8: What is the purpose of the try-with-resources statement in java ?

In Java, the try-with-resources statement is a try statement that declares one or more resources. The resource is as an object that must be closed after finishing the program. The try-with-resources statement ensures that each resource is closed at the end of the statement execution.
try (BufferedReader br = new BufferedReader(new FileReader("myfile.txt"))) {
String line;
while ((line = br.readLine()) != null) {
System.out.println(line);
}
}

## Question 9: Checked vs unchecked exceptions?

No answer provided yet.

## Question 10: Why prefer try-with-resources over manual `finally` blocks?

No answer provided yet.

## Question 11: What is the purpose of entrySet() method in java Hashmap ?

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

## Question 12: What causes `StackOverflowError`?

No answer provided yet.

## Question 13: How do you handle exceptions globally in a Spring app?

No answer provided yet.

## Question 14: What is Exception?

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

## Question 15: What is try block?

It is a block which contains risky code.
A try block always associate with catch block.
A try block is used to throw the exception to catch block.
If any exception raised in try block then try block won't be executed.
If any exception raised in the middle of the try block then reset of code won't be executed.

## Question 16: what is catch block?

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

## Question 17: What is finally block?

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

## Question 18: What is throw statement?

Sometimes we will create exception object explicitly and handover to JVM manually by using throw statement.

## Question 19: What is throws statement?

If any checked exception raised in our program we must and should handle that exception by using try and catch block or by using throws statement.

