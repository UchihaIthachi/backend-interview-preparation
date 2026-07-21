# Basic Questions

## What is JavaScript?

JavaScript is a high-level, interpreted programming language primarily used for creating interactive web pages.

## What are the different data types in JavaScript?

JavaScript supports various data types including strings, numbers, booleans, null, undefined, objects, and symbols (added in ES6).

## What are JavaScript primitive data types?

Primitive data types in JavaScript include strings, numbers, booleans, null, undefined, and symbols.

## How do you declare variables in JavaScript?

Variables in JavaScript can be declared using the `var`, `let`, or `const` keywords.

## What is the difference between `let`, `const`, and `var`?

`var` has function scope, `let` has block scope, and `const` is used to declare constants whose value cannot be reassigned.

## What is the use of `==` and `===` operators in JavaScript? / What are the differences between `==` and `===` in JavaScript? Provide examples.

`==` performs type coercion before comparison (loose equality), whereas `===` is used for strict equality comparison, which checks both value and type. For example:
```javascript
console.log(1 == '1'); // Output: true (coerces string to number) 
console.log(1 === '1'); // Output: false (strict comparison) 
```

## How do you comment in JavaScript?

Single-line comments are denoted by `//`, and multi-line comments are enclosed between `/*` and `*/`.

## What is the `typeof` operator used for?

The `typeof` operator is used to determine the type of a variable or an expression.

## What is NaN in JavaScript?

`NaN` stands for "Not-a-Number" and is a value returned when a mathematical operation cannot produce a meaningful result.

## Explain the difference between `null` and `undefined` in JavaScript.

`null` is an explicitly assigned value that represents the intentional absence of any object value, while `undefined` represents a variable that has been declared but has not yet been assigned a value.

## What is the difference between `null`, `undefined`, and `undeclared` in JavaScript?

`null` is an explicitly assigned value that represents the absence of any object value, `undefined` indicates a variable that has been declared but has not yet been assigned a value, and `undeclared` refers to variables that have not been declared at all.
