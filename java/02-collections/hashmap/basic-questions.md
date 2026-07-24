# Basic Questions — `equals()` and `hashCode()`

## Question 1: What is the significance of `equals()` and `hashCode()`?

Both methods are declared in `java.lang.Object` and are fundamental to object comparison and hash-based collections.

### `equals()`

`equals()` determines whether two objects are **logically equal**.

```java
User first = new User(101L, "Alice");
User second = new User(101L, "Alice");

System.out.println(first.equals(second));
```

Without overriding `equals()`, the implementation inherited from `Object` behaves like reference comparison:

```java
first == second
```

That means two separate objects are considered unequal even when they contain identical data.

### `hashCode()`

`hashCode()` returns an integer hash value used by hash-based collections to identify a likely storage bucket.

It is used by collections such as:

- `HashMap`
- `HashSet`
- `Hashtable`
- `ConcurrentHashMap`

```java
Map<User, String> departments = new HashMap<>();

departments.put(first, "Engineering");
```

During lookup, a hash-based collection broadly performs:

1. Call `hashCode()` to identify a bucket.
2. Search the candidate entries in that bucket.
3. Call `equals()` to find the logically equal key.

```text
hashCode() → bucket selection
equals()   → exact key matching
```

### Important correction

A hash code is **not guaranteed to be unique**.

Different objects can return the same hash code. This is called a **hash collision**.

```java
first.hashCode() == second.hashCode()
```

does not necessarily mean:

```java
first.equals(second)
```

### Interview-ready answer

> `equals()` defines logical equality between objects, while `hashCode()` provides an integer used to organize objects in hash-based collections. The hash code narrows the search to a bucket, and `equals()` confirms the exact matching object.

---

## Question 2: Why must `equals()` and `hashCode()` agree?

The contract requires:

> If two objects are equal according to `equals()`, they must return the same hash code.

```java
first.equals(second) == true
```

must imply:

```java
first.hashCode() == second.hashCode()
```

The reverse is not required. Two unequal objects may have the same hash code.

---

### What happens when the contract is broken?

Consider a class that overrides `equals()` but not `hashCode()`:

```java
public final class User {

    private final long id;
    private final String name;

    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof User other)) {
            return false;
        }

        return id == other.id;
    }
}
```

Two users with the same ID are logically equal:

```java
User first = new User(101L, "Alice");
User second = new User(101L, "Alice");

System.out.println(first.equals(second)); // true
```

However, because `hashCode()` was not overridden, the two objects may produce different hash codes.

```java
Set<User> users = new HashSet<>();

users.add(first);
users.add(second);

System.out.println(users.size());
```

The set may contain both objects even though `equals()` considers them equal.

Similarly:

```java
Map<User, String> map = new HashMap<>();

map.put(first, "Engineering");

System.out.println(map.get(second));
```

The lookup may return `null` because `second` is searched in a different bucket.

---

### Correct implementation

```java
import java.util.Objects;

public final class User {

    private final long id;
    private final String name;

    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof User other)) {
            return false;
        }

        return id == other.id
                && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
```

Now logically equal users produce equal hash codes.

---

## `equals()` contract

A correct `equals()` implementation should be:

### Reflexive

An object must equal itself.

```java
x.equals(x) == true
```

### Symmetric

If `x` equals `y`, then `y` must equal `x`.

```java
x.equals(y) == y.equals(x)
```

### Transitive

If `x` equals `y`, and `y` equals `z`, then `x` must equal `z`.

```java
x.equals(y)
&& y.equals(z)
```

must imply:

```java
x.equals(z)
```

### Consistent

Repeated comparisons should return the same result while the relevant object state remains unchanged.

### Non-null

Any non-null object must return `false` when compared with `null`.

```java
x.equals(null) == false
```

---

## `hashCode()` contract

A correct `hashCode()` implementation should satisfy:

1. Repeated calls return the same result while equality-related state remains unchanged.
2. Equal objects return the same hash code.
3. Unequal objects are allowed to return the same hash code.

Good hash distribution is desirable because excessive collisions reduce performance.

---

### Interview-ready answer

> `equals()` and `hashCode()` must agree because hash-based collections first use the hash code to locate a bucket and then use `equals()` to locate the exact object. If equal objects return different hash codes, lookups, duplicate detection, and removals can fail.

---

## Question 3: What is a hash code?

A hash code is an integer returned by the `hashCode()` method.

```java
Test test = new Test();

System.out.println(test.hashCode());
```

Every Java object can call `hashCode()` because the method is inherited from `Object`.

```java
public native int hashCode();
```

Conceptually, the method signature is:

```java
public int hashCode()
```

### Is it a unique object identifier?

No.

