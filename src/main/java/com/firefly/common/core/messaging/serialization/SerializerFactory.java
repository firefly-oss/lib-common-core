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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating serializers based on the serialization format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SerializerFactory {
    
    private final List<MessageSerializer> serializers;
    private Map<SerializationFormat, MessageSerializer> serializerMap;
    
    /**
     * Gets the appropriate serializer for the specified format.
     *
     * @param format the serialization format
     * @return the serializer, or null if not available
     */
    public MessageSerializer getSerializer(SerializationFormat format) {
        if (serializerMap == null) {
            initSerializerMap();
        }
        
        MessageSerializer serializer = serializerMap.get(format);
        if (serializer == null) {
            log.warn("Serializer for format {} is not available", format);
        }
        
        return serializer;
    }
    
    /**
     * Gets the appropriate serializer for the specified object.
     * <p>
     * This method tries to find a serializer that can handle the object's class.
     * If multiple serializers can handle the class, it returns the first one.
     *
     * @param object the object to serialize
     * @param preferredFormat the preferred serialization format, or null for auto-detection
     * @return the serializer, or null if not available
     */
    public MessageSerializer getSerializer(Object object, SerializationFormat preferredFormat) {
        if (object == null) {
            return getSerializer(SerializationFormat.JSON); // Default to JSON for null objects
        }
        
        if (preferredFormat != null) {
            MessageSerializer serializer = getSerializer(preferredFormat);
            if (serializer != null && serializer.canHandle(object.getClass())) {
                return serializer;
            }
        }
        
        // Try to find a serializer that can handle the object's class
        for (MessageSerializer serializer : serializers) {
            if (serializer.canHandle(object.getClass())) {
                return serializer;
            }
        }
        
        log.warn("No serializer found for class {}", object.getClass().getName());
        return null;
    }
    
    private void initSerializerMap() {
        serializerMap = serializers.stream()
                .collect(Collectors.toMap(
                        MessageSerializer::getFormat,
                        Function.identity(),
                        (s1, s2) -> s1 // In case of duplicates, keep the first one
                ));
    }
}
