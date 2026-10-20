package com.cezar.dev.airquality.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** JSON da busca de cidades da Open-Meteo. Sem resultados, a chave "results" não vem. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoGeocodingResponse(List<OpenMeteoPlace> results) {
}
