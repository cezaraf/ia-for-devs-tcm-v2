package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dto.WeatherDto;
import com.cezar.dev.airquality.service.WeatherService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherDto getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Informe latitude e longitude.");
            }
            return weatherService.getWeatherByCoordinates(latitude, longitude);
        }
        return weatherService.getWeatherByCity(city);
    }
}
