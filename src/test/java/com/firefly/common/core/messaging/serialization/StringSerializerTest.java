package com.firefly.common.core.messaging.serialization;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class StringSerializerTest {

    private final StringSerializer serializer = new StringSerializer();

    @Test
    void shouldSerializeObjectToString() {
        // Given
        String testString = "test string";
        Integer testInt = 123;

        // When
        byte[] stringResult = serializer.serialize(testString);
        byte[] intResult = serializer.serialize(testInt);

        // Then
        assertArrayEquals(testString.getBytes(StandardCharsets.UTF_8), stringResult);
        assertArrayEquals("123".getBytes(StandardCharsets.UTF_8), intResult);
    }

    @Test
    void shouldDeserializeStringToObject() {
        // Given
        byte[] stringBytes = "test string".getBytes(StandardCharsets.UTF_8);
        byte[] intBytes = "123".getBytes(StandardCharsets.UTF_8);

        // When
        String stringResult = serializer.deserialize(stringBytes, String.class);
        Integer intResult = serializer.deserialize(intBytes, Integer.class);

        // Then
        assertEquals("test string", stringResult);
        assertEquals(Integer.valueOf(123), intResult);
    }

    @Test
    void shouldThrowSerializationExceptionWhenDeserializationFails() {
        // Given
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);

        // When/Then
        assertThrows(SerializationException.class, () -> serializer.deserialize(bytes, TestClassWithoutStringConstructor.class));
    }

    @Test
    void shouldReturnCorrectContentType() {
        // When
        String contentType = serializer.getContentType();

        // Then
        assertEquals("text/plain", contentType);
    }

    @Test
    void shouldReturnCorrectFormat() {
        // When
        SerializationFormat format = serializer.getFormat();

        // Then
        assertEquals(SerializationFormat.STRING, format);
    }

    @Test
    void shouldHandleStringAndClassesWithStringConstructor() {
        // When
        boolean canHandleString = serializer.canHandle(String.class);
        boolean canHandleInteger = serializer.canHandle(Integer.class);
        boolean canHandleTestClass = serializer.canHandle(TestClassWithoutStringConstructor.class);

        // Then
        assertTrue(canHandleString);
        assertTrue(canHandleInteger);
        assertFalse(canHandleTestClass);
    }

    static class TestClassWithoutStringConstructor {
        private String value;

        public TestClassWithoutStringConstructor() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
