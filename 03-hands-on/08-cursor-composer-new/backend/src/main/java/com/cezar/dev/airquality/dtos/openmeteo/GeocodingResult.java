package com.cezar.dev.airquality.dtos.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResult(
        long id,
        String name,
        double latitude,
        double longitude,
        String admin1,
        String country,
        @JsonProperty("country_code") String countryCode) {
}
