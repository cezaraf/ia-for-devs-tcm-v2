package com.cezar.dev.airquality.dtos;

public record CityResponse(
    Long id,
    String name,
    String admin1,
    String country,
    String countryCode,
    Double latitude,
    Double longitude
) {}
