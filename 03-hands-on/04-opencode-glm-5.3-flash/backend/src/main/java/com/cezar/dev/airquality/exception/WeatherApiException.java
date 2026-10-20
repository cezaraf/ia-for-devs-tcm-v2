package com.cezar.dev.airquality.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro de regra de negócio do painel de clima, com o status HTTP que o
 * frontend deve receber.
 */
public class WeatherApiException extends RuntimeException {

    private final HttpStatus status;

    public WeatherApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
