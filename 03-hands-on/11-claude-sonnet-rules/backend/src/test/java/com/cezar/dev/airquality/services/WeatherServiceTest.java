package com.cezar.dev.airquality.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class WeatherServiceTest {

    private OpenMeteoClient client;
    private WeatherService service;

    @BeforeEach
    void setUp() {
        client = mock(OpenMeteoClient.class);
        service = new WeatherService(client);
    }

    @Test
    void mapeiaForecastParaResposta() {
        when(client.fetchForecast(-25.43, -49.27)).thenReturn(forecast(23.54, 78.0, 45.0, 3, 1));

        WeatherResponse r = service.current("-25.43", " -49.27 ");

        assertThat(r.latitude()).isEqualTo(-25.43);
        assertThat(r.longitude()).isEqualTo(-49.27);
        assertThat(r.timezone()).isEqualTo("America/Sao_Paulo");
        assertThat(r.observedAt()).isEqualTo("2026-10-13T14:15");
        assertThat(r.temperature()).isEqualTo(23.54);
        assertThat(r.apparentTemperature()).isEqualTo(24.0);
        assertThat(r.relativeHumidity()).isEqualTo(78);
        assertThat(r.precipitation()).isEqualTo(0.0);
        assertThat(r.windSpeed()).isEqualTo(12.4);
        assertThat(r.windDirection()).isEqualTo(45);
        assertThat(r.windDirectionCardinal()).isEqualTo("NE");
        assertThat(r.weatherCode()).isEqualTo(3);
        assertThat(r.condition()).isEqualTo("Nublado");
        assertThat(r.isDay()).isTrue();
    }

    @Test
    void isDayZeroViraFalso() {
        when(client.fetchForecast(0, 0)).thenReturn(forecast(10, 50.0, 0.0, 0, 0));

        assertThat(service.current("0", "0").isDay()).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "abc", "NaN", "Infinity", "-Infinity", "90.1", "-90.1", "200"})
    void latitudeInvalidaLancaInvalidCoordinates(String latitude) {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> service.current(latitude, "0"));

        assertThat(e.getCode()).isEqualTo("INVALID_COORDINATES");
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "x", "NaN", "180.1", "-180.1", "1e999"})
    void longitudeInvalidaLancaInvalidCoordinates(String longitude) {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> service.current("0", longitude));

        assertThat(e.getCode()).isEqualTo("INVALID_COORDINATES");
        verifyNoInteractions(client);
    }

    @Test
    void aceitaLimitesDoIntervalo() {
        when(client.fetchForecast(90, 180)).thenReturn(forecast(1, 1.0, 1.0, 0, 1));
        when(client.fetchForecast(-90, -180)).thenReturn(forecast(1, 1.0, 1.0, 0, 1));

        assertThat(service.current("90", "180")).isNotNull();
        assertThat(service.current("-90", "-180")).isNotNull();
    }

    private static final Map<Integer, String> WMO = Map.ofEntries(
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

    @Test
    void tabelaWmoCobreCadaEntrada() {
        assertThat(WMO).hasSize(28);
        WMO.forEach((code, text) -> assertThat(WeatherService.condition(code))
                .as("código %d", code).isEqualTo(text));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4, 10, 44, 46, 100, 1000})
    void codigoDesconhecidoUsaValorPadrao(int code) {
        assertThat(WeatherService.condition(code)).isEqualTo("Condição desconhecida");
    }

    @ParameterizedTest
    @CsvSource({
            "0, N", "10, N", "22, N", "22.5, NE", "45, NE", "67, NE", "67.5, E", "90, E",
            "112, E", "112.5, SE", "135, SE", "157.5, S", "180, S", "202.5, SO", "225, SO",
            "247.5, O", "270, O", "292.5, NO", "315, NO", "337, NO", "337.5, N", "359, N",
            "360, N", "720, N", "-45, NO"
    })
    void direcaoCardealPorSetorDe45Graus(double degrees, String expected) {
        assertThat(WeatherService.cardinal(degrees)).isEqualTo(expected);
    }

    private static ForecastApiResponse forecast(double temperature, double humidity, double direction,
                                                int code, int isDay) {
        return new ForecastApiResponse(-25.43, -49.27, "America/Sao_Paulo",
                new ForecastApiResponse.Current("2026-10-13T14:15", temperature, 24.0, humidity, 0.0,
                        code, 12.4, direction, isDay));
    }
}
