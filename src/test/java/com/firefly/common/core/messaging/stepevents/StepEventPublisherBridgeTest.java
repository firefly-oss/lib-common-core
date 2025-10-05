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
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for StepEventPublisherBridge integration with lib-common-core messaging.
 * Tests cover step event publishing through lib-common-core infrastructure, metadata handling,
 * and bridge pattern implementation for banking transaction workflows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("lib-common-core Step Events Bridge - Transaction Workflow Step Publishing")
class StepEventPublisherBridgeTest {

    @Mock
    private EventPublisherFactory eventPublisherFactory;

    @Mock
    private EventPublisher eventPublisher;

    private StepEventPublisherBridge stepEventBridge;
    private final String defaultTopic = "banking-step-events";
    private final PublisherType publisherType = PublisherType.KAFKA;
    private final String connectionId = "banking-connection";

    @BeforeEach
    void setUp() {
        stepEventBridge = new StepEventPublisherBridge(
                defaultTopic,
                publisherType,
                connectionId,
                eventPublisherFactory
        );
    }

    private StepEventEnvelope createStepEvent(String sagaName, String sagaId, String type, String key, Object payload) {
        return new StepEventEnvelope(
            sagaName,
            sagaId,
            "step-1", // stepId
            null, // topic
            type,
            key,
            payload,
            Map.of(), // headers
            1, // attempts
            250L, // latencyMs
            Instant.now().minusMillis(250), // startedAt
            Instant.now(), // completedAt
            "SUCCESS" // resultType
        );
    }

