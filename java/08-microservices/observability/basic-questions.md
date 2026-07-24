# Observability — Server/System and JVM Troubleshooting

Observability should help answer three questions:

- **What failed?**
- **Where did it fail?**
- **Why did it fail?**

For a Java backend, investigate four layers separately:

```text
Host operating system
        ↓
Container or Kubernetes Pod
        ↓
JVM process
        ↓
Application and dependencies
```

A “server crash” may actually be:

- A Linux host reboot
- The kernel killing the process
- A container memory-limit violation
- A JVM fatal error
- An uncaught application exception
- Kubernetes restarting an unhealthy container
- A deployment or administrative shutdown

---

# Q1: How Do You Find the Reason for a Server Crash?

## Step 1: Preserve evidence before restarting

Before manually restarting the application, preserve:

- Application logs
- System logs
- Previous container logs
- JVM fatal-error files
- Heap dumps
- Core dumps
- Kubernetes events
- Exit reason and exit code
- Monitoring graphs around the failure
- Deployment and configuration changes

Oracle’s troubleshooting guidance recommends collecting crash files, `hs_err` files, application logs, and heap dumps before evidence is removed by restarting or replacing the process. ([Oracle Docs][1])

---

## Step 2: Check the service state

For a service managed by `systemd`:

```bash
systemctl status my-application.service
```

View recent service logs:

```bash
journalctl -u my-application.service --since "1 hour ago"
```

Show the latest logs:

```bash
journalctl -u my-application.service -n 500
```

Follow logs:

```bash
journalctl -u my-application.service -f
```

Check logs from the previous boot:

```bash
journalctl -b -1
```

Check kernel logs from the previous boot:

```bash
journalctl -k -b -1
```

`journalctl` retrieves logs from the systemd journal and supports filtering by service, boot, kernel, and time range. ([FreeDesktop][2])

---

## Step 3: Check for Linux OOM kills and kernel failures

```bash
dmesg -T | grep -Ei "out of memory|oom|killed process|segfault|panic|i/o error"
```

Also check the kernel journal:

```bash
journalctl -k | grep -Ei "oom|killed process|segfault|panic"
```

Typical evidence may include:

```text
Out of memory: Killed process 18421 (java)
```

This means the operating system killed the Java process because the host or its memory control group was under memory pressure.

A JVM can disappear without producing a Java `OutOfMemoryError` when the operating system kills the process first.

---

## Step 4: Check JVM fatal-error files

A fatal JVM or native-code crash can create a file such as:

```text
hs_err_pid18421.log
```

Search for it:

```bash
find / -type f -name 'hs_err_pid*.log' 2>/dev/null
```

The file commonly contains:

- Fatal signal
- Problematic native frame
- JVM version
- Command-line arguments
- Thread information
- Heap information
- Loaded native libraries
- Operating-system details

Configure a predictable location:

```bash
-XX:ErrorFile=/var/log/myapp/hs_err_pid%p.log
```

HotSpot uses `hs_err_pid<pid>.log` for irrecoverable-error diagnostics by default, and `-XX:ErrorFile` changes its destination. ([Oracle Docs][3])

---

## Step 5: Check core dumps

```bash
coredumpctl list
```

Inspect a specific crash:

```bash
coredumpctl info <PID>
```

Open it in a debugger:

```bash
coredumpctl debug <PID>
```

Also verify whether core dumps are enabled:

```bash
ulimit -c
```

Native crashes may originate from:

- JNI libraries
- Database drivers
- Compression libraries
- JVM defects
- Operating-system libraries
- Direct or native memory corruption

---

## Step 6: Check Docker containers

```bash
docker ps -a
```

Read container logs:

```bash
docker logs --timestamps my-container
```

Inspect termination state:

```bash
docker inspect my-container
```

Useful fields include:

```text
State.Status
State.ExitCode
State.Error
State.OOMKilled
State.StartedAt
State.FinishedAt
```

A non-zero exit code provides a clue, but should be correlated with logs and host events rather than treated as the root cause by itself.

---

## Step 7: Check Kubernetes Pods

```bash
kubectl describe pod <pod-name> -n <namespace>
```

Current container logs:

```bash
kubectl logs <pod-name> -n <namespace>
```

Logs from the previously terminated container:

```bash
kubectl logs <pod-name> -n <namespace> --previous
```

Check events:

```bash
kubectl get events \
  -n <namespace> \
  --sort-by=.lastTimestamp
```

Check Pod status:

