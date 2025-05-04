package com.catalis.common.core.messaging.error;

import com.catalis.common.core.messaging.annotation.SubscriberType;
import com.catalis.common.core.messaging.handler.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Default implementation of {@link EventErrorHandler} that logs the error and acknowledges the message.
 * <p>
 * This implementation is used when no custom error handler is specified in the
 * {@link com.catalis.common.core.messaging.annotation.EventListener#errorHandler()} attribute.
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
