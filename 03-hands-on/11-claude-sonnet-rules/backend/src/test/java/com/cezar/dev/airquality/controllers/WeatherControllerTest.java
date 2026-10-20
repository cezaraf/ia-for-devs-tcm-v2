package com.cezar.dev.airquality.controllers;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import com.cezar.dev.airquality.services.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
@Import({ApiExceptionHandler.class, WeatherService.class})
class WeatherControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OpenMeteoClient client;

    @Test
    void retorna200ComContratoCompleto() throws Exception {
        when(client.fetchForecast(-25.43, -49.27)).thenReturn(new ForecastApiResponse(
                -25.43, -49.27, "America/Sao_Paulo",
                new ForecastApiResponse.Current("2026-10-13T14:15", 23.5, 24.0, 78.0, 0.0, 3, 12.4, 45.0, 0)));

        mvc.perform(get("/weather").param("latitude", "-25.43").param("longitude", "-49.27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(-25.43))
                .andExpect(jsonPath("$.longitude").value(-49.27))
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.observedAt").value("2026-10-13T14:15"))
                .andExpect(jsonPath("$.temperature").value(23.5))
                .andExpect(jsonPath("$.apparentTemperature").value(24.0))
                .andExpect(jsonPath("$.relativeHumidity").value(78))
                .andExpect(jsonPath("$.precipitation").value(0.0))
                .andExpect(jsonPath("$.windSpeed").value(12.4))
                .andExpect(jsonPath("$.windDirection").value(45))
                .andExpect(jsonPath("$.windDirectionCardinal").value("NE"))
                .andExpect(jsonPath("$.weatherCode").value(3))
                .andExpect(jsonPath("$.condition").value("Nublado"))
                .andExpect(jsonPath("$.isDay").value(false))
                .andExpect(jsonPath("$.day").doesNotExist());
    }

    @Test
    void retorna400ParaCoordenadasInvalidas() throws Exception {
        mvc.perform(get("/weather").param("latitude", "200").param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(get("/weather").param("latitude", "0").param("longitude", "-181"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        mvc.perform(get("/weather").param("latitude", "abc").param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        mvc.perform(get("/weather").param("latitude", "NaN").param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        verifyNoInteractions(client);
    }

    @Test
    void retorna400QuandoParametrosAusentes() throws Exception {
        mvc.perform(get("/weather"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        mvc.perform(get("/weather").param("latitude", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void retorna502QuandoUpstreamFalha() throws Exception {
        when(client.fetchForecast(anyDouble(), anyDouble())).thenThrow(new UpstreamException());

        mvc.perform(get("/weather").param("latitude", "1").param("longitude", "2"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").value("Falha ao consultar o serviço de clima."));
    }

    @Test
    void retorna504QuandoUpstreamDemora() throws Exception {
        when(client.fetchForecast(anyDouble(), anyDouble())).thenThrow(new UpstreamTimeoutException());

        mvc.perform(get("/weather").param("latitude", "1").param("longitude", "2"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("O serviço de clima demorou para responder."));
    }
}
