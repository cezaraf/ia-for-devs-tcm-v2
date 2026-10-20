package com.cezar.dev.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoWeatherResponse(
    Double latitude,
    Double longitude,
    String timezone,
    @JsonProperty("current_units") CurrentUnits currentUnits,
    CurrentWeather current,
    DailyWeather daily
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentUnits(
        @JsonProperty("temperature_2m") String temperature2m,
        @JsonProperty("relative_humidity_2m") String relativeHumidity2m,
        @JsonProperty("apparent_temperature") String apparentTemperature,
        String precipitation,
        @JsonProperty("wind_speed_10m") String windSpeed10m
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentWeather(
        String time,
        @JsonProperty("temperature_2m") Double temperature2m,
        @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
        @JsonProperty("apparent_temperature") Double apparentTemperature,
        @JsonProperty("is_day") Integer isDay,
        Double precipitation,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("cloud_cover") Integer cloudCover,
        @JsonProperty("wind_speed_10m") Double windSpeed10m,
        @JsonProperty("wind_direction_10m") Integer windDirection10m
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DailyWeather(
        List<String> time,
        @JsonProperty("weather_code") List<Integer> weatherCode,
        @JsonProperty("temperature_2m_max") List<Double> temperature2mMax,
        @JsonProperty("temperature_2m_min") List<Double> temperature2mMin
    ) {}
}
