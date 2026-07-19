# Redis: Caching, TTL, and Distributed Locks

## 1. Definition

Redis is an in-memory data store supporting strings, hashes, lists, sets,
sorted sets, and streams, commonly used as a cache, session store, or
lightweight message broker.

## 2. Why it exists

Relational databases are durable but comparatively slow for simple key-based
lookups under high read volume; Redis trades some durability guarantees for
sub-millisecond access.

## 3. How it works — cache-aside with TTL

```text
GET request for key
  -> check Redis first
     -> hit: return cached value
     -> miss: query DB, store result in Redis with a TTL, return it
```

```bash
SET user:123 "{...}" EX 300     # expires after 300 seconds
```

## 4. Code example — distributed lock (simplified)

```bash
SET lock:order:123 "worker-a" NX EX 10   # only sets if not already present
# ... do the exclusive work ...
DEL lock:order:123                        # release (ideally with a value check
                                           # via Lua script to avoid releasing
                                           # someone else's lock after expiry)
```

## 5. Production use case

Session storage, rate limiter counters, and read-heavy lookup caching (like
the URL shortener design) are the most common Redis use cases in backend systems.

## 6. Common mistakes

- No TTL on cache entries, leading to unbounded memory growth and permanently stale data.
- Using Redis as a system of record for data that actually needs durability
  guarantees beyond what Redis persistence options provide by default.
- Naive lock release (`DEL` without checking ownership) which can release
  another worker's lock if the original lock already expired.
- Not planning for cache stampede (many requests missing simultaneously and
  all hitting the DB at once when a hot key expires).

## 7. Trade-offs

| Redis | Relational DB |
|---|---|
| Extremely fast, in-memory | Durable, ACID transactions |
| Data can be lost on crash (config-dependent) | Durable by default |

## 8. Interview questions

1. Cache-aside vs write-through — what's the difference?
2. Why does a naive distributed lock implementation have a race condition on release?
3. What causes a cache stampede, and how would you prevent one?
4. Why shouldn't Redis be your system of record for critical data?

## 9. Short interview answer

"Redis earns its place wherever sub-millisecond lookups matter more than
strict durability — caching, rate limiting, session storage. The recurring
gotchas are unbounded memory from missing TTLs, and naive distributed locks
that can release a lock that isn't actually theirs anymore if it already expired."

## 10. Related topics

- [Rate limiter design](../system-design/design-problems/rate-limiter.md)
