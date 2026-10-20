package com.cezar.dev.airquality.exceptions;

public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException() {
        super("O serviço de clima demorou para responder.");
    }
}
