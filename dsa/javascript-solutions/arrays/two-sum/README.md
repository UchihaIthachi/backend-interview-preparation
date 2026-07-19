# Two Sum (JavaScript)

## Problem

Given an array of integers and a target, return the indices of the two
numbers that add up to the target. Assume exactly one solution exists.

## Pattern

Hash map (single pass) — see `../../../patterns.md` in the Java/Go DSA folder
for the general pattern write-up.

## Approach

Walk the array once, keeping a `Map` of value -> index seen so far. For each
element, check whether its complement (`target - value`) is already in the
map — if so, return both indices immediately.

- Time: O(n)
- Space: O(n)

## Edge cases

- Empty or single-element array (no valid pair possible).
- Duplicate values (`[3, 3]`, target `6`) — must not reuse the same index twice.
- Negative numbers.

## Interview explanation

"I trade space for time: instead of checking every pair (O(n²)), I keep a map
of value to index while scanning once. For each new number, I check whether
its complement is already in the map — if so, I've found the pair in a single
pass, bringing it down to O(n) time at the cost of O(n) extra space."
