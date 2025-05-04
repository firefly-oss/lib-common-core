# Messaging Module

This module provides functionality to automatically publish and consume events from various messaging systems using annotations.

## Features

### Publishing Events
- Annotate any method with `@PublishResult` to publish its result to a messaging system

### Consuming Events
- Annotate any method with `@EventListener` to consume events from a messaging system

### Supported Messaging Systems
- Spring Event Bus
- Apache Kafka
- RabbitMQ
- Amazon SQS
- Amazon Kinesis
- Google Cloud Pub/Sub
- Azure Service Bus
- Redis Pub/Sub
- ActiveMQ/JMS

### Serialization Formats
- JSON (using Jackson)
- Avro (for Avro-generated classes)
- Protocol Buffers (for Protobuf-generated classes)
- String (simple toString/fromString conversion)
- Java Serialization (for Serializable objects)

### Additional Features
- Configurable event type and destination/source
- Support for custom payload expressions
- Transaction ID propagation
- Asynchronous and synchronous options
- Support for reactive return types (Mono, Flux) and CompletableFuture
- Support for multiple connections to different servers of the same messaging system type
- Resilience features:
  - Circuit breaker pattern
  - Automatic retries
  - Timeout handling
  - Metrics collection

## Usage

### Publishing Events

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

### Consuming Events

```java
@Service
public class UserEventHandler {

    @EventListener(
        source = "user-events",
        eventType = "user.created",
        subscriber = SubscriberType.KAFKA
    )
    public void handleUserCreated(User user) {
        // Handle the user created event
        System.out.println("User created: " + user.getName());
    }

    @EventListener(
        source = "user-events",
        eventType = "user.updated",
        subscriber = SubscriberType.KAFKA
    )
    public Mono<Void> handleUserUpdated(User user, Map<String, Object> headers) {
        // Handle the user updated event with headers
        String transactionId = (String) headers.get("transactionId");
        System.out.println("User updated: " + user.getName() + ", transactionId: " + transactionId);
        return Mono.empty();
    }
}
```

### Publishing to Different Messaging Systems

#### Kafka

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

#### RabbitMQ

```java
@PublishResult(
    destination = "user-exchange",
    eventType = "user.deleted",
    publisher = PublisherType.RABBITMQ
)
public Mono<Void> deleteUser(String id) {
    // Method implementation
    return Mono.empty();
}
```

#### Amazon SQS

```java
@PublishResult(
    destination = "user-queue",
    eventType = "user.created",
    publisher = PublisherType.SQS
)
public User createUser(UserRequest request) {
    // Method implementation
    return user;
}
```

#### Amazon Kinesis

```java
@PublishResult(
    destination = "user-stream",
    eventType = "user.updated",
    publisher = PublisherType.KINESIS,
    serializationFormat = SerializationFormat.JSON
)
public User updateUser(String id, UserRequest request) {
    // Method implementation
    return user;
}
```

#### Google Cloud Pub/Sub

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.updated",
    publisher = PublisherType.GOOGLE_PUBSUB
)
public Mono<User> updateUser(String id, UserRequest request) {
    // Method implementation
    return Mono.just(user);
}
```

#### Azure Service Bus

```java
@PublishResult(
    destination = "user-topic",
    eventType = "user.deleted",
    publisher = PublisherType.AZURE_SERVICE_BUS
)
public Mono<Void> deleteUser(String id) {
    // Method implementation
    return Mono.empty();
}
```

#### Redis Pub/Sub

```java
@PublishResult(
    destination = "user-channel",
    eventType = "user.created",
    publisher = PublisherType.REDIS
)
public User createUser(UserRequest request) {
    // Method implementation
    return user;
}
```

#### JMS (ActiveMQ)

```java
@PublishResult(
    destination = "user-topic",
    eventType = "user.updated",
    publisher = PublisherType.JMS
)
public Mono<User> updateUser(String id, UserRequest request) {
    // Method implementation
    return Mono.just(user);
}
```

### Using Multiple Connections

You can specify which connection to use by setting the `connectionId` parameter in the annotation:

```java
// Publishing to the production Kafka cluster
@PublishResult(
    destination = "user-events",
    eventType = "user.created",
    publisher = PublisherType.KAFKA,
    connectionId = "prod-cluster"
)
public User createUserInProd(UserRequest request) {
    // Method implementation
    return user;
}

