package com.cezar.dev.airquality.controllers;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa GET /weather de ponta a ponta (controller → service → OpenMeteoClient), com a
 * Open-Meteo simulada por {@link MockRestServiceServer}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class WeatherControllerTest {

    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=-25.43&longitude=-49.27"
                    + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,"
                    + "weather_code,wind_speed_10m,wind_direction_10m,is_day&wind_speed_unit=kmh&timezone=auto";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer openMeteo;

    @Test
    void deveRetornarClimaAtual() throws Exception {
        this.openMeteo.expect(requestTo(FORECAST_URL))
                .andRespond(withSuccess("""
                        {
                          "latitude": -25.4296,
                          "longitude": -49.2713,
                          "timezone": "America/Sao_Paulo",
                          "current": {
                            "time": "2026-10-13T14:15",
                            "temperature_2m": 21.4,
                            "apparent_temperature": 24.1,
                            "relative_humidity_2m": 78,
                            "precipitation": 0.0,
                            "weather_code": 2,
                            "wind_speed_10m": 12.3,
                            "wind_direction_10m": 45,
                            "is_day": 1
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        this.mockMvc.perform(get("/weather").param("latitude", "-25.43").param("longitude", "-49.27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(-25.43))
                .andExpect(jsonPath("$.longitude").value(-49.27))
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.observedAt").value("2026-10-13T14:15"))
                .andExpect(jsonPath("$.temperature").value(21.4))
                .andExpect(jsonPath("$.apparentTemperature").value(24.1))
                .andExpect(jsonPath("$.relativeHumidity").value(78))
                .andExpect(jsonPath("$.precipitation").value(0.0))
                .andExpect(jsonPath("$.windSpeed").value(12.3))
                .andExpect(jsonPath("$.windDirection").value(45))
                .andExpect(jsonPath("$.windDirectionCardinal").value("NE"))
                .andExpect(jsonPath("$.weatherCode").value(2))
                .andExpect(jsonPath("$.condition").value("Parcialmente nublado"))
                .andExpect(jsonPath("$.isDay").value(true));
    }

    @Test
    void deveRetornar400QuandoLatitudeForaDoIntervalo() throws Exception {
        this.mockMvc.perform(get("/weather").param("latitude", "200").param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void deveRetornar400QuandoLongitudeNaoForNumerica() throws Exception {
        this.mockMvc.perform(get("/weather").param("latitude", "-25.43").param("longitude", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void deveRetornar400QuandoCoordenadasAusentes() throws Exception {
        this.mockMvc.perform(get("/weather").param("latitude", "-25.43"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void deveRetornar502QuandoOpenMeteoFalha() throws Exception {
        this.openMeteo.expect(requestTo(FORECAST_URL))
                .andRespond(withServerError());

        this.mockMvc.perform(get("/weather").param("latitude", "-25.43").param("longitude", "-49.27"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"));
    }

    @Test
    void deveRetornar504QuandoOpenMeteoExcedeOTempo() throws Exception {
        this.openMeteo.expect(requestTo(FORECAST_URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        this.mockMvc.perform(get("/weather").param("latitude", "-25.43").param("longitude", "-49.27"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"));
    }
}
