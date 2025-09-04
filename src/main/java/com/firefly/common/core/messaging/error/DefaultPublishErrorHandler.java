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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link PublishErrorHandler} that logs the error and continues execution.
 * <p>
 * This implementation is used when no custom error handler is specified in the
 * {@link com.firefly.common.core.messaging.annotation.PublishResult#errorHandler()} attribute.
 */
@Component("defaultPublishErrorHandler")
@Slf4j
public class DefaultPublishErrorHandler implements PublishErrorHandler {

    @Override
    public Mono<Void> handleError(String destination, String eventType, Object payload, 
                                 PublisherType publisherType, Throwable error) {
        log.error("Error publishing message to {} with eventType={} using {}: {}",
                destination, eventType, publisherType, error.getMessage(), error);
        
        // Complete without error to allow the method execution to continue
        return Mono.empty();
    }
}
