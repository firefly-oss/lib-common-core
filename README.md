# Firefly Common Core Library

A comprehensive Spring Boot 3.2.2 library providing enterprise-grade common components for reactive microservices architecture. This library offers messaging, logging, configuration, monitoring, and resilience capabilities with multi-cloud platform support.

## Features

### 🚀 Reactive Web Framework
- **WebFlux Configuration**: Pre-configured reactive web components
- **WebClient Integration**: Enhanced HTTP client with auto-configuration
- **Resilience Patterns**: Circuit breakers, retry mechanisms, and timeout handling
- **Filter Configuration**: Custom web filters for request/response processing

### 📨 Enterprise Messaging Framework
- **Multi-Platform Support**: Kafka, ActiveMQ, AMQP, Redis, AWS SQS, Google Pub/Sub, Azure Service Bus
- **Message Headers**: Standard distributed system headers (transaction ID, event type, source service, timestamp)
- **Publisher/Subscriber Pattern**: Reactive message publishing and subscription
- **Event Processing**: Event-driven architecture support with processors and handlers
- **Serialization Support**: JSON (Jackson), Avro, and Protobuf serialization
- **Health Monitoring**: Built-in health checks for messaging components
- **Metrics & Monitoring**: Comprehensive messaging metrics collection
- **Error Handling**: Robust error handling and recovery mechanisms
- **Resilience**: Circuit breakers and retry policies for messaging operations
- **AOP Integration**: Aspect-oriented programming for cross-cutting messaging concerns
- **Graceful Shutdown**: Proper resource cleanup and message processing completion

### 🔍 Advanced Logging
- **Structured Logging**: JSON-based structured log output with Logstash integration
- **MDC Support**: Mapped Diagnostic Context for request tracing (user ID, request ID, correlation ID)
- **Fluent Builder API**: Easy-to-use logging builder pattern
- **Performance Optimized**: JSON validation caching and recursive processing limits
- **Metrics Collection**: Logging performance metrics and cache statistics
- **Actuator Integration**: Spring Boot Actuator endpoints for logging management

### ⚙️ Configuration Management
- **Spring Cloud Config**: Centralized configuration management
- **Service Discovery**: Support for Eureka and Consul service registries
- **Auto-Configuration**: Automatic setup for cloud and registry components
- **Properties Binding**: Type-safe configuration properties

### 📊 Monitoring & Observability
- **Spring Boot Actuator**: Enhanced health endpoints and metrics
- **Micrometer Integration**: Metrics collection with Prometheus support
- **Distributed Tracing**: Zipkin integration for request tracing
- **Custom Health Indicators**: Messaging and component-specific health checks
- **Performance Monitoring**: HTTP client metrics and caching statistics

### ☁️ Multi-Cloud Support
- **AWS Integration**: DynamoDB, CloudWatch, Kinesis, SQS support
- **Google Cloud**: Pub/Sub messaging integration
- **Azure**: Service Bus messaging support
- **Resilience4j**: Circuit breakers, retry, rate limiting, and bulkhead patterns

### 🔧 Developer Experience
- **OpenAPI/Swagger**: API documentation generation
- **Lombok Integration**: Reduced boilerplate code
- **MapStruct Support**: Object mapping utilities
- **Comprehensive Testing**: Extensive test coverage with Mockito and Reactor Test

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

#### Structured Logging
```java
import com.catalis.common.core.logging.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

Logger logger = LoggerFactory.getLogger(MyClass.class);

// Simple structured logging
LoggingUtils.log("User action performed")
    .with("userId", "12345")
    .with("action", "login")
    .with("timestamp", Instant.now())
    .info(logger);

// MDC context logging
LoggingUtils.withMdc("requestId", "req-123", () -> {
    // All logs within this block will include requestId in MDC
    logger.info("Processing request");
    return processRequest();
});
```

#### Messaging
```java
import com.catalis.common.core.messaging.MessageHeaders;

// Create message headers
MessageHeaders headers = MessageHeaders.builder()
    .transactionId("txn-456")
    .eventType("USER_CREATED")
    .sourceService("user-service")
    .timestamp()
    .header("customHeader", "value")
    .build();
```

#### Configuration
```java
@ConfigurationProperties(prefix = "app.messaging")
@Component
public class MessagingConfig {
    // Auto-configured from application properties
}
```

## Configuration

### Application Properties Examples

#### Messaging Configuration
```yaml
catalis:
  messaging:
    enabled: true
    kafka:
      bootstrap-servers: localhost:9092
    redis:
      host: localhost
      port: 6379
```

#### Logging Configuration
```yaml
catalis:
  logging:
    structured: true
    max-recursion-depth: 10
    enable-metrics: true
```

#### Actuator Configuration
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always
```

## Architecture

The library is organized into modular packages:

- **`com.catalis.common.core.config`**: Configuration management and auto-configuration
  - 📖 [Detailed Configuration Documentation](src/main/java/com/catalis/common/core/config/README.md)
- **`com.catalis.common.core.web`**: Web client and resilience components
- **`com.catalis.common.core.actuator`**: Enhanced actuator endpoints and health checks
  - 📖 [Detailed Actuator Documentation](src/main/java/com/catalis/common/core/actuator/README.md)
- **`com.catalis.common.core.messaging`**: Comprehensive messaging framework
  - 📖 [Detailed Messaging Documentation](src/main/java/com/catalis/common/core/messaging/README.md)
- **`com.catalis.common.core.logging`**: Advanced logging utilities
  - 📖 [Detailed Logging Documentation](src/main/java/com/catalis/common/core/logging/README.md)

## Component Documentation

For comprehensive guides and examples for each component:

- **[🔧 Configuration Management](src/main/java/com/catalis/common/core/config/README.md)** - Cloud config, service discovery, and web configuration
- **[📊 Actuator Components](src/main/java/com/catalis/common/core/actuator/README.md)** - Health indicators, metrics, and monitoring
- **[📨 Messaging Framework](src/main/java/com/catalis/common/core/messaging/README.md)** - Multi-platform messaging with Kafka, AWS SQS, Azure Service Bus, and more
- **[🔍 Logging Utilities](src/main/java/com/catalis/common/core/logging/README.md)** - Structured logging, MDC support, and performance optimization

## Requirements

- **Java**: 17+
- **Spring Boot**: 3.2.2
- **Spring Framework**: 6.x
- **Maven**: 3.6+

## Optional Dependencies

The library includes optional support for:
- Kafka messaging
- Redis caching/messaging
- AWS services (DynamoDB, SQS, Kinesis, CloudWatch)
- Google Cloud Pub/Sub
- Azure Service Bus
- Prometheus metrics
- Zipkin tracing

Enable these by including the appropriate dependencies and configuration.

## Contributing

This library is maintained by the **Firefly Team** at [firefly-oss](https://github.com/firefly-oss). Please follow the established coding standards and ensure comprehensive test coverage for any changes.

### Development
- **GitHub Organization**: [firefly-oss](https://github.com/firefly-oss)
- **Team**: Firefly Team
- **Package Structure**: The library uses `com.catalis.*` package naming for historical reasons, but the project is part of the Firefly ecosystem.

## License

Copyright © 2024 Firefly Team. All rights reserved.

## Version

Current version: **1.0.0-SNAPSHOT**

Built with Spring Boot 3.2.2 and modern reactive programming patterns for enterprise microservices architecture.