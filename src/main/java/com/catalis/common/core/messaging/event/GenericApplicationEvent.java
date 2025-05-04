package com.catalis.common.core.messaging.event;

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
