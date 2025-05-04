# Firefly Common Core Library

A comprehensive foundation library for the Firefly platform that provides essential functionality for building robust, reactive microservices with Spring WebFlux.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
  - [Transaction Tracking](#transaction-tracking)
  - [WebClient Utilities](#webclient-utilities)
  - [Messaging System](#messaging-system)
  - [Resilience Patterns](#resilience-patterns)
  - [Actuator and Monitoring](#actuator-and-monitoring)
  - [Centralized Configuration](#centralized-configuration)
  - [Service Registry](#service-registry)
  - [Auto-Configuration](#auto-configuration)
- [Installation](#installation)
- [Usage](#usage)
  - [Transaction Tracking](#transaction-tracking-usage)
  - [WebClient Usage](#webclient-usage)
  - [WebClient Template](#webclient-template)
  - [Messaging System](#messaging-system-usage)
  - [Centralized Configuration](#centralized-configuration-usage)
  - [Service Registry](#service-registry-usage)
  - [Actuator and Monitoring](#actuator-and-monitoring-usage)
- [Configuration](#configuration)
  - [WebClient Properties](#webclient-properties)
  - [Messaging Properties](#messaging-properties)
  - [Cloud Config Properties](#cloud-config-properties)
  - [Service Registry Properties](#service-registry-properties)
  - [Actuator Properties](#actuator-properties)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)

## Overview

The Firefly Common Core Library is a foundational component of the Firefly platform, providing common utilities and configurations for building reactive microservices. It is built on top of Spring WebFlux and offers features like transaction tracking, WebClient configuration, messaging integration, centralized configuration, service discovery, and reactive HTTP client utilities.

This library serves as the backbone for all Firefly microservices, ensuring consistent behavior, traceability, and integration capabilities across the entire platform. By centralizing common functionality, it reduces code duplication and ensures best practices are followed throughout the ecosystem. The library's comprehensive set of features enables rapid development of robust, scalable, and maintainable microservices.

## Features

- **Transaction Tracking**: Automatically generates and propagates transaction IDs across microservices for distributed tracing
- **WebClient Utilities**: Pre-configured WebClient with transaction ID propagation and simplified API for making HTTP requests
- **Messaging System**: Annotation-based integration with multiple messaging systems (Kafka, RabbitMQ, SQS, etc.)
- **Resilience Patterns**: Built-in circuit breaker, retry, and timeout mechanisms for robust communication
- **Actuator and Monitoring**: Comprehensive health checks, metrics, and distributed tracing for monitoring applications (automatically configured)
- **Centralized Configuration**: Integration with Spring Cloud Config for centralized, dynamic configuration management
- **Service Registry**: Service registration and discovery with Eureka or Consul for microservices architecture
- **Auto-Configuration**: Spring Boot auto-configuration for easy integration and minimal setup

## Installation

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Or for Gradle:

```groovy
implementation 'com.catalis:lib-common-core:1.0.0-SNAPSHOT'
```

## Usage

### Transaction Tracking {#transaction-tracking-usage}

The library automatically adds a transaction filter that generates and propagates a unique transaction ID (`X-Transaction-Id`) across services. This ID is added to both incoming and outgoing requests, making it easier to trace requests across multiple services.

No additional configuration is needed as the `TransactionFilter` is automatically registered as a Spring component. The transaction ID is also available in the reactive context, allowing you to access it in your code:

```java
Mono.deferContextual(ctx -> {
    String transactionId = ctx.get("X-Transaction-Id");
    log.info("Processing request with transaction ID: {}", transactionId);
    return Mono.just("result");
});
```

### WebClient Usage

The library provides a pre-configured `WebClient` bean that automatically propagates the transaction ID. You can inject and use it directly:

```java
@Service
public class MyService {
    private final WebClient webClient;

    public MyService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<MyResponse> callExternalService() {
        return webClient.get()
                .uri("https://api.example.com/resource")
                .retrieve()
                .bodyToMono(MyResponse.class);
    }
}
```

### WebClient Template

For more convenience, you can use the `WebClientTemplate` which provides a higher-level API for making HTTP requests:

```java
@Service
public class MyService {
    private final WebClientTemplate webClientTemplate;

    public MyService(WebClientTemplate webClientTemplate) {
        this.webClientTemplate = webClientTemplate;
    }

    public Mono<MyResponse> getResource(ServerWebExchange exchange) {
        return webClientTemplate.get(
                "https://api.example.com",
                "/resource",
                MyResponse.class,
                exchange
        );
    }

    public Mono<MyResponse> createResource(MyRequest request, ServerWebExchange exchange) {
        return webClientTemplate.post(
                "https://api.example.com",
                "/resource",
                request,
                MyResponse.class,
                exchange
        );
    }
}
```

## Detailed Feature Documentation

### Transaction Tracking

The transaction tracking system ensures that every request flowing through your microservices architecture can be traced end-to-end. This is essential for debugging, monitoring, and understanding request flows in a distributed system.

Key components:
- `TransactionFilter`: Intercepts incoming requests and either uses an existing transaction ID or generates a new one
- `WebClient` configuration: Automatically propagates the transaction ID to outgoing requests
- Reactive context integration: Makes the transaction ID available throughout the reactive chain

### WebClient Utilities

The WebClient utilities provide a simplified and consistent way to make HTTP requests from your services, with automatic header propagation and error handling.

Key components:
- Pre-configured `WebClient` bean with transaction ID propagation
- `WebClientTemplate` for simplified API calls
- Automatic header propagation from incoming to outgoing requests
- Configurable header filtering

### Messaging System

The messaging system provides a declarative way to publish method results to various messaging platforms using a simple annotation.

Supported messaging systems:
- Spring Event Bus
- Apache Kafka
- RabbitMQ
- Amazon SQS
- Google Cloud Pub/Sub
- Azure Service Bus
- Redis Pub/Sub
- ActiveMQ/JMS

Key features:
- Annotation-based publishing with `@PublishResult`
- Support for reactive return types (Mono, Flux)
- Custom payload transformations
- Transaction ID propagation
- Configurable publishing behavior

### Resilience Patterns

The library implements various resilience patterns to make your services more robust and fault-tolerant.

Key patterns:
- Circuit Breaker: Prevents cascading failures by stopping calls to failing services
- Retry: Automatically retries failed operations with configurable backoff
- Timeout: Sets maximum duration for operations to prevent blocked threads
- Metrics: Collects and exposes metrics for monitoring and alerting

## Configuration

### WebClient Properties

You can configure the WebClient by adding properties to your `application.yml` or `application.properties` file:

```yaml
webclient:
  skip-headers:
    - connection
    - keep-alive
    - proxy-authenticate
    - proxy-authorization
    - te
    - trailer
    - transfer-encoding
    - upgrade
```

The `skip-headers` property specifies which HTTP headers should not be propagated when forwarding requests.

### Messaging Properties

Configure the messaging system with the following properties:

```yaml
messaging:
  # Enable or disable all messaging functionality (default: false)
  enabled: true

  # Enable or disable resilience features (circuit breaker, retry, metrics)
  resilience: true

  # Default timeout for publishing operations in seconds
  publish-timeout-seconds: 5

  # Kafka configuration
  kafka:
    enabled: true
    default-topic: events

  # RabbitMQ configuration
  rabbitmq:
    enabled: true
    default-exchange: events
    default-routing-key: default

  # Amazon SQS configuration
  sqs:
    enabled: false
    default-queue: events
    region: us-east-1

  # Google Cloud Pub/Sub configuration
  google-pub-sub:
    enabled: false
    default-topic: events
    project-id: my-project

  # Additional messaging systems configuration...
```

Refer to the [Messaging Module Documentation](src/main/java/com/catalis/common/core/messaging/README.md) for complete configuration options.

## Messaging System Usage {#messaging-system-usage}

The messaging system allows you to automatically publish the results of method executions to various messaging systems using simple annotations, as well as consume events from these systems.

### Step-by-Step Tutorial

#### 1. Enable Messaging in Your Application

First, enable the messaging functionality in your `application.yml` or `application.properties` file:

```yaml
messaging:
  enabled: true  # Enable the messaging system

  # Enable specific messaging systems as needed
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    default-topic: events

  rabbitmq:
    enabled: true
    host: localhost
    port: 5672
    default-exchange: events
```

#### 2. Add Required Dependencies

Depending on which messaging systems you want to use, add the appropriate dependencies to your project:

```xml
<!-- For Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- For RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- For Amazon SQS -->
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-messaging</artifactId>
</dependency>
```

#### 3. Publishing Events

##### Basic Event Publishing

Use the `@PublishResult` annotation on any method to automatically publish its return value as an event:

```java
import com.catalis.common.core.messaging.annotation.PublishResult;
import com.catalis.common.core.messaging.annotation.PublisherType;

@Service
public class UserService {

    @PublishResult(
        eventType = "user.created",
        publisher = PublisherType.EVENT_BUS  // Uses Spring's ApplicationEventPublisher
    )
    public User createUser(UserRequest request) {
        // Method implementation
        User user = new User(request.getName(), request.getEmail());
        return user;  // This result will be published as an event
    }
}
```

##### Publishing to Kafka

```java
@PublishResult(
    destination = "user-events",  // Kafka topic name
    eventType = "user.updated",  // Event type identifier
    publisher = PublisherType.KAFKA
)
public Mono<User> updateUser(String id, UserRequest request) {
    // Method implementation for updating a user
    return userRepository.findById(id)
        .flatMap(user -> {
            user.setName(request.getName());
            return userRepository.save(user);
        });
    // The result will be published to Kafka after the Mono completes
}
```

##### Publishing to RabbitMQ

```java
@PublishResult(
    destination = "user-exchange",  // RabbitMQ exchange name
    eventType = "user.deleted",    // Used as routing key by default
    publisher = PublisherType.RABBITMQ
)
public Mono<Void> deleteUser(String id) {
    // Method implementation for deleting a user
    return userRepository.deleteById(id);
    // Even with Mono<Void>, the event will be published with the ID as payload
}
```

##### Publishing to Amazon SQS

```java
@PublishResult(
    destination = "user-queue",  // SQS queue name
    eventType = "user.created",
    publisher = PublisherType.SQS
)
public User createUser(UserRequest request) {
    // Method implementation
    User user = new User(request.getName(), request.getEmail());
    return userRepository.save(user);
}
```

##### Custom Payload Transformation

You can customize the event payload using the `payloadExpression` parameter:

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.updated",
    publisher = PublisherType.KAFKA,
    payloadExpression = "{'id': result.id, 'name': result.name, 'action': 'update'}"
)
public User updateUser(String id, UserRequest request) {
    // Method implementation
    User user = userRepository.findById(id).orElseThrow();
    user.setName(request.getName());
    return userRepository.save(user);
}
```

##### Asynchronous vs Synchronous Publishing

By default, events are published asynchronously. You can change this behavior:

```java
@PublishResult(
    destination = "critical-events",
    eventType = "user.payment.processed",
    publisher = PublisherType.KAFKA,
    async = false  // Wait for the publishing to complete
)
public PaymentResult processPayment(PaymentRequest request) {
    // Method implementation
    return paymentProcessor.process(request);
}
```

#### 4. Consuming Events

Use the `@EventListener` annotation to consume events from messaging systems:

```java
import com.catalis.common.core.messaging.annotation.EventListener;
import com.catalis.common.core.messaging.annotation.SubscriberType;

@Service
public class UserEventHandler {

    @EventListener(
        source = "user-events",  // Kafka topic name
        eventType = "user.created",
        subscriber = SubscriberType.KAFKA
    )
    public void handleUserCreated(User user) {
        // Handle the user created event
        System.out.println("User created: " + user.getName());
    }

    @EventListener(
        source = "user-exchange",
        eventType = "user.updated",
        subscriber = SubscriberType.RABBITMQ
    )
    public void handleUserUpdated(User user, Map<String, Object> headers) {
        // Access event headers
        String transactionId = (String) headers.get("X-Transaction-Id");
        System.out.println("User updated: " + user.getName() + ", Transaction: " + transactionId);
    }
}
```

#### 5. Serialization Formats

The messaging system supports multiple serialization formats:

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.created",
    publisher = PublisherType.KAFKA,
    serializationFormat = SerializationFormat.AVRO  // Use Avro serialization
)
public UserAvro createUserAvro(UserRequest request) {
    // Method implementation returning an Avro-generated class
    return userAvroMapper.toAvro(user);
}
```

Supported formats include:
- `JSON` (default) - Uses Jackson for serialization
- `AVRO` - For Avro-generated classes
- `PROTOBUF` - For Protocol Buffers-generated classes
- `STRING` - Simple toString/fromString conversion
- `JAVA` - Java serialization for Serializable objects

#### 6. Resilience Features

The messaging system includes several resilience features:

```yaml
messaging:
  enabled: true
  resilience: true  # Enable resilience features
  publish-timeout-seconds: 5  # Timeout for publishing operations
```

These features include:
- **Circuit Breaker**: Prevents cascading failures by stopping calls to failing messaging systems
- **Automatic Retries**: Retries failed publishing operations with configurable backoff
- **Timeout Handling**: Sets maximum time for publishing operations
- **Metrics**: Collects metrics about publishing operations

#### 7. Transaction ID Propagation

The messaging system automatically propagates transaction IDs from HTTP requests to published events:

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.created",
    publisher = PublisherType.KAFKA,
    includeTransactionId = true  // Include transaction ID in event headers (default)
)
public User createUser(UserRequest request) {
    // Method implementation
    return user;
}
```

### Complete Configuration Example

Here's a comprehensive configuration example for the messaging system:

```yaml
messaging:
  # Enable or disable all messaging functionality (default: false)
  enabled: true

  # Enable or disable resilience features (circuit breaker, retry, metrics)
  resilience: true

  # Default timeout for publishing operations in seconds
  publish-timeout-seconds: 5

  # Serialization configuration
  serialization:
    # Default serialization format (JSON, AVRO, PROTOBUF, STRING, JAVA)
    default-format: JSON
    # Jackson configuration for JSON serialization
    jackson:
      # Whether to include null values in JSON
      include-nulls: false
      # Whether to use ISO-8601 date/time format
      use-iso-dates: true

  # Kafka configuration
  kafka:
    # Enable or disable Kafka publishing (default: false)
    enabled: true
    # Default topic to use if not specified in the annotation
    default-topic: events
    # Bootstrap servers for Kafka (comma-separated list)
    bootstrap-servers: localhost:9092
    # Client ID for Kafka producer
    client-id: messaging-publisher
    # Security protocol for Kafka (PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL)
    security-protocol: PLAINTEXT

  # RabbitMQ configuration
  rabbitmq:
    enabled: true
    default-exchange: events
    default-routing-key: default
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /

  # Amazon SQS configuration
  sqs:
    enabled: false
    default-queue: events
    region: us-east-1
    access-key-id: ""
    secret-access-key: ""

  # Amazon Kinesis configuration
  kinesis:
    enabled: false
    default-stream: events
    region: us-east-1
    access-key-id: ""
    secret-access-key: ""
```

For more details, see the [Messaging Module Documentation](src/main/java/com/catalis/common/core/messaging/README.md).

### Quick Start Example

Here's a complete example showing how to set up a Spring Boot application that publishes and consumes events using the messaging system:

#### 1. Configure Your Application

```yaml
# application.yml
spring:
  application:
    name: messaging-demo

messaging:
  enabled: true
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    default-topic: user-events
```

#### 2. Create a Domain Model

```java
// User.java
package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String name;
    private String email;
}
```

#### 3. Create a Service That Publishes Events

```java
// UserService.java
package com.example.demo.service;

import com.catalis.common.core.messaging.annotation.PublishResult;
import com.catalis.common.core.messaging.annotation.PublisherType;
import com.example.demo.model.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class UserService {

    @PublishResult(
        eventType = "user.created",
        publisher = PublisherType.KAFKA
    )
    public User createUser(String name, String email) {
        // In a real application, you would save to a database
        User user = new User(UUID.randomUUID().toString(), name, email);
        System.out.println("Created user: " + user.getName());
        return user;
    }

    @PublishResult(
        destination = "user-events",
        eventType = "user.updated",
        publisher = PublisherType.KAFKA
    )
    public Mono<User> updateUser(String id, String name, String email) {
        // In a real application, you would update in a database
        User user = new User(id, name, email);
        System.out.println("Updated user: " + user.getName());
        return Mono.just(user);
    }
}
```

#### 4. Create an Event Handler

```java
// UserEventHandler.java
package com.example.demo.handler;

import com.catalis.common.core.messaging.annotation.EventListener;
import com.catalis.common.core.messaging.annotation.SubscriberType;
import com.example.demo.model.User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserEventHandler {

    @EventListener(
        source = "user-events",
        eventType = "user.created",
        subscriber = SubscriberType.KAFKA
    )
    public void handleUserCreated(User user) {
        System.out.println("Received user.created event: " + user.getName());
    }

    @EventListener(
        source = "user-events",
        eventType = "user.updated",
        subscriber = SubscriberType.KAFKA
    )
    public void handleUserUpdated(User user, Map<String, Object> headers) {
        System.out.println("Received user.updated event: " + user.getName());
        System.out.println("Event headers: " + headers);
    }
}
```

#### 5. Create a REST Controller to Trigger Events

```java
// UserController.java
package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User createUser(@RequestParam String name, @RequestParam String email) {
        return userService.createUser(name, email);
    }

    @PutMapping("/{id}")
    public Mono<User> updateUser(
            @PathVariable String id,
            @RequestParam String name,
            @RequestParam String email) {
        return userService.updateUser(id, name, email);
    }
}
```

#### 6. Create the Application Class

```java
// DemoApplication.java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

#### 7. Test the Application

Start your application and make HTTP requests to trigger events:

```bash
# Create a user (will publish a user.created event)
curl -X POST "http://localhost:8080/users?name=John&email=john@example.com"

# Update a user (will publish a user.updated event)
# Replace USER_ID with the ID returned from the create request
curl -X PUT "http://localhost:8080/users/USER_ID?name=John+Updated&email=john.updated@example.com"
```

You should see log messages showing:
1. The service creating/updating users
2. The event handler receiving the published events

This demonstrates the full cycle of publishing and consuming events using the messaging system.

### Troubleshooting

Here are some common issues you might encounter when using the messaging system and how to resolve them:

#### Events Are Not Being Published

1. **Check if messaging is enabled**:
   - Ensure `messaging.enabled=true` is set in your application properties
   - For the specific messaging system, ensure its enabled property is set (e.g., `messaging.kafka.enabled=true`)

2. **Verify dependencies**:
   - Make sure you have the required dependencies for your messaging system (e.g., `spring-kafka` for Kafka)

3. **Check connection settings**:
   - Verify that connection details (hosts, ports, credentials) are correct
   - For Kafka, check that the bootstrap servers are reachable
   - For RabbitMQ, verify the host, port, and credentials

4. **Enable debug logging**:
   ```yaml
   logging:
     level:
       com.catalis.common.core.messaging: DEBUG
   ```

#### Deserialization Issues

1. **Class not found**:
   - If you see `ClassNotFoundException` or similar errors, ensure the consumer has access to the same model classes as the producer

2. **JSON parsing errors**:
   - Check that your model classes have proper constructors and getters/setters
   - For complex objects, consider using a custom serializer/deserializer

3. **Wrong serialization format**:
   - Ensure both publisher and subscriber use the same serialization format

#### Listener Not Receiving Events

1. **Group ID issues (Kafka)**:
   - For Kafka, check that the consumer group ID is correctly configured
   - If multiple instances should receive the same events, use different group IDs

2. **Topic/queue doesn't exist**:
   - Verify that the topic or queue exists and is correctly spelled
   - Some systems (like Kafka) can auto-create topics, others require manual creation

3. **Permissions issues**:
   - Check that your application has the necessary permissions to read from the topic/queue

#### Performance Issues

1. **Slow publishing**:
   - Consider using asynchronous publishing (`async = true`, which is the default)
   - Adjust batch settings for the messaging system
   - Increase thread pool size if necessary

2. **High memory usage**:
   - For large messages, consider using a more efficient serialization format (Avro or Protobuf)
   - Adjust maximum message size settings

3. **Listener overload**:
   - Adjust concurrency settings for listeners
   - Implement backpressure mechanisms
   - Consider using a dead letter queue for failed messages

### Best Practices

Follow these best practices to get the most out of the messaging system:

#### Event Design

1. **Use meaningful event types**:
   - Create a consistent naming convention (e.g., `entity.action`)
   - Examples: `user.created`, `order.shipped`, `payment.failed`

2. **Keep events small and focused**:
   - Include only necessary data in events
   - Consider using references instead of embedding large objects
   - For large payloads, consider storing the data elsewhere and including a reference

3. **Version your events**:
   - Include a version in your event type or payload
   - Example: `user.created.v1`
   - This allows for easier evolution of event schemas

#### Configuration

1. **Start with messaging disabled in development**:
   - Enable messaging explicitly in environments where it's needed
   - This prevents unexpected behavior during development

2. **Use environment-specific configurations**:
   - Use Spring profiles to configure different messaging settings per environment
   - Example: Use in-memory queues for testing, real queues for production

3. **Configure appropriate timeouts**:
   - Set reasonable timeouts based on your system's requirements
   - Consider the impact of slow messaging systems on your application

#### Error Handling

1. **Implement proper error handling**:
   - Catch and log exceptions in event listeners
   - Consider using a circuit breaker for publishing to unreliable systems

2. **Set up dead letter queues**:
   - Configure dead letter queues for messages that can't be processed
   - Implement monitoring and alerting for dead letter queues

3. **Implement retry logic**:
   - Use the built-in retry mechanism for transient failures
   - Configure appropriate retry intervals and maximum attempts

#### Testing

1. **Use test-specific configurations**:
   - Create a test configuration that uses in-memory messaging
   - For Kafka, consider using an embedded Kafka server for tests

2. **Mock messaging systems in unit tests**:
   - Use mocks or stubs for messaging systems in unit tests
   - Focus on testing the business logic, not the messaging infrastructure

3. **Write integration tests**:
   - Test the full flow from publishing to consuming
   - Verify that events are correctly serialized and deserialized

#### Monitoring

1. **Enable metrics collection**:
   - Use the built-in metrics to monitor messaging performance
   - Export metrics to a monitoring system like Prometheus

2. **Set up alerts**:
   - Alert on high error rates or slow processing
   - Monitor queue depths and processing latency

3. **Implement distributed tracing**:
   - Use the transaction ID propagation to trace events across services
   - Integrate with a distributed tracing system like Zipkin or Jaeger

## Building from Source

To build the library from source:

```bash
git clone https://github.com/firefly-oss/lib-common-core.git
cd lib-common-core
./mvnw clean install
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Centralized Configuration {#centralized-configuration-usage}

The library provides integration with Spring Cloud Config for centralized configuration management, allowing you to externalize configuration and dynamically update it without restarting your applications.

### Enabling Centralized Configuration

To enable centralized configuration, add the following to your `application.yml` or `application.properties` file:

```yaml
cloud:
  config:
    enabled: true
    uri: http://config-server:8888
```

### Dynamic Configuration Refresh

The library supports dynamic configuration refresh, allowing you to update configuration properties without restarting your application. To use this feature:

1. Expose the refresh endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: refresh,health,info
```

2. Annotate beans that should be refreshed with `@RefreshScope`:

```java
@Service
@RefreshScope
public class MyService {
    @Value("${my.dynamic.property}")
    private String dynamicProperty;

    // This bean will be recreated when a refresh event occurs
}
```

3. Trigger a refresh by sending a POST request to the refresh endpoint:

```bash
curl -X POST http://your-application:8080/actuator/refresh
```

### Complete Configuration Example

Here's a comprehensive configuration example for Spring Cloud Config:

```yaml
cloud:
  config:
    enabled: true
    uri: http://config-server:8888
    name: my-service  # Defaults to spring.application.name
    profile: dev      # Defaults to active profiles
    label: main       # Git branch or other version label
    fail-fast: true   # Whether to fail startup if config server is unavailable
    timeout-ms: 5000  # Timeout for config server requests
    retry: true       # Whether to retry failed requests
    refresh-enabled: true  # Whether to enable dynamic refresh
```

For more details, see the [Centralized Configuration Documentation](src/main/java/com/catalis/common/core/config/cloud/README.md).

## Service Registry {#service-registry-usage}

The library provides integration with service registry systems like Netflix Eureka and HashiCorp Consul, enabling service discovery in a microservices architecture.

### Enabling Service Registry

To enable service registry, add the following to your `application.yml` or `application.properties` file:

```yaml
service:
  registry:
    enabled: true
    type: EUREKA  # or CONSUL
```

### Configuring Eureka

If you're using Eureka as your service registry:

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

### Configuring Consul

If you're using Consul as your service registry:

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

### Using the ServiceRegistryHelper

The library provides a `ServiceRegistryHelper` class that makes it easy to discover and interact with other services:

```java
@Service
public class MyService {
    private final ServiceRegistryHelper serviceRegistryHelper;
    private final WebClient webClient;

    public MyService(ServiceRegistryHelper serviceRegistryHelper, WebClient webClient) {
        this.serviceRegistryHelper = serviceRegistryHelper;
        this.webClient = webClient;
    }

    public Mono<MyResponse> callOtherService() {
        // Get a URI to another service
        Optional<URI> serviceUri = serviceRegistryHelper.getServiceUri("other-service", "/api/resource");

        // Use the URI to make a request
        return serviceUri
            .map(uri -> webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(MyResponse.class))
            .orElse(Mono.error(new ServiceNotFoundException("other-service")));
    }
}
```

### Complete Configuration Example

Here's a comprehensive configuration example for service registry:

```yaml
service:
  registry:
    enabled: true
    type: EUREKA  # or CONSUL

    # Eureka configuration
    eureka:
      service-url: http://eureka-server:8761/eureka/
      register: true
      fetch-registry: true
      registry-fetch-interval-seconds: 30
      prefer-ip-address: true
      lease-renewal-interval-in-seconds: 30
      lease-expiration-duration-in-seconds: 90
      health-check-enabled: true
      health-check-url-path: /actuator/health
      status-page-url-path: /actuator/info

    # Consul configuration
    consul:
      host: consul
      port: 8500
      register: true
      deregister: true
      health-check-interval: 10
      health-check-timeout: 5
      health-check-path: /actuator/health
      health-check-enabled: true
      catalog-services-watch: true
      catalog-services-watch-timeout: 10
      catalog-services-watch-delay: 1000
```

For more details, see the [Service Registry Documentation](src/main/java/com/catalis/common/core/config/registry/README.md).

## Actuator and Monitoring {#actuator-and-monitoring-usage}

The library provides Spring Boot Actuator integration for monitoring and managing your application. It includes configuration for health checks, metrics, and distributed tracing.

### Automatic Configuration

The actuator endpoints are automatically configured and enabled when you include this library as a dependency. The following endpoints are available out of the box:

- `/actuator/health`: Shows application health information
- `/actuator/info`: Displays application information
- `/actuator/metrics`: Shows metrics information
- `/actuator/prometheus`: Exposes metrics in Prometheus format

#### Required Dependencies

To use the actuator features, include the following dependencies in your project:

```xml
<!-- Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

For additional features, you can add these optional dependencies:

```xml
<!-- Prometheus Metrics (Optional) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Distributed Tracing (Optional) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### Customizing Actuator Configuration

If you want to customize the actuator configuration, you can add the following to your `application.yml` file:

```yaml
management:
  endpoints:
    web:
      # Customize which endpoints to expose
      exposure: "health,info,metrics,prometheus,loggers,env"
  metrics:
    tags:
      # Add custom tags to metrics
      custom-tag: custom-value
  health:
    show-details: always
```

### Health Checks

The library automatically provides health checks for:

- Disk space (warns when available space is below 10MB)
- Messaging systems (checks status of enabled messaging systems)
- Application information

You can customize the health check behavior:

```yaml
management:
  health:
    show-details: always
    show-components: true
    disk-space:
      enabled: true
      threshold: 50MB  # Increase threshold to 50MB
```

### Distributed Tracing

The library integrates with the existing transaction ID mechanism to provide distributed tracing. To enable Zipkin integration:

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1
    zipkin:
      enabled: true
      base-url: http://localhost:9411
```

### Logging for Tracing

The library automatically configures logging to include trace and transaction IDs. The default pattern is:

```
%5p [%X{traceId:-},%X{spanId:-},%X{X-Transaction-Id:-}]
```

You can customize this pattern in your `application.yml` file:

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-},%X{X-Transaction-Id:-}]"
```

### Complete Configuration Example

Here's a comprehensive configuration example for actuator and monitoring:

```yaml
management:
  endpoints:
    enabled: true
    web:
      exposure: "*"
      base-path: /actuator
  metrics:
    enabled: true
    tags:
      application: ${spring.application.name:application}
      environment: ${spring.profiles.active:default}
    prometheus:
      enabled: true
  tracing:
    enabled: true
    sampling:
      probability: 0.1
    zipkin:
      enabled: true
      base-url: http://localhost:9411
  health:
    show-details: always
    show-components: true
```

For more details, see the [Actuator Module Documentation](src/main/java/com/catalis/common/core/actuator/README.md).

### Cloud Config Properties {#cloud-config-properties}

You can configure the Spring Cloud Config client by adding properties to your `application.yml` or `application.properties` file:

```yaml
cloud:
  config:
    # Enable or disable Spring Cloud Config client
    enabled: true
    # URI of the Spring Cloud Config server
    uri: http://config-server:8888
    # Name of the application to retrieve configuration for
    # Defaults to the value of spring.application.name if not specified
    name: my-service
    # Profile to use for configuration retrieval
    # Defaults to the active profiles if not specified
    profile: dev
    # Label to use for configuration retrieval (e.g., Git branch)
    label: main
    # Whether to fail startup if unable to connect to the config server
    fail-fast: true
    # Timeout in milliseconds for config server requests
    timeout-ms: 5000
    # Whether to retry failed requests to the config server
    retry: true
    # Maximum number of retries for failed requests
    max-retries: 6
    # Initial retry interval in milliseconds
    initial-retry-interval-ms: 1000
    # Maximum retry interval in milliseconds
    max-retry-interval-ms: 2000
    # Multiplier for the retry interval
    retry-multiplier: 1.1
    # Whether to enable dynamic refresh of configuration
    refresh-enabled: true
```

### Service Registry Properties {#service-registry-properties}

You can configure the service registry client by adding properties to your `application.yml` or `application.properties` file:

```yaml
service:
  registry:
    # Enable or disable service registry
    enabled: true
    # Type of service registry to use (EUREKA or CONSUL)
    type: EUREKA

    # Eureka client configuration
    eureka:
      # Service URL of the Eureka server
      service-url: http://eureka-server:8761/eureka/
      # Whether to register with Eureka
      register: true
      # Whether to fetch registry from Eureka
      fetch-registry: true
      # Registry fetch interval in seconds
      registry-fetch-interval-seconds: 30
      # Instance ID to use when registering with Eureka
      # If not specified, a default ID will be generated
      instance-id: my-service-instance
      # Prefer IP address rather than hostname for registration
      prefer-ip-address: true
      # Lease renewal interval in seconds
      lease-renewal-interval-in-seconds: 30
      # Lease expiration duration in seconds
      lease-expiration-duration-in-seconds: 90
      # Whether to enable health check
      health-check-enabled: true
      # Health check URL path
      health-check-url-path: /actuator/health
      # Status page URL path
      status-page-url-path: /actuator/info

    # Consul client configuration
    consul:
      # Host of the Consul server
      host: consul
      # Port of the Consul server
      port: 8500
      # Whether to register with Consul
      register: true
      # Whether to deregister on shutdown
      deregister: true
      # Service name to use when registering with Consul
      # If not specified, the spring.application.name will be used
      service-name: my-service
      # Instance ID to use when registering with Consul
      # If not specified, a default ID will be generated
      instance-id: my-service-instance
      # Tags to apply to the service
      tags:
        - microservice
        - spring-boot
      # Health check interval in seconds
      health-check-interval: 10
      # Health check timeout in seconds
      health-check-timeout: 5
      # Health check URL path
      health-check-path: /actuator/health
      # Whether to enable health check
      health-check-enabled: true
      # Whether to use the catalog services API
      catalog-services-watch: true
      # Catalog services watch timeout in seconds
      catalog-services-watch-timeout: 10
      # Catalog services watch delay in milliseconds
      catalog-services-watch-delay: 1000
```

### Actuator Properties {#actuator-properties}

The library provides a comprehensive set of configuration properties for customizing actuator behavior:

```yaml
management:
  # Endpoints configuration
  endpoints:
    # Enable or disable all endpoints
    enabled: true

    # Web exposure configuration
    web:
      # Endpoints to expose via web. Use '*' for all endpoints or a comma-separated list
      exposure: "*"
      # Base path for actuator endpoints
      base-path: /actuator
      # Whether to include details in responses
      include-details: true

    # JMX exposure configuration
    jmx:
      # Endpoints to expose via JMX. Use '*' for all endpoints or a comma-separated list
      exposure: "*"

  # Metrics configuration
  metrics:
    # Enable or disable metrics collection
    enabled: true
    # Tags to add to all metrics
    tags:
      application: ${spring.application.name:application}
      environment: ${spring.profiles.active:default}

    # Prometheus configuration
    prometheus:
      # Enable or disable Prometheus metrics endpoint
      enabled: true
      # Path for Prometheus metrics endpoint
      path: /actuator/prometheus

  # Tracing configuration
  tracing:
    # Enable or disable distributed tracing
    enabled: true

    # Sampling configuration
    sampling:
      # Probability for sampling traces (0.0 - 1.0)
      probability: 0.1

    # Zipkin configuration
    zipkin:
      # Enable or disable Zipkin tracing
      enabled: false
      # Base URL for Zipkin server
      base-url: http://localhost:9411
      # Service name for Zipkin traces
      service-name: ${spring.application.name:application}
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

If you have any questions or need assistance, please open an issue on the GitHub repository.