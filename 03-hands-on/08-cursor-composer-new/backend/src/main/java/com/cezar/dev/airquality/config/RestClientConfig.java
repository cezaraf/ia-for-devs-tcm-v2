package com.cezar.dev.airquality.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@Profile("!test")
public class RestClientConfig {

    @Bean
    RestClientCustomizer openMeteoRestClientCustomizer(
            @Value("${open-meteo.connect-timeout}") Duration connectTimeout,
            @Value("${open-meteo.read-timeout}") Duration readTimeout) {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);
            builder.requestFactory(requestFactory);
        };
    }
}
