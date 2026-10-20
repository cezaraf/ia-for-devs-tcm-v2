package com.cezar.dev.airquality.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingApiResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(Long id, String name, Double latitude, Double longitude, String admin1, String admin2,
                         String country, @JsonProperty("country_code") String countryCode,
                         @JsonProperty("feature_code") String featureCode, String timezone) {
    }
}
