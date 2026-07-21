# Basic Questions

## What tools are commonly used to test React components?

Jest as test runner, React Testing Library (RTL) for component testing, and sometimes Enzyme. For E2E, Cypress or Playwright.

## What is React Testing Library (RTL) and why is it recommended?

RTL encourages testing components as users would interact with them, focusing on accessibility and behavior rather than implementation details. It queries by role, text, and label.

## How do you test a component that makes an API call?

Mock the API call using `jest.mock` or MSW (Mock Service Worker). Then assert that the UI shows loading state, then the data after the promise resolves.

## What is the difference between `fireEvent` and `userEvent` in RTL?

`fireEvent` dispatches DOM events synchronously. `userEvent` simulates full browser interactions (clicks, typing) more realistically and is asynchronous.

## How do you test asynchronous behavior in React components?

Use `findBy` queries (which return a promise) or `waitFor` utility from RTL. Use `act()` to wrap state updates.

## What is snapshot testing and when should you use it?

Snapshot testing captures the rendered output of a component and compares it to a saved snapshot. Use sparingly for small, stable components to detect unexpected changes.

## How do you test custom hooks?

Use `@testing-library/react-hooks` (or `renderHook` in modern RTL) to test hooks in isolation. It provides `result.current` and `rerender` utilities.

## What is the purpose of 'data-testid'?

It's a custom attribute used to identify elements for testing when role/text queries are not feasible. Use it as a last resort.

## How do you test if a component renders conditionally?

Render the component with different props/state, and use assertions like `expect(screen.getByText('...')).toBeInTheDocument()` or `.not.toBeInTheDocument()`.

## What is the difference between 'render' and 'renderHook'?

`render` is for components, returning queries and container. `renderHook` is for testing custom hooks, returning result and rerender.
