package com.cezar.dev.airquality.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cezar.dev.airquality.dto.WeatherResponse;
import com.cezar.dev.airquality.services.CityNotFoundException;
import com.cezar.dev.airquality.services.WeatherService;
import com.cezar.dev.airquality.services.WeatherUnavailableException;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    /** GET /weather?city=Salvador  ou  GET /weather?lat=-12.97&lon=-38.5 */
    @GetMapping
    public WeatherResponse weather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {
        if (city != null && !city.isBlank()) {
            String name = city.strip();
            if (name.length() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de cidade muito longo");
            }
            return service.byCity(name);
        }
        if (lat != null && lon != null) {
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas");
            }
            return service.byCoordinates(lat, lon);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe 'city' ou 'lat' e 'lon'");
    }

    @ExceptionHandler(CityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(CityNotFoundException e) {
        return new ApiError(e.getMessage());
    }

    @ExceptionHandler(WeatherUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError unavailable(WeatherUnavailableException e) {
        return new ApiError(e.getMessage());
    }

    public record ApiError(String message) {
    }
}
