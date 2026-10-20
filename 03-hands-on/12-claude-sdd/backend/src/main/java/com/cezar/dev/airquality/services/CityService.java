package com.cezar.dev.airquality.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cezar.dev.airquality.clients.GeocodingApiClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

@Service
public class CityService {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;
    private static final int MAX_RESULTS = 10;
    private static final String EDGES = "^[\\p{IsWhite_Space}\\uFEFF]+|[\\p{IsWhite_Space}\\uFEFF]+$";

    private final GeocodingApiClient geocodingApiClient;

    public CityService(GeocodingApiClient geocodingApiClient) {
        this.geocodingApiClient = geocodingApiClient;
    }

    public List<CityResponse> search(String name) {
        String normalized = normalize(name);
        GeocodingApiResponse response = geocodingApiClient.search(normalized);
        List<GeocodingApiResponse.Result> results = response == null ? null : response.results();
        if (results == null) {
            throw new CityNotFoundException();
        }
        List<CityResponse> cities = results.stream()
                .filter(CityService::isInhabited)
                .limit(MAX_RESULTS)
                .map(CityService::toCity)
                .toList();
        if (cities.isEmpty()) {
            throw new CityNotFoundException();
        }
        return cities;
    }

    private static String normalize(String name) {
        if (name != null) {
            String normalized = name.replaceAll(EDGES, "");
            int length = normalized.codePointCount(0, normalized.length());
            if (length >= MIN_LENGTH && length <= MAX_LENGTH) {
                return normalized;
            }
        }
        throw new InvalidRequestException("INVALID_CITY_NAME",
                "Informe o nome de uma cidade com 2 a 100 caracteres.");
    }

    private static boolean isInhabited(GeocodingApiResponse.Result result) {
        return result != null && (result.featureCode() == null || result.featureCode().startsWith("PPL"));
    }

    private static CityResponse toCity(GeocodingApiResponse.Result r) {
        if (r.id() == null || r.name() == null || r.latitude() == null || r.longitude() == null) {
            throw new UpstreamException("Item incompleto na resposta da API geocoding");
        }
        return new CityResponse(r.id(), r.name(), r.admin1(), r.admin2(), r.country(), r.countryCode(),
                r.latitude(), r.longitude(), r.timezone());
    }
}
