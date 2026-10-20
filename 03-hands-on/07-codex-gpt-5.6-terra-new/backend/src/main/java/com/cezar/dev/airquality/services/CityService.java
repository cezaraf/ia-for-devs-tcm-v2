package com.cezar.dev.airquality.services;
import com.cezar.dev.airquality.clients.OpenMeteoClient; import com.cezar.dev.airquality.dtos.*; import com.cezar.dev.airquality.exceptions.*;
import java.util.*; import org.springframework.stereotype.Service;
@Service public class CityService { private final OpenMeteoClient client; public CityService(OpenMeteoClient client) { this.client=client; }
 public List<CityResponse> search(String name) { if(name==null || name.trim().length()<2 || name.trim().length()>100) throw new InvalidRequestException("INVALID_CITY_NAME"); var response=client.cities(name.trim()); if(response.results()==null || response.results().isEmpty()) throw new CityNotFoundException(); return response.results().stream().limit(10).map(r->new CityResponse(r.id(),r.name(),r.admin1(),r.country(),r.country_code(),r.latitude(),r.longitude())).toList(); } }
