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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Default implementation of {@link EventErrorHandler} that logs the error and acknowledges the message.
 * <p>
 * This implementation is used when no custom error handler is specified in the
 * {@link com.firefly.common.core.messaging.annotation.EventListener#errorHandler()} attribute.
 */
@Component("defaultEventErrorHandler")
@Slf4j
public class DefaultEventErrorHandler implements EventErrorHandler {

    @Override
    public Mono<Void> handleError(String source, String eventType, Object payload, Map<String, Object> headers,
                                 SubscriberType subscriberType, Throwable error, 
                                 EventHandler.Acknowledgement acknowledgement) {
        log.error("Error processing event from {} with eventType={} using {}: {}",
                source, eventType, subscriberType, error.getMessage(), error);
        
        // Acknowledge the message if auto-ack is disabled
        if (acknowledgement != null) {
            return acknowledgement.acknowledge()
                    .doOnSuccess(v -> log.debug("Acknowledged event after error"));
        }
        
        return Mono.empty();
    }
}
