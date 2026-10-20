package com.cezar.dev.airquality.services;

/** Códigos WMO usados pela Open-Meteo traduzidos para descrição em português e ícone. */
final class WeatherCodes {

    record Info(String description, String icon) {
    }

    private WeatherCodes() {
    }

    static Info of(int code, boolean day) {
        return switch (code) {
            case 0 -> new Info(day ? "Céu limpo" : "Noite limpa", day ? "☀️" : "🌙");
            case 1 -> new Info("Predominantemente limpo", day ? "🌤️" : "🌙");
            case 2 -> new Info("Parcialmente nublado", "⛅");
            case 3 -> new Info("Nublado", "☁️");
            case 45, 48 -> new Info("Nevoeiro", "🌫️");
            case 51, 53, 55 -> new Info("Garoa", "🌦️");
            case 56, 57 -> new Info("Garoa congelante", "🌧️");
            case 61 -> new Info("Chuva fraca", "🌧️");
            case 63 -> new Info("Chuva moderada", "🌧️");
            case 65 -> new Info("Chuva forte", "🌧️");
            case 66, 67 -> new Info("Chuva congelante", "🌧️");
            case 71, 73, 75, 77 -> new Info("Neve", "❄️");
            case 80, 81, 82 -> new Info("Pancadas de chuva", "🌦️");
            case 85, 86 -> new Info("Pancadas de neve", "🌨️");
            case 95 -> new Info("Tempestade", "⛈️");
            case 96, 99 -> new Info("Tempestade com granizo", "⛈️");
            default -> new Info("Condição desconhecida", "🌡️");
        };
    }
}
