package com.cezar.dev.airquality.config;

import java.net.http.HttpClient;

import com.cezar.dev.airquality.clients.OpenMeteoClient;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Monta o RestClient usado pela Open-Meteo com os timeouts de application.properties
 * (open-meteo.connect-timeout=3s, open-meteo.read-timeout=5s).
 *
 * <p>Os timeouts entram como {@link RestClientCustomizer} com {@code @Order(0)}: nos testes,
 * um customizer registrado depois (ordem maior) substitui a fábrica real pela do
 * {@code MockRestServiceServer}, sem chamar a Open-Meteo.
 */
@Configuration
public class OpenMeteoConfig {

    @Bean
    @Order(0)
    RestClientCustomizer openMeteoTimeoutCustomizer(OpenMeteoProperties properties) {
        return builder -> {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(properties.connectTimeout())
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(properties.readTimeout());
            builder.requestFactory(requestFactory);
        };
    }

    @Bean
    OpenMeteoClient openMeteoClient(RestClient.Builder builder, OpenMeteoProperties properties) {
        return new OpenMeteoClient(builder.build(), properties);
    }
}
