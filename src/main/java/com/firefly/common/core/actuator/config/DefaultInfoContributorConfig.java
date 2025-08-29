package com.firefly.common.core.actuator.config;

import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for default info contributors.
 * <p>
 * This class provides default info contributors that will be automatically
 * registered when the actuator dependency is present. It ensures that basic
 * application information is available through the info endpoint without
 * requiring additional configuration.
 */
@Configuration
@ConditionalOnClass(InfoEndpoint.class)
@AutoConfigureAfter(InfoContributorAutoConfiguration.class)
public class DefaultInfoContributorConfig {

    /**
     * Provides a default application info contributor if none is configured.
     * This ensures that basic application information is available by default.
     *
     * @param environment the Spring environment
     * @return the application info contributor
     */
    @Bean
    @ConditionalOnMissingBean(name = "applicationInfoContributor")
    public InfoContributor applicationInfoContributor(Environment environment) {
        return builder -> {
            Map<String, Object> details = new HashMap<>();
            
            // Add application name
            String applicationName = environment.getProperty("spring.application.name");
            if (applicationName != null) {
                details.put("name", applicationName);
            }
            
            // Add active profiles
            String activeProfiles = environment.getProperty("spring.profiles.active");
            if (activeProfiles != null) {
                details.put("profiles", activeProfiles);
            }
            
            // Add Java version
            details.put("java", Map.of(
                    "version", System.getProperty("java.version"),
                    "vendor", System.getProperty("java.vendor")
            ));
            
            // Add OS information
            details.put("os", Map.of(
                    "name", System.getProperty("os.name"),
                    "version", System.getProperty("os.version"),
                    "arch", System.getProperty("os.arch")
            ));
            
            builder.withDetail("application", details);
        };
    }
}
