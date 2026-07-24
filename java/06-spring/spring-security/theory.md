# Spring Security: JWT, OAuth 2.0, and OpenID Connect

## 1. Definitions

### OAuth 2.0

**OAuth 2.0 is an authorization framework.** It allows a client application to obtain limited access to protected resources, either on behalf of a user or on its own behalf. It defines how access is delegated but does not, by itself, define a standard way to authenticate an end user. ([RFC Editor][1])

### OpenID Connect

**OpenID Connect, or OIDC, is an authentication and identity layer built on OAuth 2.0.** It introduces the `openid` scope, the ID token, standardized user claims, discovery, and the UserInfo endpoint. The ID token communicates information about the user’s authentication to the client application. ([OpenID Foundation][2])

### JWT

**JSON Web Token, or JWT, is a token format**, not an authentication or authorization protocol. A JWT contains claims represented as JSON and may be signed, encrypted, or both. OAuth access tokens may be JWTs or opaque strings, while an OIDC ID token is represented as a JWT. ([RFC Editor][3])

```text
OAuth 2.0
→ Delegated authorization

OpenID Connect
→ Authentication and identity on top of OAuth 2.0

JWT
→ A format for carrying claims
```

---

# 2. Why These Technologies Exist

Applications frequently need to:

- Authenticate users through a centralized identity provider.
- Allow one application to access another API with limited permissions.
- Support web, mobile, machine-to-machine, and microservice clients.
- Separate identity management from business APIs.
- Scale resource servers without storing a local session for every token.
- Limit access using scopes, roles, audiences, and expiration times.

JWT access tokens can be validated locally by a resource server using the issuer’s public keys. However, session-based authentication and opaque tokens remain valid architectural options and can provide easier immediate revocation. Spring Security supports both JWT and opaque bearer tokens. ([Home][4])

---

# 3. Main OAuth and OIDC Components

| Component            | Responsibility                                   |
| -------------------- | ------------------------------------------------ |
| Resource owner       | Usually the user who owns or controls data       |
| Client               | Application requesting access                    |
| Authorization server | Authenticates users or clients and issues tokens |
| OpenID Provider      | Authorization server that supports OIDC          |
| Resource server      | API that validates access tokens                 |
| Relying Party        | OIDC client that consumes identity information   |

Example:

```text
Browser or mobile application
        ↓
Identity provider / Authorization server
        ↓ issues access token and ID token
Client application
        ↓ sends access token
Spring Boot resource server
        ↓
Protected business data
```

OIDC calls the authorization server an **OpenID Provider** and the client a **Relying Party**. ([OpenID Foundation][2])

---

# 4. Important Token Types

## Access Token

An access token authorizes access to an API.

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

It is intended for the resource server and may contain:

- Issuer
- Subject
- Audience
- Expiration
- Scopes
- Roles or permissions
- Client identifier

OAuth does not require access tokens to be JWTs. They may instead be opaque values validated through an introspection endpoint. Any party possessing an ordinary bearer token can use it, so it must be protected in storage and transport. ([RFC Editor][5])

---

## ID Token

An ID token tells an OIDC client about the authentication event and authenticated user.

```json
{
  "iss": "https://identity.example.com",
  "sub": "user-8821",
  "aud": "orders-web-client",
  "exp": 1760000000,
  "auth_time": 1759999000
}
```

The ID token is intended for the client application named in its audience. It should not normally be sent to an API as a substitute for an access token. ([OpenID Foundation][2])

---

## Refresh Token

A refresh token allows a client to obtain a new access token without requiring the user to authenticate again.

```text
Access token:
Shorter lifetime
Sent to resource server

Refresh token:
Longer-lived credential
Sent only to the authorization server
```

Refresh tokens are especially sensitive because possession may allow an attacker to mint additional access tokens. OAuth security guidance recommends sender-constrained refresh tokens or refresh-token rotation for public clients, together with expiration, client binding, secure storage, and replay detection. ([RFC Editor][6])

