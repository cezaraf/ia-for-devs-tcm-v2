package com.cezar.dev.airquality.services;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.cezar.dev.airquality.dto.ForecastResponse;
import com.cezar.dev.airquality.dto.GeocodingResponse;
import com.cezar.dev.airquality.dto.WeatherResponse;
import com.cezar.dev.airquality.exception.WeatherApiException;

/**
 * Consulta o Open-Meteo (API gratuita, sem key): primeiro converte o nome da
 * cidade em coordenadas (Geocoding API) e depois busca o clima (Weather API).
 */
@Service
public class WeatherService {

    private final RestClient restClient;
    private final String geocodingUri;
    private final String forecastUri;

    public WeatherService(RestClient.Builder builder,
            @Value("${open-meteo.geocoding-url}") String geocodingUrl,
            @Value("${open-meteo.forecast-url}") String forecastUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.requestFactory(requestFactory).build();
        this.geocodingUri = geocodingUrl + "?name={city}&count=1&language=pt&format=json";
        this.forecastUri = forecastUrl + "?latitude={lat}&longitude={lon}"
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,"
                + "precipitation,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m"
                + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code"
                + "&forecast_days=5&timezone=auto";
    }

    /** Clima da cidade informada, resolvendo as coordenadas pelo nome. */
    public WeatherResponse byCity(String city) {
        GeocodingResponse geocoding = restClient.get()
                .uri(geocodingUri, city, 1)
                .retrieve()
                .body(GeocodingResponse.class);

        List<GeocodingResponse.Place> results = geocoding == null || geocoding.results() == null
                ? List.of()
                : geocoding.results();
        if (results.isEmpty()) {
            throw new WeatherApiException(HttpStatus.NOT_FOUND,
                    "Cidade não encontrada: \"" + city + "\". Tente informar também o estado ou o país.");
        }

        GeocodingResponse.Place place = results.get(0);
        WeatherResponse.Place resolved = new WeatherResponse.Place(place.name(), place.admin1(), place.country(),
                place.countryCode(), place.latitude(), place.longitude(), place.timezone());
        return forecast(place.latitude(), place.longitude(), resolved);
    }

    /** Clima pelas coordenadas (usado quando o navegador fornece a localização). */
    public WeatherResponse byCoordinates(double latitude, double longitude) {
        return forecast(latitude, longitude, new WeatherResponse.Place(null, null, null, null,
                latitude, longitude, null));
    }

    private WeatherResponse forecast(double latitude, double longitude, WeatherResponse.Place place) {
        ForecastResponse forecast = restClient.get()
                .uri(forecastUri, latitude, longitude)
                .retrieve()
                .body(ForecastResponse.class);

        if (forecast == null || forecast.current() == null) {
            throw new WeatherApiException(HttpStatus.BAD_GATEWAY,
                    "O serviço de clima não retornou dados para esta localização.");
        }

        ForecastResponse.Current current = forecast.current();
        List<WeatherResponse.Day> days = mapDaily(forecast.daily());

        return new WeatherResponse(place,
                new WeatherResponse.Current(current.time(), current.temperature(), current.apparentTemperature(),
                        current.humidity(), current.isDay(), current.precipitation(), current.weatherCode(),
                        current.cloudCover(), current.windSpeed(), current.windDirection()),
                days, WeatherResponse.Units.METRIC);
    }

    private List<WeatherResponse.Day> mapDaily(ForecastResponse.Daily daily) {
        int count = daily == null || daily.time() == null ? 0 : daily.time().size();
        List<WeatherResponse.Day> days = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            days.add(new WeatherResponse.Day(
                    daily.time().get(i),
                    at(daily.temperatureMax(), i, 0.0),
                    at(daily.temperatureMin(), i, 0.0),
                    at(daily.precipitationProbabilityMax(), i, null),
                    at(daily.weatherCode(), i, 0)));
        }
        return days;
    }

    /** Lê a posição i de uma lista tolerando listas ausentes, curtas ou com null. */
    private static <T> T at(List<T> values, int index, T fallback) {
        if (values == null || index >= values.size()) {
            return fallback;
        }
        T value = values.get(index);
        return value != null ? value : fallback;
    }
}
