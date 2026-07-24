# Basic Questions — Spring Security, Filter Chain, and JWT

## Question 1: What is the Spring Security filter chain?

The **Spring Security filter chain** is a sequence of Servlet filters that intercepts an HTTP request before it reaches a Spring MVC controller.

These filters perform security-related operations such as:

- Loading and storing the security context
- Extracting authentication credentials
- Validating usernames, passwords, sessions, or bearer tokens
- Handling CSRF and CORS
- Converting authentication failures into HTTP responses
- Checking whether the authenticated user may access the requested endpoint

Spring Security integrates with the Servlet container through `DelegatingFilterProxy`, which delegates to Spring Security’s `FilterChainProxy`. The `FilterChainProxy` chooses the appropriate `SecurityFilterChain` for the request and executes its filters in order. ([Home][1])

---

## Main components

```text
HTTP request
    ↓
Servlet container
    ↓
DelegatingFilterProxy
    ↓
FilterChainProxy
    ↓
Matching SecurityFilterChain
    ↓
Security filters
    ↓
DispatcherServlet
    ↓
Controller
```

### `DelegatingFilterProxy`

`DelegatingFilterProxy` connects the Servlet container’s filter mechanism with a Spring-managed filter bean.

The Servlet container knows about the proxy, while Spring manages the actual security infrastructure.

### `FilterChainProxy`

`FilterChainProxy` is Spring Security’s central filter. It:

1. Receives the request from `DelegatingFilterProxy`.
2. Finds the first matching `SecurityFilterChain`.
3. Executes the filters belonging to that chain.
4. Continues to the application when security checks succeed.

### `SecurityFilterChain`

A `SecurityFilterChain` contains:

- A request matcher
- An ordered list of security filters

An application can define multiple chains for different URL groups:

```text
/api/**       → JWT resource-server security
/admin/**     → Stronger administrative security
/public/**    → Public or reduced security
```

When multiple chains exist, `FilterChainProxy` evaluates them by priority and uses the first matching chain. A request that matches no security chain is not protected by Spring Security, so applications commonly include a final catch-all chain. ([Home][2])

---

## Typical security filters

The exact filters depend on the application configuration. A JWT API and a form-login application do not have identical filter chains.

Common filters include:

| Filter                                 | Responsibility                                   |
| -------------------------------------- | ------------------------------------------------ |
| `SecurityContextHolderFilter`          | Makes the current security context available     |
| `HeaderWriterFilter`                   | Adds security-related response headers           |
| `CorsFilter`                           | Applies CORS configuration                       |
| `CsrfFilter`                           | Provides CSRF protection                         |
| `LogoutFilter`                         | Processes logout requests                        |
| `UsernamePasswordAuthenticationFilter` | Handles form username/password authentication    |
| `BearerTokenAuthenticationFilter`      | Extracts and authenticates bearer tokens         |
| `AnonymousAuthenticationFilter`        | Represents unauthenticated users anonymously     |
| `ExceptionTranslationFilter`           | Converts security exceptions into HTTP responses |
| `AuthorizationFilter`                  | Applies authorization rules                      |

The filter order matters. Authentication must normally occur before authorization because Spring needs to know the caller’s identity before checking their permissions.

---

## JWT request flow through the filter chain

```text
Client request
    ↓
Authorization: Bearer <access-token>
    ↓
BearerTokenAuthenticationFilter
    ↓
Extract bearer token
    ↓
AuthenticationManager
    ↓
JWT authentication provider
    ↓
JwtDecoder
    ├── Verify signature
    ├── Validate expiration
    ├── Validate not-before time
    ├── Validate issuer
    └── Apply custom validators such as audience
    ↓
Create authenticated Authentication object
    ↓
Store it in SecurityContextHolder
    ↓
AuthorizationFilter checks permissions
    ↓
Controller executes
```

Spring Security’s resource-server support obtains bearer tokens from the `Authorization` header by default. With issuer-based JWT configuration, it can discover the issuer’s key set and validate the token’s signature and claims. ([Home][3])

---

## Security configuration example

```java
@Configuration
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

Configuration:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://identity.example.com/realms/application
```

