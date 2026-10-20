package com.cezar.dev.airquality.exceptions;

public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException() {
        super("Cidade não encontrada.");
    }
}
