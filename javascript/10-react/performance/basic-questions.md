# Basic Questions

## Name a few techniques to optimize React app performance.

- **Using `useMemo()`:** Caches CPU-expensive functions so they are only called when needed.
- **Using `React.PureComponent` / `React.memo`:** Prevents unnecessary re-renders by doing shallow prop/state comparison.
- **Maintaining State Colocation:** Moving state as close to where you need it as possible, instead of stuffing the parent component.
- **Lazy Loading:** Using `React.lazy()` + Suspense to code-split and load code on demand, reducing initial bundle size.
- **Windowing:** For large lists (e.g., `react-window`).

## What is `React.memo` and when should you use it?

`React.memo` is a higher-order component that memoizes a component. It prevents re-rendering if props haven't changed (shallow comparison). Use it for pure components that render often with the same props.

## What is the difference between `React.memo` and `useMemo`?

`React.memo` memoizes a component (prevents re-render). `useMemo` memoizes a value (prevents expensive recalculations).

## How can you prevent function recreation on every render?

Use `useCallback` to memoize functions. This is especially useful when passing callbacks to memoized child components.

## Why should you avoid inline functions in render?

Inline functions (`onClick={() => ...}`) create a new function on each render, breaking `React.memo` and `PureComponent` optimizations. Use `useCallback` or define handlers outside.

## What is the purpose of the 'key' prop in lists? / Why should keys be stable?

Keys help React identify which items have changed, added, or removed, enabling efficient reconciliation. Using stable, unique keys (like IDs) prevents unnecessary re-mounts and preserves component state.

## What is the difference between index as key and using an ID?

Using index as key can cause issues with item reordering, insertion, or deletion because React may reuse components incorrectly, leading to bugs. Always use a stable unique identifier.

## How does React handle batching of state updates?

React batches multiple setState calls within event handlers and lifecycle methods into a single update for performance. In React 18, batching works for promises, timeouts, and native events as well.

## What is the `useTransition` hook used for?

`useTransition` marks state updates as non-urgent, allowing React to keep the UI responsive during heavy updates. It returns a pending flag and a `startTransition` function.

## What is the React Compiler (formerly 'React Forget')?

The React Compiler is an optimizing compiler that automatically memoizes components, reducing the need for manual `useMemo`/`useCallback`. It improves performance without changing developer behavior.
