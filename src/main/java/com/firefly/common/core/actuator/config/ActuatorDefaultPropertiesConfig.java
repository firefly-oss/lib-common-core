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
@ConfigurationPropertiesScan("com.firefly.common.core.actuator.config")
public class ActuatorDefaultPropertiesConfig {
    // This class only loads the default properties
}
