package com.cezar.dev.airquality.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    private static final String SEGREDO = "segredo-interno-java";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("SCN-002 InvalidRequestException vira 400 com code e message da exceção")
    void invalidRequest() throws Exception {
        mockMvc.perform(get("/throw/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"))
                .andExpect(jsonPath("$.message")
                        .value("Informe o nome de uma cidade com 2 a 100 caracteres."));
    }

    @Test
    @DisplayName("SCN-002 CityNotFoundException vira 404 CITY_NOT_FOUND")
    void cityNotFound() throws Exception {
        mockMvc.perform(get("/throw/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nenhuma cidade encontrada com esse nome."));
    }

    @Test
    @DisplayName("SCN-002 UpstreamException vira 502 UPSTREAM_ERROR sem vazar a causa")
    void upstream() throws Exception {
        mockMvc.perform(get("/throw/upstream"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "O serviço de clima está indisponível no momento. Tente novamente em instantes."))
                .andExpect(content().string(not(containsString(SEGREDO))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    @DisplayName("SCN-002 UpstreamTimeoutException vira 504 UPSTREAM_TIMEOUT sem vazar a causa")
    void upstreamTimeout() throws Exception {
        mockMvc.perform(get("/throw/timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"))
                .andExpect(jsonPath("$.message")
                        .value("O serviço de clima demorou demais para responder. Tente novamente."))
                .andExpect(content().string(not(containsString(SEGREDO))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/throw/{kind}")
        String doThrow(@PathVariable String kind) {
            return switch (kind) {
                case "invalid" -> throw new InvalidRequestException("INVALID_CITY_NAME",
                        "Informe o nome de uma cidade com 2 a 100 caracteres.");
                case "not-found" -> throw new CityNotFoundException();
                case "upstream" -> throw new UpstreamException(SEGREDO,
                        new RuntimeException(SEGREDO));
                case "timeout" -> throw new UpstreamTimeoutException(SEGREDO,
                        new SocketTimeoutException(SEGREDO));
                default -> "ok";
            };
        }
    }
}
