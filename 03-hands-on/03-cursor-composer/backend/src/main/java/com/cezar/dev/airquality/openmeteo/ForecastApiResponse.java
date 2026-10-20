package com.cezar.dev.airquality.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastApiResponse(
        double latitude,
        double longitude,
        String timezone,
        Current current,
        CurrentUnits current_units
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            String time,
            double temperature_2m,
            int relative_humidity_2m,
            int weather_code,
            double wind_speed_10m
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentUnits(
            String temperature_2m,
            String relative_humidity_2m,
            String wind_speed_10m
    ) {}
}
