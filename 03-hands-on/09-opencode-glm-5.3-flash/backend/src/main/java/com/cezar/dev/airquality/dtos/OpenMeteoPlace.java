package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Um resultado da Geocoding API da Open-Meteo (campos extras são ignorados). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoPlace(
        Long id,
        String name,
        String admin1,
        String country,
        @JsonProperty("country_code") String countryCode,
        Double latitude,
        Double longitude) {
}
