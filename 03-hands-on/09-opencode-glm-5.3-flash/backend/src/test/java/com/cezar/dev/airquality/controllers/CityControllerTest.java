package com.cezar.dev.airquality.controllers;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa GET /cities de ponta a ponta (controller → service → OpenMeteoClient), com a
 * Open-Meteo simulada por {@link MockRestServiceServer}. O servidor simulado substitui a
 * fábrica de conexões do RestClient (registrada com ordem menor em OpenMeteoConfig),
 * então nenhuma chamada real à Open-Meteo acontece.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
class CityControllerTest {

    private static final String GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=10&language=pt&format=json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer openMeteo;

    @Test
    void deveRetornarCidadesEncontradas() throws Exception {
        this.openMeteo.expect(requestTo(GEOCODING_URL.formatted("Curitiba")))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {
                              "id": 3454561,
                              "name": "Curitiba",
                              "latitude": -25.4296,
                              "longitude": -49.2713,
                              "admin1": "Paraná",
                              "country": "Brasil",
                              "country_code": "BR",
                              "elevation": 935,
                              "timezone": "America/Sao_Paulo"
                            }
                          ],
                          "generationtime_ms": 0.5
                        }
                        """, MediaType.APPLICATION_JSON));

        this.mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3454561))
                .andExpect(jsonPath("$[0].name").value("Curitiba"))
                .andExpect(jsonPath("$[0].admin1").value("Paraná"))
                .andExpect(jsonPath("$[0].country").value("Brasil"))
                .andExpect(jsonPath("$[0].countryCode").value("BR"))
                .andExpect(jsonPath("$[0].latitude").value(-25.4296))
                .andExpect(jsonPath("$[0].longitude").value(-49.2713));
    }

    @Test
    void deveRetornar404QuandoNenhumaCidadeEncontrada() throws Exception {
        this.openMeteo.expect(requestTo(GEOCODING_URL.formatted("Xyzabcqwe")))
                .andRespond(withSuccess("{\"generationtime_ms\":0.1}", MediaType.APPLICATION_JSON));

        this.mockMvc.perform(get("/cities").param("name", "Xyzabcqwe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cidade não encontrada."));
    }

    @Test
    void deveRetornar400QuandoNomeAusente() throws Exception {
        this.mockMvc.perform(get("/cities"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
    }

    @Test
    void deveRetornar400QuandoNomeComMenosDeDoisCaracteres() throws Exception {
        this.mockMvc.perform(get("/cities").param("name", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CITY_NAME"));
    }

    @Test
    void deveRetornar502QuandoOpenMeteoFalha() throws Exception {
        this.openMeteo.expect(requestTo(GEOCODING_URL.formatted("Curitiba")))
                .andRespond(withServerError());

        this.mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"));
    }

    @Test
    void deveRetornar504QuandoOpenMeteoExcedeOTempo() throws Exception {
        this.openMeteo.expect(requestTo(GEOCODING_URL.formatted("Curitiba")))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        this.mockMvc.perform(get("/cities").param("name", "Curitiba"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("UPSTREAM_TIMEOUT"));
    }
}
