package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.services.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {
        WeatherResponse weather = weatherService.getWeather(latitude, longitude);
        return ResponseEntity.ok(weather);
    }
}
