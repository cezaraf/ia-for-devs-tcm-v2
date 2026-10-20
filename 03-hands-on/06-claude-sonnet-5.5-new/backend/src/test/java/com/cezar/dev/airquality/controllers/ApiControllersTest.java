package com.cezar.dev.airquality.controllers;

import static com.cezar.dev.airquality.OpenMeteoTestSupport.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.services.CityService;
import com.cezar.dev.airquality.services.WeatherService;

@WebMvcTest({ CityController.class, WeatherController.class })
@Import({ CityService.class, WeatherService.class, ApiExceptionHandler.class,
        ApiControllersTest.MockOpenMeteo.class })
class ApiControllersTest {

    @TestConfiguration
    static class MockOpenMeteo {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = bind(builder);

        @Bean
        MockRestServiceServer mockServer() {
            return server;
        }

        @Bean
        @Primary
        OpenMeteoClient testOpenMeteoClient() {
            return client(builder);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    MockRestServiceServer server;

    @BeforeEach
    void reset() {
        server.reset();
    }

    @Test
    void citiesReturns200() throws Exception {
        server.expect(requestTo(containsString("name=Curitiba")))
                .andRespond(withSuccess(CURITIBA_JSON, MediaType.APPLICATION_JSON));

        mvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(6322752))
                .andExpect(jsonPath("$[0].name").value("Curitiba"))
                .andExpect(jsonPath("$[0].admin1").value("Paraná"))
                .andExpect(jsonPath("$[0].country").value("Brasil"))
                .andExpect(jsonPath("$[0].countryCode").value("BR"))
                .andExpect(jsonPath("$[0].latitude").value(-25.42778))
                .andExpect(jsonPath("$[0].longitude").value(-49.27306));
    }

    @Test
    void citiesReturnsNullsForMissingRegion() throws Exception {
        server.expect(requestTo(containsString("name=Santa")))
                .andRespond(withSuccess(SANTA_CRUZ_JSON, MediaType.APPLICATION_JSON));

        mvc.perform(get("/cities").param("name", "Santa Cruz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].admin1").isEmpty())
                .andExpect(jsonPath("$[2].country").isEmpty());
    }

    @Test
    void citiesReturns400ForMissingOrShortName() throws Exception {
        mvc.perform(get("/cities"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
        mvc.perform(get("/cities").param("name", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(get("/cities").param("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
    }

    @Test
    void citiesReturns404WhenNothingFound() throws Exception {
        server.expect(requestTo(containsString("name=Xyzabcqwe")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        mvc.perform(get("/cities").param("name", "Xyzabcqwe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"));
    }

    @Test
    void citiesReturns502WhenUpstreamFails() throws Exception {
        server.expect(requestTo(containsString("name=Curitiba"))).andRespond(withServerError());

        mvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void citiesReturns504OnTimeout() throws Exception {
        server.expect(requestTo(containsString("name=Curitiba")))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        mvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"));
    }

    @Test
    void weatherReturns200() throws Exception {
        server.expect(requestTo(containsString("latitude=-25.5")))
                .andRespond(withSuccess(WEATHER_JSON, MediaType.APPLICATION_JSON));

        mvc.perform(get("/weather").param("latitude", "-25.5").param("longitude", "-49.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.observedAt").value("2026-10-13T14:15"))
                .andExpect(jsonPath("$.temperature").value(23.6))
                .andExpect(jsonPath("$.apparentTemperature").value(24.1))
                .andExpect(jsonPath("$.relativeHumidity").value(78))
                .andExpect(jsonPath("$.precipitation").value(0.0))
                .andExpect(jsonPath("$.windSpeed").value(12.4))
                .andExpect(jsonPath("$.windDirection").value(50))
                .andExpect(jsonPath("$.windDirectionCardinal").value("NE"))
                .andExpect(jsonPath("$.weatherCode").value(3))
                .andExpect(jsonPath("$.condition").value("Nublado"))
                .andExpect(jsonPath("$.isDay").value(true));
    }

    @Test
    void weatherReturns400ForInvalidCoordinates() throws Exception {
        mvc.perform(get("/weather").param("latitude", "200").param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        mvc.perform(get("/weather").param("latitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        mvc.perform(get("/weather").param("latitude", "x").param("longitude", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void weatherReturns502WhenUpstreamFails() throws Exception {
        server.expect(requestTo(containsString("latitude=1.0"))).andRespond(withServerError());

        mvc.perform(get("/weather").param("latitude", "1").param("longitude", "1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"));
    }

    @Test
    void weatherReturns504OnTimeout() throws Exception {
        server.expect(requestTo(containsString("latitude=1.0")))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        mvc.perform(get("/weather").param("latitude", "1").param("longitude", "1"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("O serviço de clima demorou para responder."));
    }
}
