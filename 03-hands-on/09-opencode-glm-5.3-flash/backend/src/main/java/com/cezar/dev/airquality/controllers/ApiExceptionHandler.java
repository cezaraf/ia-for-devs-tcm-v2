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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte as exceções da aplicação no corpo de erro padrão { code, message }. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCityNotFound(CityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("CITY_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamError(UpstreamException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("UPSTREAM_ERROR", exception.getMessage()));
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamTimeout(UpstreamTimeoutException exception) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorResponse("UPSTREAM_TIMEOUT", exception.getMessage()));
    }

    /** Parâmetro com tipo errado (ex.: latitude=abc) → 400 com o código do parâmetro. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String parameter = exception.getName();
        String code = "latitude".equals(parameter) || "longitude".equals(parameter)
                ? "INVALID_COORDINATES"
                : "INVALID_CITY_NAME";
        return ResponseEntity.badRequest().body(new ErrorResponse(code, "Parâmetro inválido."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Erro interno."));
    }
}
