package com.catalis.common.core.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoggingUtilsTest {

    @BeforeEach
    void setUp() {
        // Clear MDC before each test
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear MDC after each test
        MDC.clear();
    }

    @Test
    void structuredLogWithMessageAndData() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 123);

        Map<String, Object> result = LoggingUtils.structuredLog("Test message", data);

        assertEquals("Test message", result.get("message"));
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
    }

    @Test
    void structuredLogWithMessageAndSingleField() {
        Map<String, Object> result = LoggingUtils.structuredLog("Test message", "key1", "value1");

        assertEquals("Test message", result.get("message"));
        assertEquals("value1", result.get("key1"));
    }

    @Test
    void withMdcSingleValue() {
        String result = LoggingUtils.withMdc("testKey", "testValue", () -> {
            assertEquals("testValue", MDC.get("testKey"));
            return "success";
        });

        assertEquals("success", result);
        assertNull(MDC.get("testKey"), "MDC should be cleared after execution");
    }

    @Test
    void withMdcMultipleValues() {
        Map<String, String> context = new HashMap<>();
        context.put("key1", "value1");
        context.put("key2", "value2");

        String result = LoggingUtils.withMdc(context, () -> {
            assertEquals("value1", MDC.get("key1"));
            assertEquals("value2", MDC.get("key2"));
            return "success";
        });

        assertEquals("success", result);
        assertNull(MDC.get("key1"), "MDC should be cleared after execution");
        assertNull(MDC.get("key2"), "MDC should be cleared after execution");
    }

    @Test
    void withMdcPreservesExistingValues() {
        // Set an existing MDC value
        MDC.put("existingKey", "existingValue");

        Map<String, String> context = new HashMap<>();
        context.put("key1", "value1");
        context.put("existingKey", "newValue"); // Override existing value

        LoggingUtils.withMdc(context, () -> {
            assertEquals("value1", MDC.get("key1"));
            assertEquals("newValue", MDC.get("existingKey"));
            return null;
        });

        // Verify the existing value is restored
        assertEquals("existingValue", MDC.get("existingKey"));
        assertNull(MDC.get("key1"), "Temporary key should be removed");
    }

    @Test
    void setUserIdAddsToMdc() {
        LoggingUtils.setUserId("user123");
        assertEquals("user123", MDC.get("userId"));
    }

    @Test
    void setRequestIdAddsToMdc() {
        LoggingUtils.setRequestId("req123");
        assertEquals("req123", MDC.get("requestId"));
    }

    @Test
    void setCorrelationIdAddsToMdc() {
        LoggingUtils.setCorrelationId("corr123");
        assertEquals("corr123", MDC.get("correlationId"));
    }

    @Test
    void structuredLogBuilderCreatesCorrectStructure() {
        Map<String, Object> result = LoggingUtils.log("Test message")
                .with("key1", "value1")
                .with("key2", 123)
                .build();

        assertEquals("Test message", result.get("message"));
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
    }

    @Test
    void structuredLogBuilderWithMapAddsAllEntries() {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("key1", "value1");
        additionalData.put("key2", 123);

        Map<String, Object> result = LoggingUtils.log("Test message")
                .with(additionalData)
                .with("key3", "value3")
                .build();

        assertEquals("Test message", result.get("message"));
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
        assertEquals("value3", result.get("key3"));
    }
}
