package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.services.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    @GetMapping("/weather")
    public WeatherResponse weather(@RequestParam(required = false) String latitude,
                                   @RequestParam(required = false) String longitude) {
        return service.current(latitude, longitude);
    }
}
