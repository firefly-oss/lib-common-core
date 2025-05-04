package com.catalis.common.core.messaging.subscriber;

import com.catalis.common.core.messaging.annotation.SubscriberType;
import com.catalis.common.core.messaging.resilience.ResilientEventSubscriberFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating event subscribers based on the subscriber type.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriberFactory {

    private final List<EventSubscriber> subscribers;
    private final ResilientEventSubscriberFactory resilientFactory;
    private Map<Class<? extends EventSubscriber>, EventSubscriber> subscriberMap;

    /**
     * Gets the appropriate event subscriber for the specified subscriber type.
     *
     * @param subscriberType the type of subscriber to get
     * @return the event subscriber, or null if not available
     */
    public EventSubscriber getSubscriber(SubscriberType subscriberType) {
        if (subscriberMap == null) {
            initSubscriberMap();
        }

        EventSubscriber baseSubscriber = switch (subscriberType) {
            case EVENT_BUS -> subscriberMap.get(SpringEventSubscriber.class);
            case KAFKA -> subscriberMap.get(KafkaEventSubscriber.class);
            case RABBITMQ -> subscriberMap.get(RabbitMqEventSubscriber.class);
            case SQS -> subscriberMap.get(SqsEventSubscriber.class);
            case GOOGLE_PUBSUB -> subscriberMap.get(GooglePubSubEventSubscriber.class);
            case AZURE_SERVICE_BUS -> subscriberMap.get(AzureServiceBusEventSubscriber.class);
            case REDIS -> subscriberMap.get(RedisEventSubscriber.class);
            case JMS -> subscriberMap.get(JmsEventSubscriber.class);
            case KINESIS -> subscriberMap.get(KinesisEventSubscriber.class);
        };

        if (baseSubscriber == null) {
            log.warn("Subscriber of type {} is not available", subscriberType);
            return null;
        }

        if (!baseSubscriber.isAvailable()) {
            log.warn("Subscriber of type {} is not properly configured", subscriberType);
            return null;
        }

        // Wrap the subscriber with resilience capabilities
        return resilientFactory.createResilientSubscriber(
                baseSubscriber,
                subscriberType.name().toLowerCase()
        );
    }

    private void initSubscriberMap() {
        subscriberMap = subscribers.stream()
                .collect(Collectors.toMap(
                        EventSubscriber::getClass,
                        Function.identity()
                ));
    }
}
