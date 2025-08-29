package com.firefly.common.core.messaging.handler;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Interface for handling events received from messaging systems.
 * <p>
 * This interface defines the contract for handling events received from various messaging systems.
 * It is used by the EventSubscriber implementations to pass events to the appropriate
 * handler method.
 */
public interface EventHandler {
    
    /**
     * Handles an event received from a messaging system.
     *
     * @param payload the event payload
     * @param headers the event headers
     * @param acknowledgement the acknowledgement callback (can be null if auto-ack is enabled)
     * @return a Mono that completes when the event has been handled
     */
    Mono<Void> handleEvent(byte[] payload, Map<String, Object> headers, Acknowledgement acknowledgement);
    
    /**
     * Gets the target type for deserialization.
     *
     * @return the target type class
     */
    Class<?> getTargetType();
    
    /**
     * Functional interface for acknowledging events.
     */
    @FunctionalInterface
    interface Acknowledgement {
        
        /**
         * Acknowledges the event.
         *
         * @return a Mono that completes when the acknowledgement is complete
         */
        Mono<Void> acknowledge();
    }
}