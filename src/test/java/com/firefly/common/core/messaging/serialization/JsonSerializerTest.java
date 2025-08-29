package com.firefly.common.core.messaging.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JsonSerializerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JsonSerializer serializer;

    @Test
    void shouldSerializeObjectToJson() throws Exception {
        // Given
        TestObject testObject = new TestObject("test", 123);
        byte[] expectedBytes = "{\"name\":\"test\",\"value\":123}".getBytes();
        
        when(objectMapper.writeValueAsBytes(testObject)).thenReturn(expectedBytes);

        // When
        byte[] result = serializer.serialize(testObject);

        // Then
        assertArrayEquals(expectedBytes, result);
    }

    @Test
    void shouldDeserializeJsonToObject() throws Exception {
        // Given
        byte[] jsonBytes = "{\"name\":\"test\",\"value\":123}".getBytes();
        TestObject expectedObject = new TestObject("test", 123);
        
        when(objectMapper.readValue(jsonBytes, TestObject.class)).thenReturn(expectedObject);

        // When
        TestObject result = serializer.deserialize(jsonBytes, TestObject.class);

        // Then
        assertEquals(expectedObject, result);
    }

    @Test
    void shouldThrowSerializationExceptionWhenSerializationFails() throws Exception {
        // Given
        TestObject testObject = new TestObject("test", 123);
        
        when(objectMapper.writeValueAsBytes(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Test exception") {});

        // When/Then
        assertThrows(SerializationException.class, () -> serializer.serialize(testObject));
    }

    @Test
    void shouldThrowSerializationExceptionWhenDeserializationFails() throws Exception {
        // Given
        byte[] jsonBytes = "{\"name\":\"test\",\"value\":123}".getBytes();
        
        when(objectMapper.readValue(any(byte[].class), eq(TestObject.class))).thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Test exception"));

        // When/Then
        assertThrows(SerializationException.class, () -> serializer.deserialize(jsonBytes, TestObject.class));
    }

    @Test
    void shouldReturnCorrectContentType() {
        // When
        String contentType = serializer.getContentType();

        // Then
        assertEquals("application/json", contentType);
    }

    @Test
    void shouldReturnCorrectFormat() {
        // When
        SerializationFormat format = serializer.getFormat();

        // Then
        assertEquals(SerializationFormat.JSON, format);
    }

    @Test
    void shouldHandleAnyType() {
        // When
        boolean canHandleString = serializer.canHandle(String.class);
        boolean canHandleMap = serializer.canHandle(Map.class);
        boolean canHandleTestObject = serializer.canHandle(TestObject.class);

        // Then
        assertTrue(canHandleString);
        assertTrue(canHandleMap);
        assertTrue(canHandleTestObject);
    }

    static class TestObject {
        private String name;
        private int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return value == that.value && java.util.Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, value);
        }
    }
}
