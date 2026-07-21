# Advanced Questions

## Q51: Explain OAuth 2.0 flows. Which flow should a microservices API use for M2M communication?
Client Credentials flow; JWT access tokens; scope-based authorization; token introspection.

## Q52: How does JWT validation work in a Spring Security microservice? What are common JWT attack vectors?
Signature verification, expiry, alg=none attack, key confusion (HS256 vs RS256), jku injection.

## Q53: What is the difference between authentication and authorization? How do you implement RBAC in microservices?
AuthN = who; AuthZ = what; JWT claims, OPA/Casbin for policy, propagate identity downstream.

## Q54: How do you prevent SSRF attacks in a microservice that fetches external URLs?
Allowlist domains, block 169.254.x.x metadata ranges, use separate egress proxy.

## Q55: Explain mTLS. How do you implement service-to-service mTLS in a Kubernetes cluster?
Mutual certificate exchange, Istio/Linkerd auto-mTLS, certificate rotation with cert-manager.

## Q56: How do you securely store and access secrets in a microservice running in Kubernetes?
Vault Agent Injector, Kubernetes Secrets (base64 not encrypted!), ESO, sealed secrets.

## Q57: What is the OWASP API Security Top 10? Which ones are most critical for microservices?
Broken Object Level AuthZ (#1), Excessive Data Exposure, Lack of Rate Limiting — give examples.

## Q58: How do you implement API key management for a public-facing microservices platform?
Hash keys in DB, prefix for lookup, rate limit per key, rotation strategy, audit logs.
