package com.cezar.dev.airquality.exceptions;

/** A Open-Meteo falhou ou respondeu algo inutilizável (502). */
public class UpstreamException extends RuntimeException {

    public UpstreamException(String message) {
        super(message);
    }
}
