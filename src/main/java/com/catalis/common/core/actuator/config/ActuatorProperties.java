package com.catalis.common.core.actuator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for Spring Boot Actuator.
 * <p>
 * This class provides configuration properties for customizing Spring Boot Actuator
 * behavior in the application. It allows for fine-grained control over which endpoints
 * are enabled and exposed, as well as configuration for health checks and metrics.
 */
@ConfigurationProperties(prefix = "management")
@Getter
@Setter
public class ActuatorProperties {

    /**
     * Endpoints configuration.
     */
    private final Endpoints endpoints = new Endpoints();

    /**
     * Metrics configuration.
     */
    private final Metrics metrics = new Metrics();

    /**
     * Tracing configuration.
     */
    private final Tracing tracing = new Tracing();

    /**
     * Health configuration.
     */
    private final Health health = new Health();

    /**
     * Endpoints configuration properties.
     */
    @Getter
    @Setter
    public static class Endpoints {
        /**
         * Whether to enable endpoints.
         */
        private boolean enabled = true;

        /**
         * Web exposure configuration.
         */
        private final Web web = new Web();

        /**
         * JMX exposure configuration.
         */
        private final Jmx jmx = new Jmx();

        /**
         * Web exposure configuration properties.
         */
        @Getter
        @Setter
        public static class Web {
            /**
             * Endpoints to expose. Use '*' for all endpoints.
             */
            private String exposure = "*";

            /**
             * Base path for endpoints.
             */
            private String basePath = "/actuator";

            /**
             * Whether to include details in responses.
             */
            private boolean includeDetails = true;
        }

        /**
         * JMX exposure configuration properties.
         */
        @Getter
        @Setter
        public static class Jmx {
            /**
             * Endpoints to expose. Use '*' for all endpoints.
             */
            private String exposure = "*";

            /**
             * Domain for JMX endpoints.
             */
            private String domain = "org.springframework.boot";
        }
    }

    /**
     * Metrics configuration properties.
     */
    @Getter
    @Setter
    public static class Metrics {
        /**
         * Whether to enable metrics.
         */
        private boolean enabled = true;

        /**
         * Tags to add to all metrics.
         */
        private final Map<String, String> tags = new HashMap<>();

        /**
         * Prometheus configuration.
         */
        private final Prometheus prometheus = new Prometheus();

        /**
         * Prometheus configuration properties.
         */
        @Getter
        @Setter
        public static class Prometheus {
            /**
             * Whether to enable Prometheus metrics.
             */
            private boolean enabled = true;

            /**
             * Path for Prometheus metrics endpoint.
             */
            private String path = "/actuator/prometheus";
        }
    }

    /**
     * Tracing configuration properties.
     */
    @Getter
    @Setter
    public static class Tracing {
        /**
         * Whether to enable tracing.
         */
        private boolean enabled = true;

        /**
         * Sampling configuration.
         */
        private final Sampling sampling = new Sampling();

        /**
         * Zipkin configuration.
         */
        private final Zipkin zipkin = new Zipkin();

        /**
         * Propagation configuration.
         */
        private final Propagation propagation = new Propagation();

        /**
         * Sampling configuration properties.
         */
        @Getter
        @Setter
        public static class Sampling {
            /**
             * Probability for sampling traces (0.0 - 1.0).
             */
            private double probability = 0.1;
        }

        /**
         * Zipkin configuration properties.
         */
        @Getter
        @Setter
        public static class Zipkin {
            /**
             * Whether to enable Zipkin tracing.
             */
            private boolean enabled = false;

            /**
             * Base URL for Zipkin server.
             */
            private String baseUrl = "http://localhost:9411";

            /**
             * Service name for Zipkin traces.
             */
            private String serviceName = "${spring.application.name:application}";
        }

        /**
         * Propagation configuration properties.
         */
        @Getter
        @Setter
        public static class Propagation {
            /**
             * Propagation types to use (B3, W3C, etc.).
             */
            private List<String> type = new ArrayList<>(List.of("B3", "W3C"));
        }
    }

    /**
     * Health configuration properties.
     */
    @Getter
    @Setter
    public static class Health {
        /**
         * Whether to show details in health endpoint.
         */
        private String showDetails = "always";

        /**
         * Whether to show components in health endpoint.
         */
        private boolean showComponents = true;

        /**
         * Disk space configuration.
         */
        private final DiskSpace diskSpace = new DiskSpace();

        /**
         * Disk space configuration properties.
         */
        @Getter
        @Setter
        public static class DiskSpace {
            /**
             * Whether to enable disk space health check.
             */
            private boolean enabled = true;

            /**
             * Threshold for disk space health check.
             */
            private String threshold = "10MB";

            /**
             * Path to check for disk space.
             */
            private String path = ".";
        }
    }
}
