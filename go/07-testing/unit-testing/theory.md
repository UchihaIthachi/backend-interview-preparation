# Go Testing: Table-Driven Tests and Benchmarks

## 1. Definition

Go's built-in `testing` package supports unit tests (`func TestX(t *testing.T)`),
benchmarks (`func BenchmarkX(b *testing.B)`), and fuzz tests, with no external
framework required.

## 2. Why it exists

Keeping testing in the standard library avoids fragmentation and keeps test
code simple and consistent across the ecosystem.

## 3. How it works — table-driven tests

```go
func TestAdd(t *testing.T) {
    cases := []struct {
        name     string
        a, b     int
        expected int
    }{
        {"positive numbers", 2, 3, 5},
        {"negative numbers", -2, -3, -5},
        {"zero", 0, 0, 0},
    }

    for _, tc := range cases {
        t.Run(tc.name, func(t *testing.T) {
            got := Add(tc.a, tc.b)
            if got != tc.expected {
                t.Errorf("Add(%d, %d) = %d; want %d", tc.a, tc.b, got, tc.expected)
            }
        })
    }
}
```

## 4. Code example — benchmarking

```go
func BenchmarkAdd(b *testing.B) {
    for i := 0; i < b.N; i++ {
        Add(2, 3)
    }
}
```

```bash
go test -bench=. -benchmem
```

## 5. Production use case

Table-driven tests are the idiomatic Go way to cover many input/output cases
without duplicating test boilerplate — standard practice for testing
validation logic, parsers, and business rule functions.

## 6. Common mistakes

- Writing one test function per case instead of table-driven tests, leading
  to duplicated setup code.
- Not using `t.Run` with subtests, making it hard to see which specific case
  failed.
- Benchmarking without `-benchmem` when allocation count matters as much as speed.
- Not testing concurrent code under `-race` (`go test -race`) to catch data races.

## 7. Trade-offs

Table-driven tests add a small amount of structure overhead but dramatically
reduce duplication once you have more than 2-3 cases to test.

## 8. Interview questions

1. What is a table-driven test and why is it idiomatic in Go?
2. How do you benchmark a function in Go, and what does `-benchmem` add?
3. How does `go test -race` help catch concurrency bugs?
4. What's the difference between a unit test and a fuzz test in Go?

## 9. Short interview answer

"I default to table-driven tests for anything with more than a couple of
cases — one function, a slice of input/expected pairs, and a subtest per case
via t.Run, which makes failures easy to pinpoint. For concurrent code I always
run with -race in CI, since data races often don't show up reliably without it."

## 10. Related topics

- [Concurrency](04-concurrency.md)