    @Test
    @DisplayName("Should publish money transfer step event with lib-common-core publisher")
    void shouldPublishMoneyTransferStepEvent() {
        // Given: Event publisher is available
        when(eventPublisherFactory.getPublisher(publisherType, connectionId))
            .thenReturn(eventPublisher);
        when(eventPublisher.publish(anyString(), anyString(), any(), anyString()))
            .thenReturn(Mono.empty());

        // Given: A money transfer saga step has completed
        MoneyTransferStepPayload payload = new MoneyTransferStepPayload(
            "TXN-12345",
            "ACC-001",
            "ACC-002",
            new BigDecimal("1000.00"),
            "USD",
            "COMPLETED"
        );

        StepEventEnvelope stepEvent = createStepEvent(
            "MoneyTransferSaga",
            "SAGA-67890",
            "transfer.step.completed",
            "TXN-12345",
            payload
        );

        // When: The step event is published through the bridge
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Event publisher should be called with properly transformed event
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<StepEventEnvelope> payloadCaptor = ArgumentCaptor.forClass(StepEventEnvelope.class);
        ArgumentCaptor<String> transactionIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(eventPublisher).publish(
            topicCaptor.capture(),
            typeCaptor.capture(),
            payloadCaptor.capture(),
            transactionIdCaptor.capture()
        );

        // Verify basic event properties
        assertThat(topicCaptor.getValue()).isEqualTo(defaultTopic);
        assertThat(typeCaptor.getValue()).isEqualTo("transfer.step.completed");
        assertThat(transactionIdCaptor.getValue()).isEqualTo("SAGA-67890:step-1");

        // Verify the payload contains the original step event data
        StepEventEnvelope publishedPayload = payloadCaptor.getValue();
        assertThat(publishedPayload.getSagaName()).isEqualTo("MoneyTransferSaga");
        assertThat(publishedPayload.getSagaId()).isEqualTo("SAGA-67890");
        assertThat(publishedPayload.getStepId()).isEqualTo("step-1");
        assertThat(publishedPayload.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Should auto-generate key from saga name and ID when key is missing")
    void shouldAutoGenerateKeyFromSagaNameAndId() {
        // Given: Event publisher is available
        when(eventPublisherFactory.getPublisher(publisherType, connectionId))
            .thenReturn(eventPublisher);
        when(eventPublisher.publish(anyString(), anyString(), any(), anyString()))
            .thenReturn(Mono.empty());

        // Given: A step event without a key
        StepEventEnvelope stepEvent = createStepEvent(
            "AccountOpeningSaga",
            "SAGA-12345",
            "account.validation.completed",
            null, // No key provided
            "Account validation successful"
        );

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Key should be auto-generated from saga name and ID
        verify(eventPublisher).publish(eq(defaultTopic), eq("account.validation.completed"), any(), eq("SAGA-12345:step-1"));

        // Verify the step event was modified to include the generated key
        assertThat(stepEvent.getKey()).isEqualTo("AccountOpeningSaga:SAGA-12345");
    }

    @Test
    @DisplayName("Should use default topic when topic is missing")
    void shouldUseDefaultTopicWhenTopicMissing() {
        // Given: Event publisher is available
        when(eventPublisherFactory.getPublisher(publisherType, connectionId))
            .thenReturn(eventPublisher);
        when(eventPublisher.publish(anyString(), anyString(), any(), anyString()))
            .thenReturn(Mono.empty());

        // Given: A step event without a topic
        StepEventEnvelope stepEvent = createStepEvent(
            "LoanApprovalSaga",
            "SAGA-54321",
            "loan.credit.check.completed",
            "LOAN-98765",
            "Credit check passed"
        );

        // When: The step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: Default topic should be used
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).publish(topicCaptor.capture(), anyString(), any(), anyString());
        assertThat(topicCaptor.getValue()).isEqualTo(defaultTopic);

        // Verify the step event was modified to include the default topic
        assertThat(stepEvent.getTopic()).isEqualTo(defaultTopic);
    }

    @Test
    @DisplayName("Should handle case when publisher is not available")
    void shouldHandleCaseWhenPublisherIsNotAvailable() {
        // Given: Event publisher is not available
        when(eventPublisherFactory.getPublisher(publisherType, connectionId))
            .thenReturn(null);

        // Given: A step event
        StepEventEnvelope stepEvent = createStepEvent(
            "TestSaga",
            "SAGA-ERROR-001",
            "test.step",
            "TEST-001",
            "test payload"
        );

        // When: The step event is published and publisher is not available
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete(); // Should complete without error

        // Then: No publisher interaction should occur
        verify(eventPublisher, org.mockito.Mockito.never()).publish(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Should propagate publisher errors")
    void shouldPropagatePublisherErrors() {
        // Given: Event publisher throws an error
        RuntimeException publisherError = new RuntimeException("Message broker unavailable");
        when(eventPublisherFactory.getPublisher(publisherType, connectionId))
            .thenReturn(eventPublisher);
        when(eventPublisher.publish(anyString(), anyString(), any(), anyString()))
            .thenReturn(Mono.error(publisherError));

        StepEventEnvelope stepEvent = createStepEvent(
            "TestSaga",
            "SAGA-ERROR-001",
            "test.step",
            "TEST-001",
            "test payload"
        );

        // When: The step event is published and publisher fails
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .expectError(RuntimeException.class)
            .verify();

        // Then: Error should be propagated
        verify(eventPublisher).publish(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle step event with retry attempts and failure metadata")
    void shouldHandleStepEventWithRetryAttemptsAndFailureMetadata() {
        // Given: Event publisher is available
        when(eventPublisherFactory.getPublisher(publisherType, connectionId))
            .thenReturn(eventPublisher);
        when(eventPublisher.publish(anyString(), anyString(), any(), anyString()))
            .thenReturn(Mono.empty());

        // Given: A step event that failed and was retried
        FraudCheckStepPayload payload = new FraudCheckStepPayload(
            "TXN-99999",
            "FRAUD_CHECK",
            "FAILED",
            "Suspicious transaction pattern detected",
            85.5
        );

        StepEventEnvelope stepEvent = new StepEventEnvelope(
            "FraudDetectionSaga",
            "SAGA-FRAUD-001",
            "step-fraud-check", // stepId
            "banking-fraud-events", // topic
            "fraud.check.failed", // type
            "TXN-99999", // key
            payload,
            Map.of(
                "source", "fraud-detection-service",
                "priority", "high",
                "alert-level", "critical"
            ),
            3, // Multiple attempts
            1200L, // Longer latency due to retries
            Instant.now().minusMillis(1200),
            Instant.now(),
            "FAILURE"
        );

        // When: The failed step event is published
        StepVerifier.create(stepEventBridge.publish(stepEvent))
            .verifyComplete();

        // Then: All failure and retry metadata should be preserved in the published payload
        ArgumentCaptor<StepEventEnvelope> payloadCaptor = ArgumentCaptor.forClass(StepEventEnvelope.class);
        verify(eventPublisher).publish(eq("banking-fraud-events"), eq("fraud.check.failed"), payloadCaptor.capture(), eq("SAGA-FRAUD-001:step-fraud-check"));

        StepEventEnvelope publishedPayload = payloadCaptor.getValue();
        assertThat(publishedPayload.getAttempts()).isEqualTo(3);
        assertThat(publishedPayload.getLatencyMs()).isEqualTo(1200L);
        assertThat(publishedPayload.getResultType()).isEqualTo("FAILURE");
        assertThat(publishedPayload.getHeaders()).containsEntry("priority", "high");
        assertThat(publishedPayload.getHeaders()).containsEntry("alert-level", "critical");
    }

    // Test Payloads for Banking Domain Step Events
    @Data
    static class MoneyTransferStepPayload {
        private final String transactionId;
        private final String fromAccount;
        private final String toAccount;
        private final BigDecimal amount;
        private final String currency;
        private final String status;
    }

    @Data
    static class FraudCheckStepPayload {
        private final String transactionId;
        private final String checkType;
        private final String result;
        private final String reason;
        private final Double riskScore;
    }
}