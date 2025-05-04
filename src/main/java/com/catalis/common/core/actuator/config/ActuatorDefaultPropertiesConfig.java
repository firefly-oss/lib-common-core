package com.catalis.common.core.actuator.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class that loads default actuator properties.
 * <p>
 * This class ensures that the default actuator configuration is loaded
 * automatically when the library is included as a dependency. It loads
 * the application-actuator-default.yml file which contains sensible defaults
 * for actuator endpoints, health checks, and metrics.
 */
@Configuration
@PropertySource(value = "classpath:application-actuator-default.yml", factory = YamlPropertySourceFactory.class)
@ConfigurationPropertiesScan("com.catalis.common.core.actuator.config")
public class ActuatorDefaultPropertiesConfig {
    // This class only loads the default properties
}
