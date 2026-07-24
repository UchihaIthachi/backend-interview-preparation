# Basic Questions

## Question 1: Explain the Singleton design pattern and its implementation

The **Singleton pattern** ensures that a class has only one instance within a particular class loader and provides a globally accessible method for obtaining that instance.

Typical use cases include:

- Shared configuration
- Application-wide registries
- Stateless utility services
- Resource managers
- In-memory caches

## Basic implementation

```java
public final class Singleton {

    private static Singleton instance;

    private Singleton() {
        // Prevent external object creation.
    }

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }

        return instance;
    }
}
```

The implementation uses:

- A `private` constructor to prevent external instantiation
- A `static` field to store the single instance
- A `static getInstance()` method to provide access

## Problem with this implementation

The basic lazy implementation is **not thread-safe**.

Two threads may execute the following condition at the same time:

```java
if (instance == null) {
```

Both threads could then create separate objects.

```text
Thread A sees instance == null
Thread B sees instance == null

Thread A creates Singleton
Thread B creates another Singleton
```

---

## Thread-safe implementation using eager initialization

```java
public final class Singleton {

    private static final Singleton INSTANCE = new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

### Advantages

- Simple
- Thread-safe through JVM class initialization
- No synchronization overhead

### Disadvantage

The instance is created when the class is initialized, even when it is never used.

---

## Initialization-on-demand holder idiom

This provides lazy initialization and thread safety without explicit synchronization.

```java
public final class Singleton {

    private Singleton() {
    }

    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

The nested `Holder` class is initialized only when `getInstance()` is first called. JVM class initialization guarantees thread safety.

This is generally one of the best implementations for a traditional Java Singleton.

---

## Double-checked locking

```java
public final class Singleton {

    private static volatile Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }

        return instance;
    }
}
```

The second null check ensures that only the first thread entering the synchronized block creates the object.

The `volatile` keyword is essential because it prevents unsafe publication and instruction reordering.

This implementation works, but it is more complicated than the holder idiom.

---

## Enum Singleton

```java
public enum Singleton {

    INSTANCE;

    public void performOperation() {
        System.out.println("Operation executed");
    }
}
```

Usage:

```java
Singleton.INSTANCE.performOperation();
```

### Advantages

- Thread-safe
- Serialization-safe
- Resistant to reflection-based constructor invocation
- Very concise

An enum is often considered the safest implementation when enum syntax is appropriate.

---

## Singleton in Spring

Spring beans use singleton scope by default:

```java
@Service
public class PaymentService {
}
```

This means Spring normally creates one instance of that bean definition per application context.

Therefore, manually adding a static Singleton implementation inside a Spring application is usually unnecessary.

However, a Spring singleton is not necessarily:

- One instance per JVM
- One instance across multiple application contexts
- One instance across multiple microservice replicas
- Automatically thread-safe

For example, three Kubernetes pods normally contain three separate Spring singleton instances.

## Interview-ready answer

> The Singleton pattern restricts a class to one instance and provides a global access method. It normally uses a private constructor, a static instance field, and a static `getInstance()` method. The basic lazy implementation is not thread-safe, so I prefer eager initialization, the initialization-on-demand holder idiom, or an enum Singleton. In Spring applications, beans are already singleton-scoped by default, so manually implementing a static Singleton is usually unnecessary.
