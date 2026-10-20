package com.cezar.dev.airquality.dtos;

/** Cidade devolvida por GET /cities. */
public record CityResponse(
        long id,
        String name,
        String admin1,
        String country,
        String countryCode,
        double latitude,
        double longitude) {
}
