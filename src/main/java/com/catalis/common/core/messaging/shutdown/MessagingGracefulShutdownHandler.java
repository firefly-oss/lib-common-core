package com.catalis.common.core.messaging.shutdown;

import com.catalis.common.core.messaging.publisher.ConnectionAwarePublisher;
import com.catalis.common.core.messaging.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handler for graceful shutdown of messaging systems.
 * <p>
 * This component ensures that all messaging connections are properly closed
 * when the application shuts down. It implements Spring's DisposableBean interface
 * to hook into the application shutdown process.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true")
public class MessagingGracefulShutdownHandler implements DisposableBean {

    private final ApplicationContext applicationContext;

    /**
     * Creates a new MessagingGracefulShutdownHandler.
     *
     * @param applicationContext the application context
     */
    public MessagingGracefulShutdownHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() throws Exception {
        log.info("Initiating graceful shutdown of messaging systems...");
        
        // Get all EventPublisher beans
        Map<String, EventPublisher> publishers = applicationContext.getBeansOfType(EventPublisher.class);
        
        if (publishers.isEmpty()) {
            log.info("No messaging publishers found to shut down");
            return;
        }
        
        log.info("Found {} messaging publishers to shut down", publishers.size());
        
        // Shutdown each publisher
        CompletableFuture<?>[] shutdownFutures = publishers.entrySet().stream()
                .map(entry -> {
                    String beanName = entry.getKey();
                    EventPublisher publisher = entry.getValue();
                    
                    return CompletableFuture.runAsync(() -> {
                        try {
                            log.debug("Shutting down publisher: {}", beanName);
                            
                            // If the publisher implements AutoCloseable, close it
                            if (publisher instanceof AutoCloseable) {
                                log.debug("Publisher {} is AutoCloseable, closing...", beanName);
                                ((AutoCloseable) publisher).close();
                            }
                            
                            // If the publisher is a ConnectionAwarePublisher, log the connection ID
                            if (publisher instanceof ConnectionAwarePublisher) {
                                log.debug("Publisher {} is ConnectionAwarePublisher with connection ID: {}", 
                                        beanName, ((ConnectionAwarePublisher) publisher).getConnectionId());
                            }
                            
                            log.debug("Successfully shut down publisher: {}", beanName);
                        } catch (Exception e) {
                            log.error("Error shutting down publisher {}: {}", beanName, e.getMessage(), e);
                        }
                    });
                })
                .toArray(CompletableFuture[]::new);
        
        // Wait for all shutdown operations to complete
        try {
            CompletableFuture.allOf(shutdownFutures).get(30, TimeUnit.SECONDS);
            log.info("All messaging publishers have been shut down successfully");
        } catch (Exception e) {
            log.error("Error during graceful shutdown of messaging publishers: {}", e.getMessage(), e);
        }
    }
}