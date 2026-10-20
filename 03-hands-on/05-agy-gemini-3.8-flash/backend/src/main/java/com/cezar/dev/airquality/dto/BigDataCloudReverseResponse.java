package com.cezar.dev.airquality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BigDataCloudReverseResponse(
    String city,
    String locality,
    String principalSubdivision,
    String countryName,
    String countryCode
) {}
