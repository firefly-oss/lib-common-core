package com.catalis.common.core.messaging.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link MessageSerializer} that uses Jackson for JSON serialization.
 */
@Component
@RequiredArgsConstructor
public class JsonSerializer implements MessageSerializer {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public byte[] serialize(Object object) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to serialize object to JSON", e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws SerializationException {
        try {
            return objectMapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize JSON to object", e);
        }
    }
    
    @Override
    public String getContentType() {
        return "application/json";
    }
    
    @Override
    public SerializationFormat getFormat() {
        return SerializationFormat.JSON;
    }
    
    @Override
    public boolean canHandle(Class<?> type) {
        // Jackson can handle most Java objects
        return true;
    }
}
