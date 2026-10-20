package com.cezar.dev.airquality.services;

import java.util.List;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoPlace;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;

import org.springframework.stereotype.Service;

/** Valida a busca e converte o resultado da Open-Meteo em CityResponse. */
@Service
public class CityService {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;

    private final OpenMeteoClient openMeteoClient;

    public CityService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public List<CityResponse> searchCity(String name) {
        String trimmedName = validateName(name);
        List<OpenMeteoPlace> results = this.openMeteoClient.searchCities(trimmedName);
        if (results.isEmpty()) {
            throw new CityNotFoundException("Cidade não encontrada.");
        }
        return results.stream()
                .limit(10)
                .map(CityService::toResponse)
                .toList();
    }

    private String validateName(String name) {
        if (name == null) {
            throw new InvalidRequestException("INVALID_CITY_NAME", "Informe o nome da cidade.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidRequestException("INVALID_CITY_NAME",
                    "O nome da cidade deve ter entre 2 e 100 caracteres.");
        }
        return trimmed;
    }

    private static CityResponse toResponse(OpenMeteoPlace place) {
        return new CityResponse(
                place.id() != null ? place.id() : 0L,
                place.name() != null ? place.name() : "",
                place.admin1(),
                place.country(),
                place.countryCode(),
                place.latitude() != null ? place.latitude() : 0.0,
                place.longitude() != null ? place.longitude() : 0.0);
    }
}