// Publishing to the development Kafka cluster
@PublishResult(
    destination = "user-events",
    eventType = "user.created",
    publisher = PublisherType.KAFKA,
    connectionId = "dev-cluster"
)
public User createUserInDev(UserRequest request) {
    // Method implementation
    return user;
}
```

Similarly, for event listeners:

```java
// Listening to events from the production Kafka cluster
@EventListener(
    source = "user-events",
    eventType = "user.created",
    subscriber = SubscriberType.KAFKA,
    connectionId = "prod-cluster"
)
public void handleUserCreatedInProd(User user) {
    // Handle user created event from production
}

// Listening to events from the development Kafka cluster
@EventListener(
    source = "user-events",
    eventType = "user.created",
    subscriber = SubscriberType.KAFKA,
    connectionId = "dev-cluster"
)
public void handleUserCreatedInDev(User user) {
    // Handle user created event from development
}
```

### Custom Payload Expression

You can use the `payloadExpression` parameter to customize the payload that is published:

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

## Configuration

Add the following properties to your `application.yml` or `application.properties` file to enable and configure the messaging functionality. Below is a comprehensive configuration example with all available options:

### Multiple Connections

The messaging module supports configuring multiple connections for each messaging system type. This allows you to publish to or subscribe from different servers of the same type (e.g., different Kafka clusters, RabbitMQ servers, etc.).

To use multiple connections:

1. Configure each connection in the `*-connections` section of the configuration (e.g., `kafka-connections`, `rabbitmq-connections`, etc.)
2. Give each connection a unique ID (e.g., `prod-cluster`, `dev-server`, etc.)
3. Specify the connection ID in the `connectionId` parameter of the `@PublishResult` or `@EventListener` annotation

If no connection ID is specified in the annotation, the default connection (configured directly under `kafka`, `rabbitmq`, etc.) will be used.

```yaml
messaging:
  # Enable or disable all messaging functionality (default: false)
  enabled: true

  # Enable or disable resilience features (circuit breaker, retry, metrics)
  resilience: true

  # Default timeout for publishing operations in seconds
  publish-timeout-seconds: 5

  # Default connection ID to use if not specified in the annotation
  default-connection-id: default

  # Kafka configuration (default connection)
  kafka:
    # Enable or disable Kafka publishing (default: false)
    enabled: true
    # Default topic to use if not specified in the annotation
    default-topic: events
    # Bootstrap servers for Kafka (comma-separated list)
    bootstrap-servers: localhost:9092
    # Client ID for Kafka producer
    client-id: messaging-publisher
    # Key serializer class for Kafka producer
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    # Value serializer class for Kafka producer
    value-serializer: org.apache.kafka.common.serialization.StringSerializer
    # Security protocol for Kafka (PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL)
    security-protocol: PLAINTEXT
    # SASL mechanism for Kafka (PLAIN, SCRAM-SHA-256, SCRAM-SHA-512, GSSAPI, OAUTHBEARER)
    sasl-mechanism: ""
    # SASL username for Kafka
    sasl-username: ""
    # SASL password for Kafka
    sasl-password: ""
    # Additional properties for Kafka producer
    properties:
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 1
      buffer-memory: 33554432

  # Multiple Kafka connections configuration
  kafka-connections:
    # Production Kafka cluster connection
    prod-cluster:
      enabled: true
      default-topic: prod-events
      bootstrap-servers: prod-kafka1:9092,prod-kafka2:9092,prod-kafka3:9092
      client-id: prod-messaging-publisher
      security-protocol: SASL_SSL
      sasl-mechanism: PLAIN
      sasl-username: "prod-user"
      sasl-password: "prod-password"
      properties:
        acks: all
        retries: 5

    # Development Kafka cluster connection
    dev-cluster:
      enabled: true
      default-topic: dev-events
      bootstrap-servers: dev-kafka:9092
      client-id: dev-messaging-publisher

  # RabbitMQ configuration (default connection)
  rabbitmq:
    # Enable or disable RabbitMQ publishing (default: false)
    enabled: true
    # Default exchange to use if not specified in the annotation
    default-exchange: events
    # Default routing key to use if not specified in the annotation
    default-routing-key: default
    # RabbitMQ host
    host: localhost
    # RabbitMQ port
    port: 5672
    # RabbitMQ virtual host
    virtual-host: /
    # RabbitMQ username
    username: guest
    # RabbitMQ password
    password: guest
    # Whether to use SSL for RabbitMQ connection
    ssl: false
    # Connection timeout in milliseconds
    connection-timeout: 60000
    # Additional properties for RabbitMQ connection
    properties:
      publisher-confirms: true
      publisher-returns: true
      mandatory: true

  # Multiple RabbitMQ connections configuration
  rabbitmq-connections:
    # Production RabbitMQ server connection
    prod-server:
      enabled: true
      default-exchange: prod-events
      default-routing-key: prod-default
      host: prod-rabbitmq.example.com
      port: 5672
      virtual-host: /prod
      username: prod-user
      password: prod-password
      ssl: true

    # Development RabbitMQ server connection
    dev-server:
      enabled: true
      default-exchange: dev-events
      host: dev-rabbitmq.example.com
      username: dev-user
      password: dev-password

  # Amazon SQS configuration
  sqs:
    # Enable or disable SQS publishing (default: false)
    enabled: false
    # Default queue name to use if not specified in the annotation
    default-queue: events
    # AWS region
    region: us-east-1
    # AWS access key ID
    access-key-id: ""
    # AWS secret access key
    secret-access-key: ""
    # AWS session token (for temporary credentials)
    session-token: ""
    # SQS endpoint override (useful for localstack or custom endpoints)
    endpoint: ""
    # Maximum number of messages to retrieve in a single batch
    max-number-of-messages: 10
    # Visibility timeout in seconds
    visibility-timeout: 30
    # Wait time in seconds for long polling
    wait-time-seconds: 20
    # Additional properties for SQS client
    properties: {}

  # Google Cloud Pub/Sub configuration
  google-pub-sub:
    # Enable or disable Google Pub/Sub publishing (default: false)
    enabled: false
    # Default topic to use if not specified in the annotation
    default-topic: events
    # Google Cloud project ID
    project-id: my-project
    # Path to the Google Cloud credentials JSON file
    credentials-path: ""
    # Google Cloud credentials JSON as a string
    credentials-json: ""
    # Endpoint override for Google Pub/Sub (useful for emulator)
    endpoint: ""
    # Whether to use the emulator
    use-emulator: false
    # Emulator host (e.g., localhost:8085)
    emulator-host: ""
    # Retry settings - initial retry delay in milliseconds
    initial-retry-delay-millis: 100
    # Retry settings - retry delay multiplier
    retry-delay-multiplier: 1.3
    # Retry settings - maximum retry delay in milliseconds
    max-retry-delay-millis: 60000
    # Retry settings - maximum number of attempts
    max-attempts: 5
    # Additional properties for Google Pub/Sub client
    properties: {}

  # Azure Service Bus configuration
  azure-service-bus:
    # Enable or disable Azure Service Bus publishing (default: false)
    enabled: false
    # Default topic to use if not specified in the annotation
    default-topic: events
    # Default queue to use if not specified in the annotation
    default-queue: events
    # Connection string for Azure Service Bus
    connection-string: ""
    # Namespace for Azure Service Bus
    namespace: ""
    # Shared access key name for Azure Service Bus
    shared-access-key-name: RootManageSharedAccessKey
    # Shared access key for Azure Service Bus
    shared-access-key: ""
    # Whether to use managed identity for authentication
    use-managed-identity: false
    # Client ID for managed identity
    client-id: ""
    # Retry settings - maximum number of attempts
    max-retries: 3
    # Retry settings - delay in milliseconds
    retry-delay-millis: 100
    # Retry settings - maximum delay in milliseconds
    max-retry-delay-millis: 30000
    # Retry settings - delay multiplier
    retry-delay-multiplier: 1.5
    # Additional properties for Azure Service Bus client
    properties: {}

  # Redis Pub/Sub configuration
  redis:
    # Enable or disable Redis Pub/Sub publishing (default: false)
    enabled: false
    # Default channel to use if not specified in the annotation
    default-channel: events
    # Redis host
    host: localhost
    # Redis port
    port: 6379
    # Redis password
    password: ""
    # Redis database index
    database: 0
    # Connection timeout in milliseconds
    timeout: 2000
    # Whether to use SSL for Redis connection
    ssl: false
    # Redis sentinel master name (if using Redis Sentinel)
    sentinel-master: ""
    # Redis sentinel nodes (comma-separated list, if using Redis Sentinel)
    sentinel-nodes: ""
    # Redis cluster nodes (comma-separated list, if using Redis Cluster)
    cluster-nodes: ""
    # Maximum number of redirects for Redis Cluster
    max-redirects: 3
    # Additional properties for Redis connection
    properties: {}

  # JMS (ActiveMQ) configuration
  jms:
    # Enable or disable JMS publishing (default: false)
    enabled: false
    # Default destination to use if not specified in the annotation
    default-destination: events
    # Whether to use topics (true) or queues (false)
    use-topic: true
    # JMS broker URL (e.g., tcp://localhost:61616 for ActiveMQ)
    broker-url: tcp://localhost:61616
    # JMS username
    username: ""
    # JMS password
    password: ""
    # Client ID for JMS connection
    client-id: messaging-publisher
    # Connection factory class name
    connection-factory-class: org.apache.activemq.ActiveMQConnectionFactory
    # Whether to use transactions
    transacted: false
    # Acknowledgement mode (1=AUTO_ACKNOWLEDGE, 2=CLIENT_ACKNOWLEDGE, 3=DUPS_OK_ACKNOWLEDGE, 4=SESSION_TRANSACTED)
    acknowledge-mode: 1
    # Connection timeout in milliseconds
    connection-timeout: 30000
    # Whether to use SSL for JMS connection
    ssl: false
    # Trust store path for SSL
    trust-store-path: ""
    # Trust store password for SSL
    trust-store-password: ""
    # Key store path for SSL
    key-store-path: ""
    # Key store password for SSL
    key-store-password: ""
    # Additional properties for JMS connection
    properties: {}
