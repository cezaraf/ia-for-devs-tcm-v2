package com.cezar.dev.airquality;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.controllers.ApiExceptionHandler;
import com.cezar.dev.airquality.controllers.CityController;
import com.cezar.dev.airquality.services.CityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CityControllerTest {

    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        OpenMeteoClient openMeteoClient = new OpenMeteoClient(
                builder.build(),
                "https://geocoding-api.open-meteo.com/v1/search",
                "https://api.open-meteo.com/v1/forecast"
        );
        CityService cityService = new CityService(openMeteoClient);
        CityController cityController = new CityController(cityService);

        mockMvc = MockMvcBuilders.standaloneSetup(cityController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /cities?name=Curitiba - 200 OK")
    void testGetCitiesSuccess() throws Exception {
        String json = """
                {
                  "results": [
                    {
                      "id": 3464975,
                      "name": "Curitiba",
                      "latitude": -25.42778,
                      "longitude": -49.27306,
                      "country_code": "BR",
                      "country": "Brasil",
                      "admin1": "Paraná"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://geocoding-api.open-meteo.com/v1/search?name=Curitiba&count=10&language=pt&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Curitiba")))
                .andExpect(jsonPath("$[0].admin1", is("Paraná")))
                .andExpect(jsonPath("$[0].country", is("Brasil")))
                .andExpect(jsonPath("$[0].countryCode", is("BR")))
                .andExpect(jsonPath("$[0].latitude", is(-25.42778)))
                .andExpect(jsonPath("$[0].longitude", is(-49.27306)));

        mockServer.verify();
    }

    @Test
    @DisplayName("GET /cities?name=a - 400 INVALID_CITY_NAME")
    void testGetCitiesInvalidName() throws Exception {
        mockMvc.perform(get("/cities").param("name", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_CITY_NAME")))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /cities (sem parâmetro) - 400 INVALID_CITY_NAME")
    void testGetCitiesMissingName() throws Exception {
        mockMvc.perform(get("/cities"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_CITY_NAME")));
    }

    @Test
    @DisplayName("GET /cities?name=Xyzabcqwe - 404 CITY_NOT_FOUND")
    void testGetCitiesNotFound() throws Exception {
        String json = "{}";

        mockServer.expect(requestTo("https://geocoding-api.open-meteo.com/v1/search?name=Xyzabcqwe&count=10&language=pt&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cities").param("name", "Xyzabcqwe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("CITY_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Cidade não encontrada. Verifique o nome e tente novamente.")));

        mockServer.verify();
    }

    @Test
    @DisplayName("GET /cities?name=Curitiba com upstream 502/500 - 502 UPSTREAM_ERROR")
    void testGetCitiesUpstreamError() throws Exception {
        mockServer.expect(requestTo("https://geocoding-api.open-meteo.com/v1/search?name=Curitiba&count=10&language=pt&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code", is("UPSTREAM_ERROR")))
                .andExpect(jsonPath("$.message", is("O serviço de clima está indisponível no momento.")));

        mockServer.verify();
    }

    @Test
    @DisplayName("GET /cities?name=Curitiba com timeout - 504 UPSTREAM_TIMEOUT")
    void testGetCitiesUpstreamTimeout() throws Exception {
        mockServer.expect(requestTo("https://geocoding-api.open-meteo.com/v1/search?name=Curitiba&count=10&language=pt&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code", is("UPSTREAM_TIMEOUT")))
                .andExpect(jsonPath("$.message", is("O serviço de clima demorou para responder.")));

        mockServer.verify();
    }
}
