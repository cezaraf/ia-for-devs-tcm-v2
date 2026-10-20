package com.cezar.dev.airquality.services;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    static final String UNKNOWN_CONDITION = "Condição desconhecida";

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

    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    private final OpenMeteoClient client;

    public WeatherService(OpenMeteoClient client) {
        this.client = client;
    }

    public WeatherResponse current(String latitudeText, String longitudeText) {
        double latitude = parseCoordinate(latitudeText, 90);
        double longitude = parseCoordinate(longitudeText, 180);
        ForecastApiResponse forecast = client.fetchForecast(latitude, longitude);
        ForecastApiResponse.Current c = forecast.current();
        int weatherCode = c.weatherCode();
        int windDirection = (int) Math.round(c.windDirection());
        return new WeatherResponse(
                forecast.latitude(),
                forecast.longitude(),
                forecast.timezone(),
                c.time(),
                c.temperature(),
                c.apparentTemperature(),
                (int) Math.round(c.relativeHumidity()),
                c.precipitation(),
                c.windSpeed(),
                windDirection,
                cardinal(windDirection),
                weatherCode,
                condition(weatherCode),
                c.isDay() == 1);
    }

    static String condition(int weatherCode) {
        return CONDITIONS.getOrDefault(weatherCode, UNKNOWN_CONDITION);
    }

    /** Setores de 45° centrados em cada ponto cardeal (N = 337.5°–22.5°). */
    static String cardinal(double degrees) {
        double normalized = ((degrees % 360) + 360) % 360;
        return CARDINALS[(int) (Math.round(normalized / 45.0) % 8)];
    }

    private static double parseCoordinate(String text, double limit) {
        String code = "INVALID_COORDINATES";
        String message = "Latitude e longitude devem ser numéricas e estar dentro do intervalo válido.";
        if (text == null || text.isBlank()) {
            throw new InvalidRequestException(code, message);
        }
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(code, message);
        }
        if (!Double.isFinite(value) || value < -limit || value > limit) {
            throw new InvalidRequestException(code, message);
        }
        return value;
    }
}
