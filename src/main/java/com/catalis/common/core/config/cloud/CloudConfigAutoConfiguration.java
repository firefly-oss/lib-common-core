package com.catalis.common.core.config.cloud;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.refresh.LegacyContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration.RefreshProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
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
    private final ConfigurableApplicationContext applicationContext;

    public CloudConfigAutoConfiguration(CloudConfigProperties properties, Environment environment, ConfigurableApplicationContext applicationContext) {
        this.properties = properties;
        this.environment = environment;
        this.applicationContext = applicationContext;
        log.info("Initializing Spring Cloud Config client with URI: {}", properties.getUri());
    }

    /**
     * Creates a RefreshScope for refreshing beans when configuration changes.
     *
     * @return the refresh scope bean
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.context.scope.refresh.RefreshScope")
    @ConditionalOnProperty(prefix = "cloud.config", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RefreshScope refreshScope() {
        log.info("Creating refresh scope");
        return new RefreshScope();
    }

    /**
     * Creates a RefreshProperties for configuring refresh behavior.
     *
     * @return the refresh properties bean
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.autoconfigure.RefreshAutoConfiguration$RefreshProperties")
    @ConditionalOnProperty(prefix = "cloud.config", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RefreshProperties refreshProperties() {
        log.info("Creating refresh properties");
        return new RefreshProperties();
    }

    /**
     * Creates a ContextRefresher for refreshing the application context.
     *
     * @param refreshScope the refresh scope
     * @param refreshProperties the refresh properties
     * @return the context refresher bean
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.context.refresh.ContextRefresher")
    @ConditionalOnProperty(prefix = "cloud.config", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ContextRefresher contextRefresher(RefreshScope refreshScope, RefreshProperties refreshProperties) {
        log.info("Creating context refresher");
        return new LegacyContextRefresher(applicationContext, refreshScope, refreshProperties);
    }

    /**
     * Creates a RefreshEndpoint for refreshing the application context.
     *
     * @param contextRefresher the context refresher
     * @return the refresh endpoint bean
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.endpoint.RefreshEndpoint")
    @ConditionalOnProperty(prefix = "cloud.config", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RefreshEndpoint refreshEndpoint(ContextRefresher contextRefresher) {
        log.info("Enabling configuration refresh endpoint");
        return new RefreshEndpoint(contextRefresher);
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
    @org.springframework.cloud.context.config.annotation.RefreshScope
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
