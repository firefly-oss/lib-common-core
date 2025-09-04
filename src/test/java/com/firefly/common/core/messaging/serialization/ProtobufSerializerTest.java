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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProtobufSerializerTest {

    private ProtobufSerializer serializer;
    private MockMessage mockMessage;

    @BeforeEach
    void setUp() {
        serializer = new ProtobufSerializer();
        mockMessage = mock(MockMessage.class);
    }

    @Test
    void shouldSerializeProtobufMessage() {
        // Given
        byte[] expectedBytes = new byte[]{1, 2, 3};
        when(mockMessage.toByteArray()).thenReturn(expectedBytes);

        // When
        byte[] result = serializer.serialize(mockMessage);

        // Then
        assertArrayEquals(expectedBytes, result);
        verify(mockMessage).toByteArray();
    }

    @Test
    void shouldThrowExceptionWhenSerializingNonProtobufObject() {
        // Given
        Object nonProtobufObject = "test";

        // When/Then
        SerializationException exception = assertThrows(SerializationException.class, () ->
            serializer.serialize(nonProtobufObject)
        );
        assertEquals("Object must be a Protobuf-generated class", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDeserializingProtobufMessage() {
        // Given
        byte[] bytes = new byte[]{1, 2, 3};

        // When/Then
        // Since we're using a mock interface that doesn't have the parseFrom method,
        // this should throw an exception
        assertThrows(SerializationException.class, () ->
            serializer.deserialize(bytes, MockMessage.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenDeserializingToNonProtobufClass() {
        // Given
        byte[] bytes = new byte[]{1, 2, 3};

        // When/Then
        SerializationException exception = assertThrows(SerializationException.class, () ->
            serializer.deserialize(bytes, String.class)
        );
        assertEquals("Type must be a Protobuf-generated class", exception.getMessage());
    }

    @Test
    void shouldReturnCorrectContentType() {
        // When
        String contentType = serializer.getContentType();

        // Then
        assertEquals("application/x-protobuf", contentType);
    }

    @Test
    void shouldReturnCorrectFormat() {
        // When
        SerializationFormat format = serializer.getFormat();

        // Then
        assertEquals(SerializationFormat.PROTOBUF, format);
    }

    @Test
    void shouldHandleProtobufMessages() {
        // When
        boolean canHandleProtobuf = serializer.canHandle(MockMessage.class);
        boolean canHandleString = serializer.canHandle(String.class);

        // Then
        assertTrue(canHandleProtobuf);
        assertFalse(canHandleString);
    }

    @Test
    void shouldConvertProtobufToJson() throws Exception {
        // Given
        String expectedJson = "{\"field\":\"value\"}";

        try (MockedStatic<JsonFormat> mockedJsonFormat = Mockito.mockStatic(JsonFormat.class)) {
            JsonFormat.Printer mockPrinter = mock(JsonFormat.Printer.class);
            mockedJsonFormat.when(JsonFormat::printer).thenReturn(mockPrinter);
            when(mockPrinter.print(any(Message.class))).thenReturn(expectedJson);

            // When
            String result = serializer.toJson(mockMessage);

            // Then
            assertEquals(expectedJson, result);
        }
    }

    @Test
    void shouldThrowExceptionWhenJsonConversionFails() throws Exception {
        // Given
        try (MockedStatic<JsonFormat> mockedJsonFormat = Mockito.mockStatic(JsonFormat.class)) {
            JsonFormat.Printer mockPrinter = mock(JsonFormat.Printer.class);
            mockedJsonFormat.when(JsonFormat::printer).thenReturn(mockPrinter);
            when(mockPrinter.print(any(Message.class))).thenThrow(new InvalidProtocolBufferException("Test error"));

            // When/Then
            SerializationException exception = assertThrows(SerializationException.class, () ->
                serializer.toJson(mockMessage)
            );
            assertEquals("Failed to convert Protobuf to JSON", exception.getMessage());
        }
    }

    @Test
    void shouldParseJsonToProtobuf() throws Exception {
        // Given
        String json = "{\"field\":\"value\"}";
        Message.Builder mockBuilder = mock(Message.Builder.class);

        try (MockedStatic<JsonFormat> mockedJsonFormat = Mockito.mockStatic(JsonFormat.class)) {
            JsonFormat.Parser mockParser = mock(JsonFormat.Parser.class);
            mockedJsonFormat.when(JsonFormat::parser).thenReturn(mockParser);
            doNothing().when(mockParser).merge(json, mockBuilder);

            // When
            serializer.fromJson(json, mockBuilder);

            // Then
            verify(mockParser).merge(json, mockBuilder);
        }
    }

    @Test
    void shouldThrowExceptionWhenJsonParsingFails() throws Exception {
        // Given
        String json = "{\"field\":\"value\"}";
        Message.Builder mockBuilder = mock(Message.Builder.class);

        try (MockedStatic<JsonFormat> mockedJsonFormat = Mockito.mockStatic(JsonFormat.class)) {
            JsonFormat.Parser mockParser = mock(JsonFormat.Parser.class);
            mockedJsonFormat.when(JsonFormat::parser).thenReturn(mockParser);
            doThrow(new InvalidProtocolBufferException("Test error")).when(mockParser).merge(json, mockBuilder);

            // When/Then
            SerializationException exception = assertThrows(SerializationException.class, () ->
                serializer.fromJson(json, mockBuilder)
            );
            assertEquals("Failed to parse JSON to Protobuf", exception.getMessage());
        }
    }

    // Mock interfaces for testing
    interface MockMessage extends Message {}
}