Disabling CSRF is appropriate for a genuinely bearer-token-only API where the browser does not automatically attach authentication credentials. Cookie-authenticated applications may still require CSRF protection.

---

## Authentication failure versus authorization failure

### `401 Unauthorized`

The request is not successfully authenticated.

Examples:

- No access token
- Invalid signature
- Expired token
- Wrong issuer
- Malformed token

### `403 Forbidden`

The caller is authenticated but lacks the required permission.

Example:

```text
Authenticated authorities:
SCOPE_orders.read

Required authority:
SCOPE_orders.write

Result:
403 Forbidden
```

`ExceptionTranslationFilter` helps translate authentication and authorization failures into appropriate responses before the request reaches the controller.

### Interview-ready answer

> The Spring Security filter chain is a collection of Servlet filters that intercepts requests before they reach the controller. `DelegatingFilterProxy` connects the Servlet container to Spring Security’s `FilterChainProxy`, which selects the first matching `SecurityFilterChain`. The filters then perform authentication, populate the `SecurityContext`, handle security exceptions, and apply authorization rules.

---

# Question 2: Explain basic Spring Security

Spring Security is a framework that provides:

- Authentication
- Authorization
- Password encoding
- Session management
- OAuth 2.0 and OpenID Connect support
- JWT and opaque-token resource servers
- CSRF protection
- CORS integration
- Security headers
- Method-level security
- Protection against common web attacks

Spring Security defines authentication as establishing the caller’s identity and authorization as deciding what that identity may access. ([Home][4])

---

## Authentication versus authorization

### Authentication

Answers:

> Who is making the request?

Examples:

- Username and password
- Session cookie
- JWT access token
- OAuth 2.0 login
- Client certificate
- API credentials

### Authorization

Answers:

> Is this caller allowed to perform the requested operation?

Examples:

```java
.hasRole("ADMIN")
```

```java
.hasAuthority("SCOPE_orders.write")
```

```java
@PreAuthorize("#userId == authentication.name")
```

Spring Security supports request-level and method-level authorization. Method-level rules can be placed on classes, interfaces, or methods. ([Home][5])

---

## Core authentication objects

### `Authentication`

Represents either:

- Credentials submitted for authentication, or
- An authenticated principal and their authorities

It normally contains:

- Principal
- Credentials
- Authorities
- Authentication status
- Additional request details

### `SecurityContext`

Stores the current `Authentication`.

### `SecurityContextHolder`

Provides access to the current security context during request processing.

```java
Authentication authentication =
        SecurityContextHolder.getContext()
                .getAuthentication();
```

### `AuthenticationManager`

Coordinates authentication.

### `ProviderManager`

A common `AuthenticationManager` implementation that delegates to one or more `AuthenticationProvider` objects.

### `AuthenticationProvider`

Authenticates a particular credential type.

Examples include providers for:

- Username and password
- JWT bearer tokens
- LDAP
- OAuth 2.0
- Remember-me authentication

For username/password authentication, `DaoAuthenticationProvider` commonly uses `UserDetailsService` and `PasswordEncoder`. ([Home][4])

---

## Basic username/password example

```java
@Configuration
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder encoder
    ) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("change-me"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

In production, user information would normally come from a database, LDAP directory, or external identity provider rather than hard-coded users.

### Interview-ready answer

> Spring Security protects applications through a chain of filters. It authenticates the caller, stores the authenticated identity in the `SecurityContext`, and applies request-level or method-level authorization. It also provides password encoding, sessions, JWT resource-server support, OAuth 2.0, CSRF protection, and security headers.

---

# Question 3: How have you used Spring Security in your project?

## Personalized interview answer

> In my Spring Boot projects, I used Spring Security with Keycloak as the centralized identity provider. The frontend authenticated through Keycloak using OAuth 2.0 and OpenID Connect, while the Spring Boot services operated as OAuth 2.0 resource servers.
>
> Each API received the access token through the `Authorization: Bearer` header. Spring Security validated the JWT signature using Keycloak’s published keys and checked claims such as the issuer and expiration. I then mapped token scopes or roles to Spring authorities and protected endpoints through `SecurityFilterChain` rules and method-level annotations such as `@PreAuthorize`.
>
> I allowed endpoints such as health checks and selected public routes without authentication, while business and administrative APIs required specific permissions. The APIs used stateless session management because authentication information came from the access token rather than a server-side HTTP session.
>
> I also handled `401` responses for invalid or missing tokens separately from `403` responses for insufficient permissions. For production concerns, I avoided logging tokens, kept authorization checks inside the services instead of relying only on the API gateway, and used TLS for all token transmission.

## Example configuration

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
                        .requestMatchers(
                                "/actuator/health",
                                "/api/public/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/research/**"
                        ).hasAnyRole(
                                "RESEARCHER",
                                "ADMIN"
                        )
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

Method-level authorization:

```java
@Service
public class StudyService {

