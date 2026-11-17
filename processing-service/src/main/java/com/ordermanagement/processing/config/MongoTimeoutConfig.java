package com.ordermanagement.processing.config;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Bounds how long a MongoDB operation may wait. The driver's defaults let a request hang for 30 seconds
 * when the database is down; with these, the API answers 503 within a few seconds instead.
 * Set in code rather than in the URI, so they also apply when MONGODB_URI is overridden.
 */
@Configuration
public class MongoTimeoutConfig {

    private static final Duration SERVER_SELECTION_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoTimeouts() {
        return settings -> settings
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(
                        SERVER_SELECTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .readTimeout(READ_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    }
}
