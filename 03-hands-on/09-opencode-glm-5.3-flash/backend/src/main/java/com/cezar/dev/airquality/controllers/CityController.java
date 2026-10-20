package com.cezar.dev.airquality.controllers;

import java.util.List;

import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.services.CityService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /cities?name={nome} — busca cidades para desambiguar homônimos. */
@RestController
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/cities")
    public List<CityResponse> searchCity(@RequestParam(required = false) String name) {
        return this.cityService.searchCity(name);
    }
}
