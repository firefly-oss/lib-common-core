# Firefly Common Core Library

A comprehensive foundation library for the Firefly platform that provides essential functionality for building robust, reactive microservices with Spring WebFlux.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
  - [Transaction Tracking](#transaction-tracking)
  - [WebClient Utilities](#webclient-utilities)
  - [Messaging System](#messaging-system)
  - [Resilience Patterns](#resilience-patterns)
  - [Auto-Configuration](#auto-configuration)
- [Installation](#installation)
- [Usage](#usage)
  - [Transaction Tracking](#transaction-tracking-usage)
  - [WebClient Usage](#webclient-usage)
  - [WebClient Template](#webclient-template)
  - [Messaging System](#messaging-system-usage)
- [Configuration](#configuration)
  - [WebClient Properties](#webclient-properties)
  - [Messaging Properties](#messaging-properties)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)

## Overview

The Firefly Common Core Library is a foundational component of the Firefly platform, providing common utilities and configurations for building reactive microservices. It is built on top of Spring WebFlux and offers features like transaction tracking, WebClient configuration, messaging integration, and reactive HTTP client utilities.

This library serves as the backbone for all Firefly microservices, ensuring consistent behavior, traceability, and integration capabilities across the entire platform. By centralizing common functionality, it reduces code duplication and ensures best practices are followed throughout the ecosystem.

## Features

- **Transaction Tracking**: Automatically generates and propagates transaction IDs across microservices for distributed tracing
- **WebClient Utilities**: Pre-configured WebClient with transaction ID propagation and simplified API for making HTTP requests
- **Messaging System**: Annotation-based integration with multiple messaging systems (Kafka, RabbitMQ, SQS, etc.)
- **Resilience Patterns**: Built-in circuit breaker, retry, and timeout mechanisms for robust communication
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

The messaging system allows you to automatically publish the results of method executions to various messaging systems using a simple annotation.

### Basic Usage

```java
@Service
public class UserService {

    @PublishResult(
        eventType = "user.created",
        publisher = PublisherType.EVENT_BUS
    )
    public User createUser(UserRequest request) {
        // Method implementation
        return user;
    }
}
```

### Publishing to Kafka

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.updated",
    publisher = PublisherType.KAFKA
)
public Mono<User> updateUser(String id, UserRequest request) {
    // Method implementation
    return Mono.just(user);
}
```

### Custom Payload Transformation

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.updated",
    publisher = PublisherType.KAFKA,
    payloadExpression = "{'id': result.id, 'name': result.name, 'action': 'update'}"
)
public User updateUser(String id, UserRequest request) {
    // Method implementation
    return user;
}
```

For more details, see the [Messaging Module Documentation](src/main/java/com/catalis/common/core/messaging/README.md).

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

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

If you have any questions or need assistance, please open an issue on the GitHub repository.