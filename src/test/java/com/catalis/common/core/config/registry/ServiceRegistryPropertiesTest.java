package com.catalis.common.core.config.registry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ServiceRegistryProperties}.
 */
@SpringBootTest(classes = {ServiceRegistryProperties.class})
@TestPropertySource(properties = {
        "service.registry.enabled=true",
        "service.registry.type=EUREKA",
        "service.registry.eureka.service-url=http://test-eureka:8761/eureka/",
        "service.registry.eureka.register=true",
        "service.registry.eureka.fetch-registry=true",
        "service.registry.eureka.registry-fetch-interval-seconds=60",
        "service.registry.eureka.instance-id=test-instance",
        "service.registry.eureka.prefer-ip-address=true",
        "service.registry.eureka.lease-renewal-interval-in-seconds=45",
        "service.registry.eureka.lease-expiration-duration-in-seconds=120",
        "service.registry.eureka.health-check-enabled=true",
        "service.registry.eureka.health-check-url-path=/actuator/health",
        "service.registry.eureka.status-page-url-path=/actuator/info",
        "service.registry.consul.host=test-consul",
        "service.registry.consul.port=8501",
        "service.registry.consul.register=true",
        "service.registry.consul.deregister=true",
        "service.registry.consul.service-name=test-service",
        "service.registry.consul.instance-id=test-instance",
        "service.registry.consul.health-check-interval=20",
        "service.registry.consul.health-check-timeout=10",
        "service.registry.consul.health-check-path=/actuator/health",
        "service.registry.consul.health-check-enabled=true",
        "service.registry.consul.catalog-services-watch=true",
        "service.registry.consul.catalog-services-watch-timeout=20",
        "service.registry.consul.catalog-services-watch-delay=2000"
})
public class ServiceRegistryPropertiesTest {

    @Autowired
    private ServiceRegistryProperties properties;

    @Test
    public void testPropertiesBinding() {
        assertTrue(properties.isEnabled());
        assertEquals(ServiceRegistryProperties.RegistryType.EUREKA, properties.getType());
        
        // Test Eureka properties
        assertEquals("http://test-eureka:8761/eureka/", properties.getEureka().getServiceUrl());
        assertTrue(properties.getEureka().isRegister());
        assertTrue(properties.getEureka().isFetchRegistry());
        assertEquals(60, properties.getEureka().getRegistryFetchIntervalSeconds());
        assertEquals("test-instance", properties.getEureka().getInstanceId());
        assertTrue(properties.getEureka().isPreferIpAddress());
        assertEquals(45, properties.getEureka().getLeaseRenewalIntervalInSeconds());
        assertEquals(120, properties.getEureka().getLeaseExpirationDurationInSeconds());
        assertTrue(properties.getEureka().isHealthCheckEnabled());
        assertEquals("/actuator/health", properties.getEureka().getHealthCheckUrlPath());
        assertEquals("/actuator/info", properties.getEureka().getStatusPageUrlPath());
        
        // Test Consul properties
        assertEquals("test-consul", properties.getConsul().getHost());
        assertEquals(8501, properties.getConsul().getPort());
        assertTrue(properties.getConsul().isRegister());
        assertTrue(properties.getConsul().isDeregister());
        assertEquals("test-service", properties.getConsul().getServiceName());
        assertEquals("test-instance", properties.getConsul().getInstanceId());
        assertEquals(20, properties.getConsul().getHealthCheckInterval());
        assertEquals(10, properties.getConsul().getHealthCheckTimeout());
        assertEquals("/actuator/health", properties.getConsul().getHealthCheckPath());
        assertTrue(properties.getConsul().isHealthCheckEnabled());
        assertTrue(properties.getConsul().isCatalogServicesWatch());
        assertEquals(20, properties.getConsul().getCatalogServicesWatchTimeout());
        assertEquals(2000, properties.getConsul().getCatalogServicesWatchDelay());
    }

    @Test
    public void testToString() {
        String toString = properties.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("enabled=true"));
        assertTrue(toString.contains("type=EUREKA"));
    }
}