---

# 5. JWT Structure

A commonly signed JWT has three Base64url-encoded sections:

```text
header.payload.signature
```

Example structure:

```text
eyJhbGciOiJSUzI1NiIsImtpZCI6ImtleS0xIn0
.
eyJzdWIiOiJ1c2VyLTEwMSIsImV4cCI6MTc2MDAwMDAwMH0
.
cryptographic-signature
```

## Header

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-1"
}
```

## Payload

```json
{
  "iss": "https://identity.example.com",
  "sub": "user-101",
  "aud": "orders-api",
  "exp": 1760000000,
  "iat": 1759999000,
  "scope": "orders.read orders.write"
}
```

## Signature

The signature protects the token against unauthorized modification. It does **not** hide the payload.

```text
Signed JWT:
Integrity and authenticity

Encrypted JWT:
Confidentiality

Base64url encoding:
Not encryption
```

Anyone possessing a normal signed JWT can decode its header and payload. Sensitive information such as passwords, private keys, card data, or confidential personal data should therefore not be placed in ordinary JWT claims. JWT security guidance also requires explicit algorithm verification and appropriate validation of issuer, audience, and cryptographic operations. ([RFC Editor][7])

---

# 6. Important JWT Claims

| Claim            | Meaning                                     |
| ---------------- | ------------------------------------------- |
| `iss`            | Issuer that created the token               |
| `sub`            | Subject represented by the token            |
| `aud`            | Intended recipient or resource server       |
| `exp`            | Expiration time                             |
| `nbf`            | Token must not be accepted before this time |
| `iat`            | Time at which the token was issued          |
| `jti`            | Unique token identifier                     |
| `scope` or `scp` | Granted OAuth scopes                        |

A resource server should not validate only the signature. It should also validate the expected issuer, expiration, not-before time, intended audience, token type where appropriate, and application-specific authorization claims. The JWT specification requires rejection after `exp`, while JWT security guidance emphasizes issuer and audience validation to prevent token-substitution attacks. ([IETF Datatracker][8])

---

# 7. Correct OAuth/OIDC Login Flow

The basic flow in the original notes:

```text
Client sends username and password to /login
→ API validates them
→ API creates a JWT
```

is a **custom JWT authentication implementation**. It is not automatically OAuth 2.0 or OpenID Connect.

For user-facing OAuth/OIDC systems, the authorization server normally handles authentication.

## Authorization Code Flow

```text
1. User opens the client application.

2. Client redirects the browser to the authorization server.

3. Authorization server authenticates the user.

4. User grants or has previously granted authorization.

5. Authorization server redirects back with an authorization code.

6. Client exchanges the code at the token endpoint.

7. Authorization server returns:
   - Access token
   - ID token for OIDC
   - Optionally a refresh token

8. Client sends the access token to the API.

9. Resource server validates the token and authorizes the request.
```

OIDC adds authentication to the OAuth authorization process and returns identity information through an ID token. ([OpenID Foundation][2])

For public clients such as mobile or browser applications, authorization-code flows should use PKCE and modern OAuth security controls rather than embedding a client secret that cannot be protected.

---

# 8. Service-to-Service Authentication

For a machine acting on its own behalf, OAuth commonly uses a client-credentials flow:

```text
Inventory service
        ↓ authenticates as a client
Authorization server
        ↓ issues access token
Inventory service
        ↓ sends token
Order API
```

In this case, the access token represents the client application rather than a logged-in human user. Resource servers must distinguish client identities from user identities and authorize them accordingly. OAuth security guidance warns against confusing a client’s subject with an end-user subject. ([RFC Editor][6])

---

# 9. Spring Boot Resource Server Configuration

## Maven dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

## Issuer configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://identity.example.com
```

