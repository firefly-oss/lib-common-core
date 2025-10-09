# Firefly Common Core Library

**Copyright (c) 2025 Firefly Software Solutions Inc**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%2B-orange)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-green)](https://spring.io/projects/spring-boot)

A Spring Boot library providing utilities, configuration, and shared infrastructure components for the **core-infrastructure layer** of the **Firefly OpenCore Banking Platform**.

This library is developed by **Firefly Software Solutions Inc** and released under the Apache 2.0 License.

## Overview

The Firefly Common Core library is part of the Firefly OpenCore Banking Platform architecture, specifically designed for the core-infrastructure layer. It complements the [lib-common-domain](../lib-common-domain/) library which handles domain-layer concerns.


## Features

### Messaging System
- **Unified EventPublisher API** supporting multiple messaging providers
- **Supported providers**: Kafka, RabbitMQ, AWS SQS, Google Pub/Sub, Azure Service Bus, Redis Pub/Sub, JMS/ActiveMQ, AWS Kinesis
- **Connection-aware publishers** for multiple connections per provider
- **Reactive messaging** built on Project Reactor
- **Resilience patterns** with Resilience4j integration

### Configuration Management
- **Spring Cloud Config** integration
- **Service discovery** with Eureka and Consul support
- **Multi-environment** configuration management

### Enhanced WebClient
- **Reactive WebClient** with connection pooling
- **Service registry integration** for load balancing
- **SSL and HTTP/2** support

### Observability & Monitoring
- **Health indicators** for messaging and external systems
- **Micrometer metrics** with Prometheus integration
- **Actuator enhancements** for production monitoring

### Integration Support
- **CQRS integration** with lib-common-cqrs
- **Transactional engine** support with lib-transactional-engine
- **Reactive patterns** throughout the library

## 📦 Installation

### Maven
```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle
```gradle
implementation 'com.firefly:lib-common-core:1.0.0-SNAPSHOT'
```

## 🚀 Quick Start

### 1. Enable Core Features

```yaml
# application.yml
messaging:
  enabled: true
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    default-topic: events
  
management:
  endpoints:
    web:
      exposure:
        include: "*"
  metrics:
    export:
      prometheus:
        enabled: true

spring:
  application:
    name: my-service
```

### 2. Publish Messages

```java
@RestController
public class EventController {
    
    @PostMapping("/events")
    @PublishResult(
        publisherType = KAFKA,
        destination = "user-events", 
        eventType = "user.created"
    )
    public Mono<UserCreatedEvent> createUser(@RequestBody CreateUserRequest request) {
        // Your business logic here
        return Mono.just(new UserCreatedEvent(request.getUserId(), request.getEmail()));
    }
}
```

### 3. Handle Messages

```java
@Component
public class UserEventHandler {
    
    @EventListener
    public Mono<Void> handleUserCreated(@EventPayload UserCreatedEvent event, 
                                       @EventHeaders Map<String, Object> headers) {
        log.info("User created: {} with transaction ID: {}", 
                 event.getUserId(), headers.get("transactionId"));
        
        // Process the event
        return userService.processNewUser(event);
    }
}
```

### 4. Enhanced WebClient Usage

```java
@Service
public class ExternalServiceClient {
    
    private final WebClient webClient;
    
    public ExternalServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    public Mono<ResponseData> callExternalService(RequestData request) {
        return webClient.post()
            .uri("/api/external")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ResponseData.class);
    }
}
```

## 📋 Table of Contents

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Quick Start Guide](docs/QUICKSTART.md) 
- [Configuration Reference](docs/CONFIGURATION.md)
- [API Documentation](docs/API.md)
- [Integrations Guide](docs/INTEGRATIONS.md)
- [Examples and Use Cases](docs/EXAMPLES.md)
- [Migration Guide](docs/MIGRATION.md)
- [Contributing Guidelines](CONTRIBUTING.md)

## 🏗️ Architecture

The library is organized into several key modules:

### Core Modules

| Module | Description | Key Classes |
|--------|-------------|-------------|
| **Messaging** | Multi-provider messaging system | `EventPublisher`, `EventPublisherFactory`, `MessagingProperties` |
| **Configuration** | Cloud config and service discovery | `CloudConfigProperties`, `ServiceRegistryHelper` |
| **Observability** | Metrics, tracing, and health checks | `ActuatorProperties`, `MessagingHealthIndicator` |
| **Web** | Reactive web capabilities | `WebClientProperties`, `WebFluxConfig` |
| **Integration** | CQRS and saga orchestration | `StepEventPublisherBridge`, `TransactionalEngineAutoConfiguration` |

### Messaging Publishers

| Publisher | Class | Configuration Prefix |
|-----------|-------|---------------------|
| Apache Kafka | `KafkaEventPublisher` | `messaging.kafka` |
| RabbitMQ | `RabbitMqEventPublisher` | `messaging.rabbitmq` |
| AWS SQS | `SqsEventPublisher` | `messaging.sqs` |
| Google Pub/Sub | `GooglePubSubEventPublisher` | `messaging.google-pub-sub` |
| Azure Service Bus | `AzureServiceBusEventPublisher` | `messaging.azure-service-bus` |
| Redis Pub/Sub | `RedisEventPublisher` | `messaging.redis` |
| JMS/ActiveMQ | `JmsEventPublisher` | `messaging.jms` |
| AWS Kinesis | `KinesisEventPublisher` | `messaging.kinesis` |
| Spring Events | `SpringEventPublisher` | Always available when messaging enabled |

## 🛠️ Configuration

### Messaging Configuration

```yaml
messaging:
  enabled: true
  resilience: true
  publish-timeout-seconds: 5
  default-connection-id: default
  
  # Kafka Configuration
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    default-topic: events
    client-id: my-service
    security-protocol: PLAINTEXT
    properties:
      "batch.size": "16384"
      "linger.ms": "5"
  
  # Multiple Kafka Connections
  kafka-connections:
    primary:
      enabled: true
      bootstrap-servers: kafka-primary:9092
      default-topic: primary-events
    secondary:
      enabled: true
      bootstrap-servers: kafka-secondary:9092
      default-topic: secondary-events
  
  # RabbitMQ Configuration  
  rabbitmq:
    enabled: true
    host: localhost
    port: 5672
    username: guest
    password: guest
    default-exchange: events
    default-routing-key: default
  
  # AWS SQS Configuration
  sqs:
    enabled: true
    region: us-east-1
    default-queue: events
    # Access keys can be configured via environment or IAM roles
  
  # Serialization Configuration
  serialization:
    default-format: JSON
    formats:
      json:
        enabled: true
        pretty-print: false
      avro:
        enabled: true
        schema-registry-url: http://localhost:8081
      protobuf:
        enabled: true
