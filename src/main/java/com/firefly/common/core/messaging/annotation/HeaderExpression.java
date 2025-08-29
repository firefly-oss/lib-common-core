package com.firefly.common.core.messaging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a custom header expression for message publishing.
 * <p>
 * This annotation is used within the {@link PublishResult} annotation to define
 * custom headers that should be included in the published message.
 * <p>
 * The expression is evaluated using Spring Expression Language (SpEL) and can
 * reference the method's return value using the variable 'result' and method
 * arguments using 'args[index]'.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @PublishResult(
 *     destination = "user-events",
 *     eventType = "user.created",
 *     publisher = PublisherType.KAFKA,
 *     headerExpressions = {
 *         @HeaderExpression(name = "X-Source-Service", expression = "'user-service'"),
 *         @HeaderExpression(name = "X-User-Id", expression = "result.id")
 *     }
 * )
 * public User createUser(UserRequest request) {
 *     // Method implementation
 *     return user;
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HeaderExpression {

    /**
     * The name of the header.
     *
     * @return the header name
     */
    String name();

    /**
     * The SpEL expression to evaluate for the header value.
     * <p>
     * The expression can reference the method's return value using the variable 'result'
     * and method arguments using 'args[index]'.
     * <p>
     * Example: "result.id" or "'constant-value'" or "T(java.time.Instant).now().toString()"
     *
     * @return the expression
     */
    String expression();
}