```bash
kubectl get pod <pod-name> \
  -n <namespace> \
  -o wide
```

Look for:

- `OOMKilled`
- `CrashLoopBackOff`
- Probe failures
- Node-pressure eviction
- Container exit code
- Deployment replacement
- Missing configuration or secrets
- Volume failures

Kubernetes recommends inspecting Pod logs and `kubectl describe pod`; a container exceeding its memory limit can be terminated, and node-pressure eviction can also terminate Pods when node resources are constrained. ([Kubernetes][4])

---

## Common crash causes

| Evidence                     | Likely cause                                    |
| ---------------------------- | ----------------------------------------------- |
| `OOMKilled`                  | Container exceeded memory limit                 |
| Kernel “Killed process java” | Host or cgroup OOM killer                       |
| `java.lang.OutOfMemoryError` | JVM allocation failure                          |
| `hs_err_pid*.log`            | JVM/native fatal error                          |
| `SIGSEGV`                    | Native memory or library crash                  |
| Probe failures               | Kubernetes restarted an unhealthy process       |
| Clean shutdown logs          | Deployment or administrator termination         |
| Disk-full errors             | Logs, temporary files, or storage exhaustion    |
| Repeated startup failure     | Invalid configuration or unavailable dependency |

---

# Q2: How Do You Find Server Memory Usage?

## Host-level memory

Start with:

```bash
free -h
```

Example:

```text
               total    used    free  shared  buff/cache  available
Mem:            16Gi    9Gi     1Gi    300Mi       6Gi        6Gi
Swap:            4Gi    500Mi   3.5Gi
```

Focus on `available`, not only `free`. Linux intentionally uses otherwise idle memory for caches that can often be reclaimed. `free` obtains its information from `/proc/meminfo`. ([man7.org][5])

Detailed information:

```bash
cat /proc/meminfo
```

Continuous system view:

```bash
vmstat 1
```

Interactive process view:

```bash
top
```

or:

```bash
htop
```

---

## Find the largest processes

```bash
ps aux --sort=-%mem | head -20
```

Show PID, RSS, virtual size, CPU, and command:

```bash
ps -eo pid,ppid,user,%cpu,%mem,rss,vsz,cmd \
  --sort=-rss | head -20
```

Key terms:

| Metric | Meaning                                              |
| ------ | ---------------------------------------------------- |
| RSS    | Physical memory currently resident for the process   |
| VSZ    | Virtual address space reserved or mapped             |
| PSS    | Shared memory divided proportionally among processes |
| USS    | Memory private to one process                        |

When available, `smem` provides PSS and USS, which can be more useful than RSS when processes share many pages. ([man7.org][6])

```bash
smem -tk
```

Process memory map:

```bash
pmap -x <PID>
```

---

## Container memory

Docker:

```bash
docker stats
```

Kubernetes:

```bash
kubectl top pod -n <namespace>
```

```bash
kubectl top node
```

Inside a modern Linux cgroup:

```bash
cat /sys/fs/cgroup/memory.current
cat /sys/fs/cgroup/memory.max
```

The host may have plenty of memory while a container is still killed because the container reached its own configured limit.

---

## JVM memory is not only heap

A Java process uses memory for:

```text
Java heap
Metaspace and class metadata
Code cache
Thread stacks
Direct ByteBuffers
Garbage collector structures
JNI/native libraries
Memory-mapped files
JVM internal structures
```

Therefore:

```text
Process RSS
≠
Current Java heap usage
```

A Java application can have a 2 GB heap but consume significantly more than 2 GB at the operating-system level.

## Check JVM heap

Find Java processes:

```bash
jcmd -l
```

Heap summary:

```bash
jcmd <PID> GC.heap_info
```

Class histogram:

```bash
jcmd <PID> GC.class_histogram
```

GC utilization over time:

```bash
jstat -gcutil <PID> 1000
```

Oracle recommends `jcmd` for modern JVM diagnostics and provides commands for heap information, class histograms, thread stacks, heap dumps, and Flight Recorder. `jcmd` normally needs to run on the same host with suitable process permissions. ([Oracle Docs][7])

---

## Check native JVM memory

Native Memory Tracking must be enabled when the JVM starts:

```bash
-XX:NativeMemoryTracking=summary
```

Then run:

```bash
jcmd <PID> VM.native_memory summary
```

Create a baseline:

```bash
jcmd <PID> VM.native_memory baseline
```

Compare later:

```bash
jcmd <PID> VM.native_memory summary.diff
```

