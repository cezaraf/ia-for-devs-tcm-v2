package com.cezar.dev.airquality.dtos;

public record CityResponse(long id, String name, String admin1, String country,
                           String countryCode, double latitude, double longitude) {}
