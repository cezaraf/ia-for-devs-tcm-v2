package com.cezar.dev.airquality.dto;

public record WeatherDto(
        String city,
        String country,
        String admin1,
        double latitude,
        double longitude,
        double temperatureCelsius,
        int relativeHumidityPercent,
        double windSpeedKmh,
        int weatherCode,
        String weatherDescription,
        String observedAt
) {}
