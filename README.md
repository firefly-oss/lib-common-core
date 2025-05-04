# Firefly Common Core Library

A comprehensive foundation library for the Firefly platform that provides essential functionality for building robust, reactive microservices with Spring WebFlux. This library simplifies the development of cloud-native applications by providing ready-to-use components for common microservice patterns and best practices.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Detailed Features](#detailed-features)
  - [Transaction Tracking](#transaction-tracking)
  - [WebClient Utilities](#webclient-utilities)
  - [Messaging System](#messaging-system)
  - [Resilience Patterns](#resilience-patterns)
  - [Actuator and Monitoring](#actuator-and-monitoring)
  - [Centralized Configuration](#centralized-configuration)
  - [Service Registry](#service-registry)
  - [Auto-Configuration](#auto-configuration)
- [Configuration Reference](#configuration-reference)
  - [WebClient Properties](#webclient-properties)
  - [Messaging Properties](#messaging-properties)
  - [Cloud Config Properties](#cloud-config-properties)
  - [Service Registry Properties](#service-registry-properties)
  - [Actuator Properties](#actuator-properties)
- [Usage Examples](#usage-examples)
  - [Transaction Tracking](#transaction-tracking-usage)
  - [WebClient Usage](#webclient-usage)
  - [Messaging System](#messaging-system-usage)
  - [Centralized Configuration](#centralized-configuration-usage)
  - [Service Registry](#service-registry-usage)
  - [Actuator and Monitoring](#actuator-and-monitoring-usage)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## Overview

The Firefly Common Core Library is a foundational component of the Firefly platform, providing common utilities and configurations for building reactive microservices. It is built on top of Spring WebFlux and offers features like transaction tracking, WebClient configuration, messaging integration, centralized configuration, service discovery, and reactive HTTP client utilities.

This library serves as the backbone for all Firefly microservices, ensuring consistent behavior, traceability, and integration capabilities across the entire platform. By centralizing common functionality, it reduces code duplication and ensures best practices are followed throughout the ecosystem. The library's comprehensive set of features enables rapid development of robust, scalable, and maintainable microservices.

### Why Use Firefly Common Core?

- **Accelerate Development**: Eliminate boilerplate code and focus on business logic with pre-configured components
- **Ensure Consistency**: Standardize how services communicate, configure, and operate across your platform
- **Improve Observability**: Built-in transaction tracking, metrics, and health checks make your services easier to monitor
- **Enhance Resilience**: Pre-configured resilience patterns protect your services from cascading failures
- **Simplify Integration**: Ready-to-use components for messaging, service discovery, and configuration management
- **Reduce Learning Curve**: Consistent patterns and abstractions make it easier for new developers to understand the codebase

### Architecture Overview

The library is designed with a modular architecture, allowing you to use only the components you need. Each module is auto-configured but can be customized to meet your specific requirements. The core components work together seamlessly but can also be used independently.

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                     Firefly Common Platform Library                           ║
╠═══════════════════╦═══════════════════╦═══════════════════╦═══════════════════╗
║  Transaction      ║  WebClient        ║  Messaging        ║  Resilience       ║
║  Tracking         ║  Utilities        ║  System           ║  Patterns         ║
╠═══════════════════╬═══════════════════╬═══════════════════╬═══════════════════╣
║  Centralized      ║  Service          ║  Auto-            ║  Reactive         ║
║  Configuration    ║  Registry         ║  Configuration    ║  Utilities        ║
╠═══════════════════╩═══════════════════╩═══════════════════╩═══════════════════╣
║  Actuator         ║  Error            ║  Security         ║  Data             ║
║  Monitoring       ║  Handling         ║  Framework        ║  Pipeline         ║
╚═══════════════════╩═══════════════════╩═══════════════════╩═══════════════════╝
```

## Key Features

- **Transaction Tracking**: Automatically generates and propagates transaction IDs across microservices for distributed tracing
- **WebClient Utilities**: Pre-configured WebClient with transaction ID propagation and simplified API for making HTTP requests
- **Messaging System**: Annotation-based integration with multiple messaging systems (Kafka, RabbitMQ, SQS, etc.)
- **Resilience Patterns**: Built-in circuit breaker, retry, and timeout mechanisms for robust communication
- **Actuator and Monitoring**: Comprehensive health checks, metrics, and distributed tracing for monitoring applications
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

## Quick Start

Here's a minimal example to get started with the Firefly Common Core Library:

1. Add the dependency to your project as shown in the [Installation](#installation) section.

2. Enable the features you need in your `application.yml`:

```yaml
# Enable transaction tracking (enabled by default)
transaction:
  tracking:
    enabled: true

# Enable WebClient with transaction ID propagation (enabled by default)
webclient:
  enabled: true

# Enable messaging system (disabled by default)
messaging:
  enabled: true
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092

# Enable centralized configuration (disabled by default)
cloud:
  config:
    enabled: true
    uri: http://config-server:8888

# Enable service registry (disabled by default)
service:
  registry:
    enabled: true
    type: EUREKA
    eureka:
      service-url: http://eureka-server:8761/eureka/

# Enable actuator endpoints (enabled by default)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

3. Use the provided components in your application:

```java
@RestController
@RequestMapping("/api")
public class ExampleController {
    private final WebClient webClient;

    public ExampleController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping("/example")
    public Mono<String> getExample() {
        return webClient.get()
                .uri("https://api.example.com/resource")
                .retrieve()
                .bodyToMono(String.class);
    }
}
```

## Detailed Features

### Transaction Tracking

The transaction tracking system ensures that every request flowing through your microservices architecture can be traced end-to-end. This is essential for debugging, monitoring, and understanding request flows in a distributed system.

#### How It Works

1. When a request enters your system, the `TransactionFilter` checks if it contains a transaction ID header (`X-Transaction-Id`).
2. If the header exists, the filter uses that ID; otherwise, it generates a new UUID.
3. The transaction ID is stored in the reactive context, making it available throughout the request processing chain.
4. When making outgoing requests, the WebClient automatically adds the transaction ID as a header.
5. Logs throughout your services include the transaction ID, allowing you to trace a request across multiple services.

#### Key Components

- **TransactionFilter**: Intercepts incoming requests and either uses an existing transaction ID or generates a new one
- **WebClient configuration**: Automatically propagates the transaction ID to outgoing requests
- **Reactive context integration**: Makes the transaction ID available throughout the reactive chain
- **MDC integration**: Automatically adds the transaction ID to your logs (when using SLF4J)
- **Messaging integration**: Propagates the transaction ID to messages published to messaging systems

#### Benefits

- **End-to-end tracing**: Follow requests as they travel through multiple services
- **Simplified debugging**: Quickly identify all logs related to a specific request
- **Correlation with monitoring systems**: Link metrics and traces to specific transactions
- **Improved troubleshooting**: Easily reproduce and diagnose issues by tracking the transaction ID

#### Implementation Details

The transaction ID is propagated using the `X-Transaction-Id` HTTP header by default. This header name can be customized through configuration. The transaction ID is a UUID by default, but you can provide a custom generator if needed.

### WebClient Utilities

The WebClient utilities provide a simplified and consistent way to make HTTP requests from your services, with automatic header propagation and error handling.

#### How It Works

1. The library auto-configures a `WebClient` bean with transaction ID propagation and sensible defaults.
2. When you make a request using this WebClient, it automatically:
   - Propagates the transaction ID from the current request context
   - Propagates other relevant headers (configurable)
   - Applies connection and read timeouts
   - Sets up error handling
3. The `WebClientTemplate` provides a higher-level API for common HTTP operations, reducing boilerplate code.

#### Key Components

- **Pre-configured WebClient bean**: Ready-to-use WebClient with transaction ID propagation
- **WebClientTemplate**: Higher-level API for simplified HTTP calls
- **Automatic header propagation**: Transfers headers from incoming to outgoing requests
- **Configurable header filtering**: Control which headers are propagated
- **Resilience integration**: Built-in retry, circuit breaker, and timeout mechanisms
- **Error handling**: Consistent error handling and conversion to domain exceptions

#### Benefits

- **Reduced boilerplate**: Make HTTP requests with minimal code
- **Consistent behavior**: Ensure all services handle HTTP communication in the same way
- **Automatic tracing**: Transaction IDs are propagated without additional code
- **Improved resilience**: Built-in mechanisms to handle transient failures
- **Simplified testing**: Easier to mock and test HTTP interactions

#### Implementation Details

The WebClient is built on Spring WebFlux's WebClient and adds:
- Header propagation filter
- Default timeouts
- Default error handlers
- Integration with the transaction tracking system
- Optional retry mechanism with exponential backoff

### Messaging System

The messaging system provides a declarative way to publish method results to various messaging platforms using a simple annotation, eliminating the need for boilerplate messaging code.

#### How It Works

1. Annotate any method with `@PublishResult` to automatically publish its return value as an event.
2. When the method is called, an aspect intercepts the method execution.
3. After the method completes successfully, the result is published to the specified messaging system.
4. For reactive return types (Mono/Flux), the result is published when the reactive sequence completes.
5. The transaction ID is automatically propagated to the message headers.

#### Supported Messaging Systems

- **Spring Event Bus**: In-memory event publishing within the application
- **Apache Kafka**: Distributed streaming platform for high-throughput, fault-tolerant messaging
- **RabbitMQ**: Message broker implementing Advanced Message Queuing Protocol (AMQP)
- **Amazon SQS**: Fully managed message queuing service from AWS
- **Amazon Kinesis**: Fully managed, scalable streaming data service from AWS
- **Google Cloud Pub/Sub**: Messaging service for event-driven systems and streaming analytics
- **Azure Service Bus**: Fully managed enterprise message broker from Microsoft
- **Redis Pub/Sub**: Simple messaging system using Redis
- **ActiveMQ/JMS**: Java Message Service implementation

#### Key Features

- **Annotation-based publishing**: Use `@PublishResult` to declaratively publish method results
- **Reactive support**: Works with both blocking and reactive (Mono/Flux) return types
- **Custom payload transformations**: Transform the method result before publishing
- **Transaction ID propagation**: Automatically includes the transaction ID in message headers
- **Configurable publishing behavior**: Control when and how messages are published
- **Error handling**: Configure how publishing errors are handled
- **Dead letter queues**: Support for sending failed messages to dead letter queues
- **Header customization**: Add custom headers to messages
- **Multiple serialization formats**: Support for various serialization formats:
  - JSON (using Jackson)
  - Avro (for Avro-generated classes)
  - Protocol Buffers (for Protobuf-generated classes)
  - String (simple toString/fromString conversion)
  - Java Serialization (for Serializable objects)

#### Benefits

- **Reduced boilerplate**: No need to write messaging code in your business logic
- **Separation of concerns**: Keep your business logic focused on its core responsibility
- **Consistent messaging**: Ensure all services publish messages in the same way
- **Simplified testing**: Easier to test business logic without messaging dependencies
- **Flexible integration**: Support for multiple messaging systems with the same code

### Resilience Patterns

The library implements various resilience patterns to make your services more robust and fault-tolerant in the face of failures and high load.

#### How It Works

The resilience patterns are applied at different levels:

1. **HTTP Client Level**: The WebClient is configured with retry, circuit breaker, and timeout mechanisms.
2. **Messaging Level**: Publishing operations have retry and circuit breaker capabilities.
3. **Service Discovery Level**: Service lookups include fallback mechanisms.
4. **Configuration Level**: Configuration fetching includes retry and fallback strategies.

#### Key Patterns

- **Circuit Breaker**: Prevents cascading failures by stopping calls to failing services
  - Automatically opens when error threshold is reached
  - Half-open state to test if the service has recovered
  - Configurable thresholds and recovery parameters

- **Retry**: Automatically retries failed operations with configurable backoff
  - Exponential backoff to prevent overwhelming the target service
  - Configurable max attempts and backoff parameters
  - Selective retry based on exception types

- **Timeout**: Sets maximum duration for operations to prevent blocked threads
  - Connection timeouts for establishing connections
  - Read/write timeouts for data transfer
  - Global and per-request timeout configuration

- **Bulkhead**: Limits the number of concurrent calls to a service
  - Prevents a single service from consuming all resources
  - Configurable concurrency limits
  - Queue-based or rejection-based strategies

- **Fallback**: Provides alternative behavior when an operation fails
  - Default values when a service is unavailable
  - Cached data as a fallback
  - Degraded functionality when dependencies are down

#### Benefits

- **Prevent Cascading Failures**: Isolate failures to prevent them from affecting the entire system
- **Graceful Degradation**: Continue providing service even when some components fail
- **Self-Healing**: Automatically recover from transient failures
- **Resource Protection**: Prevent resource exhaustion during high load or failure scenarios
- **Improved User Experience**: Maintain responsiveness even during partial system failures

#### Implementation Details

The resilience patterns are implemented using Resilience4j, a lightweight fault tolerance library inspired by Netflix Hystrix. The library provides integration with Spring Boot's auto-configuration to make it easy to use these patterns with minimal configuration.

### Actuator and Monitoring

The library provides Spring Boot Actuator integration for monitoring and managing your application. It includes configuration for health checks, metrics, and distributed tracing, making your services observable and easier to operate.

#### How It Works

1. The library auto-configures Spring Boot Actuator with sensible defaults.
2. Health checks are automatically registered for various components (messaging, database, etc.).
3. Metrics are collected and exposed through Micrometer, which can be integrated with various monitoring systems.
4. Distributed tracing is configured to work with the transaction tracking system.
5. Actuator endpoints are exposed for monitoring and management.

#### Key Features

- **Health Checks**: Monitor the health of your application and its dependencies
  - Automatic health checks for messaging systems
  - Database connection health checks
  - Disk space health checks
  - Custom health indicators for application-specific checks

- **Metrics**: Collect and expose metrics about your application
  - JVM metrics (memory, garbage collection, etc.)
  - System metrics (CPU, disk, etc.)
  - HTTP request metrics (count, duration, etc.)
  - Messaging metrics (publish, subscribe, etc.)
  - Custom business metrics

- **Distributed Tracing**: Track requests across multiple services
  - Integration with Zipkin and Jaeger
  - Automatic trace ID generation and propagation
  - Span creation for HTTP requests and messaging operations
  - Custom span annotations for business operations

- **Actuator Endpoints**: Access information about your application through HTTP endpoints
  - Health information
  - Application information
  - Metrics information
  - Environment properties
  - Logger configuration
  - Thread dumps
  - Heap dumps

#### Available Endpoints

- `/actuator/health`: Shows application health information
- `/actuator/info`: Displays application information
- `/actuator/metrics`: Shows metrics information
- `/actuator/prometheus`: Exposes metrics in Prometheus format
- `/actuator/env`: Exposes environment properties
- `/actuator/loggers`: Shows and modifies logger configurations
- `/actuator/httptrace`: Displays HTTP trace information
- `/actuator/mappings`: Displays all request mappings
- `/actuator/threaddump`: Displays a thread dump
- `/actuator/heapdump`: Generates a heap dump

#### Benefits

- **Improved Observability**: Understand how your services are performing
- **Proactive Monitoring**: Detect issues before they affect users
- **Faster Troubleshooting**: Quickly identify the root cause of problems
- **Capacity Planning**: Use metrics to plan for future growth
- **Operational Insights**: Gain insights into how your services are used

#### Integration with Monitoring Systems

The library provides out-of-the-box integration with:

- **Prometheus**: For metrics collection and alerting
- **Grafana**: For metrics visualization
- **Zipkin/Jaeger**: For distributed tracing
- **ELK Stack**: For log aggregation and analysis

### Centralized Configuration

The library provides integration with Spring Cloud Config for centralized configuration management, allowing you to externalize configuration and dynamically update it without restarting your applications.

#### How It Works

1. The library auto-configures a Spring Cloud Config client with sensible defaults.
2. On application startup, the client connects to the config server to retrieve configuration.
3. Configuration is loaded based on the application name, profile, and label (e.g., Git branch).
4. If the config server is unavailable, the application falls back to local configuration.
5. Configuration can be refreshed at runtime without restarting the application.

#### Key Features

- **Automatic Integration**: Works out of the box with Spring Cloud Config Server
  - Auto-configuration with sensible defaults
  - Customizable through properties
  - Support for multiple config server instances

- **Dynamic Configuration Refresh**: Update configuration without application restart
  - Refresh endpoint for triggering configuration updates
  - Support for Spring Cloud Bus for coordinated refreshes
  - `@RefreshScope` for beans that should be recreated on refresh

- **Resilience**: Robust handling of config server unavailability
  - Configurable retry mechanism for config server connection
  - Fallback to local configuration when config server is unavailable
  - Circuit breaker to prevent cascading failures

- **Environment Support**: Configuration for different environments and scenarios
  - Support for different environments (dev, test, prod)
  - Profile-specific configuration
  - Label-specific configuration (e.g., Git branch)

- **Security**: Secure access to configuration
  - Support for encrypted properties
  - Integration with HashiCorp Vault for secrets
  - Authentication with the config server

#### Benefits

- **Centralized Management**: Manage configuration for all services in one place
- **Environment Consistency**: Ensure consistent configuration across environments
- **Dynamic Updates**: Change configuration without service restarts
- **Configuration Versioning**: Track changes to configuration over time
- **Separation of Concerns**: Keep configuration separate from code

#### Configuration Server Options

The library works with different configuration backends through Spring Cloud Config Server:

- **Git**: Store configuration in a Git repository
- **File System**: Store configuration in the local file system
- **Vault**: Store sensitive configuration in HashiCorp Vault
- **JDBC**: Store configuration in a database
- **Redis**: Store configuration in Redis

### Service Registry

The library provides integration with service registry systems like Netflix Eureka and HashiCorp Consul, enabling service discovery in a microservices architecture.

#### How It Works

1. The library auto-configures a service registry client with sensible defaults.
2. On application startup, the service registers itself with the registry server.
3. The service periodically sends heartbeats to the registry to indicate it's still alive.
4. Other services can discover and communicate with your service using its registered name.
5. The library provides helper utilities to simplify service discovery and communication.

#### Key Features

- **Automatic Service Registration**: Register your service with minimal configuration
  - Auto-registration with Eureka or Consul
  - Customizable instance metadata
  - Health check integration
  - Automatic deregistration on shutdown

- **Service Discovery**: Find and communicate with other services
  - Lookup services by name
  - Client-side load balancing
  - Helper utilities for building service URLs
  - Integration with WebClient for seamless HTTP communication

- **Health Check Integration**: Ensure only healthy instances receive traffic
  - Integration with Spring Boot Actuator health checks
  - Customizable health check paths and intervals
  - Automatic service status updates based on health

- **Multiple Registry Support**: Choose the registry that fits your needs
  - Netflix Eureka support
  - HashiCorp Consul support
  - Consistent API regardless of the underlying registry
  - Ability to use multiple registries simultaneously

- **Resilience**: Robust handling of registry unavailability
  - Local cache of service registry
  - Periodic registry fetching
  - Fallback mechanisms for registry unavailability

#### Benefits

- **Dynamic Scaling**: Add or remove service instances without configuration changes
- **Location Transparency**: Communicate with services without knowing their exact location
- **Load Balancing**: Distribute traffic across multiple instances of a service
- **Fault Tolerance**: Automatically route around failed instances
- **Self-Healing**: Automatically detect and remove unhealthy instances

#### Supported Registry Servers

- **Netflix Eureka**: Service registry server from Netflix OSS
  - Simple setup and operation
  - Highly available with peer replication
  - Optimized for AWS deployments

- **HashiCorp Consul**: Service discovery and configuration tool
  - Rich health checking capabilities
  - Key-value store for configuration
  - Service mesh capabilities with Consul Connect

### Auto-Configuration

The library uses Spring Boot's auto-configuration mechanism to automatically configure components based on the classpath and properties. This makes it easy to integrate the library into your application with minimal configuration.

#### How It Works

1. The library provides auto-configuration classes that are activated based on conditions.
2. When your application starts, Spring Boot detects these auto-configuration classes.
3. Each auto-configuration class checks if it should be applied based on:
   - Presence of specific classes on the classpath
   - Absence of specific beans in the application context
   - Configuration properties
4. If the conditions are met, the auto-configuration creates and registers the necessary beans.
5. You can override any auto-configured bean by defining your own bean of the same type.

#### Key Auto-Configurations

- **Transaction Tracking**: Automatically configures the transaction tracking filter
  - Registers the `TransactionFilter` as a WebFilter
  - Configures transaction ID generation
  - Sets up MDC integration for logging

- **WebClient**: Pre-configures WebClient with transaction ID propagation
  - Creates a `WebClient.Builder` bean with default settings
  - Registers header propagation filters
  - Configures timeouts and error handling

- **Messaging System**: Sets up the messaging infrastructure
  - Configures message publishers for different messaging systems
  - Registers the aspect for `@PublishResult` annotation processing
  - Sets up message serialization and deserialization

- **Actuator Endpoints**: Configures Spring Boot Actuator
  - Registers health indicators
  - Configures metrics collection
  - Sets up distributed tracing

- **Centralized Configuration**: Configures Spring Cloud Config client
  - Sets up the config client with default settings
  - Configures retry and fallback mechanisms
  - Enables dynamic configuration refresh

- **Service Registry**: Configures service registry clients
  - Sets up Eureka or Consul client
  - Configures service registration
  - Enables service discovery

#### Benefits

- **Minimal Configuration**: Get started with minimal or no configuration
- **Convention over Configuration**: Sensible defaults that work for most cases
- **Flexibility**: Override any auto-configured component when needed
- **Modularity**: Use only the components you need
- **Consistency**: Ensure consistent configuration across services

#### Customization

You can customize the auto-configuration behavior in several ways:

1. **Properties**: Override default settings using application properties
2. **Custom Beans**: Define your own beans to replace auto-configured ones
3. **Conditional Annotations**: Use `@ConditionalOnProperty` to control when auto-configuration is applied
4. **Exclusions**: Explicitly exclude specific auto-configurations

## Configuration Reference

### WebClient Properties

You can configure the WebClient by adding properties to your `application.yml` or `application.properties` file:

```yaml
webclient:
  enabled: true  # Enable or disable WebClient auto-configuration
  skip-headers:  # Headers that should not be propagated
    - connection
    - keep-alive
    - proxy-authenticate
    - proxy-authorization
    - te
    - trailer
    - transfer-encoding
    - upgrade
  connect-timeout-ms: 5000  # Connection timeout in milliseconds
  read-timeout-ms: 10000    # Read timeout in milliseconds
  write-timeout-ms: 10000   # Write timeout in milliseconds
  max-in-memory-size: 16777216  # Maximum size of in-memory buffer in bytes
  retry:
    enabled: true           # Enable retry for WebClient requests
    max-attempts: 3         # Maximum number of retry attempts
    initial-backoff-ms: 1000  # Initial backoff interval in milliseconds
    max-backoff-ms: 5000    # Maximum backoff interval in milliseconds
    backoff-multiplier: 2.0  # Backoff interval multiplier
```

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

  # Additional messaging systems configuration...
```

### Cloud Config Properties

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

### Service Registry Properties

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
```

### Actuator Properties

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
      exposure:
        include: health,info,metrics,prometheus
      # Base path for actuator endpoints
      base-path: /actuator
      # Whether to include details in responses
      include-details: true

    # JMX exposure configuration
    jmx:
      # Endpoints to expose via JMX. Use '*' for all endpoints or a comma-separated list
      exposure:
        include: "*"

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

## Usage Examples

### Transaction Tracking Usage

The library automatically adds a transaction filter that generates and propagates a unique transaction ID (`X-Transaction-Id`) across services. This ID is added to both incoming and outgoing requests, making it easier to trace requests across multiple services.

#### Basic Usage

No additional configuration is needed as the `TransactionFilter` is automatically registered as a Spring component. The transaction ID is automatically:

- Generated for incoming requests that don't have one
- Propagated to outgoing requests via WebClient
- Added to the reactive context for access in your code
- Included in log messages (when using SLF4J with MDC)
- Added to message headers when publishing to messaging systems

#### Accessing the Transaction ID

You can access the transaction ID from the reactive context in your code:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /**
     * Process an order and access the transaction ID from the reactive context.
     * The transaction ID is automatically added to the context by the TransactionFilter.
     */
    public Mono<OrderResult> processOrder(Order order) {
        // Access transaction ID from reactive context
        return Mono.deferContextual(ctx -> {
            // Get the transaction ID from the context
            String transactionId = ctx.getOrDefault("X-Transaction-Id", "unknown");

            // Log with the transaction ID (it will be automatically included if using MDC)
            log.info("Processing order {} for customer {}", 
                    order.getId(), order.getCustomerId());

            // Perform business logic
            return validateOrder(order)
                .flatMap(this::saveOrder)
                .flatMap(this::notifyShipping)
                .map(o -> new OrderResult(o.getId(), "PROCESSED"));
        });
    }

    // Other methods...
}
```

#### Custom Transaction ID Header

If you need to use a different header name for the transaction ID, you can configure it in your `application.yml`:

```yaml
transaction:
  tracking:
    header-name: X-Custom-Transaction-Id
```

#### Manual Transaction ID Generation

In some cases, you might want to generate a transaction ID manually (e.g., for background jobs):

```java
import com.catalis.common.core.transaction.TransactionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    private final TransactionManager transactionManager;
    private final ReportService reportService;

    public ScheduledTasks(TransactionManager transactionManager, ReportService reportService) {
        this.transactionManager = transactionManager;
        this.reportService = reportService;
    }

    @Scheduled(cron = "0 0 1 * * *") // Run at 1:00 AM every day
    public void generateDailyReport() {
        // Generate a new transaction ID for this background job
        String transactionId = transactionManager.generateTransactionId();

        // Use the transaction manager to execute with this transaction ID
        transactionManager.executeWithTransaction(transactionId, () -> {
            reportService.generateDailyReport()
                .subscribe(
                    report -> log.info("Daily report generated successfully"),
                    error -> log.error("Failed to generate daily report", error)
                );
        });
    }
}
```

### WebClient Usage

The library provides a pre-configured `WebClient` bean that automatically propagates the transaction ID and other headers. This makes it easy to maintain traceability across service boundaries.

#### Basic WebClient Usage

You can inject and use the pre-configured WebClient directly:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final WebClient webClient;

    /**
     * Inject the pre-configured WebClient bean.
     * This WebClient is already configured with:
     * - Transaction ID propagation
     * - Default timeouts
     * - Error handling
     */
    public ProductService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get product details from an external service.
     * The transaction ID is automatically propagated in the request headers.
     */
    public Mono<ProductDetails> getProductDetails(String productId) {
        log.info("Fetching details for product: {}", productId);

        return webClient.get()
                .uri("https://product-service/products/{id}", productId)
                .retrieve()
                // Handle 4xx client errors
                .onStatus(status -> status.is4xxClientError(),
                        response -> Mono.error(new ProductNotFoundException("Product not found: " + productId)))
                // Handle 5xx server errors
                .onStatus(status -> status.is5xxServerError(),
                        response -> Mono.error(new ServiceUnavailableException("Product service unavailable")))
                .bodyToMono(ProductDetails.class)
                .doOnSuccess(product -> log.info("Successfully retrieved details for product: {}", productId))
                .doOnError(error -> log.error("Error retrieving product details: {}", error.getMessage()));
    }

    /**
     * Update a product in the external service.
     */
    public Mono<ProductDetails> updateProduct(String productId, ProductUpdateRequest updateRequest) {
        log.info("Updating product: {}", productId);

        return webClient.put()
                .uri("https://product-service/products/{id}", productId)
                .bodyValue(updateRequest)
                .retrieve()
                .bodyToMono(ProductDetails.class)
                .doOnSuccess(product -> log.info("Successfully updated product: {}", productId));
    }
}
```

#### Using WebClientTemplate

For more convenience, you can use the `WebClientTemplate` which provides a higher-level API for making HTTP requests with simplified error handling and header propagation:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.catalis.common.core.web.WebClientTemplate;
import com.catalis.common.core.web.exception.WebClientResponseException;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final WebClientTemplate webClientTemplate;

    /**
     * Inject the WebClientTemplate for simplified HTTP requests.
     * The WebClientTemplate provides:
     * - Simplified API for common HTTP operations
     * - Automatic error handling and conversion to domain exceptions
     * - Header propagation from the current request
     */
    public CustomerService(WebClientTemplate webClientTemplate) {
        this.webClientTemplate = webClientTemplate;
    }

    /**
     * Get customer details from the customer service.
     * The ServerWebExchange is used to extract headers from the current request.
     */
    public Mono<CustomerDetails> getCustomerDetails(String customerId, ServerWebExchange exchange) {
        log.info("Fetching details for customer: {}", customerId);

        return webClientTemplate.get(
                "https://customer-service",  // Base URL
                "/customers/{id}",           // Path with placeholders
                CustomerDetails.class,       // Response type
                exchange,                    // Current exchange for header propagation
                customerId                   // Path variables
        ).doOnSuccess(customer -> log.info("Successfully retrieved details for customer: {}", customerId))
         .onErrorResume(WebClientResponseException.class, ex -> {
             if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                 log.warn("Customer not found: {}", customerId);
                 return Mono.error(new CustomerNotFoundException("Customer not found: " + customerId));
             }
             log.error("Error retrieving customer details: {}", ex.getMessage());
             return Mono.error(ex);
         });
    }

    /**
     * Create a new customer.
     */
    public Mono<CustomerDetails> createCustomer(CustomerCreationRequest request, ServerWebExchange exchange) {
        log.info("Creating new customer");

        return webClientTemplate.post(
                "https://customer-service",  // Base URL
                "/customers",                // Path
                request,                     // Request body
                CustomerDetails.class,       // Response type
                exchange                     // Current exchange for header propagation
        ).doOnSuccess(customer -> log.info("Successfully created customer with ID: {}", customer.getId()));
    }

    /**
     * Update an existing customer.
     */
    public Mono<CustomerDetails> updateCustomer(String customerId, CustomerUpdateRequest request, ServerWebExchange exchange) {
        log.info("Updating customer: {}", customerId);

        return webClientTemplate.put(
                "https://customer-service",  // Base URL
                "/customers/{id}",           // Path with placeholders
                request,                     // Request body
                CustomerDetails.class,       // Response type
                exchange,                    // Current exchange for header propagation
                customerId                   // Path variables
        ).doOnSuccess(customer -> log.info("Successfully updated customer: {}", customerId));
    }
}
```

#### Advanced WebClient Configuration

If you need to customize the WebClient beyond the default configuration:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CustomWebClientConfig {

    /**
     * Create a custom WebClient for a specific service with different settings.
     * The auto-configured WebClient.Builder ensures that transaction ID propagation 
     * and other common configurations are still applied.
     */
    @Bean
    public WebClient paymentServiceWebClient() {
        // Get the auto-configured builder from the library
        return WebClient.builder()
                .baseUrl("https://payment-service")
                .defaultHeader("API-Key", "your-api-key")
                .defaultHeader("Service-Client", "customer-service")
                .build();
    }
}
```

### Messaging System Usage

The messaging system allows you to automatically publish the results of method executions to various messaging systems using simple annotations, as well as consume events from these systems.

#### Enabling the Messaging System

First, enable the messaging system in your `application.yml`:

```yaml
messaging:
  enabled: true
  # Enable specific messaging systems as needed
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
  rabbitmq:
    enabled: true
    host: localhost
    port: 5672
```

#### Publishing Events with @PublishResult

Use the `@PublishResult` annotation on any method to automatically publish its return value as an event:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.catalis.common.core.messaging.annotation.PublishResult;
import com.catalis.common.core.messaging.annotation.PublisherType;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Create a new order and publish the result to the Spring Event Bus.
     * The method result will be automatically published as an event.
     */
    @PublishResult(
        eventType = "order.created",
        publisher = PublisherType.EVENT_BUS  // Uses Spring's ApplicationEventPublisher
    )
    public Order createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        // Method implementation
        Order order = new Order(
            request.getCustomerId(),
            request.getItems(),
            request.getShippingAddress()
        );

        // Save the order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        return savedOrder;  // This result will be published as an event
    }

    /**
     * Update an order status and publish the result to Kafka.
     * The method returns a Mono, so the result will be published
     * when the Mono completes successfully.
     */
    @PublishResult(
        destination = "order-events",  // Kafka topic name
        eventType = "order.status.updated",  // Event type identifier
        publisher = PublisherType.KAFKA,
        includeHeaders = true  // Include headers like transaction ID in the message
    )
    public Mono<Order> updateOrderStatus(String orderId, OrderStatus newStatus) {
        log.info("Updating order status: {} -> {}", orderId, newStatus);

        return orderRepository.findById(orderId)
            .flatMap(order -> {
                order.setStatus(newStatus);
                order.setUpdatedAt(Instant.now());
                return orderRepository.save(order);
            })
            .doOnSuccess(order -> log.info("Order status updated: {}", order.getId()));
        // The result will be published to Kafka after the Mono completes
    }

    /**
     * Cancel an order and publish the result to RabbitMQ.
     * You can customize the payload using the payloadExpression attribute.
     */
    @PublishResult(
        destination = "order-exchange",  // RabbitMQ exchange name
        routingKey = "order.cancelled",  // RabbitMQ routing key
        eventType = "order.cancelled",
        publisher = PublisherType.RABBITMQ,
        payloadExpression = "{'orderId': #result.id, 'reason': #args[1], 'timestamp': T(java.time.Instant).now().toString()}"
    )
    public Order cancelOrder(String orderId, String reason) {
        log.info("Cancelling order: {} for reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId).block();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setUpdatedAt(Instant.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Order cancelled: {}", savedOrder.getId());

        return savedOrder;
    }
}
```

#### Advanced Publishing Options

The `@PublishResult` annotation supports various options for customizing the publishing behavior:

```java
@PublishResult(
    destination = "inventory-events",  // Destination (topic, queue, exchange)
    routingKey = "inventory.updated",  // Routing key (for RabbitMQ)
    eventType = "inventory.updated",   // Event type identifier
    publisher = PublisherType.KAFKA,   // Messaging system to use
    condition = "#result != null",     // SpEL condition to determine whether to publish
    payloadExpression = "{'itemId': #result.id, 'quantity': #result.quantity, 'timestamp': T(java.time.Instant).now().toString()}", // Custom payload
    includeHeaders = true,             // Include headers like transaction ID
    headerExpressions = {              // Custom headers
        @HeaderExpression(name = "X-Source-Service", expression = "'inventory-service'"),
        @HeaderExpression(name = "X-Operation", expression = "'update'")
    },
    serializationFormat = SerializationFormat.JSON, // Serialization format
    errorHandler = "customErrorHandler"  // Bean name of custom error handler
)
public Mono<InventoryItem> updateInventory(String itemId, int quantity) {
    // Method implementation
}
```

#### Consuming Events with @EventListener

Use the `@EventListener` annotation to consume events from messaging systems:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

import com.catalis.common.core.messaging.annotation.EventListener;
import com.catalis.common.core.messaging.annotation.SubscriberType;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Listen for order created events from the Spring Event Bus.
     * This method will be called whenever an order.created event is published.
     */
    @EventListener(
        eventType = "order.created",
        subscriber = SubscriberType.EVENT_BUS
    )
    public void sendOrderConfirmation(Order order) {
        log.info("Received order created event for order: {}", order.getId());

        // Send confirmation email
        emailService.sendOrderConfirmation(
            order.getCustomerEmail(),
            order.getId(),
            order.getItems()
        );

        log.info("Order confirmation email sent for order: {}", order.getId());
    }

    /**
     * Listen for order status updated events from Kafka.
     * This method will be called whenever an order.status.updated event is published to the order-events topic.
     */
    @EventListener(
        source = "order-events",  // Kafka topic name
        eventType = "order.status.updated",
        subscriber = SubscriberType.KAFKA,
        errorHandler = "orderEventErrorHandler"  // Custom error handler
    )
    public void notifyOrderStatusUpdate(Order order, Map<String, Object> headers) {
        // Access event headers including the transaction ID
        String transactionId = (String) headers.get("X-Transaction-Id");
        log.info("Received order status update event for order: {}, transaction: {}", 
                order.getId(), transactionId);

        // Send status update notification
        emailService.sendOrderStatusUpdate(
            order.getCustomerEmail(),
            order.getId(),
            order.getStatus()
        );

        log.info("Order status update notification sent for order: {}", order.getId());
    }

    /**
     * Listen for order cancelled events from RabbitMQ.
     * This method accepts a custom payload type (not the original Order object).
     */
    @EventListener(
        source = "order-exchange",
        routingKey = "order.cancelled",  // RabbitMQ routing key
        eventType = "order.cancelled",
        subscriber = SubscriberType.RABBITMQ
    )
    public void handleOrderCancellation(Map<String, Object> cancellationInfo) {
        String orderId = (String) cancellationInfo.get("orderId");
        String reason = (String) cancellationInfo.get("reason");

        log.info("Received order cancellation event for order: {}, reason: {}", 
                orderId, reason);

        // Process the cancellation
        // ...

        log.info("Order cancellation processed for order: {}", orderId);
    }
}
```

#### Programmatic Publishing

In addition to the annotation-based approach, you can also publish events programmatically:

```java
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.catalis.common.core.messaging.MessagePublisher;
import com.catalis.common.core.messaging.MessageHeaders;

@Service
public class InventoryService {
    private final MessagePublisher kafkaPublisher;
    private final InventoryRepository inventoryRepository;

    public InventoryService(MessagePublisher kafkaPublisher, InventoryRepository inventoryRepository) {
        this.kafkaPublisher = kafkaPublisher;
        this.inventoryRepository = inventoryRepository;
    }

    public Mono<Void> adjustInventory(String productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
            .flatMap(inventory -> {
                inventory.setQuantity(inventory.getQuantity() + quantity);
                return inventoryRepository.save(inventory);
            })
            .flatMap(inventory -> {
                // Create message headers
                MessageHeaders headers = MessageHeaders.builder()
                    .header("X-Product-Id", productId)
                    .header("X-Operation", "adjust")
                    .build();

                // Publish the event programmatically
                return kafkaPublisher.publish(
                    "inventory-events",  // topic
                    "inventory.adjusted", // event type
                    inventory,           // payload
                    headers              // headers
                );
            });
    }
}
```

### Centralized Configuration Usage

The library provides integration with Spring Cloud Config for centralized configuration management, allowing you to externalize configuration and dynamically update it without restarting your applications.

#### Enabling Centralized Configuration

To enable centralized configuration, add the following to your `application.yml` or `application.properties` file:

```yaml
cloud:
  config:
    enabled: true
    uri: http://config-server:8888
    # Optional settings
    name: my-service  # Defaults to spring.application.name
    profile: dev      # Defaults to active profiles
    label: main       # Git branch or other version label
    fail-fast: true   # Whether to fail startup if config server is unavailable
    retry: true       # Whether to retry failed requests
```

#### Using Centralized Configuration

Once enabled, your application will automatically fetch configuration from the config server on startup. You can use the configuration properties in your code just like any other Spring properties:

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    // This property is fetched from the config server
    @Value("${payment.gateway.url}")
    private String paymentGatewayUrl;

    @Value("${payment.timeout-seconds:30}")
    private int timeoutSeconds;

    // Use the properties in your code
    public void processPayment(PaymentRequest request) {
        // ...
    }
}
```

#### Dynamic Configuration Refresh

The library supports dynamic configuration refresh, allowing you to update configuration properties without restarting your application. To use this feature:

1. Expose the refresh endpoint in your `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: refresh,health,info
```

2. Annotate beans that should be refreshed with `@RefreshScope`:

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@Service
@RefreshScope  // This bean will be recreated when a refresh event occurs
public class FeatureToggleService {
    @Value("${features.payment-methods.crypto-currency.enabled:false}")
    private boolean cryptoCurrencyEnabled;

    @Value("${features.shipping.international.enabled:false}")
    private boolean internationalShippingEnabled;

    /**
     * Check if a feature is enabled.
     * This method will reflect the latest configuration after a refresh.
     */
    public boolean isFeatureEnabled(String featureKey) {
        switch (featureKey) {
            case "crypto-currency":
                return cryptoCurrencyEnabled;
            case "international-shipping":
                return internationalShippingEnabled;
            default:
                return false;
        }
    }
}
```

3. Trigger a refresh by sending a POST request to the refresh endpoint:

```bash
curl -X POST http://your-application:8080/actuator/refresh
```

#### Configuration Profiles

You can use Spring profiles to load different configurations for different environments:

```yaml
# application.yml
spring:
  application:
    name: order-service
  profiles:
    active: dev

# Config server will load:
# - order-service.yml (common properties)
# - order-service-dev.yml (dev-specific properties)
```

#### Handling Configuration Server Unavailability

The library provides several options for handling configuration server unavailability:

```yaml
cloud:
  config:
    # Fail application startup if config server is unavailable
    fail-fast: true

    # Retry configuration
    retry:
      # Whether to retry failed requests
      enabled: true
      # Maximum number of retries
      max-attempts: 6
      # Initial retry interval in milliseconds
      initial-interval: 1000
      # Maximum retry interval in milliseconds
      max-interval: 2000
      # Multiplier for the retry interval
      multiplier: 1.1

    # Fallback to local configuration if config server is unavailable
    # This requires having a local copy of the configuration
    fallback-to-local: true
```

#### Accessing the Environment Directly

For more complex scenarios, you can inject the `Environment` directly:

```java
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {
    private final Environment environment;

    public ConfigurationService(Environment environment) {
        this.environment = environment;
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return environment.getProperty(key, targetType, defaultValue);
    }

    public boolean isProductionMode() {
        return environment.matchesProfiles("prod");
    }
}
```

### Service Registry Usage

The library provides integration with service registry systems like Netflix Eureka and HashiCorp Consul, enabling service discovery in a microservices architecture.

#### Enabling Service Registry

To enable service registry, add the following to your `application.yml` or `application.properties` file:

```yaml
service:
  registry:
    enabled: true
    type: EUREKA  # or CONSUL
```

#### Configuring Eureka

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
      instance-id: ${spring.application.name}:${random.value}  # Unique instance ID
      lease-renewal-interval-in-seconds: 30  # How often to send heartbeats
      registry-fetch-interval-seconds: 30    # How often to fetch registry
```

#### Configuring Consul

If you're using Consul as your service registry:

```yaml
service:
  registry:
    enabled: true
    type: CONSUL
    consul:
      host: consul-server
      port: 8500
      register: true
      deregister: true  # Deregister on shutdown
      service-name: ${spring.application.name}
      instance-id: ${spring.application.name}-${random.uuid}
      tags:
        - microservice
        - spring-boot
      health-check-path: /actuator/health
      health-check-interval: 15s
```

#### Using the ServiceRegistryHelper

The library provides a `ServiceRegistryHelper` class that makes it easy to discover and interact with other services:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

import com.catalis.common.core.config.registry.ServiceRegistryHelper;
import com.catalis.common.core.config.registry.exception.ServiceNotFoundException;

@Service
public class ProductCatalogService {
    private static final Logger log = LoggerFactory.getLogger(ProductCatalogService.class);
    private final ServiceRegistryHelper serviceRegistryHelper;
    private final WebClient webClient;

    /**
     * Inject the ServiceRegistryHelper and WebClient.
     * The ServiceRegistryHelper provides methods for discovering services.
     */
    public ProductCatalogService(ServiceRegistryHelper serviceRegistryHelper, WebClient webClient) {
        this.serviceRegistryHelper = serviceRegistryHelper;
        this.webClient = webClient;
    }

    /**
     * Get product details from the product service.
     * Uses service discovery to find the product service.
     */
    public Mono<ProductDetails> getProductDetails(String productId) {
        log.info("Fetching details for product: {}", productId);

        // Get a URI to the product service
        Optional<URI> serviceUri = serviceRegistryHelper.getServiceUri(
            "product-service",           // Service name in the registry
            "/products/{id}",            // Path with placeholders
            productId                    // Path variables
        );

        // Use the URI to make a request
        return serviceUri
            .map(uri -> {
                log.info("Calling product service at: {}", uri);
                return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(ProductDetails.class)
                    .doOnSuccess(product -> log.info("Successfully retrieved details for product: {}", productId))
                    .doOnError(error -> log.error("Error retrieving product details: {}", error.getMessage()));
            })
            .orElseGet(() -> {
                log.error("Product service not found in registry");
                return Mono.error(new ServiceNotFoundException("product-service"));
            });
    }

    /**
     * Get all instances of a service from the registry.
     * This can be useful for custom load balancing or for calling multiple instances.
     */
    public Mono<List<String>> getAllProductServiceInstances() {
        return Mono.fromCallable(() -> {
            List<ServiceInstance> instances = serviceRegistryHelper.getInstances("product-service");

            if (instances.isEmpty()) {
                log.warn("No instances of product-service found");
                return Collections.emptyList();
            }

            log.info("Found {} instances of product-service", instances.size());
            return instances.stream()
                .map(instance -> instance.getHost() + ":" + instance.getPort())
                .collect(Collectors.toList());
        });
    }

    /**
     * Check if a service is available in the registry.
     */
    public boolean isServiceAvailable(String serviceName) {
        boolean available = serviceRegistryHelper.isServiceAvailable(serviceName);
        log.info("Service {} is {}", serviceName, available ? "available" : "not available");
        return available;
    }
}
```

#### Client-Side Load Balancing

The library integrates with Spring Cloud LoadBalancer to provide client-side load balancing:

```java
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class InventoryService {
    private final WebClient.Builder loadBalancedWebClientBuilder;

    /**
     * Inject the load-balanced WebClient.Builder.
     * This builder is configured to use the service registry for service discovery
     * and client-side load balancing.
     */
    public InventoryService(ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter) {
        this.loadBalancedWebClientBuilder = WebClient.builder()
            .filter(loadBalancerFilter);
    }

    /**
     * Check inventory for a product.
     * Uses the service name directly in the URL, and the load balancer
     * resolves it to an actual instance.
     */
    public Mono<InventoryStatus> checkInventory(String productId) {
        return loadBalancedWebClientBuilder.build()
            .get()
            .uri("http://inventory-service/inventory/{productId}", productId)
            .retrieve()
            .bodyToMono(InventoryStatus.class);
    }
}
```

#### Health Checks and Metadata

You can configure health checks and add metadata to your service registration:

```yaml
service:
  registry:
    enabled: true
    type: EUREKA
    eureka:
      health-check-enabled: true
      health-check-url-path: /actuator/health
      status-page-url-path: /actuator/info
      metadata-map:
        version: ${project.version}
        environment: ${spring.profiles.active}
        zone: us-east-1
```

#### Handling Service Registry Unavailability

The library provides options for handling service registry unavailability:

```java
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

@Service
public class ResilientServiceCaller {
    private final ServiceRegistryHelper serviceRegistryHelper;
    private final WebClient webClient;
    private final Map<String, String> fallbackUrls;

    public ResilientServiceCaller(ServiceRegistryHelper serviceRegistryHelper, WebClient webClient) {
        this.serviceRegistryHelper = serviceRegistryHelper;
        this.webClient = webClient;

        // Fallback URLs to use when service registry is unavailable
        this.fallbackUrls = Map.of(
            "product-service", "http://product-service-fallback:8080",
            "inventory-service", "http://inventory-service-fallback:8080"
        );
    }

    public Mono<ProductDetails> getProductDetails(String productId) {
        // Try to get the service URI from the registry
        Optional<URI> serviceUri = serviceRegistryHelper.getServiceUri(
            "product-service", 
            "/products/{id}", 
            productId
        );

        // If service registry is available, use the discovered URI
        if (serviceUri.isPresent()) {
            return webClient.get()
                .uri(serviceUri.get())
                .retrieve()
                .bodyToMono(ProductDetails.class);
        }

        // Otherwise, use the fallback URL
        return webClient.get()
            .uri(fallbackUrls.get("product-service") + "/products/{id}", productId)
            .retrieve()
            .bodyToMono(ProductDetails.class);
    }
}
```

### Actuator and Monitoring Usage

The library provides Spring Boot Actuator integration for monitoring and managing your application. It includes configuration for health checks, metrics, and distributed tracing.

#### Automatic Configuration

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

#### Customizing Actuator Configuration

If you want to customize the actuator configuration, you can add the following to your `application.yml` file:

```yaml
management:
  # Enable or disable all endpoints
  endpoints:
    enabled-by-default: true

    web:
      # Base path for actuator endpoints
      base-path: /actuator

      # Customize which endpoints to expose
      exposure:
        include: health,info,metrics,prometheus,loggers,env,threaddump
        exclude: shutdown

  # Health check configuration
  health:
    # Show detailed health information
    show-details: always

    # Configure specific health indicators
    diskspace:
      enabled: true
      threshold: 10MB

    # Configure health groups
    group:
      readiness:
        include: db,redis,kafka
      liveness:
        include: ping,diskspace

  # Metrics configuration
  metrics:
    # Enable or disable metrics collection
    enabled: true

    # Add common tags to all metrics
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
      region: us-east-1

    # Configure specific metrics
    distribution:
      percentiles:
        http.server.requests: 0.5, 0.9, 0.95, 0.99
      slo:
        http.server.requests: 50ms, 100ms, 200ms

    # Enable specific metrics
    enable:
      jvm: true
      process: true
      system: true
      http: true

  # Tracing configuration
  tracing:
    enabled: true
    sampling:
      probability: 0.1

    # Zipkin configuration
    zipkin:
      enabled: true
      base-url: http://zipkin:9411
      service-name: ${spring.application.name}

  # Info contributor configuration
  info:
    # Include build information
    build:
      enabled: true
    # Include git information
    git:
      enabled: true
    # Include environment information
    env:
      enabled: true
    # Include Java information
    java:
      enabled: true
```

#### Custom Health Indicators

You can create custom health indicators to monitor the health of your application's dependencies:

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for an external payment gateway.
 * This will be included in the /actuator/health endpoint.
 */
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {
    private final PaymentGatewayClient paymentGatewayClient;

    public PaymentGatewayHealthIndicator(PaymentGatewayClient paymentGatewayClient) {
        this.paymentGatewayClient = paymentGatewayClient;
    }

    @Override
    public Health health() {
        try {
            // Check if the payment gateway is available
            boolean isAvailable = paymentGatewayClient.isAvailable();

            if (isAvailable) {
                return Health.up()
                    .withDetail("gateway", "payment-gateway")
                    .withDetail("status", "available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("gateway", "payment-gateway")
                    .withDetail("status", "unavailable")
                    .withDetail("reason", "API returned unavailable status")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("gateway", "payment-gateway")
                .withDetail("status", "error")
                .withDetail("reason", e.getMessage())
                .build();
        }
    }
}
```

#### Custom Metrics

You can create custom metrics to monitor specific aspects of your application:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service that records custom metrics for order processing.
 */
@Service
public class OrderMetricsService {
    private final Counter orderCreatedCounter;
    private final Counter orderFailedCounter;
    private final Timer orderProcessingTimer;

    public OrderMetricsService(MeterRegistry meterRegistry) {
        // Counter for tracking the number of orders created
        this.orderCreatedCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .tag("type", "ecommerce")
            .register(meterRegistry);

        // Counter for tracking the number of failed orders
        this.orderFailedCounter = Counter.builder("orders.failed")
            .description("Number of failed orders")
            .tag("type", "ecommerce")
            .register(meterRegistry);

        // Timer for measuring order processing time
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Time taken to process orders")
            .tag("type", "ecommerce")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Record a successful order creation.
     */
    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }

    /**
     * Record a failed order.
     */
    public void recordOrderFailed() {
        orderFailedCounter.increment();
    }

    /**
     * Record the time taken to process an order.
     */
    public void recordOrderProcessingTime(long timeInMs) {
        orderProcessingTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Measure the execution time of a runnable.
     */
    public void measureOrderProcessingTime(Runnable action) {
        orderProcessingTimer.record(action);
    }
}
```

#### Using the Metrics in Your Code

```java
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMetricsService metricsService;

    public OrderService(OrderRepository orderRepository, OrderMetricsService metricsService) {
        this.orderRepository = orderRepository;
        this.metricsService = metricsService;
    }

    public Mono<Order> createOrder(OrderRequest request) {
        long startTime = System.currentTimeMillis();

        return orderRepository.save(new Order(request))
            .doOnSuccess(order -> {
                // Record metrics for successful order creation
                metricsService.recordOrderCreated();
                metricsService.recordOrderProcessingTime(System.currentTimeMillis() - startTime);
            })
            .doOnError(error -> {
                // Record metrics for failed order creation
                metricsService.recordOrderFailed();
            });
    }
}
```

#### Integrating with Prometheus and Grafana

1. Configure Prometheus to scrape metrics from your application:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
      - targets: ['your-application:8080']
```

2. Create a Grafana dashboard to visualize the metrics:

```json
{
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": "-- Grafana --",
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "name": "Annotations & Alerts",
        "type": "dashboard"
      }
    ]
  },
  "editable": true,
  "gnetId": null,
  "graphTooltip": 0,
  "id": 1,
  "links": [],
  "panels": [
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Prometheus",
      "fill": 1,
      "fillGradient": 0,
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 0
      },
      "hiddenSeries": false,
      "id": 2,
      "legend": {
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "nullPointMode": "null",
      "options": {
        "dataLinks": []
      },
      "percentage": false,
      "pointradius": 2,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "expr": "rate(orders_created_total[1m])",
          "interval": "",
          "legendFormat": "Orders Created",
          "refId": "A"
        },
        {
          "expr": "rate(orders_failed_total[1m])",
          "interval": "",
          "legendFormat": "Orders Failed",
          "refId": "B"
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Order Rate",
      "tooltip": {
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    }
  ],
  "schemaVersion": 22,
  "style": "dark",
  "tags": [],
  "templating": {
    "list": []
  },
  "time": {
    "from": "now-6h",
    "to": "now"
  },
  "timepicker": {},
  "timezone": "",
  "title": "Order Metrics",
  "uid": "abc123",
  "variables": {
    "list": []
  },
  "version": 1
}
```

#### Distributed Tracing with Zipkin

1. Configure Zipkin in your application:

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1
    zipkin:
      enabled: true
      base-url: http://zipkin:9411
      service-name: ${spring.application.name}
```

2. Add custom spans to your code:

```java
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentService {
    private final PaymentGatewayClient paymentGatewayClient;
    private final ObservationRegistry observationRegistry;

    public PaymentService(PaymentGatewayClient paymentGatewayClient, ObservationRegistry observationRegistry) {
        this.paymentGatewayClient = paymentGatewayClient;
        this.observationRegistry = observationRegistry;
    }

    public Mono<PaymentResult> processPayment(String orderId, PaymentRequest request) {
        // Create a new span for the payment processing
        return Mono.fromCallable(() -> 
            Observation.createNotStarted("payment.process", observationRegistry)
                .lowCardinalityKeyValue("orderId", orderId)
                .lowCardinalityKeyValue("paymentMethod", request.getPaymentMethod())
                .highCardinalityKeyValue("amount", String.valueOf(request.getAmount()))
                .observe(() -> {
                    // This code will be executed within the span
                    try {
                        // Process the payment
                        PaymentResult result = paymentGatewayClient.processPayment(request);

                        // Add the result to the span
                        Observation.Scope scope = Observation.currentObservation().openScope();
                        try {
                            Observation.currentObservation()
                                .lowCardinalityKeyValue("status", result.getStatus())
                                .highCardinalityKeyValue("transactionId", result.getTransactionId());
                        } finally {
                            scope.close();
                        }

                        return result;
                    } catch (Exception e) {
                        // Add the error to the span
                        Observation.Scope scope = Observation.currentObservation().openScope();
                        try {
                            Observation.currentObservation()
                                .lowCardinalityKeyValue("status", "error")
                                .highCardinalityKeyValue("error", e.getMessage());
                        } finally {
                            scope.close();
                        }

                        throw e;
                    }
                })
        );
    }
}
```

## Best Practices

### General Best Practices

1. **Use meaningful service names** that reflect the service's purpose
2. **Configure appropriate health checks** to ensure accurate service status
3. **Set appropriate timeouts and retry intervals** for service discovery and HTTP requests
4. **Use circuit breakers** for resilience when calling external services
5. **Monitor your services** using the provided actuator endpoints
6. **Use distributed tracing** to track requests across services
7. **Centralize configuration** to make it easier to manage and update
8. **Use service discovery** to avoid hardcoding service URLs

### Messaging Best Practices

1. **Use meaningful event types** with a consistent naming convention (e.g., `entity.action`)
2. **Keep events small and focused** by including only necessary data
3. **Version your events** to allow for schema evolution
4. **Implement proper error handling** in event listeners
5. **Set up dead letter queues** for messages that can't be processed
6. **Use the built-in retry mechanism** for transient failures

### Configuration Best Practices

1. **Use a hierarchical configuration structure** with common properties in a base file
2. **Use environment-specific files** for overriding properties
3. **Use application-specific files** for application-specific properties
4. **Use profiles** for different environments (dev, test, prod)
5. **Store sensitive information** in a secure location (e.g., HashiCorp Vault)
6. **Use version control** for configuration files

## Troubleshooting

### Common Issues

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

#### Service Discovery Issues

1. **Service not registering**:
   - Check that the service registry is enabled and configured correctly
   - Verify that the service registry server is running and accessible
   - Check that the service has a valid instance ID

2. **Service not being discovered**:
   - Check that the service is registered correctly
   - Verify that the service name is correct
   - Check that the service is healthy according to the health check

3. **Enable debug logging**:
   ```yaml
   logging:
     level:
       com.catalis.common.core.config.registry: DEBUG
   ```

#### Configuration Issues

1. **Configuration not being loaded**:
   - Check that the config server is running and accessible
   - Verify that the application name and profile are correct
   - Check that the configuration files exist in the config repository

2. **Configuration not being refreshed**:
   - Check that the refresh endpoint is exposed
   - Verify that the beans are annotated with `@RefreshScope`
   - Check that the refresh event is being triggered

3. **Enable debug logging**:
   ```yaml
   logging:
     level:
       com.catalis.common.core.config.cloud: DEBUG
   ```

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

### Development Guidelines

1. Follow the existing code style and conventions
2. Write unit tests for new features and bug fixes
3. Update documentation for new features
4. Keep backward compatibility in mind
5. Consider performance implications of changes

## License

This project is licensed under the Apache 2.0 License—see the LICENSE file for details.

## Support

If you have any questions or need assistance, please open an issue on the GitHub repository or contact the Firefly team.

For commercial support, please contact [support@firefly.io](mailto:support@firefly.io).
