package com.cezar.dev.airquality.clients;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenMeteoConfig {

    @Bean
    RestClient geocodingApiRestClient(RestClient.Builder builder,
                                      @Value("${geocoding-api.connect-timeout}") Duration connectTimeout,
                                      @Value("${geocoding-api.read-timeout}") Duration readTimeout) {
        return build(builder, connectTimeout, readTimeout);
    }

    @Bean
    RestClient forecastApiRestClient(RestClient.Builder builder,
                                     @Value("${forecast-api.connect-timeout}") Duration connectTimeout,
                                     @Value("${forecast-api.read-timeout}") Duration readTimeout) {
        return build(builder, connectTimeout, readTimeout);
    }

    private static RestClient build(RestClient.Builder builder, Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return builder.clone().requestFactory(factory).build();
    }
}
