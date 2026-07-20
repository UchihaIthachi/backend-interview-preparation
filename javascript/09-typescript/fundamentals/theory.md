# TypeScript: Types, Interfaces, Generics, Utility Types

## 1. Definition

TypeScript adds a static type system on top of JavaScript, checked entirely
at compile time — the emitted JavaScript has no runtime trace of the types at all.

## 2. Why it exists

JavaScript's dynamic typing pushes type errors to runtime, often in
production. TypeScript catches an entire category of bugs (wrong argument
types, typos in property names, null/undefined access) before the code ever runs.

## 3. How it works — interface vs type alias

```typescript
interface User {
  id: string;
  email: string;
}
interface Admin extends User {
  permissions: string[];
}

type UserType = {
  id: string;
  email: string;
};
type ID = string | number; // type aliases can express unions; interfaces can't directly
```

Rule of thumb: use `interface` for object shapes that might be extended
(especially public API contracts), `type` when you need unions, intersections,
or mapped/conditional types.

## 4. Code example — generics and utility types

```typescript
function firstOf<T>(items: T[]): T | undefined {
  return items[0];
}

interface ApiUser {
  id: string;
  email: string;
  password: string;
}

type PublicUser = Omit<ApiUser, "password">;   // removes a field
type PartialUser = Partial<ApiUser>;            // all fields optional
type ReadonlyUser = Readonly<ApiUser>;          // all fields immutable
```

## 5. Code example — type guards and narrowing

```typescript
function isString(value: unknown): value is string {
  return typeof value === "string";
}

function process(input: unknown) {
  if (isString(input)) {
    console.log(input.toUpperCase()); // TypeScript now knows input is string
  }
}
```

`unknown` forces you to narrow the type before using it (safe); `any` opts
out of type checking entirely (unsafe, defeats the purpose of TypeScript).

## 6. Production use case

API boundary types are the highest-value place to use TypeScript — but since
types vanish at runtime, an incoming HTTP request body still needs runtime
validation (e.g., with Zod or a JSON schema validator) since TypeScript can't
verify that the actual bytes on the wire match the declared type.

## 7. Common mistakes

- Using `any` to silence a type error instead of properly typing the value —
  this disables type checking for everything that flows from that value.
- Assuming a TypeScript type guarantees runtime shape — it doesn't; external
  data (API responses, JSON.parse, user input) still needs runtime validation.
- Overusing `interface extends` chains instead of composing smaller types.

## 8. Trade-offs

| any | unknown |
|---|---|
| No type checking at all | Forces narrowing before use |
| Easy to accidentally propagate bugs | Safe, slightly more upfront code |

## 9. Interview questions

1. `interface` vs `type` — when do you pick one over the other?
2. `any` vs `unknown` — why is `unknown` considered safer?
3. What is a type guard, and how does it enable narrowing?
4. Give an example of a utility type (`Partial`, `Omit`, `Pick`, `Readonly`)
   and when you'd use it.
5. Why do API payloads still need runtime validation even with TypeScript?

## 10. Short interview answer

"TypeScript's types are fully compile-time — they vanish in the emitted
JavaScript, which is exactly why external data like an API request body still
needs runtime validation; TypeScript can't check something it can't see at
runtime. I default to unknown over any when a type is genuinely not known
yet, since unknown forces me to narrow it with a type guard before using it,
while any just silently disables checking."

## 11. Related topics

- [Fundamentals](fundamentals.md)
