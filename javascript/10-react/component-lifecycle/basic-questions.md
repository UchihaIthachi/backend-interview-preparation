# Basic Questions

## What is the React component lifecycle? / What are the different phases of the component lifecycle?

Lifecycle refers to the phases a component goes through: 
1. **Initialization:** Component prepares by setting up default props and initial state.
2. **Mounting:** Insertion of the component into the browser DOM. Includes methods `componentWillMount` and `componentDidMount`.
3. **Updating:** When state or props change. Includes `shouldComponentUpdate`, `render`, `componentDidUpdate`.
4. **Unmounting:** Component is removed from the DOM. Includes `componentWillUnmount`.

## What is the difference between mounting and rendering?

Mounting is the initial insertion of the component into the DOM. Rendering is the process of generating the virtual DOM representation; it can happen many times after mounting due to state/props changes.

## What are the lifecycle methods in class components?

- **constructor()**: Called when component is initiated before anything is done. Sets up initial state.
- **getDerivedStateFromProps()**: Called just before element rendering. First method called on update.
- **render()**: Outputs HTML to DOM. Essential method always called.
- **componentDidMount()**: Called after rendering. Useful for statements that need the component already in DOM.
- **shouldComponentUpdate()**: Returns boolean specifying whether React should proceed with rendering. Default true.
- **getSnapshotBeforeUpdate()**: Captures info from DOM before it changes (e.g., scroll position) to pass to `componentDidUpdate`.
- **componentDidUpdate()**: Called after component has been updated in DOM.
- **componentWillUnmount()**: Called when component removal from DOM is about to happen.

## What is the order of lifecycle calls when a component mounts?

`constructor` -> `static getDerivedStateFromProps` -> `render` -> `componentDidMount` (then children mount).

## What is the order of lifecycle calls when a component updates?

`static getDerivedStateFromProps` -> `shouldComponentUpdate` -> `render` -> `getSnapshotBeforeUpdate` -> `componentDidUpdate`.

## How do you replicate `componentDidMount` in a functional component?

`useEffect` with an empty dependency array: `useEffect(() => { /* side effect */ }, []);`

## How do you replicate `componentDidUpdate` in a functional component?

`useEffect` with dependencies: `useEffect(() => { /* runs when deps change */ }, [deps]);` or without deps (runs after every render).

## How do you replicate `componentWillUnmount` in a functional component?

Return a cleanup function inside `useEffect`: `useEffect(() => { return () => { /* cleanup */ }; }, []);`
