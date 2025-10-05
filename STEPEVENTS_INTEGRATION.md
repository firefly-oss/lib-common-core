# Step Events and Saga Integration in lib-common-core

This document describes the integration of `lib-transactional-engine` with `lib-common-core`, enabling saga orchestration and step events publishing through the unified messaging infrastructure.

## Overview

The integration provides:
- **Saga Support**: Full support for `@Saga` and `@SagaStep` annotations
- **Step Events Publishing**: Automatic publishing of step events through lib-common-core messaging infrastructure
- **Multi-Broker Support**: Step events can be published to any supported message broker (Kafka, RabbitMQ, SQS, etc.)
- **CQRS Integration**: Seamless integration with lib-common-cqrs for commands and queries within saga steps

## Architecture

### Bridge Pattern
The integration uses a bridge pattern to adapt between lib-transactional-engine's `StepEventPublisher` interface and lib-common-core's `EventPublisherFactory`:

```
lib-transactional-engine -> StepEventPublisherBridge -> EventPublisherFactory -> EventPublisher (Kafka/RabbitMQ/etc.)
```

### Key Components

1. **StepEventPublisherBridge**: Adapts StepEvents to lib-common-core publishers
2. **StepEventsProperties**: Configuration for step events publishing
3. **StepEventsAutoConfiguration**: Auto-configuration for the integration
4. **TransactionalEngineAutoConfiguration**: Component scanning and engine enablement

## Configuration

### Basic Configuration

```yaml
messaging:
  enabled: true

firefly:
  stepevents:
    enabled: true
    default-topic: step-events
    publisher-type: EVENT_BUS  # For development
    connection-id: default
```

### Production Configuration (Kafka)

```yaml
messaging:
  enabled: true
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    default-topic: banking-events

firefly:
  stepevents:
    enabled: true
    default-topic: step-events
    publisher-type: KAFKA  # Use Kafka for production
    connection-id: default
```

### Production Configuration (RabbitMQ)

```yaml
messaging:
  enabled: true
  rabbitmq:
    enabled: true
    default-exchange: step-events-exchange

firefly:
  stepevents:
    enabled: true
    default-topic: step-events
    publisher-type: RABBITMQ  # Use RabbitMQ for production
    connection-id: default
```

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `firefly.stepevents.enabled` | Enable/disable step events | `true` |
| `firefly.stepevents.default-topic` | Default topic for step events | `step-events` |
| `firefly.stepevents.publisher-type` | Type of publisher to use | `EVENT_BUS` |
| `firefly.stepevents.connection-id` | Connection ID for publisher | `default` |

## Usage

### Creating a Saga

```java
@Component
@Saga(name = "customer-registration")
@EnableTransactionalEngine
public class CustomerRegistrationSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public CustomerRegistrationSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = "validate-customer", retry = 3, backoffMs = 1000)
    public Mono<CustomerValidationResult> validateCustomer(@Input CustomerRegistrationRequest request) {
        ValidateCustomerQuery query = ValidateCustomerQuery.builder()
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .build();
            
        return queryBus.query(query)
            .map(validation -> CustomerValidationResult.builder()
                .customerId(request.getCustomerId())
                .isValid(validation.isValid())
                .build());
    }

    @SagaStep(id = "create-profile", 
              dependsOn = "validate-customer", 
              compensate = "deleteProfile")
    public Mono<CustomerProfileResult> createProfile(
            @FromStep("validate-customer") CustomerValidationResult validation) {

        if (!validation.isValid()) {
            return Mono.error(new CustomerValidationException("Invalid customer"));
        }

        CreateCustomerProfileCommand command = CreateCustomerProfileCommand.builder()
            .customerId(validation.getCustomerId())
            .build();

        return commandBus.send(command);
    }

    // Compensation method
    public Mono<Void> deleteProfile(@FromStep("create-profile") CustomerProfileResult profile) {
        // Compensation logic
        return Mono.empty();
    }
}
```

### Step Events

When saga steps execute, step events are automatically published containing:
- Saga name and ID
- Step ID and execution metadata
- Payload data
- Retry attempts and latency information
- Success/failure status

Example step event:
```json
{
  "sagaName": "customer-registration",
  "sagaId": "SAGA-12345",
  "stepId": "validate-customer",
  "type": "step.completed",
  "payload": {...},
  "attempts": 1,
  "latencyMs": 250,
  "resultType": "SUCCESS"
}
```

## Supported Publishers

The integration supports all lib-common-core publishers:

- **EVENT_BUS**: Spring Application Events (development)
- **KAFKA**: Apache Kafka
- **RABBITMQ**: RabbitMQ
- **SQS**: Amazon SQS
- **GOOGLE_PUBSUB**: Google Cloud Pub/Sub
- **AZURE_SERVICE_BUS**: Azure Service Bus
- **REDIS**: Redis Pub/Sub
- **JMS**: JMS/ActiveMQ
- **KINESIS**: AWS Kinesis

