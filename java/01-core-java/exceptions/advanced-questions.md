# Advanced Questions

## Question 1: What is the difference between final, finally, and finalize()?

• final: Prevents modification of variables, methods, or classes.
• finally: Ensures execution of code after a try-catch.
• finalize(): Called by the garbage collector before destroying an object.

## Question 2: What is difference between checked and un checked Exceptions in java ?

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

## Question 3: What causes `OutOfMemoryError`?

No answer provided yet.

## Question 4: What is the difference between Exception and Error?

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

## Question 5: What is the difference between final, finally and finalized method?

final
A final is a modifier which is applicable for variables,methods and classes.
If we declare any variable as final then reassignment of that variable is not possible.
If we declare any method as final then overriding of that method is not possible.
If we declare any class as final then creating child class is not possible.
finally
It is a block which contains cleanup code and it will execute irrespective of exception raised or not.
finalized method
It is a method called by garbage collector just before destroying an object for cleanup activity.
