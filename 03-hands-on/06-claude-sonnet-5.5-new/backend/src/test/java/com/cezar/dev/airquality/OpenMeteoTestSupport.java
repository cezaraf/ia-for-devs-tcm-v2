package com.cezar.dev.airquality;

import java.time.Duration;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.config.OpenMeteoProperties;

public final class OpenMeteoTestSupport {

    public static final OpenMeteoProperties PROPERTIES = new OpenMeteoProperties(
            "https://geocoding.test/v1/search",
            "https://forecast.test/v1/forecast",
            Duration.ofSeconds(3),
            Duration.ofSeconds(5));

    public static final String CURITIBA_JSON = """
            {"results":[{"id":6322752,"name":"Curitiba","latitude":-25.42778,"longitude":-49.27306,
            "country_code":"BR","admin1":"Paraná","country":"Brasil","timezone":"America/Sao_Paulo"}]}
            """;

    public static final String SANTA_CRUZ_JSON = """
            {"results":[
            {"id":1,"name":"Santa Cruz","admin1":"Rio Grande do Norte","country":"Brasil","country_code":"BR","latitude":-6.2,"longitude":-36.0},
            {"id":2,"name":"Santa Cruz","admin1":"California","country":"Estados Unidos","country_code":"US","latitude":36.9,"longitude":-122.0},
            {"id":3,"name":"Santa Cruz","admin1":null,"country":null,"country_code":null,"latitude":-17.8,"longitude":-63.1}]}
            """;

    public static final String WEATHER_JSON = """
            {"latitude":-25.5,"longitude":-49.25,"timezone":"America/Sao_Paulo","elevation":934.0,
            "current":{"time":"2026-10-13T14:15","interval":900,"temperature_2m":23.6,"apparent_temperature":24.1,
            "relative_humidity_2m":78,"precipitation":0.0,"weather_code":3,"wind_speed_10m":12.4,
            "wind_direction_10m":50,"is_day":1}}
            """;

    private OpenMeteoTestSupport() {
    }

    public static MockRestServiceServer bind(RestClient.Builder builder) {
        return MockRestServiceServer.bindTo(builder).build();
    }

    public static OpenMeteoClient client(RestClient.Builder builder) {
        return new OpenMeteoClient(builder, PROPERTIES);
    }
}
