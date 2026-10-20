package com.cezar.dev.airquality.clients;

import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import java.util.List;

import com.cezar.dev.airquality.config.OpenMeteoProperties;
import com.cezar.dev.airquality.dtos.OpenMeteoCurrent;
import com.cezar.dev.airquality.dtos.OpenMeteoForecastResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoGeocodingResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoPlace;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Única classe que chama a Open-Meteo. O RestClient (com timeouts de
 * application.properties) é montado em config/OpenMeteoConfig.
 */
@Component
public class OpenMeteoClient {

    private final RestClient restClient;
    private final OpenMeteoProperties properties;

    public OpenMeteoClient(RestClient restClient, OpenMeteoProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /** Busca cidades por nome; sem resultados, devolve lista vazia (não é erro). */
    public List<OpenMeteoPlace> searchCities(String name) {
        try {
            OpenMeteoGeocodingResponse response = this.restClient.get()
                    .uri(this.properties.geocodingUrl()
                            + "?name={name}&count=10&language=pt&format=json", name)
                    .retrieve()
                    .body(OpenMeteoGeocodingResponse.class);
            if (response == null || response.results() == null) {
                return List.of();
            }
            return response.results();
        } catch (RestClientResponseException exception) {
            throw new UpstreamException("O serviço de busca de cidades respondeu com erro.");
        } catch (ResourceAccessException exception) {
            throw translate(exception);
        } catch (RestClientException exception) {
            throw new UpstreamException("Não foi possível interpretar a resposta do serviço de busca de cidades.");
        }
    }

    /** Busca o clima atual nas coordenadas. */
    public OpenMeteoForecastResponse getForecast(double latitude, double longitude) {
        try {
            OpenMeteoForecastResponse response = this.restClient.get()
                    .uri(this.properties.forecastUrl()
                            + "?latitude={latitude}&longitude={longitude}"
                            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,"
                            + "precipitation,weather_code,wind_speed_10m,wind_direction_10m,is_day"
                            + "&wind_speed_unit=kmh&timezone=auto",
                            latitude, longitude)
                    .retrieve()
                    .body(OpenMeteoForecastResponse.class);
            if (response == null || response.current() == null) {
                throw new UpstreamException("Resposta incompleta do serviço de clima.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new UpstreamException("O serviço de clima respondeu com erro.");
        } catch (ResourceAccessException exception) {
            throw translate(exception);
        } catch (RestClientException exception) {
            throw new UpstreamException("Não foi possível interpretar a resposta do serviço de clima.");
        }
    }

    /** Erros de I/O: timeout de conexão/leitura → 504; demais falhas → 502. */
    private RuntimeException translate(ResourceAccessException exception) {
        Throwable cause = exception.getRootCause();
        if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
            return new UpstreamTimeoutException("O serviço de clima demorou para responder.");
        }
        return new UpstreamException("Não foi possível conectar ao serviço de clima.");
    }
}
