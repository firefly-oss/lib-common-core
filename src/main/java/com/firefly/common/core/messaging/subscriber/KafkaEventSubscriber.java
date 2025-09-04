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


package com.firefly.common.core.messaging.subscriber;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.handler.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link EventSubscriber} that uses Kafka for event handling.
 * <p>
 * This implementation supports multiple Kafka connections through the {@link ConnectionAwareSubscriber}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "kafka.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
@Slf4j
public class KafkaEventSubscriber implements EventSubscriber, ConnectionAwareSubscriber {

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;
    private final ObjectProvider<KafkaListenerEndpointRegistry> kafkaListenerEndpointRegistryProvider;
    private final MessagingProperties messagingProperties;
    private final Map<String, Flux<reactor.kafka.receiver.ReceiverRecord<String, byte[]>>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentMessageListenerContainer<String, Object>> containers = new ConcurrentHashMap<>();

    private String connectionId = "default";

    public KafkaEventSubscriber(
            ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider,
            ObjectProvider<KafkaListenerEndpointRegistry> kafkaListenerEndpointRegistryProvider,
            MessagingProperties messagingProperties) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.kafkaListenerEndpointRegistryProvider = kafkaListenerEndpointRegistryProvider;
        this.messagingProperties = messagingProperties;
    }

    @Override
    public Mono<Void> subscribe(
            String source,
            String eventType,
            EventHandler eventHandler,
            String groupId,
            String clientId,
            int concurrency,
            boolean autoAck) {

        String key = getEventKey(source, eventType);

        if (subscriptions.containsKey(key)) {
            log.warn("Already subscribed to Kafka topic {} with event type {}", source, eventType);
            return Mono.empty();
        }

        try {
            // Create consumer properties
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, messagingProperties.getKafka().getBootstrapServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId.isEmpty() ?
                    "messaging-consumer-" + source : groupId);
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId.isEmpty() ?
                    "messaging-client-" + source : clientId);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoAck);

            // Add security properties if needed
            if (!"PLAINTEXT".equals(messagingProperties.getKafka().getSecurityProtocol())) {
                props.put("security.protocol", messagingProperties.getKafka().getSecurityProtocol());

                if (!messagingProperties.getKafka().getSaslMechanism().isEmpty()) {
                    props.put("sasl.mechanism", messagingProperties.getKafka().getSaslMechanism());
                    props.put("sasl.jaas.config", String.format(
                            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                            messagingProperties.getKafka().getSaslUsername(),
                            messagingProperties.getKafka().getSaslPassword()
                    ));
                }
            }

            // Add additional properties
            props.putAll(messagingProperties.getKafka().getProperties());

            // Create receiver options
            ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions.<String, byte[]>create(props)
                    .subscription(Collections.singleton(source));

            // Create consumer template
            ReactiveKafkaConsumerTemplate<String, byte[]> template =
                    new ReactiveKafkaConsumerTemplate<>(receiverOptions);

            // Subscribe to the topic
            Flux<reactor.kafka.receiver.ReceiverRecord<String, byte[]>> flux = template.receive()
                    .filter(record -> {
                        // Filter by event type if specified
                        if (!eventType.isEmpty()) {
                            String recordEventType = record.headers().lastHeader("eventType") != null ?
                                    new String(record.headers().lastHeader("eventType").value()) : "";
                            return eventType.equals(recordEventType);
                        }
                        return true;
                    })
                    .doOnNext(record -> {
                        // Create headers
                        Map<String, Object> headers = new HashMap<>();
                        headers.put(KafkaHeaders.RECEIVED_TOPIC, record.topic());
                        headers.put(KafkaHeaders.RECEIVED_KEY, record.key());
                        headers.put(KafkaHeaders.RECEIVED_PARTITION, record.partition());
                        headers.put(KafkaHeaders.OFFSET, record.offset());

                        // Add custom headers
                        record.headers().forEach(header ->
                                headers.put(header.key(), new String(header.value())));

                        // Create acknowledgement
                        EventHandler.Acknowledgement ack = autoAck ? null :
                                () -> Mono.fromRunnable(() -> record.receiverOffset().acknowledge());

                        // Handle the event
                        eventHandler.handleEvent(record.value(), headers, ack)
                                .doOnSuccess(v -> log.debug("Successfully handled Kafka event from topic {}", record.topic()))
                                .doOnError(error -> log.error("Error handling Kafka event: {}", error.getMessage(), error))
                                .subscribe();
                    })
                    .doOnError(error -> log.error("Error in Kafka subscription: {}", error.getMessage(), error))
                    .doOnComplete(() -> log.info("Kafka subscription completed for topic {}", source))
                    .doOnCancel(() -> log.info("Kafka subscription cancelled for topic {}", source));

            // Start the subscription
            subscriptions.put(key, flux);
            flux.subscribe();

            log.info("Subscribed to Kafka topic {} with event type {}", source, eventType);
            return Mono.empty();
        } catch (Exception e) {
            log.error("Failed to subscribe to Kafka topic {} with event type {}: {}",
                    source, eventType, e.getMessage(), e);
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        String key = getEventKey(source, eventType);
        Flux<reactor.kafka.receiver.ReceiverRecord<String, byte[]>> flux = subscriptions.remove(key);

        if (flux != null) {
            // Cancel the subscription
            flux.subscribe().dispose();
            log.info("Unsubscribed from Kafka topic {} with event type {}", source, eventType);
        }

        return Mono.empty();
    }

    @Override
    public boolean isAvailable() {
        return kafkaTemplateProvider.getIfAvailable() != null &&
               kafkaListenerEndpointRegistryProvider.getIfAvailable() != null &&
               messagingProperties.getKafkaConfig(connectionId).isEnabled();
    }

    @Override
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    private String getEventKey(String source, String eventType) {
        return source + ":" + eventType;
    }
}