NMT tracks native memory allocated internally by HotSpot, but does not account for every allocation made by third-party native code. It must be enabled at startup and adds diagnostic overhead, so it should be enabled deliberately. ([Oracle Docs][8])

---

# Q3: How Do You Debug High CPU or Memory Issues in the JVM?

# A. High CPU Investigation

## Step 1: Confirm the Java process

```bash
top
```

or:

```bash
ps -eo pid,%cpu,%mem,cmd --sort=-%cpu | head
```

## Step 2: Find the hot Java thread

```bash
top -H -p <JAVA_PID>
```

Alternative:

```bash
ps -L -p <JAVA_PID> \
  -o pid,tid,pcpu,pmem,state,comm \
  --sort=-pcpu
```

Suppose the hot Linux thread ID is:

```text
18455
```

Convert it to hexadecimal:

```bash
printf '%x\n' 18455
```

Example result:

```text
4817
```

## Step 3: Capture a thread dump

```bash
jcmd <JAVA_PID> Thread.print -l > thread-dump.txt
```

Search for:

```text
nid=0x4817
```

The matching Java stack shows what the CPU-intensive thread is doing.

## Step 4: Take repeated dumps

Take several dumps five to ten seconds apart:

```bash
for i in 1 2 3 4 5; do
  jcmd <JAVA_PID> Thread.print -l \
    > "thread-dump-$i.txt"
  sleep 5
done
```

One dump is only one instant. A thread repeatedly appearing in the same runnable stack is stronger evidence of the hotspot.

Oracle’s diagnostic tools support thread stack collection through `jcmd Thread.print`; thread dumps are useful for identifying loops, deadlocks, blocking, and thread contention. ([Oracle Docs][9])

---

## Step 5: Check whether GC is consuming CPU

```bash
jstat -gcutil <PID> 1000
```

Look for:

- Very frequent young collections
- Repeated full collections
- Heap remaining nearly full after GC
- Rapid object allocation
- Long GC pauses
- Promotion pressure

High CPU may be application computation, but it may also be the JVM repeatedly trying to reclaim memory.

## Step 6: Capture a Java Flight Recording

```bash
jcmd <PID> JFR.start \
  name=cpu-investigation \
  settings=profile \
  duration=5m \
  filename=/tmp/cpu-investigation.jfr
```

A JFR recording can include:

- CPU samples
- Allocation profiles
- Garbage collection
- Thread contention
- Locks
- Exceptions
- File and socket activity

Java Flight Recorder is integrated into the JVM and is intended for diagnostic and profiling data collection, including thread samples, lock profiles, and GC activity. ([Oracle Docs][10])

---

## Common high-CPU causes

- Infinite or inefficient loop
- Excessive serialization
- Regex backtracking
- Large collection processing
- Busy-spin retry
- Too many runnable threads
- Lock contention
- Frequent garbage collection
- Compression or encryption
- Excessive logging
- Repeated exception creation
- Hot Kafka message or partition
- Unbounded parallel processing

---

# B. High Memory Investigation

## Step 1: Determine which memory is growing

```text
Heap growing?
Native memory growing?
Thread count growing?
Direct buffers growing?
Metaspace growing?
Page cache or mapped files growing?
```

Check both:

```bash
free -h
ps -o pid,rss,vsz,%mem,cmd -p <PID>
jcmd <PID> GC.heap_info
```

## Step 2: Inspect object counts

```bash
jcmd <PID> GC.class_histogram \
  > /tmp/class-histogram.txt
```

Take multiple histograms:

```bash
jcmd <PID> GC.class_histogram \
  > /tmp/histogram-before.txt

sleep 300

jcmd <PID> GC.class_histogram \
  > /tmp/histogram-after.txt
```

Look for classes whose instance count and retained size continually increase.

Possible leak patterns include:

- Unbounded maps
- Cache entries
- Queued tasks
- HTTP session objects
- `ThreadLocal` values
- Listeners that are never removed
- Classloader retention
- Large byte arrays
- Buffered request or response bodies

## Step 3: Capture a heap dump

```bash
jcmd <PID> GC.heap_dump /var/dumps/app-heap.hprof
```

Analyze using tools such as:

- Eclipse Memory Analyzer
- Java Mission Control
- VisualVM
- Commercial APM memory profilers

A heap dump can be large and may significantly affect the JVM while it is generated. Oracle classifies `GC.heap_dump` as a high-impact diagnostic command. ([Oracle Docs][11])

