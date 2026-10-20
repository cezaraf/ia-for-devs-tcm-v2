package com.cezar.dev.airquality.clients;

import com.cezar.dev.airquality.dtos.OpenMeteoResponses.*;
import com.cezar.dev.airquality.exceptions.*;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class OpenMeteoClient {
  private final RestClient geocoding; private final RestClient forecast;
  public OpenMeteoClient(@Value("${open-meteo.geocoding-url}") String geoUrl,
      @Value("${open-meteo.forecast-url}") String forecastUrl,
      @Value("${open-meteo.connect-timeout}") Duration connect,
      @Value("${open-meteo.read-timeout}") Duration read) {
    var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connect).build()); factory.setReadTimeout(read);
    geocoding = RestClient.builder().baseUrl(geoUrl).requestFactory(factory).build();
    forecast = RestClient.builder().baseUrl(forecastUrl).requestFactory(factory).build();
  }
  public Geocoding cities(String name) { return call(() -> geocoding.get().uri(b -> b.path("/v1/search").queryParam("name", name).queryParam("count", 10).queryParam("language", "pt").queryParam("format", "json").build()).retrieve().body(Geocoding.class)); }
  public Forecast weather(double latitude, double longitude) { return call(() -> forecast.get().uri(b -> b.path("/v1/forecast").queryParam("latitude", latitude).queryParam("longitude", longitude).queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day").queryParam("wind_speed_unit", "kmh").queryParam("timezone", "auto").build()).retrieve().body(Forecast.class)); }
  private <T> T call(java.util.function.Supplier<T> request) { try { T result = request.get(); if (result == null) throw new UpstreamException(); return result; } catch (UpstreamException e) { throw e; } catch (ResourceAccessException e) { if (hasTimeout(e)) throw new UpstreamTimeoutException(e); throw new UpstreamException(e); } catch (RestClientException e) { throw new UpstreamException(e); } }
  private boolean hasTimeout(Throwable error) { for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof SocketTimeoutException) return true; return false; }
}
