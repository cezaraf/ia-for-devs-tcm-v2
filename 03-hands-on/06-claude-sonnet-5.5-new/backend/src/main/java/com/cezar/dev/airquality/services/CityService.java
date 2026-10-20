package com.cezar.dev.airquality.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse.GeocodingApiResult;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

@Service
public class CityService {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;

    private final OpenMeteoClient client;

    public CityService(OpenMeteoClient client) {
        this.client = client;
    }

    public List<CityResponse> search(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidRequestException("INVALID_CITY_NAME",
                    "O nome da cidade deve ter entre 2 e 100 caracteres.");
        }

        List<CityResponse> cities = client.searchCities(trimmed).stream()
                .map(this::toResponse)
                .toList();
        if (cities.isEmpty()) {
            throw new CityNotFoundException();
        }
        return cities;
    }

    private CityResponse toResponse(GeocodingApiResult result) {
        if (result.id() == null || result.name() == null
                || result.latitude() == null || result.longitude() == null) {
            throw new UpstreamException("Resultado de geocodificação incompleto.");
        }
        return new CityResponse(result.id(), result.name(), result.admin1(), result.country(),
                result.countryCode(), result.latitude(), result.longitude());
    }
}
