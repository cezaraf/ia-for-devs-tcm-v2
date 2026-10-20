package com.cezar.dev.airquality.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cezar.dev.airquality.dtos.ErrorResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCityNotFound(CityNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "CITY_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamTimeout(UpstreamTimeoutException e) {
        return build(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT",
                "O serviço de clima demorou para responder.");
    }

    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(UpstreamException e) {
        return build(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR",
                "O serviço de clima está indisponível no momento.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
