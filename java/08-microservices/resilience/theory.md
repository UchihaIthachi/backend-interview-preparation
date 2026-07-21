# Resilience and Circuit Breaker Pattern

## What is Circuit Breaker?
The Circuit Breaker pattern is like a safety switch for microservices. Suppose your online store depends on a payment service. If that service starts failing repeatedly, the circuit breaker “trips” and stops further calls for a while, preventing cascades and giving the service time to recover. 

### Example:
- Your store makes a request to the payment service to process a payment. Everything works fine. 
- Suddenly, the payment service has issues and fails three times in a row. 
- The circuit breaker trips and enters an "open" state. Now, when your store tries to contact the payment service, it immediately gets an error response instead of trying to connect again. 
- After a set time, the circuit breaker changes to a "half-open" state. It allows a few test requests to see if the payment service is back online. 
- If those requests succeed, the circuit breaker resets to "closed," and everything goes back to normal. If they fail, it stays open longer, giving the payment service more time to recover. 

### Characteristics of Circuit Breaker Pattern 
- Improves fault tolerance by isolating failing services. 
- Monitors latency, error rate, and timeouts. 
- Prevents cascading failures. 
- Supports fallbacks (cached data, default responses, queues). 
- Auto-recovers when the service stabilizes. 

### Steps to Implement Circuit Breaker Pattern 
- **Step 1: Identify Dependencies:** Going for the external services that will bring interactions and make the microservice functional in turn. 
- **Step 2: Choose a Circuit Breaker Library:** Choose a circuit breaker library, or create your own framework.
- **Step 3: Integrate Circuit Breaker into Code:** Insert the selected circuit breaker library into your microservices code base. 
- **Step 4: Define Failure Thresholds:** Set boundaries for faults and time-outs that turn the mechanism of the circuit breaker to open. 
- **Step 5: Implement Fallback Mechanisms:** Include whenever the circuit has open or close requests, the fallback mechanism should be implemented. 
- **Step 6: Monitor Circuit Breaker Metrics:** Use statistics built into the circuit breaker library to see the health and behaviour of your services.
- **Step 7: Tune Configuration Parameters:** Tuning configuration parameters like timeouts, thresholds, and retry methods. 
- **Step 8: Test Circuit Breaker Behavior:** Perform live testing of your circuit breaker during different operating states.
- **Step 9: Deploy and Monitor:** Move/deploy your microservice with circuit breaker into your production environment. 

### Use Cases of Circuit Breaker Pattern 
- When microservices are communicating with one another over the network through the pattern of the Circuit breaker, it helps deal with network failures, unavailability of service, or slow responses. 
- It avoids collateral damage of failures by serving as a barrier between a final service and providing alternative options when failure occurs. 
- The microservices are the APIs or services which may be external or from other parties. 
- This Circuit Breaker pattern can be included as a contingency to mitigate against failures in the integrations, enabling the whole system to stay functional even when the external parties are affected by unforeseen issues. 
