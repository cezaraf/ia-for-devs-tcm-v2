package com.cezar.dev.airquality.services;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.OpenMeteoForecastResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;

    public WeatherService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public WeatherResponse getWeather(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new InvalidRequestException("INVALID_COORDINATES", "Latitude e longitude são obrigatórias.");
        }

        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new InvalidRequestException("INVALID_COORDINATES", "Coordenadas fora do intervalo permitido (-90 a 90 para latitude, -180 a 180 para longitude).");
        }

        OpenMeteoForecastResponse response = openMeteoClient.getWeather(latitude, longitude);

        if (response == null || response.current() == null) {
            throw new UpstreamException("O serviço de clima está indisponível no momento.");
        }

        var current = response.current();
        String cardinal = toCardinal(current.windDirection10m());
        String condition = toCondition(current.weatherCode());
        boolean isDay = current.isDay() != null && current.isDay() == 1;

        return new WeatherResponse(
                response.latitude(),
                response.longitude(),
                response.timezone(),
                current.time(),
                current.temperature2m(),
                current.apparentTemperature(),
                current.relativeHumidity2m(),
                current.precipitation(),
                current.windSpeed10m(),
                current.windDirection10m(),
                cardinal,
                current.weatherCode(),
                condition,
                isDay
        );
    }

    public static String toCondition(Integer code) {
        if (code == null) {
            return "Condição desconhecida";
        }
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

    public static String toCardinal(Integer degrees) {
        if (degrees == null) {
            return "N";
        }
        double normalized = ((degrees % 360) + 360) % 360;
        int index = (int) Math.floor((normalized + 22.5) / 45.0) % 8;
        return switch (index) {
            case 0 -> "N";
            case 1 -> "NE";
            case 2 -> "E";
            case 3 -> "SE";
            case 4 -> "S";
            case 5 -> "SO";
            case 6 -> "O";
            case 7 -> "NO";
            default -> "N";
        };
    }
}
