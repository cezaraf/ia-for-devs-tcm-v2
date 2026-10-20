package com.cezar.dev.airquality.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.cezar.dev.airquality.exceptions.InvalidRequestException;

class CityServiceTest {

    @Test
    void validateCityName_trimsAndAcceptsValid() {
        assertThat(CityService.validateCityName("  Curitiba  ")).isEqualTo("Curitiba");
    }

    @Test
    void validateCityName_rejectsTooShort() {
        assertThatThrownBy(() -> CityService.validateCityName("a"))
                .isInstanceOf(InvalidRequestException.class)
                .extracting(ex -> ((InvalidRequestException) ex).getCode())
                .isEqualTo("INVALID_CITY_NAME");
    }

    @Test
    void validateCityName_rejectsEmpty() {
        assertThatThrownBy(() -> CityService.validateCityName("   "))
                .isInstanceOf(InvalidRequestException.class);
    }
}
