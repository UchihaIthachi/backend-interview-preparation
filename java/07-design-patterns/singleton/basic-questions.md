# Basic Questions

## Question 1: Explain the Singleton design pattern and its implementation.

Restricts a class to one instance and provides a global access point to it. 
java 

class Singleton { 
private static Singleton instance; 
private Singleton() { } 
public static Singleton getInstance() { 
if (instance == null) { 
instance = new Singleton(); 
} 
return instance; 
} 
}

