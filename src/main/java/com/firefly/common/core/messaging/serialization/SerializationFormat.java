package com.firefly.common.core.messaging.serialization;

/**
 * Enumeration of supported serialization formats.
 */
public enum SerializationFormat {
    /**
     * JSON serialization using Jackson.
     */
    JSON,
    
    /**
     * Apache Avro binary serialization.
     */
    AVRO,
    
    /**
     * Google Protocol Buffers serialization.
     */
    PROTOBUF,
    
    /**
     * String serialization (toString).
     */
    STRING,
    
    /**
     * Java serialization.
     */
    JAVA
}
