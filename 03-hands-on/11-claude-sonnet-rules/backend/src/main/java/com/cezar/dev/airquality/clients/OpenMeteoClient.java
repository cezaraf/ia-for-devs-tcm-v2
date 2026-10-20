package com.cezar.dev.airquality.clients;

import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OpenMeteoClient {

    private static final String CURRENT_FIELDS = "temperature_2m,apparent_temperature,"
            + "relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day";

    private final RestClient restClient;
    private final String geocodingUrl;
    private final String forecastUrl;

    public OpenMeteoClient(RestClient openMeteoRestClient,
                           @Value("${open-meteo.geocoding-url}") String geocodingUrl,
                           @Value("${open-meteo.forecast-url}") String forecastUrl) {
        this.restClient = openMeteoRestClient;
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
    }

    /** Lista vazia quando a Geocoding API não traz resultados. */
    public List<GeocodingApiResponse.Result> searchCities(String name) {
        URI uri = UriComponentsBuilder.fromUriString(geocodingUrl)
                .queryParam("name", name)
                .queryParam("count", 10)
                .queryParam("language", "pt")
                .queryParam("format", "json")
                .encode().build().toUri();
        GeocodingApiResponse body = call(() -> restClient.get().uri(uri).retrieve()
                .body(GeocodingApiResponse.class));
        if (body == null || body.results() == null) {
            return List.of();
        }
        for (GeocodingApiResponse.Result r : body.results()) {
            if (r == null || r.id() == null || r.name() == null
                    || r.latitude() == null || r.longitude() == null) {
                throw new UpstreamException();
            }
        }
        return body.results();
    }

    public ForecastApiResponse fetchForecast(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString(forecastUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", CURRENT_FIELDS)
                .queryParam("wind_speed_unit", "kmh")
                .queryParam("timezone", "auto")
                .encode().build().toUri();
        ForecastApiResponse body = call(() -> restClient.get().uri(uri).retrieve()
                .body(ForecastApiResponse.class));
        if (body == null || body.latitude() == null || body.longitude() == null
                || body.timezone() == null || !hasAllCurrentFields(body.current())) {
            throw new UpstreamException();
        }
        return body;
    }

    private static boolean hasAllCurrentFields(ForecastApiResponse.Current c) {
        return c != null && c.time() != null && c.temperature() != null
                && c.apparentTemperature() != null && c.relativeHumidity() != null
                && c.precipitation() != null && c.weatherCode() != null && c.windSpeed() != null
                && c.windDirection() != null && c.isDay() != null;
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new UpstreamException();
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new UpstreamTimeoutException();
            }
            throw new UpstreamException();
        } catch (RestClientException e) {
            throw new UpstreamException();
        }
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
