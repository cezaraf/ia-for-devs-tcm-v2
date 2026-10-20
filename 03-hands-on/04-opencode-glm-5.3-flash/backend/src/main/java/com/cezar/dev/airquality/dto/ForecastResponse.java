package com.cezar.dev.airquality.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta da Weather (Forecast) API do Open-Meteo
 * (https://api.open-meteo.com/v1/forecast).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastResponse(@JsonProperty("current") Current current,
        @JsonProperty("daily") Daily daily) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            @JsonProperty("time") String time,
            @JsonProperty("temperature_2m") double temperature,
            @JsonProperty("apparent_temperature") double apparentTemperature,
            @JsonProperty("relative_humidity_2m") int humidity,
            @JsonProperty("is_day") boolean isDay,
            @JsonProperty("precipitation") double precipitation,
            @JsonProperty("weather_code") int weatherCode,
            @JsonProperty("cloud_cover") int cloudCover,
            @JsonProperty("wind_speed_10m") double windSpeed,
            @JsonProperty("wind_direction_10m") int windDirection) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
            @JsonProperty("time") List<String> time,
            @JsonProperty("temperature_2m_max") List<Double> temperatureMax,
            @JsonProperty("temperature_2m_min") List<Double> temperatureMin,
            @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbabilityMax,
            @JsonProperty("weather_code") List<Integer> weatherCode) {
    }
}
