package com.cezar.dev.airquality.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("status", "UP", "service", "weather-api");
    }

    @GetMapping("/healthcheck")
    public String healthCheck() {
        return "OK";
    }
}