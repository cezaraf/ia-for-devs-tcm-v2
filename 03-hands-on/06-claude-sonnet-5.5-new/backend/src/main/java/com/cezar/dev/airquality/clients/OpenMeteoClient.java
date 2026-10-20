package com.cezar.dev.airquality.clients;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cezar.dev.airquality.config.OpenMeteoProperties;
import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse.GeocodingApiResult;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

/**
 * Única classe que conversa com a Open-Meteo. Falhas viram {@link UpstreamException}
 * ou {@link UpstreamTimeoutException}; nada do conteúdo bruto da resposta vaza.
 */
public class OpenMeteoClient {

    private static final String CURRENT_FIELDS = "temperature_2m,apparent_temperature,relative_humidity_2m,"
            + "precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day";

    private final RestClient restClient;
    private final OpenMeteoProperties properties;

    public OpenMeteoClient(RestClient.Builder builder, OpenMeteoProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public List<GeocodingApiResult> searchCities(String name) {
        GeocodingApiResponse response = call(() -> restClient.get()
                .uri(properties.geocodingUrl() + "?name={name}&count=10&language=pt&format=json", name)
                .retrieve()
                .body(GeocodingApiResponse.class));
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results();
    }

    public ForecastApiResponse fetchCurrentWeather(double latitude, double longitude) {
        ForecastApiResponse response = call(() -> restClient.get()
                .uri(properties.forecastUrl()
                        + "?latitude={latitude}&longitude={longitude}&current=" + CURRENT_FIELDS
                        + "&wind_speed_unit=kmh&timezone=auto", latitude, longitude)
                .retrieve()
                .body(ForecastApiResponse.class));
        if (response == null || response.current() == null) {
            throw new UpstreamException("Resposta da Open-Meteo sem dados atuais.");
        }
        return response;
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new UpstreamTimeoutException("Open-Meteo não respondeu no prazo.", e);
            }
            throw new UpstreamException("Falha ao consultar a Open-Meteo.", e);
        }
    }

    private boolean isTimeout(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) {
                return true;
            }
            if (t instanceof IOException && t.getMessage() != null
                    && t.getMessage().toLowerCase().contains("timed out")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
