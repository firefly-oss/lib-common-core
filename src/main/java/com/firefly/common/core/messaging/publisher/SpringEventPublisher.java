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


package com.firefly.common.core.messaging.publisher;

import com.firefly.common.core.messaging.event.GenericApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link EventPublisher} that uses Spring's ApplicationEventPublisher.
 * <p>
 * This implementation supports the {@link ConnectionAwarePublisher} interface for consistency,
 * but since Spring's ApplicationEventPublisher is internal to the application, the connection ID
 * is not used for any configuration lookup.
 */
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SpringEventPublisher implements EventPublisher, ConnectionAwarePublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    private String connectionId = "default";

    @Override
    public Mono<Void> publish(String destination, String eventType, Object payload, String transactionId) {
        return Mono.fromRunnable(() -> {
            log.debug("Publishing event to Spring Event Bus: type={}, transactionId={}", eventType, transactionId);
            GenericApplicationEvent event = new GenericApplicationEvent(
                    payload,
                    eventType,
                    destination,
                    transactionId
            );
            applicationEventPublisher.publishEvent(event);
        });
    }

    @Override
    public boolean isAvailable() {
        return true; // Spring Event Bus is always available
    }

    @Override
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }
}
