package com.cezar.dev.airquality.service;

import org.springframework.stereotype.Component;

@Component
public class WeatherCodeMapper {

    public record WeatherDescription(String condition, String icon) {}

    public WeatherDescription map(Integer code, Boolean isDay) {
        if (code == null) {
            return new WeatherDescription("Desconhecido", "❓");
        }

        boolean day = isDay == null || isDay;

        return switch (code) {
            case 0 -> new WeatherDescription("Céu limpo", day ? "☀️" : "🌙");
            case 1 -> new WeatherDescription("Predominantemente limpo", day ? "🌤️" : "🌤️");
            case 2 -> new WeatherDescription("Parcialmente nublado", day ? "⛅" : "☁️");
            case 3 -> new WeatherDescription("Nublado", "☁️");
            case 45 -> new WeatherDescription("Nevoeiro", "🌫️");
            case 48 -> new WeatherDescription("Nevoeiro com geada", "🌫️");
            case 51 -> new WeatherDescription("Garoa fraca", "🌦️");
            case 53 -> new WeatherDescription("Garoa moderada", "🌦️");
            case 55 -> new WeatherDescription("Garoa densa", "🌧️");
            case 56, 57 -> new WeatherDescription("Garoa congelante", "🌨️");
            case 61 -> new WeatherDescription("Chuva fraca", "🌧️");
            case 63 -> new WeatherDescription("Chuva moderada", "🌧️");
            case 65 -> new WeatherDescription("Chuva forte", "🌧️");
            case 66, 67 -> new WeatherDescription("Chuva congelante", "🌨️");
            case 71 -> new WeatherDescription("Queda de neve fraca", "🌨️");
            case 73 -> new WeatherDescription("Queda de neve moderada", "🌨️");
            case 75 -> new WeatherDescription("Queda de neve forte", "❄️");
            case 77 -> new WeatherDescription("Grãos de neve", "❄️");
            case 80 -> new WeatherDescription("Pancadas de chuva fracas", "🌦️");
            case 81 -> new WeatherDescription("Pancadas de chuva moderadas", "🌧️");
            case 82 -> new WeatherDescription("Pancadas de chuva violentas", "⛈️");
            case 85 -> new WeatherDescription("Pancadas de neve fracas", "🌨️");
            case 86 -> new WeatherDescription("Pancadas de neve fortes", "❄️");
            case 95 -> new WeatherDescription("Trovoada", "⛈️");
            case 96, 99 -> new WeatherDescription("Trovoada com granizo", "⛈️");
            default -> new WeatherDescription("Condições variáveis", day ? "🌤️" : "☁️");
        };
    }
}
