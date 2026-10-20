package com.cezar.dev.airquality.services;

import static com.cezar.dev.airquality.OpenMeteoTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

class CityServiceTest {

    private MockRestServiceServer server;
    private CityService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = bind(builder);
        service = new CityService(client(builder));
    }

    @Test
    void returnsMappedCities() {
        server.expect(requestTo(containsString("name=Curitiba")))
                .andExpect(requestTo(containsString("count=10")))
                .andExpect(requestTo(containsString("language=pt")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(CURITIBA_JSON, MediaType.APPLICATION_JSON));

        var cities = service.search("  Curitiba ");

        assertThat(cities).hasSize(1);
        var city = cities.getFirst();
        assertThat(city.id()).isEqualTo(6322752L);
        assertThat(city.name()).isEqualTo("Curitiba");
        assertThat(city.admin1()).isEqualTo("Paraná");
        assertThat(city.country()).isEqualTo("Brasil");
        assertThat(city.countryCode()).isEqualTo("BR");
        assertThat(city.latitude()).isEqualTo(-25.42778);
        assertThat(city.longitude()).isEqualTo(-49.27306);
        server.verify();
    }

    @Test
    void keepsNullRegionAndCountry() {
        server.expect(requestTo(containsString("name=Santa%20Cruz")))
                .andRespond(withSuccess(SANTA_CRUZ_JSON, MediaType.APPLICATION_JSON));

        var cities = service.search("Santa Cruz");

        assertThat(cities).hasSize(3);
        assertThat(cities.get(2).admin1()).isNull();
        assertThat(cities.get(2).country()).isNull();
    }

    @Test
    void rejectsShortBlankAndLongNamesWithoutCallingUpstream() {
        for (String name : new String[] { null, "", "   ", "a", " a ", "x".repeat(101) }) {
            assertThatThrownBy(() -> service.search(name))
                    .isInstanceOfSatisfying(InvalidRequestException.class,
                            e -> assertThat(e.getCode()).isEqualTo("INVALID_CITY_NAME"));
        }
        server.verify();
    }

    @Test
    void acceptsLimitLengths() {
        server.expect(requestTo(containsString("name=ab")))
                .andRespond(withSuccess(CURITIBA_JSON, MediaType.APPLICATION_JSON));
        assertThat(service.search("ab")).hasSize(1);
    }

    @Test
    void throwsNotFoundWhenResultsKeyIsMissing() {
        server.expect(requestTo(containsString("name=Xyzabcqwe")))
                .andRespond(withSuccess("{\"generationtime_ms\":0.5}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("Xyzabcqwe")).isInstanceOf(CityNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenResultsIsEmpty() {
        server.expect(requestTo(containsString("name=Nada")))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("Nada")).isInstanceOf(CityNotFoundException.class);
    }

    @Test
    void mapsServerErrorToUpstreamException() {
        server.expect(requestTo(containsString("name=Curitiba"))).andRespond(withServerError());

        assertThatThrownBy(() -> service.search("Curitiba")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void mapsInvalidBodyToUpstreamException() {
        server.expect(requestTo(containsString("name=Curitiba")))
                .andRespond(withSuccess("isto não é json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("Curitiba")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void mapsConnectionFailureToUpstreamException() {
        server.expect(requestTo(containsString("name=Curitiba")))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> service.search("Curitiba")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void mapsTimeoutToUpstreamTimeoutException() {
        server.expect(requestTo(containsString("name=Curitiba")))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> service.search("Curitiba")).isInstanceOf(UpstreamTimeoutException.class);
    }
}
