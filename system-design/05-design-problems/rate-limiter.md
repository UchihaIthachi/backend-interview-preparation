A rate limiter is the most underestimated system design question.

Easy to describe. Brutal to get right.

The ask sounds too simple.
Cap each user at 100 requests per minute.
Most engineers write the counter and call it done.

That version works on your laptop.
It dies in production.

Here is what actually breaks it.

THE NAIVE APPROACH:
- Keep a counter per user in memory
- Increment on every request
- Reset it each minute
- Over the limit, reject

Clean. Simple. Wrong at scale.

THE REAL PROBLEMS:

1. Your counter lives on one server
 - You scale to 50 servers behind a load balancer.
 - Each one keeps its own counter.
 - The user hits 100 on every single server.
 - Your "100 per minute" is now 5,000 per minute.
 - Fix: Move the counter to Redis. Every server checks the same one.

2. The window boundary is a loophole
 - Your window resets at the top of each minute.
 - User sends 100 at 11:00:59.
 - Then 100 more at 11:01:00.
 - 200 requests in two seconds. The limit said 100.
 - Fix: Sliding window. Weight the last window into the current one.

3. Two requests read the same count
 - Request A reads count = 99.
 - Request B reads 99 at the same instant.
 - Both think they are under. Both write 100.
 - One request slipped straight past your limit.
 - Fix: Use an atomic increment. Each request gets its own number back. No two ever collide.

4. Redis becomes the thing that kills you
 - Every request now leans on Redis.
 - Redis stalls for one second. Your whole API stalls with it.
 - Fix: Redis cluster with replicas. And decide now. If Redis dies, do you block everyone or let everyone through?

The architecture that holds:
Request to API Gateway to Redis to Service.
- API Gateway rejects with 429 the moment a user goes over
- Redis holds one shared counter per user, same across all 50 servers
- The increment is atomic, so no request slips through the cracks
- Your service only ever sees traffic that already passed the limit

The follow-up that catches people:
 - Redis goes down at peak traffic. Now what?
 - You fail open or you fail closed.
 - Fail open: let requests through, protect availability.
 - Fail closed: block them, protect the system behind you.
 - Most APIs fail open. You would rather serve traffic than take yourself down over a counter.

A rate limiter is not about counting requests.
It is about counting correctly when 50 servers count at once.

Most engineers stop at the in-memory counter. The ones who get the offer know why it falls apart.