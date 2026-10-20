package com.cezar.dev.airquality.services;

public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(String city) {
        super("Cidade não encontrada: " + city);
    }
}
