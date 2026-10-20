package com.cezar.dev.airquality;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cezar.dev.airquality.dto.WeatherResponse;
import com.cezar.dev.airquality.services.CityNotFoundException;
import com.cezar.dev.airquality.services.WeatherService;
import com.cezar.dev.airquality.services.WeatherUnavailableException;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WeatherService service;

    private static WeatherResponse sample() {
        return new WeatherResponse(
                new WeatherResponse.Location("Salvador", "Bahia", "Brasil", -12.97, -38.5, "America/Bahia"),
                new WeatherResponse.Current("2026-10-13T15:00", 27.4, 29.1, 70, 0.0, 12.5, 90, true, 1,
                        "Predominantemente limpo", "🌤️"),
                new WeatherResponse.Units("°C", "%", "mm", "km/h"));
    }

    @Test
    void returnsWeatherForCity() throws Exception {
        when(service.byCity("Salvador")).thenReturn(sample());

        mvc.perform(get("/weather").param("city", " Salvador "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.name").value("Salvador"))
                .andExpect(jsonPath("$.current.temperature").value(27.4))
                .andExpect(jsonPath("$.units.temperature").value("°C"));
    }

    @Test
    void returnsWeatherForCoordinates() throws Exception {
        when(service.byCoordinates(anyDouble(), anyDouble())).thenReturn(sample());

        mvc.perform(get("/weather").param("lat", "-12.97").param("lon", "-38.5"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsMissingParameters() throws Exception {
        mvc.perform(get("/weather")).andExpect(status().isBadRequest());
        mvc.perform(get("/weather").param("city", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/weather").param("lat", "95").param("lon", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownCityIs404() throws Exception {
        when(service.byCity("Xyzzy")).thenThrow(new CityNotFoundException("Xyzzy"));

        mvc.perform(get("/weather").param("city", "Xyzzy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cidade não encontrada: Xyzzy"));
    }

    @Test
    void upstreamFailureIs502() throws Exception {
        when(service.byCity("Salvador")).thenThrow(new WeatherUnavailableException("fora do ar", null));

        mvc.perform(get("/weather").param("city", "Salvador"))
                .andExpect(status().isBadGateway());
    }
}
