package com.cezar.dev.airquality.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("open-meteo")
public record OpenMeteoProperties(
        String geocodingUrl,
        String forecastUrl,
        Duration connectTimeout,
        Duration readTimeout) {
}