## Integration with CQRS

The integration works seamlessly with lib-common-cqrs:

```java
@SagaStep(id = "process-payment")
public Mono<PaymentResult> processPayment(@Input PaymentRequest request) {
    // Use CommandBus within saga steps
    ProcessPaymentCommand command = ProcessPaymentCommand.builder()
        .amount(request.getAmount())
        .customerId(request.getCustomerId())
        .build();
        
    return commandBus.send(command);
}

@SagaStep(id = "validate-account")
public Mono<AccountInfo> validateAccount(@Input AccountRequest request) {
    // Use QueryBus within saga steps
    GetAccountQuery query = GetAccountQuery.builder()
        .accountId(request.getAccountId())
        .build();
        
    return queryBus.query(query);
}
```

## Error Handling and Compensation

The integration supports full saga compensation patterns:

```java
@SagaStep(id = "reserve-funds", compensate = "releaseFunds")
public Mono<FundReservation> reserveFunds(@Input ReservationRequest request) {
    // Reserve funds logic
    return fundService.reserve(request.getAmount(), request.getAccountId());
}

// Compensation method - automatically called if subsequent steps fail
public Mono<Void> releaseFunds(@FromStep("reserve-funds") FundReservation reservation) {
    return fundService.release(reservation.getReservationId());
}
```

## Testing

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerRegistrationSagaTest {

    @Mock
    private EventPublisherFactory eventPublisherFactory;
    
    @Mock
    private EventPublisher eventPublisher;

    private StepEventPublisherBridge stepEventBridge;

    @BeforeEach
    void setUp() {
        stepEventBridge = new StepEventPublisherBridge(
            "test-topic", 
            PublisherType.EVENT_BUS, 
            "test",
            eventPublisherFactory
        );
        
        when(eventPublisherFactory.getPublisher(PublisherType.EVENT_BUS, "test"))
            .thenReturn(eventPublisher);
        when(eventPublisher.publish(anyString(), anyString(), any(), anyString()))
            .thenReturn(Mono.empty());
    }

    @Test
    void shouldPublishStepEvent() {
        StepEventEnvelope stepEvent = createStepEvent();
        
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();
            
        verify(eventPublisher).publish(eq("test-topic"), eq("test.step"), any(), anyString());
    }
}
```

### Integration Testing

```yaml
# application-test.yml
spring:
  profiles:
    active: test

messaging:
  enabled: true

firefly:
  stepevents:
    enabled: true
    publisher-type: EVENT_BUS
  cqrs:
    enabled: true
```

## Monitoring and Observability

Step events include rich metadata for monitoring:
- Execution attempts and retries
- Step latency and timing information
- Success/failure status
- Correlation IDs for distributed tracing

This metadata can be consumed by monitoring systems to track saga execution patterns and performance.

## Migration from lib-common-domain

If migrating from lib-common-domain:

1. **Update Dependencies**: Replace `lib-common-domain` with `lib-common-core`
2. **Update Configuration**: Change from `firefly.events` to `messaging` configuration
3. **Update Publisher Configuration**: Use `publisher-type` instead of `adapter` configuration
4. **Test**: Verify step events are published correctly with new infrastructure

### Configuration Mapping

| lib-common-domain | lib-common-core |
|------------------|------------------|
| `firefly.events.adapter: kafka` | `firefly.stepevents.publisher-type: KAFKA` |
| `firefly.events.enabled: true` | `messaging.enabled: true` |
| `domain.topic: events` | `firefly.stepevents.default-topic: events` |

## Best Practices

1. **Error Handling**: Always implement compensation methods for steps that modify state
2. **Idempotency**: Ensure saga steps are idempotent for retry safety
3. **Timeouts**: Configure appropriate timeouts for long-running steps
4. **Monitoring**: Use step events metadata for saga execution monitoring
5. **Testing**: Test both success and failure scenarios including compensations

## Troubleshooting

### Step Events Not Published

1. Check `messaging.enabled=true` in configuration
2. Check `firefly.stepevents.enabled=true` in configuration
3. Verify the configured publisher is available and properly configured
4. Check logs for publisher availability and configuration errors

### Saga Not Executing

1. Ensure `@EnableTransactionalEngine` is present on saga class
2. Verify `lib-transactional-engine` dependency is on classpath
3. Check component scanning includes saga packages
4. Verify CQRS components (CommandBus, QueryBus) are properly configured

### Publisher Not Available

1. Check messaging system configuration (Kafka, RabbitMQ, etc.)
2. Verify connection parameters (bootstrap servers, credentials, etc.)
3. Check network connectivity to message brokers
4. Review EventPublisherFactory logs for publisher creation errors

## Examples

Complete examples are available in the test package:
- `StepEventPublisherBridgeTest`: Unit tests for the bridge pattern
- `CustomerRegistrationSagaTest`: Example saga with CQRS integration
- `application-stepevents-sample.yml`: Complete configuration examples