```

**Note**: By default, all messaging functionality is disabled. You need to explicitly enable it in your configuration. You only need to configure the messaging systems you plan to use.

### Connection Configuration

Each messaging system requires specific connection details. Here's a summary of the connection properties for each system:

#### Kafka
- `bootstrap-servers`: Comma-separated list of Kafka broker addresses (e.g., `localhost:9092,localhost:9093`)
- `security-protocol`: Security protocol to use (PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL)
- `sasl-mechanism`, `sasl-username`, `sasl-password`: SASL authentication details if using SASL

#### RabbitMQ
- `host`, `port`: RabbitMQ server address
- `virtual-host`: RabbitMQ virtual host
- `username`, `password`: Authentication credentials
- `ssl`: Whether to use SSL for the connection

#### Amazon SQS
- `region`: AWS region where the SQS queue is located
- `access-key-id`, `secret-access-key`: AWS credentials
- `session-token`: Optional session token for temporary credentials
- `endpoint`: Custom endpoint URL (useful for localstack)

#### Amazon Kinesis
- `region`: AWS region where the Kinesis stream is located
- `access-key-id`, `secret-access-key`: AWS credentials
- `session-token`: Optional session token for temporary credentials
- `endpoint`: Custom endpoint URL (useful for localstack)
- `initial-position`: Initial position in the stream when starting a new consumer (LATEST, TRIM_HORIZON, AT_TIMESTAMP)
- `application-name`: Application name for Kinesis Client Library (KCL)
- `enhanced-fan-out`: Whether to use enhanced fan-out for Kinesis consumers

#### Google Cloud Pub/Sub
- `project-id`: Google Cloud project ID
- `credentials-path`: Path to the Google Cloud credentials JSON file
- `credentials-json`: Google Cloud credentials JSON as a string
- `use-emulator`, `emulator-host`: Settings for using the Pub/Sub emulator

#### Azure Service Bus
- `connection-string`: Connection string for Azure Service Bus
- `namespace`: Service Bus namespace
- `shared-access-key-name`, `shared-access-key`: Shared access key details
- `use-managed-identity`, `client-id`: Settings for using managed identity

#### Redis
- `host`, `port`: Redis server address
- `password`: Redis password
- `database`: Redis database index
- `ssl`: Whether to use SSL for the connection
- `sentinel-master`, `sentinel-nodes`: Settings for Redis Sentinel
- `cluster-nodes`: Settings for Redis Cluster

#### JMS (ActiveMQ)
- `broker-url`: JMS broker URL (e.g., `tcp://localhost:61616`)
- `username`, `password`: Authentication credentials
- `connection-factory-class`: Class name of the connection factory
- `ssl`: Whether to use SSL for the connection
- `trust-store-path`, `trust-store-password`, `key-store-path`, `key-store-password`: SSL settings

