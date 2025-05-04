# Actuator and Monitoring

This package provides Spring Boot Actuator integration for monitoring and managing your application. It includes configuration for health checks, metrics, and distributed tracing.

**Important**: The actuator endpoints are automatically configured and enabled when you include this library as a dependency. No additional configuration is required to get started with basic monitoring.

## Features

- **Health Checks**: Monitor the health of your application and its dependencies
- **Metrics**: Collect and expose metrics about your application
- **Distributed Tracing**: Track requests across multiple services
- **Actuator Endpoints**: Access information about your application through HTTP endpoints

## Configuration

### Dependencies

To use the actuator features, include the following dependencies in your project:

```xml
<!-- Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

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

### Basic Configuration

The library provides default configuration out of the box. The following endpoints are automatically enabled:

- `/actuator/health`: Health information
- `/actuator/info`: Application information
- `/actuator/metrics`: Metrics information
- `/actuator/prometheus`: Prometheus metrics

If you want to customize the configuration, you can add the following to your `application.yml` file:

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
  tracing:
    enabled: true
    sampling:
      probability: 0.1
```

### Available Endpoints

The following endpoints are available:

- `/actuator/health`: Shows application health information
- `/actuator/info`: Displays application information
- `/actuator/metrics`: Shows metrics information
- `/actuator/prometheus`: Exposes metrics in Prometheus format
- `/actuator/env`: Exposes environment properties
- `/actuator/loggers`: Shows and modifies logger configurations
- `/actuator/httptrace`: Displays HTTP trace information
- `/actuator/mappings`: Displays all request mappings

### Health Checks

The library provides health checks for the following components:

- Messaging systems (Kafka, RabbitMQ, SQS, etc.)
- Disk space
- Database connections (if configured)

### Metrics

The library collects metrics for:

- JVM (memory, garbage collection, etc.)
- System (CPU, disk, etc.)
- HTTP requests (count, duration, etc.)
- Messaging operations (publish, subscribe, etc.)

### Distributed Tracing

The library integrates with the existing transaction ID mechanism to provide distributed tracing across services. It uses Micrometer Tracing and Brave to implement tracing.

## Complete Configuration Example

See the `application-actuator-example.yml` file for a complete configuration example.

## Integration with Monitoring Systems

### Prometheus and Grafana

1. Configure Prometheus to scrape metrics from your application:

```yaml
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['your-application:8080']
```

2. Import the Grafana dashboard for Spring Boot applications.

### Zipkin

1. Configure Zipkin in your application:

```yaml
management:
  tracing:
    zipkin:
      enabled: true
      base-url: http://your-zipkin-server:9411
```

2. Start Zipkin server:

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

## Best Practices

- Secure actuator endpoints in production environments
- Use sampling for tracing in high-traffic applications
- Monitor disk space and memory usage to prevent outages
- Set up alerts for critical health checks
