package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dto.WeatherResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    void testGetWeatherByCitySuccess() throws Exception {
        WeatherResponse mockResponse = new WeatherResponse(
            new WeatherResponse.LocationInfo("Salvador", "Bahia", "Brasil", -12.97, -38.51, "America/Bahia"),
            new WeatherResponse.CurrentWeatherInfo(28.0, 30.0, 70, 0.0, 15.0, 90, 10, 0, "Céu limpo", "☀️", true, "2026-10-13T12:00"),
            List.of(new WeatherResponse.DailyForecastInfo("2026-10-13", 29.0, 24.0, 0, "Céu limpo", "☀️"))
        );

        when(weatherService.getWeatherByCity("Salvador")).thenReturn(mockResponse);

        mockMvc.perform(get("/weather").param("city", "Salvador"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.location.name").value("Salvador"))
            .andExpect(jsonPath("$.location.state").value("Bahia"))
            .andExpect(jsonPath("$.current.temperature").value(28.0))
            .andExpect(jsonPath("$.current.condition").value("Céu limpo"));

        mockMvc.perform(get("/api/weather").param("city", "Salvador"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.location.name").value("Salvador"));
    }

    @Test
    void testGetWeatherByCoordinatesSuccess() throws Exception {
        WeatherResponse mockResponse = new WeatherResponse(
            new WeatherResponse.LocationInfo("Salvador", "Bahia", "Brasil", -12.97, -38.51, "America/Bahia"),
            new WeatherResponse.CurrentWeatherInfo(28.0, 30.0, 70, 0.0, 15.0, 90, 10, 0, "Céu limpo", "☀️", true, "2026-10-13T12:00"),
            List.of()
        );

        when(weatherService.getWeatherByCoordinates(-12.97, -38.51)).thenReturn(mockResponse);

        mockMvc.perform(get("/weather").param("lat", "-12.97").param("lon", "-38.51"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.location.name").value("Salvador"));
    }

    @Test
    void testGetWeatherCityNotFound() throws Exception {
        when(weatherService.getWeatherByCity("CidadeInexistente"))
            .thenThrow(new CityNotFoundException("Cidade não encontrada: CidadeInexistente"));

        mockMvc.perform(get("/weather").param("city", "CidadeInexistente"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Cidade não encontrada: CidadeInexistente"));
    }

    @Test
    void testGetWeatherMissingParams() throws Exception {
        mockMvc.perform(get("/weather"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
