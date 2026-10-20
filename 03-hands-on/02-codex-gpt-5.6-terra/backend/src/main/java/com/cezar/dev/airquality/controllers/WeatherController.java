package com.cezar.dev.airquality.controllers;

import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class WeatherController {
    private final RestClient http;

    public WeatherController(RestClient.Builder builder) {
        this.http = builder.build();
    }

    @GetMapping("/weather")
    public Map<String, Object> getWeather(@RequestParam String city) {
        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma cidade para a consulta.");
        }

        try {
            JsonNode geocoding = http.get()
                    .uri("https://geocoding-api.open-meteo.com/v1/search?name={city}&count=1&language=pt&format=json", city.trim())
                    .retrieve().body(JsonNode.class);
            JsonNode place = geocoding == null ? null : geocoding.path("results").path(0);
            if (place == null || place.isMissingNode()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidade não encontrada. Tente informar também o estado ou país.");
            }

            JsonNode forecast = http.get()
                    .uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=auto",
                            place.path("latitude").asDouble(), place.path("longitude").asDouble())
                    .retrieve().body(JsonNode.class);
            JsonNode current = forecast == null ? null : forecast.path("current");
            if (current == null || current.isMissingNode()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O serviço de clima não retornou dados para esta cidade.");
            }

            return Map.of(
                    "location", Map.of("name", place.path("name").asText(), "admin1", place.path("admin1").asText(""), "country", place.path("country").asText()),
                    "observedAt", current.path("time").asText(),
                    "weather", Map.of(
                            "temperature", current.path("temperature_2m").asDouble(),
                            "apparentTemperature", current.path("apparent_temperature").asDouble(),
                            "humidity", current.path("relative_humidity_2m").asInt(),
                            "windSpeed", current.path("wind_speed_10m").asDouble(),
                            "code", current.path("weather_code").asInt()));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o Open-Meteo no momento.", exception);
        }
    }
}
