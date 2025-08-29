package com.firefly.common.core.messaging.serialization;

/**
 * Interface for serializing and deserializing messages.
 * <p>
 * Implementations of this interface handle the conversion of objects to and from
 * byte arrays or strings for transmission over messaging systems.
 */
public interface MessageSerializer {
    
    /**
     * Serializes an object to a byte array.
     *
     * @param object the object to serialize
     * @return the serialized byte array
     * @throws SerializationException if serialization fails
     */
    byte[] serialize(Object object) throws SerializationException;
    
    /**
     * Deserializes a byte array to an object.
     *
     * @param bytes the byte array to deserialize
     * @param type the class of the object to deserialize to
     * @param <T> the type of the object
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     */
    <T> T deserialize(byte[] bytes, Class<T> type) throws SerializationException;
    
    /**
     * Gets the content type of the serialized data.
     * <p>
     * This is used to set the content type header in messages.
     *
     * @return the content type (e.g., "application/json", "application/avro", etc.)
     */
    String getContentType();
    
    /**
     * Gets the format of this serializer.
     *
     * @return the serialization format
     */
    SerializationFormat getFormat();
    
    /**
     * Checks if this serializer can handle the given object type.
     *
     * @param type the class to check
     * @return true if this serializer can handle the given type
     */
    boolean canHandle(Class<?> type);
}
