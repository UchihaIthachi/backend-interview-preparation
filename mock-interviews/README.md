# Mock Interviews

Log every mock interview here (self-recorded, with a peer, or with a
service). Consistency matters more than volume — one mock per week, reviewed
honestly, beats five unreviewed ones.

## Log format

```markdown
## YYYY-MM-DD — [Java / Go / System Design / Behavioral]

**Format:** (self-recorded / peer / platform name)
**Topic covered:**
**What went well:**
**What went poorly:**
**Specific fix for next time:**
```

## Feedback template

See [`feedback-template.md`](feedback-template.md) to fill in after each session.


𝗥𝗼𝘂𝗻𝗱 𝟭: 𝗠𝗮𝗰𝗵𝗶𝗻𝗴 𝗖𝗼𝗱𝗶𝗻𝗴 𝗥𝗼𝘂𝗻𝗱
 - Cricket ScoreBoard Application (commonly found in Leetcode posts)
 - Build a cricket scorecard system that displays the team score and each player's performance.
 - Inputs: Number of players per team, number of overs, and batting order.
Ball-by-ball input includes runs (including wides, no balls, or wickets).
 - At the end of every over, print:
 - Individual scores, balls faced, number of 4s and 6s.
 - Total team score, wickets.
 - Implement strike changes, handle extras, and determine the match winner.

𝗥𝗼𝘂𝗻𝗱 𝟮: 𝗗𝗮𝘁𝗮 𝗦𝘁𝗿𝘂𝗰𝘁𝘂𝗿𝗲𝘀 & 𝗣𝗿𝗼𝗯𝗹𝗲𝗺 𝗦𝗼𝗹𝘃𝗶𝗻𝗴
 - Find all pairs in an array with a given sum.
 - Find all triplets in an array with a given sum.
 - Check if a binary tree has a duplicate subtree.
 - Given a matrix, a position, and a value k, return the sum of the element at the position and all its neighbors within distance k (including diagonals).

𝗥𝗼𝘂𝗻𝗱 𝟯: 𝗛𝗶𝗴𝗵 𝗟𝗲𝘃𝗲𝗹 𝗗𝗲𝘀𝗶𝗴𝗻 (𝗛𝗟𝗗)
 - Design an Alert Monitoring System
 - Context: Central system in a microservices environment to manage alerts from different systems.
 - Features to support:
 - SIMPLE_COUNT: Raise alert if event count crosses a threshold.
 - BUCKETED_WINDOW: Count events in fixed buckets (e.g., 10 events in 1-hour bucket).
 - MOVING_WINDOW: Count events in a sliding time window.
 - The system should handle user/system events, trigger alerts based on configuration, and support real-time monitoring.

𝗥𝗼𝘂𝗻𝗱 𝟰: 𝗛𝗠 𝗥𝗼𝘂𝗻𝗱 (𝗛𝗶𝗿𝗶𝗻𝗴 𝗠𝗮𝗻𝗮𝗴𝗲𝗿)
 - Discussion Topics: Project experiences.
 - Day-to-day responsibilities.
 - Light behavioral questions.
 - Design Question: Tiny URL system (ran out of time midway).

𝗥𝗼𝘂𝗻𝗱 𝟱: 𝗛𝗟𝗗 𝟮
 - Design Stack Overflow-like System
 - Cover user flows, question/answer posting, tagging, voting, reputation system, and content visibility.
 - Performance, scale, and consistency challenges.

## Mock Interview Experience

𝗥𝗼𝘂𝗻𝗱 𝟭 - 𝗗𝗦 & 𝗔𝗹𝗴𝗼:
They gave him a HashMap frequency problem first. Straightforward. Then a sliding window question with dynamic constraints, the window size kept changing based on conditions mid-stream.

He said the trick wasn't solving it. It was explaining his thought process out loud while coding. The interviewer kept nudging him toward edge cases he hadn't considered.

𝗥𝗼𝘂𝗻𝗱 𝟮 - 𝗗𝗦 & 𝗔𝗹𝗴𝗼:
This round went deeper into graphs. Model a financial network as a graph, traverse it, find anomalies. BFS vs DFS came up naturally in the discussion.

The follow-up hit hard, what happens when the graph has 10 million nodes? He said most candidates freeze here. He didn't because he'd practiced the tradeoff conversation, not just the code.

