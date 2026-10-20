package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastApiResponse(String timezone, Current current) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(String time,
                          @JsonProperty("temperature_2m") Double temperature2m,
                          @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
                          @JsonProperty("apparent_temperature") Double apparentTemperature,
                          @JsonProperty("is_day") Integer isDay,
                          @JsonProperty("weather_code") Integer weatherCode,
                          @JsonProperty("wind_speed_10m") Double windSpeed10m,
                          @JsonProperty("wind_direction_10m") Double windDirection10m) {
    }
}
