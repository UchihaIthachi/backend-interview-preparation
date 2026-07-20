# Advanced Questions

## Question 1: What is garbage collection in Java? How does it work?

Garbage collection automatically deallocates memory for objects no longer in use, reclaiming
memory in the heap.

## Question 2: Comparable vs Comparator — what's the difference and when do you use each?

No answer provided yet.

## Question 3: What is difference between a thread and a process in java ?

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

## Question 4: How does Java achieve memory management?

Java uses automatic garbage collection to manage memory. Objects are allocated in the
heap memory, and when they are no longer referenced, the garbage collector deallocates
them.

## Question 5: Describe the Java Memory Model (JMM).

Defines how threads interact through memory, ensuring visibility and ordering of variable
accesses.

## Question 6: How does Java handle memory leaks?

Java uses garbage collection but memory leaks can occur if references to unused objects are
maintained.

## Question 7: How would you investigate a memory leak?

No answer provided yet.

## Question 8: Differences betweend JDK , JRE and JVM ?

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

## Question 9: What is the difference b/w length and length() ?

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
