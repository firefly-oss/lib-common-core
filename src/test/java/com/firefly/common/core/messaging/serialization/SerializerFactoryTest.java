package com.firefly.common.core.messaging.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SerializerFactoryTest {

    @Mock
    private JsonSerializer jsonSerializer;

    @Mock
    private AvroSerializer avroSerializer;

    @Mock
    private ProtobufSerializer protobufSerializer;

    @Mock
    private StringSerializer stringSerializer;

    @Mock
    private JavaSerializer javaSerializer;

    private SerializerFactory factory;

    @BeforeEach
    void setUp() {
        // Configure mocks
        lenient().when(jsonSerializer.getFormat()).thenReturn(SerializationFormat.JSON);
        lenient().when(avroSerializer.getFormat()).thenReturn(SerializationFormat.AVRO);
        lenient().when(protobufSerializer.getFormat()).thenReturn(SerializationFormat.PROTOBUF);
        lenient().when(stringSerializer.getFormat()).thenReturn(SerializationFormat.STRING);
        lenient().when(javaSerializer.getFormat()).thenReturn(SerializationFormat.JAVA);

        List<MessageSerializer> serializers = Arrays.asList(
                jsonSerializer,
                avroSerializer,
                protobufSerializer,
                stringSerializer,
                javaSerializer
        );

        factory = new SerializerFactory(serializers);
    }

    @Test
    void shouldReturnSerializerForFormat() {
        // When
        MessageSerializer jsonResult = factory.getSerializer(SerializationFormat.JSON);
        MessageSerializer avroResult = factory.getSerializer(SerializationFormat.AVRO);
        MessageSerializer protobufResult = factory.getSerializer(SerializationFormat.PROTOBUF);
        MessageSerializer stringResult = factory.getSerializer(SerializationFormat.STRING);
        MessageSerializer javaResult = factory.getSerializer(SerializationFormat.JAVA);

        // Then
        assertEquals(jsonSerializer, jsonResult);
        assertEquals(avroSerializer, avroResult);
        assertEquals(protobufSerializer, protobufResult);
        assertEquals(stringSerializer, stringResult);
        assertEquals(javaSerializer, javaResult);
    }

    @Test
    void shouldReturnNullForUnknownFormat() {
        // When
        MessageSerializer result = factory.getSerializer(null);

        // Then
        assertNull(result);
    }

    @Test
    void shouldReturnSerializerForObject() {
        // Given
        String stringObject = "test";
        TestObject testObject = new TestObject();

        lenient().when(jsonSerializer.canHandle(String.class)).thenReturn(true);
        lenient().when(stringSerializer.canHandle(String.class)).thenReturn(true);
        lenient().when(jsonSerializer.canHandle(TestObject.class)).thenReturn(true);

        // When
        MessageSerializer stringResult = factory.getSerializer(stringObject, null);
        MessageSerializer objectResult = factory.getSerializer(testObject, null);

        // Then
        assertEquals(jsonSerializer, stringResult); // First serializer that can handle the class
        assertEquals(jsonSerializer, objectResult);
    }

    @Test
    void shouldReturnPreferredSerializerForObject() {
        // Given
        String stringObject = "test";

        lenient().when(jsonSerializer.canHandle(String.class)).thenReturn(true);
        lenient().when(stringSerializer.canHandle(String.class)).thenReturn(true);

        // When
        MessageSerializer result = factory.getSerializer(stringObject, SerializationFormat.STRING);

        // Then
        assertEquals(stringSerializer, result);
    }

    @Test
    void shouldReturnNullForObjectWithNoSerializer() {
        // Given
        TestObject testObject = new TestObject();

        lenient().when(jsonSerializer.canHandle(TestObject.class)).thenReturn(false);
        lenient().when(avroSerializer.canHandle(TestObject.class)).thenReturn(false);
        lenient().when(protobufSerializer.canHandle(TestObject.class)).thenReturn(false);
        lenient().when(stringSerializer.canHandle(TestObject.class)).thenReturn(false);
        lenient().when(javaSerializer.canHandle(TestObject.class)).thenReturn(false);

        // When
        MessageSerializer result = factory.getSerializer(testObject, null);

        // Then
        assertNull(result);
    }

    static class TestObject {
    }
}
