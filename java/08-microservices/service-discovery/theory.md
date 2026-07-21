# Service Discovery and Configuration

## Service Discovery
Service Discovery is a mechanism in microservices architecture that allows services to automatically find and communicate with each other without hard-coding IP addresses or ports. 

### Why Service Discovery is needed? 
- Services scale up/down dynamically 
- IPs & ports change frequently 
- Manual configuration is not possible 

### How Does Service Discovery Works? 
Service Discovery handles things in two parts. First, it provides a mechanism for an instance to register and say, “I’m here!” Second, it provides a way to find the service once it has registered. 

Example: a Service Consumer and a Service Provider. The Service Consumer needs the Service Provider to read and write data. 
Steps: 
1. The location of the Service Provider is sent to the Service Registry (a database containing the locations of all available service instances). 
2. The Service Consumer asks the Service Discovery Server for the location of the Service Provider. 
3. The location of the Service Provider is searched by the Service Registry in its internal database and returned to the Service Consumer. 
4. The Service Consumer can now make direct requests to the Service Provider. 

### Service Discovery Implementations 
**1- Client-Side Service Discovery** 
When using Client-Side Discovery, the Service Consumer is responsible for determining the network locations of available service instances and load balancing requests between them. The client queries the Service Registry. Then the client uses a load-balancing algorithm to choose one of the available service instances and performs a request. 
Pros: saves an extra hop that we would’ve had with a dedicated load balancer. 
Cons: Service Consumer must implement the load balancing logic, coupling Consumer and Registry. Must be implemented for each programming language.

**2- Server-Side Service Discovery** 
Uses an intermediary that acts as a Load Balancer. The client makes a request to a service via a load balancer that acts as an orchestrator. The load balancer queries the Service Registry and routes each request to an available service instance. 
Pros: Service Consumer is lighter, no need to deal with lookup procedure. No need to implement discovery logic in multiple languages. 
Cons: Must set up and manage the Load Balancer.

## Service Registry
A Service Registry serves as a centralized database or directory where information about available services and their locations is stored and maintained. It acts as a vital component of service discovery by providing a central point for service registration, lookup, and management. 

### How a Service Registry typically works: 
1. **Service Registration:** When a microservice instance starts up or becomes available, it registers itself with the Service Registry. This includes providing metadata such as the service name, network location (IP address and port), health status, and possibly other attributes. 
2. **Service Lookup:** Clients or other microservices that need to communicate query the Service Registry to dynamically discover the available instances. 
3. **Health Monitoring:** Service Registries often include health-checking mechanisms to monitor the status of registered service instances, removing unhealthy or unavailable ones. 
4. **Load Balancing:** Some Service Registries incorporate load-balancing capabilities. 
5. **Dynamic Updates:** Continuous updates to reflect changes in availability and health.

## Configuration Servers
In a microservices architecture, each service may need different configurations (databases, API URLs, credentials, feature flags). Managing these individually becomes error-prone and hard to maintain. A Config Server centralizes configuration management and provides it to all microservices dynamically. 

### Key Roles of a Config Server 
1. **Centralized Configuration Management:** Stores all microservice configurations in a single repository (Git, SVN, filesystem). Avoids duplicating config. 
2. **Environment-Specific Configurations:** Supports dev, test, prod environments. Services fetch the correct config based on their profile. 
3. **Dynamic Refresh / Hot Reload:** Microservices can reload configuration at runtime without restarting. Useful for feature flags or endpoint changes. 
4. **Security & Version Control:** Sensitive data can be encrypted. Config changes are version-controlled in Git. 
5. **Consistency Across Services:** Ensures that all services use the same configuration values. Prevents mismatched or outdated settings.
