package com.catalis.common.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties class for WebClient.
 *
 * This class is designed to hold properties related to the
 * configuration of WebClient functionality. It is annotated with
 * `@ConfigurationProperties` to enable externalized configuration
 * using a specified prefix (`webclient`). The properties can be
 * defined in external configuration files such as application.yml
 * or application.properties.
 *
 * The `skipHeaders` property represents a list of HTTP header names
 * that should be excluded or ignored during WebClient requests.
 *
 * Annotations:
 * - `@Configuration`: Marks this class as a source of bean definitions.
 * - `@ConfigurationProperties`: Indicates the property prefix (`webclient`)
 *   for externalized configuration mapping.
 * - `@Getter`, `@Setter`: Lombok annotations to automatically generate
 *   getter and setter methods for the fields.
 */
@Configuration
@ConfigurationProperties(prefix = "webclient")
@Getter
@Setter
public class WebClientProperties {
    private List<String> skipHeaders;
}
