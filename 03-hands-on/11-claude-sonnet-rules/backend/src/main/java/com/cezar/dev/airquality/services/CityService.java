package com.cezar.dev.airquality.services;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CityService {

    static final int MIN_LENGTH = 2;
    static final int MAX_LENGTH = 100;
    private static final int MAX_RESULTS = 10;

    private final OpenMeteoClient client;

    public CityService(OpenMeteoClient client) {
        this.client = client;
    }

    public List<CityResponse> search(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new InvalidRequestException("INVALID_CITY_NAME",
                    "O nome da cidade deve ter entre 2 e 100 caracteres.");
        }
        List<GeocodingApiResponse.Result> results = client.searchCities(trimmed);
        if (results.isEmpty()) {
            throw new CityNotFoundException();
        }
        return results.stream().limit(MAX_RESULTS).map(CityService::toResponse).toList();
    }

    private static CityResponse toResponse(GeocodingApiResponse.Result r) {
        return new CityResponse(r.id(), r.name(), r.admin1(), r.country(), r.countryCode(),
                r.latitude(), r.longitude());
    }
}
