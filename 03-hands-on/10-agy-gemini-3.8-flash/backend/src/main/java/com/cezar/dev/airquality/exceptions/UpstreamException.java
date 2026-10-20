package com.cezar.dev.airquality.exceptions;

public class UpstreamException extends RuntimeException {
    private final String code;

    public UpstreamException(String code, String message) {
        super(message);
        this.code = code;
    }

    public UpstreamException(String message) {
        this("UPSTREAM_ERROR", message);
    }

    public UpstreamException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public UpstreamException(String message, Throwable cause) {
        this("UPSTREAM_ERROR", message, cause);
    }

    public String getCode() {
        return code;
    }
}
