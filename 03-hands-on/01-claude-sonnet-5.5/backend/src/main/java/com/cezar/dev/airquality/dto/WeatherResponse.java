package com.cezar.dev.airquality.dto;

/** Clima atual devolvido ao frontend (já normalizado, sem o formato da Open-Meteo). */
public record WeatherResponse(
        Location location,
        Current current,
        Units units) {

    public record Location(String name, String region, String country, double latitude, double longitude,
                           String timezone) {
    }

    public record Current(
            String time,
            double temperature,
            double apparentTemperature,
            int humidity,
            double precipitation,
            double windSpeed,
            int windDirection,
            boolean day,
            int weatherCode,
            String description,
            String icon) {
    }

    public record Units(String temperature, String humidity, String precipitation, String windSpeed) {
    }
}
