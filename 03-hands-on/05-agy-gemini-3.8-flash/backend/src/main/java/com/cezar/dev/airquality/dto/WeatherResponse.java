package com.cezar.dev.airquality.dto;

import java.util.List;

public record WeatherResponse(
    LocationInfo location,
    CurrentWeatherInfo current,
    List<DailyForecastInfo> dailyForecast
) {
    public record LocationInfo(
        String name,
        String state,
        String country,
        Double latitude,
        Double longitude,
        String timezone
    ) {}

    public record CurrentWeatherInfo(
        Double temperature,
        Double apparentTemperature,
        Integer humidity,
        Double precipitation,
        Double windSpeed,
        Integer windDirection,
        Integer cloudCover,
        Integer weatherCode,
        String condition,
        String icon,
        Boolean isDay,
        String observedAt
    ) {}

    public record DailyForecastInfo(
        String date,
        Double maxTemp,
        Double minTemp,
        Integer weatherCode,
        String condition,
        String icon
    ) {}
}
