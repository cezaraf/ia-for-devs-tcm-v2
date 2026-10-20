package com.cezar.dev.airquality.services;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoGeocodingResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityService {

    private final OpenMeteoClient openMeteoClient;

    public CityService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public List<CityResponse> searchCities(String name) {
        if (name == null) {
            throw new InvalidRequestException("INVALID_CITY_NAME", "Nome da cidade é obrigatório.");
        }

        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new InvalidRequestException("INVALID_CITY_NAME", "Nome da cidade deve conter entre 2 e 100 caracteres.");
        }

        OpenMeteoGeocodingResponse response = openMeteoClient.searchCities(trimmed);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new CityNotFoundException("CITY_NOT_FOUND", "Cidade não encontrada. Verifique o nome e tente novamente.");
        }

        return response.results().stream()
                .limit(10)
                .map(item -> new CityResponse(
                        item.id(),
                        item.name(),
                        item.admin1(),
                        item.country(),
                        item.countryCode(),
                        item.latitude(),
                        item.longitude()
                ))
                .toList();
    }
}
