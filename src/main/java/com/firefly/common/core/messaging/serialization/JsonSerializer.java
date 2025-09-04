/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.common.core.messaging.serialization;

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
