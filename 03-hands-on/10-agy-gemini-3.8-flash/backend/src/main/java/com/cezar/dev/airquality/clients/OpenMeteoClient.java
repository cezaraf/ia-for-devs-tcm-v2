package com.cezar.dev.airquality.clients;

import com.cezar.dev.airquality.dtos.OpenMeteoForecastResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoGeocodingResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;

@Component
public class OpenMeteoClient {

    private final RestClient restClient;
    private final String geocodingUrl;
    private final String forecastUrl;

    @Autowired
    public OpenMeteoClient(
            RestClient.Builder restClientBuilder,
            @Value("${open-meteo.geocoding-url}") String geocodingUrl,
            @Value("${open-meteo.forecast-url}") String forecastUrl,
            @Value("${open-meteo.connect-timeout:3s}") Duration connectTimeout,
            @Value("${open-meteo.read-timeout:5s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
    }

    public OpenMeteoClient(RestClient restClient, String geocodingUrl, String forecastUrl) {
        this.restClient = restClient;
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
    }

    public OpenMeteoGeocodingResponse searchCities(String name) {
        URI uri = buildGeocodingUri(name);
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OpenMeteoGeocodingResponse.class);
        } catch (ResourceAccessException ex) {
            handleResourceAccessException(ex);
            throw new UpstreamException("O serviço de clima está indisponível no momento.", ex);
        } catch (RestClientResponseException ex) {
            throw new UpstreamException("O serviço de clima está indisponível no momento.", ex);
        } catch (Exception ex) {
            if (ex instanceof UpstreamException || ex instanceof UpstreamTimeoutException) {
                throw ex;
            }
            throw new UpstreamException("O serviço de clima está indisponível no momento.", ex);
        }
    }

    public OpenMeteoForecastResponse getWeather(Double latitude, Double longitude) {
        URI uri = buildForecastUri(latitude, longitude);
        try {
            OpenMeteoForecastResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OpenMeteoForecastResponse.class);
            if (response == null || response.current() == null) {
                throw new UpstreamException("O serviço de clima está indisponível no momento.");
            }
            return response;
        } catch (ResourceAccessException ex) {
            handleResourceAccessException(ex);
            throw new UpstreamException("O serviço de clima está indisponível no momento.", ex);
        } catch (RestClientResponseException ex) {
            throw new UpstreamException("O serviço de clima está indisponível no momento.", ex);
        } catch (Exception ex) {
            if (ex instanceof UpstreamException || ex instanceof UpstreamTimeoutException) {
                throw ex;
            }
            throw new UpstreamException("O serviço de clima está indisponível no momento.", ex);
        }
    }

    private URI buildGeocodingUri(String name) {
        String base = geocodingUrl.endsWith("/v1/search") ? geocodingUrl : geocodingUrl + "/v1/search";
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("name", name)
                .queryParam("count", 10)
                .queryParam("language", "pt")
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();
    }

    private URI buildForecastUri(Double latitude, Double longitude) {
        String base = forecastUrl.endsWith("/v1/forecast") ? forecastUrl : forecastUrl + "/v1/forecast";
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day")
                .queryParam("wind_speed_unit", "kmh")
                .queryParam("timezone", "auto")
                .build()
                .encode()
                .toUri();
    }

    private void handleResourceAccessException(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        if (isTimeout(cause) || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timeout"))) {
            throw new UpstreamTimeoutException("O serviço de clima demorou para responder.", ex);
        }
    }

    private boolean isTimeout(Throwable cause) {
        if (cause == null) {
            return false;
        }
        if (cause instanceof SocketTimeoutException) {
            return true;
        }
        String className = cause.getClass().getName();
        if (className.contains("Timeout") || className.contains("TimedOut")) {
            return true;
        }
        return isTimeout(cause.getCause());
    }
}
