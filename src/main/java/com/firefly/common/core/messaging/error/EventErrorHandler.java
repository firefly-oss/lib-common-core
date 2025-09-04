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


package com.firefly.common.core.messaging.error;

import com.firefly.common.core.messaging.annotation.SubscriberType;
import com.firefly.common.core.messaging.handler.EventHandler;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Interface for handling errors that occur during event processing.
 * <p>
 * Implementations of this interface can be registered as Spring beans and referenced
 * by name in the {@link com.firefly.common.core.messaging.annotation.EventListener#errorHandler()}
 * attribute to provide custom error handling for event processing.
 * <p>
 * The error handler is called when an error occurs during the event processing,
 * and it can decide how to handle the error (e.g., log it, retry, or throw a different exception).
 */
public interface EventErrorHandler {

    /**
     * Handles an error that occurred during event processing.
     *
     * @param source the source from which the event was received
     * @param eventType the type of event that was being processed
     * @param payload the payload that was being processed
     * @param headers the headers of the event
     * @param subscriberType the type of subscriber that received the event
     * @param error the error that occurred
     * @param acknowledgement the acknowledgement callback (can be null if auto-ack is enabled)
     * @return a Mono that completes when the error has been handled
     */
    Mono<Void> handleError(String source, String eventType, Object payload, Map<String, Object> headers,
                          SubscriberType subscriberType, Throwable error, 
                          EventHandler.Acknowledgement acknowledgement);
}
