package com.cezar.dev.airquality.clients;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

@Component
public class GeocodingApiClient {

    private static final Logger log = LoggerFactory.getLogger(GeocodingApiClient.class);
    private static final String API = "geocoding";

    private final RestClient restClient;
    private final String baseUrl;

    public GeocodingApiClient(@Qualifier("geocodingApiRestClient") RestClient restClient,
                              @Value("${geocoding-api.base-url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public GeocodingApiResponse search(String name) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/search")
                .queryParam("name", "{name}")
                .queryParam("count", 10)
                .queryParam("language", "pt")
                .queryParam("format", "json")
                .encode().buildAndExpand(name).toUri();
        long start = System.nanoTime();
        try {
            GeocodingApiResponse body = restClient.get().uri(uri).retrieve().body(GeocodingApiResponse.class);
            if (body == null) {
                warn("invalid-body", start);
                throw new UpstreamException("Resposta vazia da API geocoding");
            }
            return body;
        } catch (RestClientResponseException e) {
            warn("status=" + e.getStatusCode().value(), start);
            throw new UpstreamException("Status inesperado da API geocoding");
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                warn("timeout", start);
                throw new UpstreamTimeoutException("Timeout na API geocoding");
            }
            warn("io", start);
            throw new UpstreamException("Falha de conexão com a API geocoding");
        } catch (RestClientException e) {
            warn("invalid-body", start);
            throw new UpstreamException("Resposta inválida da API geocoding");
        }
    }

    private static void warn(String category, long startNanos) {
        log.warn("Falha na API {}: {} ({} ms)", API, category, (System.nanoTime() - startNanos) / 1_000_000);
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
