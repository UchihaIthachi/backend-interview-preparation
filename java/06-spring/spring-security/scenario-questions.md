# Scenario-Based Questions — Spring Security, JWT, OAuth 2.0, and API Security

The repeated JWT, OAuth 2.0, token-expiration, and role-based access-control questions are consolidated below.

---

# Scenario 1: Your API is publicly exposed. How would you secure it?

I would apply **defence in depth** rather than relying on one control such as JWT validation or an API gateway.

## Security layers

```text
Internet
   ↓
TLS / HTTPS
   ↓
WAF, rate limiting, request-size limits
   ↓
API gateway or ingress
   ↓
Spring Security filter chain
   ├── Authentication
   ├── Authorization
   ├── CORS and CSRF policy
   └── Security headers
   ↓
Input validation
   ↓
Business and object-level authorization
   ↓
Parameterized database access
   ↓
Encrypted and access-controlled data
```

## Main controls

### 1. Require HTTPS

All credentials, API keys, access tokens, and sensitive payloads must travel over HTTPS. For highly privileged service-to-service endpoints, mutual TLS may provide an additional control. ([OWASP Cheat Sheet Series][1])

### 2. Authenticate every protected request

Use an established identity provider and configure the Spring Boot service as an OAuth 2.0 resource server.

Possible token types include:

- JWT access tokens
- Opaque access tokens validated through introspection

Spring Security supports both models. ([Home][2])

### 3. Deny access by default

