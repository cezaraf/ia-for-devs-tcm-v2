package com.cezar.dev.airquality.dto;

import java.util.List;

/**
 * Payload devolvido ao frontend pelo endpoint GET /weather.
 *
 * @param place informações do lugar resolvido; o nome fica null quando a
 *              consulta foi feita por coordenadas (geolocation do navegador).
 */
public record WeatherResponse(Place place, Current current, List<Day> daily, Units units) {

    public record Place(String name, String admin1, String country, String countryCode,
            double latitude, double longitude, String timezone) {
    }

    public record Current(String observedAt, double temperature, double apparentTemperature, int humidity,
            boolean isDay, double precipitation, int weatherCode, int cloudCover,
            double windSpeed, int windDirection) {
    }

    public record Day(String date, double tempMax, double tempMin, Integer precipitationProbability,
            int weatherCode) {
    }

    public record Units(String temperature, String windSpeed, String precipitation) {
        public static final Units METRIC = new Units("°C", "km/h", "mm");
    }
}
