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
