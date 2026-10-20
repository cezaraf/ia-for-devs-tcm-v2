package com.cezar.dev.airquality;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.controllers.ApiExceptionHandler;
import com.cezar.dev.airquality.controllers.WeatherController;
import com.cezar.dev.airquality.services.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeatherControllerTest {

    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        OpenMeteoClient openMeteoClient = new OpenMeteoClient(
                builder.build(),
                "https://geocoding-api.open-meteo.com/v1/search",
                "https://api.open-meteo.com/v1/forecast"
        );
        WeatherService weatherService = new WeatherService(openMeteoClient);
        WeatherController weatherController = new WeatherController(weatherService);

        mockMvc = MockMvcBuilders.standaloneSetup(weatherController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /weather?latitude=-25.42&longitude=-49.27 - 200 OK")
    void testGetWeatherSuccess() throws Exception {
        String json = """
                {
                  "latitude": -25.42,
                  "longitude": -49.27,
                  "timezone": "America/Sao_Paulo",
                  "current": {
                    "time": "2026-10-13T14:15",
                    "temperature_2m": 24.1,
                    "apparent_temperature": 23.8,
                    "relative_humidity_2m": 75,
                    "precipitation": 0.0,
                    "weather_code": 0,
                    "wind_speed_10m": 11.2,
                    "wind_direction_10m": 90,
                    "is_day": 1
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.open-meteo.com/v1/forecast?latitude=-25.42&longitude=-49.27&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/weather")
                        .param("latitude", "-25.42")
                        .param("longitude", "-49.27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude", is(-25.42)))
                .andExpect(jsonPath("$.longitude", is(-49.27)))
                .andExpect(jsonPath("$.timezone", is("America/Sao_Paulo")))
                .andExpect(jsonPath("$.observedAt", is("2026-10-13T14:15")))
                .andExpect(jsonPath("$.temperature", is(24.1)))
                .andExpect(jsonPath("$.apparentTemperature", is(23.8)))
                .andExpect(jsonPath("$.relativeHumidity", is(75)))
                .andExpect(jsonPath("$.precipitation", is(0.0)))
                .andExpect(jsonPath("$.windSpeed", is(11.2)))
                .andExpect(jsonPath("$.windDirection", is(90)))
                .andExpect(jsonPath("$.windDirectionCardinal", is("E")))
                .andExpect(jsonPath("$.weatherCode", is(0)))
                .andExpect(jsonPath("$.condition", is("Céu limpo")))
                .andExpect(jsonPath("$.isDay", is(true)));

        mockServer.verify();
    }

    @Test
    @DisplayName("GET /weather?latitude=200&longitude=0 - 400 INVALID_COORDINATES")
    void testGetWeatherCoordinatesOutOfBounds() throws Exception {
        mockMvc.perform(get("/weather")
                        .param("latitude", "200")
                        .param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
    }

    @Test
    @DisplayName("GET /weather?latitude=abc&longitude=0 - 400 INVALID_COORDINATES")
    void testGetWeatherNonNumericCoordinates() throws Exception {
        mockMvc.perform(get("/weather")
                        .param("latitude", "abc")
                        .param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
    }

    @Test
    @DisplayName("GET /weather sem parâmetros - 400 INVALID_COORDINATES")
    void testGetWeatherMissingCoordinates() throws Exception {
        mockMvc.perform(get("/weather"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
    }

    @Test
    @DisplayName("GET /weather com upstream 502 - 502 UPSTREAM_ERROR")
    void testGetWeatherUpstreamError() throws Exception {
        mockServer.expect(requestTo("https://api.open-meteo.com/v1/forecast?latitude=0.0&longitude=0.0&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        mockMvc.perform(get("/weather")
                        .param("latitude", "0.0")
                        .param("longitude", "0.0"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code", is("UPSTREAM_ERROR")))
                .andExpect(jsonPath("$.message", is("O serviço de clima está indisponível no momento.")));

        mockServer.verify();
    }

    @Test
    @DisplayName("GET /weather com timeout - 504 UPSTREAM_TIMEOUT")
    void testGetWeatherUpstreamTimeout() throws Exception {
        mockServer.expect(requestTo("https://api.open-meteo.com/v1/forecast?latitude=0.0&longitude=0.0&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        mockMvc.perform(get("/weather")
                        .param("latitude", "0.0")
                        .param("longitude", "0.0"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code", is("UPSTREAM_TIMEOUT")))
                .andExpect(jsonPath("$.message", is("O serviço de clima demorou para responder.")));

        mockServer.verify();
    }
}
