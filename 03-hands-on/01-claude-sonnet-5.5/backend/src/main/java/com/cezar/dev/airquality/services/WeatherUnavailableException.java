package com.cezar.dev.airquality.services;

public class WeatherUnavailableException extends RuntimeException {

    public WeatherUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