𝗥𝗼𝘂𝗻𝗱 𝟯 - 𝗟𝗼𝘄 𝗟𝗲𝘃𝗲𝗹 𝗗𝗲𝘀𝗶𝗴𝗻:
Design a Notification Service supporting push, email, and SMS. Priority-based delivery. Retry on failure with exponential backoff.

They didn't care much about perfect syntax. They wanted clean class boundaries, clear ownership, and extensible design. He said the moment he started thinking in interfaces instead of implementations the round got easier.

𝗥𝗼𝘂𝗻𝗱 𝟰 - 𝗛𝗶𝗴𝗵 𝗟𝗲𝘃𝗲𝗹 𝗗𝗲𝘀𝗶𝗴𝗻:
Design a Stock Price Feed System for millions of concurrent users.

The interesting part wasn't the architecture. It was when the interviewer said, "market just opened and 50 million users hit simultaneously. Walk me through what breaks first."

What they were really testing:
 - Do you think in failure modes, not just features?
 - Can you justify database and protocol choices?
 - Do you understand bottlenecks before being told?

𝗥𝗼𝘂𝗻𝗱 𝟱 - 𝗛𝗶𝗿𝗶𝗻𝗴 𝗠𝗮𝗻𝗮𝗴𝗲𝗿 𝗥𝗼𝘂𝗻𝗱:
Walk me through the most complex system you built. How do you handle disagreements on architecture? Tell me about a time you improved reliability of an existing service.

He said this round felt casual but was the most important one. They were evaluating whether he could own things end to end not just code them.

The interviews weren't testing whether he knew the right answer. They were testing whether he could think clearly under pressure. That's the actual skill.

𝗥𝗼𝘂𝗻𝗱 𝟭: 𝗢𝗻𝗹𝗶𝗻𝗲 𝗦𝗰𝗿𝗲𝗲𝗻𝗶𝗻𝗴
 - Initial intro + discussion on a recent project
 - DSA - Minimum Size Subarray Sum
 - Minimum Window Substring

𝗥𝗼𝘂𝗻𝗱 𝟮: 𝗠𝗮𝗰𝗵𝗶𝗻𝗲 𝗖𝗼𝗱𝗶𝗻𝗴
 - Problem Statement: Locker Delivery System (new feature for an E-commerce platform)
 - Flow: Delivery agent requests locker (includes user and package info)
 - The system assigns a random available locker
 - Should be extensible for package attributes like weight, volume
 - Respond with locker ID + unique unlock code
 - The agent uses code to drop the package
 - Customer is notified with locker ID + unique code
 - Customer uses code to collect the package

𝗥𝗼𝘂𝗻𝗱 𝟯: 𝗛𝗶𝗴𝗵 𝗟𝗲𝘃𝗲𝗹 𝗗𝗲𝘀𝗶𝗴𝗻 (𝗛𝗟𝗗)
 - Design: NoBroker-like Platform
 - Core Functionalities: Search for properties (with map view)
 - Owners can list/unlist properties; rarely update amenities
 - Customers can rent available properties, pay deposits, and rent
 - Both parties can view transaction history and analytics

𝗥𝗼𝘂𝗻𝗱 𝟰: 𝗛𝗠 & 𝗖𝘂𝗹𝘁𝘂𝗿𝗮𝗹 𝗙𝗶𝘁 𝗥𝗼𝘂𝗻𝗱
 - General discussion on past projects
 - Explained MindTickle’s business model and products
 - Describe a situation where you successfully handled a project with vague or unclear requirements.
 - Explain how you mentored or helped a team member grow technically or professionally.

𝗥𝗼𝘂𝗻𝗱 𝟭: 𝗢𝗻𝗹𝗶𝗻𝗲 𝗦𝗰𝗿𝗲𝗲𝗻𝗶𝗻𝗴 (𝗛𝗮𝗰𝗸𝗲𝗿𝗘𝗮𝗿𝘁𝗵)
 - Duration: 75 minutes (2 Problems)
 - Problems: Max Tasks That Can Be Completed in Given Budget (50 Marks)
 - Beautiful Numbers - HackerEarth (100 Marks)

