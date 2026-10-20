package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherResponse(
        double latitude,
        double longitude,
        String timezone,
        String observedAt,
        double temperature,
        double apparentTemperature,
        int relativeHumidity,
        double precipitation,
        double windSpeed,
        int windDirection,
        String windDirectionCardinal,
        int weatherCode,
        String condition,
        @JsonProperty("isDay") boolean isDay) {
}
