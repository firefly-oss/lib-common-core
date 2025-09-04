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

import com.firefly.common.core.messaging.annotation.PublisherType;
import reactor.core.publisher.Mono;

/**
 * Interface for handling errors that occur during message publishing.
 * <p>
 * Implementations of this interface can be registered as Spring beans and referenced
 * by name in the {@link com.firefly.common.core.messaging.annotation.PublishResult#errorHandler()}
 * attribute to provide custom error handling for publishing operations.
 * <p>
 * The error handler is called when an error occurs during the publishing operation,
 * and it can decide how to handle the error (e.g., log it, retry, or throw a different exception).
 */
public interface PublishErrorHandler {

    /**
     * Handles an error that occurred during message publishing.
     *
     * @param destination the destination where the message was being published
     * @param eventType the type of event that was being published
     * @param payload the payload that was being published
     * @param publisherType the type of publisher that was being used
     * @param error the error that occurred
     * @return a Mono that completes when the error has been handled
     */
    Mono<Void> handleError(String destination, String eventType, Object payload, 
                          PublisherType publisherType, Throwable error);
}
