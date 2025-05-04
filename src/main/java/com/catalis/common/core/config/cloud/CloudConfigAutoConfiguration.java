package com.catalis.common.core.config.cloud;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

/**
 * Auto-configuration for Spring Cloud Config client.
 * <p>
 * This class provides auto-configuration for the Spring Cloud Config client,
 * which enables centralized configuration management for microservices.
 * <p>
 * The configuration is automatically enabled when the Spring Cloud Config client
 * dependency is present and the cloud.config.enabled property is set to true.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.config.client.ConfigServicePropertySourceLocator")
@ConditionalOnProperty(prefix = "cloud.config", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CloudConfigProperties.class)
@Slf4j
public class CloudConfigAutoConfiguration {

    private final CloudConfigProperties properties;
    private final Environment environment;

    public CloudConfigAutoConfiguration(CloudConfigProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
        log.info("Initializing Spring Cloud Config client with URI: {}", properties.getUri());
    }

    /**
     * Configures the refresh endpoint for dynamic configuration updates.
     * This endpoint allows for refreshing configuration at runtime without
     * restarting the application.
     *
     * @return the refresh endpoint bean
     */
    @Bean
    @ConditionalOnClass(RefreshEndpoint.class)
    @ConditionalOnProperty(prefix = "cloud.config", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RefreshEndpoint refreshEndpoint(ConfigurationPropertiesRebinder rebinder) {
        log.info("Enabling configuration refresh endpoint");
        return new RefreshEndpoint(rebinder);
    }

    /**
     * Listener for refresh events to log when configuration has been refreshed.
     *
     * @return the refresh event listener
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloud.config", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationListener<RefreshScopeRefreshedEvent> refreshEventListener() {
        return event -> log.info("Configuration refreshed: {}", event);
    }

    /**
     * Example of a bean that can be refreshed when configuration changes.
     * This is just a demonstration of how to use the @RefreshScope annotation.
     *
     * @return a refreshable configuration bean
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "refreshableConfig")
    public RefreshableConfig refreshableConfig() {
        return new RefreshableConfig(environment);
    }

    /**
     * A simple class that demonstrates how to use the @RefreshScope annotation.
     * Beans annotated with @RefreshScope will be recreated when a refresh event occurs.
     */
    public static class RefreshableConfig {
        private final Environment environment;

        public RefreshableConfig(Environment environment) {
            this.environment = environment;
            log.info("RefreshableConfig created with environment: {}", environment);
        }

        /**
         * Gets a property from the environment.
         *
         * @param key the property key
         * @return the property value or null if not found
         */
        public String getProperty(String key) {
            return environment.getProperty(key);
        }
    }
}
