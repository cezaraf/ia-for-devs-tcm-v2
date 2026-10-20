package com.cezar.dev.airquality.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import com.cezar.dev.airquality.services.CityService;

@WebMvcTest(CityController.class)
@Import(ApiExceptionHandler.class)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    @Test
    @DisplayName("SCN-008 200 devolve o array com todos os campos e null presente")
    void ok() throws Exception {
        when(cityService.search("São José")).thenReturn(List.of(
                new CityResponse(3448744L, "São José", "Santa Catarina", "São José", "Brasil", "BR", -27.61528,
                        -48.6275, "America/Sao_Paulo"),
                new CityResponse(2L, "Vila", null, null, null, null, 1.5, 2.5, null)));

        mockMvc.perform(get("/cities").param("name", "São José"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(3448744))
                .andExpect(jsonPath("$[0].name").value("São José"))
                .andExpect(jsonPath("$[0].region").value("Santa Catarina"))
                .andExpect(jsonPath("$[0].subregion").value("São José"))
                .andExpect(jsonPath("$[0].country").value("Brasil"))
                .andExpect(jsonPath("$[0].countryCode").value("BR"))
                .andExpect(jsonPath("$[0].latitude").value(-27.61528))
                .andExpect(jsonPath("$[0].longitude").value(-48.6275))
                .andExpect(jsonPath("$[0].timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$[1].region").hasJsonPath())
                .andExpect(jsonPath("$[1].region").isEmpty())
                .andExpect(jsonPath("$[1].subregion").hasJsonPath())
                .andExpect(jsonPath("$[1].subregion").isEmpty())
                .andExpect(jsonPath("$[1].country").hasJsonPath())
                .andExpect(jsonPath("$[1].country").isEmpty())
                .andExpect(jsonPath("$[1].countryCode").hasJsonPath())
                .andExpect(jsonPath("$[1].countryCode").isEmpty())
                .andExpect(jsonPath("$[1].timezone").hasJsonPath())
                .andExpect(jsonPath("$[1].timezone").isEmpty());
    }

    @Test
    @DisplayName("SCN-011 400 INVALID_CITY_NAME com corpo exato")
    void invalidName() throws Exception {
        when(cityService.search("a")).thenThrow(new InvalidRequestException("INVALID_CITY_NAME",
                "Informe o nome de uma cidade com 2 a 100 caracteres."));

        mockMvc.perform(get("/cities").param("name", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"))
                .andExpect(jsonPath("$.message").value("Informe o nome de uma cidade com 2 a 100 caracteres."));
    }

    @Test
    @DisplayName("SCN-011 name ausente chega ao service como null e vira 400")
    void missingName() throws Exception {
        when(cityService.search(null)).thenThrow(new InvalidRequestException("INVALID_CITY_NAME",
                "Informe o nome de uma cidade com 2 a 100 caracteres."));

        mockMvc.perform(get("/cities"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
    }

    @Test
    @DisplayName("SCN-010 404 CITY_NOT_FOUND com corpo exato")
    void notFound() throws Exception {
        when(cityService.search("Xyz")).thenThrow(new CityNotFoundException());

        mockMvc.perform(get("/cities").param("name", "Xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nenhuma cidade encontrada com esse nome."));
    }

    @Test
    @DisplayName("SCN-012 502 UPSTREAM_ERROR com corpo só com code e message")
    void upstreamError() throws Exception {
        when(cityService.search("Recife")).thenThrow(new UpstreamException("detalhe interno"));

        mockMvc.perform(get("/cities").param("name", "Recife"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("O serviço de clima está indisponível no momento. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("SCN-013 504 UPSTREAM_TIMEOUT com mensagem PT-BR exata")
    void upstreamTimeout() throws Exception {
        when(cityService.search("Recife")).thenThrow(new UpstreamTimeoutException("detalhe interno"));

        mockMvc.perform(get("/cities").param("name", "Recife"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"))
                .andExpect(jsonPath("$.message")
                        .value("O serviço de clima demorou demais para responder. Tente novamente."));
    }
}
