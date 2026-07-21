# Basic Questions

## How do you handle events in React?

Using camelCase event names (`onClick`) and passing a function as the event handler. Example: `<button onClick={handleClick}>Click</button>`

## What is the difference between React events and native DOM events?

React events are `SyntheticEvent` wrappers that normalize cross-browser behavior. They bubble through the virtual DOM.

## How do you pass an argument to an event handler?

Use an arrow function: `onClick={() => handleClick(id)}`. Or use bind: `onClick={handleClick.bind(this, id)}`.

## How do you prevent default behavior in React events?

Call `e.preventDefault()` inside the event handler, where `e` is the `SyntheticEvent` object.

## What is the difference between controlled and uncontrolled components?

Controlled: form value is controlled by React state (`value` prop + `onChange`). Uncontrolled: form data is handled by the DOM itself, accessed via refs. Controlled gives more control and validation.

## How do you handle form submission in React?

Add an `onSubmit` handler to the `<form>` element, call `e.preventDefault()`, and access form data from state or refs.

## What is two-way data binding in React?

React doesn't have built-in two-way binding like Angular. You implement it by setting `value` from state and updating state via `onChange`: `<input value={value} onChange={e => setValue(e.target.value)} />`

## How do you manage multiple form fields?

Use a single state object where keys are field names, and a generic handler that updates based on `e.target.name` and `e.target.value`.

## What are refs and how are they used with forms?

Refs provide access to DOM nodes or React elements. In forms, they can be used to read input values (uncontrolled components) or focus inputs imperatively.

## What is the difference between `onChange` and `onInput` in React?

In React, `onChange` behaves like `onInput` (fires on every keystroke). There's no difference for text inputs.

## How do you create a checkbox or radio group in React?

For checkbox: use `checked={isChecked}` and `onChange`. For radio group: set the `checked` property based on the selected value and update state accordingly.
