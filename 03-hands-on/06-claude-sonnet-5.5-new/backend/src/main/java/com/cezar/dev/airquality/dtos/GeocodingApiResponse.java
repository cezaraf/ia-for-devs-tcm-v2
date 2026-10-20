package com.cezar.dev.airquality.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingApiResponse(List<GeocodingApiResult> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingApiResult(
            Long id,
            String name,
            String admin1,
            String country,
            @JsonProperty("country_code") String countryCode,
            Double latitude,
            Double longitude) {
    }
}
