# JavaScript Fundamentals

## 1. Definition

JavaScript is dynamically typed with 7 primitive types (`string`, `number`,
`bigint`, `boolean`, `undefined`, `symbol`, `null`) and one reference type
(`object`, which arrays and functions are special cases of). `var`, `let`, and
`const` all declare variables but differ in scope and mutability.

## 2. Why it exists

Dynamic typing keeps the language flexible for scripting, but pushes type
errors to runtime — which is precisely the gap TypeScript exists to close.
`let`/`const` were added in ES6 specifically to fix scoping problems inherent
in `var`.

## 3. How it works — var vs let vs const

```javascript
var vs let vs const:

var    -> function-scoped, hoisted and initialized as undefined, re-declarable
let    -> block-scoped, hoisted but NOT initialized (Temporal Dead Zone), reassignable
const  -> block-scoped, hoisted but NOT initialized, cannot be reassigned
          (but object/array CONTENTS can still be mutated)
```

```javascript
if (true) {
  var x = 1;
  let y = 2;
}
console.log(x); // 1 — leaked out of the block, var is function-scoped
console.log(y); // ReferenceError — y is block-scoped to the if statement

const arr = [1, 2, 3];
arr.push(4);       // fine — mutating contents is allowed
arr = [5, 6];       // TypeError — reassigning the binding itself is not
```

## 4. Code example — type coercion and equality

```javascript
"5" == 5      // true  -> string coerced to number before comparing
"5" === 5     // false -> no coercion, different types
[] == false   // true  -> [] coerces to "" -> coerces to 0; false coerces to 0
null == undefined   // true  -> special-cased equal to each other, nothing else
null === undefined  // false -> different types

// Truthy/falsy: only these are falsy — everything else is truthy
false, 0, -0, 0n, "", null, undefined, NaN
```

## 5. Production use case

Always default to `===`/`!==` in application code to avoid coercion
surprises; reach for `==` only in the specific, well-understood case of
checking `value == null` to catch both `null` and `undefined` in one check.

## 6. Common mistakes

- Using `var` inside loops with closures/callbacks and expecting each
  iteration to capture its own value (it doesn't — all closures share the
  same `var` binding; `let` fixes this by creating a new binding per iteration).
- Assuming `const` makes an object immutable — it only prevents reassigning
  the variable itself, not mutating the object's properties.
- Relying on `==` broadly instead of `===`, hitting subtle coercion bugs.

## 7. Trade-offs

| var | let/const |
|---|---|
| Function-scoped, hoisted+initialized | Block-scoped, Temporal Dead Zone until declaration |
| Legacy, rarely the right choice today | Default choice in modern JavaScript |

## 8. Interview questions

1. `var` vs `let` vs `const` — what actually differs?
2. Why does `[] == false` evaluate to `true`?
3. What are the exact falsy values in JavaScript?
4. Why doesn't `const` make an array or object immutable?
5. Classic loop-closure bug: why does a `var` loop capture the wrong value in
   an async callback, and how does `let` fix it?

## 9. Short interview answer

"I default to const, and let only when reassignment is genuinely needed —
var's function scoping and hoisting behavior causes real bugs, especially in
loops with closures. For equality I stick to strict equality except for the
one deliberate case of `value == null`, which is a concise way to catch both
null and undefined."

## 10. Related topics

- [Closures & scope](functions-scope-closures.md)
