package com.cezar.dev.airquality.services;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.ForecastApiResponse.Current;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

@Service
public class WeatherService {

    private static final String UNKNOWN_CONDITION = "Condição desconhecida";

    private static final String[] CARDINALS = { "N", "NE", "E", "SE", "S", "SO", "O", "NO" };

    private static final Map<Integer, String> CONDITIONS = Map.ofEntries(
            Map.entry(0, "Céu limpo"),
            Map.entry(1, "Predominantemente limpo"),
            Map.entry(2, "Parcialmente nublado"),
            Map.entry(3, "Nublado"),
            Map.entry(45, "Nevoeiro"),
            Map.entry(48, "Nevoeiro com geada"),
            Map.entry(51, "Garoa fraca"),
            Map.entry(53, "Garoa moderada"),
            Map.entry(55, "Garoa intensa"),
            Map.entry(56, "Garoa congelante fraca"),
            Map.entry(57, "Garoa congelante intensa"),
            Map.entry(61, "Chuva fraca"),
            Map.entry(63, "Chuva moderada"),
            Map.entry(65, "Chuva forte"),
            Map.entry(66, "Chuva congelante fraca"),
            Map.entry(67, "Chuva congelante forte"),
            Map.entry(71, "Neve fraca"),
            Map.entry(73, "Neve moderada"),
            Map.entry(75, "Neve forte"),
            Map.entry(77, "Grãos de neve"),
            Map.entry(80, "Pancadas de chuva fracas"),
            Map.entry(81, "Pancadas de chuva moderadas"),
            Map.entry(82, "Pancadas de chuva violentas"),
            Map.entry(85, "Pancadas de neve fracas"),
            Map.entry(86, "Pancadas de neve fortes"),
            Map.entry(95, "Trovoada"),
            Map.entry(96, "Trovoada com granizo fraco"),
            Map.entry(99, "Trovoada com granizo forte"));

    private final OpenMeteoClient client;

    public WeatherService(OpenMeteoClient client) {
        this.client = client;
    }

    public WeatherResponse current(String latitudeText, String longitudeText) {
        double latitude = parseCoordinate(latitudeText, 90);
        double longitude = parseCoordinate(longitudeText, 180);

        ForecastApiResponse forecast = client.fetchCurrentWeather(latitude, longitude);
        Current current = forecast.current();
        if (forecast.latitude() == null || forecast.longitude() == null || forecast.timezone() == null
                || current.time() == null || current.temperature() == null
                || current.apparentTemperature() == null || current.relativeHumidity() == null
                || current.precipitation() == null || current.weatherCode() == null
                || current.windSpeed() == null || current.windDirection() == null
                || current.isDay() == null) {
            throw new UpstreamException("Resposta da Open-Meteo incompleta.");
        }

        int windDirection = (int) Math.round(current.windDirection());
        return new WeatherResponse(
                forecast.latitude(),
                forecast.longitude(),
                forecast.timezone(),
                current.time(),
                current.temperature(),
                current.apparentTemperature(),
                (int) Math.round(current.relativeHumidity()),
                current.precipitation(),
                current.windSpeed(),
                windDirection,
                toCardinal(windDirection),
                current.weatherCode(),
                toCondition(current.weatherCode()),
                current.isDay() == 1);
    }

    public static String toCondition(int weatherCode) {
        return CONDITIONS.getOrDefault(weatherCode, UNKNOWN_CONDITION);
    }

    public static String toCardinal(double degrees) {
        double normalized = ((degrees % 360) + 360) % 360;
        int sector = (int) Math.floor((normalized + 22.5) / 45) % 8;
        return CARDINALS[sector];
    }

    private double parseCoordinate(String text, double limit) {
        if (text == null || text.isBlank()) {
            throw invalidCoordinates();
        }
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw invalidCoordinates();
        }
        if (!Double.isFinite(value) || value < -limit || value > limit) {
            throw invalidCoordinates();
        }
        return value;
    }

    private InvalidRequestException invalidCoordinates() {
        return new InvalidRequestException("INVALID_COORDINATES",
                "Latitude (-90 a 90) e longitude (-180 a 180) devem ser numéricas e estar no intervalo.");
    }
}
