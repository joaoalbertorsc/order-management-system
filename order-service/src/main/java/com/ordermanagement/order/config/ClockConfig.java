package com.ordermanagement.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** A single injectable clock keeps timestamps in UTC and lets tests freeze time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
