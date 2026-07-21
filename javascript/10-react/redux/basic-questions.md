# Basic Questions

## What is the difference between Context and Redux?

Context is built into React and is fine for low-frequency updates. Redux provides a more powerful architecture with devtools, middleware, time-travel debugging, and predictable state updates, suited for large applications.

## What is react-redux and what are its benefits?

React-redux is a state management tool that makes it easier to pass states from one component to another irrespective of their position in the component tree, preventing application complexity.
Benefits:
- Centralized state management (single store).
- Optimizes performance as it prevents re-rendering.
- Makes debugging easier.
- Offers persistent state management.

## Explain the core components of react-redux?

- **Redux Store:** Object holding application state.
- **Action Creators:** Functions returning actions.
- **Actions:** Simple objects conventionally having `type` and `payload`.
- **Reducers:** Pure functions updating application state in response to actions.

## How can we combine multiple reducers in React?

We use the `combineReducers` function in Redux. It helps combine multiple reducers into a single unit to manage the Redux store when multiple actions require multiple reducers.

## Can React Hook replaces Redux?

React Hook cannot entirely replace Redux for managing global application state in large complex applications. Redux is powerful for handling dependent state pieces at a lower hierarchy level. Often, developers combine React Hooks with Redux.
