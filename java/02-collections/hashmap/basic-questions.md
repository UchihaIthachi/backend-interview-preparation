# Basic Questions

## Question 1: Explain the significance of the equals() and hashCode() methods.

• equals(): Checks logical equality.
• hashCode(): Provides a unique hash for an object, used in hash-based collections
like HashMap.

## Question 2: Why must `equals()` and `hashCode()` agree?

No answer provided yet.

## Question 3: What is hashcode?

For every object , JVM will create a unique identifier number i.e hash code.
In order to read hash code we need to use hashCode() method of Object class.
ex:
Test t=new Test();
System.out.println(t.hashCode());
