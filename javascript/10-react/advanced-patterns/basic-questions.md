# Basic Questions

## What are Higher-Order Components (HOC) and give an example?

An HOC is a function that takes a component and returns a new component with enhanced behavior. Example: `withAuth(Component)` that adds authentication logic.

## What is the render props pattern?

A pattern where a component receives a function as a prop (usually named 'render') that returns React elements. The component calls that function with internal data.

## What is the difference between HOCs and render props?

HOCs wrap a component to add behavior. Render props use a function prop that the component calls with data, offering more flexibility and avoiding naming collisions.

## What are React Portals and when would you use them?

Portals allow rendering children into a different DOM node outside the parent component's hierarchy. Useful for modals, tooltips, and dropdowns to avoid CSS overflow and z-index issues.

## What is Error Boundary and how does it work?

Error Boundaries are components that catch JavaScript errors anywhere in their child component tree, log errors, and display a fallback UI. Implement using `static getDerivedStateFromError` and `componentDidCatch`. Functional components cannot be Error Boundaries.

## What is the difference between `useEffect` and `useLayoutEffect`?

`useEffect` runs asynchronously after paint, not blocking visual updates. `useLayoutEffect` runs synchronously before paint, useful for measuring DOM layout to avoid flickering.

## What are React Server Components (RSC) and how are they different from client components?

RSCs run on the server and never ship JavaScript to the client, reducing bundle size. They can directly access backend resources. They cannot use `useState`, `useEffect`, or browser APIs.

## What is the 'use' hook (React 19) used for?

The 'use' hook reads the value of a Promise or Context. It can be used conditionally and in loops, unlike other hooks.

## What is the difference between 'server actions' and traditional API routes?

Server actions are functions that run on the server and can be called directly from client components, handling mutations and data fetching without creating API endpoints.
