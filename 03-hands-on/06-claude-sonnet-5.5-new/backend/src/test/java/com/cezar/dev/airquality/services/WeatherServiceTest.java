package com.cezar.dev.airquality.services;

import static com.cezar.dev.airquality.OpenMeteoTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

class WeatherServiceTest {

    private MockRestServiceServer server;
    private WeatherService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = bind(builder);
        service = new WeatherService(client(builder));
    }

    @Test
    void mapsCurrentWeather() {
        server.expect(requestTo(containsString("latitude=-25.5")))
                .andExpect(requestTo(containsString("longitude=-49.25")))
                .andExpect(requestTo(containsString("wind_speed_unit=kmh")))
                .andExpect(requestTo(containsString("timezone=auto")))
                .andRespond(withSuccess(WEATHER_JSON, MediaType.APPLICATION_JSON));

        var weather = service.current("-25.5", "-49.25");

        assertThat(weather.latitude()).isEqualTo(-25.5);
        assertThat(weather.longitude()).isEqualTo(-49.25);
        assertThat(weather.timezone()).isEqualTo("America/Sao_Paulo");
        assertThat(weather.observedAt()).isEqualTo("2026-10-13T14:15");
        assertThat(weather.temperature()).isEqualTo(23.6);
        assertThat(weather.apparentTemperature()).isEqualTo(24.1);
        assertThat(weather.relativeHumidity()).isEqualTo(78);
        assertThat(weather.precipitation()).isEqualTo(0.0);
        assertThat(weather.windSpeed()).isEqualTo(12.4);
        assertThat(weather.windDirection()).isEqualTo(50);
        assertThat(weather.windDirectionCardinal()).isEqualTo("NE");
        assertThat(weather.weatherCode()).isEqualTo(3);
        assertThat(weather.condition()).isEqualTo("Nublado");
        assertThat(weather.isDay()).isTrue();
        server.verify();
    }

    @Test
    void mapsNightFlag() {
        server.expect(requestTo(containsString("latitude=0.0")))
                .andRespond(withSuccess(WEATHER_JSON.replace("\"is_day\":1", "\"is_day\":0"),
                        MediaType.APPLICATION_JSON));

        assertThat(service.current("0", "0").isDay()).isFalse();
    }

    @Test
    void translatesEveryWmoCode() {
        Map<Integer, String> expected = Map.ofEntries(
                Map.entry(0, "Céu limpo"), Map.entry(1, "Predominantemente limpo"),
                Map.entry(2, "Parcialmente nublado"), Map.entry(3, "Nublado"),
                Map.entry(45, "Nevoeiro"), Map.entry(48, "Nevoeiro com geada"),
                Map.entry(51, "Garoa fraca"), Map.entry(53, "Garoa moderada"),
                Map.entry(55, "Garoa intensa"), Map.entry(56, "Garoa congelante fraca"),
                Map.entry(57, "Garoa congelante intensa"), Map.entry(61, "Chuva fraca"),
                Map.entry(63, "Chuva moderada"), Map.entry(65, "Chuva forte"),
                Map.entry(66, "Chuva congelante fraca"), Map.entry(67, "Chuva congelante forte"),
                Map.entry(71, "Neve fraca"), Map.entry(73, "Neve moderada"),
                Map.entry(75, "Neve forte"), Map.entry(77, "Grãos de neve"),
                Map.entry(80, "Pancadas de chuva fracas"), Map.entry(81, "Pancadas de chuva moderadas"),
                Map.entry(82, "Pancadas de chuva violentas"), Map.entry(85, "Pancadas de neve fracas"),
                Map.entry(86, "Pancadas de neve fortes"), Map.entry(95, "Trovoada"),
                Map.entry(96, "Trovoada com granizo fraco"), Map.entry(99, "Trovoada com granizo forte"));

        expected.forEach((code, text) -> assertThat(WeatherService.toCondition(code)).isEqualTo(text));
        assertThat(WeatherService.toCondition(4)).isEqualTo("Condição desconhecida");
        assertThat(WeatherService.toCondition(-1)).isEqualTo("Condição desconhecida");
        assertThat(WeatherService.toCondition(100)).isEqualTo("Condição desconhecida");
    }

    @Test
    void translatesDegreesToCardinalPoints() {
        Object[][] cases = {
                { 0, "N" }, { 22, "N" }, { 23, "NE" }, { 45, "NE" }, { 67, "NE" }, { 68, "E" },
                { 90, "E" }, { 112, "E" }, { 113, "SE" }, { 135, "SE" }, { 157, "SE" }, { 158, "S" },
                { 180, "S" }, { 202, "S" }, { 203, "SO" }, { 225, "SO" }, { 247, "SO" }, { 248, "O" },
                { 270, "O" }, { 292, "O" }, { 293, "NO" }, { 315, "NO" }, { 337, "NO" }, { 338, "N" },
                { 359, "N" }, { 360, "N" } };
        for (Object[] c : cases) {
            assertThat(WeatherService.toCardinal(((Integer) c[0]).doubleValue()))
                    .as("%s°", c[0]).isEqualTo(c[1]);
        }
    }

    @Test
    void rejectsInvalidCoordinatesWithoutCallingUpstream() {
        String[][] cases = { { null, "0" }, { "0", null }, { "", "0" }, { "abc", "0" }, { "0", "abc" },
                { "200", "0" }, { "-90.1", "0" }, { "0", "180.1" }, { "0", "-181" },
                { "NaN", "0" }, { "0", "Infinity" } };
        for (String[] c : cases) {
            assertThatThrownBy(() -> service.current(c[0], c[1]))
                    .as("%s,%s", c[0], c[1])
                    .isInstanceOfSatisfying(InvalidRequestException.class,
                            e -> assertThat(e.getCode()).isEqualTo("INVALID_COORDINATES"));
        }
        server.verify();
    }

    @Test
    void acceptsBoundaryCoordinates() {
        server.expect(requestTo(containsString("latitude=90.0")))
                .andRespond(withSuccess(WEATHER_JSON, MediaType.APPLICATION_JSON));
        assertThat(service.current("90", "-180")).isNotNull();
    }

    @Test
    void mapsIncompletePayloadToUpstreamException() {
        server.expect(requestTo(containsString("latitude=1.0")))
                .andRespond(withSuccess("{\"latitude\":1,\"longitude\":1,\"timezone\":\"UTC\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.current("1", "1")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void mapsMissingFieldToUpstreamException() {
        server.expect(requestTo(containsString("latitude=1.0")))
                .andRespond(withSuccess(WEATHER_JSON.replace("\"temperature_2m\":23.6,", ""),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.current("1", "1")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void mapsServerErrorToUpstreamException() {
        server.expect(requestTo(containsString("latitude=1.0"))).andRespond(withServerError());

        assertThatThrownBy(() -> service.current("1", "1")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void mapsTimeoutToUpstreamTimeoutException() {
        server.expect(requestTo(containsString("latitude=1.0")))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> service.current("1", "1")).isInstanceOf(UpstreamTimeoutException.class);
    }
}
