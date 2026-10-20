package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoGeocodingResponse(
    List<OpenMeteoCityResult> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoCityResult(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        @JsonProperty("country_code") String countryCode,
        String country,
        String admin1
    ) {}
}
