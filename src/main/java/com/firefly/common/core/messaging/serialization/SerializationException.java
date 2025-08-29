package com.firefly.common.core.messaging.serialization;

/**
 * Exception thrown when serialization or deserialization fails.
 */
public class SerializationException extends RuntimeException {
    
    /**
     * Creates a new SerializationException with the specified message.
     *
     * @param message the detail message
     */
    public SerializationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new SerializationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
