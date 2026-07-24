# Advanced Questions — OAuth 2.0, JWT, RBAC, SSRF, mTLS, Secrets, and API Security

## Important Corrections

- **OAuth 2.0 is primarily an authorization framework.** OpenID Connect adds identity and authentication capabilities.
- Client Credentials is appropriate when a service acts on its **own identity**, not when it needs to preserve an end user’s delegated identity.
- OAuth access tokens do not have to be JWTs. They may be opaque and validated using token introspection.
- A signed JWT protects integrity but does not hide its claims. Sensitive information should not be placed in a normal signed JWT.
- mTLS authenticates workload identities and encrypts transport, but it does not replace application-level authorization.
- Kubernetes Secrets use Base64 encoding for representation; Base64 provides no confidentiality. Kubernetes recommends encryption at rest, least-privilege RBAC, restricted container access, and consideration of an external secret store. ([Kubernetes][1])
- In the OWASP API Security Top 10 2023, “Excessive Data Exposure” is incorporated into **Broken Object Property Level Authorization**, while “Lack of Rate Limiting” is represented more broadly by **Unrestricted Resource Consumption**. ([OWASP Foundation][2])

---

# Q51: Explain OAuth 2.0 Flows

OAuth 2.0 allows a client to obtain limited access to protected resources either on behalf of a user or on its own behalf. ([RFC Editor][3])

## Major OAuth flows

| Flow                         | Typical use                                                                |
| ---------------------------- | -------------------------------------------------------------------------- |
| Authorization Code with PKCE | Browser, mobile, SPA, and user-facing applications                         |
| Client Credentials           | Machine-to-machine communication                                           |
| Device Authorization         | Devices with limited input capability                                      |
| Refresh Token                | Obtain a new access token without repeating the full authorization process |

The old Implicit and Resource Owner Password Credentials approaches should not be selected for new systems. Current OAuth security guidance favors Authorization Code with PKCE for user-facing clients. PKCE protects against authorization-code interception. ([RFC Editor][4])

---

## Authorization Code with PKCE

Use this flow when a user is involved.

```text
User
  ↓
Client application
  ↓ authorization request + PKCE challenge
Authorization Server
  ↓ user authenticates and approves access
Authorization code
  ↓ code + PKCE verifier
Token endpoint
  ↓
Access token
```

The client sends a generated code challenge during authorization and proves possession of the corresponding verifier when exchanging the authorization code.

This protects a stolen authorization code from being exchanged by an attacker who does not possess the verifier. ([RFC Editor][4])

---

## Client Credentials flow

Use Client Credentials when a confidential service acts as itself.

```text
Order Service
    ↓ client_id + client authentication
Authorization Server
    ↓
Access token
    ↓
Inventory API
```

Example token request:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic <encoded-client-credentials>

grant_type=client_credentials&scope=inventory.read
```

Conceptual response:

```json
{
  "access_token": "access-token-value",
  "token_type": "Bearer",
  "expires_in": 300,
  "scope": "inventory.read"
}
```

RFC 6749 defines Client Credentials for clients acting on their own behalf or based on authorization arranged in advance. It should be used only by clients capable of securely holding their credentials. ([RFC Editor][3])

## Suitable use cases

- Order Service calling Inventory Service
- Reconciliation worker calling Payment Service
- Scheduled reporting job calling Reporting API
- Backend service accessing an internal platform API

## Unsuitable case

```text
User asks Order Service to access their private bank account
```

A Client Credentials token identifies the Order Service—not the user. When downstream authorization depends on the user, use a delegated user token, token exchange, or another explicitly designed delegation mechanism.

---

## JWT access token vs opaque token

### JWT access token

The resource server validates the token locally using trusted signing keys.

```text
Request with JWT
    ↓
Resource server validates:
signature
issuer
audience
expiry
not-before
scope
```

Advantages:

- No introspection call for every request
- Low authorization-server dependency
- Claims available locally

Trade-offs:

- Revocation is harder
- Claims may become stale until expiry
- Token contents are visible to anyone holding the token

### Opaque token

The resource server calls an introspection endpoint:

```text
Opaque token
    ↓
Authorization Server introspection endpoint
    ↓
