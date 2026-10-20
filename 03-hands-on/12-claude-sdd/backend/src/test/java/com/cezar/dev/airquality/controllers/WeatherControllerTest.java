package com.cezar.dev.airquality.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import com.cezar.dev.airquality.services.WeatherService;

@WebMvcTest(WeatherController.class)
@Import(ApiExceptionHandler.class)
class WeatherControllerTest {

    private static final String INVALID_MESSAGE =
            "Coordenadas inválidas. Use latitude entre -90 e 90 e longitude entre -180 e 180.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    @DisplayName("SCN-014 200 devolve o JSON do contrato")
    void ok() throws Exception {
        when(weatherService.current("-8.05", "-34.88")).thenReturn(new WeatherResponse(-8.05, -34.88,
                "America/Recife", "2026-10-20T16:00", 28.6, 30.8, 62, 12.7, 111.0, "Leste", 1,
                "Predominantemente limpo", true));

        mockMvc.perform(get("/weather").param("latitude", "-8.05").param("longitude", "-34.88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$.latitude").value(-8.05))
                .andExpect(jsonPath("$.longitude").value(-34.88))
                .andExpect(jsonPath("$.timezone").value("America/Recife"))
                .andExpect(jsonPath("$.observedAt").value("2026-10-20T16:00"))
                .andExpect(jsonPath("$.temperature").value(28.6))
                .andExpect(jsonPath("$.apparentTemperature").value(30.8))
                .andExpect(jsonPath("$.humidity").value(62))
                .andExpect(jsonPath("$.windSpeed").value(12.7))
                .andExpect(jsonPath("$.windDirectionDegrees").value(111.0))
                .andExpect(jsonPath("$.windDirection").value("Leste"))
                .andExpect(jsonPath("$.weatherCode").value(1))
                .andExpect(jsonPath("$.condition").value("Predominantemente limpo"))
                .andExpect(jsonPath("$.isDay").value(true));
    }

    @Test
    @DisplayName("SCN-015 campos nulos presentes no JSON")
    void nullFieldsPresent() throws Exception {
        when(weatherService.current("1", "2")).thenReturn(new WeatherResponse(1, 2, null, "2026-10-20T16:00",
                null, null, null, null, null, null, null, "Condição desconhecida", null));

        mockMvc.perform(get("/weather").param("latitude", "1").param("longitude", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$.timezone").hasJsonPath())
                .andExpect(jsonPath("$.timezone").isEmpty())
                .andExpect(jsonPath("$.temperature").hasJsonPath())
                .andExpect(jsonPath("$.temperature").isEmpty())
                .andExpect(jsonPath("$.apparentTemperature").hasJsonPath())
                .andExpect(jsonPath("$.apparentTemperature").isEmpty())
                .andExpect(jsonPath("$.humidity").hasJsonPath())
                .andExpect(jsonPath("$.humidity").isEmpty())
                .andExpect(jsonPath("$.windSpeed").hasJsonPath())
                .andExpect(jsonPath("$.windSpeed").isEmpty())
                .andExpect(jsonPath("$.windDirectionDegrees").hasJsonPath())
                .andExpect(jsonPath("$.windDirectionDegrees").isEmpty())
                .andExpect(jsonPath("$.windDirection").hasJsonPath())
                .andExpect(jsonPath("$.windDirection").isEmpty())
                .andExpect(jsonPath("$.weatherCode").hasJsonPath())
                .andExpect(jsonPath("$.weatherCode").isEmpty())
                .andExpect(jsonPath("$.isDay").hasJsonPath())
                .andExpect(jsonPath("$.isDay").isEmpty());
    }

    @Test
    @DisplayName("SCN-018 400 INVALID_COORDINATES com corpo exato")
    void invalidCoordinates() throws Exception {
        when(weatherService.current("abc", "10"))
                .thenThrow(new InvalidRequestException("INVALID_COORDINATES", INVALID_MESSAGE));

        mockMvc.perform(get("/weather").param("latitude", "abc").param("longitude", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"))
                .andExpect(jsonPath("$.message").value(INVALID_MESSAGE));
    }

    @Test
    @DisplayName("SCN-018 parâmetros ausentes chegam ao service como null e viram 400")
    void missingParams() throws Exception {
        when(weatherService.current(null, null))
                .thenThrow(new InvalidRequestException("INVALID_COORDINATES", INVALID_MESSAGE));

        mockMvc.perform(get("/weather"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    @DisplayName("SCN-019 502 UPSTREAM_ERROR sem o reason do provedor")
    void upstreamError() throws Exception {
        when(weatherService.current("1", "2")).thenThrow(new UpstreamException("reason do provedor"));

        mockMvc.perform(get("/weather").param("latitude", "1").param("longitude", "2"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("O serviço de clima está indisponível no momento. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("SCN-019 504 UPSTREAM_TIMEOUT com mensagem PT-BR exata")
    void upstreamTimeout() throws Exception {
        when(weatherService.current("1", "2")).thenThrow(new UpstreamTimeoutException("detalhe interno"));

        mockMvc.perform(get("/weather").param("latitude", "1").param("longitude", "2"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"))
                .andExpect(jsonPath("$.message")
                        .value("O serviço de clima demorou demais para responder. Tente novamente."));
    }
}
