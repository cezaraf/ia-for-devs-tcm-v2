package com.cezar.dev.airquality.controllers;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class WeatherControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void getWeather_returns200() throws Exception {
        mockServer.expect(requestTo(Matchers.containsString("api.open-meteo.com")))
                .andRespond(withSuccess("""
                        {
                          "latitude": -25.42,
                          "longitude": -49.27,
                          "timezone": "America/Sao_Paulo",
                          "current": {
                            "time": "2026-10-13T14:15",
                            "temperature_2m": 22.3,
                            "apparent_temperature": 24.0,
                            "relative_humidity_2m": 78,
                            "precipitation": 0.0,
                            "weather_code": 0,
                            "wind_speed_10m": 12.0,
                            "wind_direction_10m": 45,
                            "is_day": 1
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/weather").param("latitude", "-25.42").param("longitude", "-49.27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature").value(22.3))
                .andExpect(jsonPath("$.condition").value("Céu limpo"))
                .andExpect(jsonPath("$.windDirectionCardinal").value("NE"))
                .andExpect(jsonPath("$.isDay").value(true));
        mockServer.verify();
    }

    @Test
    void getWeather_returns400ForInvalidCoordinates() throws Exception {
        mockMvc.perform(get("/weather").param("latitude", "200").param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void getWeather_returns502OnUpstreamError() throws Exception {
        mockServer.expect(requestTo(Matchers.containsString("api.open-meteo.com")))
                .andRespond(MockRestResponseCreators.withServerError());

        mockMvc.perform(get("/weather").param("latitude", "-25").param("longitude", "-49"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"));
        mockServer.verify();
    }

    @Test
    void getWeather_returns504OnTimeout() throws Exception {
        mockServer.expect(requestTo(Matchers.containsString("api.open-meteo.com")))
                .andRespond((request) -> {
                    throw new org.springframework.web.client.ResourceAccessException(
                            "I/O error on GET request",
                            new java.net.SocketTimeoutException("Read timed out"));
                });

        mockMvc.perform(get("/weather").param("latitude", "-25").param("longitude", "-49"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"));
        mockServer.verify();
    }
}
