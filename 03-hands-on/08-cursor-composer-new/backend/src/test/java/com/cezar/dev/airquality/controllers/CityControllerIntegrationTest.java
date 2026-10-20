package com.cezar.dev.airquality.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.hamcrest.Matchers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class CityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void searchCities_returns200WithResults() throws Exception {
        mockServer.expect(MockRestRequestMatchers.requestTo(Matchers.containsString("geocoding-api.open-meteo.com")))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                          "results": [{
                            "id": 1,
                            "name": "Curitiba",
                            "latitude": -25.42,
                            "longitude": -49.27,
                            "admin1": "Paraná",
                            "country": "Brasil",
                            "country_code": "BR"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Curitiba"))
                .andExpect(jsonPath("$[0].admin1").value("Paraná"));
        mockServer.verify();
    }

    @Test
    void searchCities_returns400ForInvalidName() throws Exception {
        mockMvc.perform(get("/cities").param("name", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
    }

    @Test
    void searchCities_returns404WhenNotFound() throws Exception {
        mockServer.expect(MockRestRequestMatchers.requestTo(Matchers.containsString("geocoding-api.open-meteo.com")))
                .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cities").param("name", "Xyzabcqwe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"));
        mockServer.verify();
    }

    @Test
    void searchCities_returns502OnUpstreamError() throws Exception {
        mockServer.expect(MockRestRequestMatchers.requestTo(Matchers.containsString("geocoding-api.open-meteo.com")))
                .andRespond(MockRestResponseCreators.withServerError());

        mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"));
        mockServer.verify();
    }
}