```

### Step Events Integration

```yaml
# Step Events Configuration (for Transactional Engine integration)
step-events:
  enabled: true
  publisher-type: KAFKA
  connection-id: default
  event-destination: saga-events
  include-step-context: true
  
# Transactional Engine Configuration  
transactional-engine:
  enabled: true
  step-event-publisher: stepEventPublisherBridge
```

### Cloud Configuration

```yaml
# Spring Cloud Config
cloud:
  config:
    enabled: true
    uri: http://config-server:8888
    name: ${spring.application.name}
    profile: ${spring.profiles.active:default}
    label: main
    fail-fast: true
    retry:
      enabled: true
      max-attempts: 3

# Service Registry (Eureka)
service:
  registry:
    enabled: true
    type: EUREKA
    eureka:
      service-url: http://eureka:8761/eureka/
      register: true
      fetch-registry: true
      health-check-enabled: true
      health-check-url-path: /actuator/health
```

### Actuator and Observability

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      show-components: true
    prometheus:
      enabled: true
  
  metrics:
    export:
      prometheus:
        enabled: true
        step: 60s
    tags:
      service: ${spring.application.name}
      environment: ${spring.profiles.active:default}
  
  tracing:
    sampling:
      probability: 0.1
    zipkin:
      endpoint: http://zipkin:9411/api/v2/spans

# Custom Actuator Configuration
actuator:
  extended-metrics:
    jvm:
      enabled: true
      memory: true
      gc: true
      threads: true
    database:
      enabled: true
      connection-timeout: 5
    application:
      enabled: true
      startup-phases: true
      enhanced-info: true
```

### WebClient Configuration

```yaml
webclient:
  enabled: true
  connect-timeout-ms: 5000
  read-timeout-ms: 10000
  write-timeout-ms: 10000
  max-in-memory-size: 16777216
  
  connection-pool:
    enabled: true
    max-connections: 500
    max-idle-time-ms: 30000
  
  http2:
    enabled: true
    max-concurrent-streams: 100
```

## 🔧 Advanced Features

### Multiple Messaging Connections

