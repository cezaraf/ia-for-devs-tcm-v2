package com.cezar.dev.airquality.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenMeteoClientTest {

    private static final String GEO = "https://geo.test/v1/search";
    private static final String FORECAST = "https://forecast.test/v1/forecast";

    private MockRestServiceServer server;
    private OpenMeteoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenMeteoClient(builder.build(), GEO, FORECAST);
    }

    @Test
    void searchCitiesMapeiaResultadosEIgnoraCamposExtras() {
        server.expect(once(), requestTo(startsWith(GEO)))
                .andExpect(queryParam("name", "Santa%20Cruz"))
                .andExpect(queryParam("count", "10"))
                .andExpect(queryParam("language", "pt"))
                .andExpect(queryParam("format", "json"))
                .andRespond(withSuccess("""
                        {"results":[{"id":1,"name":"Santa Cruz","admin1":"Bahia","country":"Brasil",
                        "country_code":"BR","latitude":-16.1,"longitude":-39.0,"population":5,
                        "timezone":"America/Bahia"}],"generationtime_ms":0.3}
                        """, MediaType.APPLICATION_JSON));

        List<GeocodingApiResponse.Result> results = client.searchCities("Santa Cruz");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().countryCode()).isEqualTo("BR");
        assertThat(results.getFirst().admin1()).isEqualTo("Bahia");
        server.verify();
    }

    @Test
    void searchCitiesSemChaveResultsRetornaListaVazia() {
        server.expect(once(), requestTo(startsWith(GEO)))
                .andRespond(withSuccess("{\"generationtime_ms\":0.2}", MediaType.APPLICATION_JSON));

        assertThat(client.searchCities("Xyzabcqwe")).isEmpty();
    }

    @Test
    void searchCitiesComResultadoSemCoordenadasViraUpstream() {
        server.expect(once(), requestTo(startsWith(GEO)))
                .andRespond(withSuccess("{\"results\":[{\"id\":1,\"name\":\"X\"}]}",
                        MediaType.APPLICATION_JSON));

        assertThrows(UpstreamException.class, () -> client.searchCities("x"));
    }

    @Test
    void fetchForecastEnviaParametrosEMapeiaCurrent() {
        server.expect(once(), requestTo(startsWith(FORECAST)))
                .andExpect(queryParam("latitude", "-25.43"))
                .andExpect(queryParam("longitude", "-49.27"))
                .andExpect(queryParam("wind_speed_unit", "kmh"))
                .andExpect(queryParam("timezone", "auto"))
                .andExpect(queryParam("current", "temperature_2m,apparent_temperature,"
                        + "relative_humidity_2m,precipitation,weather_code,wind_speed_10m,"
                        + "wind_direction_10m,is_day"))
                .andRespond(withSuccess(validForecast(), MediaType.APPLICATION_JSON));

        ForecastApiResponse response = client.fetchForecast(-25.43, -49.27);

        assertThat(response.timezone()).isEqualTo("America/Sao_Paulo");
        assertThat(response.current().temperature()).isEqualTo(23.5);
        assertThat(response.current().isDay()).isEqualTo(1);
        server.verify();
    }

    @Test
    void fetchForecastComCampoObrigatorioAusenteViraUpstream() {
        server.expect(once(), requestTo(startsWith(FORECAST)))
                .andRespond(withSuccess("""
                        {"latitude":1,"longitude":2,"timezone":"UTC",
                        "current":{"time":"2026-10-13T14:15","temperature_2m":20.0}}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(UpstreamException.class, () -> client.fetchForecast(1, 2));
    }

    @Test
    void corpoMalformadoViraUpstream() {
        server.expect(once(), requestTo(startsWith(FORECAST)))
                .andRespond(withSuccess("<html>nao e json</html>", MediaType.APPLICATION_JSON));

        assertThrows(UpstreamException.class, () -> client.fetchForecast(1, 2));
    }

    @Test
    void erro5xxViraUpstream() {
        server.expect(once(), requestTo(startsWith(GEO))).andRespond(withServerError());

        assertThrows(UpstreamException.class, () -> client.searchCities("Rio"));
    }

    @Test
    void status4xxViraUpstream() {
        server.expect(once(), requestTo(startsWith(FORECAST)))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(UpstreamException.class, () -> client.fetchForecast(1, 2));
    }

    @Test
    void timeoutDeLeituraViraUpstreamTimeout() {
        server.expect(once(), requestTo(startsWith(GEO)))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThrows(UpstreamTimeoutException.class, () -> client.searchCities("Rio"));
    }

    @Test
    void timeoutNoForecastViraUpstreamTimeout() {
        server.expect(once(), requestTo(startsWith(FORECAST)))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Connect timed out");
                });

        assertThrows(UpstreamTimeoutException.class, () -> client.fetchForecast(1, 2));
    }

    @Test
    void conexaoRecusadaViraUpstream() {
        server.expect(once(), requestTo(startsWith(GEO)))
                .andRespond(request -> {
                    throw new ConnectException("Connection refused");
                });

        assertThrows(UpstreamException.class, () -> client.searchCities("Rio"));
    }

    @Test
    void ioExceptionGenericaViraUpstream() {
        server.expect(once(), requestTo(startsWith(FORECAST)))
                .andRespond(request -> {
                    throw new IOException("boom");
                });

        assertThrows(UpstreamException.class, () -> client.fetchForecast(1, 2));
    }

    static String validForecast() {
        return """
                {"latitude":-25.43,"longitude":-49.27,"generationtime_ms":0.1,"elevation":900.0,
                "timezone":"America/Sao_Paulo","current_units":{"time":"iso8601"},
                "current":{"time":"2026-10-13T14:15","interval":900,"temperature_2m":23.5,
                "apparent_temperature":24.0,"relative_humidity_2m":78,"precipitation":0.0,
                "weather_code":3,"wind_speed_10m":12.4,"wind_direction_10m":45,"is_day":1}}
                """;
    }
}
