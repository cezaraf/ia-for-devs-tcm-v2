package com.cezar.dev.airquality.dtos.openmeteo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingApiResponse(List<GeocodingResult> results) {
}
