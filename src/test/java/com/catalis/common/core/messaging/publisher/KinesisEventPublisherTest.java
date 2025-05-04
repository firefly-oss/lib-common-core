package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.catalis.common.core.messaging.serialization.SerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KinesisEventPublisherTest {

    @Mock
    private ObjectProvider<KinesisAsyncClient> kinesisClientProvider;

    @Mock
    private KinesisAsyncClient kinesisClient;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private MessagingProperties.KinesisConfig kinesisConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MessageSerializer serializer;

    @InjectMocks
    private KinesisEventPublisher publisher;

    @BeforeEach
    void setUp() {
        lenient().when(messagingProperties.getKinesisConfig(anyString())).thenReturn(kinesisConfig);
        lenient().when(kinesisConfig.isEnabled()).thenReturn(true);
        lenient().when(kinesisClientProvider.getIfAvailable()).thenReturn(kinesisClient);
    }

    @Test
    void shouldPublishEvent() {
        // Given
        String destination = "test-stream";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(kinesisClient.putRecord(any(PutRecordRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(PutRecordResponse.builder().sequenceNumber("123").build()));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .verifyComplete();

        // Then
        verify(kinesisClient).putRecord(any(PutRecordRequest.class));
    }

    @Test
    void shouldNotPublishWhenKinesisClientIsNotAvailable() {
        // Given
        String destination = "test-stream";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(kinesisClientProvider.getIfAvailable()).thenReturn(null);

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectError(IllegalStateException.class)
                .verify();

        verify(kinesisClient, never()).putRecord(any(PutRecordRequest.class));
    }

    @Test
    void shouldNotPublishWhenDestinationIsEmpty() {
        // Given
        String destination = "";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(kinesisClient, never()).putRecord(any(PutRecordRequest.class));
    }

    @Test
    void shouldPublishWithSerializer() throws Exception {
        // Given
        String destination = "test-stream";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        byte[] serializedPayload = "serialized".getBytes();

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("payload", serializedPayload);
        wrapper.put("eventType", eventType);
        wrapper.put("transactionId", transactionId);
        wrapper.put("contentType", "application/json");

        when(serializer.serialize(payload)).thenReturn(serializedPayload);
        when(serializer.getContentType()).thenReturn("application/json");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":\"serialized\"}");
        when(kinesisClient.putRecord(any(PutRecordRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(PutRecordResponse.builder().sequenceNumber("123").build()));

        // When
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId, serializer))
                .verifyComplete();

        // Then
        verify(kinesisClient).putRecord(any(PutRecordRequest.class));
        verify(serializer).serialize(payload);
        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    void shouldHandleSerializationException() throws Exception {
        // Given
        String destination = "test-stream";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";

        when(serializer.serialize(payload)).thenThrow(new SerializationException("Test error"));

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId, serializer))
                .expectError(SerializationException.class)
                .verify();

        verify(kinesisClient, never()).putRecord(any(PutRecordRequest.class));
    }

    @Test
    void shouldHandleJsonProcessingException() throws Exception {
        // Given
        String destination = "test-stream";
        String eventType = "test.event";
        String payload = "test payload";
        String transactionId = "test-transaction-id";
        byte[] serializedPayload = "serialized".getBytes();

        when(serializer.serialize(payload)).thenReturn(serializedPayload);
        when(serializer.getContentType()).thenReturn("application/json");
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

        // When/Then
        StepVerifier.create(publisher.publish(destination, eventType, payload, transactionId, serializer))
                .expectError(RuntimeException.class)
                .verify();

        verify(kinesisClient, never()).putRecord(any(PutRecordRequest.class));
    }

    @Test
    void shouldBeAvailableWhenKinesisClientIsAvailable() {
        // Given
        when(kinesisClientProvider.getIfAvailable()).thenReturn(kinesisClient);
        when(messagingProperties.getKinesisConfig(anyString())).thenReturn(kinesisConfig);
        when(kinesisConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void shouldNotBeAvailableWhenKinesisClientIsNotAvailable() {
        // Given
        when(kinesisClientProvider.getIfAvailable()).thenReturn(null);
        // These mocks are not used in this test because the method returns early
        // when kinesisClientProvider.getIfAvailable() returns null
        // lenient().when(messagingProperties.getKinesisConfig(anyString())).thenReturn(kinesisConfig);
        // lenient().when(kinesisConfig.isEnabled()).thenReturn(true);

        // When
        boolean available = publisher.isAvailable();

        // Then
        assertFalse(available);
    }
}
