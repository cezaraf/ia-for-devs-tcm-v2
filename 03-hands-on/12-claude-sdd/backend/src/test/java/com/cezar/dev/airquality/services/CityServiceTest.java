package com.cezar.dev.airquality.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.cezar.dev.airquality.clients.GeocodingApiClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse;
import com.cezar.dev.airquality.dtos.GeocodingApiResponse.Result;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.exceptions.UpstreamException;
import com.cezar.dev.airquality.exceptions.UpstreamTimeoutException;

class CityServiceTest {

    private GeocodingApiClient client;
    private CityService service;

    @BeforeEach
    void setUp() {
        client = mock(GeocodingApiClient.class);
        service = new CityService(client);
    }

    private static Result result(long id, String name, String featureCode) {
        return new Result(id, name, -8.05, -34.88, "Pernambuco", "Recife", "Brasil", "BR", featureCode,
                "America/Recife");
    }

    private void provider(Result... results) {
        when(client.search(anyString())).thenReturn(new GeocodingApiResponse(List.of(results)));
    }

    @Test
    @DisplayName("SCN-008 mantém só PPL* na ordem do provedor e mapeia os campos")
    void keepsOnlyInhabitedInOrder() {
        provider(result(1, "Recife", "PPLA"), result(2, "Aeroporto", "AIRP"), result(3, "Recife2", "PPL"),
                result(4, "Ilha", "ISL"));

        List<CityResponse> cities = service.search("Recife");

        assertThat(cities).extracting(CityResponse::id).containsExactly(1L, 3L);
        assertThat(cities.get(0)).isEqualTo(new CityResponse(1, "Recife", "Pernambuco", "Recife", "Brasil", "BR",
                -8.05, -34.88, "America/Recife"));
    }

    @Test
    @DisplayName("SCN-008 feature_code ausente é mantido e campos opcionais ausentes viram null")
    void missingFeatureCodeKeptAndOptionalNull() {
        provider(new Result(9L, "Vila", 1.0, 2.0, null, null, null, null, null, null));

        CityResponse city = service.search("Vila").get(0);

        assertThat(city).isEqualTo(new CityResponse(9, "Vila", null, null, null, null, 1.0, 2.0, null));
    }

    @Test
    @DisplayName("SCN-009 mais de 10 candidatas são cortadas nas 10 primeiras")
    void cutsAtTen() {
        provider(IntStream.rangeClosed(1, 12).mapToObj(i -> result(i, "C" + i, "PPL")).toArray(Result[]::new));

        assertThat(service.search("Cidade")).extracting(CityResponse::id)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    @DisplayName("SCN-010 results ausente gera CityNotFoundException")
    void resultsAbsent() {
        when(client.search(anyString())).thenReturn(new GeocodingApiResponse(null));

        assertThatThrownBy(() -> service.search("Xyz")).isInstanceOf(CityNotFoundException.class);
    }

    @Test
    @DisplayName("SCN-010 results vazio gera CityNotFoundException")
    void resultsEmpty() {
        provider();

        assertThatThrownBy(() -> service.search("Xyz")).isInstanceOf(CityNotFoundException.class);
    }

    @Test
    @DisplayName("SCN-010 só AIRP e ISL gera CityNotFoundException")
    void onlyNonInhabited() {
        provider(result(1, "Aeroporto", "AIRP"), result(2, "Ilha", "ISL"));

        assertThatThrownBy(() -> service.search("Xyz")).isInstanceOf(CityNotFoundException.class);
    }

    @ParameterizedTest(name = "SCN-011 nome inválido [{0}]")
    @NullSource
    @ValueSource(strings = {"", "  ", " a ", " a ", "﻿ a"})
    @DisplayName("SCN-011 nome inválido gera 400 sem chamar a Geocoding")
    void invalidNames(String name) {
        assertThatThrownBy(() -> service.search(name))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        e -> assertThat(e.getCode()).isEqualTo("INVALID_CITY_NAME"));
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("SCN-011 101 code points é inválido")
    void tooLong() {
        assertThatThrownBy(() -> service.search("a".repeat(101))).isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("SCN-011 101 code points com emoji (surrogates) é inválido")
    void tooLongEmoji() {
        assertThatThrownBy(() -> service.search("😀".repeat(101))).isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("SCN-011 2 e 100 code points são válidos")
    void limitsValid() {
        provider(result(1, "X", "PPL"));

        service.search("ab");
        service.search("a".repeat(100));

        verify(client).search("ab");
        verify(client).search("a".repeat(100));
    }

    @Test
    @DisplayName("SCN-011 emoji é contado por code point (100 emojis = 100 code points, 200 chars)")
    void emojiByCodePoint() {
        provider(result(1, "X", "PPL"));
        String hundred = "😀".repeat(100);

        service.search(hundred);

        verify(client).search(hundred);
    }

    @Test
    @DisplayName("SCN-011 U+FEFF e U+2007 nas extremidades são removidos e o valor normalizado é enviado")
    void normalizedValueSent() {
        provider(result(1, "X", "PPL"));

        service.search("﻿ab ");

        verify(client).search("ab");
    }

    @Test
    @DisplayName("SCN-011 espaços internos e acentos são preservados")
    void innerSpacesKept() {
        provider(result(1, "X", "PPL"));

        service.search("  São José  ");

        verify(client).search("São José");
    }

    @Test
    @DisplayName("SCN-012 item sem name gera UpstreamException")
    void itemWithoutName() {
        provider(result(1, null, "PPL"));

        assertThatThrownBy(() -> service.search("Recife")).isInstanceOf(UpstreamException.class);
    }

    @Test
    @DisplayName("SCN-012 item sem id, latitude ou longitude gera UpstreamException")
    void itemWithoutIdOrCoordinates() {
        provider(new Result(null, "A", 1.0, 2.0, null, null, null, null, "PPL", null));
        assertThatThrownBy(() -> service.search("Recife")).isInstanceOf(UpstreamException.class);

        provider(new Result(1L, "A", null, 2.0, null, null, null, null, "PPL", null));
        assertThatThrownBy(() -> service.search("Recife")).isInstanceOf(UpstreamException.class);

        provider(new Result(1L, "A", 1.0, null, null, null, null, null, "PPL", null));
        assertThatThrownBy(() -> service.search("Recife")).isInstanceOf(UpstreamException.class);
    }

    @Test
    @DisplayName("SCN-012 e SCN-013 exceções do client se propagam sem tradução")
    void clientExceptionsPropagate() {
        doThrow(new UpstreamException("x")).when(client).search(anyString());
        assertThatThrownBy(() -> service.search("Recife")).isInstanceOf(UpstreamException.class);

        doThrow(new UpstreamTimeoutException("x")).when(client).search(anyString());
        assertThatThrownBy(() -> service.search("Recife")).isInstanceOf(UpstreamTimeoutException.class);
    }
}
