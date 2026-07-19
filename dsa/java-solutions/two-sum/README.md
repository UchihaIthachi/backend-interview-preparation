# Two Sum

## Problem

Given an array of integers and a target, return the indices of the two
numbers that add up to the target. Assume exactly one solution exists.

## Pattern

Hash map (single pass)

## Brute force

Check every pair: O(n^2) time, O(1) space.

## Optimized approach

Walk the array once. For each element, check if `target - element` has
already been seen; if so, return both indices. Otherwise, record the current
element's index in the map.

- Time: O(n)
- Space: O(n)

## Edge cases

- Empty or single-element array (no valid pair possible).
- Duplicate values (`[3, 3]`, target `6`) — must not reuse the same index twice.
- Negative numbers.
- No solution (shouldn't happen per problem constraints, but worth guarding).

## Interview explanation

"I trade space for time: instead of checking every pair, I keep a map of
value -> index as I scan once. For each new number, I check whether its
complement is already in the map — if so, I've found my pair in one pass. This
brings it from O(n^2) down to O(n) at the cost of O(n) extra space."
