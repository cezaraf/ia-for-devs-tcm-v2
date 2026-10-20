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
    ResponseEntity<ErrorResponse> invalidRequest(InvalidRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(CityNotFoundException.class)
    ResponseEntity<ErrorResponse> cityNotFound(CityNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "CITY_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    ResponseEntity<ErrorResponse> upstreamTimeout(UpstreamTimeoutException e) {
        return build(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT", e.getMessage());
    }

    @ExceptionHandler(UpstreamException.class)
    ResponseEntity<ErrorResponse> upstream(UpstreamException e) {
        return build(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", e.getMessage());
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