active, scope, client, subject, expiry
```

RFC 7662 defines token introspection. Spring Security notes that opaque-token introspection is useful where authorization-server-managed token activity and revocation are important. ([RFC Editor][5])

---

## M2M security checklist

For service-to-service OAuth:

- Use a unique client identity for each workload.
- Grant narrow scopes.
- Validate the intended audience.
- Use short-lived access tokens.
- Rotate credentials.
- Prefer private-key JWT or mTLS client authentication over widely shared secrets for higher-security environments.
- Do not place client credentials in source code or container images.
- Do not blindly forward a service token to unrelated downstream services.
- Audit token issuance and sensitive API access.

OAuth mTLS can authenticate clients to the authorization server and bind access tokens to client certificates, reducing the usefulness of stolen bearer tokens. ([RFC Editor][6])

## Interview-ready answer

> I use Authorization Code with PKCE when a user is involved and Client Credentials when a confidential service acts on its own behalf. For microservice M2M communication, each workload receives a separate client identity with narrow scopes and audience restrictions. The access token may be a JWT validated locally or an opaque token validated through introspection. I use short token lifetimes, credential rotation, TLS, and local authorization enforcement at every resource service.

---

# Q52: How Does JWT Validation Work in Spring Security?

A Spring Boot API typically acts as an OAuth 2.0 Resource Server.

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

## Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://identity.example.com/realms/platform
          audiences:
            - order-api
```

Spring Security uses the issuer to discover or obtain trusted signing keys and validates the `iss` claim. Spring Boot also supports audience validation through the `audiences` property. ([Home][7])

