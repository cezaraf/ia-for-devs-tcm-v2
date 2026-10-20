package com.cezar.dev.airquality.openmeteo;

import java.util.Map;

public final class WeatherCodeDescriptions {

    private static final Map<Integer, String> PT = Map.ofEntries(
            Map.entry(0, "Céu limpo"),
            Map.entry(1, "Principalmente limpo"),
            Map.entry(2, "Parcialmente nublado"),
            Map.entry(3, "Nublado"),
            Map.entry(45, "Neblina"),
            Map.entry(48, "Neblina com geada"),
            Map.entry(51, "Garoa leve"),
            Map.entry(53, "Garoa moderada"),
            Map.entry(55, "Garoa forte"),
            Map.entry(61, "Chuva leve"),
            Map.entry(63, "Chuva moderada"),
            Map.entry(65, "Chuva forte"),
            Map.entry(71, "Neve leve"),
            Map.entry(73, "Neve moderada"),
            Map.entry(75, "Neve forte"),
            Map.entry(80, "Pancadas de chuva leves"),
            Map.entry(81, "Pancadas de chuva moderadas"),
            Map.entry(82, "Pancadas de chuva violentas"),
            Map.entry(95, "Tempestade"),
            Map.entry(96, "Tempestade com granizo leve"),
            Map.entry(99, "Tempestade com granizo forte")
    );

    private WeatherCodeDescriptions() {}

    public static String describe(int code) {
        return PT.getOrDefault(code, "Condição meteorológica (código " + code + ")");
    }
}
