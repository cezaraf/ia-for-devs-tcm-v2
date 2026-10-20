package com.cezar.dev.airquality.exceptions;

public class CityNotFoundException extends RuntimeException {
    private final String code;

    public CityNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CityNotFoundException(String message) {
        this("CITY_NOT_FOUND", message);
    }

    public String getCode() {
        return code;
    }
}
