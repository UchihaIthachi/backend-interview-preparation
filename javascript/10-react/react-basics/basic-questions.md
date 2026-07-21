# Basic Questions

## What is React and what problems does it solve?

React is a declarative, component-based JavaScript library for building user interfaces. It solves the problem of efficiently updating and rendering the right parts of the UI when data changes, using the virtual DOM and unidirectional data flow.

## What are the advantages of using React?

- **Use of Virtual DOM to improve efficiency:** React uses virtual DOM to render the view. Creating a virtual DOM is much faster than rendering the UI inside the browser. Therefore, with the use of virtual DOM, the efficiency of the app improves.
- **Gentle learning curve:** React has a gentle learning curve when compared to frameworks like Angular. Anyone with little knowledge of javascript can start building web applications using React.
- **SEO friendly:** React allows server-side rendering, which boosts the SEO of an app.
- **Reusable components:** React uses component-based architecture for developing applications. Components are independent and reusable bits of code.
- **Huge ecosystem:** React provides you with the freedom to choose the tools, libraries, and architecture.

## What are the limitations of React?

- React is not a full-blown framework as it is only a library.
- The components of React are numerous and will take time to fully grasp the benefits of all.
- It might be difficult for beginner programmers to understand React.
- Coding might become complex as it will make use of inline templating and JSX.

## Explain the building blocks of React?

The five main building blocks of React are:
1. **Components:** These are reusable blocks of code that return HTML.
2. **JSX:** It stands for JavaScript and XML and allows you to write HTML in React.
3. **Props and State:** props are like function parameters and State is similar to variables.
4. **Context:** This allows data to be passed through components as props in a hierarchy.
5. **Virtual DOM:** It is a lightweight copy of the actual DOM which makes DOM manipulation easier.

## What is JSX and why is it used?

JSX stands for JavaScript XML. It is a syntax extension that looks like HTML but allows you to write JavaScript expressions inside curly braces. It allows us to write HTML inside JavaScript and place them in the DOM without using functions like appendChild( ) or createElement( ). It gets compiled to `React.createElement` calls, making code more readable and expressive.

## What are the rules of JSX?

Return a single root element (or use Fragment), all tags must close, use camelCase for attributes (className, htmlFor), embed JavaScript in `{}`, and use `className` instead of `class`.

## What are React Fragments and when would you use them? / What are the benefits of using React fragments?

Fragments let you group multiple children without adding extra DOM nodes. Use `<React.Fragment>` or `<> </>`. Useful for returning sibling elements from a component without a wrapper div, keeping the DOM clean, and they can be used with `key` in lists.

## How do you embed JavaScript expressions in JSX?

Use curly braces `{}`. For example: `<h1>Hello, {name}</h1>` or `<div>{condition ? 'Yes' : 'No'}</div>`.

## What is the `React.createElement` equivalent of `<div className='box'>Hello</div>`?

`React.createElement('div', { className: 'box' }, 'Hello')`

## Why do React components need to start with a capital letter?

JSX treats lowercase tag names as built-in HTML elements. Capitalized names are recognized as custom React components.
