package com.bhuvana.nursery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Injected wherever clock-dependent behaviour must be testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