With issuer-based configuration, Spring Security discovers the authorization server metadata and JWK set, validates the signature using trusted public keys, and checks `iss`, `exp`, and `nbf`. OAuth scopes are mapped to authorities prefixed with `SCOPE_`. ([Home][9])

---

## Security filter chain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
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

Disabling CSRF is appropriate only when the API is genuinely bearer-token-only and browsers do not automatically attach authentication credentials such as session cookies. Cookie-based authentication remains vulnerable to CSRF and should retain suitable protection. Spring Security enables CSRF protection by default for unsafe methods. ([Home][10])

---

# 10. Scope-Based Authorization

Spring Security normally maps:

```json
{
  "scope": "orders.read orders.write"
}
```

to:

```text
SCOPE_orders.read
SCOPE_orders.write
```

This allows request-level authorization:

```java
.requestMatchers(
        HttpMethod.GET,
        "/api/orders/**"
).hasAuthority("SCOPE_orders.read")
```

or method-level authorization:

```java
@Service
public class OrderService {

    @PreAuthorize("hasAuthority('SCOPE_orders.write')")
    public OrderResponse createOrder(
            CreateOrderRequest request
    ) {
        return create(request);
    }
}
```

Method security is enabled with `@EnableMethodSecurity`, while Spring’s resource-server support maps OAuth scopes to `SCOPE_` authorities by default. ([Home][9])

---

# 11. Role-Based Authorization

Identity providers often store roles in provider-specific claims rather than the standard `scope` claim.

Example:

```json
{
  "roles": ["ADMIN", "SUPPORT"]
}
```

A custom converter can map those values to Spring authorities:

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
    );

    return converter;
}
```

Register it:

```java
.oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt
                .jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                )
        )
)
```

This produces authorities such as:

```text
ROLE_ADMIN
ROLE_SUPPORT
```

Spring Security supports configuring the claim name and authority prefix used by the JWT authentication converter. ([Home][9])

---

# 12. Audience Validation

Issuer validation alone does not prove that a token was intended for the current API.

Suppose an identity provider issues tokens for:

```text
orders-api
payments-api
inventory-api
```

A token intended for `inventory-api` should not be accepted by `orders-api`.

A custom audience validator can be configured:

```java
@Bean
JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        String issuer
) {
    NimbusJwtDecoder decoder =
            (NimbusJwtDecoder)
                    JwtDecoders.fromIssuerLocation(issuer);

    OAuth2TokenValidator<Jwt> issuerValidator =
            JwtValidators.createDefaultWithIssuer(issuer);

    OAuth2TokenValidator<Jwt> audienceValidator =
            new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD,
                    audience -> audience.contains("orders-api")
            );

    decoder.setJwtValidator(
            new DelegatingOAuth2TokenValidator<>(
                    issuerValidator,
                    audienceValidator
            )
    );

    return decoder;
}
```

Audience restriction limits where a stolen token can be replayed. Spring Security provides APIs for adding audience validation, while OAuth security best practices recommend audience-restricted access tokens. ([Home][9])

---

# 13. JWT Validation Flow in Spring Security

```text
HTTP request
    ↓
BearerTokenAuthenticationFilter
    ↓
Extract Authorization: Bearer token
    ↓
JwtDecoder
    ├── Parse token
    ├── Verify signature
    ├── Resolve trusted key
    ├── Validate issuer
    ├── Validate expiration
    ├── Validate not-before time
    └── Apply custom validators
    ↓
JwtAuthenticationConverter
    ↓
Authentication stored in SecurityContext
    ↓
Authorization rules evaluated
    ↓
