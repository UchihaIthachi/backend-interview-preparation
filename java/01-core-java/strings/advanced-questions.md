# Advanced Questions

## Question 1: What is the difference between String, StringBuilder, and StringBuffer?

• String: Immutable.
• StringBuilder: Mutable, non-thread-safe.
• StringBuffer: Mutable, thread-safe.

## Question 2: What is difference b/w String, StringBuilder, and StringBuffer in java ?

String :
➢
String is immutable, meaning that it cannot be changed after it is created. This is because String objects are stored in a String pool, which is a shared area of memory that is used to store all String objects in a Java program.
StringBuilder :
➢
StringBuilder is a mutable class, meaning that it can be changed after it is created. This is because StringBuilder objects are not stored in the String pool.
➢
StringBuilder is also thread-safe, meaning that it can be safely used by multiple threads at the same time. This is because StringBuilder methods are synchronized.
StringBuffer :
➢
StringBuffer is also a mutable class, meaning that it can be changed after it is created. This is because StringBuffer objects are not stored in the String pool.
➢
StringBuffer is also thread-safe, meaning that it can be safely used by multiple threads at the same time. This is because StringBuffer methods are synchronized.

## Question 3: Difference between StringBuffer and StringBuilder?

StringBuffer StringBuilder
All the methods present in All the methods present in StringBuilder are
StringBuffe are synchronized. not synchronized.
At a time only one thread Multiple threads are allowed to access
is allowed to access Hence we can StringBuilder object.
StringBuffer object. achieve thread safety.
Hence we can't achieve
achieve thread safety.
Waiting time of a thread will There is no waiting thread effectively
increase effectively performance is high.
performance is low.
StringBuffer introduced in 1.0v. StringBuilder introduced in 1.5v.
