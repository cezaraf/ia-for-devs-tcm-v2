package com.cezar.dev.airquality.clients;

import java.net.SocketTimeoutException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.cezar.dev.airquality.dtos.openmeteo.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.openmeteo.GeocodingApiResponse;
import com.cezar.dev.airquality.dtos.openmeteo.GeocodingResult;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

@Component
public class OpenMeteoClient {

    private final RestClient restClient;
    private final String geocodingUrl;
    private final String forecastUrl;

    public OpenMeteoClient(
            RestClient.Builder restClientBuilder,
            @Value("${open-meteo.geocoding-url}") String geocodingUrl,
            @Value("${open-meteo.forecast-url}") String forecastUrl) {
        this.restClient = restClientBuilder.build();
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
    }

    public List<GeocodingResult> searchCities(String name) {
        String uri = UriComponentsBuilder.fromUriString(geocodingUrl)
                .queryParam("name", name)
                .queryParam("count", 10)
                .queryParam("language", "pt")
                .queryParam("format", "json")
                .build()
                .toUriString();
        try {
            GeocodingApiResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(GeocodingApiResponse.class);
            if (response == null || response.results() == null) {
                return List.of();
            }
            return response.results();
        } catch (RestClientResponseException ex) {
            throw new UpstreamException("Falha ao consultar o serviço de geocodificação.");
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new UpstreamTimeoutException("O serviço de geocodificação demorou para responder.");
            }
            throw new UpstreamException("Não foi possível conectar ao serviço de geocodificação.");
        } catch (Exception ex) {
            throw new UpstreamException("Resposta inválida do serviço de geocodificação.");
        }
    }

    public ForecastApiResponse fetchForecast(double latitude, double longitude) {
        String uri = UriComponentsBuilder.fromUriString(forecastUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day")
                .queryParam("wind_speed_unit", "kmh")
                .queryParam("timezone", "auto")
                .build()
                .toUriString();
        try {
            ForecastApiResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(ForecastApiResponse.class);
            if (response == null || response.current() == null) {
                throw new UpstreamException("Resposta incompleta do serviço de previsão.");
            }
            return response;
        } catch (UpstreamException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new UpstreamException("Falha ao consultar o serviço de previsão.");
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new UpstreamTimeoutException("O serviço de previsão demorou para responder.");
            }
            throw new UpstreamException("Não foi possível conectar ao serviço de previsão.");
        } catch (Exception ex) {
            throw new UpstreamException("Resposta inválida do serviço de previsão.");
        }
    }

    private boolean isTimeout(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        String message = ex.getMessage();
        return message != null && message.toLowerCase().contains("timed out");
    }
}