## Dependencies

To use this module, you need to include the following dependencies based on which messaging systems you want to use:

### Core Dependencies
- Spring AOP: `spring-boot-starter-aop`

### Messaging System Dependencies
- For Kafka: `spring-kafka`
- For RabbitMQ: `spring-boot-starter-amqp`
- For Amazon SQS: `spring-cloud-aws-messaging`
- For Google Cloud Pub/Sub: `spring-cloud-gcp-starter-pubsub`
- For Azure Service Bus: `spring-cloud-azure-starter-servicebus`
- For Redis Pub/Sub: `spring-boot-starter-data-redis`
- For JMS/ActiveMQ: `spring-boot-starter-activemq`

### Resilience Dependencies
- For circuit breaker and retry: `resilience4j-spring-boot3` and `resilience4j-reactor`
- For metrics: `micrometer-registry-prometheus`

All these dependencies are marked as optional in the library, so you need to include the ones you want to use in your project.

## Resilience Features

The messaging module includes several resilience features to make your messaging more robust:

### Circuit Breaker
Prevents cascading failures by stopping calls to a failing service. If a messaging system is experiencing issues, the circuit breaker will open after a certain number of failures, preventing further calls until the system recovers.

### Automatic Retries
Automatically retries failed publishing operations with a configurable backoff strategy.