```java
@RestController
public class MultiConnectionController {
    
    @PostMapping("/primary-events")
    @PublishResult(
        publisherType = KAFKA,
        connectionId = "primary",
        destination = "primary-topic"
    )
    public Mono<Event> publishToPrimary(@RequestBody Event event) {
        return Mono.just(event);
    }
    
    @PostMapping("/secondary-events") 
    @PublishResult(
        publisherType = KAFKA,
        connectionId = "secondary", 
        destination = "secondary-topic"
    )
    public Mono<Event> publishToSecondary(@RequestBody Event event) {
        return Mono.just(event);
    }
}
```

### Custom Serialization

```java
@Component
public class EventSerializationConfig {
    
    @Bean
    public MessageSerializer customAvroSerializer() {
        return new AvroSerializer("http://schema-registry:8081");
    }
    
    @PostMapping("/avro-events")
    @PublishResult(
        publisherType = KAFKA,
        destination = "avro-events",
        serializer = "customAvroSerializer"
    )
    public Mono<AvroEvent> publishAvroEvent(@RequestBody AvroEvent event) {
        return Mono.just(event);
    }
}
```

### Circuit Breaker Integration

```java
@Service
public class ResilientMessagingService {
    
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @CircuitBreaker(name = "messaging", fallbackMethod = "fallbackPublish")
    @PublishResult(publisherType = KAFKA, destination = "resilient-events")
    public Mono<String> publishWithResilience(@RequestBody String message) {
        return Mono.just(message);
    }
    
    public Mono<String> fallbackPublish(String message, Exception ex) {
        log.warn("Publishing failed, using fallback: {}", ex.getMessage());
        return Mono.just("FALLBACK_PROCESSED");
    }
}
```

### Service Discovery Integration

```java
@Service
public class ServiceIntegration {
    
    private final ServiceRegistryHelper serviceRegistry;
    private final WebClient.Builder webClientBuilder;
    
    public Mono<String> callDownstreamService(String serviceId, String path) {
        return Mono.fromCallable(() -> serviceRegistry.getServiceUri(serviceId))
            .flatMap(uri -> webClientBuilder.build()
                .get()
                .uri(uri.resolve(path))
                .retrieve()
                .bodyToMono(String.class));
    }
}
```

## 📊 Monitoring and Observability

### Available Metrics

| Metric Category | Description | Examples |
|-----------------|-------------|----------|
| **Messaging** | Publisher success/failure rates, latency | `messaging.publish.timer`, `messaging.publish.counter` |
| **HTTP Client** | Request metrics, connection pool stats | `http.client.requests`, `reactor.netty.connection.provider` |
| **JVM** | Memory, GC, threads, class loading | `jvm.memory.used`, `jvm.gc.pause` |
| **Application** | Custom business metrics | `application.startup.phase`, `application.feature.usage` |

### Health Checks

- **Messaging**: Connectivity to all enabled messaging systems
- **Service Registry**: Connection to Eureka/Consul
- **Database**: Connection pool health (if applicable)
- **Disk Space**: Available storage monitoring
- **Custom**: Application-specific health indicators

### Distributed Tracing

Automatic trace propagation for:
- HTTP requests (WebClient/WebFlux)
- Messaging operations
- Database operations (when using Spring Data)
- Inter-service calls

## 🧪 Testing

### Test Configuration

```yaml
# application-test.yml
messaging:
  enabled: true
  kafka:
    enabled: true
    bootstrap-servers: ${spring.embedded.kafka.brokers}
  
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
```

### Integration Tests

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-events"})
class MessagingIntegrationTest {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Test
    void shouldPublishAndConsumeMessages() {
        StepVerifier.create(
            eventPublisher.publish("test-events", "test.event", "test-payload", "tx-123")
        ).verifyComplete();
        
        // Verify message was consumed
        // ... assertion logic
    }
}
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

```bash
# Clone repository
git clone https://github.com/firefly-oss/lib-common-core.git
cd lib-common-core

# Build and test
./mvnw clean test

# Generate documentation
./mvnw javadoc:javadoc

# Run integration tests
./mvnw verify -Pintegration-tests
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Projects

- [lib-common-cqrs](../lib-common-cqrs/) - CQRS patterns and event sourcing
- [lib-transactional-engine](../lib-transactional-engine/) - Transaction orchestration
- [lib-common-domain](../lib-common-domain/) - Domain layer utilities

## 📄 License

Copyright 2025 Firefly Software Solutions Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
