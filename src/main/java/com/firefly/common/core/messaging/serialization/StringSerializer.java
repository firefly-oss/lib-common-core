package com.firefly.common.core.messaging.serialization;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link MessageSerializer} that uses simple string conversion.
 * <p>
 * This serializer converts objects to strings using their toString() method and
 * deserializes by calling the string constructor of the target class.
 */
@Component
public class StringSerializer implements MessageSerializer {
    
    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }
        
        return object.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        String str = new String(bytes, StandardCharsets.UTF_8);
        
        if (type == String.class) {
            return (T) str;
        }
        
        try {
            // Try to use a constructor that takes a String
            return type.getConstructor(String.class).newInstance(str);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize string to object", e);
        }
    }
    
    @Override
    public String getContentType() {
        return "text/plain";
    }
    
    @Override
    public SerializationFormat getFormat() {
        return SerializationFormat.STRING;
    }
    
    @Override
    public boolean canHandle(Class<?> type) {
        if (type == String.class) {
            return true;
        }
        
        try {
            // Check if the class has a constructor that takes a String
            type.getConstructor(String.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
