package com.cezar.dev.airquality.exceptions;

public class UpstreamTimeoutException extends RuntimeException {
    private final String code;

    public UpstreamTimeoutException(String code, String message) {
        super(message);
        this.code = code;
    }

    public UpstreamTimeoutException(String message) {
        this("UPSTREAM_TIMEOUT", message);
    }

    public UpstreamTimeoutException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public UpstreamTimeoutException(String message, Throwable cause) {
        this("UPSTREAM_TIMEOUT", message, cause);
    }

    public String getCode() {
        return code;
    }
}
