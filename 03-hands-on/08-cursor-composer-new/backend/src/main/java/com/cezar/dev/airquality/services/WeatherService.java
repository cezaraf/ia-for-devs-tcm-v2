package com.cezar.dev.airquality.services;

import org.springframework.stereotype.Service;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.dtos.openmeteo.CurrentWeather;
import com.cezar.dev.airquality.dtos.openmeteo.ForecastApiResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

@Service
public class WeatherService {

    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    private final OpenMeteoClient openMeteoClient;

    public WeatherService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public WeatherResponse getWeather(Double latitude, Double longitude) {
        double lat = validateLatitude(latitude);
        double lon = validateLongitude(longitude);
        ForecastApiResponse forecast = openMeteoClient.fetchForecast(lat, lon);
        CurrentWeather current = forecast.current();
        if (current.temperature2m() == null
                || current.apparentTemperature() == null
                || current.relativeHumidity2m() == null
                || current.precipitation() == null
                || current.weatherCode() == null
                || current.windSpeed10m() == null
                || current.windDirection10m() == null
                || current.isDay() == null
                || current.time() == null) {
            throw new UpstreamException("Resposta incompleta do serviço de previsão.");
        }
        int windDirection = current.windDirection10m();
        return new WeatherResponse(
                forecast.latitude(),
                forecast.longitude(),
                forecast.timezone(),
                current.time(),
                current.temperature2m(),
                current.apparentTemperature(),
                current.relativeHumidity2m(),
                current.precipitation(),
                current.windSpeed10m(),
                windDirection,
                toCardinal(windDirection),
                current.weatherCode(),
                translateWeatherCode(current.weatherCode()),
                current.isDay() != 0);
    }

    static double validateLatitude(Double latitude) {
        if (latitude == null || Double.isNaN(latitude) || latitude < -90 || latitude > 90) {
            throw new InvalidRequestException("INVALID_COORDINATES", "Coordenadas inválidas.");
        }
        return latitude;
    }

    static double validateLongitude(Double longitude) {
        if (longitude == null || Double.isNaN(longitude) || longitude < -180 || longitude > 180) {
            throw new InvalidRequestException("INVALID_COORDINATES", "Coordenadas inválidas.");
        }
        return longitude;
    }

    static String toCardinal(int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        int index = (int) Math.round(normalized / 45.0) % 8;
        return CARDINALS[index];
    }

    static String translateWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Céu limpo";
            case 1 -> "Predominantemente limpo";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45 -> "Nevoeiro";
            case 48 -> "Nevoeiro com geada";
            case 51 -> "Garoa fraca";
            case 53 -> "Garoa moderada";
            case 55 -> "Garoa intensa";
            case 56 -> "Garoa congelante fraca";
            case 57 -> "Garoa congelante intensa";
            case 61 -> "Chuva fraca";
            case 63 -> "Chuva moderada";
            case 65 -> "Chuva forte";
            case 66 -> "Chuva congelante fraca";
            case 67 -> "Chuva congelante forte";
            case 71 -> "Neve fraca";
            case 73 -> "Neve moderada";
            case 75 -> "Neve forte";
            case 77 -> "Grãos de neve";
            case 80 -> "Pancadas de chuva fracas";
            case 81 -> "Pancadas de chuva moderadas";
            case 82 -> "Pancadas de chuva violentas";
            case 85 -> "Pancadas de neve fracas";
            case 86 -> "Pancadas de neve fortes";
            case 95 -> "Trovoada";
            case 96 -> "Trovoada com granizo fraco";
            case 99 -> "Trovoada com granizo forte";
            default -> "Condição desconhecida";
        };
    }
}
