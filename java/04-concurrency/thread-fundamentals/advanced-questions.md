# Advanced Questions

## Question 1: What is ThreadLocal? How and when should you use it?

`ThreadLocal<T>` provides per-thread variable instances. Each thread gets its own copy, completely isolated from other threads.

```java
ThreadLocal<SimpleDateFormat> formatter =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// Each thread gets its own SimpleDateFormat instance – thread safe!
String date = formatter.get().format(new Date());
```

Use cases: User session context in web servers, database connections, `SimpleDateFormat` (not thread-safe), request-scoped data.

❌ **Common Mistakes**
- Not calling `ThreadLocal.remove()` in thread pool threads – causes memory leaks because threads are reused.
- Storing heavy objects in `ThreadLocal` unnecessarily.

## Question 2: What are memory leaks in multithreaded applications and how to prevent them?

- **ThreadLocal leaks** – not calling `remove()` in thread pool threads. Fix: always `remove()` in `finally`.
- **Listener leaks** – registering listeners that hold references to large objects. Fix: use `WeakReference` listeners.
- **Unclosed ExecutorService** – threads kept alive indefinitely. Fix: always `shutdown()` and `awaitTermination()`.
- **Task queue overflow** – unbounded task queues grow without limit. Fix: use bounded queues.
