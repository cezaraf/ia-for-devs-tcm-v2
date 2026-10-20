package com.cezar.dev.airquality.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingApiResponse(List<GeocodingResult> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingResult(
            String name,
            double latitude,
            double longitude,
            String country,
            String country_code,
            String admin1
    ) {}
}
