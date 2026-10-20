package com.cezar.dev.airquality.service;

import com.cezar.dev.airquality.dto.WeatherDto;
import com.cezar.dev.airquality.openmeteo.ForecastApiResponse;
import com.cezar.dev.airquality.openmeteo.GeocodingApiResponse;
import com.cezar.dev.airquality.openmeteo.WeatherCodeDescriptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WeatherService {

    private static final String GEOCODING_SEARCH =
            "https://geocoding-api.open-meteo.com/v1/search?name={name}&count=1&language=pt";
    private static final String GEOCODING_SEARCH_BIAS =
            "https://geocoding-api.open-meteo.com/v1/search?name={name}&count=1&language=pt"
                    + "&latitude={latitude}&longitude={longitude}";
    private static final String FORECAST =
            "https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}"
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto";

    private final RestClient restClient;

    public WeatherService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public WeatherDto getWeatherByCity(String city) {
        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome da cidade.");
        }

        GeocodingApiResponse geocoding = restClient
                .get()
                .uri(GEOCODING_SEARCH, city.trim())
                .retrieve()
                .body(GeocodingApiResponse.class);

        if (geocoding == null || geocoding.results() == null || geocoding.results().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidade não encontrada.");
        }

        GeocodingApiResponse.GeocodingResult location = geocoding.results().getFirst();
        return fetchWeather(
                location.name(),
                location.country(),
                location.admin1(),
                location.latitude(),
                location.longitude());
    }

    public WeatherDto getWeatherByCoordinates(double latitude, double longitude) {
        ForecastApiResponse forecast = fetchForecast(latitude, longitude);
        GeocodingApiResponse.GeocodingResult location =
                resolveNearestPlace(latitude, longitude, forecast.timezone());
        return toWeatherDto(location, forecast);
    }

    private ForecastApiResponse fetchForecast(double latitude, double longitude) {
        ForecastApiResponse forecast = restClient
                .get()
                .uri(FORECAST, latitude, longitude)
                .retrieve()
                .body(ForecastApiResponse.class);

        if (forecast == null || forecast.current() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Não foi possível obter dados meteorológicos.");
        }
        return forecast;
    }

    private WeatherDto fetchWeather(
            String city, String country, String admin1, double latitude, double longitude) {
        GeocodingApiResponse.GeocodingResult location =
                new GeocodingApiResponse.GeocodingResult(city, latitude, longitude, country, null, admin1);
        return toWeatherDto(location, fetchForecast(latitude, longitude));
    }

    private GeocodingApiResponse.GeocodingResult resolveNearestPlace(
            double latitude, double longitude, String timezone) {
        String hint = timezoneHintName(timezone);
        GeocodingApiResponse geocoding = restClient
                .get()
                .uri(GEOCODING_SEARCH_BIAS, hint, latitude, longitude)
                .retrieve()
                .body(GeocodingApiResponse.class);

        if (geocoding != null && geocoding.results() != null && !geocoding.results().isEmpty()) {
            return geocoding.results().getFirst();
        }

        return new GeocodingApiResponse.GeocodingResult(
                hint, latitude, longitude, null, null, null);
    }

    private static String timezoneHintName(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return "Localização atual";
        }
        int slash = timezone.lastIndexOf('/');
        String segment = slash >= 0 ? timezone.substring(slash + 1) : timezone;
        return segment.replace('_', ' ');
    }

    private static WeatherDto toWeatherDto(
            GeocodingApiResponse.GeocodingResult location, ForecastApiResponse forecast) {
        ForecastApiResponse.Current current = forecast.current();
        return new WeatherDto(
                location.name(),
                location.country(),
                location.admin1(),
                forecast.latitude(),
                forecast.longitude(),
                current.temperature_2m(),
                current.relative_humidity_2m(),
                current.wind_speed_10m(),
                current.weather_code(),
                WeatherCodeDescriptions.describe(current.weather_code()),
                current.time());
    }
}
