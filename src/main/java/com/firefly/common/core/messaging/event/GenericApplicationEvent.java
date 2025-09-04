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


package com.firefly.common.core.messaging.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * A generic application event that can be published to the Spring Event Bus.
 */
@Getter
public class GenericApplicationEvent extends ApplicationEvent {
    
    private final String eventType;
    private final String destination;
    private final String transactionId;
    
    /**
     * Creates a new GenericApplicationEvent.
     *
     * @param source the event payload
     * @param eventType the type of event
     * @param destination the destination (optional, can be used for filtering)
     * @param transactionId the transaction ID (optional)
     */
    public GenericApplicationEvent(Object source, String eventType, String destination, String transactionId) {
        super(source);
        this.eventType = eventType;
        this.destination = destination;
        this.transactionId = transactionId;
    }
    
    /**
     * Gets the event payload.
     *
     * @return the event payload
     */
    public Object getPayload() {
        return getSource();
    }
}
