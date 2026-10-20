package com.cezar.dev.airquality;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.OpenMeteoForecastResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoGeocodingResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoClientTest {

    private MockRestServiceServer mockServer;
    private OpenMeteoClient openMeteoClient;

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        openMeteoClient = new OpenMeteoClient(builder.build(), GEOCODING_URL, FORECAST_URL);
    }

    @Test
    @DisplayName("searchCities: returns OpenMeteoGeocodingResponse on 200 OK")
    void testSearchCitiesSuccess() {
        String json = """
                {
                  "results": [
                    {
                      "id": 3448439,
                      "name": "São Paulo",
                      "latitude": -23.5475,
                      "longitude": -46.63611,
                      "country_code": "BR",
                      "country": "Brasil",
                      "admin1": "São Paulo"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://geocoding-api.open-meteo.com/v1/search?name=S%C3%A3o%20Paulo&count=10&language=pt&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        OpenMeteoGeocodingResponse response = openMeteoClient.searchCities("São Paulo");

        mockServer.verify();
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().name()).isEqualTo("São Paulo");
        assertThat(response.results().getFirst().countryCode()).isEqualTo("BR");
    }

    @Test
    @DisplayName("searchCities: throws UpstreamException when OpenMeteo returns 500")
    void testSearchCitiesUpstreamError() {
        mockServer.expect(requestTo("https://geocoding-api.open-meteo.com/v1/search?name=Error&count=10&language=pt&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> openMeteoClient.searchCities("Error"))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("O serviço de clima está indisponível no momento");

        mockServer.verify();
    }

    @Test
    @DisplayName("getWeather: returns OpenMeteoForecastResponse on 200 OK")
    void testGetWeatherSuccess() {
        String json = """
                {
                  "latitude": -23.55,
                  "longitude": -46.63,
                  "timezone": "America/Sao_Paulo",
                  "current": {
                    "time": "2026-10-13T14:15",
                    "temperature_2m": 22.4,
                    "apparent_temperature": 21.8,
                    "relative_humidity_2m": 72,
                    "precipitation": 0.0,
                    "weather_code": 1,
                    "wind_speed_10m": 12.3,
                    "wind_direction_10m": 45,
                    "is_day": 1
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.open-meteo.com/v1/forecast?latitude=-23.55&longitude=-46.63&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        OpenMeteoForecastResponse response = openMeteoClient.getWeather(-23.55, -46.63);

        mockServer.verify();
        assertThat(response.latitude()).isEqualTo(-23.55);
        assertThat(response.current().temperature2m()).isEqualTo(22.4);
        assertThat(response.current().weatherCode()).isEqualTo(1);
    }

    @Test
    @DisplayName("getWeather: throws UpstreamException on 502/500")
    void testGetWeatherUpstreamError() {
        mockServer.expect(requestTo("https://api.open-meteo.com/v1/forecast?latitude=-23.55&longitude=-46.63&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> openMeteoClient.getWeather(-23.55, -46.63))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("O serviço de clima está indisponível no momento");

        mockServer.verify();
    }
}
