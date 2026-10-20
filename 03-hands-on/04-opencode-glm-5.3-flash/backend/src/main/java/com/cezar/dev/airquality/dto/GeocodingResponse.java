package com.cezar.dev.airquality.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta da Geocoding API do Open-Meteo
 * (https://geocoding-api.open-meteo.com/v1/search).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResponse(@JsonProperty("results") List<Place> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            @JsonProperty("name") String name,
            @JsonProperty("admin1") String admin1,
            @JsonProperty("country") String country,
            @JsonProperty("country_code") String countryCode,
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("population") Long population) {
    }
}
