# Node.js: Runtime, libuv, Streams, Worker Threads

## 1. Definition

Node.js runs JavaScript outside the browser using the V8 engine, plus libuv —
a C library providing the event loop, a thread pool for certain blocking
operations, and non-blocking I/O primitives.

## 2. Why it exists

Node lets JavaScript run as a general-purpose server runtime, using an
event-driven, non-blocking model that handles high I/O concurrency (many open
connections, mostly waiting on network/disk) efficiently with a single main thread.

## 3. How it works — architecture

```text
JavaScript code
     |
V8 engine (compiles/runs JS)
     |
Node.js APIs (fs, http, streams, ...)
     |
libuv event loop  <-- coordinates timers, I/O callbacks, and the thread pool
     |
OS / thread pool (used for fs operations, DNS lookups, some crypto)
```

Node's own JavaScript execution is single-threaded, but libuv's thread pool
(default size 4) handles specific blocking operations (like `fs` calls)
behind the scenes so they don't block the main thread.

## 4. Code example — streams avoid loading everything into memory

```javascript
import fs from "node:fs";

const readStream = fs.createReadStream("large-file.csv");
const writeStream = fs.createWriteStream("copy.csv");

readStream.pipe(writeStream);
// processes the file in small chunks instead of loading the whole thing
// into memory — essential for large files

readStream.on("error", (err) => console.error("read failed:", err));
writeStream.on("error", (err) => console.error("write failed:", err));
```

**Backpressure**: if the writable side is slower than the readable side,
`pipe()` automatically pauses reading until the writable side catches up —
without this, a fast producer could overwhelm a slow consumer's memory buffer.

## 5. Code example — worker threads for CPU-bound work

```javascript
import { Worker } from "node:worker_threads";

const worker = new Worker("./cpu-intensive-task.js");
worker.on("message", (result) => console.log("done:", result));
```

Worker threads exist because Node's event loop is single-threaded for JS
execution — a genuinely CPU-bound task (heavy computation, not I/O) run
directly would block the entire server from handling any other request until
it finishes. Worker threads run JS in a separate thread, dedicated to that
CPU-bound work.

## 6. Production use case

Streaming large file uploads/downloads, and offloading CPU-heavy work (image
processing, complex calculations) to worker threads so the main event loop
stays free to handle other requests, are two of the most common Node.js
production patterns.

## 7. Common mistakes

- Doing CPU-bound work (large synchronous loops, heavy computation) directly
  on the main thread, blocking the entire server for every other request
  while it runs.
- Loading an entire large file into memory (`fs.readFileSync`) instead of
  streaming it.
- Confusing `process.nextTick()` (runs even before other microtasks, Node-specific)
  with Promise microtasks — both jump ahead of macrotasks, but `nextTick` has
  its own even-higher-priority queue in Node.
- Not handling stream `'error'` events, causing unhandled errors to crash the process.

## 8. Trade-offs

| Main thread | Worker thread |
|---|---|
| Simple, shares memory directly | Isolated, needs message passing |
| Blocks everything if work is CPU-heavy | Keeps main event loop free |

## 9. Interview questions

1. Is Node.js single-threaded? Be precise about what part is and isn't.
2. What is libuv, and what runs in its thread pool by default?
3. What is backpressure, and how does `pipe()` handle it automatically?
4. When would you reach for worker threads instead of just async I/O?
5. `process.nextTick()` vs Promise microtasks — which runs first, and why?

## 10. Short interview answer

"Node's JavaScript execution is single-threaded, but libuv gives it a thread
pool for certain blocking operations like file system calls, plus the
non-blocking event loop for I/O generally. That single-threaded execution is
exactly why CPU-bound work — as opposed to I/O-bound work — needs to be
offloaded to worker threads, or it blocks the entire server from handling any
other request until it's done."

## 11. Related topics

- [Async JavaScript](asynchronous-javascript.md)
