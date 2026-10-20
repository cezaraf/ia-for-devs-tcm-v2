package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.services.WeatherService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /weather?latitude={lat}&longitude={lon} — clima atual nas coordenadas. */
@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public WeatherResponse getCurrentWeather(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return this.weatherService.getCurrentWeather(latitude, longitude);
    }
}
