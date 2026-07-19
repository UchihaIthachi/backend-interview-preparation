# Exception Handling in Java

## 1. Definition

Checked exceptions (`IOException`, `SQLException`) must be declared or caught at
compile time. Unchecked exceptions (`RuntimeException` and subclasses) are not
enforced by the compiler.

## 2. Why it exists

Checked exceptions force callers to handle recoverable, expected failure modes
(a file not existing, a network call failing). Unchecked exceptions represent
programmer errors or truly unrecoverable states (`NullPointerException`,
`IllegalArgumentException`) that shouldn't clutter every method signature.

## 3. How it works

```java
class InsufficientFundsException extends RuntimeException {
    InsufficientFundsException(String message) { super(message); }
}

void withdraw(BigDecimal amount) {
    if (amount.compareTo(balance) > 0) {
        throw new InsufficientFundsException("Balance too low for withdrawal");
    }
    balance = balance.subtract(amount);
}
```

Prefer unchecked custom exceptions for business rule violations in backend
services — checked exceptions tend to leak implementation details up through
every layer of a REST API and require boilerplate `throws` clauses.

## 4. Code example — global exception handling in Spring

```java
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(InsufficientFundsException.class)
    ResponseEntity<ErrorResponse> handle(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }
}
```

## 5. Production use case

Centralizing exception-to-HTTP-status mapping in one `@RestControllerAdvice`
avoids repeating try/catch blocks in every controller and gives consistent error
response bodies across an entire API.

## 6. Common mistakes

- Catching `Exception` broadly and swallowing the real cause.
- Using exceptions for normal control flow (expensive and unreadable).
- Forgetting try-with-resources for closeable resources (leaks file handles/connections).

## 7. Trade-offs

| Checked | Unchecked |
|---|---|
| Compiler-enforced handling | No compiler enforcement |
| Good for recoverable I/O errors | Good for programmer errors/business rules |
| Can lead to boilerplate `throws` | Cleaner method signatures |

## 8. Interview questions

1. Checked vs unchecked exceptions — when do you use each?
2. What does try-with-resources do under the hood?
3. How would you design a global exception-handling strategy for a REST API?
4. What's the difference between `throw` and `throws`?

## 9. Short interview answer

"I use unchecked exceptions for business-rule violations so they don't pollute
method signatures across every layer, and handle them centrally with something
like `@RestControllerAdvice` so each exception type maps to one consistent HTTP
status and error body."

## 10. Related topics

- [REST API development](../06-spring/rest-api.md)
