## Top 10 Concurrency Questions

1. Your service handles 50,000 requests/sec. You add one synchronized block and throughput drops by 70%. What would you look at first?


2. You increase the thread pool size from 50 to 500, expecting better performance. Instead, the API gets slower. Why?


3. A ThreadPoolExecutor has idle threads, but tasks are still waiting in the queue. How is that possible?


4. Why does volatile solve visibility problems but not make count++ thread-safe?


5. Your production service suddenly has 10,000 threads. CPU usage is low, but response times are terrible. What's your first hypothesis?


6. A thread dump shows hundreds of threads in the BLOCKED state. What does that tell you? More importantly, what doesn't it tell you?


7. Your thread pool queue keeps growing, but CPU utilization stays below 30%. Where would you start your investigation?


8. How do you decide the right thread pool size for a CPU-bound workload versus an I/O-bound workload?


9. A task submitted to an ExecutorService never runs. The application is healthy, there are no exceptions, and nothing shows up in the logs. What could be the reason?


10. A thread acquires a ReentrantLock and throws an exception before unlocking it. What happens next?