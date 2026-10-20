package com.cezar.dev.airquality.exceptions;

/** Conexão ou leitura da Open-Meteo excedeu o timeout (504). */
public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message) {
        super(message);
    }
}
