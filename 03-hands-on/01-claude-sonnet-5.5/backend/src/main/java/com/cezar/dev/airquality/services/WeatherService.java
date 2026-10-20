package com.cezar.dev.airquality.services;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cezar.dev.airquality.dto.WeatherResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class WeatherService {

    private static final String CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,apparent_temperature,"
            + "is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m";

    private final RestClient geocoding;
    private final RestClient forecast;

    public WeatherService(
            RestClient.Builder builder,
            @Value("${openmeteo.geocoding-url:https://geocoding-api.open-meteo.com}") String geocodingUrl,
            @Value("${openmeteo.forecast-url:https://api.open-meteo.com}") String forecastUrl) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.geocoding = builder.clone().baseUrl(geocodingUrl).requestFactory(factory).build();
        this.forecast = builder.clone().baseUrl(forecastUrl).requestFactory(factory).build();
    }

    /** Resolve a cidade pela Geocoding API e devolve o clima atual dela. */
    public WeatherResponse byCity(String city) {
        GeocodingResult place = geocode(city);
        var location = new WeatherResponse.Location(place.name(), place.admin1(), place.country(),
                place.latitude(), place.longitude(), place.timezone());
        return currentWeather(location);
    }

    /** Clima atual para coordenadas (usado quando o navegador informa a localização do usuário). */
    public WeatherResponse byCoordinates(double latitude, double longitude) {
        var location = new WeatherResponse.Location(null, null, null, latitude, longitude, null);
        return currentWeather(location);
    }

    private GeocodingResult geocode(String city) {
        GeocodingResponse response;
        try {
            response = geocoding.get()
                    .uri(u -> u.path("/v1/search")
                            .queryParam("name", city)
                            .queryParam("count", 1)
                            .queryParam("language", "pt")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(GeocodingResponse.class);
        } catch (RestClientException e) {
            throw new WeatherUnavailableException("Serviço de geocodificação indisponível", e);
        }
        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new CityNotFoundException(city);
        }
        return response.results().getFirst();
    }

    private WeatherResponse currentWeather(WeatherResponse.Location base) {
        ForecastResponse response;
        try {
            response = forecast.get()
                    .uri(u -> u.path("/v1/forecast")
                            .queryParam("latitude", base.latitude())
                            .queryParam("longitude", base.longitude())
                            .queryParam("current", CURRENT_FIELDS)
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(ForecastResponse.class);
        } catch (RestClientException e) {
            throw new WeatherUnavailableException("Serviço de clima indisponível", e);
        }
        if (response == null || response.current() == null) {
            throw new WeatherUnavailableException("Resposta inválida do serviço de clima", null);
        }

        var c = response.current();
        var u = response.currentUnits();
        boolean day = c.isDay() == 1;
        var info = WeatherCodes.of(c.weatherCode(), day);

        var location = new WeatherResponse.Location(base.name(), base.region(), base.country(),
                response.latitude(), response.longitude(), response.timezone());
        var current = new WeatherResponse.Current(c.time(), c.temperature(), c.apparentTemperature(),
                (int) Math.round(c.humidity()), c.precipitation(), c.windSpeed(),
                (int) Math.round(c.windDirection()), day, c.weatherCode(), info.description(), info.icon());
        var units = new WeatherResponse.Units(u.temperature(), u.humidity(), u.precipitation(), u.windSpeed());
        return new WeatherResponse(location, current, units);
    }

    // --- formato da Open-Meteo (só os campos usados) ---

    record GeocodingResponse(List<GeocodingResult> results) {
    }

    record GeocodingResult(String name, String admin1, String country, double latitude, double longitude,
                           String timezone) {
    }

    record ForecastResponse(
            double latitude,
            double longitude,
            String timezone,
            @JsonProperty("current_units") CurrentUnits currentUnits,
            Current current) {
    }

    record CurrentUnits(
            @JsonProperty("temperature_2m") String temperature,
            @JsonProperty("relative_humidity_2m") String humidity,
            @JsonProperty("precipitation") String precipitation,
            @JsonProperty("wind_speed_10m") String windSpeed) {
    }

    record Current(
            String time,
            @JsonProperty("temperature_2m") double temperature,
            @JsonProperty("apparent_temperature") double apparentTemperature,
            @JsonProperty("relative_humidity_2m") double humidity,
            double precipitation,
            @JsonProperty("wind_speed_10m") double windSpeed,
            @JsonProperty("wind_direction_10m") double windDirection,
            @JsonProperty("is_day") int isDay,
            @JsonProperty("weather_code") int weatherCode) {
    }
}
