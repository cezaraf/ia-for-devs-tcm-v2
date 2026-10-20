package com.cezar.dev.airquality.exceptions;

/**
 * Requisição inválida (400). O código identifica o parâmetro problemático
 * (INVALID_CITY_NAME ou INVALID_COORDINATES).
 */
public class InvalidRequestException extends RuntimeException {

    private final String code;

    public InvalidRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
