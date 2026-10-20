package com.cezar.dev.airquality.exceptions;

public class UpstreamException extends RuntimeException {

    public UpstreamException() {
        super("Falha ao consultar o serviço de clima.");
    }
}
