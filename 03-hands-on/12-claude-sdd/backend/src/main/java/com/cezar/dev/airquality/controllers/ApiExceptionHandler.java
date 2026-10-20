package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dtos.ErrorResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCityNotFound(CityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                "CITY_NOT_FOUND", "Nenhuma cidade encontrada com esse nome."));
    }

    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(UpstreamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(
                "UPSTREAM_ERROR",
                "O serviço de clima está indisponível no momento. Tente novamente em instantes."));
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamTimeout(UpstreamTimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(new ErrorResponse(
                "UPSTREAM_TIMEOUT",
                "O serviço de clima demorou demais para responder. Tente novamente."));
    }
}
