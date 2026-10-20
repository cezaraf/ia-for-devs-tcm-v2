package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dtos.ErrorResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCityNotFound(CityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getCode() != null ? ex.getCode() : "CITY_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamException(UpstreamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(ex.getCode() != null ? ex.getCode() : "UPSTREAM_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamTimeout(UpstreamTimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorResponse(ex.getCode() != null ? ex.getCode() : "UPSTREAM_TIMEOUT", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        if ("latitude".equalsIgnoreCase(name) || "longitude".equalsIgnoreCase(name)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_COORDINATES", "Coordenadas devem ser numéricas."));
        }
        if ("name".equalsIgnoreCase(name)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_CITY_NAME", "Nome da cidade inválido."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", "Parâmetro inválido: " + name));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        String param = ex.getParameterName();
        if ("name".equalsIgnoreCase(param)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_CITY_NAME", "Nome da cidade é obrigatório."));
        }
        if ("latitude".equalsIgnoreCase(param) || "longitude".equalsIgnoreCase(param)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_COORDINATES", "Latitude e longitude são obrigatórias."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", "Parâmetro obrigatório ausente: " + param));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "Erro interno do servidor."));
    }
}
