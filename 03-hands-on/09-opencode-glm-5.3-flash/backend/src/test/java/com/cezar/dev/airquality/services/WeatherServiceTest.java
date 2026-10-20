package com.cezar.dev.airquality.services;

import java.util.stream.Stream;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.OpenMeteoCurrent;
import com.cezar.dev.airquality.dtos.OpenMeteoForecastResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class WeatherServiceTest {

    private final OpenMeteoClient openMeteoClient = Mockito.mock(OpenMeteoClient.class);
    private final WeatherService weatherService = new WeatherService(this.openMeteoClient);

    @Test
    void deveRejeitarCoordenadasAusentes() {
        Assertions.assertThatThrownBy(() -> this.weatherService.getCurrentWeather(null, -49.27))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> Assertions.assertThat(exception.getCode()).isEqualTo("INVALID_COORDINATES"));
        Assertions.assertThatThrownBy(() -> this.weatherService.getCurrentWeather(-25.43, null))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> Assertions.assertThat(exception.getCode()).isEqualTo("INVALID_COORDINATES"));
        Mockito.verifyNoInteractions(this.openMeteoClient);
    }

    @Test
    void deveRejeitarCoordenadasForaDoIntervalo() {
        Assertions.assertThatThrownBy(() -> this.weatherService.getCurrentWeather(90.01, 0.0))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> Assertions.assertThat(exception.getCode()).isEqualTo("INVALID_COORDINATES"));
        Assertions.assertThatThrownBy(() -> this.weatherService.getCurrentWeather(-25.43, 180.01))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> Assertions.assertThat(exception.getCode()).isEqualTo("INVALID_COORDINATES"));
        Mockito.verifyNoInteractions(this.openMeteoClient);
    }

    @Test
    void deveMapearClimaAtualCompleto() {
        OpenMeteoCurrent current = new OpenMeteoCurrent(21.4, 24.1, 78, 0.0, 2, 12.3, 45, 1, "2026-10-13T14:15");
        Mockito.when(this.openMeteoClient.getForecast(-25.43, -49.27))
                .thenReturn(new OpenMeteoForecastResponse(-25.43, -49.27, "America/Sao_Paulo", current));

        WeatherResponse clima = this.weatherService.getCurrentWeather(-25.43, -49.27);

        Assertions.assertThat(clima.latitude()).isEqualTo(-25.43);
        Assertions.assertThat(clima.longitude()).isEqualTo(-49.27);
        Assertions.assertThat(clima.timezone()).isEqualTo("America/Sao_Paulo");
        Assertions.assertThat(clima.observedAt()).isEqualTo("2026-10-13T14:15");
        Assertions.assertThat(clima.temperature()).isEqualTo(21.4);
        Assertions.assertThat(clima.apparentTemperature()).isEqualTo(24.1);
        Assertions.assertThat(clima.relativeHumidity()).isEqualTo(78);
        Assertions.assertThat(clima.precipitation()).isEqualTo(0.0);
        Assertions.assertThat(clima.windSpeed()).isEqualTo(12.3);
        Assertions.assertThat(clima.windDirection()).isEqualTo(45);
        Assertions.assertThat(clima.windDirectionCardinal()).isEqualTo("NE");
        Assertions.assertThat(clima.weatherCode()).isEqualTo(2);
        Assertions.assertThat(clima.condition()).isEqualTo("Parcialmente nublado");
        Assertions.assertThat(clima.isDay()).isTrue();
    }

    @Test
    void deveMapearNoiteQuandoIsDayForZero() {
        OpenMeteoCurrent current = new OpenMeteoCurrent(18.0, 17.5, 90, 1.2, 61, 8.0, 180, 0, "2026-10-13T22:00");
        Mockito.when(this.openMeteoClient.getForecast(0.0, 0.0))
                .thenReturn(new OpenMeteoForecastResponse(0.0, 0.0, "UTC", current));

        WeatherResponse clima = this.weatherService.getCurrentWeather(0.0, 0.0);

        Assertions.assertThat(clima.isDay()).isFalse();
        Assertions.assertThat(clima.condition()).isEqualTo("Chuva fraca");
        Assertions.assertThat(clima.windDirectionCardinal()).isEqualTo("S");
    }

    @Test
    void deveTratarCamposAusentesComoIndisponiveis() {
        OpenMeteoCurrent current = new OpenMeteoCurrent(null, null, null, null, null, null, null, null, null);
        Mockito.when(this.openMeteoClient.getForecast(0.0, 0.0))
                .thenReturn(new OpenMeteoForecastResponse(0.0, 0.0, null, current));

        WeatherResponse clima = this.weatherService.getCurrentWeather(0.0, 0.0);

        Assertions.assertThat(clima.condition()).isEqualTo("Condição desconhecida");
        Assertions.assertThat(clima.isDay()).isFalse();
        Assertions.assertThat(clima.timezone()).isEqualTo("UTC");
    }

    @Test
    void deveLancarUpstreamQuandoCorpoIncompleto() {
        Mockito.when(this.openMeteoClient.getForecast(0.0, 0.0))
                .thenReturn(new OpenMeteoForecastResponse(0.0, 0.0, "UTC", null));

        Assertions.assertThatThrownBy(() -> this.weatherService.getCurrentWeather(0.0, 0.0))
                .isInstanceOf(UpstreamException.class);
    }

    @ParameterizedTest(name = "código {0} → {1}")
    @CsvSource({
            "0,Céu limpo",
            "1,Predominantemente limpo",
            "2,Parcialmente nublado",
            "3,Nublado",
            "45,Nevoeiro",
            "48,Nevoeiro com geada",
            "51,Garoa fraca",
            "53,Garoa moderada",
            "55,Garoa intensa",
            "56,Garoa congelante fraca",
            "57,Garoa congelante intensa",
            "61,Chuva fraca",
            "63,Chuva moderada",
            "65,Chuva forte",
            "66,Chuva congelante fraca",
            "67,Chuva congelante forte",
            "71,Neve fraca",
            "73,Neve moderada",
            "75,Neve forte",
            "77,Grãos de neve",
            "80,Pancadas de chuva fracas",
            "81,Pancadas de chuva moderadas",
            "82,Pancadas de chuva violentas",
            "85,Pancadas de neve fracas",
            "86,Pancadas de neve fortes",
            "95,Trovoada",
            "96,Trovoada com granizo fraco",
            "99,Trovoada com granizo forte"
    })
    void deveTraduzirCodigoWmo(int codigo, String esperado) {
        Assertions.assertThat(WeatherService.condition(codigo)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "código {0} → condição desconhecida")
    @MethodSource("codigosDesconhecidos")
    void deveTraduzirCodigoWmoDesconhecido(int codigo) {
        Assertions.assertThat(WeatherService.condition(codigo)).isEqualTo("Condição desconhecida");
    }

    static Stream<Integer> codigosDesconhecidos() {
        return Stream.of(-1, 4, 42, 100, 143, 200);
    }

    @ParameterizedTest(name = "{0}° → {1}")
    @CsvSource({
            "0,N",
            "22,N",
            "23,NE",
            "45,NE",
            "67,NE",
            "68,E",
            "90,E",
            "112,E",
            "113,SE",
            "135,SE",
            "157,SE",
            "158,S",
            "180,S",
            "202,S",
            "203,SO",
            "225,SO",
            "247,SO",
            "248,O",
            "270,O",
            "292,O",
            "293,NO",
            "315,NO",
            "337,NO",
            "338,N",
            "360,N"
    })
    void deveConverterGrausEmPontoCardeal(int graus, String esperado) {
        Assertions.assertThat(WeatherService.cardinalPoint(graus)).isEqualTo(esperado);
    }

    @Test
    void deveNormalizarGrausNegativos() {
        Assertions.assertThat(WeatherService.cardinalPoint(-45)).isEqualTo("NO");
        Assertions.assertThat(WeatherService.cardinalPoint(-90)).isEqualTo("O");
    }
}
