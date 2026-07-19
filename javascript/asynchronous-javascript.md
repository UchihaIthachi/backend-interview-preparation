# Asynchronous JavaScript: Promises, async/await, Event Loop

## 1. Definition

JavaScript is single-threaded but non-blocking: long-running operations
(I/O, timers, network) are handed off to the runtime, and their callbacks are
queued to run later, coordinated by the event loop.

## 2. Why it exists

A single-threaded language needs some way to handle I/O without freezing the
whole program while waiting — the event loop, callback queue, and later
promises/async-await are the layers built to solve that without introducing
real OS-level threads into JavaScript itself.

## 3. How it works — the event loop, concretely

```text
1. Run all synchronous code on the call stack, top to bottom.
2. Once the call stack is empty, drain the ENTIRE microtask queue
   (Promise .then/.catch/.finally callbacks, queueMicrotask).
3. Take ONE task from the macrotask queue (setTimeout, setInterval, I/O
   callbacks, UI events) and run it.
4. Drain the microtask queue again (any new microtasks queued during step 3).
5. Repeat from step 3.
```

Microtasks always fully drain before the next macrotask runs — this is why
promises "jump the queue" ahead of `setTimeout`, even a `setTimeout(fn, 0)`.

## 4. Code example — the classic ordering question

```javascript
console.log("A");
setTimeout(() => console.log("B"), 0);   // macrotask
Promise.resolve().then(() => console.log("C")); // microtask
console.log("D");

// Output: A, D, C, B
// A, D run synchronously first.
// The microtask queue (C) drains completely before the event loop
// picks up the next macrotask (B), regardless of the 0ms delay.
```

## 5. Code example — async/await is sugar over promises

```javascript
async function fetchUser(id) {
  const response = await fetch(`/users/${id}`);
  if (!response.ok) throw new Error("Request failed");
  return response.json();
}
// `await` pauses the async function (without blocking the JS thread) until
// the promise settles, then resumes — errors from a rejected awaited promise
// surface as a normal thrown exception, catchable with try/catch.

// Promise.all — fails fast if ANY promise rejects
const [user, orders] = await Promise.all([loadUser(), loadOrders()]);

// Promise.allSettled — always resolves, giving status for EVERY promise
const results = await Promise.allSettled([sendEmail(), sendSMS()]);
```

## 6. Production use case

Fanning out independent API calls with `Promise.all` instead of awaiting them
one at a time turns a sequential N-call chain into effectively one round trip
of latency — but running `Promise.all` over an unbounded array (e.g., mapping
one million records) creates unbounded concurrent work with no backpressure,
which can overwhelm a downstream database or API.

## 7. Common mistakes

- Awaiting independent operations sequentially instead of using `Promise.all`,
  needlessly adding up their latencies.
- Using `Promise.all` over a huge, unbounded list — this has no concurrency
  limit and can exhaust connections/memory. Use a bounded worker pool or a
  concurrency-limiting library instead.
- Forgetting that a `.then()` callback's thrown error needs a `.catch()`
  somewhere in the chain, or it becomes an unhandled rejection.
- Assuming `await` blocks the JS thread — it doesn't; it only pauses the
  current async function, other code keeps running.

## 8. Trade-offs

| Sequential await | Promise.all | Bounded worker pool |
|---|---|---|
| Simple, slow for independent calls | Fast, but unbounded concurrency risk | Fast AND bounded, more code |

## 9. Interview questions

1. Walk through the exact output order of the classic `console.log` +
   `setTimeout` + `Promise.resolve().then()` example, and explain why.
2. Microtask vs macrotask — give two examples of each.
3. Why is unbounded `Promise.all(hugeArray.map(...))` dangerous in production?
4. `Promise.all` vs `Promise.allSettled` — when do you use each?
5. Does `await` block the JavaScript thread? What exactly does it pause?

## 10. Short interview answer

"The event loop always fully drains the microtask queue — where promise
callbacks live — before picking up the next macrotask, which is why a
Promise.resolve().then() runs before a setTimeout(fn, 0), even though both are
technically deferred. In practice, I use Promise.all to run independent
operations concurrently instead of awaiting them one by one, but I never run
it unbounded over a huge array — I use a bounded worker pool instead, since
unbounded concurrent promises remove any backpressure on the downstream system."

## 11. Related topics

- [Node.js](nodejs.md) (event loop implementation specifics)
