package com.catalis.common.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for enhanced structured logging.
 * <p>
 * This class provides helper methods for creating structured logs in JSON format,
 * making it easier to include contextual information and structured data in log messages.
 * <p>
 * The methods in this class help create logs that are more easily parsed and analyzed
 * by log aggregation and analysis tools.
 */
public final class LoggingUtils {

    private static final Logger log = LoggerFactory.getLogger(LoggingUtils.class);

    private LoggingUtils() {
        // Utility class, no instantiation
    }

    /**
     * Creates a structured log entry with additional context.
     *
     * @param message The log message
     * @param data    Additional data to include in the log entry
     * @return A map containing the message and data, suitable for structured logging
     */
    public static Map<String, Object> structuredLog(String message, Map<String, Object> data) {
        Map<String, Object> structuredData = new HashMap<>(data != null ? data : new HashMap<>());
        structuredData.put("message", message);
        return structuredData;
    }

    /**
     * Creates a structured log entry with a single data field.
     *
     * @param message The log message
     * @param key     The key for the data field
     * @param value   The value for the data field
     * @return A map containing the message and data, suitable for structured logging
     */
    public static Map<String, Object> structuredLog(String message, String key, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        return structuredLog(message, data);
    }

    /**
     * Executes an operation with temporary MDC context values that are removed after execution.
     *
     * @param key       The MDC key
     * @param value     The MDC value
     * @param operation The operation to execute
     * @param <T>       The return type of the operation
     * @return The result of the operation
     */
    public static <T> T withMdc(String key, String value, Supplier<T> operation) {
        MDC.put(key, value);
        try {
            return operation.get();
        } finally {
            MDC.remove(key);
        }
    }

    /**
     * Executes an operation with multiple temporary MDC context values that are removed after execution.
     *
     * @param context   Map of MDC keys and values
     * @param operation The operation to execute
     * @param <T>       The return type of the operation
     * @return The result of the operation
     */
    public static <T> T withMdc(Map<String, String> context, Supplier<T> operation) {
        Map<String, String> previousContext = new HashMap<>();
        
        // Store previous values and set new ones
        context.forEach((key, value) -> {
            String previousValue = MDC.get(key);
            if (previousValue != null) {
                previousContext.put(key, previousValue);
            }
            MDC.put(key, value);
        });
        
        try {
            return operation.get();
        } finally {
            // Restore previous context
            context.keySet().forEach(key -> {
                if (previousContext.containsKey(key)) {
                    MDC.put(key, previousContext.get(key));
                } else {
                    MDC.remove(key);
                }
            });
        }
    }

    /**
     * Adds a user ID to the MDC context.
     *
     * @param userId The user ID
     */
    public static void setUserId(@Nullable String userId) {
        if (userId != null) {
            MDC.put("userId", userId);
        }
    }

    /**
     * Adds a request ID to the MDC context.
     *
     * @param requestId The request ID
     */
    public static void setRequestId(@Nullable String requestId) {
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
    }

    /**
     * Adds a correlation ID to the MDC context.
     *
     * @param correlationId The correlation ID
     */
    public static void setCorrelationId(@Nullable String correlationId) {
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
    }

    /**
     * Creates a builder for constructing structured log entries.
     *
     * @param message The log message
     * @return A builder for the structured log entry
     */
    public static StructuredLogBuilder log(String message) {
        return new StructuredLogBuilder(message);
    }

    /**
     * Builder class for creating structured log entries.
     */
    public static class StructuredLogBuilder {
        private final String message;
        private final Map<String, Object> data = new HashMap<>();

        private StructuredLogBuilder(String message) {
            this.message = message;
        }

        /**
         * Adds a field to the structured log entry.
         *
         * @param key   The field key
         * @param value The field value
         * @return This builder for method chaining
         */
        public StructuredLogBuilder with(String key, Object value) {
            data.put(key, value);
            return this;
        }

        /**
         * Adds multiple fields to the structured log entry.
         *
         * @param fields A map of fields to add
         * @return This builder for method chaining
         */
        public StructuredLogBuilder with(Map<String, Object> fields) {
            data.putAll(fields);
            return this;
        }

        /**
         * Builds the structured log entry.
         *
         * @return A map containing the message and data, suitable for structured logging
         */
        public Map<String, Object> build() {
            return structuredLog(message, data);
        }

        /**
         * Logs the structured entry at INFO level.
         *
         * @param logger The logger to use
         */
        public void info(Logger logger) {
            logger.info("{}", build());
        }

        /**
         * Logs the structured entry at DEBUG level.
         *
         * @param logger The logger to use
         */
        public void debug(Logger logger) {
            logger.debug("{}", build());
        }

        /**
         * Logs the structured entry at WARN level.
         *
         * @param logger The logger to use
         */
        public void warn(Logger logger) {
            logger.warn("{}", build());
        }

        /**
         * Logs the structured entry at ERROR level.
         *
         * @param logger The logger to use
         */
        public void error(Logger logger) {
            logger.error("{}", build());
        }

        /**
         * Logs the structured entry at ERROR level with an exception.
         *
         * @param logger    The logger to use
         * @param throwable The exception to log
         */
        public void error(Logger logger, Throwable throwable) {
            logger.error("{}", build(), throwable);
        }
    }
}
