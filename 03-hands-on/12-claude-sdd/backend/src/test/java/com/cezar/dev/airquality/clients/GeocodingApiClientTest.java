package com.cezar.dev.airquality.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

class GeocodingApiClientTest {

    private static final String BASE = "https://geocoding.test/v1";

    private MockRestServiceServer server;
    private GeocodingApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeocodingApiClient(builder.build(), BASE);
    }

    @Test
    @DisplayName("SCN-003 monta a URL de busca com name codificado uma única vez")
    void scn003UrlExata() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(BASE + "/search?name=S%C3%A3o%20Jos%C3%A9&count=10&language=pt&format=json"))
                .andRespond(withSuccess("""
                        {"results":[{"id":1,"name":"São José","latitude":-27.6,"longitude":-48.6,
                        "admin1":"Santa Catarina","admin2":"São José","country":"Brasil",
                        "country_code":"BR","feature_code":"PPL","timezone":"America/Sao_Paulo"}]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingApiResponse response = client.search("São José");

        assertThat(response.results()).hasSize(1);
        GeocodingApiResponse.Result r = response.results().get(0);
        assertThat(r.id()).isEqualTo(1L);
        assertThat(r.name()).isEqualTo("São José");
        assertThat(r.countryCode()).isEqualTo("BR");
        assertThat(r.featureCode()).isEqualTo("PPL");
        assertThat(r.timezone()).isEqualTo("America/Sao_Paulo");
        server.verify();
    }

    @Test
    @DisplayName("SCN-003 não interpreta chaves do nome como variável de URI")
    void scn003NomeComChaves() {
        server.expect(requestTo(BASE + "/search?name=%7Bx%7D&count=10&language=pt&format=json"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.search("{x}");

        server.verify();
    }

    @Test
    @DisplayName("SCN-007 aceita corpo sem results e campos desconhecidos")
    void scn007CorpoTolerante() {
        server.expect(requestTo(BASE + "/search?name=Zzz&count=10&language=pt&format=json"))
                .andRespond(withSuccess("{\"generationtime_ms\":0.1}", MediaType.APPLICATION_JSON));

        assertThat(client.search("Zzz").results()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("SCN-007 ignora campos desconhecidos dentro de results")
    void scn007CamposDesconhecidos() {
        server.expect(requestTo(BASE + "/search?name=Recife&count=10&language=pt&format=json"))
                .andRespond(withSuccess("""
                        {"results":[{"id":2,"name":"Recife","novo":"x","admin1_id":3}],"generationtime_ms":0.2}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.search("Recife").results()).singleElement()
                .satisfies(r -> assertThat(r.name()).isEqualTo("Recife"));
    }

    @Test
    @DisplayName("SCN-005 erro 500 vira UpstreamException")
    void scn005Erro500() {
        server.expect(requestTo(BASE + "/search?name=x&count=10&language=pt&format=json"))
                .andRespond(withServerError());

        assertThrows(UpstreamException.class, () -> client.search("x"));
    }

    @Test
    @DisplayName("SCN-005 status 400 com corpo de erro vira UpstreamException")
    void scn005Status400() {
        server.expect(requestTo(BASE + "/search?name=x&count=10&language=pt&format=json"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":true,\"reason\":\"segredo\"}"));

        UpstreamException e = assertThrows(UpstreamException.class, () -> client.search("x"));
        assertThat(e.getMessage()).doesNotContain("segredo");
    }

    @Test
    @DisplayName("SCN-005 corpo \"not json\" vira UpstreamException")
    void scn005CorpoInvalido() {
        server.expect(requestTo(BASE + "/search?name=x&count=10&language=pt&format=json"))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        assertThrows(UpstreamException.class, () -> client.search("x"));
    }

    @Test
    @DisplayName("SCN-005 ConnectException vira UpstreamException")
    void scn005ConexaoRecusada() {
        server.expect(requestTo(BASE + "/search?name=x&count=10&language=pt&format=json"))
                .andRespond(request -> {
                    throw new ConnectException("refused");
                });

        assertThrows(UpstreamException.class, () -> client.search("x"));
    }

    @Test
    @DisplayName("SCN-006 SocketTimeoutException vira UpstreamTimeoutException")
    void scn006Timeout() {
        server.expect(requestTo(BASE + "/search?name=x&count=10&language=pt&format=json"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThrows(UpstreamTimeoutException.class, () -> client.search("x"));
    }
}
