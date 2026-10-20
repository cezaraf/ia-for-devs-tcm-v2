package com.cezar.dev.airquality.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.cezar.dev.airquality.clients.OpenMeteoClient;

@Configuration
@EnableConfigurationProperties(OpenMeteoProperties.class)
public class OpenMeteoConfig {

    @Bean
    OpenMeteoClient openMeteoClient(RestClient.Builder builder, OpenMeteoProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return new OpenMeteoClient(builder.requestFactory(requestFactory), properties);
    }
}
