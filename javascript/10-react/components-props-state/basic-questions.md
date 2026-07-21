# Basic Questions

## What is the difference between a React element and a React component?

An element is a plain object describing what you want to see on the screen (e.g., `React.createElement('div')`). A component is a function or class that accepts props and returns a React element tree.

## What is the difference between a React component and a React pure component?

`React.PureComponent` implements `shouldComponentUpdate` with a shallow prop and state comparison. Class components re-render by default; `PureComponent` can skip re-renders for performance.

## What is the difference between functional and class components?

**Functional Components:**
- A plain JavaScript pure function that accepts props as an argument and returns a React element(JSX).
- There is no render method used.
- Run from top to bottom and once returned it can’t be kept alive.
- Also known as Stateless components (historically), but Hooks allow them to have state and side effects.
- React lifecycle methods cannot be used directly, hooks are used instead.

**Class Components:**
- Requires you to extend from `React.Component` and create a render function that returns a React element.
- Must have the `render()` method returning JSX.
- Instantiated and different life cycle methods are kept alive and invoked depending on the phase.
- Also known as Stateful components because they implement logic and state.
- React lifecycle methods (e.g., `componentDidMount`) can be used.
- Constructors are used to store state.

## What is the difference between state and props?

Props are read-only data passed from one component to another (parent to child). State is internal, mutable data managed within a component. State changes trigger re-renders; props changes from parent also cause child re-renders.

## How do you pass data from parent to child?

Via props. The parent sets an attribute on the child component: `<Child name={value} />`. The child accesses it via `props.name`.

## How do you pass data from child to parent?

Pass a callback function as a prop from parent to child. The child calls that callback with the data as an argument.

## What is lifting state up?

Moving shared state to the closest common ancestor component so that multiple children can access and update it. This maintains a single source of truth.

## What is prop drilling and how can you avoid it?

Prop drilling is passing props through many intermediate components that don't need them. Avoid it by using Context, component composition, or state management libraries.

## How do you set default props for a component?

Using `defaultProps` property on the component or default parameters in the function signature: `function Greeting({ name = 'Guest' }) { ... }`

## Why are props immutable in React?

To maintain unidirectional data flow and predictability. If a child could mutate props, debugging would become very hard. The parent owns the data and is responsible for changes.

## How do you update state based on previous state?

Pass a function to the state setter: `setCount(prev => prev + 1)`. This ensures you work with the most up-to-date value, especially in async updates.

## Is state update synchronous or asynchronous?

State updates are batched and asynchronous for performance. You cannot read the new value immediately after calling `setState`; use `useEffect` to react to changes.