## Step 4: Investigate native memory

When NMT is enabled:

```bash
jcmd <PID> VM.native_memory summary
```

Common categories include:

- Java Heap
- Class
- Thread
- Code
- GC
- Compiler
- Internal
- Symbol
- Native Memory Tracking

A large thread category may indicate too many threads or oversized stacks. A growing class category may suggest classloader leakage.

---

# Q4: How Do You Capture Heap and Thread Dumps?

## Thread dump with `jcmd`

Preferred approach:

```bash
jcmd <PID> Thread.print -l \
  > /var/dumps/thread-$(date +%Y%m%d-%H%M%S).txt
```

List available commands for that JVM:

```bash
jcmd <PID> help
```

Check command syntax:

```bash
jcmd <PID> help Thread.print
```

## Thread dump with `jstack`

```bash
jstack -l <PID> \
  > /var/dumps/thread-$(date +%Y%m%d-%H%M%S).txt
```

Modern Oracle guidance generally prefers `jcmd` over older `jstack`, `jmap`, and `jinfo` commands where the equivalent diagnostic command is available. ([Oracle Docs][12])

## Thread dump using a signal

On Linux:

```bash
kill -3 <PID>
```

or equivalently:

```bash
kill -QUIT <PID>
```

This does **not** normally terminate the Java process. It requests a thread dump, which is written to the JVM’s standard error destination—often the application or container log. Oracle documents `kill -QUIT` as a mechanism for requesting a thread dump on Linux and Unix systems. ([Oracle Docs][13])

---

## Heap dump with `jcmd`

```bash
jcmd <PID> GC.heap_dump \
  /var/dumps/heap-$(date +%Y%m%d-%H%M%S).hprof
```

Ensure:

- The destination has sufficient free space.
- File permissions protect sensitive data.
- The container can write to the directory.
- The dump will survive Pod replacement.
- The impact is acceptable during production traffic.

Heap dumps can contain passwords, tokens, customer data, request bodies, and other sensitive objects held in memory.

## Older `jmap` fallback

```bash
jmap -dump:format=b,file=/var/dumps/app.hprof <PID>
```

A live-object dump may trigger a full garbage collection:

```bash
jmap -dump:live,format=b,file=/var/dumps/app-live.hprof <PID>
```

Prefer `jcmd` when supported.

---

## Automatic heap dump on `OutOfMemoryError`

Configure the JVM:

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/dumps
```

Configure fatal-error output:

```bash
-XX:ErrorFile=/var/log/myapp/hs_err_pid%p.log
```

Example:

```bash
java \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/dumps \
  -XX:ErrorFile=/var/log/myapp/hs_err_pid%p.log \
  -jar application.jar
```

HotSpot can automatically create a heap dump when an allocation failure produces an `OutOfMemoryError`. ([Oracle Docs][14])

---

# Capturing Dumps in Docker

Find the Java PID:

```bash
docker exec my-container jcmd -l
```

Often Java is PID `1`:

```bash
docker exec my-container \
  jcmd 1 Thread.print -l
```

Heap dump:

```bash
docker exec my-container \
  jcmd 1 GC.heap_dump /tmp/app.hprof
```

Copy it to the host:

```bash
docker cp \
  my-container:/tmp/app.hprof \
  ./app.hprof
```

A minimal runtime image may not contain JDK diagnostic tools. Options include:

- Use a diagnostic image
- Add an ephemeral debugging container
- Attach from the host when permissions allow
- Build an operational image containing required tools

---

# Capturing Dumps in Kubernetes

Find the Java process:

```bash
kubectl exec \
  -n <namespace> \
  <pod-name> \
  -- jcmd -l
```

Thread dump:

```bash
kubectl exec \
  -n <namespace> \
  <pod-name> \
  -- jcmd 1 Thread.print -l \
  > thread-dump.txt
```

Heap dump inside the Pod:

```bash
kubectl exec \
  -n <namespace> \
  <pod-name> \
  -- jcmd 1 GC.heap_dump /tmp/app.hprof
```

Copy it locally:

```bash
kubectl cp \
  <namespace>/<pod-name>:/tmp/app.hprof \
  ./app.hprof
```

For automatic dumps, mount persistent or sufficiently sized storage:

```yaml
volumeMounts:
  - name: diagnostics
    mountPath: /var/dumps
```

Without persistent storage, a heap dump may disappear when the Pod is deleted.

---

# Production Investigation Runbook

```text
1. Record the incident time and affected service.

