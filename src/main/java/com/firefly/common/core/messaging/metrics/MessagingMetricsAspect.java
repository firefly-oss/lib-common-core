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


package com.firefly.common.core.messaging.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Aspect for tracking messaging operations metrics.
 * <p>
 * This aspect intercepts method calls to publishers and subscribers
 * and records metrics for message publishing and subscribing operations.
 */
@Aspect
public class MessagingMetricsAspect {

    private final MeterRegistry meterRegistry;

    /**
     * Creates a new MessagingMetricsAspect.
     *
     * @param meterRegistry the meter registry
     */
    public MessagingMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Pointcut for publisher methods.
     */
    @Pointcut("execution(* com.firefly.common.core.messaging.publisher.*.publish(..))")
    public void publisherMethods() {
    }

    /**
     * Pointcut for subscriber methods.
     */
    @Pointcut("execution(* com.firefly.common.core.messaging.subscriber.*.onMessage(..))")
    public void subscriberMethods() {
    }

    /**
     * Advice for tracking publisher metrics.
     *
     * @param joinPoint the join point
     * @return the result of the method call
     * @throws Throwable if an error occurs
     */
    @Around("publisherMethods()")
    public Object trackPublisherMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String publisherType = getPublisherType(joinPoint);
        String destination = getDestination(joinPoint);
        String eventType = getEventType(joinPoint);

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            recordMetrics("publish", publisherType, destination, eventType, sample, success);
        }
    }

    /**
     * Advice for tracking subscriber metrics.
     *
     * @param joinPoint the join point
     * @return the result of the method call
     * @throws Throwable if an error occurs
     */
    @Around("subscriberMethods()")
    public Object trackSubscriberMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String subscriberType = getSubscriberType(joinPoint);
        String destination = "unknown"; // Subscribers typically don't have a destination parameter
        String eventType = "unknown"; // Subscribers typically don't have an event type parameter

        Timer.Sample sample = Timer.start(meterRegistry);
        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            recordMetrics("subscribe", subscriberType, destination, eventType, sample, success);
        }
    }

    private String getPublisherType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("EventPublisher", "").toLowerCase();
    }

    private String getSubscriberType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("EventSubscriber", "").toLowerCase();
    }

    private String getDestination(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String) {
            String destination = (String) args[0];
            return StringUtils.hasText(destination) ? destination : "default";
        }
        return "unknown";
    }

    private String getEventType(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 1 && args[1] instanceof String) {
            String eventType = (String) args[1];
            return StringUtils.hasText(eventType) ? eventType : "default";
        }
        return "unknown";
    }

    private void recordMetrics(String operation, String systemType, String destination, String eventType, Timer.Sample sample, boolean success) {
        // Record timer metric
        sample.stop(Timer.builder("messaging." + operation + ".time")
                .tag("system", systemType)
                .tag("destination", destination)
                .tag("eventType", eventType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));

        // Record counter metric
        Counter.builder("messaging." + operation + ".count")
                .tag("system", systemType)
                .tag("destination", destination)
                .tag("eventType", eventType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }
}