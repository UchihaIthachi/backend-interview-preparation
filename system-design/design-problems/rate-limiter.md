# Design a Rate Limiter

## 1. Requirements

**Functional**: limit each client to N requests per time window; return 429
when exceeded.
**Non-functional**: low added latency, works correctly across multiple API
server instances (not just in-memory per-instance).

## 2. Capacity estimation

Rate limiter state needs to be checked on every single request, so it must be
extremely fast (sub-millisecond) and shared across all API instances — this
points strongly toward an in-memory data store like Redis rather than a
relational DB for the counter state.

## 3. API design

Not a public API itself — it's typically a piece of middleware/library used
by other services, keyed by client ID or API key.

## 4. Data model (in Redis)

```text
key: rate_limit:{client_id}:{window_start}
value: request count
TTL: window duration
```

## 5. High-level architecture

```text
Client -> API Gateway (rate limit check against Redis) -> Backend service
```

## 6. Detailed components — algorithm choice

```text
Token bucket: allows bursts up to bucket size, refills at a steady rate.
              Good general-purpose choice.
Sliding window log: most accurate, but stores a timestamp per request -> more
              memory.
Sliding window counter: approximates sliding window with much less memory by
              weighting the previous window's count.
```

## 7. Scalability

Redis (or similar) as a shared, fast store lets rate limit state work
correctly across many stateless API instances behind a load balancer — a
purely in-process counter would be wrong the moment you have more than one
instance.

## 8. Reliability

If Redis is briefly unavailable, decide explicitly: fail open (allow requests,
risk abuse) or fail closed (reject requests, risk false positives) — this is a
genuine business decision, not just a technical one.

## 9. Security

Rate limiting itself is a security control (prevents abuse/DoS); ensure the
client identifier used as the rate-limit key can't be trivially spoofed.

## 10. Observability

Track rejection rate per client, and alert on a sudden spike in 429s, which
often signals either an abusive client or a legitimate client whose limits
need adjusting.

## 11. Bottlenecks

A single hot key (one very high-volume client) could still create Redis
hotspotting even though overall load is distributed — consider local
pre-checks or sharding the counter for extreme cases.

## 12. Trade-offs

Token bucket is simple and allows reasonable bursts; sliding window counter
gives more accuracy with modest memory cost; sliding window log gives perfect
accuracy at real memory cost — pick based on how much burst tolerance and
precision the product actually needs.

## 13. Interview summary

"I'd use a shared Redis store (not in-process state) since rate limits must
be consistent across multiple API instances, implement token bucket for
general-purpose burst tolerance, and make fail-open vs fail-closed on a Redis
outage an explicit decision rather than an accident."