    @PreAuthorize(
        "hasRole('ADMIN') or hasAuthority('SCOPE_studies.write')"
    )
    public StudyResponse createStudy(
            CreateStudyRequest request
    ) {
        return create(request);
    }
}
```

---

# Question 4: How did you implement Spring Security in distributed microservices?

In a distributed system, authentication should normally be centralized, while authorization must still be enforced by every service.

## Architecture

```text
User
  ↓
Identity provider
  ├── Authenticates user
  └── Issues access token
  ↓
API gateway
  ├── Routing
  ├── Optional edge validation
  └── Rate limiting
  ↓
Microservice A
  ├── Validates access token
  └── Enforces its own permissions
  ↓
Microservice B
  ├── Validates access token
  └── Enforces its own permissions
```

## Implementation approach

### 1. Central identity provider

Use an identity provider such as:

- Keycloak
- Spring Authorization Server
- Microsoft Entra ID
- Auth0
- Okta

The identity provider handles:

- User authentication
- Client registration
- Token issuance
- Signing-key rotation
- OAuth 2.0 and OIDC protocols

### 2. Each microservice is a resource server

Each Spring Boot service independently validates access tokens.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com/realms/platform
```

The services can use the issuer’s JWK set to validate signatures without sharing private signing keys.

### 3. Apply service-specific authorization

```java
.requestMatchers("/api/inventory/**")
.hasAuthority("SCOPE_inventory.read")
```

```java
.requestMatchers(HttpMethod.POST, "/api/payments/**")
.hasAuthority("SCOPE_payments.write")
```

An inventory token should not automatically authorize payment operations.

### 4. Validate the intended audience

A token intended for one API should not automatically be accepted by another.

```text
aud = inventory-api
```

The payment service should reject that token unless it is also an intended audience.

### 5. Service-to-service authentication

For a service acting on its own behalf, use the OAuth 2.0 client-credentials flow:

```text
Order service
    ↓ authenticates using client credentials
Authorization server
    ↓ issues service access token
Order service
    ↓
Inventory service
```

The service receives only the scopes required for its operation.

```text
inventory.reserve
```

not a broad administrative permission.

### 6. Do not rely only on the gateway

The gateway can provide:

- Initial token validation
- Routing
- Rate limiting
- Coarse access control

However, each service should still enforce authorization because:

- Internal routes may bypass the gateway.
- Gateway configuration can be incorrect.
- Services understand their own business rules.
- Object-level authorization belongs near the protected resource.

### 7. Propagate tokens carefully

For user-driven requests, the calling service may relay the user’s access token when the downstream API is an intended audience.

For internal operations, it may instead obtain a service token. Do not blindly forward every received token to every downstream service.

### Interview-ready answer

> I used a centralized identity provider to authenticate users and issue OAuth access tokens. The gateway handled routing and basic edge security, while every Spring Boot microservice was independently configured as a resource server. Each service validated the JWT using the issuer’s public keys and enforced its own scopes, roles, audience, and business authorization. For service-to-service requests, I used client credentials and least-privilege scopes rather than sharing user passwords or signing keys.

---

# Question 5: What is JWT security, and how have you used it?

A JWT is a compact claims format that is commonly signed to provide integrity and authenticity.

```text
header.payload.signature
```

Example claims:

