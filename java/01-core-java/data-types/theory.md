# Java Data Types, Casting and Parameter Passing

## 1. Definition

Java has 8 primitive types (`byte`, `short`, `int`, `long`, `float`, `double`, `char`,
`boolean`) stored directly by value, and reference types (objects, arrays) where the
variable holds a reference to heap memory.

## 2. Why it exists

Primitives avoid the overhead of object allocation for simple values used constantly
(loop counters, arithmetic). Reference types allow complex, mutable, shared structures.

## 3. How it works — pass-by-value, always

Java is **always pass-by-value**. For primitives, the value itself is copied. For
objects, the *reference* (a pointer-like value) is copied — so the method gets its
own copy of the reference, but both copies point to the same object.

```java
void mutate(StringBuilder sb) {
    sb.append("changed");      // mutates the shared object -> visible outside
    sb = new StringBuilder("new"); // reassigns the LOCAL copy of the reference only
}
```

After calling `mutate(myBuilder)`, `myBuilder` contains `"changed"` but is NOT
`"new"` — the reassignment only affected the local copy of the reference.

## 4. Code example

```java
public class PassByValueDemo {
    static void increment(int x) { x++; }              // no effect outside
    static void appendTo(StringBuilder sb) { sb.append("!"); } // visible outside

    public static void main(String[] args) {
        int n = 5;
        increment(n);
        System.out.println(n); // 5

        StringBuilder sb = new StringBuilder("hi");
        appendTo(sb);
        System.out.println(sb); // hi!
    }
}
```

## 5. Production use case

Understanding this prevents bugs where developers assume passing an object into a
method and reassigning it inside will affect the caller — a very common source of
confusion when refactoring service methods that take DTOs as parameters.

## 6. Common mistakes

- Believing Java is pass-by-reference because objects appear "mutated" across calls.
- Reassigning a parameter inside a method and expecting the caller to see the change.
- Using mutable wrapper objects (`int[1]`) as a workaround instead of returning a value.

## 7. Trade-offs

| Primitives | Objects |
|---|---|
| Fast, no GC pressure | Flexible, can be null, can be shared |
| No behavior/methods | Support behavior + identity |

## 8. Interview questions

1. Is Java pass-by-value or pass-by-reference?
2. What actually gets copied when you pass an object to a method?
3. Why does mutating an object inside a method affect the caller, but reassigning it does not?
4. What's the difference between `==` and `.equals()` for objects?

## 9. Short interview answer

"Java is always pass-by-value. For objects, the value being copied is the reference
itself — so mutating the object through that reference is visible to the caller, but
reassigning the local reference variable is not, because that only changes the copy."

## 10. Related topics

- [Strings](strings.md)
- [equals() and hashCode()](../02-collections/hashmap-internals.md)
