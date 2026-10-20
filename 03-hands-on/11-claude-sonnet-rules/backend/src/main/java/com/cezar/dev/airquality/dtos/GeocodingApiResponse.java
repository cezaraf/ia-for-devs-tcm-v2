package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingApiResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            Long id,
            String name,
            String admin1,
            String country,
            @JsonProperty("country_code") String countryCode,
            Double latitude,
            Double longitude) {
    }
}
