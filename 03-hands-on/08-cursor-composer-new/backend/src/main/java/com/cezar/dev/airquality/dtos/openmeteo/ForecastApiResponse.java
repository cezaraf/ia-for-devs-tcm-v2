package com.cezar.dev.airquality.dtos.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastApiResponse(
        double latitude,
        double longitude,
        String timezone,
        CurrentWeather current) {
}
