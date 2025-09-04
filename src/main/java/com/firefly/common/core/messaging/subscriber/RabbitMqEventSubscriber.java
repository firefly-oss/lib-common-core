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


package com.firefly.common.core.messaging.subscriber;

import com.firefly.common.core.messaging.config.MessagingProperties;
import com.firefly.common.core.messaging.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link EventSubscriber} that uses RabbitMQ for event handling.
 * <p>
 * This implementation supports multiple RabbitMQ connections through the {@link ConnectionAwareSubscriber}
 * interface. Each connection is identified by a connection ID, which is used to look up the
 * appropriate configuration in {@link MessagingProperties}.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "messaging",
        name = {"enabled", "rabbitmq.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class RabbitMqEventSubscriber implements EventSubscriber, ConnectionAwareSubscriber {

    private final ObjectProvider<ConnectionFactory> connectionFactoryProvider;
    private final ObjectProvider<RabbitAdmin> rabbitAdminProvider;
    private final MessagingProperties messagingProperties;
    private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();

    private String connectionId = "default";

    @Override
    public Mono<Void> subscribe(
            String source,
            String eventType,
            EventHandler eventHandler,
            String groupId,
            String clientId,
            int concurrency,
            boolean autoAck) {

        return Mono.fromRunnable(() -> {
            ConnectionFactory connectionFactory = connectionFactoryProvider.getIfAvailable();
            if (connectionFactory == null) {
                log.warn("ConnectionFactory is not available. Cannot subscribe to RabbitMQ.");
                return;
            }

            RabbitAdmin rabbitAdmin = rabbitAdminProvider.getIfAvailable();
            if (rabbitAdmin == null) {
                log.warn("RabbitAdmin is not available. Cannot subscribe to RabbitMQ.");
                return;
            }

            String key = getEventKey(source, eventType);
            if (containers.containsKey(key)) {
                log.warn("Already subscribed to RabbitMQ queue {} with routing key {}", source, eventType);
                return;
            }

            try {
                // Get the RabbitMQ configuration for this connection ID
                MessagingProperties.RabbitMqConfig rabbitConfig = messagingProperties.getRabbitMqConfig(connectionId);

                // Declare exchange if it doesn't exist
                Exchange exchange = new TopicExchange(
                        source,
                        rabbitConfig.isDurable(),
                        rabbitConfig.isAutoDelete()
                );
                rabbitAdmin.declareExchange(exchange);

                // Create a queue with a unique name if not specified
                String queueName = source + ".queue";
                Queue queue = new Queue(
                        queueName,
                        rabbitConfig.isDurable(),
                        rabbitConfig.isExclusive(),
                        rabbitConfig.isAutoDelete()
                );
                rabbitAdmin.declareQueue(queue);

                // Bind the queue to the exchange with the routing key
                String routingKey = eventType.isEmpty() ?
                        rabbitConfig.getDefaultRoutingKey() : eventType;
                Binding binding = BindingBuilder.bind(queue)
                        .to((TopicExchange) exchange)
                        .with(routingKey);
                rabbitAdmin.declareBinding(binding);

                // Create a message listener container
                SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
                container.setConnectionFactory(connectionFactory);
                container.setQueueNames(queueName);
                container.setConcurrentConsumers(concurrency);
                container.setPrefetchCount(messagingProperties.getRabbitMqConfig(connectionId).getPrefetchCount());
                container.setAcknowledgeMode(autoAck ?
                        AcknowledgeMode.AUTO : AcknowledgeMode.MANUAL);

                // Set the message listener
                container.setMessageListener(message -> {
                    try {
                        // Extract headers
                        Map<String, Object> headers = new HashMap<>();
                        message.getMessageProperties().getHeaders().forEach((k, v) -> headers.put(k, v));
                        headers.put("contentType", message.getMessageProperties().getContentType());
                        headers.put("contentEncoding", message.getMessageProperties().getContentEncoding());
                        headers.put("correlationId", message.getMessageProperties().getCorrelationId());
                        headers.put("messageId", message.getMessageProperties().getMessageId());
                        headers.put("routingKey", message.getMessageProperties().getReceivedRoutingKey());

                        // Create acknowledgement if needed
                        EventHandler.Acknowledgement ack = autoAck ? null :
                                () -> Mono.fromRunnable(() -> {
                                    try {
                                        // Note: SimpleMessageListenerContainer doesn't expose getChannel() directly
                                        // In a real implementation, you would need to use a different approach
                                        // to acknowledge messages manually
                                        long deliveryTag = message.getMessageProperties().getDeliveryTag();
                                        log.info("Manual acknowledgement for delivery tag {} (implementation needed)", deliveryTag);
                                    } catch (Exception e) {
                                        log.error("Failed to acknowledge message", e);
                                    }
                                });

                        // Handle the event
                        eventHandler.handleEvent(message.getBody(), headers, ack)
                                .doOnSuccess(v -> log.debug("Successfully handled RabbitMQ message from queue {}", queueName))
                                .doOnError(error -> log.error("Error handling RabbitMQ message: {}", error.getMessage(), error))
                                .subscribe();
                    } catch (Exception e) {
                        log.error("Error processing RabbitMQ message", e);
                    }
                });

                // Start the container
                container.start();
                containers.put(key, container);

                log.info("Subscribed to RabbitMQ queue {} with routing key {}", queueName, routingKey);
            } catch (Exception e) {
                log.error("Failed to subscribe to RabbitMQ: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Mono<Void> unsubscribe(String source, String eventType) {
        return Mono.fromRunnable(() -> {
            String key = getEventKey(source, eventType);
            MessageListenerContainer container = containers.remove(key);

            if (container != null) {
                container.stop();
                log.info("Unsubscribed from RabbitMQ for source {} and event type {}", source, eventType);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return connectionFactoryProvider.getIfAvailable() != null &&
               rabbitAdminProvider.getIfAvailable() != null &&
               messagingProperties.getRabbitMqConfig(connectionId).isEnabled();
    }

    @Override
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    private String getEventKey(String source, String eventType) {
        return source + ":" + eventType;
    }
}
