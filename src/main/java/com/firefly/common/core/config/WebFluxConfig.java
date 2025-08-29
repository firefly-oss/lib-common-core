package com.firefly.common.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configuration class for customizing WebFlux settings.
 * Implements the {@link WebFluxConfigurer} interface to provide
 * specific configuration for WebFlux applications.
 *
 * This class is responsible for configuring HTTP message codecs,
 * specifically for customizing JSON serialization and deserialization support.
 * It registers a Jackson-based encoder and decoder with specific settings.
 *
 * Key aspects of this configuration include:
 * - The use of the {@link JavaTimeModule} to handle Java 8 date and time types.
 * - Disabling the serialization of dates as timestamps by configuring the Jackson {@link ObjectMapper}.
 * - Setting the default media type for JSON encoding and decoding to `application/json`.
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        configurer.defaultCodecs().jackson2JsonEncoder(
                new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));

        configurer.defaultCodecs().jackson2JsonDecoder(
                new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .exposedHeaders("X-Transaction-Id")
                .allowedHeaders("*");
    }
}