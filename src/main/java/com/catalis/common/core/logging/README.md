# Enhanced Structured Logging

This package provides enhanced structured logging capabilities for applications using the lib-common-core library. The logging system is designed to produce consistent, well-structured JSON logs that are easy to parse and analyze in log aggregation systems like ELK (Elasticsearch, Logstash, Kibana) or other log management platforms.

## Features

- **Structured JSON Logging**: All logs are formatted as JSON objects with a consistent structure
- **Contextual Information**: Automatically includes service name, environment, host, and instance information
- **Transaction Tracing**: Integrates with the transaction ID mechanism for request tracing
- **MDC Support**: Includes Mapped Diagnostic Context (MDC) values in logs
- **Location Information**: Includes class, method, line number, and file information
- **Optimized Stack Traces**: Formats stack traces for better readability and reduced size
- **Performance Optimized**: Uses asynchronous logging to minimize impact on application performance
- **Pretty Printing**: Automatically pretty-prints logs in development environments
- **Structured Arguments**: Supports structured arguments in log messages

## Log Structure

The JSON log structure includes the following fields:

```json
{
  "timestamp": "2023-04-15T12:34:56.789Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.catalis.example.Controller",
  "message": "Processing request",
  "service": {
    "name": "example-service",
    "environment": "production",
    "host": "host-123",
    "instance": "pod-abc"
  },
  "context": {
    "X-Transaction-Id": "550e8400-e29b-41d4-a716-446655440000",
    "traceId": "4fd0b6131f19f39af59518d7b713ee15",
    "spanId": "d2f9288a2904cc16",
    "userId": "user123",
    "requestId": "req456"
  },
  "location": {
    "class": "Controller",
    "method": "processRequest",
    "line": "42",
    "file": "Controller.java"
  },
  "exception": {
    "stacktrace": "..."
  },
  "customField1": "value1",
  "customField2": "value2"
}
```

## Usage

### Basic Logging

Use standard SLF4J logging methods:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleService {
    private static final Logger log = LoggerFactory.getLogger(ExampleService.class);
    
    public void doSomething() {
        log.info("Processing request");
        log.debug("Detailed debug information");
        log.error("An error occurred", new RuntimeException("Error details"));
    }
}
```

### Structured Logging

For more structured logs, use the `LoggingUtils` class:

```java
import com.catalis.common.core.logging.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleService {
    private static final Logger log = LoggerFactory.getLogger(ExampleService.class);
    
    public void processOrder(String orderId, String customerId, double amount) {
        LoggingUtils.log("Processing order")
            .with("orderId", orderId)
            .with("customerId", customerId)
            .with("amount", amount)
            .with("timestamp", System.currentTimeMillis())
            .info(log);
    }
    
    public void handleError(String orderId, Exception e) {
        LoggingUtils.log("Error processing order")
            .with("orderId", orderId)
            .with("errorType", e.getClass().getSimpleName())
            .error(log, e);
    }
}
```

### Using MDC for Context

Add context information that will be included in all subsequent logs:

```java
import com.catalis.common.core.logging.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ExampleService {
    private static final Logger log = LoggerFactory.getLogger(ExampleService.class);
    
    public void processUserRequest(String userId, String requestId) {
        // Add context information
        LoggingUtils.setUserId(userId);
        LoggingUtils.setRequestId(requestId);
        
        try {
            log.info("Starting request processing");
            // Process request...
            log.info("Request processing completed");
        } finally {
            // Clean up MDC
            MDC.remove("userId");
            MDC.remove("requestId");
        }
    }
    
    public void withTemporaryContext() {
        // Execute with temporary MDC context
        LoggingUtils.withMdc("tempKey", "tempValue", () -> {
            log.info("This log includes the temporary context");
            return null;
        });
        
        // Multiple MDC values
        Map<String, String> context = new HashMap<>();
        context.put("key1", "value1");
        context.put("key2", "value2");
        
        LoggingUtils.withMdc(context, () -> {
            log.info("This log includes multiple context values");
            return null;
        });
    }
}
```

## Configuration

### Log Levels

Configure log levels in your `application.yml` or `application-logging.yml`:

```yaml
logging:
  level:
    root: INFO
    com.catalis: DEBUG
    org.springframework.web: INFO
    org.hibernate: WARN
```

### Custom Configuration

For advanced customization, you can provide your own `logback-spring.xml` file that extends the default configuration.

## Best Practices

1. **Use Structured Logging**: Prefer structured logging with `LoggingUtils` for complex log entries
2. **Include Context**: Add relevant context information to logs (user ID, request ID, etc.)
3. **Clean Up MDC**: Always clean up MDC values when they are no longer needed
4. **Log Levels**: Use appropriate log levels (DEBUG for detailed information, INFO for normal operations, WARN for potential issues, ERROR for errors)
5. **Sensitive Data**: Never log sensitive information (passwords, credit card numbers, etc.)
6. **Performance**: Be mindful of logging performance, especially in high-throughput applications
