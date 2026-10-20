package com.cezar.dev.airquality.exceptions;

/** Nenhuma cidade corresponde à busca (404). */
public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(String message) {
        super(message);
    }
}
