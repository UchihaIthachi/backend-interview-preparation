# Advanced Questions

## Q73: What is the difference between Deployment, StatefulSet, and DaemonSet in Kubernetes?
Stateless vs ordered/stable identity vs one-per-node; microservices usually use Deployments.

## Q74: Explain blue-green vs canary deployments. How do you implement them with Kubernetes?
Blue-green = instant switch; Canary = gradual traffic shift; Argo Rollouts, Flagger, Istio.

## Q75: How do you configure resource requests and limits for a Java microservice pod?
JVM heap + off-heap + metaspace; -XX:MaxRAMPercentage; CPU throttling vs OOMKilled.

## Q76: What is HPA vs VPA vs KEDA? When would you use KEDA over HPA?
HPA = CPU/mem; VPA = rightsizing; KEDA = event-driven (Kafka lag, queue depth scaling).

## Q77: How do you handle graceful shutdown in a Spring Boot microservice running in Kubernetes?
server.shutdown=graceful, SIGTERM handler, preStop lifecycle hook, terminationGracePeriod.

## Q78: What is a Service Mesh? Compare Istio vs Linkerd for microservices.
mTLS, traffic management, observability at sidecar; Linkerd = simpler/Rust; Istio = powerful.

## Q79: How do you implement zero-downtime rolling updates for a stateful Java microservice?
PodDisruptionBudget, maxUnavailable=0, readiness gates, DB backward-compatible migrations.

## Q80: Explain Kubernetes Network Policies. How do you restrict inter-service communication?
Default deny-all; allow only specific pods/namespaces; label selectors; ingress/egress rules.
