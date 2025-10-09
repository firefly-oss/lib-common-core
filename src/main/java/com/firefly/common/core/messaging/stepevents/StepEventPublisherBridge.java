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

package com.firefly.common.core.messaging.stepevents;

import com.firefly.common.core.messaging.annotation.PublisherType;
import com.firefly.common.core.messaging.publisher.EventPublisher;
import com.firefly.common.core.messaging.publisher.EventPublisherFactory;
import com.firefly.transactional.events.StepEventEnvelope;
import com.firefly.transactional.events.StepEventPublisher;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Bridges StepEvents to the lib-common-core EventPublisher system.
 * This adapter allows StepEvents from the transactional engine to use the messaging infrastructure
 * provided by lib-common-core, including support for multiple message brokers and connection management.
 */
@Slf4j
public class StepEventPublisherBridge implements StepEventPublisher {

    private final EventPublisherFactory eventPublisherFactory;
    private final String defaultTopic;
    private final PublisherType publisherType;
    private final String connectionId;

    public StepEventPublisherBridge(String defaultTopic, 
                                  PublisherType publisherType,
                                  String connectionId,
                                  EventPublisherFactory eventPublisherFactory) {
        this.eventPublisherFactory = eventPublisherFactory;
        this.defaultTopic = defaultTopic;
        this.publisherType = publisherType;
        this.connectionId = connectionId;
    }

    @Override
    public Mono<Void> publish(StepEventEnvelope envelope) {
        try {
            log.debug("Publishing step event through lib-common-core: sagaName={}, stepId={}, type={}", 
                     envelope.getSagaName(), envelope.getStepId(), envelope.getType());

            // Get the appropriate publisher for the configured type
            EventPublisher publisher = eventPublisherFactory.getPublisher(publisherType, connectionId);
            if (publisher == null) {
                log.warn("No publisher available for type {} with connection ID {}. Step event will not be published.", 
                        publisherType, connectionId);
                return Mono.empty();
            }

            // Set default topic if not provided
            String topic = envelope.getTopic();
            if (topic == null || topic.isEmpty()) {
                envelope.setTopic(defaultTopic);
                topic = defaultTopic;
            }

            // Set default key if not provided (saga name + saga ID)
            String key = envelope.getKey();
            if (key == null || key.isEmpty()) {
                envelope.setKey(envelope.getSagaName() + ":" + envelope.getSagaId());
            }

            // Create enriched payload with step metadata
            StepEventEnvelope enrichedPayload = createEnrichedPayload(envelope);

            // Create transaction ID for tracing
            String transactionId = envelope.getSagaId() + ":" + envelope.getStepId();

            // Make variables effectively final for lambda usage
            final String finalTopic = topic;
            final PublisherType finalPublisherType = publisherType;

            // Publish using the selected publisher
            return publisher.publish(finalTopic, envelope.getType(), enrichedPayload, transactionId)
                    .doOnSuccess(v -> log.debug("Successfully published step event via {}: topic={}", 
                                               finalPublisherType, finalTopic))
                    .doOnError(error -> log.error("Failed to publish step event via {}: {}", 
                                                 finalPublisherType, error.getMessage(), error));

        } catch (Exception e) {
            log.error("Error preparing step event for publication: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }

    /**
     * Creates an enriched payload with step metadata that provides context about the step event origin.
     */
    private StepEventEnvelope createEnrichedPayload(StepEventEnvelope original) {
        // The enriched payload includes all original data plus additional metadata
        StepEventEnvelope enriched = new StepEventEnvelope(
                original.getSagaName(),
                original.getSagaId(),
                original.getStepId(),
                original.getTopic(),
                original.getType(),
                original.getKey(),
                original.getPayload(),
                original.getHeaders(),
                original.getAttempts(),
                original.getLatencyMs(),
                original.getStartedAt(),
                original.getCompletedAt(),
                original.getResultType()
        );

        // Set timestamp if not already set
        if (enriched.getTimestamp() == null) {
            enriched.setTimestamp(java.time.Instant.now());
        }

        return enriched;
    }
}