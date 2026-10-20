package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastApiResponse(
        Double latitude,
        Double longitude,
        String timezone,
        Current current) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            String time,
            @JsonProperty("temperature_2m") Double temperature,
            @JsonProperty("apparent_temperature") Double apparentTemperature,
            @JsonProperty("relative_humidity_2m") Double relativeHumidity,
            Double precipitation,
            @JsonProperty("weather_code") Integer weatherCode,
            @JsonProperty("wind_speed_10m") Double windSpeed,
            @JsonProperty("wind_direction_10m") Double windDirection,
            @JsonProperty("is_day") Integer isDay) {
    }
}