𝗥𝗼𝘂𝗻𝗱 𝟮: 𝗖𝗼𝗱𝗶𝗻𝗴 (𝗩𝗶𝗿𝘁𝘂𝗮𝗹)
 - Problems: Reverse Substrings Between Each Pair of Parentheses
 - Water Jug Problem (variation of Two Water Jug Puzzle – GFG)

𝗥𝗼𝘂𝗻𝗱 𝟯: 𝗝𝗮𝘃𝗮 & 𝗖𝗼𝗻𝗰𝗲𝗽𝘁𝘀
 - Topics Covered: Java Streams API (filter, collect to list, group by)
 - SQL: Second highest marks using the RANK function
 - POJO creation — how to make it immutable
 - Comparable vs Comparator
 - String immutability and advantages
 - Java Collection Hierarchy
 - HashMap collision handling + Java 8 improvements
 - Spring: Bean life cycle
 - Autowiring methods
 - Bean scopes
 - @SessionScope annotation
 - @ControllerAdvice
 - Spring Batch – overview
 - REST & APIs: REST vs SOAP
 - PUT vs PATCH
 - REST request/response structure
 - HTTP status codes (2xx, 4xx, 5xx)
 - API Maturity levels

𝗥𝗼𝘂𝗻𝗱 𝟰: 𝗛𝗶𝗿𝗶𝗻𝗴 𝗠𝗮𝗻𝗮𝗴𝗲𝗿 𝗥𝗼𝘂𝗻𝗱
 - HLD: Design Twitter-like system
 - Discussed chosen components and trade-offs
 - Deep dive into resume projects
 - Standard behavioral questions

𝗥𝗼𝘂𝗻𝗱 𝟭: 𝗣𝗵𝗼𝗻𝗲 𝗦𝗰𝗿𝗲𝗲𝗻 (𝗗𝗦𝗔)
 - Problem: Graph Traversal Optimization
 - Task: Reduce space from O(N) → O(1) while maintaining time efficiency.
 - Focus: Graph representation + traversal logic.

𝗥𝗼𝘂𝗻𝗱 𝟮: 𝗢𝗻𝘀𝗶𝘁𝗲 𝟭 (𝗗𝗦𝗔)
 - Problem: String manipulation with state transitions.
 - Similar to LeetCode 777 – Swap Adjacent in LR String.
 - Tested logic, constraints, and invariant reasoning.

𝗥𝗼𝘂𝗻𝗱 𝟯: 𝗢𝗻𝘀𝗶𝘁𝗲 𝟮 (𝗗𝗮𝘁𝗮 𝗦𝘁𝗿𝘂𝗰𝘁𝘂𝗿𝗲 𝗗𝗲𝘀𝗶𝗴𝗻)
 - Problem: Manage elements ordered by 2 criteria (fixed numeric range + temporal value).
 - Goal: O(1) insertion & retrieval of min element.
 - Concepts: Bucket Sort variations, direct indexing, optimized data design.
 - Related to LeetCode 347 – Top K Frequent Elements.

𝗥𝗼𝘂𝗻𝗱 𝟰: 𝗢𝗻𝘀𝗶𝘁𝗲 𝟯 (𝗔𝗿𝗿𝗮𝘆𝘀 + 𝗥𝗮𝗻𝗴𝗲 𝗤𝘂𝗲𝗿𝗶𝗲𝘀)
 - Problem: Check the feasibility of reaching the target array after cumulative range updates.
 - Follow-up 1: Find the minimum number of initial operations → Binary Search approach.
 - Follow-up 2: Clarified constraints on applying operations.
 - Related to LeetCode 3362.

𝗥𝗼𝘂𝗻𝗱 𝟱: 𝗚𝗼𝗼𝗴𝗹𝗶𝗻𝗲𝘀𝘀 (𝗕𝗲𝗵𝗮𝘃𝗶𝗼𝘂𝗿𝗮𝗹)
 - Why Google?
 - Handling disagreements with senior teammates.
 - Passion projects & continuous learning.
 - Ownership examples, problem-solving stories.
 - Career aspirations.

𝗧𝗲𝗮𝗺 𝗠𝗮𝘁𝗰𝗵𝗶𝗻𝗴:
 - Conversation with Hiring Manager.
 - Discussed developer tools, backend vs frontend balance, and interests.
 - Two-way fit for role & team needs.