A hash code is not guaranteed to be:

- Unique
- A memory address
- Permanent across different executions
- Globally unique across JVMs

Two different objects can have the same hash code.

```java
objectA.hashCode() == objectB.hashCode()
```

does not prove that the objects are equal.

The integer range is finite, while a program can create more objects than the number of possible `int` values. Therefore, uniqueness cannot be guaranteed.

---

## Default `Object.hashCode()`

When a class does not override `hashCode()`, it inherits the implementation from `Object`.

The default hash code is related to object identity, but the Java specification does not require it to be the physical memory address.

```java
Object value = new Object();

System.out.println(value.hashCode());
```

For debugging identity-based behavior, Java also provides:

```java
System.identityHashCode(value);
```

This returns the hash code that would be produced using identity-based behavior, even when the class overrides `hashCode()`.

---

## Example of a collision

Two unequal strings can produce the same hash code:

```java
String first = "FB";
String second = "Ea";

System.out.println(first.equals(second));   // false
System.out.println(first.hashCode());       // same value
System.out.println(second.hashCode());      // same value
```

This is valid. `HashMap` handles the collision by placing the entries in the same bucket and distinguishing them with `equals()`.

---

### Interview-ready answer

> A hash code is an integer representation used mainly by hash-based collections to select a bucket. It is not a unique object ID or guaranteed memory address. Different objects may have the same hash code, so collections use `equals()` after hashing to identify the exact key.

---

# Why Immutable Map Keys Are Preferred

Consider a mutable key:

```java
public final class UserKey {

    private String username;

    public UserKey(String username) {
        this.username = username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof UserKey other)) {
            return false;
        }

        return Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
```

Usage:

```java
UserKey key = new UserKey("alice");

Map<UserKey, String> map = new HashMap<>();
map.put(key, "Account");

key.setUsername("bob");

System.out.println(map.get(key)); // May return null
```

The entry was stored in a bucket based on the old hash code. After mutation, lookup calculates a new hash code and searches another bucket.

Prefer immutable keys:

```java
public record UserKey(String username) {
}
```

Records automatically generate `equals()` and `hashCode()` using their components.

---

# Common Mistakes

## 1. Assuming hash codes are unique

Incorrect:

> The JVM creates a unique hash code for every object.

Correct:

> Every object can produce a hash code, but hash codes are not guaranteed to be unique.

---

## 2. Overriding only `equals()`

Incorrect:

```java
@Override
public boolean equals(Object object) {
    return true;
}
```

without a corresponding `hashCode()` override.

Rule:

> Override both `equals()` and `hashCode()`, or override neither.

---

## 3. Using every field in `hashCode()` but not in `equals()`

The same fields should generally participate in both methods.

Problematic:

```java
@Override
public boolean equals(Object object) {
    User other = (User) object;
    return id == other.id;
}

@Override
public int hashCode() {
    return Objects.hash(id, name, email);
}
```

Two users with the same ID may be considered equal but produce different hash codes because their names or emails differ.

Correct:

```java
@Override
public boolean equals(Object object) {
    if (this == object) {
        return true;
    }

    if (!(object instanceof User other)) {
        return false;
    }

    return id == other.id;
}

@Override
public int hashCode() {
    return Long.hashCode(id);
}
```

---

## 4. Returning a random hash code

Incorrect:

```java
@Override
public int hashCode() {
    return new Random().nextInt();
}
```

Repeated calls must remain consistent while the equality-relevant state is unchanged.

---

## 5. Returning a constant hash code

This is technically contract-valid:

```java
@Override
public int hashCode() {
    return 1;
}
```

Equal objects still receive the same hash code.

However, it creates excessive collisions and can seriously degrade hash-based collection performance.

---

## 6. Including mutable fields

Avoid using fields that change while the object is used as a `HashMap` key or `HashSet` element.

Prefer immutable equality state.

---

# Quick Contract Table

| Relationship                                                   | Required? |
| -------------------------------------------------------------- | --------: |
| Equal objects must have equal hash codes                       |       Yes |
| Unequal objects must have different hash codes                 |        No |
| Same hash code means objects are equal                         |        No |
| Equal hash code may represent a collision                      |       Yes |
| Hash code must remain stable while relevant state is unchanged |       Yes |
| Hash code must remain identical across program runs            |        No |

---

# Short Interview Answer

> `equals()` defines logical equality, while `hashCode()` returns an integer used to select buckets in hash-based collections. Equal objects must return the same hash code, but unequal objects may also collide. If the contract is broken, `HashMap` and `HashSet` may fail to find entries or prevent duplicates. A hash code is not a unique ID or guaranteed memory address, and immutable keys are preferred because their equality and hash values remain stable.
