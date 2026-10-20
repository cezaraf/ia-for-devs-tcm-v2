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
    RestClient openMeteoRestClient(RestClient.Builder builder,
                                   @Value("${open-meteo.connect-timeout}") Duration connectTimeout,
                                   @Value("${open-meteo.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return builder.requestFactory(factory).build();
    }
}
