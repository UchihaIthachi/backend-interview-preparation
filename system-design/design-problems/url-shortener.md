# Design a URL Shortener

## 1. Requirements

**Functional**: shorten a long URL, redirect a short URL to the original,
optionally support custom aliases and expiration.
**Non-functional**: low-latency redirects (this is a read-heavy system),
high availability, uniqueness of generated short codes.

## 2. Capacity estimation

```text
Assume: 100M new URLs/month, 100:1 read:write ratio
Writes/sec ~ 100M / (30*86400) ~ 40/sec
Reads/sec ~ 4,000/sec
Storage: 100M/month * 500 bytes/record * 12 months ~ 600 GB/year -> easily fits
a single well-indexed relational DB with room to spare, replicate for read scaling.
```

## 3. API design

```http
POST /urls        { "longUrl": "..." } -> { "shortCode": "abc123" }
GET /{shortCode}  -> 302 redirect to the long URL
```

## 4. Data model

```text
urls(short_code PK, long_url, created_at, expires_at, created_by)
```

## 5. High-level architecture

```text
Client -> Load Balancer -> API service -> Cache (short_code -> long_url) -> DB
```

## 6. Detailed components

- **Code generation**: base62-encode an auto-incrementing ID, or use a
  pre-generated pool of unique codes to avoid collision-checking on the hot path.
- **Cache**: since reads vastly outnumber writes, cache short_code -> long_url
  aggressively (long TTL, since mappings rarely change).
- **Redirect**: use HTTP 302 (temporary) rather than 301 (permanent) if you
  want to retain the ability to track clicks or change the mapping later.

## 7. Scalability

Read-heavy -> cache is the single highest-leverage addition. The DB itself can
be scaled with read replicas; writes are low-volume enough that a single
primary is fine for a long time.

## 8. Reliability

If the cache is down, fall back to DB directly (with a circuit breaker to
avoid overwhelming the DB during a cache outage).

## 9. Security

Rate limit URL creation to prevent abuse (spam/phishing link generation);
validate and sanitize submitted URLs.

## 10. Observability

Track cache hit rate (this is the key health metric for a read-heavy system
like this), redirect latency, and creation rate for abuse detection.

## 11. Bottlenecks

Hot short codes (viral links) could overwhelm a single cache node — mitigate
with cache replication or a CDN in front of redirects.

## 12. Trade-offs

Base62 encoding of sequential IDs is simple but reveals creation order/volume;
a pre-generated random code pool avoids that at the cost of extra bookkeeping.

## 13. Interview summary

"This is fundamentally a read-heavy caching problem: base62-encode a unique
ID for the short code, cache the short-code-to-URL mapping aggressively since
reads outnumber writes by roughly 100:1, and rate-limit creation to prevent abuse."
