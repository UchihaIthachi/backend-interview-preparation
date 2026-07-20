# Spring Security: JWT, OAuth 2.0, OpenID Connect

## 1. Definition

- **OAuth 2.0** — an authorization framework (delegated access via tokens).
- **OpenID Connect (OIDC)** — an identity layer built on top of OAuth 2.0 (who
  the user is, not just what they're allowed to do).
- **JWT** — a token *format* (a signed, self-contained set of claims) — not
  itself an auth protocol.

## 2. Why it exists

Modern APIs need stateless authentication that scales horizontally without a
shared session store, and need to interoperate with third-party identity
providers instead of managing passwords directly.

## 3. How it works — JWT flow

```text
1. Client sends credentials to /login
2. Server verifies credentials, issues a signed JWT access token (+ refresh token)
3. Client sends the access token as "Authorization: Bearer <token>" on each request
4. A security filter validates the token's signature and claims (expiry, issuer)
5. If valid, the request proceeds with the authenticated principal attached
```

## 4. Code example — securing endpoints

```java
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

## 5. Production use case

Access tokens are short-lived (minutes) to limit damage if leaked; refresh
tokens are longer-lived and used to obtain new access tokens without
re-authenticating — but refresh token revocation requires server-side state
(a token itself can't be "un-issued" once signed and handed out).

## 6. Common mistakes

- Treating JWT as inherently secure — a JWT is only as safe as its signature
  validation and expiry enforcement; an unvalidated JWT can be forged.
- Storing sensitive data in JWT claims (they're base64-encoded, not encrypted,
  and readable by anyone who has the token).
- Not having a revocation strategy for compromised tokens (short expiry +
  refresh token rotation is the common mitigation).
- Confusing OAuth 2.0 (authorization) with authentication — OAuth alone
  doesn't tell you who the user is, that's what OIDC's ID token adds.

## 7. Trade-offs

| Session-based auth | JWT-based auth |
|---|---|
| Easy to revoke (delete session) | Hard to revoke before expiry |
| Requires shared session store to scale | Stateless, scales horizontally |

## 8. Interview questions

1. What's the difference between OAuth 2.0, OpenID Connect, and JWT?
2. Walk through the JWT authentication flow end to end.
3. Why is JWT revocation difficult, and how do you mitigate it?
4. What's stored in a JWT, and why shouldn't sensitive data go in claims?

## 9. Short interview answer

"OAuth 2.0 is the authorization framework, OpenID Connect adds an identity
layer on top of it for authentication, and JWT is just the token format both
can use. My biggest practical concern with JWTs is that they can't easily be
revoked before they expire, so I keep access tokens short-lived and rely on
refresh token rotation to limit the blast radius of a leaked token."

## 10. Related topics

- [REST API development](rest-api.md)
