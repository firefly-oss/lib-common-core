package com.catalis.common.core.messaging.publisher;

import com.catalis.common.core.messaging.config.MessagingProperties;
import com.catalis.common.core.messaging.serialization.MessageSerializer;
import com.catalis.common.core.messaging.serialization.SerializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link EventPublisher} that uses AWS Kinesis for event publishing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KinesisEventPublisher implements EventPublisher {

    private final ObjectProvider<KinesisAsyncClient> kinesisClientProvider;
    private final MessagingProperties messagingProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.defer(() -> {
            KinesisAsyncClient kinesisClient = getKinesisClient();
            if (kinesisClient == null) {
                log.warn("KinesisAsyncClient is not available. Event will not be published to Kinesis.");
                return Mono.error(new IllegalStateException("KinesisAsyncClient is not available"));
            }

            if (destination == null || destination.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Destination (stream name) cannot be null or empty"));
            }

            log.debug("Publishing event to Kinesis: stream={}, type={}, transactionId={}",
                    destination, eventType, transactionId);

            try {
                // Convert payload to string if it's not already a string
                String payloadStr;
                if (payload == null) {
                    payloadStr = "null";
                } else if (payload instanceof String) {
                    payloadStr = (String) payload;
                } else {
                    // This is not ideal, but we'll use toString() for now
                    // A better approach would be to use a proper JSON serializer
                    payloadStr = payload.toString();
                }

                // Add metadata to the payload
                String finalPayload = String.format(
                        "{\"eventType\":\"%s\",\"transactionId\":\"%s\",\"payload\":%s}",
                        eventType != null ? eventType : "",
                        transactionId != null ? transactionId : "",
                        payloadStr
                );

                // Create a partition key (random UUID or based on eventType)
                String partitionKey = eventType != null ? eventType : UUID.randomUUID().toString();

                // Create the request
                PutRecordRequest request = PutRecordRequest.builder()
                        .streamName(destination)
                        .partitionKey(partitionKey)
                        .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(finalPayload.getBytes(StandardCharsets.UTF_8))))
                        .build();

                // Send the record asynchronously and convert to Mono
                return Mono.fromFuture(() ->
                    kinesisClient.putRecord(request)
                        .thenApply(response -> {
                            log.debug("Successfully published event to Kinesis: {}", response.sequenceNumber());
                            return response;
                        })
                ).onErrorMap(e -> {
                    log.error("Failed to publish event to Kinesis: {}", e.getMessage(), e);
                    return e;
                }).then();
            } catch (Exception e) {
                log.error("Error publishing event to Kinesis: {}", e.getMessage(), e);
                return Mono.error(e);
            }
        });
    }

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId, MessageSerializer serializer) {
        return Mono.defer(() -> {
            if (serializer == null) {
                log.warn("Serializer is null, falling back to default publish method");
                return publish(destination, eventType, payload, transactionId);
            }

            KinesisAsyncClient kinesisClient = getKinesisClient();
            if (kinesisClient == null) {
                log.warn("KinesisAsyncClient is not available. Event will not be published to Kinesis.");
                return Mono.error(new IllegalStateException("KinesisAsyncClient is not available"));
            }

            if (destination == null || destination.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Destination (stream name) cannot be null or empty"));
            }

            log.debug("Publishing event to Kinesis with serializer {}: stream={}, type={}, transactionId={}",
                    serializer.getFormat(), destination, eventType, transactionId);

            try {
                // Serialize the payload
                byte[] serializedPayload = payload != null ? serializer.serialize(payload) : new byte[0];

                // Create a partition key (random UUID or based on eventType)
                String partitionKey = eventType != null ? eventType : UUID.randomUUID().toString();

                // Create a metadata wrapper for the payload
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("payload", serializedPayload);

                if (eventType != null) {
                    wrapper.put("eventType", eventType);
                }

                if (transactionId != null) {
                    wrapper.put("transactionId", transactionId);
                }

                wrapper.put("contentType", serializer.getContentType());

                // Serialize the wrapper to JSON
                String wrapperJson = objectMapper.writeValueAsString(wrapper);

                // Create the request
                PutRecordRequest request = PutRecordRequest.builder()
                        .streamName(destination)
                        .partitionKey(partitionKey)
                        .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(wrapperJson.getBytes(StandardCharsets.UTF_8))))
                        .build();

                // Send the record asynchronously and convert to Mono
                return Mono.fromFuture(() ->
                    kinesisClient.putRecord(request)
                        .thenApply(response -> {
                            log.debug("Successfully published event to Kinesis with serializer {}: {}",
                                    serializer.getFormat(), response.sequenceNumber());
                            return response;
                        })
                ).onErrorMap(e -> {
                    log.error("Failed to publish event to Kinesis: {}", e.getMessage(), e);
                    return e;
                }).then();
            } catch (SerializationException e) {
                log.error("Failed to serialize payload for Kinesis: {}", e.getMessage(), e);
                return Mono.error(e);
            } catch (Exception e) {
                log.error("Error publishing event to Kinesis: {}", e.getMessage(), e);
                return Mono.error(e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return getKinesisClient() != null;
    }

    private KinesisAsyncClient getKinesisClient() {
        // Try to get the client from the provider
        KinesisAsyncClient client = kinesisClientProvider.getIfAvailable();
        if (client != null) {
            return client;
        }

        // If not available, try to create a new client
        try {
            MessagingProperties.KinesisConfig config = messagingProperties.getKinesis();

            KinesisAsyncClientBuilder builder = KinesisAsyncClient.builder()
                    .region(Region.of(config.getRegion()));

            // Set credentials if provided
            if (!config.getAccessKeyId().isEmpty() && !config.getSecretAccessKey().isEmpty()) {
                AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey())
                );
                builder.credentialsProvider(credentialsProvider);
            }

            // Set endpoint if provided
            if (!config.getEndpoint().isEmpty()) {
                builder.endpointOverride(URI.create(config.getEndpoint()));
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to create KinesisAsyncClient: {}", e.getMessage(), e);
            return null;
        }
    }
}
