package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.WeatherException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
@CrossOrigin(origins = "*")
public class GlobalExceptionHandler {

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCityNotFound(CityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.NOT_FOUND.value(),
            "error", "NOT_FOUND",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "BAD_REQUEST",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(WeatherException.class)
    public ResponseEntity<Map<String, Object>> handleWeatherException(WeatherException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_GATEWAY.value(),
            "error", "BAD_GATEWAY",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error", "INTERNAL_SERVER_ERROR",
            "message", "Ocorreu um erro interno ao processar a requisição."
        ));
    }
}
