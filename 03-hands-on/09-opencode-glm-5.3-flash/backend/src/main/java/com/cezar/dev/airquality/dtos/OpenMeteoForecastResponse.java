package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** JSON do Forecast API da Open-Meteo (bloco "current"). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoForecastResponse(
        Double latitude,
        Double longitude,
        String timezone,
        OpenMeteoCurrent current) {
}
