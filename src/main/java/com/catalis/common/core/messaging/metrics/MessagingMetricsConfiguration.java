package com.catalis.common.core.messaging.metrics;

import com.catalis.common.core.messaging.config.MessagingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for messaging system metrics.
 * <p>
 * This class configures metrics for the messaging systems using Micrometer.
 * It registers metrics for message publishing and subscribing operations.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
public class MessagingMetricsConfiguration {

    /**
     * Creates a meter registry customizer for messaging metrics.
     * This adds common tags to all messaging metrics.
     *
     * @param messagingProperties the messaging properties
     * @return a meter registry customizer
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> messagingMetricsCommonTags(MessagingProperties messagingProperties) {
        return registry -> registry.config()
                .commonTags("application", messagingProperties.getApplicationName() != null ? 
                        messagingProperties.getApplicationName() : "unknown");
    }

    /**
     * Creates a meter binder for messaging system metrics.
     * This registers gauges for each enabled messaging system.
     *
     * @param messagingProperties the messaging properties
     * @return a meter binder
     */
    @Bean
    public MeterBinder messagingSystemMetrics(MessagingProperties messagingProperties) {
        return registry -> {
            // Register gauges for each enabled messaging system
            registerMessagingSystemGauge(registry, "kafka", messagingProperties.getKafka().isEnabled(), messagingProperties);
            registerMessagingSystemGauge(registry, "rabbitmq", messagingProperties.getRabbitmq().isEnabled(), messagingProperties);
            registerMessagingSystemGauge(registry, "sqs", messagingProperties.getSqs().isEnabled(), messagingProperties);
            registerMessagingSystemGauge(registry, "google-pub-sub", messagingProperties.getGooglePubSub().isEnabled(), messagingProperties);
            registerMessagingSystemGauge(registry, "azure-service-bus", messagingProperties.getAzureServiceBus().isEnabled(), messagingProperties);
            registerMessagingSystemGauge(registry, "redis", messagingProperties.getRedis().isEnabled(), messagingProperties);
            registerMessagingSystemGauge(registry, "jms", messagingProperties.getJms().isEnabled(), messagingProperties);
            registerMessagingSystemGauge(registry, "kinesis", messagingProperties.getKinesis().isEnabled(), messagingProperties);
        };
    }

    /**
     * Creates a messaging metrics aspect for tracking message publishing and subscribing operations.
     *
     * @param meterRegistry the meter registry
     * @return a messaging metrics aspect
     */
    @Bean
    public MessagingMetricsAspect messagingMetricsAspect(MeterRegistry meterRegistry) {
        return new MessagingMetricsAspect(meterRegistry);
    }

    private void registerMessagingSystemGauge(MeterRegistry registry, String system, boolean enabled, MessagingProperties properties) {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("messaging.system", system));
        
        // Add system-specific tags
        switch (system) {
            case "kafka":
                tags.add(Tag.of("bootstrap.servers", properties.getKafka().getBootstrapServers()));
                break;
            case "rabbitmq":
                tags.add(Tag.of("host", properties.getRabbitmq().getHost()));
                tags.add(Tag.of("port", String.valueOf(properties.getRabbitmq().getPort())));
                break;
            case "sqs":
                tags.add(Tag.of("region", properties.getSqs().getRegion()));
                break;
            case "google-pub-sub":
                tags.add(Tag.of("project.id", properties.getGooglePubSub().getProjectId()));
                break;
            case "azure-service-bus":
                tags.add(Tag.of("namespace", properties.getAzureServiceBus().getNamespace()));
                break;
            case "redis":
                tags.add(Tag.of("host", properties.getRedis().getHost()));
                tags.add(Tag.of("port", String.valueOf(properties.getRedis().getPort())));
                break;
            case "jms":
                tags.add(Tag.of("broker.url", properties.getJms().getBrokerUrl()));
                break;
            case "kinesis":
                tags.add(Tag.of("region", properties.getKinesis().getRegion()));
                break;
        }
        
        // Register a gauge that reports whether the system is enabled (1) or disabled (0)
        registry.gauge("messaging.system.enabled", Tags.of(tags), enabled ? 1 : 0);
    }
}