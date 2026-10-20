package com.cezar.dev.airquality.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import com.cezar.dev.airquality.services.CityService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CityController.class)
@Import({ApiExceptionHandler.class, CityService.class})
class CityControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OpenMeteoClient client;

    @Test
    void retorna200ComListaDeCidades() throws Exception {
        when(client.searchCities("Curitiba")).thenReturn(List.of(
                new GeocodingApiResponse.Result(1L, "Curitiba", "Paraná", "Brasil", "BR", -25.43, -49.27),
                new GeocodingApiResponse.Result(2L, "Outra", null, null, null, 1.0, 2.0)));

        mvc.perform(get("/cities").param("name", " Curitiba "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Curitiba"))
                .andExpect(jsonPath("$[0].admin1").value("Paraná"))
                .andExpect(jsonPath("$[0].country").value("Brasil"))
                .andExpect(jsonPath("$[0].countryCode").value("BR"))
                .andExpect(jsonPath("$[0].latitude").value(-25.43))
                .andExpect(jsonPath("$[0].longitude").value(-49.27))
                .andExpect(jsonPath("$[1].admin1").isEmpty())
                .andExpect(jsonPath("$[1].countryCode").isEmpty());
    }

    @Test
    void retorna400QuandoNomeAusente() throws Exception {
        mvc.perform(get("/cities"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void retorna400QuandoNomeCurtoOuVazio() throws Exception {
        mvc.perform(get("/cities").param("name", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
        mvc.perform(get("/cities").param("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
    }

    @Test
    void retorna400QuandoNomeComMaisDe100Caracteres() throws Exception {
        mvc.perform(get("/cities").param("name", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
    }

    @Test
    void retorna404QuandoSemResultados() throws Exception {
        when(client.searchCities("Xyzabcqwe")).thenReturn(List.of());

        mvc.perform(get("/cities").param("name", "Xyzabcqwe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cidade não encontrada."));
    }

    @Test
    void retorna502QuandoUpstreamFalha() throws Exception {
        when(client.searchCities(anyString())).thenThrow(new UpstreamException());

        mvc.perform(get("/cities").param("name", "Rio"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").value("Falha ao consultar o serviço de clima."));
    }

    @Test
    void retorna504QuandoUpstreamDemora() throws Exception {
        when(client.searchCities(anyString())).thenThrow(new UpstreamTimeoutException());

        mvc.perform(get("/cities").param("name", "Rio"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("O serviço de clima demorou para responder."));
    }
}
