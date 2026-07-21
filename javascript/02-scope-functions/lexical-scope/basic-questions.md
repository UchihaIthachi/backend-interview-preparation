# Basic Questions

## Explain hoisting in JavaScript.

Hoisting is JavaScript's default behavior of moving variable and function declarations to the top of their containing scope during compilation.

## Explain closures in JavaScript. / What are closures and how are they implemented in JavaScript? / What are closures and how can they lead to memory leaks in JavaScript?

Closures are functions that have access to their outer function's scope, even after the outer function has finished executing. They are implemented by creating a scope chain that remains intact even after the outer function exits.

They can lead to memory leaks if they hold references to large objects or variables that are no longer needed, preventing them from being garbage collected.

## What are higher-order functions in JavaScript?

Higher-order functions are functions that operate on other functions, either by taking them as arguments or by returning them.

## What is a callback function?

A callback function is a function passed as an argument to another function, which is then invoked inside the outer function to complete some kind of routine or action.

## What is the `this` keyword in JavaScript?

The `this` keyword refers to the object on which a method is being invoked or the context in which a function is called.

## What are arrow functions in JavaScript?

Arrow functions are a concise way to write anonymous functions in JavaScript, introduced in ES6.

## Explain the concept of currying in JavaScript with an example.

Currying is the technique of converting a function that takes multiple arguments into a sequence of functions that each take a single argument.
For example: 
```javascript
function multiply(a) { 
  return function(b) { 
    return a * b; 
  }; 
} 
const multiplyByTwo = multiply(2); 
console.log(multiplyByTwo(3)); // Output: 6 
```

## What is memoization and how is it useful in JavaScript?

Memoization is an optimization technique used to speed up function execution by caching the results of expensive function calls and returning the cached result when the same inputs occur again.

## Explain the concept of `bind`, `call`, and `apply` methods in JavaScript.

`bind`, `call`, and `apply` are methods used to manipulate the `this` keyword in JavaScript functions. `bind` creates a new function with a specified `this` value, `call` calls a function with a specified `this` value and arguments passed individually, and `apply` calls a function with a specified `this` value and arguments passed as an array.

## What are generators in JavaScript?

Generators are functions that can be paused and resumed, allowing for more flexible control flow.
