package com.catalis.common.core.config.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CloudConfigProperties}.
 */
@SpringBootTest(classes = {CloudConfigProperties.class})
@TestPropertySource(properties = {
        "cloud.config.enabled=true",
        "cloud.config.uri=http://test-config-server:8888",
        "cloud.config.name=test-service",
        "cloud.config.profile=test",
        "cloud.config.label=test-branch",
        "cloud.config.fail-fast=true",
        "cloud.config.timeout-ms=10000",
        "cloud.config.retry=true",
        "cloud.config.max-retries=10",
        "cloud.config.initial-retry-interval-ms=2000",
        "cloud.config.max-retry-interval-ms=5000",
        "cloud.config.retry-multiplier=2.0",
        "cloud.config.refresh-enabled=true"
})
public class CloudConfigPropertiesTest {

    @Autowired
    private CloudConfigProperties properties;

    @Test
    public void testPropertiesBinding() {
        assertTrue(properties.isEnabled());
        assertEquals("http://test-config-server:8888", properties.getUri());
        assertEquals("test-service", properties.getName());
        assertEquals("test", properties.getProfile());
        assertEquals("test-branch", properties.getLabel());
        assertTrue(properties.isFailFast());
        assertEquals(10000, properties.getTimeoutMs());
        assertTrue(properties.isRetry());
        assertEquals(10, properties.getMaxRetries());
        assertEquals(2000, properties.getInitialRetryIntervalMs());
        assertEquals(5000, properties.getMaxRetryIntervalMs());
        assertEquals(2.0, properties.getRetryMultiplier());
        assertTrue(properties.isRefreshEnabled());
    }

    @Test
    public void testToString() {
        String toString = properties.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("enabled=true"));
        assertTrue(toString.contains("uri=http://test-config-server:8888"));
    }
}
