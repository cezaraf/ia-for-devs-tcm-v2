package com.cezar.dev.airquality.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherCodeMapperTest {

    private WeatherCodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeatherCodeMapper();
    }

    @Test
    void testClearSkyDayAndNight() {
        var day = mapper.map(0, true);
        assertEquals("Céu limpo", day.condition());
        assertEquals("☀️", day.icon());

        var night = mapper.map(0, false);
        assertEquals("Céu limpo", night.condition());
        assertEquals("🌙", night.icon());
    }

    @Test
    void testRainCodes() {
        var rain = mapper.map(61, true);
        assertEquals("Chuva fraca", rain.condition());
        assertEquals("🌧️", rain.icon());

        var thunderstorm = mapper.map(95, true);
        assertEquals("Trovoada", thunderstorm.condition());
        assertEquals("⛈️", thunderstorm.icon());
    }

    @Test
    void testUnknownCode() {
        var unknown = mapper.map(999, true);
        assertEquals("Condições variáveis", unknown.condition());
    }

    @Test
    void testNullCode() {
        var nullResult = mapper.map(null, true);
        assertEquals("Desconhecido", nullResult.condition());
        assertEquals("❓", nullResult.icon());
    }
}
