# Core DSA Patterns — One-Page Reference

## Two pointers

Use when working with sorted arrays or needing to compare elements from both
ends / at different speeds without nested loops.

```text
left = 0, right = len(arr) - 1
while left < right:
    if arr[left] + arr[right] == target: found
    elif sum < target: left += 1
    else: right -= 1
```

Signals: "sorted array", "pair that sums to X", "palindrome check".

## Sliding window

Use for contiguous subarray/substring problems — maintain a window that
expands and contracts instead of recomputing from scratch each time.

```text
left = 0
for right in range(len(arr)):
    add arr[right] to window
    while window invalid:
        remove arr[left] from window
        left += 1
    update answer with current window
```

Signals: "longest/shortest substring/subarray", "at most K distinct", "max sum
subarray of size K".

## Binary search

Use on sorted data, or on an answer space that is monotonic (if X works, every
value greater/less than X also works).

```text
lo, hi = 0, len(arr) - 1
while lo <= hi:
    mid = (lo + hi) // 2
    if arr[mid] == target: return mid
    elif arr[mid] < target: lo = mid + 1
    else: hi = mid - 1
```

Signals: "sorted array", "find minimum X such that...", O(log n) requirement.

## BFS / DFS

BFS for shortest path in an unweighted graph or level-order traversal. DFS for
exploring all paths, detecting cycles, or when recursion naturally fits
(trees).

```text
BFS: queue, visited set, process level by level
DFS: recursion or explicit stack, visited set, explore fully before backtrack
```

Signals: "shortest path", "connected components", "islands", "tree traversal".

## Backtracking

Use for generating all valid combinations/permutations/subsets — try a choice,
recurse, undo the choice if it doesn't lead anywhere.

```text
def backtrack(state):
    if state is a complete solution: record it; return
    for choice in options:
        make choice
        backtrack(state + choice)
        undo choice
```

Signals: "all permutations", "all subsets", "N-Queens", "valid combinations".

## Dynamic programming

Use when a problem has overlapping subproblems and optimal substructure —
break into smaller subproblems, cache results (memoization) or build bottom-up
(tabulation).

```text
dp[i] = best answer considering the first i elements
dp[i] = f(dp[i-1], dp[i-2], ..., arr[i])
```

Signals: "maximum/minimum ...", "number of ways to ...", "can you reach ...".

## Prefix sums

Use for repeated range-sum queries — precompute cumulative sums so any range
sum is O(1) instead of O(n) per query.

```text
prefix[i] = prefix[i-1] + arr[i]
rangeSum(l, r) = prefix[r] - prefix[l-1]
```

## Fast & slow pointers

Use for cycle detection in linked lists, or finding the middle of a list in
one pass.

```text
slow = head; fast = head
while fast and fast.next:
    slow = slow.next
    fast = fast.next.next
    if slow == fast: cycle detected
```
