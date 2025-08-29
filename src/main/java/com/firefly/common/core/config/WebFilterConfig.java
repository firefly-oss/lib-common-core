package com.firefly.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFilterConfig {
    @Bean
    public TransactionFilter transactionFilter() {
        return new TransactionFilter();
    }
}
