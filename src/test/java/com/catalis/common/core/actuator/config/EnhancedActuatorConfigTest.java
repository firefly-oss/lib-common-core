package com.catalis.common.core.actuator.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for the enhanced actuator configurations.
 * Tests that all new actuator configurations load correctly and provide expected functionality.
 */
class EnhancedActuatorConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withUserConfiguration(
                    ActuatorConfig.class,
                    JvmMetricsConfig.class,
                    DatabaseHealthConfig.class,
                    ThreadPoolMetricsConfig.class,
                    HttpClientMetricsConfig.class,
                    CacheHealthConfig.class,
                    ApplicationMetricsConfig.class
            );

    @Test
    void shouldLoadJvmMetricsConfig() {
        contextRunner
                .withPropertyValues("management.metrics.jvm.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(JvmMetricsConfig.class);
                    
                    // Verify JVM metrics beans are present
                    assertThat(context).hasBean("jvmMemoryMetrics");
                    assertThat(context).hasBean("jvmGcMetrics");
                    assertThat(context).hasBean("jvmThreadMetrics");
                    assertThat(context).hasBean("classLoaderMetrics");
                    assertThat(context).hasBean("processorMetrics");
                    assertThat(context).hasBean("uptimeMetrics");
                });
    }

    @Test
    void shouldLoadDatabaseHealthConfig() {
        contextRunner
                .withPropertyValues("management.health.database.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DatabaseHealthConfig.class);
                    
                    // Verify database health indicator is present
                    assertThat(context).hasBean("databaseHealthIndicator");
                    assertThat(context.getBean("databaseHealthIndicator")).isInstanceOf(HealthIndicator.class);
                    
                    // Test health indicator
                    HealthIndicator healthIndicator = (HealthIndicator) context.getBean("databaseHealthIndicator");
                    assertThat(healthIndicator.health()).isNotNull();
                });
    }

    @Test
    void shouldLoadThreadPoolMetricsConfig() {
        contextRunner
                .withPropertyValues("management.metrics.threadpool.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ThreadPoolMetricsConfig.class);
                    
                    // Verify thread pool health indicator is present
                    assertThat(context).hasBean("threadPoolHealthIndicator");
                    assertThat(context.getBean("threadPoolHealthIndicator")).isInstanceOf(HealthIndicator.class);
                    
                    // Test health indicator
                    HealthIndicator healthIndicator = (HealthIndicator) context.getBean("threadPoolHealthIndicator");
                    assertThat(healthIndicator.health()).isNotNull();
                });
    }

    @Test
    void shouldLoadHttpClientMetricsConfig() {
        contextRunner
                .withPropertyValues("management.metrics.http.client.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpClientMetricsConfig.class);
                    
                    // Verify HTTP client components are present
                    assertThat(context).hasBean("httpClientHealthIndicator");
                    assertThat(context).hasBean("restTemplateMetricsCustomizer");
                    
                    assertThat(context.getBean("httpClientHealthIndicator")).isInstanceOf(HealthIndicator.class);
                    
                    // Test health indicator
                    HealthIndicator healthIndicator = (HealthIndicator) context.getBean("httpClientHealthIndicator");
                    assertThat(healthIndicator.health()).isNotNull();
                });
    }

    @Test
    void shouldLoadCacheHealthConfig() {
        contextRunner
                .withPropertyValues("management.health.cache.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheHealthConfig.class);
                    
                    // Verify cache health indicator is present
                    assertThat(context).hasBean("cacheHealthIndicator");
                    assertThat(context.getBean("cacheHealthIndicator")).isInstanceOf(HealthIndicator.class);
                    
                    // Test health indicator
                    HealthIndicator healthIndicator = (HealthIndicator) context.getBean("cacheHealthIndicator");
                    assertThat(healthIndicator.health()).isNotNull();
                });
    }

    @Test
    void shouldLoadApplicationMetricsConfig() {
        contextRunner
                .withPropertyValues("management.metrics.application.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationMetricsConfig.class);
                    
                    // Verify application metrics components are present
                    assertThat(context).hasBean("applicationStartedListener");
                    assertThat(context).hasBean("contextRefreshedListener");
                    assertThat(context).hasBean("applicationReadyListener");
                    assertThat(context).hasBean("enhancedApplicationInfoContributor");
                });
    }

    @Test
    void shouldLoadActuatorPropertiesCorrectly() {
        contextRunner
                .withPropertyValues(
                        "management.endpoints.web.exposure=*",
                        "management.metrics.jvm.enabled=true",
                        "management.health.database.enabled=true",
                        "management.metrics.threadpool.enabled=true",
                        "management.metrics.http.client.enabled=true",
                        "management.health.cache.enabled=true",
                        "management.metrics.application.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ActuatorProperties.class);
                    
                    ActuatorProperties properties = context.getBean(ActuatorProperties.class);
                    assertThat(properties).isNotNull();
                    assertThat(properties.getExtendedMetrics()).isNotNull();
                    assertThat(properties.getExtendedMetrics().getJvm().isEnabled()).isTrue();
                    assertThat(properties.getExtendedMetrics().getDatabase().isEnabled()).isTrue();
                    assertThat(properties.getExtendedMetrics().getThreadpool().isEnabled()).isTrue();
                    assertThat(properties.getExtendedMetrics().getHttpClient().isEnabled()).isTrue();
                    assertThat(properties.getExtendedMetrics().getCache().isEnabled()).isTrue();
                    assertThat(properties.getExtendedMetrics().getApplication().isEnabled()).isTrue();
                });
    }

    @Test
    void shouldDisableConfigurationsWhenPropertiesSetToFalse() {
        contextRunner
                .withPropertyValues(
                        "management.metrics.jvm.enabled=false",
                        "management.health.database.enabled=false",
                        "management.metrics.threadpool.enabled=false",
                        "management.metrics.http.client.enabled=false",
                        "management.health.cache.enabled=false",
                        "management.metrics.application.enabled=false"
                )
                .run(context -> {
                    // Verify configurations are not loaded when disabled
                    assertThat(context).doesNotHaveBean(JvmMetricsConfig.class);
                    assertThat(context).doesNotHaveBean(DatabaseHealthConfig.class);
                    assertThat(context).doesNotHaveBean(ThreadPoolMetricsConfig.class);
                    assertThat(context).doesNotHaveBean(HttpClientMetricsConfig.class);
                    assertThat(context).doesNotHaveBean(CacheHealthConfig.class);
                    assertThat(context).doesNotHaveBean(ApplicationMetricsConfig.class);
                });
    }

    @Test
    void shouldWorkWithoutOptionalDependencies() {
        // Test that configurations work even when optional beans are not available
        new ApplicationContextRunner()
                .withUserConfiguration(
                        JvmMetricsConfig.class,
                        ThreadPoolMetricsConfig.class,
                        HttpClientMetricsConfig.class,
                        CacheHealthConfig.class,
                        ApplicationMetricsConfig.class
                )
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    // Should not fail even without DataSource, RestTemplate, etc.
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    void shouldIntegrateWithMicrometer() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(MeterRegistry.class);
                    
                    MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
                    assertThat(meterRegistry).isNotNull();
                    
                    // Verify registry is usable
                    assertThat(meterRegistry.getMeters()).isNotNull();
                });
    }

    @TestConfiguration
    static class TestConfig {
        
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
        
        
        @Bean
        public DataSource dataSource() throws Exception {
            // Mock DataSource for testing
            DataSource dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(5)).thenReturn(true);
            return dataSource;
        }
        
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
        
        @Bean
        public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(5);
            executor.setQueueCapacity(10);
            executor.setThreadNamePrefix("test-");
            executor.initialize();
            return executor;
        }
        
        @Bean
        public ExecutorService executorService() {
            return Executors.newFixedThreadPool(3);
        }
        
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("testCache");
        }
    }
}