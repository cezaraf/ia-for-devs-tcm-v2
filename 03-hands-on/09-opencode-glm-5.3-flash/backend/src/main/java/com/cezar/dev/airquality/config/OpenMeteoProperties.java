package com.cezar.dev.airquality.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs e timeouts da Open-Meteo, definidos em application.properties.
 */
@ConfigurationProperties(prefix = "open-meteo")
public record OpenMeteoProperties(
        String geocodingUrl,
        String forecastUrl,
        Duration connectTimeout,
        Duration readTimeout) {
}
