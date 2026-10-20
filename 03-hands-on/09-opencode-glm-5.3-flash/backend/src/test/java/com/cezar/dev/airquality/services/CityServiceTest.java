package com.cezar.dev.airquality.services;

import java.util.List;

import com.cezar.dev.airquality.clients.OpenMeteoClient;
import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.dtos.OpenMeteoPlace;
import com.cezar.dev.airquality.exceptions.CityNotFoundException;
import com.cezar.dev.airquality.exceptions.InvalidRequestException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CityServiceTest {

    private final OpenMeteoClient openMeteoClient = Mockito.mock(OpenMeteoClient.class);
    private final CityService cityService = new CityService(this.openMeteoClient);

    @Test
    void deveRejeitarNomeNulo() {
        Assertions.assertThatThrownBy(() -> this.cityService.searchCity(null))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> Assertions.assertThat(exception.getCode()).isEqualTo("INVALID_CITY_NAME"));
        Mockito.verifyNoInteractions(this.openMeteoClient);
    }

    @Test
    void deveRejeitarNomeComMenosDeDoisCaracteres() {
        Assertions.assertThatThrownBy(() -> this.cityService.searchCity("a"))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> Assertions.assertThat(exception.getCode()).isEqualTo("INVALID_CITY_NAME"));
        Mockito.verifyNoInteractions(this.openMeteoClient);
    }

    @Test
    void deveRejeitarNomeComMaisDeCemCaracteres() {
        String longo = "a".repeat(101);
        Assertions.assertThatThrownBy(() -> this.cityService.searchCity(longo))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> Assertions.assertThat(exception.getCode()).isEqualTo("INVALID_CITY_NAME"));
        Mockito.verifyNoInteractions(this.openMeteoClient);
    }

    @Test
    void deveRemoverEspacosDasExtremidadesAntesDeBuscar() {
        Mockito.when(this.openMeteoClient.searchCities("Curitiba"))
                .thenReturn(List.of(new OpenMeteoPlace(3454561L, "Curitiba", "Paraná", "Brasil", "BR", -25.43, -49.27)));

        this.cityService.searchCity("  Curitiba  ");

        Mockito.verify(this.openMeteoClient).searchCities("Curitiba");
    }

    @Test
    void deveRetornarCidadeNaoEncontradaQuandoNaoHaResultados() {
        Mockito.when(this.openMeteoClient.searchCities("Xyzabcqwe")).thenReturn(List.of());

        Assertions.assertThatThrownBy(() -> this.cityService.searchCity("Xyzabcqwe"))
                .isInstanceOf(CityNotFoundException.class);
    }

    @Test
    void deveMapearCamposDaCidade() {
        Mockito.when(this.openMeteoClient.searchCities("Curitiba"))
                .thenReturn(List.of(new OpenMeteoPlace(3454561L, "Curitiba", "Paraná", "Brasil", "BR", -25.43, -49.27)));

        List<CityResponse> resultado = this.cityService.searchCity("Curitiba");

        Assertions.assertThat(resultado).hasSize(1);
        CityResponse cidade = resultado.getFirst();
        Assertions.assertThat(cidade.id()).isEqualTo(3454561L);
        Assertions.assertThat(cidade.name()).isEqualTo("Curitiba");
        Assertions.assertThat(cidade.admin1()).isEqualTo("Paraná");
        Assertions.assertThat(cidade.country()).isEqualTo("Brasil");
        Assertions.assertThat(cidade.countryCode()).isEqualTo("BR");
        Assertions.assertThat(cidade.latitude()).isEqualTo(-25.43);
        Assertions.assertThat(cidade.longitude()).isEqualTo(-49.27);
    }

    @Test
    void deveMapearCamposNulosComoNulos() {
        Mockito.when(this.openMeteoClient.searchCities("Ilha"))
                .thenReturn(List.of(new OpenMeteoPlace(1L, "Ilha", null, null, null, 0.0, 0.0)));

        List<CityResponse> resultado = this.cityService.searchCity("Ilha");

        Assertions.assertThat(resultado.getFirst().admin1()).isNull();
        Assertions.assertThat(resultado.getFirst().country()).isNull();
        Assertions.assertThat(resultado.getFirst().countryCode()).isNull();
    }
}
