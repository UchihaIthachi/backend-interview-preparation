# Java Streams: map, flatMap, reduce, collect

## 1. Definition

A `Stream` is a lazily-evaluated pipeline of operations over a sequence of
elements — it does not store data itself, it processes data from a source
(a collection, array, or generator) on demand.

## 2. Why it exists

Streams let you express data transformations declaratively (what to do) instead
of imperatively (how to loop), and enable easy parallelization without manually
managing threads.

## 3. How it works

```text
Intermediate operations (map, filter, sorted) are LAZY — they just build up a
pipeline description and don't run anything.

Terminal operations (collect, forEach, reduce, count) trigger actual execution,
pulling elements through the pipeline one at a time.
```

```java
List<String> names = people.stream()
    .filter(p -> p.getAge() > 18)   // lazy: not executed yet
    .map(Person::getName)            // lazy: not executed yet
    .sorted()                        // lazy: not executed yet
    .collect(Collectors.toList());   // terminal: NOW the whole pipeline runs
```

`map()` transforms each element 1-to-1. `flatMap()` transforms each element into
a stream and flattens all resulting streams into one — used when each input
produces zero or more outputs (e.g., a list of orders each containing a list of
line items, flattened into one stream of line items).

## 4. Code example

```java
List<LineItem> allItems = orders.stream()
    .flatMap(order -> order.getLineItems().stream())
    .collect(Collectors.toList());

BigDecimal total = allItems.stream()
    .map(LineItem::getPrice)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

## 5. Production use case

Transforming entity lists into DTOs, aggregating totals, and grouping data
(`Collectors.groupingBy`) are extremely common in service layers — but streams
should not replace simple loops when the logic is more readable imperatively.

## 6. Common mistakes

- Using streams with side effects (mutating external state inside `.map()`),
  which breaks referential transparency and can behave unpredictably with
  parallel streams.
- Reaching for `.parallel()` by default — for small collections or I/O-bound
  work, parallel streams often perform *worse* due to thread-pool overhead.
- Chaining so many operations that the code becomes harder to read than a loop
  would have been.

## 7. Trade-offs

| Streams | Loops |
|---|---|
| Declarative, composable | Imperative, sometimes clearer for complex logic |
| Easy parallelization | Manual thread management needed otherwise |
| Harder to debug mid-pipeline | Easy to add breakpoints/logs anywhere |

## 8. Interview questions

1. `map()` vs `flatMap()` — give a concrete example of when you need `flatMap`.
2. Why are streams lazy, and what triggers execution?
3. `reduce()` vs `collect()` — when do you use each?
4. Why can parallel streams sometimes make performance worse?
5. When would you prefer a plain for-loop over a stream pipeline?

## 9. Short interview answer

"Streams describe a lazy pipeline of transformations that only executes once a
terminal operation is called. I use map for 1-to-1 transforms and flatMap when
each element produces its own stream of results that need flattening — like
extracting all line items across a list of orders. I avoid parallel streams by
default since the overhead often outweighs the benefit for small or I/O-bound
workloads."

## 10. Related topics

- [Collections framework](../02-collections/list-set-map.md)