2. Preserve logs and previous container state.

3. Check host, container, and Kubernetes termination reasons.

4. Check CPU, memory, disk, network, and process limits.

5. Correlate the issue with deployments and configuration changes.

6. For high CPU:
   identify the process → identify hot thread → capture repeated dumps → JFR.

7. For high memory:
   compare RSS and heap → histogram → heap dump → native-memory analysis.

8. For a crash:
   collect hs_err, core dump, kernel logs, exit reason, and previous logs.

9. Check downstream systems:
   database, Redis, Kafka, external APIs, DNS, and connection pools.

10. Mitigate safely, then perform root-cause analysis.
```

---

# Interview-Ready Answers

## 1. How do you find the reason for a server crash?

> I first determine whether the host, container, JVM, or application failed. I preserve evidence before restarting, then inspect systemd logs, kernel OOM messages, container termination state, Kubernetes events and previous logs, JVM `hs_err_pid` files, heap dumps, and core dumps. I correlate the failure with resource graphs, deployments, configuration changes, and dependency failures.

## 2. How do you find server memory usage?

> At the host level, I use `free`, `/proc/meminfo`, `vmstat`, `top`, and process RSS/PSS information. For a container, I check cgroup or Kubernetes memory usage and limits. For Java, I separate heap from native memory using `jcmd GC.heap_info`, class histograms, GC metrics, and Native Memory Tracking, because process RSS includes thread stacks, metaspace, direct buffers, code cache, and native libraries in addition to heap.

## 3. How do you debug high JVM CPU?

> I identify the Java process, use `top -H` to find the hottest native thread, convert its thread ID to hexadecimal, and match it to the `nid` in repeated thread dumps. I also inspect GC activity and capture a Java Flight Recording to identify CPU samples, allocations, locks, and blocking operations.

## 4. How do you debug high memory?

> I first determine whether heap or native memory is growing. I compare process RSS with JVM heap usage, capture repeated class histograms, and take a heap dump for retained-object analysis. For off-heap growth, I use Native Memory Tracking when enabled and inspect thread count, direct buffers, metaspace, mapped files, and native libraries.

## 5. How do you capture heap and thread dumps?

> I use `jcmd <pid> Thread.print -l` for thread dumps and `jcmd <pid> GC.heap_dump <file>` for heap dumps. On Linux, `kill -3 <pid>` also requests a thread dump. I enable `HeapDumpOnOutOfMemoryError` for automatic heap capture, ensure sufficient secured disk space, and store dumps outside ephemeral container storage.

[1]: https://docs.oracle.com/javase/jp/13/troubleshoot/troubleshooting-guide.pdf?utm_source=chatgpt.com "Troubleshooting Guide"
[2]: https://www.freedesktop.org/software/systemd/man/journalctl.html?utm_source=chatgpt.com "journalctl"
[3]: https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html?utm_source=chatgpt.com "java"
[4]: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/?utm_source=chatgpt.com "Resource Management for Pods and Containers"
[5]: https://man7.org/linux/man-pages/man1/free.1.html?utm_source=chatgpt.com "free(1) - Linux manual page"
[6]: https://man7.org/linux/man-pages/man8/smem.8.html?utm_source=chatgpt.com "smem(8) - Linux manual page"
[7]: https://docs.oracle.com/en/java/javase/22/docs/specs/man/jcmd.html?utm_source=chatgpt.com "The jcmd Command"
[8]: https://docs.oracle.com/en/java/javase/21/troubleshoot/diagnostic-tools.html?utm_source=chatgpt.com "2 Diagnostic Tools - Java"
[9]: https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr006.html?utm_source=chatgpt.com "2.6 The jcmd Utility"
[10]: https://docs.oracle.com/en/java/javase/25/troubleshoot/diagnostic-tools.html?utm_source=chatgpt.com "2 Diagnostic Tools - Java"
[11]: https://docs.oracle.com/en/java/javase/24/docs/specs/man/jcmd.html?utm_source=chatgpt.com "The jcmd Command"
[12]: https://docs.oracle.com/en/java/javase/24/troubleshoot/diagnostic-tools.html?embed=1&utm_source=chatgpt.com "Diagnostic Tools"
[13]: https://docs.oracle.com/cd/E19424-01/820-4814/gewlb/index.html?utm_source=chatgpt.com "Using Java Tools With Directory Proxy Server 7.0"
[14]: https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/clopts001.html?utm_source=chatgpt.com "Java HotSpot VM Command-Line Options"
