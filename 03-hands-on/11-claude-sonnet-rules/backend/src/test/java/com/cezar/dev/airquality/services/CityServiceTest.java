package com.cezar.dev.airquality.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CityServiceTest {

    private OpenMeteoClient client;
    private CityService service;

    @BeforeEach
    void setUp() {
        client = mock(OpenMeteoClient.class);
        service = new CityService(client);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "a", " a ", "abababababababababababababababababababababababababababababababababababababababababababababababababababababab"})
    void nomeInvalidoLancaInvalidCityName(String name) {
        InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> service.search(name));

        assertThat(e.getCode()).isEqualTo("INVALID_CITY_NAME");
        verifyNoInteractions(client);
    }

    @Test
    void aceitaLimitesDe2e100CaracteresAposTrim() {
        String hundred = "a".repeat(100);
        when(client.searchCities("ab")).thenReturn(List.of(result(1L, "Ab")));
        when(client.searchCities(hundred)).thenReturn(List.of(result(2L, "Longo")));

        assertThat(service.search("  ab  ")).hasSize(1);
        assertThat(service.search(hundred)).hasSize(1);
    }

    @Test
    void listaVaziaLancaCityNotFound() {
        when(client.searchCities("Xyzabcqwe")).thenReturn(List.of());

        assertThrows(CityNotFoundException.class, () -> service.search("Xyzabcqwe"));
    }

    @Test
    void mapeiaCamposIncluindoNulos() {
        when(client.searchCities("Curitiba")).thenReturn(List.of(
                new GeocodingApiResponse.Result(10L, "Curitiba", "Paraná", "Brasil", "BR", -25.43, -49.27),
                new GeocodingApiResponse.Result(11L, "Lugar", null, null, null, 1.0, 2.0)));

        List<CityResponse> cities = service.search("Curitiba");

        assertThat(cities.getFirst())
                .isEqualTo(new CityResponse(10L, "Curitiba", "Paraná", "Brasil", "BR", -25.43, -49.27));
        assertThat(cities.get(1))
                .isEqualTo(new CityResponse(11L, "Lugar", null, null, null, 1.0, 2.0));
    }

    @Test
    void limitaADezResultados() {
        List<GeocodingApiResponse.Result> many = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> result(i, "Cidade " + i)).toList();
        when(client.searchCities("Cidade")).thenReturn(many);

        assertThat(service.search("Cidade")).hasSize(10);
    }

    private static GeocodingApiResponse.Result result(long id, String name) {
        return new GeocodingApiResponse.Result(id, name, null, null, null, 0.0, 0.0);
    }
}
