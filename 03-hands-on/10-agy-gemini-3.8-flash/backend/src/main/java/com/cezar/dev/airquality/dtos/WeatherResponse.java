package com.cezar.dev.airquality.dtos;

public record WeatherResponse(
    Double latitude,
    Double longitude,
    String timezone,
    String observedAt,
    Double temperature,
    Double apparentTemperature,
    Integer relativeHumidity,
    Double precipitation,
    Double windSpeed,
    Integer windDirection,
    String windDirectionCardinal,
    Integer weatherCode,
    String condition,
    Boolean isDay
) {}
