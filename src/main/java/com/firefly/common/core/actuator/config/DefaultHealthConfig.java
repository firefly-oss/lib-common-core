/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.common.core.actuator.config;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.io.File;

/**
 * Configuration class for default health indicators.
 * <p>
 * This class provides default health indicators that will be automatically
 * registered when the actuator dependency is present. It ensures that basic
 * health checks are available without requiring additional configuration.
 */
@Configuration
@ConditionalOnClass(HealthEndpoint.class)
@AutoConfigureBefore(HealthEndpointAutoConfiguration.class)
@AutoConfigureAfter(HealthContributorAutoConfiguration.class)
public class DefaultHealthConfig {

    private final ActuatorProperties actuatorProperties;

    /**
     * Constructor for DefaultHealthConfig.
     *
     * @param actuatorProperties the actuator properties
     */
    public DefaultHealthConfig(ActuatorProperties actuatorProperties) {
        this.actuatorProperties = actuatorProperties;
    }

    /**
     * Provides a default disk space health indicator if none is configured.
     * This ensures that disk space health check is available by default.
     *
     * @return the disk space health indicator
     */
    @Bean
    @ConditionalOnMissingBean(name = "diskSpaceHealthIndicator")
    public DiskSpaceHealthIndicator diskSpaceHealthIndicator() {
        ActuatorProperties.Health.DiskSpace diskSpace = actuatorProperties.getHealth().getDiskSpace();
        DataSize threshold = DataSize.parse(diskSpace.getThreshold());
        return new DiskSpaceHealthIndicator(new File(diskSpace.getPath()), threshold);
    }


}