### Timeout Handling
Sets a maximum time for publishing operations to complete, preventing blocked threads.

### Metrics
Collects metrics about publishing operations, such as success rate, failure rate, and latency, which can be exported to monitoring systems like Prometheus.

## Event Listeners

The messaging module provides the `@EventListener` annotation to mark methods that should be called when specific events are received from a messaging system.

### Basic Event Listener

```java
@Service
public class UserEventHandler {

    @EventListener(
        source = "user-events",
        eventType = "user.created",
        subscriber = SubscriberType.KAFKA
    )
    public void handleUserCreated(User user) {
        // Handle the user created event
        System.out.println("User created: " + user.getName());
    }
}
```

### Event Listener with Headers

You can access the event headers by adding a `Map<String, Object>` parameter to your method:

```java
@EventListener(
    source = "user-events",
    eventType = "user.updated",
    subscriber = SubscriberType.KAFKA
)
public void handleUserUpdated(User user, Map<String, Object> headers) {
    // Handle the user updated event with headers
    String transactionId = (String) headers.get("transactionId");
    System.out.println("User updated: " + user.getName() + ", transactionId: " + transactionId);
}
```

### Reactive Event Listener

You can return a `Mono<Void>` or `Flux<Void>` to handle events reactively:

```java
@EventListener(
    source = "user-events",
    eventType = "user.deleted",
    subscriber = SubscriberType.KAFKA
)
public Mono<Void> handleUserDeleted(User user) {
    // Handle the user deleted event reactively
    return userRepository.deleteById(user.getId())
        .then(Mono.fromRunnable(() ->
            System.out.println("User deleted: " + user.getName())
        ));
}
```

### Event Listener with Custom Serialization

You can specify the serialization format to use for deserializing the event payload:

```java
@EventListener(
    source = "user-events",
    eventType = "user.created",
    subscriber = SubscriberType.KAFKA,
    serializationFormat = SerializationFormat.AVRO
)
public void handleUserCreatedAvro(UserAvro user) {
    // Handle the user created event with Avro serialization
    System.out.println("User created: " + user.getName());
}
```

