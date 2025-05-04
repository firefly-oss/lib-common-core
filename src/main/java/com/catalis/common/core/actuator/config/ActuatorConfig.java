package com.catalis.common.core.actuator.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration class for Spring Boot Actuator.
 * <p>
 * This class provides configuration for Spring Boot Actuator endpoints and metrics.
 * It is automatically enabled when the actuator dependency is present, without requiring
 * any additional configuration from the user.
 * <p>
 * The configuration includes:
 * - Custom meter registry configuration for application-specific metrics
 * - Health indicators for various components
 * - Default actuator endpoint configuration
 * - Integration with the transaction ID mechanism for distributed tracing
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration")
@EnableConfigurationProperties(ActuatorProperties.class)
@AutoConfigureBefore(ManagementContextAutoConfiguration.class)
public class ActuatorConfig {

    /**
     * Customizes the meter registry with application-specific tags.
     *
     * @param environment the Spring environment
     * @return a customizer for the meter registry
     */
    private final ActuatorProperties actuatorProperties;

    /**
     * Constructor for ActuatorConfig.
     *
     * @param actuatorProperties the actuator properties
     */
    public ActuatorConfig(ActuatorProperties actuatorProperties) {
        this.actuatorProperties = actuatorProperties;
    }

    /**
     * Customizes the meter registry with application-specific tags.
     *
     * @param environment the Spring environment
     * @return a customizer for the meter registry
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        return registry -> {
            String applicationName = environment.getProperty("spring.application.name", "application");
            registry.config().commonTags(
                    "application", applicationName
            );

            // Add environment tag if available
            String activeProfile = environment.getProperty("spring.profiles.active");
            if (activeProfile != null) {
                registry.config().commonTags("environment", activeProfile);
            }
        };
    }

    /**
     * Provides default actuator endpoint configuration.
     * This ensures that basic endpoints are enabled by default.
     *
     * @return the default actuator properties
     */
    @Bean
    @ConditionalOnMissingBean(name = "defaultActuatorEndpointConfiguration")
    public ActuatorProperties.Endpoints defaultActuatorEndpointConfiguration() {
        ActuatorProperties.Endpoints endpoints = actuatorProperties.getEndpoints();

        // Ensure web exposure is configured
        if (endpoints.getWeb().getExposure() == null || endpoints.getWeb().getExposure().isEmpty()) {
            // By default, expose health, info, and prometheus endpoints
            endpoints.getWeb().setExposure("health,info,prometheus,metrics");
        }

        return endpoints;
    }
}