## Security configuration

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/**")
                            .hasAuthority("SCOPE_orders.read")
                        .requestMatchers(HttpMethod.POST, "/api/orders/**")
                            .hasAuthority("SCOPE_orders.write")
                        .anyRequest()
                            .authenticated()
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(Customizer.withDefaults())
                )
                .build();
    }
}
```

Spring Security maps OAuth scopes to authorities such as:

```text
scope: orders.read
        ↓
authority: SCOPE_orders.read
```

---

## JWT validation process

```text
1. Extract Bearer token.
2. Parse the JOSE header and claims.
3. Select a trusted key.
4. Verify the signature.
5. Validate the expected signing algorithm.
6. Validate issuer.
7. Validate audience.
8. Validate expiration and not-before time.
9. Validate token type and required claims.
10. Convert scopes or roles into authorities.
11. Apply endpoint and domain authorization.
```

Signature validation alone is insufficient. RFC 8725 recommends algorithm verification, issuer validation, audience validation, cryptographic-input validation, and mutually exclusive validation rules for different JWT types. ([RFC Editor][8])

---

## Common JWT attack vectors

### 1. Unsigned or `alg: none` token

An insecure validator may accept a token without verifying a signature.

```json
{
  "alg": "none",
  "typ": "JWT"
}
```

OAuth JWT access tokens must be signed and must not use `none`. ([RFC Editor][9])

**Protection:**

- Require signatures.
- Pin permitted algorithms.
- Reject `none`.
- Use maintained JWT libraries rather than custom parsing.

---

### 2. Algorithm confusion

An attacker changes an asymmetric algorithm such as `RS256` to a symmetric algorithm such as `HS256` and attempts to make the server use a public key as an HMAC secret.

**Protection:**

- Configure an explicit algorithm allowlist.
- Do not trust the token’s `alg` value by itself.
- Match the expected algorithm with the expected key type.
- Separate keys and validators for different token types.

RFC 8725 requires applications to verify the algorithm and use appropriate validation rules rather than accepting arbitrary algorithms selected by the token. ([RFC Editor][10])

---

### 3. Untrusted `jku` or embedded `jwk`

A JWT header may reference a remote JSON Web Key Set:

```json
{
  "alg": "RS256",
  "jku": "https://attacker.example/jwks.json",
  "kid": "attacker-key"
}
```

If the application fetches keys from an arbitrary token-supplied URL, the attacker can provide their own verification key.

**Protection:**

- Configure the trusted issuer and JWK Set location on the server.
- Never accept arbitrary key URLs from token headers.
- Restrict key retrieval to expected HTTPS hosts.
- Apply SSRF protections to any remote key retrieval.
- Validate `kid` against the keys from the trusted issuer.

`jku` injection occurs when a server trusts an attacker-controlled key URL instead of restricting verification keys to trusted sources. ([PortSwigger][11])

---

### 4. `kid` injection or path traversal

An insecure implementation may use the `kid` value directly in:

- A file path
- SQL query
- Shell command
- Untrusted cache key

**Protection:**

- Treat `kid` as untrusted input.
- Resolve it only against a preloaded trusted key set.
- Do not concatenate it into file paths or SQL.

---

### 5. Missing issuer or audience validation

A valid token issued for another application may be accepted by the wrong API.

```text
Token audience: billing-api
Used against:   admin-api
```

**Protection:**

- Validate `iss`.
- Validate `aud`.
- Validate token purpose/type.
- Use separate audiences and scopes for different APIs.

RFC 8725 specifically recommends validating issuer and audience to prevent substitution and cross-JWT confusion. ([RFC Editor][8])

---

### 6. Token replay

A stolen bearer token can generally be replayed until it expires or is revoked.

**Protection:**

- Use short-lived tokens.
- Protect tokens in transit and storage.
- Avoid logging tokens.
- Consider sender-constrained tokens using mTLS or DPoP for sensitive systems.
- Use opaque tokens or revocation checks when immediate invalidation is necessary.

DPoP and OAuth mTLS are standards for sender-constraining access tokens so possession of the token alone is insufficient. ([RFC Editor][6])

---

### 7. Weak symmetric signing secret

A low-entropy HMAC secret may be brute-forced offline.

**Protection:**

- Use cryptographically random, high-entropy keys.
- Prefer centrally managed asymmetric signing for multi-service systems.
- Rotate keys safely.
- Never use human-memorable passwords as HMAC keys.

RFC 8725 requires sufficient key entropy and advises against using human-memorable passwords directly as MAC keys. ([RFC Editor][10])

## Interview-ready answer

> Spring Security extracts the Bearer token, verifies its signature using keys from a trusted issuer, validates issuer, audience, expiry, not-before, algorithm and required claims, and converts scopes or roles into authorities. I protect against unsigned tokens, algorithm confusion, untrusted `jku` or `jwk` headers, weak keys, token substitution and replay by pinning algorithms and trusted issuers, validating audience and token purpose, using short lifetimes, and never trusting token-selected verification keys.

---

# Q53: Authentication vs Authorization

## Authentication

Authentication answers:

> Who or what is making the request?

Examples:

- User authenticated through OIDC
- Service authenticated through Client Credentials
- Workload authenticated through mTLS
- Partner authenticated through an API key

## Authorization

Authorization answers:

> What is that authenticated identity allowed to do?

Examples:

- May read orders
- May refund payments
- May modify only its own account
- May access only one tenant
- May invoke only a specific internal service

```text
Authentication
    ↓ establish identity
Authorization
    ↓ evaluate permission
Business operation
```

---

## Role-Based Access Control

RBAC assigns permissions to roles and roles to identities.

```text
ROLE_CUSTOMER
├── orders:read-own
└── orders:create

ROLE_SUPPORT
├── orders:read
└── refunds:request

ROLE_ADMIN
├── orders:read
├── refunds:approve
└── users:manage
```

## Spring method security

```java
@Service
public class RefundService {

    @PreAuthorize(
        "hasAuthority('SCOPE_refunds.approve')"
    )
    public Refund approveRefund(String paymentId) {
        return performApproval(paymentId);
    }
}
```

Role example:

```java
@PreAuthorize("hasRole('ADMIN')")
public void disableUser(String userId) {
    // ...
}
```

---

## JWT authority conversion

Suppose the token contains:

```json
{
  "sub": "service-account-order-service",
  "scope": "orders.read orders.write",
  "roles": ["ORDER_OPERATOR"]
}
```

Spring can use the scopes directly as:

```text
SCOPE_orders.read
SCOPE_orders.write
```

For a custom `roles` claim, configure a `JwtAuthenticationConverter`.

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter roles =
            new JwtGrantedAuthoritiesConverter();

    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(roles);
    return converter;
}
```

---

## RBAC is not enough for object-level authorization

This check is insufficient:

```java
@PreAuthorize("hasAuthority('SCOPE_accounts.read')")
public Account getAccount(String accountId) {
    return accountRepository.findById(accountId);
}
```

It verifies that the caller may read accounts generally, but not whether the caller owns the requested account.

Use object-level authorization:

```java
@PreAuthorize(
    "@accountAuthorization.canRead(authentication, #accountId)"
)
public Account getAccount(String accountId) {
    return accountRepository.findById(accountId)
            .orElseThrow(AccountNotFoundException::new);
}
```

Object ownership and tenant isolation are essential because Broken Object Level Authorization is the first risk in the OWASP API Security Top 10 2023. ([OWASP Foundation][2])

---

## Authorization placement in microservices

```text
API Gateway
→ coarse authentication and route policy

Each microservice
→ local scope, role, tenant and object authorization

Database
→ constraints and optional row-level protection
```

Do not rely only on the gateway. Internal requests may bypass it, and the downstream service understands its own domain rules better.

## Policy engines

Systems with complex centralized policy may use:

- Open Policy Agent
- Cedar
- Casbin
- A custom authorization service

A policy engine supports decision consistency, but each service must still:

- Supply trustworthy input.
- Enforce the returned decision.
- Handle policy-service failure safely.
- Prevent stale or cached decisions from violating security.

## Identity propagation

Choose explicitly between:

### End-user delegation

The downstream service needs the user’s identity and permissions.

```text
User token or exchanged delegated token
```

### Service identity

The downstream service needs only the calling workload’s identity.

```text
Client Credentials token or mTLS workload identity
```

Blindly forwarding one powerful user token through every service increases exposure and can give downstream services unnecessary permissions.

## Interview-ready answer

> Authentication establishes who the caller is; authorization determines what that caller may do. I implement coarse route checks at the gateway but enforce scopes, roles, tenant boundaries and object ownership inside each service. RBAC handles broad permissions, while object-level and attribute-based checks protect individual resources. User identity is propagated only when delegation is required; otherwise, services use their own workload identity.

---

# Q54: How Do You Prevent SSRF?

Server-Side Request Forgery occurs when an attacker influences a server to make a request to a destination the attacker should not be able to access.

Example vulnerable API:

```http
POST /images/import
Content-Type: application/json

{
  "url": "http://169.254.169.254/latest/meta-data/"
}
```

The attacker attempts to use the application as a proxy to access:

- Cloud metadata endpoints
- Internal services
- Loopback interfaces
- Private network ranges
- Administrative endpoints
- Local files or non-HTTP protocols

OWASP API Security 2023 includes SSRF as API7. ([OWASP Foundation][2])

---

## Preferred defence: avoid arbitrary URLs

Instead of accepting:

```json
{
  "url": "https://anything-the-user-selects.example"
}
```

accept a constrained business identifier:

```json
{
  "provider": "trusted-image-provider",
  "imageId": "IMG-100"
}
```

The server constructs the trusted URL internally.

---

## Defence-in-depth controls

### 1. Allowlist destinations

Allow only required:

- Schemes
- Domains
- Ports
- Paths where practical

```text
Allowed scheme: https
Allowed host:   images.partner.example
Allowed port:   443
```

OWASP recommends an allowlist when the permitted destination applications are known. ([OWASP Cheat Sheet Series][12])

### 2. Block non-public addresses

Resolve both IPv4 and IPv6 addresses and reject:

- Loopback
- Link-local
- Private networks
- Multicast
- Unspecified addresses
- Reserved ranges
- Internal organization ranges
- Cloud metadata addresses

Do not block only the literal string `169.254.169.254`. An attacker may use:

- IPv6
- Alternate numeric IP formats
- DNS aliases
- Redirects
- DNS rebinding

### 3. Defend against DNS rebinding

```text
Validation resolution
→ public address

Connection-time resolution
→ internal address
```

Controls include:

- Resolve and validate every returned address.
- Use controlled DNS resolvers.
- Connect only to the validated address.
- Revalidate after redirects.
- Monitor allowlisted domains for unexpected private resolution.

OWASP warns that domain validation alone remains vulnerable to DNS pinning or rebinding and recommends validating resolved IP addresses as well. ([OWASP Cheat Sheet Series][12])

### 4. Disable or validate redirects

A trusted URL may redirect to:

```text
http://127.0.0.1/admin
```

Disable automatic redirects where possible. When redirects are required, validate every redirect target using the complete SSRF policy. ([OWASP Cheat Sheet Series][12])

### 5. Restrict protocols

Allow only:

```text
https
```

Reject:

```text
file:
ftp:
gopher:
jar:
dict:
ldap:
```

### 6. Apply egress controls

Use:

- Egress proxy
- Firewall rules
- Kubernetes NetworkPolicy
- Service mesh egress policy
- Cloud network controls

The application should not have network access to destinations it never needs.

### 7. Limit the fetched response

Apply:

- Connect and response timeouts
- Maximum response size
- Maximum redirect count
- Allowed content types
- Streaming rather than unbounded buffering
- Decompression limits

### 8. Do not forward sensitive credentials

Do not automatically forward:

- User Authorization headers
- Service tokens
- Cookies
- Internal API keys
- mTLS identities

to a user-selected destination.

## Interview-ready answer

> I avoid accepting arbitrary URLs whenever possible and use an allowlist of schemes, hosts and ports. I resolve and validate every IPv4 and IPv6 address, reject private, loopback, link-local and metadata destinations, defend against DNS rebinding, and disable or revalidate redirects. I also apply network-level egress controls, strict timeouts and response-size limits, and never forward internal credentials to the requested destination.

---

# Q55: Explain mTLS

Normal TLS authenticates the server to the client.

```text
Client
→ verifies server certificate
```

Mutual TLS authenticates both sides.

```text
Client verifies server certificate
Server verifies client certificate
```

mTLS provides:

- Transport encryption
- Integrity protection
- Server authentication
- Client or workload authentication

It does not by itself determine whether the authenticated service may approve refunds or access a specific account.

```text
mTLS
→ who is the calling workload?

Authorization policy
→ what may that workload do?
```

OAuth 2.0 also defines mTLS client authentication and certificate-bound tokens. ([RFC Editor][6])

---

## mTLS in Kubernetes using a service mesh

A service mesh can:

1. Issue workload identities and certificates.
2. Place proxies beside or around workloads.
3. Establish mTLS between proxies.
4. Rotate certificates.
5. Enforce workload-level policies.
6. Export security telemetry.

## Istio example

```yaml
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: default
  namespace: istio-system
spec:
  mtls:
    mode: STRICT
```

Istio automatically configures mTLS between participating proxies. Setting `STRICT` prevents workloads covered by the policy from accepting plaintext peer traffic. ([Istio][13])

## Authorization policy

mTLS should be combined with policy:

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-order-service
  namespace: inventory
spec:
  selector:
    matchLabels:
      app: inventory-service
  rules:
    - from:
        - source:
            principals:
              - cluster.local/ns/orders/sa/order-service
```

The certificate establishes the source workload identity; the authorization policy determines whether that workload may access Inventory Service.

---

## Certificate rotation

Short-lived workload certificates reduce the impact of certificate theft. A service mesh normally handles its workload identity lifecycle.

For custom application-managed certificates, cert-manager can issue and automatically renew Kubernetes `Certificate` resources according to their duration and renewal settings. ([cert-manager][14])

Do not assume that installing cert-manager alone automatically creates service-to-service mTLS. You still need:

- A trusted CA model
- Client and server TLS configuration
- Identity mapping
- Authorization rules
- Certificate distribution and reload
- Revocation or short lifetime
- Rotation monitoring

## Interview-ready answer

> mTLS authenticates both client and server using certificates while encrypting the connection. In Kubernetes, I normally use a service mesh such as Istio to issue workload identities, rotate certificates and establish mTLS transparently, then apply authorization policies based on service-account identity. mTLS proves the calling workload, but the service must still enforce scopes, roles, tenant and business authorization.

---

# Q56: Securely Store and Access Secrets in Kubernetes

## Secret hierarchy

A strong design is:

```text
External secret manager
        ↓ workload identity
Secret delivery mechanism
        ↓
Pod receives only required secrets
        ↓
Application reads secret
```

Possible external managers include:

- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault
- Google Secret Manager

Possible delivery mechanisms include:

- Vault Agent Injector
- External Secrets Operator
- Secrets Store CSI Driver
- Application retrieval through a secret-manager SDK

External Secrets Operator integrates Kubernetes with external secret-management systems such as Vault and major cloud secret stores. ([External Secrets][15])

---

## Kubernetes Secrets warning

Example:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: database-credentials
type: Opaque
data:
  username: YXBwX3VzZXI=
  password: c2VjcmV0LXZhbHVl
```

These values are only Base64 encoded.

```bash
echo 'c2VjcmV0LXZhbHVl' | base64 --decode
```

Kubernetes documentation states that Secrets are stored unencrypted in etcd by default unless encryption at rest is configured. It recommends encryption at rest, least-privilege RBAC, restricted container access and consideration of external secret stores. ([Kubernetes][1])

---

## Required controls

### 1. Enable encryption at rest

Protect Secret data stored in etcd.

### 2. Restrict RBAC

A service account should access only the secrets needed by its workload.

Avoid broad permissions such as:

```yaml
resources:
  - secrets
verbs:
  - "*"
```

### 3. Use workload identity

Prefer Kubernetes service accounts integrated with the cloud or Vault identity system over static cloud credentials stored inside the cluster.

### 4. Mount secrets only into required containers

A sidecar that does not need a database password should not receive it.

### 5. Prefer mounted files where rotation matters

Environment variables are copied into the process at startup and do not automatically update inside a running process.

Mounted secret files can be refreshed, but the application must:

- Detect the update.
- Reopen or reload the value.
- Recreate affected clients or pools safely.

### 6. Rotate secrets

Use overlapping credentials where necessary:

```text
Create credential B
→ deploy applications accepting B
→ verify all instances moved
→ revoke credential A
```

OWASP recommends least privilege, dynamic secrets where practical, and automated rotation of static secrets. ([OWASP Cheat Sheet Series][16])

### 7. Avoid secrets in unsafe locations

Never place secrets in:

- Git repositories
- Dockerfiles
- Container images
- Build arguments
- Plain ConfigMaps
- Application logs
- Exception messages
- Metrics labels
- Heap dumps shared without protection

### 8. Audit secret access

Track:

- Which workload accessed the secret
- Which version was retrieved
- When access occurred
- Rotation and revocation events
- Failed access attempts

---

## External Secrets example

```yaml
apiVersion: external-secrets.io/v1
kind: ExternalSecret
metadata:
  name: payment-provider-secret
  namespace: payment
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: production-secret-store
    kind: SecretStore
  target:
    name: payment-provider-secret
  data:
    - secretKey: client-secret
      remoteRef:
        key: production/payment-provider
        property: client-secret
```

External Secrets Operator fetches values from an external provider and materializes them according to the `ExternalSecret` definition. ([External Secrets][15])

## Important trade-off

When an external secret is synchronized into a Kubernetes Secret, the secret still exists in Kubernetes and requires Kubernetes encryption and RBAC controls.

A CSI-mounted or directly retrieved secret can avoid persisting the value as a normal Kubernetes Secret, depending on the chosen implementation.

## Interview-ready answer

> I keep long-lived secret material in a dedicated secret manager and let Pods authenticate using workload identity. Secrets are delivered through Vault Agent, External Secrets Operator, CSI or a provider SDK. When Kubernetes Secrets are used, I enable encryption at rest, enforce namespace-scoped least-privilege RBAC, mount values only into required containers, automate rotation, and ensure the application reloads changed credentials safely. Base64 encoding is never treated as encryption.

---

# Q57: What Is the OWASP API Security Top 10?

The current published API-specific edition is OWASP API Security Top 10 2023. ([OWASP Foundation][2])

| ID    | Risk                                            |
| ----- | ----------------------------------------------- |
| API1  | Broken Object Level Authorization               |
| API2  | Broken Authentication                           |
| API3  | Broken Object Property Level Authorization      |
| API4  | Unrestricted Resource Consumption               |
| API5  | Broken Function Level Authorization             |
| API6  | Unrestricted Access to Sensitive Business Flows |
| API7  | Server-Side Request Forgery                     |
| API8  | Security Misconfiguration                       |
| API9  | Improper Inventory Management                   |
| API10 | Unsafe Consumption of APIs                      |

---

## API1: Broken Object Level Authorization

Vulnerable request:

```http
GET /api/accounts/ACC-200
```

The API verifies that the caller is authenticated but does not verify that the caller owns `ACC-200`.

### Prevention

- Check ownership or tenancy for every object.
- Use server-derived identity rather than a user-supplied owner ID.
- Test horizontal and vertical authorization.
- Avoid predictable identifiers as the only defence.

---

## API2: Broken Authentication

Examples:

- Weak password recovery
- Missing brute-force protection
- Long-lived tokens
- Tokens in URLs
- Incorrect JWT validation
- Shared service credentials

### Prevention

- Centralize authentication.
- Use standard OAuth/OIDC libraries.
- Apply MFA where appropriate.
- Protect token storage.
- Rate-limit login and recovery.
- Rotate credentials.
- Validate JWTs completely.

---

## API3: Broken Object Property Level Authorization

Examples:

### Excessive response data

```json
{
  "customerId": "C-100",
  "name": "User",
  "passwordHash": "...",
  "internalRiskScore": 92
}
```

### Mass assignment

```http
PATCH /api/users/me
```

```json
{
  "displayName": "User",
  "role": "ADMIN"
}
```

### Prevention

- Use explicit request and response DTOs.
- Allowlist writable fields.
- Apply field-level authorization.
- Do not serialize entities directly.
- Do not rely on the client to hide sensitive fields.

---

## API4: Unrestricted Resource Consumption

Examples:

- No rate limit
- Unbounded page size
- Large file upload
- Expensive search filters
- Unlimited GraphQL depth
- SMS or email abuse
- No downstream timeout

### Prevention

- Rate and concurrency limits
- Request-size limits
- Pagination caps
- Query-cost limits
- Timeouts
- Quotas
- Bounded queues
- Financial-spending limits for paid providers

---

## API5: Broken Function Level Authorization

Example:

```http
DELETE /api/admin/users/U-100
```

A normal user can invoke an administrator endpoint because only authentication was checked.

### Prevention

- Deny by default.
- Apply role or scope checks.
- Enforce authorization inside the service.
- Test every method and route.
- Separate administrative interfaces where appropriate.

---

## API6: Unrestricted Access to Sensitive Business Flows

Examples:

- Automated ticket scalping
- OTP flooding
- Unlimited coupon redemption attempts
- Automated account creation
- Payment-card testing
- Inventory hoarding

The individual request may be valid, but automation abuses the business workflow.

### Prevention

- Per-user and per-device limits
- Behaviour analysis
- Step-up verification
- Business quotas
- Velocity checks
- Anti-automation controls

---

## API7: SSRF

A user-controlled URL causes the server to call internal or metadata endpoints.

### Prevention

- Destination allowlist
- IP and DNS validation
- Redirect validation
- Network egress controls
- Protocol restrictions
- Response limits

---

## API8: Security Misconfiguration

Examples:

- Exposed Actuator endpoints
- Default credentials
- Verbose stack traces
- Overly permissive CORS
- Missing TLS
- Unsecured cloud storage
- Unnecessary HTTP methods

### Prevention

- Secure defaults
- Automated configuration scanning
- Environment hardening
- Patch management
- Minimal exposed surface
- Production-safe error responses

---

## API9: Improper Inventory Management

Examples:

- Forgotten API versions
- Undocumented partner endpoints
- Test APIs exposed publicly
- Unknown service owners
- Unsupported older deployments

### Prevention

- Maintain an API inventory.
- Record owners and lifecycle states.
- Discover shadow APIs.
- Retire old versions.
- Track environments, hosts and schemas.

---

## API10: Unsafe Consumption of APIs

A service trusts data from a third-party API because it comes from a known provider.

Examples:

- Rendering provider HTML without sanitization
- Following provider redirects without validation
- Deserializing unsafe polymorphic data
- Trusting callback values without signature verification
- No timeout or response-size limit

### Prevention

- Treat third-party data as untrusted.
- Validate schemas.
- Verify webhook signatures.
- Apply SSRF and redirect controls.
- Limit response size.
- Use timeouts and circuit breakers.
- Escape output according to context.

## Interview-ready answer

> The OWASP API Security Top 10 2023 focuses on object and function authorization, authentication, property-level exposure, resource consumption, sensitive workflow abuse, SSRF, configuration, API inventory and unsafe third-party consumption. For microservices, BOLA, broken authentication, resource exhaustion and unsafe service-to-service trust are particularly important because every independently deployed API creates another authorization and attack boundary.

---

# Q58: API Key Management for a Public Platform

An API key identifies a calling application or account. It should not be treated as a substitute for user-delegated OAuth when individual user identity and consent are required.

## Secure API key structure

Use two parts:

```text
public prefix / key ID
+
high-entropy secret
```

Example:

```text
pk_live_8F2A.kQ7m...random-secret...
```

The prefix helps locate the database record without storing the full secret in plaintext.

---

## Key generation

Generate at least 128 bits of cryptographically secure randomness.

```java
public final class ApiKeyGenerator {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private ApiKeyGenerator() {
    }

    public static String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
```

Display the full key once:

```text
Key created successfully.

pk_live_8F2A.kQ7m...

This secret will not be shown again.
```

---

## Storage

Store:

```text
key ID or prefix
key verification hash/HMAC
owner
status
scopes
created time
expiry
last-used time
rate-limit plan
allowed environments
```

Example schema:

```sql
CREATE TABLE api_key (
    key_id UUID PRIMARY KEY,
    key_prefix VARCHAR(32) NOT NULL UNIQUE,
    key_digest VARCHAR(128) NOT NULL,
    owner_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    scopes TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP
);
```

Do not store the complete API secret in plaintext.

For high-entropy API keys, a keyed HMAC or cryptographic digest can support efficient verification. A server-side pepper stored separately from the database limits the usefulness of a database-only compromise.

---

## Request usage

Prefer a header:

```http
Authorization: ApiKey pk_live_8F2A.kQ7m...
```

or:

```http
X-API-Key: pk_live_8F2A.kQ7m...
```

Do not place API keys in query strings:

```http
GET /api/orders?api_key=secret
```

URLs commonly appear in:

- Browser history
- Proxy logs
- Access logs
- Referrer headers
- Monitoring systems

Always use HTTPS.

---

## Verification flow

```text
1. Extract key from header.
2. Parse prefix.
3. Retrieve candidate key record.
4. Compute digest using the supplied secret.
5. Compare in constant time.
6. Verify status and expiry.
7. Verify scopes.
8. Apply tenant and object authorization.
9. Apply rate and concurrency limits.
10. Record a safe audit event.
```

---

## Scopes

Avoid one unrestricted key:

```text
orders.read
orders.write
payments.create
reports.read
```

Create separate keys for:

- Production and testing
- Different applications
- Different integrations
- Different permission sets

The principle of least privilege and automated credential rotation are central secrets-management practices. ([OWASP Cheat Sheet Series][16])

---

## Rate limiting

Apply limits by:

- API key
- Account or tenant
- Route
- Operation cost
- IP as a secondary signal
- Global platform capacity

```text
Key A:
1,000 requests/minute

Key B:
100 payment submissions/minute

Report endpoint:
10 expensive jobs/hour
```

Do not rely only on IP-based limits because many clients may share an IP, while one client may use many IPs.

---

## Rotation without downtime

```text
1. Create replacement key B.
2. Allow A and B during a bounded overlap.
3. Update the client to use B.
4. Verify B is active.
5. Revoke A.
```

Support at least two active keys per integration during rotation where operationally necessary. OWASP recommends automating static-secret rotation where possible. ([OWASP Cheat Sheet Series][16])

---

## Revocation

Support immediate revocation for:

- Compromise
- Employee departure
- Customer account closure
- Abuse
- Expired integration
- Scope reduction

Cache validation results only for a short period or provide a revocation-invalidation mechanism.

---

## Audit logs

Record:

- Key ID or prefix—not the full secret
- Owner
- Operation
- Timestamp
- Source context
- Result
- Rate-limit decision
- Administrative creation, rotation and revocation

Never log the complete key.

## Interview-ready answer

> I generate high-entropy API keys with a public prefix and secret component, display the full key once, and store only a verification digest or HMAC. Each key has an owner, scopes, environment, expiry and rate-limit policy. Requests use HTTPS and a header rather than a query parameter. I support overlapping-key rotation, immediate revocation, per-key quotas, constant-time verification and audit logs that include only the key ID or prefix.

---

# Quick Interview Cheat Sheet

| Topic                     | Key point                                        |
| ------------------------- | ------------------------------------------------ |
| OAuth 2.0                 | Authorization framework                          |
| OIDC                      | Authentication and identity layer                |
| Authorization Code + PKCE | User-facing clients                              |
| Client Credentials        | Service acts as itself                           |
| JWT token                 | Local signature and claim validation             |
| Opaque token              | Authorization-server introspection               |
| Authentication            | Who is calling?                                  |
| Authorization             | What may the caller do?                          |
| RBAC                      | Permissions grouped into roles                   |
| BOLA                      | Missing object ownership authorization           |
| mTLS                      | Mutual workload authentication and encryption    |
| SSRF                      | Server makes attacker-influenced network request |
| Kubernetes Secret         | Base64 is not encryption                         |
| External secret manager   | Central storage, rotation and audit              |
| API key storage           | Prefix plus digest/HMAC                          |
| API key rotation          | Overlap, migrate, revoke                         |
| Gateway authorization     | Coarse control only                              |
| Service authorization     | Local domain and object enforcement              |

# Senior Interview Summary

> For user-facing applications, I use Authorization Code with PKCE. For machine-to-machine calls, I use Client Credentials with a separate client identity, narrow scopes, short-lived tokens and audience restrictions. Each resource server validates JWT signature, issuer, audience, time claims and algorithm using Spring Security, and it enforces authorization locally rather than trusting only the gateway.
>
> I combine OAuth with mTLS where stronger workload authentication or sender-constrained tokens are required. In Kubernetes, a service mesh can automate workload certificate issuance and rotation, while authorization policies determine which authenticated workloads may communicate.
>
> Secrets remain in a dedicated manager and are delivered using workload identity, Vault, External Secrets or CSI. Kubernetes Secrets are encrypted at rest and protected by least-privilege RBAC when used. Public API keys are high entropy, stored only as verification digests, scoped, rate-limited, rotated and audited.
>
> I treat all user-supplied URLs and third-party API data as untrusted. SSRF protection combines destination allowlists, IPv4 and IPv6 validation, redirect and DNS-rebinding defence, egress restrictions and bounded network clients. API authorization includes route, function, tenant, property and object-level checks to address the major risks identified by the OWASP API Security Top 10.

[1]: https://kubernetes.io/docs/concepts/configuration/secret/?utm_source=chatgpt.com "Secrets"
[2]: https://owasp.org/API-Security/editions/2023/en/0x11-t10/?utm_source=chatgpt.com "OWASP Top 10 API Security Risks – 2023"
[3]: https://www.rfc-editor.org/info/rfc6749/?utm_source=chatgpt.com "RFC 6749: The OAuth 2.0 Authorization Framework"
[4]: https://www.rfc-editor.org/info/rfc7636/?utm_source=chatgpt.com "RFC 7636: Proof Key for Code Exchange by OAuth Public ..."
[5]: https://www.rfc-editor.org/info/rfc7662/?utm_source=chatgpt.com "RFC 7662: OAuth 2.0 Token Introspection"
[6]: https://www.rfc-editor.org/info/rfc8705/?utm_source=chatgpt.com "RFC 8705: OAuth 2.0 Mutual-TLS Client Authentication ..."
[7]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server JWT"
[8]: https://www.rfc-editor.org/info/rfc8725/?utm_source=chatgpt.com "RFC 8725: JSON Web Token Best Current Practices"
[9]: https://www.rfc-editor.org/info/rfc9068/?utm_source=chatgpt.com "JSON Web Token (JWT) Profile for OAuth 2.0 Access Tokens"
[10]: https://www.rfc-editor.org/rfc/rfc8725.pdf?utm_source=chatgpt.com "RFC 8725: JSON Web Token Best Current Practices"
[11]: https://portswigger.net/web-security/jwt/lab-jwt-authentication-bypass-via-jku-header-injection?utm_source=chatgpt.com "Lab: JWT authentication bypass via jku header injection"
[12]: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html?utm_source=chatgpt.com "Server-Side Request Forgery Prevention Cheat Sheet"
[13]: https://istio.io/latest/docs/tasks/security/authentication/authn-policy/?utm_source=chatgpt.com "Authentication Policy"
[14]: https://cert-manager.io/docs/usage/certificate/?utm_source=chatgpt.com "Certificate resource - cert-manager Documentation"
[15]: https://external-secrets.io/?utm_source=chatgpt.com "External Secrets Operator: Introduction"
[16]: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html?utm_source=chatgpt.com "Secrets Management Cheat Sheet"
