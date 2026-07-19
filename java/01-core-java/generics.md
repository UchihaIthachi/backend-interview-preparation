# Generics, Bounded Types and Wildcards

## 1. Definition

Generics let classes and methods operate on typed parameters (`List<T>`) while
retaining compile-time type safety, without needing to write a separate class
per type.

## 2. Why it exists

Before generics, collections stored `Object` and required manual casting, which
was unsafe and verbose. Generics push type errors to compile time instead of
runtime `ClassCastException`.

## 3. How it works — wildcards

```java
// Producer: read-only, use "extends" (PECS: Producer Extends)
void printAll(List<? extends Number> list) {
    for (Number n : list) System.out.println(n);
}

// Consumer: write-only, use "super" (PECS: Consumer Super)
void addIntegers(List<? super Integer> list) {
    list.add(1);
    list.add(2);
}
```

**Type erasure**: generic type information is removed at compile time — at
runtime, `List<String>` and `List<Integer>` are both just `List`. This is why you
can't do `new T()` or `array instanceof List<String>`.

## 4. Code example

```java
class Repository<T, ID> {
    private final Map<ID, T> store = new HashMap<>();

    void save(ID id, T entity) { store.put(id, entity); }
    Optional<T> findById(ID id) { return Optional.ofNullable(store.get(id)); }
}
```

## 5. Production use case

Generic repository/DAO base classes (`Repository<T, ID>`) are the foundation of
Spring Data JPA's `JpaRepository<T, ID>` — understanding generics is a prerequisite
for understanding how Spring Data works under the hood.

## 6. Common mistakes

- Confusing `? extends T` (read-only) with `? super T` (write-only).
- Trying to create generic arrays (`new T[10]` doesn't compile due to erasure).
- Not understanding why `List<Object>` is not a supertype of `List<String>`.

## 7. Trade-offs

Generics add compile-time safety at the cost of some API complexity (wildcards
can be confusing) and the limitations imposed by type erasure at runtime.

## 8. Interview questions

1. What is type erasure and what problems does it cause?
2. `? extends T` vs `? super T` — when do you use each (PECS)?
3. Why can't you create a generic array in Java?
4. How does Spring Data JPA use generics for its repository interfaces?

## 9. Short interview answer

"Generics give compile-time type safety without needing a class per type. I use
PECS to decide wildcards: extends for read-only producers, super for write-only
consumers. Because of type erasure, generic type info doesn't exist at runtime,
which is why you can't instantiate `T` directly or create generic arrays."

## 10. Related topics

- [Collections framework](../02-collections/list-set-map.md)
- [Spring Data JPA](../06-spring/spring-data-jpa.md)
