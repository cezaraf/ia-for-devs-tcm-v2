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

import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

class ForecastApiClientTest {

    private static final String BASE = "https://forecast.test/v1";
    private static final String URL = BASE + "/forecast?latitude=-8.05&longitude=-34.88"
            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,"
            + "wind_speed_10m,wind_direction_10m&timezone=auto&temperature_unit=celsius&wind_speed_unit=kmh";

    private MockRestServiceServer server;
    private ForecastApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ForecastApiClient(builder.build(), BASE);
    }

    @Test
    @DisplayName("SCN-004 monta a URL de forecast com os sete campos e unidades fixas")
    void scn004UrlExata() {
        server.expect(method(HttpMethod.GET)).andExpect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"latitude":-8.05,"timezone":"America/Recife","extra":1,
                        "current":{"time":"2026-10-20T10:00","interval":900,"temperature_2m":28.4,
                        "relative_humidity_2m":70,"apparent_temperature":31.0,"is_day":1,
                        "weather_code":3,"wind_speed_10m":12.5,"wind_direction_10m":90}}
                        """, MediaType.APPLICATION_JSON));

        ForecastApiResponse response = client.current(-8.05, -34.88);

        assertThat(response.timezone()).isEqualTo("America/Recife");
        ForecastApiResponse.Current c = response.current();
        assertThat(c.time()).isEqualTo("2026-10-20T10:00");
        assertThat(c.temperature2m()).isEqualTo(28.4);
        assertThat(c.relativeHumidity2m()).isEqualTo(70);
        assertThat(c.apparentTemperature()).isEqualTo(31.0);
        assertThat(c.isDay()).isEqualTo(1);
        assertThat(c.weatherCode()).isEqualTo(3);
        assertThat(c.windSpeed10m()).isEqualTo(12.5);
        assertThat(c.windDirection10m()).isEqualTo(90.0);
        server.verify();
    }

    @Test
    @DisplayName("SCN-007 aceita corpo sem current e campos desconhecidos")
    void scn007CorpoTolerante() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"generationtime_ms\":0.1}", MediaType.APPLICATION_JSON));

        ForecastApiResponse response = client.current(-8.05, -34.88);

        assertThat(response.current()).isNull();
        assertThat(response.timezone()).isNull();
    }

    @Test
    @DisplayName("SCN-005 erro 500 vira UpstreamException")
    void scn005Erro500() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(UpstreamException.class, () -> client.current(-8.05, -34.88));
    }

    @Test
    @DisplayName("SCN-005 status 400 com corpo de erro vira UpstreamException")
    void scn005Status400() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":true,\"reason\":\"segredo\"}"));

        UpstreamException e = assertThrows(UpstreamException.class, () -> client.current(-8.05, -34.88));
        assertThat(e.getMessage()).doesNotContain("segredo");
    }

    @Test
    @DisplayName("SCN-005 corpo \"not json\" vira UpstreamException")
    void scn005CorpoInvalido() {
        server.expect(requestTo(URL)).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        assertThrows(UpstreamException.class, () -> client.current(-8.05, -34.88));
    }

    @Test
    @DisplayName("SCN-005 ConnectException vira UpstreamException")
    void scn005ConexaoRecusada() {
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new ConnectException("refused");
        });

        assertThrows(UpstreamException.class, () -> client.current(-8.05, -34.88));
    }

    @Test
    @DisplayName("SCN-006 SocketTimeoutException vira UpstreamTimeoutException")
    void scn006Timeout() {
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new SocketTimeoutException("Connect timed out");
        });

        assertThrows(UpstreamTimeoutException.class, () -> client.current(-8.05, -34.88));
    }
}
