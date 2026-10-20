package com.cezar.dev.airquality.dtos;

public record CityResponse(long id, String name, String region, String subregion, String country,
                           String countryCode, double latitude, double longitude, String timezone) {
}
