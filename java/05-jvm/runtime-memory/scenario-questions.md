# Scenario-based Questions

**Scenario/Question:** Memory Leak in Java Application Your Spring Boot app crashes after running for long hours. How would you identify a memory leak? What tools would you use (heap dump, etc.)? How would you fix it?

**Scenario/Question:** Large Payload Handling An API receives very large JSON payloads. How do you optimize memory usage? Would you use streaming or chunking?

**Scenario/Question:** A memory leak is reported in production - how will you investigate?

**Scenario/Question:** A service crashes due to OutOfMemoryError - what steps will you take?

**Scenario/Question:** A method is slow due to object creation - how will you optimize it?

**Scenario/Question:** How will you prevent memory leaks in Java applications?

**Scenario/Question:** How will you handle high GC pauses in production?



⚡ Performance & Latency
 
Your API response time suddenly jumps from 200 ms to 5 seconds. Where do you investigate first?
The application performs well with 100 users but struggles with 10,000 concurrent users. What usually breaks first?
CPU usage is low, but latency is extremely high. What could cause this?
How do you determine whether the bottleneck is your application code, database, network, or downstream services?
A database query executes in milliseconds in development but takes 10 seconds in production. Why?
Memory usage gradually increases over several days until the application crashes. How do you identify the leak?

PERFORMANCE AND LATENCY:
1. Your API response jumps from 200 ms to 5 seconds. Where do you look first?
2. Fine at 100 users. Falls over at 10,000. What breaks?
3. CPU is normal but latency is high. What is going on?
4. Is the slowness in your code, the DB, the network, or a downstream call? How do you tell?
5. One query runs fast in dev and takes 10 seconds in prod. Why?
6. Memory creeps up for days, then the app crashes. Find the leak.

Your Spring Boot service works perfectly in development. It crashes every night at 2am in production. Walk me through how you debug it."

Most candidates say:
 - I would check the logs.
 - I would restart the service.
 - I would add more memory?

Here is what Interviewer actually looking for:

Step 1: Isolate the pattern 2am every night. Not random. Not traffic-based. This is a scheduled event or a resource leak. First question: what runs at 2am? Batch jobs? Scheduled tasks? Cron?

Step 2: Check memory before it crashes Use JVM metrics heap usage over time. If memory climbs steadily from 10pm to 2am then crashes that is a memory leak.
Not a bug. Not infrastructure. A leak.

Step 3: Find the leak Enable GC logs. Check heap dumps. Look for objects that keep growing unclosed connections, static collections, ThreadLocal variables never cleared. One unclosed DB connection in a loop will kill your service every single night.

Step 4: Check connection pools HikariCP default pool size is 10. If your batch job opens 10 connections and never releases them next request hangs. By 2am pool exhausted. Service down. Fix: connection timeout + proper try-with-resources everywhere.

Step 5: Verify with APM tools Prometheus + Grafana. New Relic. Datadog. Set alerts before the crash not after. If heap crosses 80% at lam alert fires. You fix it before 2am. That is production engineering. Not just development.