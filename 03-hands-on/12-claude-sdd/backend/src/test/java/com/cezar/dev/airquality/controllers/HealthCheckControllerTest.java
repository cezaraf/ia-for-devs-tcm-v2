package com.cezar.dev.airquality.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthCheckController.class)
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SCN-001 GET /healthcheck responde 200 com corpo OK")
    void healthcheckRespondeOk() throws Exception {
        mockMvc.perform(get("/healthcheck"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
