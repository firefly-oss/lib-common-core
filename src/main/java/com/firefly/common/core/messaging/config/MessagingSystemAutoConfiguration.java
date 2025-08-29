package com.firefly.common.core.messaging.config;

import com.firefly.common.core.messaging.publisher.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Auto-configuration for messaging systems.
 * <p>
 * This class uses the factory pattern to conditionally register beans for each messaging system.
 * Only the messaging systems that are enabled in the configuration will have their beans registered.
 * This approach makes the conditional loading more explicit and easier to understand.
 */
@Configuration
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
public class MessagingSystemAutoConfiguration {

    /**
     * Creates a Kafka event publisher bean if Kafka is enabled.
     *
     * @param kafkaTemplateProvider provider for the Kafka template
     * @param properties messaging properties
     * @return a Kafka event publisher
     */
    @Bean("kafkaEventPublisher")
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.kafka", name = "enabled", havingValue = "true")
    public KafkaEventPublisher kafkaEventPublisher(ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider,
                                             MessagingProperties properties) {
        return new KafkaEventPublisher(kafkaTemplateProvider, properties);
    }

    /**
     * Creates a RabbitMQ event publisher bean if RabbitMQ is enabled.
     *
     * @param rabbitTemplateProvider provider for the RabbitMQ template
     * @param properties messaging properties
     * @return a RabbitMQ event publisher
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.rabbitmq", name = "enabled", havingValue = "true")
    public EventPublisher rabbitMqEventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider, 
                                                MessagingProperties properties) {
        return new RabbitMqEventPublisher(rabbitTemplateProvider, properties);
    }

    /**
     * Creates an Amazon SQS event publisher bean if SQS is enabled.
     *
     * @param sqsTemplateProvider provider for the SQS template
     * @param sqsAsyncClientProvider provider for the SQS async client
     * @param properties messaging properties
     * @return an SQS event publisher
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.sqs", name = "enabled", havingValue = "true")
    public EventPublisher sqsEventPublisher(ObjectProvider<SqsTemplate> sqsTemplateProvider, 
                                           ObjectProvider<SqsAsyncClient> sqsAsyncClientProvider,
                                           MessagingProperties properties) {
        return new SqsEventPublisher(sqsTemplateProvider, sqsAsyncClientProvider, properties);
    }

    /**
     * Creates a Google Pub/Sub event publisher bean if Google Pub/Sub is enabled.
     *
     * @param pubSubTemplateProvider provider for the PubSub template
     * @param pubSubPublisherTemplateProvider provider for the PubSub publisher template
     * @param properties messaging properties
     * @param objectMapper object mapper for JSON serialization
     * @return a Google Pub/Sub event publisher
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.googlePubSub", name = "enabled", havingValue = "true")
    public EventPublisher googlePubSubEventPublisher(ObjectProvider<PubSubTemplate> pubSubTemplateProvider, 
                                                    ObjectProvider<PubSubPublisherTemplate> pubSubPublisherTemplateProvider,
                                                    MessagingProperties properties,
                                                    ObjectMapper objectMapper) {
        return new GooglePubSubEventPublisher(pubSubTemplateProvider, pubSubPublisherTemplateProvider, properties, objectMapper);
    }

    /**
     * Creates an Azure Service Bus event publisher bean if Azure Service Bus is enabled.
     *
     * @param serviceBusClientBuilderProvider provider for the Azure Service Bus client builder
     * @param properties messaging properties
     * @param objectMapper object mapper for JSON serialization
     * @return an Azure Service Bus event publisher
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.azureServiceBus", name = "enabled", havingValue = "true")
    public EventPublisher azureServiceBusEventPublisher(ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider, 
                                                       MessagingProperties properties,
                                                       ObjectMapper objectMapper) {
        return new AzureServiceBusEventPublisher(serviceBusClientBuilderProvider, properties, objectMapper);
    }

    /**
     * Creates a Redis event publisher bean if Redis is enabled.
     *
     * @param redisTemplateProvider provider for the Redis template
     * @param properties messaging properties
     * @param objectMapper object mapper for JSON serialization
     * @return a Redis event publisher
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.redis", name = "enabled", havingValue = "true")
    public EventPublisher redisEventPublisher(ObjectProvider<ReactiveRedisTemplate<String, Object>> redisTemplateProvider, 
                                             MessagingProperties properties,
                                             ObjectMapper objectMapper) {
        return new RedisEventPublisher(redisTemplateProvider, properties, objectMapper);
    }

    /**
     * Creates a JMS event publisher bean if JMS is enabled.
     *
     * @param jmsTemplateProvider provider for the JMS template
     * @param properties messaging properties
     * @param objectMapper object mapper for JSON serialization
     * @return a JMS event publisher
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.jms", name = "enabled", havingValue = "true")
    public EventPublisher jmsEventPublisher(ObjectProvider<JmsTemplate> jmsTemplateProvider, 
                                           MessagingProperties properties,
                                           ObjectMapper objectMapper) {
        return new JmsEventPublisher(jmsTemplateProvider, properties, objectMapper);
    }

    /**
     * Creates a Kinesis event publisher bean if Kinesis is enabled.
     *
     * @param kinesisClientProvider provider for the Kinesis client
     * @param properties messaging properties
     * @param objectMapper object mapper for JSON serialization
     * @return a Kinesis event publisher
     */
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "messaging.kinesis", name = "enabled", havingValue = "true")
    public EventPublisher kinesisEventPublisher(ObjectProvider<KinesisAsyncClient> kinesisClientProvider, 
                                               MessagingProperties properties,
                                               ObjectMapper objectMapper) {
        return new KinesisEventPublisher(kinesisClientProvider, properties, objectMapper);
    }

    /**
     * Creates a Spring event publisher bean.
     * This is always enabled when messaging is enabled.
     *
     * @param applicationEventPublisher Spring's application event publisher
     * @return a Spring event publisher
     */
    @Bean
    public EventPublisher springEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringEventPublisher(applicationEventPublisher);
    }
}
