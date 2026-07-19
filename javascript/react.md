# React: Components, Hooks, Rendering, Performance

## 1. Definition

React components are functions that return a description of UI (JSX). Props
are inputs passed from a parent; state is internal, component-owned data that
persists across renders and triggers a re-render when it changes.

## 2. Why it exists

React re-renders declaratively — you describe what the UI should look like
for a given state, and React figures out the minimal DOM changes needed
(reconciliation), instead of manually mutating the DOM imperatively.

## 3. How it works — useEffect and dependency arrays

```jsx
function UserProfile({ userId }) {
  const [user, setUser] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetchUser(userId).then((data) => {
      if (!cancelled) setUser(data);
    });
    return () => { cancelled = true; }; // cleanup: avoid setting state after unmount
  }, [userId]); // re-run only when userId changes

  return user ? <div>{user.name}</div> : <div>Loading...</div>;
}
```

The dependency array controls when the effect re-runs — an empty array means
"run once on mount"; omitting it entirely means "run after every render,"
which is rarely what you want.

## 4. Code example — useMemo vs useCallback

```jsx
// useMemo: memoizes a COMPUTED VALUE
const sortedItems = useMemo(() => [...items].sort(comparator), [items]);

// useCallback: memoizes a FUNCTION REFERENCE (so child components with
// React.memo don't re-render just because a new function was created)
const handleClick = useCallback(() => {
  onSelect(item.id);
}, [item.id, onSelect]);
```

## 5. Production use case

`useMemo`/`useCallback` matter most when passing values/functions to a
memoized child component (`React.memo`) or an expensive computation — using
them everywhere "just in case" adds overhead without benefit, since the
memoization check itself isn't free.

## 6. Common mistakes

- Mutating state directly (`state.items.push(x)`) instead of creating a new
  array/object — React compares references to decide whether to re-render, so
  a mutated-in-place object with the same reference won't trigger one.
- Missing dependencies in a `useEffect` array, causing stale closures that
  reference outdated props/state.
- Using array index as a `key` in a list that can reorder or have items
  inserted/removed — this confuses React's reconciliation and can cause
  incorrect component state to "stick" to the wrong item.
- Reaching for `useMemo`/`useCallback` everywhere without profiling first —
  premature optimization that adds complexity without measurable benefit.

## 7. Trade-offs

| Controlled component | Uncontrolled component |
|---|---|
| React state is the single source of truth | DOM holds its own state, accessed via ref |
| Easier to validate/transform input live | Less re-rendering, simpler for basic forms |

## 8. Interview questions

1. Props vs state — what's the actual difference in ownership and mutability?
2. Why does mutating state directly fail to trigger a re-render?
3. Why do list items need stable, unique keys instead of array indices?
4. `useMemo` vs `useCallback` — what does each actually memoize?
5. What causes a "stale closure" bug inside `useEffect`, and how do you fix it?

## 9. Short interview answer

"React decides whether to re-render by comparing state/prop references, which
is why mutating an object or array directly instead of creating a new one
silently fails to trigger an update. For lists, keys need to be stable and
tied to the actual data — not the array index — or reconciliation can attach
the wrong internal state to the wrong rendered item after a reorder or insertion."

## 10. Related topics

- [TypeScript](typescript.md) (typing props/hooks)
