# Messaging Framework

The Firefly Common Core messaging framework provides a comprehensive, multi-platform messaging solution for enterprise applications with support for various messaging systems and patterns.

## Supported Platforms

- **Apache Kafka** - High-throughput distributed streaming
- **ActiveMQ** - Traditional JMS messaging
- **AMQP** - Advanced Message Queuing Protocol (RabbitMQ)
- **Redis** - In-memory data structure store messaging
- **AWS SQS** - Amazon Simple Queue Service
- **Google Cloud Pub/Sub** - Google's messaging service
- **Azure Service Bus** - Microsoft's cloud messaging service

## Key Components

### Message Headers
Standard distributed system headers with fluent builder API:

```java
MessageHeaders headers = MessageHeaders.builder()
    .transactionId("txn-123")
    .eventType("ORDER_CREATED")
    .sourceService("order-service")
    .timestamp()
    .header("customField", "value")
    .build();
```

### Publisher/Subscriber Pattern
- **Publishers**: Send messages to various messaging platforms
- **Subscribers**: Receive and process messages reactively
- **Event Processing**: Handle domain events in event-driven architecture

### Serialization Support
- **Jackson JSON**: Default JSON serialization
- **Apache Avro**: Schema-based binary serialization
- **Google Protobuf**: Protocol buffer serialization

### Health Monitoring
Built-in health indicators for all messaging platforms:
- Connection status monitoring
- Queue/topic availability checks
- Consumer lag monitoring
- Error rate tracking

### Metrics & Observability
Comprehensive metrics collection:
- Message throughput rates
- Processing latencies
- Error counts and rates
- Queue depths and consumer lag

### Error Handling & Resilience
- **Circuit Breakers**: Prevent cascade failures
- **Retry Policies**: Configurable retry mechanisms
- **Dead Letter Queues**: Handle unprocessable messages
- **Graceful Degradation**: Fallback mechanisms

### AOP Integration
Aspect-oriented programming support for:
- Message tracing and correlation
- Performance monitoring
- Security and authorization
- Audit logging

### Graceful Shutdown
Proper resource cleanup:
- Message processing completion
- Connection cleanup
- Consumer group leaving
- Resource deallocation

## Configuration Examples

### Kafka Configuration
```yaml
firefly:
  messaging:
    kafka:
      bootstrap-servers: localhost:9092
      producer:
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      consumer:
        group-id: my-service
        key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
        value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

### AWS SQS Configuration
```yaml
firefly:
  messaging:
    aws:
      sqs:
        region: us-east-1
        queue-prefix: my-app
```

### Azure Service Bus Configuration
```yaml
firefly:
  messaging:
    azure:
      servicebus:
        connection-string: ${AZURE_SERVICEBUS_CONNECTION_STRING}
        namespace: my-namespace
```

## Usage Patterns

### Event Publishing
```java
@Component
public class OrderEventPublisher {
    
    @Autowired
    private MessagePublisher publisher;
    
    public void publishOrderCreated(Order order) {
        MessageHeaders headers = MessageHeaders.builder()
            .eventType("ORDER_CREATED")
            .sourceService("order-service")
            .transactionId(order.getTransactionId())
            .timestamp()
            .build();
            
        publisher.publish("order-events", order, headers);
    }
}
```

### Event Subscription
```java
@Component
public class OrderEventSubscriber {
    
    @MessageHandler("order-events")
    public Mono<Void> handleOrderCreated(Order order, MessageHeaders headers) {
        return processOrder(order)
            .doOnSuccess(result -> log.info("Order processed: {}", order.getId()))
            .doOnError(error -> log.error("Failed to process order: {}", order.getId(), error));
    }
}
```

## Best Practices

1. **Use Correlation IDs**: Always include transaction/correlation IDs for tracing
2. **Handle Failures**: Implement proper error handling and retry mechanisms  
3. **Monitor Health**: Use built-in health indicators for monitoring
4. **Schema Evolution**: Plan for message schema changes
5. **Resource Management**: Properly configure connection pools and timeouts
6. **Security**: Implement authentication and authorization where needed
7. **Testing**: Use embedded messaging for integration tests

## Error Handling

The framework provides multiple levels of error handling:

1. **Retry Policies**: Automatic retries with exponential backoff
2. **Circuit Breakers**: Prevent system overload during failures
3. **Dead Letter Queues**: Handle permanently failed messages
4. **Error Metrics**: Monitor and alert on error rates
5. **Graceful Degradation**: Fallback to alternative processing

## Performance Tuning

- **Connection Pooling**: Configure optimal connection pool sizes
- **Batch Processing**: Use batching for high-throughput scenarios
- **Serialization**: Choose appropriate serialization format for your use case
- **Consumer Scaling**: Scale consumers based on message volume
- **Monitoring**: Use metrics to identify bottlenecks