### Listening to Kinesis Streams

```java
@EventListener(
    source = "user-stream",
    eventType = "user.updated",
    subscriber = SubscriberType.KINESIS,
    serializationFormat = SerializationFormat.JSON,
    concurrency = 2
)
public void handleUserUpdatedFromKinesis(User user) {
    // Handle the user updated event from Kinesis
    System.out.println("User updated from Kinesis: " + user.getName());
}
```

### Event Listener with Concurrency

You can specify the concurrency level for processing events:

```java
@EventListener(
    source = "user-events",
    eventType = "user.created",
    subscriber = SubscriberType.KAFKA,
    concurrency = 5
)
public void handleUserCreatedConcurrent(User user) {
    // This method can be called by up to 5 threads concurrently
    System.out.println("User created: " + user.getName());
}
```

### Event Listener with Manual Acknowledgement

You can disable automatic acknowledgement and manually acknowledge events:

```java
@EventListener(
    source = "user-events",
    eventType = "user.created",
    subscriber = SubscriberType.KAFKA,
    autoAck = false
)
public void handleUserCreatedManualAck(User user, EventHandler.Acknowledgement ack) {
    try {
        // Handle the user created event
        System.out.println("User created: " + user.getName());
        // Acknowledge the event
        ack.acknowledge().subscribe();
    } catch (Exception e) {
        // Don't acknowledge the event if an error occurs
        System.err.println("Error handling user created event: " + e.getMessage());
    }
}
```

## Serialization

The messaging module supports multiple serialization formats for the payload. You can specify the serialization format in the `@PublishResult` annotation:

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.created",
    publisher = PublisherType.KAFKA,
    serializationFormat = SerializationFormat.AVRO
)
public User createUser(UserRequest request) {
    // Method implementation
    return user;
}
```

### JSON Serialization

JSON serialization is the default format and uses Jackson for converting objects to JSON. It can handle most Java objects and is a good choice for interoperability with other systems.

```java
@PublishResult(
    serializationFormat = SerializationFormat.JSON
)
public User getUser(String id) {
    // Method implementation
    return user;
}
```

### Avro Serialization

Avro serialization is a binary format that is more compact and efficient than JSON. It requires Avro-generated classes that extend `SpecificRecordBase`.

```java
@PublishResult(
    serializationFormat = SerializationFormat.AVRO
)
public UserAvro getUserAvro(String id) {
    // Method implementation
    return userAvro;
}
```

### Protocol Buffers Serialization

Protocol Buffers is another binary format that is compact and efficient. It requires Protobuf-generated classes that extend `Message`.

```java
@PublishResult(
    serializationFormat = SerializationFormat.PROTOBUF
)
public UserProto.User getUserProto(String id) {
    // Method implementation
    return userProto;
}
```

### String Serialization

String serialization uses the `toString()` method of the object and is useful for simple objects or when you want to control the string representation yourself.

```java
@PublishResult(
    serializationFormat = SerializationFormat.STRING
)
public String getUserId(String id) {
    // Method implementation
    return userId;
}
```

### Java Serialization

Java serialization uses the standard Java serialization mechanism and requires objects to implement `Serializable`. It is useful for complex objects that are not easily serialized to JSON or other formats.

```java
@PublishResult(
    serializationFormat = SerializationFormat.JAVA
)
public SerializableUser getSerializableUser(String id) {
    // Method implementation
    return serializableUser;
}
```

## Advanced Usage

### Transaction ID Propagation

You can control whether to include the transaction ID in the published events:

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.created",
    publisher = PublisherType.KAFKA,
    includeTransactionId = true
)
public User createUser(UserRequest request) {
    // Method implementation
    return user;
}
```

### Asynchronous vs Synchronous Publishing

By default, events are published asynchronously. You can change this behavior:

```java
@PublishResult(
    destination = "user-events",
    eventType = "user.created",
    publisher = PublisherType.KAFKA,
    async = false // Wait for the publishing to complete
)
public User createUser(UserRequest request) {
    // Method implementation
    return user;
}
```
