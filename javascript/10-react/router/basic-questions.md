# Basic Questions

## What is React Router and why is it used?

React Router is a standard routing library for React that enables navigation between different views/URLs in a single-page application without full page reloads.

## What is the difference between `BrowserRouter` and `HashRouter`?

`BrowserRouter` uses HTML5 history API (clean URLs like `/about`). `HashRouter` uses URL hash (`#/about`) and works in environments where the server cannot be configured for client-side routing.

## How do you define routes in React Router v6?

Use `<Routes>` and `<Route>` components: `<Routes><Route path='/' element={<Home />} /></Routes>`. Or use `createBrowserRouter` with an array of route objects.

## What is the `useNavigate` hook used for?

`useNavigate` returns a function to programmatically navigate. Example: `const navigate = useNavigate(); navigate('/about'); navigate(-1); // go back.`

## How do you pass data between routes?

Use state in navigate: `navigate('/user', { state: { id: 1 } });` then access via `useLocation().state`. Or use URL parameters (`/user/:id`) and `useParams`.

## What are nested routes in React Router?

Routes defined inside another route's component using `<Outlet />`. They render based on the child path, allowing for shared layouts and nested UI.

## What is the `useParams` hook?

`useParams` returns an object of key/value pairs of URL parameters. For route `/user/:id`, `useParams()` returns `{ id: '123' }`.

## What is the `useLocation` hook used for?

`useLocation` returns the current location object with pathname, search, hash, and state. Useful for reading query parameters or handling redirects.

## How do you implement protected routes in React Router?

Create a wrapper component that checks authentication and either renders `<Outlet />` or navigates to `/login` using `<Navigate replace to='/login' />`.

## What is the difference between `Link` and `NavLink`?

`Link` is for general navigation. `NavLink` adds styling attributes (`activeClassName`, `style`) when its path matches the current URL, useful for navigation menus.
