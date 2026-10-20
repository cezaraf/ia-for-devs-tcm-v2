package com.cezar.dev.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoGeocodingResponse(
    List<GeocodingResult> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingResult(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        Double elevation,
        String country,
        @JsonProperty("country_code") String countryCode,
        String admin1,
        String admin2,
        String timezone
    ) {}
}
