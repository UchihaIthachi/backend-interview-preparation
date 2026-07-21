# Basic Questions

## What are React Hooks and why were they introduced?

Hooks are functions that let you use state and lifecycle features in functional components. They were introduced to reuse stateful logic, simplify complex components, and avoid class component pitfalls like 'this' binding and wrapper hell.

## Differentiate React Hooks vs Classes. / How does the performance of using Hooks differ?

**React Hooks:**
- Used in functional components.
- Will not require declaration of constructor.
- Does not require use of `this` keyword in state declaration or modification.
- Easier to use because of the `useState` functionality.
- Avoids a lot of overheads such as instance creation, binding of events.
- Results in smaller component trees by avoiding nesting present in HOCs.

**Classes:**
- Used in class-based components.
- Necessary to declare constructor.
- Keyword `this` will be used (`this.state`, `this.setState()`).
- No specific function to access state and setState variable.
- Long setup of state declarations makes them generally less preferred now.

## Do Hooks cover all the functionalities provided by the classes?

There are no Hook equivalents for the following methods yet: `getSnapshotBeforeUpdate()`, `getDerivedStateFromError()`, `componentDidCatch()`.

## Does React Hook work with static typing?

React Hooks are functions that can be statically typed. For enforcing stricter static typing, we can make use of the React API with custom Hooks (e.g., using TypeScript).

## What are the rules of Hooks?

Only call Hooks at the top level (not inside loops, conditions, or nested functions). Only call Hooks from React function components or custom Hooks.

## How does React know which state belongs to which `useState` call?

React relies on the order in which Hooks are called. Each component has an internal list of Hook nodes. On every render, React traverses this list in order.

## What is the `useState` hook and how does it work?

`useState()` is a built-in React Hook that allows you to have state variables in functional components. It returns a stateful value and a function to update it. Example: `const [count, setCount] = useState(0)`. The update triggers a re-render.

## What is the `useEffect` hook and what are its common use cases?

`useEffect` runs side effects after render. Use cases: data fetching, subscriptions, timers, DOM mutations, and manually changing the DOM.

## Explain the dependency array in `useEffect`.

It tells React when to re-run the effect. Empty `[]` = run once after mount. No array = run after every render. Values in array = re-run when any value changes.

## How do you clean up side effects in `useEffect`?

Return a cleanup function from the effect. React runs it before the component unmounts and before re-running the effect if dependencies changed.

## What is the `useContext` hook?

`useContext` consumes a React context value. It accepts a context object (created by `React.createContext`) and returns the current context value for that context.

## What is the `useReducer` hook and when would you use it over `useState`?

`useReducer` is for complex state logic that involves multiple sub-values or when the next state depends on the previous. It's like Redux but local to a component.

## What is `useRef` used for?

`useRef` returns a mutable ref object that persists for the lifetime of the component. It can hold DOM nodes directly or store any mutable value without causing re-renders.

## What is the difference between `useRef` and `createRef` in React?

- `useRef`: It is a hook. It uses the same ref throughout. It saves its value between re-renders in a functional component. The refs created can persist for the entire component lifetime. Used in functional components.
- `createRef`: It is a function. It creates a new ref every time (for every re-render). Used in class components.

## What is the difference between `useMemo` and `useCallback`?

`useMemo` memoizes a computed value; `useCallback` memoizes a function reference. Both optimize performance by preventing unnecessary recalculations or re-renders.

## Explain about other types of Hooks in React.

- `useImperativeHandle`: Enables modifying the instance that will be passed with the ref object.
- `useDebugValue`: Used for displaying a label for custom hooks in React DevTools.
- `useLayoutEffect`: Used for reading layout from the DOM and re-rendering synchronously.

## What is a custom Hook and how do you create one?

A custom Hook is a JavaScript function whose name starts with 'use' and that may call other Hooks. It lets you extract and reuse stateful logic across components. It is considered sufficient for replacing render props and HoCs, avoiding multiple layers of abstraction. Custom Hooks cannot be used inside classes.
