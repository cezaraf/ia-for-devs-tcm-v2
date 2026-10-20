package com.cezar.dev.airquality.dtos;

public record WeatherResponse(double latitude, double longitude, String timezone, String observedAt,
                              Double temperature, Double apparentTemperature, Integer humidity, Double windSpeed,
                              Double windDirectionDegrees, String windDirection, Integer weatherCode,
                              String condition, Boolean isDay) {
}
