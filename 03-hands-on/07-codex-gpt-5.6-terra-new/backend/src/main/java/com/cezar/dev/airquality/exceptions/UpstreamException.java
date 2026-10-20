package com.cezar.dev.airquality.exceptions;
public class UpstreamException extends RuntimeException { public UpstreamException() { super("Serviço externo indisponível."); } public UpstreamException(Throwable cause) { super(cause); } }
