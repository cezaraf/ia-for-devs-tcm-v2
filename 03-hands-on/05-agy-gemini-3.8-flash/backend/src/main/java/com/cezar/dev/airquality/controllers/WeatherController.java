package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dto.WeatherResponse;
import com.cezar.dev.airquality.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping({"/weather", "/api/weather"})
    public ResponseEntity<WeatherResponse> getWeather(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lon
    ) {
        if (city != null && !city.trim().isEmpty()) {
            WeatherResponse response = weatherService.getWeatherByCity(city);
            return ResponseEntity.ok(response);
        }

        if (lat != null && lon != null) {
            WeatherResponse response = weatherService.getWeatherByCoordinates(lat, lon);
            return ResponseEntity.ok(response);
        }

        throw new IllegalArgumentException("Informe o nome da cidade ou as coordenadas geográficas (lat e lon).");
    }
}
