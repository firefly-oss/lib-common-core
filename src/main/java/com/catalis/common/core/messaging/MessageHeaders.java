package com.catalis.common.core.messaging;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing headers for a message.
 * <p>
 * This class is used to store headers for messages that are published to messaging systems.
 * It provides a builder pattern for creating headers and methods for adding and retrieving
 * header values.
 */
@Getter
@Builder
public class MessageHeaders {
    
    private final Map<String, Object> headers;
    
    /**
     * Creates a new MessageHeaders instance with an empty headers map.
     */
    public MessageHeaders() {
        this.headers = new HashMap<>();
    }
    
    /**
     * Creates a new MessageHeaders instance with the specified headers map.
     *
     * @param headers the headers map
     */
    public MessageHeaders(Map<String, Object> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }
    
    /**
     * Adds a header to the headers map.
     *
     * @param name the header name
     * @param value the header value
     * @return this MessageHeaders instance for method chaining
     */
    public MessageHeaders header(String name, Object value) {
        headers.put(name, value);
        return this;
    }
    
    /**
     * Gets a header value from the headers map.
     *
     * @param name the header name
     * @return the header value, or null if not found
     */
    public Object getHeader(String name) {
        return headers.get(name);
    }
    
    /**
     * Gets a header value from the headers map as a string.
     *
     * @param name the header name
     * @return the header value as a string, or null if not found
     */
    public String getHeaderAsString(String name) {
        Object value = headers.get(name);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Gets all headers as a map.
     *
     * @return the headers map
     */
    public Map<String, Object> getAll() {
        return new HashMap<>(headers);
    }
    
    /**
     * Creates a new builder for MessageHeaders.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for MessageHeaders.
     */
    public static class Builder {
        private final Map<String, Object> headers = new HashMap<>();
        
        /**
         * Adds a header to the headers map.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         */
        public Builder header(String name, Object value) {
            headers.put(name, value);
            return this;
        }
        
        /**
         * Adds the transaction ID header.
         *
         * @param transactionId the transaction ID
         * @return this builder
         */
        public Builder transactionId(String transactionId) {
            if (transactionId != null) {
                headers.put("X-Transaction-Id", transactionId);
            }
            return this;
        }
        
        /**
         * Adds the event type header.
         *
         * @param eventType the event type
         * @return this builder
         */
        public Builder eventType(String eventType) {
            if (eventType != null) {
                headers.put("X-Event-Type", eventType);
            }
            return this;
        }
        
        /**
         * Adds the source service header.
         *
         * @param sourceService the source service
         * @return this builder
         */
        public Builder sourceService(String sourceService) {
            if (sourceService != null) {
                headers.put("X-Source-Service", sourceService);
            }
            return this;
        }
        
        /**
         * Adds the timestamp header.
         *
         * @return this builder
         */
        public Builder timestamp() {
            headers.put("X-Timestamp", Instant.now().toString());
            return this;
        }
        
        /**
         * Builds the MessageHeaders instance.
         *
         * @return a new MessageHeaders instance
         */
        public MessageHeaders build() {
            return new MessageHeaders(headers);
        }
    }
}
