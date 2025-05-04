# Service Registry and Discovery

This package provides integration with service registry and discovery systems like Netflix Eureka and HashiCorp Consul for microservices.

## Features

- Automatic service registration with Eureka or Consul
- Service discovery for client-side load balancing
- Health check integration with Spring Boot Actuator
- Helper utilities for working with discovered services
- Support for multiple service registry types

## Usage

### 1. Enable Service Registry

Add the following to your `application.yml` or `application.properties`:

```yaml
service:
  registry:
    enabled: true
    type: EUREKA  # or CONSUL
```

### 2. Configure Eureka (if using Eureka)

```yaml
service:
  registry:
    enabled: true
    type: EUREKA
    eureka:
      service-url: http://eureka-server:8761/eureka/
      register: true
      fetch-registry: true
      prefer-ip-address: true
```

### 3. Configure Consul (if using Consul)

```yaml
service:
  registry:
    enabled: true
    type: CONSUL
    consul:
      host: consul
      port: 8500
      register: true
      tags:
        - microservice
        - spring-boot
```

### 4. Enable Health Check Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

### 5. Use the ServiceRegistryHelper for Service Discovery

```java
@Service
public class MyService {
    private final ServiceRegistryHelper serviceRegistryHelper;
    
    public MyService(ServiceRegistryHelper serviceRegistryHelper) {
        this.serviceRegistryHelper = serviceRegistryHelper;
    }
    
    public void callOtherService() {
        // Get a URI to another service
        Optional<URI> serviceUri = serviceRegistryHelper.getServiceUri("other-service", "/api/resource");
        
        // Use the URI to make a request
        serviceUri.ifPresent(uri -> {
            // Make HTTP request to the service
        });
    }
}
```

## Example Configuration

See `application-service-registry-example.yml` for a complete example configuration.

## Service Registry Server Setup

### Eureka Server Setup

1. Create a new Spring Boot project with the Spring Cloud Netflix Eureka Server dependency
2. Add `@EnableEurekaServer` to your main application class
3. Configure the server

Example `application.yml` for the Eureka server:

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
```

### Consul Setup

1. Download and install Consul from https://www.consul.io/downloads
2. Start Consul in development mode:

```
consul agent -dev
```

For production, refer to the Consul documentation for proper setup.

## Best Practices

1. Use meaningful service names that reflect the service's purpose
2. Configure appropriate health checks to ensure accurate service status
3. Set appropriate timeouts and retry intervals for service discovery
4. Use service discovery in conjunction with client-side load balancing
5. Implement circuit breakers for resilience
6. Monitor the service registry for availability and performance
7. Use tags or metadata to provide additional information about services
8. Consider using multiple registry instances for high availability
