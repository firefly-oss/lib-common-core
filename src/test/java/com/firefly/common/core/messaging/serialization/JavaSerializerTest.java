package com.firefly.common.core.messaging.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JavaSerializerTest {

    private JavaSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JavaSerializer();
    }

    @Test
    void shouldSerializeSerializableObject() {
        // Given
        TestSerializable testObject = new TestSerializable("test", 123);

        // When
        byte[] result = serializer.serialize(testObject);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void shouldThrowExceptionWhenSerializingNonSerializableObject() {
        // Given
        Object nonSerializableObject = new Object();

        // When/Then
        SerializationException exception = assertThrows(SerializationException.class, () -> 
            serializer.serialize(nonSerializableObject)
        );
        assertEquals("Object must implement Serializable", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() {
        // Given
        ProblemSerializable testObject = new ProblemSerializable();

        // When/Then
        SerializationException exception = assertThrows(SerializationException.class, () -> 
            serializer.serialize(testObject)
        );
        assertEquals("Failed to serialize object to bytes", exception.getMessage());
    }

    @Test
    void shouldDeserializeToSerializableObject() {
        // Given
        TestSerializable originalObject = new TestSerializable("test", 123);
        byte[] serialized = serializer.serialize(originalObject);

        // When
        TestSerializable result = serializer.deserialize(serialized, TestSerializable.class);

        // Then
        assertNotNull(result);
        assertEquals(originalObject.getName(), result.getName());
        assertEquals(originalObject.getValue(), result.getValue());
    }

    @Test
    void shouldThrowExceptionWhenDeserializationFails() {
        // Given
        byte[] invalidBytes = new byte[]{1, 2, 3};

        // When/Then
        SerializationException exception = assertThrows(SerializationException.class, () -> 
            serializer.deserialize(invalidBytes, TestSerializable.class)
        );
        assertTrue(exception.getMessage().contains("Failed to deserialize bytes to object"));
    }

    @Test
    void shouldReturnCorrectContentType() {
        // When
        String contentType = serializer.getContentType();

        // Then
        assertEquals("application/java-serialized-object", contentType);
    }

    @Test
    void shouldReturnCorrectFormat() {
        // When
        SerializationFormat format = serializer.getFormat();

        // Then
        assertEquals(SerializationFormat.JAVA, format);
    }

    @Test
    void shouldHandleSerializableObjects() {
        // When
        boolean canHandleSerializable = serializer.canHandle(TestSerializable.class);
        boolean canHandleNonSerializable = serializer.canHandle(Object.class);

        // Then
        assertTrue(canHandleSerializable);
        assertFalse(canHandleNonSerializable);
    }

    // Test class for serialization
    static class TestSerializable implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private int value;

        public TestSerializable() {
        }

        public TestSerializable(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        // Custom serialization for testing
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
        }
    }

    // Problematic serializable class that throws exception during serialization
    static class ProblemSerializable implements Serializable {
        private static final long serialVersionUID = 1L;

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new IOException("Intentional serialization failure");
        }
    }
}
