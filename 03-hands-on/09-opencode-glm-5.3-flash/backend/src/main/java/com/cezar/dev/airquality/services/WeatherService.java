package com.cezar.dev.airquality.services;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.OpenMeteoCurrent;
import com.cezar.dev.airquality.dtos.OpenMeteoForecastResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

import org.springframework.stereotype.Service;

/**
 * Valida as coordenadas, consulta a Open-Meteo e converte a resposta em
 * WeatherResponse (condição em PT-BR e ponto cardeal em PT-BR).
 */
@Service
public class WeatherService {

    /** Setores de 45° centrados em N (0°), NE (45°), E (90°), SE (135°), S (180°), SO (225°), O (270°) e NO (315°). */
    private static final String[] CARDINAL_POINTS = { "N", "NE", "E", "SE", "S", "SO", "O", "NO" };

    private final OpenMeteoClient openMeteoClient;

    public WeatherService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public WeatherResponse getCurrentWeather(Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        OpenMeteoForecastResponse response = this.openMeteoClient.getForecast(latitude, longitude);
        OpenMeteoCurrent current = response.current();
        if (current == null) {
            throw new UpstreamException("Resposta incompleta do serviço de clima.");
        }
        int weatherCode = current.weather_code() != null ? current.weather_code() : -1;
        return new WeatherResponse(
                latitude,
                longitude,
                response.timezone() != null ? response.timezone() : "UTC",
                current.time() != null ? current.time() : "",
                orZero(current.temperature_2m()),
                orZero(current.apparent_temperature()),
                orZero(current.relative_humidity_2m()),
                orZero(current.precipitation()),
                orZero(current.wind_speed_10m()),
                orZero(current.wind_direction_10m()),
                cardinalPoint(orZero(current.wind_direction_10m())),
                weatherCode,
                condition(weatherCode),
                current.is_day() != null && current.is_day() == 1);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new InvalidRequestException("INVALID_COORDINATES", "Informe latitude e longitude.");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidRequestException("INVALID_COORDINATES",
                    "Latitude deve estar entre -90 e 90 e longitude entre -180 e 180.");
        }
    }

    /** Tabela WMO de weather_code para condição em PT-BR. */
    static String condition(int code) {
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

    /** Direção do vento em graus para o ponto cardeal cujo setor de 45° contém o valor. */
    static String cardinalPoint(int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        return CARDINAL_POINTS[(int) (Math.round(normalized / 45.0) % CARDINAL_POINTS.length)];
    }

    private static int orZero(Integer value) {
        return value != null ? value : 0;
    }

    private static double orZero(Double value) {
        return value != null ? value : 0.0;
    }
}
