package com.catalis.common.core.actuator.config;

import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import brave.baggage.CorrelationScopeConfig;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.catalis.common.core.config.TransactionFilter.TRANSACTION_ID_HEADER;

/**
 * Configuration class for distributed tracing.
 * <p>
 * This class provides configuration for distributed tracing using Micrometer Tracing
 * and Brave. It integrates with the existing transaction ID mechanism to provide
 * consistent tracing across services.
 * <p>
 * The configuration includes:
 * - Baggage propagation for transaction IDs
 * - Correlation of trace IDs with transaction IDs
 * - Sampling configuration
 * - MDC integration for logging
 */
@Configuration
@ConditionalOnClass(name = {"io.micrometer.tracing.Tracer", "brave.Tracing"})
@ConditionalOnProperty(prefix = "management.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

    /**
     * Configures the transaction ID field for baggage propagation.
     *
     * @return the transaction ID baggage field
     */
    @Bean
    public BaggageField transactionIdField() {
        return BaggageField.create(TRANSACTION_ID_HEADER);
    }

    /**
     * Configures the propagation factory for distributed tracing.
     *
     * @param transactionIdField the transaction ID baggage field
     * @return the propagation factory
     */
    @Bean
    public Propagation.Factory propagationFactory(BaggageField transactionIdField) {
        return BaggagePropagation.newFactoryBuilder(B3Propagation.FACTORY)
                .add(BaggagePropagationConfig.SingleBaggageField.newBuilder(transactionIdField)
                        .addKeyName(TRANSACTION_ID_HEADER)
                        .build())
                .build();
    }

    /**
     * Configures the current trace context for distributed tracing.
     *
     * @param transactionIdField the transaction ID baggage field
     * @return the current trace context
     */
    @Bean
    public ThreadLocalCurrentTraceContext currentTraceContext(BaggageField transactionIdField) {
        return ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator(MDCScopeDecorator.newBuilder()
                        .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(transactionIdField)
                                .flushOnUpdate()
                                .build())
                        .build())
                .build();
    }

    /**
     * Configures the sampler for distributed tracing.
     *
     * @param actuatorProperties the actuator properties
     * @return the sampler
     */
    @Bean
    public Sampler sampler(ActuatorProperties actuatorProperties) {
        return Sampler.create((float)actuatorProperties.getTracing().getSampling().getProbability());
    }

    /**
     * Configures the Brave tracer for distributed tracing.
     *
     * @param braveTracer the Brave tracer
     * @return the Micrometer tracer
     */
    @Bean
    @ConditionalOnClass(name = "brave.Tracer")
    public Tracer tracer(brave.Tracer braveTracer) {
        return new BraveTracer(braveTracer, new BraveCurrentTraceContext(currentTraceContext(transactionIdField())),
                new BraveBaggageManager());
    }
}
