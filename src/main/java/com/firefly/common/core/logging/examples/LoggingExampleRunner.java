package com.firefly.common.core.logging.examples;

import com.firefly.common.core.logging.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Example runner to demonstrate the enhanced logging structure.
 * <p>
 * This class runs when the application starts and demonstrates different
 * logging patterns using the LoggingUtils class and structured logging.
 * <p>
 * To enable this example, set the property:
 * {@code logging.example.enabled=true}
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "logging.example", name = "enabled", havingValue = "true")
public class LoggingExampleRunner implements CommandLineRunner {

    private final LoggingExample loggingExample;

    public LoggingExampleRunner(LoggingExample loggingExample) {
        this.loggingExample = loggingExample;
    }

    @Override
    public void run(String... args) {
        log.info("Starting logging example demonstration");

        // Demonstrate basic logging
        log.info("=== Basic Logging Examples ===");
        loggingExample.demonstrateBasicLogging();

        // Demonstrate structured logging
        log.info("=== Structured Logging Examples ===");
        loggingExample.demonstrateStructuredLogging();

        // Demonstrate MDC logging
        log.info("=== MDC Context Logging Examples ===");
        loggingExample.demonstrateMdcLogging();

        // Demonstrate manual structured logging
        log.info("=== Manual Structured Logging Example ===");
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", "ORD-12345");
        orderData.put("customerId", "CUST-6789");
        orderData.put("amount", 99.99);
        orderData.put("items", 3);

        // Log with structured data
        log.info("{}", LoggingUtils.structuredLog("Order processed", orderData));

        // Log with a single field
        log.info("{}", LoggingUtils.structuredLog("User logged in", "userId", "user123"));

        log.info("Logging example demonstration completed");
    }
}
