# Projects

Build these **one at a time, to completion**, rather than starting all three
in parallel. A single finished project demonstrates far more in an interview
than three half-built ones.

## Build order

1. [`java-order-service/`](java-order-service/) — Spring Boot + PostgreSQL + Kafka
2. `go-notification-service/` — add this folder once (1) is fully working
3. `distributed-order-platform/` — combine both into a capstone once each
   works independently

## Why this order

The Java order service exercises everything in `java/06-spring/` and
`databases/sql.md` in one place. Only after it's genuinely done (tested,
documented, running via `docker-compose`) should you start the Go service —
otherwise you end up with two unfinished projects instead of one finished one.
