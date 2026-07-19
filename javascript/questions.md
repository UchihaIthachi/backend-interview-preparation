# JavaScript — Consolidated Interview Question Bank

## Core JavaScript
1. `var`, `let`, and `const` — what actually differs?
2. `==` vs `===`?
3. `null` vs `undefined`?
4. What is type coercion? Why is `[] == false` true?
5. What is hoisting, and what's the Temporal Dead Zone?
6. What is lexical scope?
7. What is a closure? Give a real use case.
8. How is `this` determined for a given call?
9. Arrow function vs regular function — behavior differences beyond syntax?
10. `call`, `apply`, and `bind` — what does each do?
11. Shallow copy vs deep copy?
12. What are the exact falsy values in JavaScript?

## Async JavaScript
13. How does the event loop work, precisely (call stack, microtasks, macrotasks)?
14. Microtask vs macrotask — give two examples of each.
15. Why does `Promise.resolve().then()` run before `setTimeout(fn, 0)`?
16. `Promise.all` vs `Promise.allSettled` vs `Promise.race` vs `Promise.any`?
17. Why can unbounded `Promise.all` over a huge array be dangerous in production?
18. Does `await` block the JavaScript thread?

## Node.js
19. Is Node.js single-threaded? What exactly is and isn't?
20. What is libuv, and what runs in its thread pool?
21. What is backpressure, and how does `pipe()` handle it?
22. When should you use worker threads instead of async I/O alone?
23. `process.nextTick()` vs Promise microtasks — priority order?

## TypeScript
24. `any` vs `unknown`?
25. `interface` vs `type` — when do you pick each?
26. What is a type guard?
27. Why do API payloads still need runtime validation with TypeScript?

## React
28. Props vs state?
29. Why do list items need stable keys instead of array indices?
30. `useMemo` vs `useCallback`?
31. What causes a stale closure inside `useEffect`?

> Answer each out loud in under 90 seconds before checking the corresponding
> topic file.
