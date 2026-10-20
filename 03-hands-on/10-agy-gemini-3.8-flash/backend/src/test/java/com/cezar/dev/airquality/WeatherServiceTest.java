package com.cezar.dev.airquality;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.OpenMeteoForecastResponse;
import com.cezar.dev.airquality.dtos.WeatherResponse;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.services.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private OpenMeteoClient openMeteoClient;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(openMeteoClient);
    }

    @ParameterizedTest(name = "WMO code {0} -> {1}")
    @CsvSource({
            "0, Céu limpo",
            "1, Predominantemente limpo",
            "2, Parcialmente nublado",
            "3, Nublado",
            "45, Nevoeiro",
            "48, Nevoeiro com geada",
            "51, Garoa fraca",
            "53, Garoa moderada",
            "55, Garoa intensa",
            "56, Garoa congelante fraca",
            "57, Garoa congelante intensa",
            "61, Chuva fraca",
            "63, Chuva moderada",
            "65, Chuva forte",
            "66, Chuva congelante fraca",
            "67, Chuva congelante forte",
            "71, Neve fraca",
            "73, Neve moderada",
            "75, Neve forte",
            "77, Grãos de neve",
            "80, Pancadas de chuva fracas",
            "81, Pancadas de chuva moderadas",
            "82, Pancadas de chuva violentas",
            "85, Pancadas de neve fracas",
            "86, Pancadas de neve fortes",
            "95, Trovoada",
            "96, Trovoada com granizo fraco",
            "99, Trovoada com granizo forte",
            "100, Condição desconhecida",
            "-1, Condição desconhecida"
    })
    void testToCondition(int code, String expectedCondition) {
        assertThat(WeatherService.toCondition(code)).isEqualTo(expectedCondition);
    }

    @Test
    void testToConditionNull() {
        assertThat(WeatherService.toCondition(null)).isEqualTo("Condição desconhecida");
    }

    @ParameterizedTest(name = "Degrees {0} -> {1}")
    @CsvSource({
            "0, N",
            "15, N",
            "22, N",
            "23, NE",
            "45, NE",
            "67, NE",
            "68, E",
            "90, E",
            "112, E",
            "113, SE",
            "135, SE",
            "157, SE",
            "158, S",
            "180, S",
            "202, S",
            "203, SO",
            "225, SO",
            "247, SO",
            "248, O",
            "270, O",
            "292, O",
            "293, NO",
            "315, NO",
            "337, NO",
            "338, N",
            "360, N",
            "-45, NO"
    })
    void testToCardinal(int degrees, String expectedCardinal) {
        assertThat(WeatherService.toCardinal(degrees)).isEqualTo(expectedCardinal);
    }

    @Test
    void testToCardinalNull() {
        assertThat(WeatherService.toCardinal(null)).isEqualTo("N");
    }

    @Test
    @DisplayName("Should validate null latitude and longitude")
    void testValidateCoordinatesNull() {
        assertThatThrownBy(() -> weatherService.getWeather(null, -46.0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Latitude e longitude são obrigatórias");

        assertThatThrownBy(() -> weatherService.getWeather(-23.0, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Latitude e longitude são obrigatórias");
    }

    @Test
    @DisplayName("Should validate coordinates out of bounds")
    void testValidateCoordinatesOutOfBounds() {
        assertThatThrownBy(() -> weatherService.getWeather(-91.0, 0.0))
                .isInstanceOf(InvalidRequestException.class);

        assertThatThrownBy(() -> weatherService.getWeather(91.0, 0.0))
                .isInstanceOf(InvalidRequestException.class);

        assertThatThrownBy(() -> weatherService.getWeather(0.0, -181.0))
                .isInstanceOf(InvalidRequestException.class);

        assertThatThrownBy(() -> weatherService.getWeather(0.0, 181.0))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should map OpenMeteo response to WeatherResponse successfully")
    void testGetWeatherSuccess() {
        var current = new OpenMeteoForecastResponse.OpenMeteoCurrent(
                "2026-10-13T14:15",
                25.4,
                24.2,
                78,
                0.0,
                1,
                12.5,
                45,
                1
        );
        var mockResponse = new OpenMeteoForecastResponse(
                -23.55,
                -46.63,
                "America/Sao_Paulo",
                current
        );

        when(openMeteoClient.getWeather(-23.55, -46.63)).thenReturn(mockResponse);

        WeatherResponse response = weatherService.getWeather(-23.55, -46.63);

        assertThat(response.latitude()).isEqualTo(-23.55);
        assertThat(response.longitude()).isEqualTo(-46.63);
        assertThat(response.timezone()).isEqualTo("America/Sao_Paulo");
        assertThat(response.observedAt()).isEqualTo("2026-10-13T14:15");
        assertThat(response.temperature()).isEqualTo(25.4);
        assertThat(response.apparentTemperature()).isEqualTo(24.2);
        assertThat(response.relativeHumidity()).isEqualTo(78);
        assertThat(response.precipitation()).isEqualTo(0.0);
        assertThat(response.windSpeed()).isEqualTo(12.5);
        assertThat(response.windDirection()).isEqualTo(45);
        assertThat(response.windDirectionCardinal()).isEqualTo("NE");
        assertThat(response.weatherCode()).isEqualTo(1);
        assertThat(response.condition()).isEqualTo("Predominantemente limpo");
        assertThat(response.isDay()).isTrue();
    }

    @Test
    @DisplayName("Should map isDay false when is_day is 0")
    void testGetWeatherNight() {
        var current = new OpenMeteoForecastResponse.OpenMeteoCurrent(
                "2026-10-13T22:15",
                18.0,
                18.0,
                85,
                1.5,
                61,
                8.0,
                180,
                0
        );
        var mockResponse = new OpenMeteoForecastResponse(
                -23.55,
                -46.63,
                "America/Sao_Paulo",
                current
        );

        when(openMeteoClient.getWeather(-23.55, -46.63)).thenReturn(mockResponse);

        WeatherResponse response = weatherService.getWeather(-23.55, -46.63);

        assertThat(response.isDay()).isFalse();
        assertThat(response.condition()).isEqualTo("Chuva fraca");
        assertThat(response.windDirectionCardinal()).isEqualTo("S");
    }
}
