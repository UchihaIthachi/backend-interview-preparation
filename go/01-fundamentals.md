# Go Fundamentals: Slices, Maps, Structs

## 1. Definition

A slice is a growable view over an underlying array (pointer + length +
capacity). A map is a hash table. A struct is a typed collection of fields —
Go has no classes; behavior is attached via methods on types.

## 2. Why it exists

Go favors simplicity and explicitness over inheritance-heavy OOP: structs +
interfaces + composition cover what classes/inheritance do in other languages,
with less implicit magic.

## 3. How it works — slices are NOT arrays

```go
arr := [3]int{1, 2, 3}       // fixed-size array, value type
s := []int{1, 2, 3}          // slice: {pointer, len, cap}

s2 := s[:2]                  // shares the SAME underlying array as s
s2[0] = 99                   // this also changes s[0]! shared backing array

s3 := append(s, 4)           // if cap is exceeded, a NEW array is allocated
                              // and s3 no longer shares memory with s
```

This is one of the most common Go interview gotchas: slicing shares memory,
but `append` may or may not, depending on capacity.

## 4. Code example — struct embedding (composition, not inheritance)

```go
type Base struct {
    ID int
}

func (b Base) Describe() string { return fmt.Sprintf("ID: %d", b.ID) }

type Order struct {
    Base            // embedded -> Order automatically gets Describe()
    Total float64
}

o := Order{Base: Base{ID: 1}, Total: 99.99}
o.Describe() // "ID: 1" -- promoted method, no explicit forwarding needed
```

## 5. Production use case

Understanding slice aliasing prevents subtle bugs where modifying a "copy"
of a slice unexpectedly mutates shared data passed between functions — very
common when slicing into request buffers or batch-processing data.

## 6. Common mistakes

- Assuming slicing always copies data (it doesn't — it shares the backing array).
- Appending to a slice passed into a function and expecting the caller to see
  the new element when capacity was exceeded (it won't, since a new array was
  allocated).
- Using maps without checking the "comma ok" idiom, treating a zero value as
  if it means "key not found."

```go
val, ok := m["missing"]
if !ok {
    // key truly doesn't exist, val is the zero value
}
```

## 7. Trade-offs

| Array | Slice |
|---|---|
| Fixed size, value semantics | Dynamic size, reference-like semantics |
| Rarely used directly | The default choice for sequences in Go |

## 8. Interview questions

1. What's the difference between an array and a slice in Go?
2. Why can appending to a slice sometimes affect the original and sometimes not?
3. What does the "comma ok" idiom protect against with maps?
4. What is struct embedding, and how is it different from inheritance?

## 9. Short interview answer

"A slice is a view — pointer, length, capacity — over a backing array, so
slicing shares memory while appending may or may not, depending on whether
capacity is exceeded. Go doesn't have inheritance; struct embedding gives you
composition with promoted methods and fields instead, which keeps behavior
sharing explicit rather than relying on a class hierarchy."

## 10. Related topics

- [Interfaces & pointers](02-interfaces.md)
