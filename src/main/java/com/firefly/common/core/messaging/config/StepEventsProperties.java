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

package com.firefly.common.core.messaging.config;

import com.firefly.common.core.messaging.annotation.PublisherType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Step Events integration in lib-common-core.
 * 
 * Step Events use the lib-common-core messaging infrastructure for publishing via the bridge pattern.
 * This properties class controls step events configuration and which messaging system to use.
 */
@ConfigurationProperties(prefix = "firefly.stepevents")
@Data
public class StepEventsProperties {

    /**
     * Whether Step Events are enabled. When enabled, Step Events will use the lib-common-core 
     * messaging infrastructure for publishing via the StepEventPublisherBridge.
     */
    private boolean enabled = true;

    /**
     * The default topic to use for publishing step events when no topic is specified.
     */
    private String defaultTopic = "step-events";

    /**
     * The type of publisher to use for step events. Defaults to EVENT_BUS for development.
     */
    private PublisherType publisherType = PublisherType.EVENT_BUS;

    /**
     * The connection ID to use for the publisher. Defaults to "default".
     */
    private String connectionId = "default";
}