Public endpoints should be explicitly listed. Everything else should require authentication or a specific authority.

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/public/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/orders/**"
                        ).hasAuthority("SCOPE_orders.read")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/orders/**"
                        ).hasAuthority("SCOPE_orders.write")
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}
```

Authorization should use least privilege, deny by default, and validate permissions on every request. ([OWASP Cheat Sheet Series][3])

The example disables CSRF only because it assumes a bearer-token-only API where the browser does not automatically attach authentication credentials. Cookie-authenticated applications generally still require CSRF protection; Spring Security enables it by default for unsafe methods. ([Home][4])

### 4. Validate tokens completely

Validate:

- Signature
- Trusted algorithm
- Issuer
- Audience
- Expiration
- Not-before time
- Required scopes or roles

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://identity.example.com/realms/platform
          audiences: orders-api
```

Spring Boot can use `issuer-uri` to discover verification keys and validate the issuer, while its `audiences` property validates that the token was intended for the resource server. ([Home][5])

### 5. Enforce object-level authorization

Endpoint-level security alone is insufficient.

A user with `orders.read` should not automatically be able to read every order.

```java
@PreAuthorize(
    "hasRole('ADMIN') or " +
    "@orderAuthorization.canRead(#orderId, authentication)"
)
public OrderResponse findOrder(long orderId) {
    return orderService.find(orderId);
}
```

This prevents horizontal privilege escalation such as changing:

```text
GET /orders/100
```

to:

```text
GET /orders/101
```

and accessing another user’s data.

### 6. Protect availability

Configure:

- Rate limits
- Request and upload-size limits
- Connection timeouts
- Bounded executor queues
- Database connection-pool limits
- Pagination and maximum page size
- Circuit breakers for downstream dependencies

### 7. Reduce information disclosure

Do not return:

- Stack traces
- SQL statements
- Internal hostnames
- Class names
- File-system paths
- Credentials
- Token-validation details

Return a stable external error response and log the internal cause securely.

### Interview-ready answer

> I secure a public API using defence in depth: HTTPS, centralized authentication, local authorization in every service, deny-by-default rules, token issuer and audience validation, object-level permission checks, parameterized queries, input validation, rate limiting, secure error responses, protected logs, dependency scanning, and continuous monitoring. I do not rely only on the API gateway because each service must enforce its own business authorization.

---

# Scenario 2: How do you prevent SQL injection?

SQL injection occurs when untrusted input is concatenated into executable SQL.

## Vulnerable example

```java
String sql =
        "SELECT * FROM users WHERE username = '"
        + username
        + "'";
```

An attacker may submit input that changes the query’s meaning.

## Use parameterized queries

### Spring Data derived query

```java
Optional<UserEntity> findByUsername(String username);
```

### JPQL with parameters

```java
@Query("""
    select u
    from UserEntity u
    where u.username = :username
    """)
Optional<UserEntity> findByUsername(
        @Param("username") String username
);
```

### JDBC prepared statement

```java
String sql =
        "SELECT id, username FROM users WHERE username = ?";

return jdbcTemplate.queryForObject(
        sql,
        userRowMapper,
        username
);
```

Parameterized queries keep the SQL structure separate from supplied values. OWASP recommends stopping dynamic query construction through string concatenation and using parameterized statements as the primary defence. ([OWASP Cheat Sheet Series][6])

## Dynamic identifiers require allow-lists

Values can be bound as parameters, but identifiers such as column names generally cannot.

Unsafe:

```java
String sql =
        "SELECT * FROM orders ORDER BY " + sortColumn;
```

Use a fixed mapping:

```java
private static final Map<String, String> SORT_COLUMNS =
        Map.of(
                "createdAt", "created_at",
                "status", "status",
                "total", "total_amount"
        );

String databaseColumn = Optional
        .ofNullable(SORT_COLUMNS.get(requestedSort))
        .orElseThrow(() ->
                new IllegalArgumentException("Invalid sort field")
        );
```

## Additional controls

- Give the application database account only required privileges.
- Do not connect using a schema owner or database administrator account.
- Validate expected type, length, range, and format.
- Avoid exposing raw database exceptions.
- Review native queries and dynamic specifications carefully.
- Test injection payloads in automated security testing.
- Use database constraints as a final integrity layer.

Input validation is useful, but it does not replace query parameterization. ([OWASP Cheat Sheet Series][6])

### Interview-ready answer

> I prevent SQL injection by never concatenating user input into SQL. I use Spring Data repositories, named JPQL parameters, or prepared statements. For dynamic identifiers such as sort columns, I map client values through a fixed allow-list. I also use a least-privilege database account, validate input, hide database errors, and test native or dynamically generated queries during security review.

---

# Scenario 3: How do you protect an API and web application against XSS?

Cross-site scripting occurs when attacker-controlled content is interpreted as executable browser content.

A JSON API response does not execute JavaScript by itself. The risk appears when a browser application places that value into HTML, JavaScript, CSS, or a URL without the correct contextual protection.

## 1. Encode output for its context

For a server-rendered Thymeleaf page:

```html
<p th:text="${comment}"></p>
```

`th:text` escapes the value.

This should not be used with untrusted input unless it has been safely sanitized:

```html
<p th:utext="${comment}"></p>
```

## 2. Avoid dangerous frontend APIs

Avoid inserting untrusted content through:

```javascript
element.innerHTML = userContent;
```

Use safe text assignment:

```javascript
element.textContent = userContent;
```

Modern frameworks provide useful automatic escaping, but bypass functions such as raw HTML rendering can reintroduce XSS. OWASP emphasizes that no single control prevents every XSS variant and recommends contextual encoding, safe framework usage, and sanitization where HTML is intentionally supported. ([OWASP Cheat Sheet Series][7])

## 3. Sanitize intentionally supported HTML

When users are allowed to submit rich text, output encoding would display the tags rather than render them. In that case, use a maintained HTML sanitizer with a strict allow-list.

Allow only required elements and attributes, for example:

```text
Allowed tags:
p, strong, em, ul, ol, li

Disallowed:
script
iframe
event-handler attributes
javascript: URLs
```

## 4. Add Content Security Policy

A restrictive Content Security Policy can reduce the impact of an encoding mistake.

Example policy:

```text
default-src 'self';
script-src 'self';
object-src 'none';
base-uri 'none';
frame-ancestors 'none';
```

CSP is a defence-in-depth control, not a replacement for correct output handling.

## 5. Validate URLs and uploaded content

Validate:

- URL schemes
- File types
- MIME types
- SVG content
- Markdown-to-HTML rendering
- User-controlled redirects

## 6. Distinguish XSS from CORS and CSRF

```text
XSS:
Attacker-controlled content executes in the browser.

CSRF:
A browser is tricked into sending an authenticated request.

CORS:
A browser policy controlling which origins may read responses.
```

CORS does not sanitize output and does not replace authentication.

### Interview-ready answer

> I prevent XSS primarily through contextual output encoding and safe frontend rendering. I avoid raw HTML APIs, sanitize rich HTML with a strict allow-list, validate dangerous URL schemes and uploads, and deploy a restrictive Content Security Policy as defence in depth. Input validation alone is not sufficient because output must be protected for the context in which it is rendered.

---

# Scenario 4: How would you implement JWT-based authentication?

For a production system, I would normally use an identity provider rather than implementing password validation, token issuance, refresh-token storage, key rotation, and revocation independently inside every API.

## Architecture

```text
User or client
      ↓
Authorization server / identity provider
      ↓ issues access token
Client
      ↓ Authorization: Bearer <token>
Spring Boot resource server
      ↓
JWT validation
      ↓
Authentication stored in SecurityContext
      ↓
Authorization rules evaluated
```

## Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>
        spring-boot-starter-oauth2-resource-server
    </artifactId>
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
          audiences: order-service
```

## Security filter chain

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**")
                        .permitAll()
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}
```

With resource-server configuration, Spring Security processes bearer tokens, verifies the JWT with trusted public keys, and converts validated claims into an authenticated principal. ([Home][5])

## Important distinction

```text
Access token:
Sent to the resource API.

ID token:
Consumed by the OIDC client to understand the login.

Refresh token:
Sent only to the authorization server to obtain new tokens.
```

Do not use an ID token as an API access token, and do not send refresh tokens to ordinary resource services.

### Interview-ready answer

> I implement JWT authentication by using an authorization server to issue access tokens and configuring each Spring Boot API as an OAuth 2.0 resource server. Spring Security validates the token’s signature, issuer, lifetime, audience, and authorities before creating the authenticated security context. The application remains stateless, while refresh-token issuance, rotation, revocation, and signing-key management stay with the authorization server.

---

# Scenario 5: Users are randomly logged out. What could be wrong?

“Random logout” usually means authentication state is being lost or rejected under particular timing, browser, token, or server conditions.

## Likely causes

### 1. Access token expiration

The frontend may not refresh the token before `exp`, or it may keep sending the expired token.

Check:

- Token issue time
- Expiration time
- Client clock
- Server clock
- Actual time of the `401`

### 2. Clock differences between systems

A token may appear valid on one machine and expired or not-yet-valid on another.

Verify time synchronization across:

- Identity provider
- API instances
- Gateway
- Client devices

### 3. Refresh-token rotation race

Two browser tabs may attempt refresh simultaneously:

```text
Tab A uses refresh token RT1
→ RT1 invalidated
→ RT2 issued

Tab B also sends RT1
→ replay detected
→ token family may be revoked
```

Use a single-flight refresh mechanism so only one refresh request runs at a time.

### 4. Refresh token is not persisted correctly

Possible causes include:

- Token stored only in memory
- Browser refresh clears state
- Mobile application loses secure-storage state
- Cookie path or domain is incorrect
- `SameSite` settings prevent the cookie from being sent
- Cookie expires before the refresh token does

### 5. Different configuration between application instances

One pod may use:

- A different issuer
- A different audience
- An old public key
- A different environment
- Incorrect clock configuration

Correlate failures with pod or instance ID.

### 6. Signing-key rotation problems

The identity provider may rotate keys while a service has stale JWK information or cannot retrieve the updated key set.

Inspect:

- JWT `kid`
- Current JWK set
- Network access to the identity provider
- Decoder cache and refresh behaviour

### 7. Token overwritten by another user or tab

Poor client-side storage keys may cause one account or environment to overwrite another account’s token.

### 8. Incorrect handling of API errors

The frontend may treat every `401`, `403`, `429`, `500`, or network failure as “log the user out.”

Correct behaviour:

```text
401:
Authentication is missing or invalid.
Attempt controlled refresh when appropriate.

403:
User is authenticated but lacks permission.
Do not log out automatically.

5xx or network timeout:
Service failure.
Do not delete authentication state.
```

## Diagnostic process

Capture:

- Access-token `jti` or a safe hash—not the full token
- `iss`, `aud`, `iat`, `nbf`, and `exp`
- Response code
- Identity-provider response
- Refresh attempt count
- Browser or mobile version
- Pod ID
- Trace ID
- Current clocks
- JWT `kid`

Never write the complete token into logs.

### Interview-ready answer

> I first determine whether the logout follows a `401`, refresh failure, browser-storage loss, or client-side error handling. I compare token expiry and clock values, inspect refresh-token rotation, verify cookie and storage settings, correlate failures with specific pods, and check signing-key rotation. I also ensure the client does not treat `403`, network failures, or server errors as authentication failures.

---

# Scenario 6: How would you implement authentication and authorization?

## Authentication

Authentication establishes the identity of the caller.

Examples:

- OIDC user login
- Username and password
- Client credentials
- Mutual TLS
- API credentials

## Authorization

Authorization decides whether that identity may perform a particular action.

```text
Authentication:
Who are you?

Authorization:
Can you perform this action on this resource?
```

## Recommended design

### Central authentication

Use an identity provider for:

- User login
- MFA
- Password policies
- Account recovery
- Token issuance
- Key rotation
- Session and refresh-token management

### Local authorization

Each service enforces:

- Required scopes
- Required roles
- Tenant boundary
- Resource ownership
- Resource state
- Business rules

OWASP recommends centralized identity with access-control decisions enforced locally at REST endpoints, as well as validating authorization on every request. ([OWASP Cheat Sheet Series][1])

---

# Scenario 7: How would you implement OAuth 2.0 authorization?

The correct OAuth grant depends on the client.

## User-facing web or mobile application

Use:

```text
Authorization Code
+
PKCE
```

Flow:

```text
1. Client redirects user to authorization server.
2. User authenticates.
3. Authorization server returns an authorization code.
4. Client exchanges the code and PKCE verifier.
5. Authorization server issues access and ID tokens.
6. Client calls the API using the access token.
```

## Service-to-service communication

Use client credentials when the service acts on its own behalf.

```text
Order service
    ↓ authenticates as OAuth client
Authorization server
    ↓ access token with inventory.reserve scope
Order service
    ↓
Inventory API
```

Spring Security’s OAuth client support includes authorization-code, refresh-token, client-credentials, JWT-bearer, and token-exchange capabilities. ([Home][8])

## Authorization rules

Use narrow scopes:

```text
orders.read
orders.write
inventory.reserve
payments.authorize
```

Avoid issuing one broad scope such as:

```text
full_access
```

### Interview-ready answer

> For user-facing clients, I use the authorization-code flow with PKCE and OIDC for authentication. For service-to-service calls, I use client credentials with least-privilege scopes. The APIs validate access tokens as resource servers and enforce both token permissions and business-level access rules.

---

# Scenario 8: How would you implement role-based access control?

RBAC assigns permissions to roles and roles to users.

```text
User
  ↓
Role
  ↓
Permission
  ↓
Resource action
```

## Example permission matrix

| Role          |     Read orders |  Modify orders | Refund | Manage users |
| ------------- | --------------: | -------------: | -----: | -----------: |
| Customer      |        Own only | Own draft only |     No |           No |
| Support       | Assigned orders |        Limited |     No |           No |
| Finance       |            Read |             No |    Yes |           No |
| Administrator |             Yes |            Yes |    Yes |          Yes |

## Endpoint authorization

```java
.requestMatchers("/api/admin/**")
.hasRole("ADMIN")
```

## Method authorization

```java
@PreAuthorize(
    "hasRole('FINANCE') and " +
    "@refundPolicy.canRefund(#orderId, authentication)"
)
public RefundResponse refund(long orderId) {
    return refundService.refund(orderId);
}
```

## Do not rely only on roles

RBAC may answer:

```text
Is the caller a SUPPORT user?
```

It does not automatically answer:

```text
Is this support user assigned to this customer?
Does the resource belong to the caller's tenant?
Is the order in a refundable state?
```

Combine RBAC with:

- Attribute-based controls
- Tenant checks
- Ownership checks
- Relationship-based controls
- Resource-state validation

OWASP recommends least privilege and notes that attribute- or relationship-based controls may be more suitable than pure RBAC for fine-grained decisions. ([OWASP Cheat Sheet Series][3])

## Operational controls

- Review access periodically.
- Remove unused roles.
- Prevent privilege accumulation.
- Separate administrative duties.
- Record role changes in an audit log.
- Test authorization failures.
- Default new endpoints to denied access.

---

# Scenario 9: How would you handle access-token expiration?

Access-token expiration should be expected, not treated as an exceptional system failure.

## Client flow

```text
API request
   ↓
401 because access token expired
   ↓
Client performs one controlled refresh
   ├── Refresh succeeds → retry original request once
   └── Refresh fails → require login
```

## Important rules

- Keep access tokens relatively short-lived.
- Protect refresh tokens more strongly.
- Rotate refresh tokens where supported.
- Prevent simultaneous refresh attempts.
- Do not retry refresh indefinitely.
- Do not send refresh tokens to resource APIs.
- Do not automatically refresh after `403`.
- Preserve idempotency when retrying the original request.
- Revoke refresh tokens during logout or account compromise.

## Example frontend control flow

```text
if response == 401 and request has not been retried:
    acquire refresh lock
    refresh token
    retry request once
else:
    return error
```

For stronger centralized revocation requirements, opaque access tokens with introspection may be preferable because the authorization server can report whether a token is currently active. ([Home][9])

---

# Scenario 10: How would you secure sensitive data in APIs?

Start by minimizing the data that exists.

```text
Best protected sensitive field:
A field the system never collected or stored.
```

OWASP recommends minimizing sensitive information before deciding how to encrypt it. ([OWASP Cheat Sheet Series][10])

## Data in transit

Use:

- HTTPS
- Valid certificate verification
- Internal TLS where required
- Mutual TLS for privileged integrations
- No sensitive data in URLs

Do not place secrets in query strings:

```text
GET /reset-password?token=secret
```

URLs may be copied into browser history, proxies, analytics systems, and access logs.

## Data at rest

Possible layers include:

- Disk or volume encryption
- Database encryption
- Column- or field-level encryption
- Encrypted backups
- Application-level envelope encryption

The correct layer depends on the threat model. Storage-level encryption protects against stolen storage but may not protect data after an application or database account has been compromised. ([OWASP Cheat Sheet Series][10])

## API responses

Use explicit DTOs:

```java
public record CustomerResponse(
        Long id,
        String displayName,
        String maskedEmail
) {
}
```

Do not expose JPA entities directly because they may contain internal or sensitive fields.

## Data masking

```text
Email:
ha***@example.com

Card:
**** **** **** 4242

Government ID:
*******123
```

## Passwords

Passwords should be hashed using a purpose-built adaptive password-hashing algorithm, not reversibly encrypted. ([OWASP Cheat Sheet Series][10])

## Cryptographic keys

Store encryption keys separately from encrypted data and manage their:

- Generation
- Access
- Versioning
- Rotation
- Revocation
- Recovery
- Destruction

Avoid inventing custom encryption algorithms or storing keys alongside ciphertext.

---

# Scenario 11: Sensitive data was exposed in logs. How do you fix and prevent it?

## Immediate response

1. Stop the source of the leak.
2. Restrict access to affected logs.
3. Determine which fields and time periods were affected.
4. Rotate exposed passwords, API keys, tokens, or certificates.
5. Invalidate compromised sessions or refresh tokens.
6. Preserve required forensic evidence.
7. Remove or redact exposed data according to approved retention and incident procedures.
8. Notify security, privacy, and legal teams where required.

Deleting a log line does not make an exposed credential safe; credentials should be treated as compromised and rotated.

## Data that should not normally be logged

- Access tokens
- Refresh tokens
- Passwords
- Session identifiers
- Encryption keys
- Database connection strings
- Payment information
- Sensitive personal data
- Complete request or response bodies containing secrets

OWASP specifically recommends removing, masking, sanitizing, hashing, or encrypting these categories rather than recording them directly. ([OWASP Cheat Sheet Series][11])

## Unsafe logging

```java
log.info(
        "Login request username={} password={} token={}",
        request.username(),
        request.password(),
        token
);
```

## Safer structured logging

```java
log.info(
        "security_event={} actorId={} action={} outcome={} traceId={}",
        "authentication_attempt",
        safeActorId,
        "login",
        outcome,
        traceId
);
```

## Central redaction

Implement common redaction for fields such as:

```text
password
authorization
access_token
refresh_token
client_secret
api_key
cookie
set-cookie
cardNumber
```

Sanitize carriage returns, line feeds, and delimiters in untrusted log values to prevent log injection. ([OWASP Cheat Sheet Series][11])

---

# Scenario 12: How would you audit application logs?

Audit logs should answer:

```text
Who performed which action,
on which resource,
at what time,
from where,
and with what result?
```

## Useful audit fields

- Timestamp
- Actor or service identity
- Tenant
- Action
- Target resource type
- Target resource identifier
- Outcome
- Reason or error code
- Trace or interaction ID
- Source address where appropriate
- Application version
- Authentication method

## Events worth auditing

- Login success and failure
- MFA changes
- Password resets
- Role or permission changes
- Administrative actions
- Sensitive-data access
- Data export
- Financial operation
- Secret rotation
- Configuration changes
- Repeated authorization failures
- Account lock or disablement

## Audit-log protections

- Restrict read access.
- Separate audit access from ordinary developer access.
- Transfer logs over secure channels.
- Centralize collection.
- Use retention policies.
- Detect modification or deletion.
- Record access to the logs.
- Alert on high-risk events.

OWASP recommends tamper detection, controlled log access, centralized monitoring, secure transport, and retention aligned with legal and contractual obligations. ([OWASP Cheat Sheet Series][11])

---

# Scenario 13: How would you manage secrets securely?

Secrets include:

- Database passwords
- API keys
- OAuth client secrets
- Private keys
- Certificates
- Encryption keys
- Message-broker credentials

## Avoid

```yaml
spring:
  datasource:
    password: SuperSecret123
```

Do not store secrets in:

- Git repositories
- Docker images
- Plain configuration files
- CI logs
- Chat messages
- Source-code constants
- Container command lines

## Preferred model

```text
Application workload identity
          ↓
Secrets manager
          ↓
Short-lived or rotated credential
          ↓
Application memory
```

Possible secret-management systems include cloud secret stores, Vault-style systems, and Kubernetes-integrated secret providers.

## Good controls

- Centralize secret storage.
- Use workload identity where possible.
- Grant each service only its own secrets.
- Rotate credentials.
- Prefer short-lived credentials.
- Audit secret access.
- Detect secrets in source control and CI output.
- Define emergency revocation procedures.
- Avoid sharing one credential across many services.

OWASP recommends centralized provisioning, auditing, rotation, lifecycle management, and controlled access for application secrets. ([OWASP Cheat Sheet Series][12])

---

# Scenario 14: How would you ensure compliance with security standards?

Compliance depends on the specific system, jurisdiction, data, contracts, and industry. Implementing JWT, encryption, or an OWASP checklist does not by itself make an application compliant.

## Process

### 1. Identify applicable requirements

Examples may include:

- Internal security policy
- Data-protection legislation
- Customer contracts
- Financial or payment requirements
- Healthcare requirements
- Data-residency rules
- Retention and deletion requirements

### 2. Convert requirements into controls

Map requirements to:

- Authentication
- Authorization
- Encryption
- Logging
- Retention
- Backup
- Incident response
- Vulnerability management
- Access review
- Secure development

### 3. Use an application-security verification baseline

OWASP ASVS provides a structured basis for specifying and testing web-application security requirements. The official OWASP page currently identifies **ASVS 5.0.0** as its latest stable version. ([OWASP][13])

### 4. Maintain evidence

Examples:

- Access-review records
- Test reports
- Deployment approvals
- Vulnerability reports
- Dependency inventories
- Incident exercises
- Backup-restore tests
- Key-rotation records
- Audit-log reviews

### 5. Test continuously

Use:

- Unit and integration security tests
- SAST
- Dependency and container scanning
- Secret scanning
- DAST
- Penetration testing
- Authorization regression tests
- Configuration reviews

---

# Scenario 15: How would you design secure coding practices?

Security should be included across the software-development lifecycle.

## Requirements and design

- Classify data.
- Define trust boundaries.
- Perform threat modelling.
- Document authentication and authorization rules.
- Design secure failure behaviour.
- Minimize collected data.
- Define audit requirements.

## Development

- Use maintained frameworks.
- Parameterize database queries.
- Validate input.
- Encode output.
- Apply least privilege.
- Avoid custom cryptography.
- Use secure password hashing.
- Avoid exposing internal exceptions.
- Keep transactions and security boundaries explicit.

## Code review

Review high-risk areas such as:

- Authentication
- Authorization
- Database-query construction
- File handling
- Deserialization
- Cryptography
- Logging
- External redirects
- SSRF risks
- Object-level access checks

OWASP’s secure-code-review guidance highlights authentication, access control, injection, cryptography, error handling, and logging as key review areas. ([OWASP Cheat Sheet Series][14])

## CI/CD

- Run unit and integration tests.
- Scan dependencies.
- Scan source and images.
- Detect committed secrets.
- Produce an SBOM where required.
- Sign or verify artifacts.
- Protect deployment credentials.
- Require review for security-sensitive changes.

## Runtime

- Monitor failures and anomalies.
- Patch dependencies.
- Rotate credentials.
- Review permissions.
- Exercise incident-response plans.
- Retest critical controls.

---

# Scenario 16: How would you handle a security breach?

Use an established incident-response process rather than improvising during the incident.

## 1. Detect and validate

Determine:

- What happened
- When it started
- Which systems are involved
- Whether the activity is continuing
- Which identities and data may be affected

## 2. Contain

Possible actions include:

- Disable compromised accounts.
- Revoke sessions and refresh tokens.
- Rotate secrets and keys.
- Isolate affected workloads.
- Block malicious addresses or indicators.
- Disable a vulnerable feature.
- Preserve unaffected service availability where possible.

## 3. Preserve evidence

Collect:

- Logs
- Audit events
- Traces
- Cloud activity records
- Identity-provider events
- Database activity
- Container or host snapshots
- Relevant configuration and deployment versions

Do not destroy evidence through uncontrolled cleanup.

## 4. Eradicate

- Patch the vulnerability.
- Remove persistence mechanisms.
- Correct configuration.
- Replace compromised credentials.
- Rebuild affected systems from trusted artifacts.
- Review lateral movement.

## 5. Recover

- Restore service in controlled stages.
- Verify integrity.
- Monitor closely.
- Validate backups.
- Confirm authentication and authorization.
- Increase detection around known indicators.

## 6. Communicate

Engage:

- Incident-response team
- Operations
- Security
- Management
- Legal and privacy teams
- Affected customers or regulators where required

## 7. Learn and improve

Perform a blameless post-incident review:

```text
Root cause
Contributing conditions
Detection gaps
Control failures
Response delays
Corrective actions
Owners and deadlines
```

NIST SP 800-61 Revision 3, published in April 2025, integrates incident-response preparation, detection, response, and recovery into broader cybersecurity-risk management. ([NIST Computer Security Resource Center][15])

### Interview-ready answer

> I follow a documented incident-response plan: validate the event, contain the impact, preserve evidence, rotate or revoke compromised credentials, remove the root cause, recover from trusted artifacts, monitor for recurrence, and coordinate required communications. After recovery, I conduct a post-incident review and track corrective actions to completion.

---

# Quick Interview Cheat Sheet

```text
Public API:
HTTPS, authentication, local authorization, rate limits,
validation, safe errors, monitoring, and least privilege.

SQL injection:
Parameterized queries.
Never concatenate untrusted input into SQL.
Allow-list dynamic identifiers.

XSS:
Contextual output encoding, safe DOM APIs,
HTML sanitization, and CSP.

JWT:
Validate signature, issuer, audience, lifetime,
algorithm, scopes, and roles.

Random logout:
Check expiry, clocks, refresh rotation,
cookies/storage, pod configuration, and key rotation.

Authentication:
Establishes identity.

Authorization:
Determines permitted actions on specific resources.

OAuth 2.0:
Authorization framework.

OIDC:
Authentication and identity layer on OAuth 2.0.

RBAC:
Users receive roles; roles receive permissions.
Add ownership, tenant, and resource checks.

Token expiration:
Refresh once in a controlled manner.
Do not loop or refresh after 403.

Sensitive data:
Minimize, mask, encrypt in transit and at rest,
and separate keys from data.

Logs:
Never log tokens, passwords, secrets, or full sensitive payloads.

Secrets:
Central store, workload identity, least privilege,
rotation, auditing, and leak detection.

Compliance:
Map requirements to controls, testing, and evidence.

Security breach:
Detect, contain, preserve evidence, eradicate,
recover, communicate, and improve.
```

[1]: https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html "REST Security - OWASP Cheat Sheet Series"
[2]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server"
[3]: https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html "Authorization - OWASP Cheat Sheet Series"
[4]: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html "Cross Site Request Forgery (CSRF) :: Spring Security"
[5]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html "OAuth 2.0 Resource Server JWT :: Spring Security"
[6]: https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html "SQL Injection Prevention - OWASP Cheat Sheet Series"
[7]: https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html "Cross Site Scripting Prevention - OWASP Cheat Sheet Series"
[8]: https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html?utm_source=chatgpt.com "OAuth 2.0 Client"
[9]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/opaque-token.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server Opaque Token :: Spring Security"
[10]: https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html "Cryptographic Storage - OWASP Cheat Sheet Series"
[11]: https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html "Logging - OWASP Cheat Sheet Series"
[12]: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html "Secrets Management - OWASP Cheat Sheet Series"
[13]: https://owasp.org/www-project-application-security-verification-standard/ "OWASP Application Security Verification Standard (ASVS) | OWASP Foundation"
[14]: https://cheatsheetseries.owasp.org/cheatsheets/Secure_Code_Review_Cheat_Sheet.html?utm_source=chatgpt.com "Secure Code Review - OWASP Cheat Sheet Series"
[15]: https://csrc.nist.gov/pubs/sp/800/61/r3/final "SP 800-61 Rev. 3, Incident Response Recommendations and Considerations for Cybersecurity Risk Management: A CSF 2.0 Community Profile | CSRC"
