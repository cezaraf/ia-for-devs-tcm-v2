package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoForecastResponse(
    Double latitude,
    Double longitude,
    String timezone,
    OpenMeteoCurrent current
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoCurrent(
        String time,
        @JsonProperty("temperature_2m") Double temperature2m,
        @JsonProperty("apparent_temperature") Double apparentTemperature,
        @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
        Double precipitation,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("wind_speed_10m") Double windSpeed10m,
        @JsonProperty("wind_direction_10m") Integer windDirection10m,
        @JsonProperty("is_day") Integer isDay
    ) {}
}
