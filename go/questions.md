# Go — Interview Question Bank

## Fundamentals
1. Array vs slice — what's actually different?
2. Why can appending to a slice sometimes affect the caller and sometimes not?
3. What does the "comma ok" idiom protect against?
4. What is struct embedding and how does it differ from inheritance?

## Interfaces & pointers
5. Why is interface satisfaction implicit in Go?
6. Value receiver vs pointer receiver — how do you decide?
7. Explain the nil interface gotcha with an example.

## Error handling
8. Why does Go prefer explicit error returns over exceptions?
9. `errors.Is` vs `errors.As`?
10. When is `panic`/`recover` appropriate?

## Concurrency
11. What is a goroutine leak and how do you detect one?
12. How does `context.Context` propagate cancellation?
13. Why should only the sender close a channel?
14. Walk through building a worker pool with graceful shutdown.

## Backend & runtime
15. How would you implement HTTP middleware chaining?
16. Why does graceful shutdown matter for containerized services?
17. Explain the GMP scheduling model.
18. How would you diagnose high CPU or growing memory in a Go service?