Controller invoked
```

Spring Security can obtain current and rotated public verification keys from the issuer’s JWK set and convert scopes into granted authorities. ([Home][9])

---

# 14. JWT Revocation

A self-contained JWT is usually accepted until it:

- Expires
- Fails validation
- Uses a signing key that is no longer trusted
- Appears on an application-maintained revocation list
- Is rejected because the underlying authorization has been revoked through additional state

The resource server normally validates a JWT without contacting the authorization server for every request. This improves independence and reduces per-request network calls, but it makes immediate individual-token revocation more difficult.

## Common mitigation strategies

### Short-lived access tokens

```text
Access token lifetime:
Minutes rather than days
```

This limits the maximum time a stolen token remains useful.

### Refresh-token rotation

```text
Refresh token RT1 used
→ RT1 invalidated
→ RT2 issued

RT1 used again
→ replay detected
→ active token family revoked
```

OAuth security guidance requires public clients to use sender-constrained refresh tokens or refresh-token rotation to detect replay. ([RFC Editor][6])

### Revocation list

Store revoked identifiers such as `jti` until the corresponding token expires.

Trade-off:

```text
Local JWT validation
+
central revocation lookup
=
partially stateful authentication
```

### Opaque tokens and introspection

A resource server can call an authorization server’s introspection endpoint to determine whether an opaque token is active. This provides more centralized control but introduces an additional dependency and possible latency unless carefully cached. ([Home][11])

### Sender-constrained tokens

Mechanisms such as mutual TLS or DPoP bind a token to key material held by the client, reducing the usefulness of a stolen token by requiring proof of possession. ([RFC Editor][6])

---

# 15. Logout with JWTs

Logout can mean different things:

```text
1. Delete local client state.
2. Revoke the refresh token.
3. End the identity-provider session.
4. Revoke an authorization grant.
5. Denylist an access token until expiry.
```

Deleting a token from one browser does not invalidate copies stolen or stored elsewhere.

A production logout design should therefore define:

- Whether refresh tokens are revoked
- Whether the identity-provider session ends
- Whether all devices or one device are logged out
- Whether current access tokens remain valid until expiry
- Whether a token denylist is required

---

# 16. Where Should Tokens Be Stored?

## Browser local storage

Advantages:

- Simple client implementation
- Easy to attach to requests

Risk:

- JavaScript can read the token, so an XSS vulnerability can steal it.

## HTTP-only cookie

Advantages:

- JavaScript cannot directly read an `HttpOnly` cookie.

Risks:

- Browsers attach cookies automatically.
- CSRF protection and appropriate `SameSite`, `Secure`, and domain settings are required.
- Cross-origin architecture can become more complex.

## In-memory browser storage

Advantages:

- Reduces long-term persistence.

Trade-off:

- Tokens disappear on refresh.
- Additional session restoration or backend-for-frontend design may be required.

## Mobile and desktop applications

Use platform-secure storage, such as an operating-system keychain or credential vault, rather than ordinary files or unprotected preferences.

There is no universally correct token-storage mechanism. The decision depends on XSS exposure, CSRF exposure, user experience, architecture, and platform capabilities.

---

# 17. Common Security Mistakes

## Mistake 1: Treating JWT as an authentication protocol

JWT is a format. A system still needs a secure protocol and issuer responsible for authentication, token issuance, validation rules, and lifecycle management.

---

## Mistake 2: Accepting any correctly signed token

A token can be validly signed but intended for:

- Another API
- Another environment
- Another client
- Another token purpose

Validate issuer, audience, expiration, token type, and required claims—not only the signature. ([RFC Editor][7])

---

## Mistake 3: Trusting the token’s algorithm without restriction

Applications should configure acceptable algorithms instead of blindly trusting the `alg` header supplied by the token. JWT best-practice guidance documents attacks involving `none` and algorithm confusion. ([RFC Editor][7])

---

## Mistake 4: Using an ID token to call an API

The ID token is intended for the OIDC client. The access token is intended for the resource server.

```text
ID token
→ client identity/session

