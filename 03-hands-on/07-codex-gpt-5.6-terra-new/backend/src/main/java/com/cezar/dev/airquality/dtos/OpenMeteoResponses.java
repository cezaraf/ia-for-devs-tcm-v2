package com.cezar.dev.airquality.dtos;

import java.util.List;

public final class OpenMeteoResponses {
  private OpenMeteoResponses() {}
  public record Geocoding(List<Result> results) {}
  public record Result(long id, String name, String admin1, String country, String country_code,
                       double latitude, double longitude) {}
  public record Forecast(double latitude, double longitude, String timezone, Current current) {}
  public record Current(String time, double temperature_2m, double apparent_temperature,
                        int relative_humidity_2m, double precipitation, int weather_code,
                        double wind_speed_10m, int wind_direction_10m, int is_day) {}
}
