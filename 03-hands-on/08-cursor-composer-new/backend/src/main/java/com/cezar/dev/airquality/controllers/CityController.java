package com.cezar.dev.airquality.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cezar.dev.airquality.dtos.CityResponse;
import com.cezar.dev.airquality.services.CityService;

@RestController
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/cities")
    public List<CityResponse> searchCities(@RequestParam String name) {
        return cityService.searchCities(name);
    }
}
