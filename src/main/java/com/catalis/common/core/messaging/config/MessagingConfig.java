package com.catalis.common.core.messaging.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class for messaging components.
 * <p>
 * This class enables component scanning for the messaging package and enables
 * AspectJ auto-proxy for AOP support.
 */
@Configuration
@ComponentScan("com.catalis.common.core.messaging")
@EnableAspectJAutoProxy
public class MessagingConfig {
    // Configuration is handled through annotations
}
