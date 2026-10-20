package com.cezar.dev.airquality.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cezar.dev.airquality.clients.ForecastApiClient;
import com.cezar.dev.airquality.dtos.ForecastApiResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private ForecastApiClient client;

    @InjectMocks
    private WeatherService service;

    private static ForecastApiResponse.Current current(String time, Integer isDay, Integer code, Double dir) {
        return new ForecastApiResponse.Current(time, 28.6, 62, 30.8, isDay, code, 12.7, dir);
    }

    @ParameterizedTest(name = "SCN-016 código {0} → {1}")
    @CsvSource({
            "0,Céu limpo", "1,Predominantemente limpo", "2,Parcialmente nublado", "3,Nublado",
            "45,Nevoeiro", "48,Nevoeiro com geada", "51,Garoa fraca", "53,Garoa moderada",
            "55,Garoa intensa", "56,Garoa congelante fraca", "57,Garoa congelante intensa",
            "61,Chuva fraca", "63,Chuva moderada", "65,Chuva forte", "66,Chuva congelante fraca",
            "67,Chuva congelante forte", "71,Neve fraca", "73,Neve moderada", "75,Neve forte",
            "77,Grãos de neve", "80,Pancadas de chuva fracas", "81,Pancadas de chuva moderadas",
            "82,Pancadas de chuva violentas", "85,Pancadas de neve fracas", "86,Pancadas de neve fortes",
            "95,Trovoada", "96,Trovoada com granizo fraco", "97,Trovoada forte",
            "99,Trovoada com granizo forte",
            "4,Condição desconhecida", "100,Condição desconhecida", "-1,Condição desconhecida"})
    void weatherCodes(int code, String expected) {
        assertThat(WeatherService.describeWeatherCode(code)).isEqualTo(expected);
    }

    @Test
    @DisplayName("SCN-016 código null → Condição desconhecida")
    void nullCode() {
        assertThat(WeatherService.describeWeatherCode(null)).isEqualTo("Condição desconhecida");
    }

    @ParameterizedTest(name = "SCN-017 {0}° → {1}")
    @CsvSource({
            "0,Norte", "22.49,Norte", "22.5,Nordeste", "45,Nordeste", "67.49,Nordeste", "67.5,Leste",
            "112.5,Sudeste", "157.5,Sul", "202.5,Sudoeste", "247.5,Oeste", "292.5,Noroeste",
            "337.49,Noroeste", "337.5,Norte", "359.99,Norte", "360,Norte", "720,Norte", "-45,Noroeste"})
    void directions(double degrees, String expected) {
        assertThat(WeatherService.windDirection(degrees)).isEqualTo(expected);
    }

    @Test
    @DisplayName("SCN-017 graus null, NaN e infinito → null")
    void nonFiniteDirections() {
        assertThat(WeatherService.windDirection(null)).isNull();
        assertThat(WeatherService.windDirection(Double.NaN)).isNull();
        assertThat(WeatherService.windDirection(Double.POSITIVE_INFINITY)).isNull();
        assertThat(WeatherService.windDirection(Double.NEGATIVE_INFINITY)).isNull();
    }

    @ParameterizedTest(name = "SCN-018 latitude \"{0}\" longitude \"{1}\" inválidas")
    @CsvSource(nullValues = "null", value = {
            "null,10", "'',10", "'  ',10", "abc,10", "NaN,10", "Infinity,10", "-90.0001,10", "90.0001,10",
            "10,null", "10,'',", "10,abc", "10,NaN", "10,-Infinity", "10,180.0001", "10,-180.0001"})
    void invalidCoordinates(String lat, String lon) {
        assertThatThrownBy(() -> service.current(lat, lon))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        e -> assertThat(e.getCode()).isEqualTo("INVALID_COORDINATES"));
        verifyNoInteractions(client);
    }

    @ParameterizedTest(name = "SCN-014 limite {0},{1} aceito")
    @CsvSource({"-90,-180", "90,180", "0,0", "' 10.5 ',' -20 '"})
    void validBoundaries(String lat, String lon) {
        when(client.current(anyDouble(), anyDouble()))
                .thenReturn(new ForecastApiResponse("UTC", current("2026-10-20T16:00", 1, 1, 111.0)));
        WeatherResponse r = service.current(lat, lon);
        assertThat(r.latitude()).isEqualTo(Double.parseDouble(lat.trim()));
        assertThat(r.longitude()).isEqualTo(Double.parseDouble(lon.trim()));
    }

    @Test
    @DisplayName("SCN-014 mapeamento completo com latitude/longitude ecoadas da requisição")
    void fullMapping() {
        when(client.current(-8.05, -34.88)).thenReturn(new ForecastApiResponse("America/Recife",
                current("2026-10-20T16:00", 1, 1, 111.0)));

        WeatherResponse r = service.current("-8.05", "-34.88");

        assertThat(r).isEqualTo(new WeatherResponse(-8.05, -34.88, "America/Recife", "2026-10-20T16:00", 28.6,
                30.8, 62, 12.7, 111.0, "Leste", 1, "Predominantemente limpo", true));
        verify(client).current(-8.05, -34.88);
    }

    @Test
    @DisplayName("SCN-015 campos ausentes viram null e is_day 2 vira null")
    void missingFields() {
        when(client.current(1.0, 2.0)).thenReturn(new ForecastApiResponse(null,
                new ForecastApiResponse.Current("2026-10-20T16:00", null, null, null, 2, null, null, null)));

        WeatherResponse r = service.current("1", "2");

        assertThat(r.timezone()).isNull();
        assertThat(r.temperature()).isNull();
        assertThat(r.apparentTemperature()).isNull();
        assertThat(r.humidity()).isNull();
        assertThat(r.windSpeed()).isNull();
        assertThat(r.windDirectionDegrees()).isNull();
        assertThat(r.windDirection()).isNull();
        assertThat(r.weatherCode()).isNull();
        assertThat(r.condition()).isEqualTo("Condição desconhecida");
        assertThat(r.isDay()).isNull();
    }

    @ParameterizedTest(name = "SCN-014 is_day {0} → {1}")
    @CsvSource(nullValues = "null", value = {"1,true", "0,false", "2,null", "null,null"})
    void isDay(Integer raw, Boolean expected) {
        when(client.current(1.0, 2.0))
                .thenReturn(new ForecastApiResponse("UTC", current("2026-10-20T16:00", raw, 0, 0.0)));
        assertThat(service.current("1", "2").isDay()).isEqualTo(expected);
    }

    @Test
    @DisplayName("SCN-019 current ausente → UpstreamException")
    void missingCurrent() {
        when(client.current(1.0, 2.0)).thenReturn(new ForecastApiResponse("UTC", null));
        assertThatThrownBy(() -> service.current("1", "2")).isInstanceOf(UpstreamException.class);
    }

    @ParameterizedTest(name = "SCN-019 current.time \"{0}\" → UpstreamException")
    @NullSource
    @ValueSource(strings = "")
    void missingTime(String time) {
        when(client.current(1.0, 2.0))
                .thenReturn(new ForecastApiResponse("UTC", current(time, 1, 0, 0.0)));
        assertThatThrownBy(() -> service.current("1", "2")).isInstanceOf(UpstreamException.class);
    }
}
