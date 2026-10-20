package com.cezar.dev.airquality.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Medição atual da Open-Meteo. "time" é a data-hora local ISO-8601 sem offset
 * (ex.: "2026-10-13T14:15") e "is_day" vale 1 (dia) ou 0 (noite).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoCurrent(
        Double temperature_2m,
        Double apparent_temperature,
        Integer relative_humidity_2m,
        Double precipitation,
        Integer weather_code,
        Double wind_speed_10m,
        Integer wind_direction_10m,
        Integer is_day,
        String time) {
}
