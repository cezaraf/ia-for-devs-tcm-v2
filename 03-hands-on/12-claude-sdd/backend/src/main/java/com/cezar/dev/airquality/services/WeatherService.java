package com.cezar.dev.airquality.services;

import static java.util.Map.entry;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cezar.dev.airquality.clients.ForecastApiClient;
import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

@Service
public class WeatherService {

    private static final String EDGES = "^[\\p{IsWhite_Space}\\uFEFF]+|[\\p{IsWhite_Space}\\uFEFF]+$";
    private static final String UNKNOWN_CONDITION = "Condição desconhecida";
    private static final String[] DIRECTIONS = {"Norte", "Nordeste", "Leste", "Sudeste", "Sul", "Sudoeste",
            "Oeste", "Noroeste"};

    private static final Map<Integer, String> CONDITIONS = Map.ofEntries(
            entry(0, "Céu limpo"),
            entry(1, "Predominantemente limpo"),
            entry(2, "Parcialmente nublado"),
            entry(3, "Nublado"),
            entry(45, "Nevoeiro"),
            entry(48, "Nevoeiro com geada"),
            entry(51, "Garoa fraca"),
            entry(53, "Garoa moderada"),
            entry(55, "Garoa intensa"),
            entry(56, "Garoa congelante fraca"),
            entry(57, "Garoa congelante intensa"),
            entry(61, "Chuva fraca"),
            entry(63, "Chuva moderada"),
            entry(65, "Chuva forte"),
            entry(66, "Chuva congelante fraca"),
            entry(67, "Chuva congelante forte"),
            entry(71, "Neve fraca"),
            entry(73, "Neve moderada"),
            entry(75, "Neve forte"),
            entry(77, "Grãos de neve"),
            entry(80, "Pancadas de chuva fracas"),
            entry(81, "Pancadas de chuva moderadas"),
            entry(82, "Pancadas de chuva violentas"),
            entry(85, "Pancadas de neve fracas"),
            entry(86, "Pancadas de neve fortes"),
            entry(95, "Trovoada"),
            entry(96, "Trovoada com granizo fraco"),
            entry(97, "Trovoada forte"),
            entry(99, "Trovoada com granizo forte"));

    private final ForecastApiClient forecastApiClient;

    public WeatherService(ForecastApiClient forecastApiClient) {
        this.forecastApiClient = forecastApiClient;
    }

    public WeatherResponse current(String latitude, String longitude) {
        double lat = parse(latitude, 90);
        double lon = parse(longitude, 180);
        ForecastApiResponse response = forecastApiClient.current(lat, lon);
        ForecastApiResponse.Current c = response == null ? null : response.current();
        if (c == null || c.time() == null || c.time().isEmpty()) {
            throw new UpstreamException("Bloco current ausente na resposta da API forecast");
        }
        return new WeatherResponse(lat, lon, response.timezone(), c.time(), c.temperature2m(),
                c.apparentTemperature(), c.relativeHumidity2m(), c.windSpeed10m(), c.windDirection10m(),
                windDirection(c.windDirection10m()), c.weatherCode(), describeWeatherCode(c.weatherCode()),
                toBoolean(c.isDay()));
    }

    public static String describeWeatherCode(Integer code) {
        return code == null ? UNKNOWN_CONDITION : CONDITIONS.getOrDefault(code, UNKNOWN_CONDITION);
    }

    public static String windDirection(Double degrees) {
        if (degrees == null || !Double.isFinite(degrees)) {
            return null;
        }
        double d = ((degrees % 360) + 360) % 360;
        return DIRECTIONS[(int) Math.floor((d + 22.5) / 45) % 8];
    }

    private static Boolean toBoolean(Integer isDay) {
        if (isDay == null || (isDay != 0 && isDay != 1)) {
            return null;
        }
        return isDay == 1;
    }

    private static double parse(String raw, double limit) {
        if (raw != null) {
            String value = raw.replaceAll(EDGES, "");
            if (!value.isEmpty()) {
                try {
                    double parsed = Double.parseDouble(value);
                    if (Double.isFinite(parsed) && parsed >= -limit && parsed <= limit) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // cai na exceção de coordenadas inválidas
                }
            }
        }
        throw new InvalidRequestException("INVALID_COORDINATES",
                "Coordenadas inválidas. Use latitude entre -90 e 90 e longitude entre -180 e 180.");
    }
}
