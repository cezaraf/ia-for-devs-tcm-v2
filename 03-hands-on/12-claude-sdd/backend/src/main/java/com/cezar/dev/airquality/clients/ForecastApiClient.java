package com.cezar.dev.airquality.clients;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

@Component
public class ForecastApiClient {

    private static final Logger log = LoggerFactory.getLogger(ForecastApiClient.class);
    private static final String API = "forecast";
    private static final String CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,apparent_temperature,"
            + "is_day,weather_code,wind_speed_10m,wind_direction_10m";

    private final RestClient restClient;
    private final String baseUrl;

    public ForecastApiClient(@Qualifier("forecastApiRestClient") RestClient restClient,
                             @Value("${forecast-api.base-url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public ForecastApiResponse current(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", CURRENT_FIELDS)
                .queryParam("timezone", "auto")
                .queryParam("temperature_unit", "celsius")
                .queryParam("wind_speed_unit", "kmh")
                .encode().build().toUri();
        long start = System.nanoTime();
        try {
            ForecastApiResponse body = restClient.get().uri(uri).retrieve().body(ForecastApiResponse.class);
            if (body == null) {
                warn("invalid-body", start);
                throw new UpstreamException("Resposta vazia da API forecast");
            }
            return body;
        } catch (RestClientResponseException e) {
            warn("status=" + e.getStatusCode().value(), start);
            throw new UpstreamException("Status inesperado da API forecast");
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                warn("timeout", start);
                throw new UpstreamTimeoutException("Timeout na API forecast");
            }
            warn("io", start);
            throw new UpstreamException("Falha de conexão com a API forecast");
        } catch (RestClientException e) {
            warn("invalid-body", start);
            throw new UpstreamException("Resposta inválida da API forecast");
        }
    }

    private static void warn(String category, long startNanos) {
        log.warn("Falha na API {}: {} ({} ms)", API, category, (System.nanoTime() - startNanos) / 1_000_000);
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
