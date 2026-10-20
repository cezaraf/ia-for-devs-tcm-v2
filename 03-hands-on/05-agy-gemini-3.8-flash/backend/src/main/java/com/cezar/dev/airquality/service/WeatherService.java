package com.cezar.dev.airquality.service;

import com.cezar.dev.airquality.dto.BigDataCloudReverseResponse;
import com.cezar.dev.airquality.dto.OpenMeteoGeocodingResponse;
import com.cezar.dev.airquality.dto.OpenMeteoWeatherResponse;
import com.cezar.dev.airquality.dto.WeatherResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.WeatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private static final String GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String REVERSE_GEO_URL = "https://api.bigdatacloud.net/data/reverse-geocode-client";

    private final RestClient restClient;
    private final WeatherCodeMapper weatherCodeMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public WeatherService(org.springframework.web.client.RestClient.Builder restClientBuilder, WeatherCodeMapper weatherCodeMapper) {
        this.restClient = restClientBuilder.build();
        this.weatherCodeMapper = weatherCodeMapper;
    }

    public WeatherService(RestClient restClient, WeatherCodeMapper weatherCodeMapper) {
        this.restClient = restClient;
        this.weatherCodeMapper = weatherCodeMapper;
    }

    public WeatherResponse getWeatherByCity(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da cidade deve ser informado.");
        }

        String cleanedCity = cityName.trim();
        log.info("Buscando coordenadas para a cidade: {}", cleanedCity);

        OpenMeteoGeocodingResponse geoResponse;
        try {
            geoResponse = restClient.get()
                .uri(GEOCODING_BASE_URL + "?name={name}&count=1&language=pt&format=json", cleanedCity)
                .retrieve()
                .body(OpenMeteoGeocodingResponse.class);
        } catch (Exception e) {
            log.error("Erro ao consultar Open-Meteo Geocoding API para {}", cleanedCity, e);
            throw new WeatherException("Erro ao buscar coordenadas da cidade: " + e.getMessage(), e);
        }

        if (geoResponse == null || geoResponse.results() == null || geoResponse.results().isEmpty()) {
            throw new CityNotFoundException("Cidade não encontrada: " + cleanedCity);
        }

        OpenMeteoGeocodingResponse.GeocodingResult location = geoResponse.results().getFirst();
        return fetchForecast(
            location.name(),
            location.admin1(),
            location.country(),
            location.latitude(),
            location.longitude(),
            location.timezone()
        );
    }

    public WeatherResponse getWeatherByCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude e longitude são obrigatórias.");
        }

        log.info("Buscando clima por coordenadas: lat={}, lon={}", latitude, longitude);

        String cityName = "Localização Atual";
        String state = "";
        String country = "";

        try {
            BigDataCloudReverseResponse reverseResponse = restClient.get()
                .uri(REVERSE_GEO_URL + "?latitude={lat}&longitude={lon}&localityLanguage=pt", latitude, longitude)
                .retrieve()
                .body(BigDataCloudReverseResponse.class);

            if (reverseResponse != null) {
                if (reverseResponse.city() != null && !reverseResponse.city().isBlank()) {
                    cityName = reverseResponse.city();
                } else if (reverseResponse.locality() != null && !reverseResponse.locality().isBlank()) {
                    cityName = reverseResponse.locality();
                }
                state = reverseResponse.principalSubdivision() != null ? reverseResponse.principalSubdivision() : "";
                country = reverseResponse.countryName() != null ? reverseResponse.countryName() : "";
            }
        } catch (Exception e) {
            log.warn("Falha no reverse geocode para lat={}, lon={}. Usando fallback. Erro: {}", latitude, longitude, e.getMessage());
        }

        return fetchForecast(cityName, state, country, latitude, longitude, "auto");
    }

    private WeatherResponse fetchForecast(
        String cityName,
        String state,
        String country,
        Double latitude,
        Double longitude,
        String timezone
    ) {
        log.info("Buscando previsão do tempo para lat={}, lon={}", latitude, longitude);

        OpenMeteoWeatherResponse weatherResponse;
        try {
            weatherResponse = restClient.get()
                .uri(WEATHER_BASE_URL + "?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto",
                    latitude, longitude)
                .retrieve()
                .body(OpenMeteoWeatherResponse.class);
        } catch (Exception e) {
            log.error("Erro ao consultar Open-Meteo Forecast API para lat={}, lon={}", latitude, longitude, e);
            throw new WeatherException("Erro ao buscar dados de clima: " + e.getMessage(), e);
        }

        if (weatherResponse == null || weatherResponse.current() == null) {
            throw new WeatherException("Dados de clima indisponíveis para a localidade informada.");
        }

        OpenMeteoWeatherResponse.CurrentWeather cur = weatherResponse.current();
        boolean isDay = cur.isDay() != null && cur.isDay() == 1;
        WeatherCodeMapper.WeatherDescription desc = weatherCodeMapper.map(cur.weatherCode(), isDay);

        WeatherResponse.LocationInfo locationInfo = new WeatherResponse.LocationInfo(
            cityName,
            state,
            country,
            latitude,
            longitude,
            weatherResponse.timezone() != null ? weatherResponse.timezone() : timezone
        );

        WeatherResponse.CurrentWeatherInfo currentInfo = new WeatherResponse.CurrentWeatherInfo(
            cur.temperature2m(),
            cur.apparentTemperature(),
            cur.relativeHumidity2m(),
            cur.precipitation(),
            cur.windSpeed10m(),
            cur.windDirection10m(),
            cur.cloudCover(),
            cur.weatherCode(),
            desc.condition(),
            desc.icon(),
            isDay,
            cur.time()
        );

        List<WeatherResponse.DailyForecastInfo> dailyList = new ArrayList<>();
        if (weatherResponse.daily() != null && weatherResponse.daily().time() != null) {
            var daily = weatherResponse.daily();
            int count = daily.time().size();
            for (int i = 0; i < count; i++) {
                String date = daily.time().get(i);
                Double max = (daily.temperature2mMax() != null && i < daily.temperature2mMax().size()) ? daily.temperature2mMax().get(i) : null;
                Double min = (daily.temperature2mMin() != null && i < daily.temperature2mMin().size()) ? daily.temperature2mMin().get(i) : null;
                Integer code = (daily.weatherCode() != null && i < daily.weatherCode().size()) ? daily.weatherCode().get(i) : null;
                WeatherCodeMapper.WeatherDescription dailyDesc = weatherCodeMapper.map(code, true);

                dailyList.add(new WeatherResponse.DailyForecastInfo(
                    date,
                    max,
                    min,
                    code,
                    dailyDesc.condition(),
                    dailyDesc.icon()
                ));
            }
        }

        return new WeatherResponse(locationInfo, currentInfo, dailyList);
    }
}
