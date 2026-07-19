# Go Interfaces, Pointers and nil Gotchas

## 1. Definition

An interface in Go is satisfied implicitly — any type with the right methods
implements the interface, with no `implements` keyword. A pointer receiver
method can mutate the receiver; a value receiver method operates on a copy.

## 2. Why it exists

Implicit interface satisfaction enables decoupled design — a package can
define an interface and consumers can pass in any type that happens to match,
without the producing package needing to know about the interface at all.

## 3. How it works — value vs pointer receivers

```go
type Counter struct { count int }

func (c Counter) IncValue() { c.count++ }     // mutates a COPY, no effect on caller
func (c *Counter) IncPointer() { c.count++ }   // mutates the real receiver

c := Counter{}
c.IncValue()
c.IncPointer()   // Go automatically takes &c here
fmt.Println(c.count) // 1, only the pointer version had an effect
```

Rule of thumb: if any method on a type needs a pointer receiver (to mutate
state), make ALL methods on that type use pointer receivers, for consistency.

## 4. Code example — the nil interface trap

```go
type MyError struct{}
func (e *MyError) Error() string { return "boom" }

func doWork() error {
    var err *MyError = nil
    return err  // returns a non-nil error interface wrapping a nil *MyError!
}

if doWork() != nil {
    fmt.Println("error is not nil") // this actually prints! classic gotcha
}
```

An interface value is nil only if BOTH its type and value are nil. Returning a
typed nil pointer as an interface produces a non-nil interface.

## 5. Production use case

This nil-interface gotcha is a very common source of bugs when a function
returns a custom error type through the `error` interface — always return a
literal `nil`, not a nil-valued typed variable, when there's no error.

## 6. Common mistakes

- Returning a nil-valued concrete type through an interface and expecting
  `== nil` checks to work.
- Using value receivers when the method needs to mutate state.
- Mixing pointer and value receivers on the same type inconsistently.

## 7. Trade-offs

| Value receiver | Pointer receiver |
|---|---|
| Safe, no mutation, cheap for small structs | Required for mutation, avoids copying large structs |

## 8. Interview questions

1. Why does Go use implicit interface satisfaction instead of explicit `implements`?
2. What's the difference between a value receiver and a pointer receiver?
3. Explain the nil interface gotcha with a concrete example.
4. When would you choose a pointer receiver purely for performance, not mutation?

## 9. Short interview answer

"Go interfaces are satisfied implicitly, which decouples definitions from
implementations. The classic gotcha is that an interface value is only nil if
both its underlying type and value are nil — so returning a nil-valued
concrete pointer through an interface produces a non-nil interface, which
trips up naive `== nil` error checks."

## 10. Related topics

- [Error handling](03-error-handling.md)
