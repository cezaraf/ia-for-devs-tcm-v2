package com.cezar.dev.airquality;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoGeocodingResponse;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;
import com.cezar.dev.airquality.services.CityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private OpenMeteoClient openMeteoClient;

    private CityService cityService;

    @BeforeEach
    void setUp() {
        cityService = new CityService(openMeteoClient);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when name is null or blank")
    void testSearchCitiesNullOrBlank() {
        assertThatThrownBy(() -> cityService.searchCities(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Nome da cidade é obrigatório");

        assertThatThrownBy(() -> cityService.searchCities(" "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Nome da cidade deve conter entre 2 e 100 caracteres");
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when name has less than 2 or more than 100 chars")
    void testSearchCitiesInvalidLength() {
        assertThatThrownBy(() -> cityService.searchCities("a"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("entre 2 e 100 caracteres");

        String longName = "a".repeat(101);
        assertThatThrownBy(() -> cityService.searchCities(longName))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("entre 2 e 100 caracteres");
    }

    @Test
    @DisplayName("Should throw CityNotFoundException when OpenMeteo returns null or empty results")
    void testSearchCitiesNotFound() {
        when(openMeteoClient.searchCities("Xyznotfound")).thenReturn(null);
        assertThatThrownBy(() -> cityService.searchCities("Xyznotfound"))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessageContaining("Cidade não encontrada");

        when(openMeteoClient.searchCities("Xyznotfound2")).thenReturn(new OpenMeteoGeocodingResponse(null));
        assertThatThrownBy(() -> cityService.searchCities("Xyznotfound2"))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessageContaining("Cidade não encontrada");

        when(openMeteoClient.searchCities("Xyznotfound3")).thenReturn(new OpenMeteoGeocodingResponse(Collections.emptyList()));
        assertThatThrownBy(() -> cityService.searchCities("Xyznotfound3"))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessageContaining("Cidade não encontrada");
    }

    @Test
    @DisplayName("Should return mapped city list and trim input")
    void testSearchCitiesSuccess() {
        var cityResult = new OpenMeteoGeocodingResponse.OpenMeteoCityResult(
                3448439L,
                "São Paulo",
                -23.5475,
                -46.63611,
                "BR",
                "Brasil",
                "São Paulo"
        );
        when(openMeteoClient.searchCities("São Paulo"))
                .thenReturn(new OpenMeteoGeocodingResponse(List.of(cityResult)));

        List<CityResponse> responses = cityService.searchCities("  São Paulo  ");

        verify(openMeteoClient).searchCities("São Paulo");
        assertThat(responses).hasSize(1);
        CityResponse city = responses.getFirst();
        assertThat(city.id()).isEqualTo(3448439L);
        assertThat(city.name()).isEqualTo("São Paulo");
        assertThat(city.admin1()).isEqualTo("São Paulo");
        assertThat(city.country()).isEqualTo("Brasil");
        assertThat(city.countryCode()).isEqualTo("BR");
        assertThat(city.latitude()).isEqualTo(-23.5475);
        assertThat(city.longitude()).isEqualTo(-46.63611);
    }
}
