package com.cezar.dev.airquality.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.cezar.dev.airquality.dto.ErrorResponse;
import com.cezar.dev.airquality.dto.WeatherResponse;
import com.cezar.dev.airquality.exception.WeatherApiException;
import com.cezar.dev.airquality.services.WeatherService;

/**
 * Endpoint consumido pelo painel do clima. Aceita consulta por nome de cidade
 * (?city=Salvador) ou por coordenadas (?latitude=&longitude=), que o frontend
 * envia quando o usuário autoriza a geolocation do navegador.
 */
@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public WeatherResponse weather(@RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        boolean hasCity = city != null && !city.isBlank();
        boolean hasCoordinates = latitude != null || longitude != null;

        if (hasCity && hasCoordinates) {
            throw new WeatherApiException(HttpStatus.BAD_REQUEST,
                    "Informe a cidade OU as coordenadas, não os dois.");
        }
        if (hasCity) {
            return weatherService.byCity(city.trim());
        }
        if (latitude == null || longitude == null) {
            throw new WeatherApiException(HttpStatus.BAD_REQUEST,
                    "Informe ?city= ou ?latitude= e ?longitude=.");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new WeatherApiException(HttpStatus.BAD_REQUEST,
                    "Coordenadas inválidas: latitude entre -90 e 90, longitude entre -180 e 180.");
        }
        return weatherService.byCoordinates(latitude, longitude);
    }

    @ExceptionHandler(WeatherApiException.class)
    public ResponseEntity<ErrorResponse> weatherApiError(WeatherApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> upstreamError(RestClientException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Não foi possível consultar o serviço de clima agora. Tente novamente."));
    }
}
