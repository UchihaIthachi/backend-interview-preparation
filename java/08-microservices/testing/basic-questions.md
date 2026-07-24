# Testing — Basic Interview Questions

## Q1: How Do You Verify That a Method Is Called Twice in Mockito?

Use Mockito’s `verify()` method with `times(2)`.

```java
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

verify(notificationService, times(2))
        .sendNotification();
```

### Complete Example

```java
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OrderServiceTest {

    @Test
    void shouldSendNotificationTwice() {
        NotificationService notificationService =
                mock(NotificationService.class);

        OrderService orderService =
                new OrderService(notificationService);

        orderService.processOrders();

        verify(notificationService, times(2))
                .sendNotification();
    }
}
```

You can also verify method arguments:

```java
verify(notificationService, times(2))
        .sendNotification("Order completed");
```

### Other Verification Modes

```java
verify(service, times(1)).execute();
verify(service, never()).execute();
verify(service, atLeastOnce()).execute();
verify(service, atLeast(2)).execute();
verify(service, atMost(3)).execute();
```

Because one invocation is the default, these are equivalent:

```java
verify(service).execute();
verify(service, times(1)).execute();
```

### Interview Answer

> I use `Mockito.verify(mock, times(2))` followed by the expected method call. Mockito then verifies that the method was invoked exactly twice with the specified arguments.

---

## Q2: In Which Situations Do You Use PowerMock?

PowerMock is mainly used when testing **legacy or tightly coupled Java code** that contains constructs that older Mockito versions could not mock easily.

Typical cases include:

- Static methods
- Private methods
- Final classes or final methods
- Constructors invoked directly with `new`
- Static initializers
- Some system or framework classes

### Example Legacy Code

```java
public class PaymentService {

    public PaymentResult process(PaymentRequest request) {
        PaymentGateway gateway = new PaymentGateway();

        String reference =
                ReferenceGenerator.generateReference();

        return gateway.charge(
                request,
                reference
        );
    }
}
```

This class is difficult to test because it:

- Creates `PaymentGateway` directly.
- Calls a static method.
- Does not allow dependencies to be injected.

Historically, PowerMock could intercept:

```java
new PaymentGateway()
ReferenceGenerator.generateReference()
```

---

# When PowerMock May Be Considered

## 1. Mocking Static Methods in Legacy Code

```java
String reference =
        ReferenceGenerator.generateReference();
```

PowerMock was commonly used when the static dependency could not be changed.

## 2. Mocking Constructor Calls

```java
PaymentGateway gateway =
        new PaymentGateway();
```

PowerMock can replace the object created by the constructor with a mock.

## 3. Mocking Private Methods

PowerMock can access and stub private methods.

However, directly testing private methods is usually discouraged. Tests should verify observable behavior through the public API.

## 4. Mocking Final Classes or Methods

Older Mockito versions could not mock final types without additional configuration, so PowerMock was often used.

## 5. Suppressing Static Initializers

PowerMock can suppress problematic static initialization in legacy classes.

This may be useful when a static initializer:

- Connects to an external system.
- Loads unavailable native libraries.
- Reads production-only configuration.
- Performs heavy work during class loading.

---

# Why PowerMock Should Be Avoided in New Code

PowerMock performs bytecode manipulation and uses custom class loading. This can cause:

- Difficult test configuration
- Slow test execution
- Compatibility problems
- Fragile tests
- Problems with newer JUnit, Mockito, and Java versions
- Tests that depend heavily on implementation details

Modern Mockito can directly handle several cases previously associated with PowerMock.

## Mocking a Static Method with Mockito

```java
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class ReferenceGeneratorTest {

    @Test
    void shouldMockStaticReferenceGeneration() {
        try (MockedStatic<ReferenceGenerator> mocked =
                     mockStatic(ReferenceGenerator.class)) {

            mocked.when(
                    ReferenceGenerator::generateReference
            ).thenReturn("REF-100");

            assertEquals(
                    "REF-100",
                    ReferenceGenerator.generateReference()
            );
        }
    }
}
```

## Mocking Constructor Calls with Mockito

```java
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

class PaymentServiceTest {

    @Test
    void shouldMockCreatedGateway() {
        try (MockedConstruction<PaymentGateway> mocked =
                     mockConstruction(PaymentGateway.class)) {

            PaymentService service =
                    new PaymentService();

            service.process(new PaymentRequest());

            PaymentGateway gateway =
                    mocked.constructed().getFirst();

            verify(gateway)
                    .charge(
                            org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.anyString()
                    );
        }
    }
}
```

---

# Preferred Solution: Refactor the Code

Instead of mocking constructors and static methods, inject dependencies.

```java
public class PaymentService {

    private final PaymentGateway paymentGateway;
    private final ReferenceGenerator referenceGenerator;

    public PaymentService(
            PaymentGateway paymentGateway,
            ReferenceGenerator referenceGenerator
    ) {
        this.paymentGateway = paymentGateway;
        this.referenceGenerator = referenceGenerator;
    }

    public PaymentResult process(
            PaymentRequest request
    ) {
        String reference =
                referenceGenerator.generate();

        return paymentGateway.charge(
                request,
                reference
        );
    }
}
```

The test becomes straightforward:

```java
@Test
void shouldProcessPayment() {
    PaymentGateway gateway =
            mock(PaymentGateway.class);

    ReferenceGenerator generator =
            mock(ReferenceGenerator.class);

    when(generator.generate())
            .thenReturn("REF-100");

    PaymentService service =
            new PaymentService(
                    gateway,
                    generator
            );

    PaymentRequest request =
            new PaymentRequest();

    service.process(request);

    verify(generator).generate();

    verify(gateway).charge(
            request,
            "REF-100"
    );
}
```

---

# Interview-Ready Answer

> PowerMock is mainly used for testing legacy code that contains static methods, private methods, final types, constructor calls, or static initialization that cannot easily be changed. However, I avoid using it in new code because it relies on bytecode manipulation and can create fragile tests. Modern Mockito supports static and constructor mocking, and the preferred solution is usually to refactor the code and inject dependencies.
