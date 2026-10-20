package com.cezar.dev.airquality.controllers;

import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.services.CityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/cities")
    public ResponseEntity<List<CityResponse>> getCities(@RequestParam(value = "name", required = false) String name) {
        List<CityResponse> cities = cityService.searchCities(name);
        return ResponseEntity.ok(cities);
    }
}