Access token
→ API authorization
```

---

## Mistake 5: Sending refresh tokens to every API

Refresh tokens should normally be sent only to the authorization server’s token endpoint.

---

## Mistake 6: Storing sensitive information in claims

A normal signed JWT is readable by anyone who obtains it. Avoid including secrets or unnecessary personal data.

---

## Mistake 7: Creating tokens without expiration

A bearer token without a reasonable lifetime increases the impact of theft.

---

## Mistake 8: Disabling CSRF solely because the application is “stateless”

Statelessness alone does not remove CSRF risk. The important question is whether the browser automatically attaches the authentication credential. Cookie-authenticated APIs still require protection. ([Home][10])

---

## Mistake 9: Confusing scopes with all business authorization

A scope such as:

```text
orders.write
```

may permit the operation category, but the service must still enforce object-level rules:

```text
Can this user modify this specific order?
Does the order belong to the same tenant?
Is the order currently editable?
```

---

## Mistake 10: Logging complete tokens

Access and refresh tokens are credentials. Do not place them in:

- Application logs
- Error messages
- Analytics events
- URLs
- Support tickets
- Distributed-tracing attributes

Bearer-token specifications require protection from disclosure in storage and transport. ([RFC Editor][5])

---

# 18. JWT vs Opaque Tokens

| JWT access token                      | Opaque access token                           |
| ------------------------------------- | --------------------------------------------- |
| Contains claims                       | Meaningless value to the resource server      |
| Can usually be validated locally      | Commonly requires introspection               |
| Reduces per-request issuer dependency | Central server can determine current activity |
| Harder to revoke immediately          | Easier centralized revocation                 |
| Claims may become stale               | Current state can be checked centrally        |
| Larger token                          | Usually shorter                               |
| Claims are visible unless encrypted   | Internal data is not exposed in the token     |

The choice should be based on revocation requirements, latency, dependency tolerance, privacy, and operational complexity rather than assuming JWT is always superior.

---

# 19. Session Authentication vs JWT Authentication

| Session-based authentication                                               | JWT bearer authentication                                      |
| -------------------------------------------------------------------------- | -------------------------------------------------------------- |
| Server stores or retrieves session state                                   | Token usually carries authorization claims                     |
| Session can be invalidated centrally                                       | Access token may remain valid until expiry                     |
| Browser cookies integrate naturally                                        | Authorization header is common                                 |
| Requires session replication, affinity, or shared storage when distributed | Resource servers can validate independently                    |
| Cookie authentication requires CSRF protection                             | Header bearer tokens are not automatically attached cross-site |
| Claims can reflect current server state                                    | Claims can become stale before expiry                          |

JWT authentication is not automatically more secure or appropriate than sessions. Traditional server-rendered applications often work well with secure server-side sessions, while distributed APIs may benefit from bearer-token resource-server architecture.

---

# 20. Production Checklist

Before releasing a JWT-secured Spring Boot API, verify:

- TLS is mandatory.
- Tokens are accepted only from trusted issuers.
- Signature algorithms are restricted.
- Signing keys are rotated safely.
- `iss`, `exp`, `nbf`, and `aud` are validated.
- ID tokens are not accepted as API access tokens.
- Access tokens have short, risk-appropriate lifetimes.
- Refresh tokens are protected, rotated, or sender-constrained.
- Scopes and roles follow least privilege.
- Object-level authorization is enforced.
- Tokens are never logged.
- CSRF configuration matches the credential-storage model.
- CORS is narrowly configured.
- Authentication failures return `401`.
- Insufficient permissions return `403`.
- Clock skew is bounded.
- Token validation failures are monitored without exposing token contents.
- Key and identity-provider outages have defined behavior.
- A revocation and compromised-token response procedure exists.

---

# 21. Interview Questions and Answers

## Q1: What is the difference between OAuth 2.0, OIDC, and JWT?

> OAuth 2.0 is an authorization framework for delegated access. OpenID Connect adds authentication and standardized identity information on top of OAuth 2.0. JWT is a format for carrying claims and can be used for access tokens, ID tokens, and other security tokens.

## Q2: Is every OAuth access token a JWT?

> No. OAuth does not require a particular access-token format. An access token may be a JWT or an opaque value.

## Q3: What is the difference between an access token and an ID token?

> An access token is presented to a resource server to authorize API access. An ID token is consumed by the OIDC client and communicates information about the user’s authentication.

## Q4: What does Spring Security validate for a JWT resource server?

> With issuer-based configuration, Spring Security verifies the JWT signature using trusted issuer keys and validates the issuer, expiration, and not-before claims. I also configure audience validation when the API has a defined audience.

## Q5: Why is JWT revocation difficult?

> A resource server can validate a self-contained JWT without consulting central state, so an already issued token may remain valid until expiration. I mitigate that with short-lived access tokens, refresh-token rotation, revocation state where required, or opaque tokens with introspection.

## Q6: What is refresh-token rotation?

> Each successful refresh invalidates the old refresh token and returns a new one. Reuse of an invalidated token indicates possible theft, allowing the authorization server to revoke the active token family.

## Q7: Why should sensitive data not be placed in JWT claims?

> A signed JWT protects integrity but does not encrypt the payload. Anyone possessing the token can normally decode its claims.

## Q8: What is the difference between scopes and roles?

> Scopes represent permissions granted to a client or token, such as `orders.read`. Roles commonly represent broader organizational privileges, such as `ADMIN`. Their precise meaning is system-specific, and both still require object-level authorization.

## Q9: Why validate the `aud` claim?

> Audience validation prevents a token issued for one API from being replayed against another API that trusts the same issuer.

## Q10: Should CSRF always be disabled for JWT APIs?

> No. It is often disabled for a bearer-only API where credentials are supplied explicitly in an Authorization header. If authentication uses browser cookies, CSRF protection is still required even when the server does not maintain a traditional session.

---

# Interview-Ready Summary

> OAuth 2.0 is an authorization framework, OpenID Connect adds authentication and identity, and JWT is only a token format. In Spring Boot, I configure the application as an OAuth resource server, validate the signature and claims such as issuer, expiration, not-before time, and audience, and map scopes or role claims to Spring authorities. I use access tokens for APIs, ID tokens for the client’s authenticated session, and refresh tokens only with the authorization server. Because self-contained access tokens are difficult to revoke immediately, I keep them short-lived and use refresh-token rotation, secure storage, audience restrictions, and revocation mechanisms according to the system’s risk requirements.

[1]: https://www.rfc-editor.org/info/rfc6749/?utm_source=chatgpt.com "RFC 6749: The OAuth 2.0 Authorization Framework"
[2]: https://openid.net/specs/openid-connect-core-1_0.html "Final: OpenID Connect Core 1.0 incorporating errata set 2"
[3]: https://www.rfc-editor.org/info/rfc7519/ "RFC 7519: JSON Web Token (JWT) | RFC Editor"
[4]: https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html?utm_source=chatgpt.com "OAuth2 :: Spring Security"
[5]: https://www.rfc-editor.org/info/rfc6750/?utm_source=chatgpt.com "RFC 6750: The OAuth 2.0 Authorization Framework"
[6]: https://www.rfc-editor.org/info/rfc9700/ "RFC 9700: Best Current Practice for OAuth 2.0 Security | RFC Editor"
[7]: https://www.rfc-editor.org/info/rfc8725/ "RFC 8725: JSON Web Token Best Current Practices | RFC Editor"
[8]: https://datatracker.ietf.org/doc/html/rfc7519?utm_source=chatgpt.com "RFC 7519 - JSON Web Token (JWT) - Datatracker - IETF"
[9]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html "OAuth 2.0 Resource Server JWT :: Spring Security"
[10]: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html?utm_source=chatgpt.com "Cross Site Request Forgery (CSRF) :: Spring Security"
[11]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/opaque-token.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server Opaque Token :: Spring Security"