```json
{
  "iss": "https://auth.example.com",
  "sub": "user-123",
  "aud": "orders-api",
  "scope": "orders.read orders.write",
  "iat": 1780000000,
  "exp": 1780000900
}
```

A signed JWT is generally **readable** by anyone possessing it. Its signature prevents undetected modification but does not encrypt the claims.

---

## JWT security flow

```text
1. User or service authenticates with the authorization server.

2. Authorization server issues a signed access token.

3. Client sends:
   Authorization: Bearer <token>

4. Spring Security extracts the token.

5. JwtDecoder validates:
   - Signature
   - Trusted algorithm
   - Issuer
   - Expiration
   - Not-before time
   - Audience when configured

6. Claims are converted into authorities.

7. Spring applies endpoint and method authorization.
```

Spring Security supports JWT resource-server validation through its OAuth 2.0 resource-server functionality. OAuth scopes are normally mapped to authorities with the `SCOPE_` prefix. ([Home][3])

---

## How I used JWT in a project

> I used JWT access tokens issued by Keycloak rather than generating and validating tokens manually inside each service. The client sent the token in the bearer authorization header, and Spring Security validated it using Keycloak’s issuer metadata and public JWK keys.
>
> I mapped claims to Spring authorities and used URL-level and method-level authorization. I kept the APIs stateless, excluded only explicitly public endpoints, and returned `401` for invalid authentication and `403` when a valid user lacked permission.
>
> I also treated JWTs as credentials: I transmitted them only over TLS, avoided putting sensitive information in claims, never logged complete tokens, and used short-lived access tokens. Refresh-token handling remained with the authorization server rather than the resource APIs.

---

## Important JWT checks

A resource server should validate more than the signature:

| Check           | Purpose                             |
| --------------- | ----------------------------------- |
| Signature       | Detects token modification          |
| Algorithm       | Prevents algorithm confusion        |
| `iss`           | Confirms the trusted issuer         |
| `exp`           | Rejects expired tokens              |
| `nbf`           | Rejects tokens used too early       |
| `aud`           | Confirms the token targets this API |
| Scopes or roles | Determines permissions              |

---

## Common JWT mistakes

### Accepting an ID token as an API access token

An OIDC ID token is intended for the client application. APIs should generally receive access tokens.

### Storing secrets in claims

JWT payloads are encoded, not automatically encrypted.

### Using long-lived access tokens

A stolen self-contained token may remain usable until it expires.

### Logging tokens

A bearer token allows whoever possesses it to use it.

### Validating only the signature

A correctly signed token may still have:

- The wrong issuer
- The wrong audience
- An expired lifetime
- Insufficient permissions

### Trusting only the gateway

Backend services must still protect their resources and business operations.

---

# Complete Interview Answer

> Spring Security protects HTTP requests through a chain of Servlet filters. `DelegatingFilterProxy` delegates to Spring Security’s `FilterChainProxy`, which selects the first matching `SecurityFilterChain`. Authentication filters extract credentials such as a bearer token, authenticate them through the configured providers, and store the result in the `SecurityContext`. Authorization filters then determine whether the caller may access the endpoint.
>
> In my projects, I integrated Spring Security with Keycloak using OAuth 2.0 and OpenID Connect. Each Spring Boot microservice acted as a JWT resource server, validated tokens using Keycloak’s published signing keys, and enforced roles or scopes through request rules and `@PreAuthorize`. The gateway provided routing and coarse security, but every service still enforced its own permissions. For service-to-service communication, I used client credentials and least-privilege scopes rather than sharing user credentials.

[1]: https://docs.spring.io/spring-security/reference/servlet/architecture.html?utm_source=chatgpt.com "Architecture :: Spring Security"
[2]: https://docs.spring.io/spring-security/reference/_images/servlet/architecture/multi-securityfilterchain.odg?utm_source=chatgpt.com "multi-securityfilterchain.odg - Spring"
[3]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server JWT"
[4]: https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html?utm_source=chatgpt.com "Servlet Authentication Architecture :: Spring Security"
[5]: https://docs.spring.io/spring-security/reference/servlet/authorization/index.html?utm_source=chatgpt.com "Authorization :: Spring Security"
