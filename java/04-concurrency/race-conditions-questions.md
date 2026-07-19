# Race Conditions Questions

### What is a race condition?

No answer provided yet.

### `volatile` vs `synchronized`?

No answer provided yet.

### Why is `volatile` insufficient for counters?

No answer provided yet.

### `synchronized` vs `ReentrantLock` — when would you pick the lock?

No answer provided yet.

### How do you identify deadlocks in a running application?

No answer provided yet.

### What is Synchronization?

A synchronized keyword is applicable for methods and blocks.
A synchronization is allowed one thread to execute given object.Hence we achieve thread safety.
The main advantage of synchronization is we solve data inconsistence problem.
The main disadvantage of synchronization is ,it will increase waiting time of a thread which reduce the performance of the system.
If there is no specific requirement then it is never recommanded to use synchronization concept.
synchronization internally uses lock mechanism.
Whenever a thread wants to access object , first it has to acquire lock of an object and thread will release the lock when it completes it's task.
When a thread wants to execute synchronized method.It automatically gets the lock of an object.
When one thread is executing synchronized method then other threads are not allowed to execute other synchronized methods in a same object concurently.But other threads are allowed to execute non-synchronized method concurently.

