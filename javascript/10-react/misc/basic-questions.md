# Basic Questions

## Explain conditional rendering in React.

Conditional rendering refers to the dynamic output of UI markups based on a condition state. It is possible to toggle specific application functions, show/hide elements, etc.
Approaches:
- Using if-else logic.
- Using ternary operators.
- Using logical && operators.
- Using element variables.

## How to create a switching component for displaying different pages?

A switching component renders one of multiple components based on prop values, often using an object for mapping prop values to components:
```javascript
const PAGES = { home: HomePage, about: AboutPage };
const Page = (props) => {
  const Handler = PAGES[props.page];
  return <Handler {...props} />;
}
```

## How to re-render the view when the browser is resized?

Listen to the resize event in `useEffect` (or `componentDidMount`) and update state with dimensions, and return a cleanup function to remove the listener.
```javascript
useEffect(() => {
  const handleResize = () => setDimensions({ width: window.innerWidth, height: window.innerHeight });
  window.addEventListener('resize', handleResize);
  return () => window.removeEventListener('resize', handleResize);
}, []);
```

## What is the difference between React and ReactDOM?

React contains the core component logic, while ReactDOM provides DOM-specific methods like `render`, `hydrate`, and `createPortal`. React Native uses React but not ReactDOM.

## What is the StrictMode component and why use it?

StrictMode helps detect potential problems (unsafe lifecycles, legacy refs, unexpected side effects) by running additional checks and double-invoking certain functions in development.

## What is the difference between shallow rendering and full rendering?

Shallow rendering renders only one level deep, ignoring child components — useful for unit testing. Full rendering renders the entire component tree.

## How do you handle forms with many inputs without a library?

Use a single state object and a generic onChange handler that uses `e.target.name` to update the correct field.

## What is the purpose of the 'displayName' property?

It helps with debugging by providing a custom name for the component in React DevTools, especially useful for Higher-Order Components.

## How do you handle environment variables in a React app?

Use `.env` files and prefix variable names with `REACT_APP_` (Create React App) or `NEXT_PUBLIC_` (Next.js). Access them via `process.env`.
