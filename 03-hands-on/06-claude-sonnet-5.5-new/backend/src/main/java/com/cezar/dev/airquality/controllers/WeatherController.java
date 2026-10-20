package com.cezar.dev.airquality.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.services.WeatherService;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public WeatherResponse current(
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude) {
        return weatherService.current(latitude, longitude);
    }
}
