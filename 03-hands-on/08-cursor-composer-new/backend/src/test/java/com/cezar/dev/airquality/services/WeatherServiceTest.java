package com.cezar.dev.airquality.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.cezar.dev.airquality.exceptions.InvalidRequestException;

class WeatherServiceTest {

    @Test
    void toCardinal_mapsEightSectors() {
        assertThat(WeatherService.toCardinal(0)).isEqualTo("N");
        assertThat(WeatherService.toCardinal(45)).isEqualTo("NE");
        assertThat(WeatherService.toCardinal(90)).isEqualTo("E");
        assertThat(WeatherService.toCardinal(135)).isEqualTo("SE");
        assertThat(WeatherService.toCardinal(180)).isEqualTo("S");
        assertThat(WeatherService.toCardinal(225)).isEqualTo("SO");
        assertThat(WeatherService.toCardinal(270)).isEqualTo("O");
        assertThat(WeatherService.toCardinal(315)).isEqualTo("NO");
    }

    @Test
    void translateWeatherCode_knownAndUnknown() {
        assertThat(WeatherService.translateWeatherCode(0)).isEqualTo("Céu limpo");
        assertThat(WeatherService.translateWeatherCode(95)).isEqualTo("Trovoada");
        assertThat(WeatherService.translateWeatherCode(123)).isEqualTo("Condição desconhecida");
    }

    @Test
    void validateLatitude_rejectsOutOfRange() {
        assertThatThrownBy(() -> WeatherService.validateLatitude(200.0))
                .isInstanceOf(InvalidRequestException.class)
                .extracting(ex -> ((InvalidRequestException) ex).getCode())
                .isEqualTo("INVALID_COORDINATES");
    }

    @Test
    void validateLongitude_rejectsOutOfRange() {
        assertThatThrownBy(() -> WeatherService.validateLongitude(null))
                .isInstanceOf(InvalidRequestException.class);
    }
}
