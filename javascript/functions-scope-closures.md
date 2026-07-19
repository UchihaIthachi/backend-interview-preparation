# Functions, Scope, Closures, Hoisting, and `this`

## 1. Definition

Scope determines where a variable is accessible. A closure is a function that
retains access to variables from its defining (lexical) scope even after that
outer scope has finished executing. `this` is a dynamic binding determined by
*how* a function is called, not where it's defined (except for arrow functions).

## 2. Why it exists

Closures let JavaScript implement private state and factory functions without
classes. Lexical scoping (as opposed to dynamic scoping) makes code
readable — you can determine what a variable refers to just by reading the
source, without tracing the call stack. Dynamic `this` binding gives
flexibility (the same function can act as a method on different objects) at
the cost of being one of JavaScript's most common footguns.

## 3. How it works — closures

```javascript
function createCounter() {
  let count = 0;              // private state, not accessible from outside
  return function increment() {
    count++;
    return count;
  };
}

const counter = createCounter();
counter(); // 1
counter(); // 2 — the returned function still has access to `count`
           // even though createCounter() already returned
```

## 4. Code example — this binding rules, in order of precedence

```javascript
// 1. new binding — `this` is the newly created object
function User(name) { this.name = name; }
const u = new User("Alice");

// 2. explicit binding — call/apply/bind set `this` directly
function greet() { return `Hi, ${this.name}`; }
greet.call({ name: "Bob" });   // "Hi, Bob"

// 3. implicit binding — `this` is whatever object the method was called on
const obj = { name: "Carol", greet() { return this.name; } };
obj.greet(); // "Carol"

const detached = obj.greet;
detached();  // undefined (or throws in strict mode) — `this` is lost when
             // the method is passed around detached from its object

// 4. arrow functions — NO own `this`; they capture `this` lexically from
//    their enclosing scope at definition time, ignoring all the rules above
const arrowObj = {
  name: "Dave",
  greet: () => this.name // `this` here is whatever `this` was OUTSIDE arrowObj
};
```

## 5. Production use case

Losing `this` when passing a method as a callback (`setTimeout(obj.method,
1000)`) is one of the most common JavaScript bugs — fixed with `.bind(obj)`,
an arrow function wrapper, or defining the method as a class field arrow
function in the first place.

## 6. Common mistakes

- Passing `obj.method` as a callback without binding, then being confused why
  `this` is `undefined` inside it.
- Using a regular function (not arrow) for an object literal method meant to
  capture the surrounding lexical `this` — arrow functions deliberately don't
  have their own `this`, which is useful, but only when that's actually what you want.
- Confusing hoisting of function declarations (fully hoisted, callable before
  their definition in code) with function expressions/arrow functions
  assigned to `let`/`const` (in the Temporal Dead Zone until reached).

## 7. Trade-offs

| Regular function | Arrow function |
|---|---|
| Own `this`, determined by call site | No own `this` — inherits from enclosing scope |
| Has `arguments` object | No `arguments` object |
| Can be a constructor (`new`) | Cannot be used with `new` |

## 8. Interview questions

1. What is a closure, and give a concrete use case beyond a counter example.
2. Walk through how `this` is determined for a given function call.
3. Why do arrow functions behave differently with `this`?
4. What's the Temporal Dead Zone, and which declarations are affected by it?
5. `call` vs `apply` vs `bind` — what's the practical difference between the three?

## 9. Short interview answer

"A closure is a function that keeps access to its defining scope's variables
even after that scope has returned — it's how JavaScript does private state
without classes. `this` is dynamic and determined by the call site, following
a precedence order: new, explicit bind, implicit (object.method()), then
default — except arrow functions, which skip all of that and just inherit
`this` lexically from where they're defined, which is why they're the fix for
losing `this` in callbacks."

## 10. Related topics

- [Fundamentals](fundamentals.md)
- [Async JavaScript](asynchronous-javascript.md)
