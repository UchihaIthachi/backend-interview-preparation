# Basic Questions

## What is Context API and when should you use it?

Context API is used to pass global variables anywhere in the code without prop drilling. It helps when sharing state between a lot of nested components (like theme, user auth, locale). It is lightweight and eliminates the need for third-party libraries like Redux for simple global state. It has two properties: Provider and Consumer.

## How do you create and consume a context?

Create: `const MyContext = React.createContext(defaultValue)`. 
Consume: `useContext(MyContext)` hook, or `<MyContext.Consumer>` component.

## How do you update a context value?

Create a Context with a default value, then wrap a Provider with the new value. Typically, a parent component holds state and passes the state and updater functions via Context.

## What happens when a context value changes?

All components that consume that context will re-render, regardless of `shouldComponentUpdate` optimization.

## How can you avoid unnecessary re-renders with Context?

Split contexts by domain (e.g., ThemeContext, UserContext). Memoize context values with `useMemo`.

## What is the default value passed to `createContext` used for?

It's used when a component does not have a matching Provider above it. Useful for testing or when the value is optional.

## Can you use multiple contexts in a single component?

Yes, call `useContext` multiple times or nest multiple Consumers. Each context is independent.

## What is the difference between `Context.Provider` and `Context.Consumer`?

Provider supplies the context value. Consumer subscribes to context changes in class components or with render props. In functional components, `useContext` is preferred.
