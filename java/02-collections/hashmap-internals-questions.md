# Hashmap Internals Questions

### Explain the significance of the equals() and hashCode() methods.

• equals(): Checks logical equality. 
• hashCode(): Provides a unique hash for an object, used in hash-based collections 
like HashMap.

### Why must `equals()` and `hashCode()` agree?

No answer provided yet.

### What is hashcode?

For every object , JVM will create a unique identifier number i.e hash code.
In order to read hash code we need to use hashCode() method of Object class.
ex:
Test t=new Test();
System.out.println(t.hashCode());

### Difference between == and .equals() method

==
It is a equality operator or comparision operator.
It is used for reference comparision or address comparision.
.equals()
It is a method present in String class.
It is used for content comparision.
It is a case sensitive.

