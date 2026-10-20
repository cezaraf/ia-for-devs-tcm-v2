package com.cezar.dev.airquality.exceptions;

public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message) {
        super(message);
    }

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
