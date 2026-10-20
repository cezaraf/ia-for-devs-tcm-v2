package com.cezar.dev.airquality.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.openmeteo.GeocodingResult;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;

@Service
public class CityService {

    private final OpenMeteoClient openMeteoClient;

    public CityService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public List<CityResponse> searchCities(String name) {
        String trimmed = validateCityName(name);
        List<GeocodingResult> results = openMeteoClient.searchCities(trimmed);
        if (results.isEmpty()) {
            throw new CityNotFoundException("Cidade não encontrada.");
        }
        return results.stream().map(this::toCityResponse).toList();
    }

    static String validateCityName(String name) {
        if (name == null) {
            throw new InvalidRequestException("INVALID_CITY_NAME", "Nome da cidade inválido.");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new InvalidRequestException("INVALID_CITY_NAME", "Nome da cidade inválido.");
        }
        return trimmed;
    }

    private CityResponse toCityResponse(GeocodingResult result) {
        return new CityResponse(
                result.id(),
                result.name(),
                result.admin1(),
                result.country(),
                result.countryCode(),
                result.latitude(),
                result.longitude());
    }
}
