import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CityResponse {
  id: number;
  name: string;
  admin1: string | null;
  country: string | null;
  countryCode: string | null;
  latitude: number;
  longitude: number;
}

export interface WeatherResponse {
  latitude: number;
  longitude: number;
  timezone: string;
  observedAt: string;
  temperature: number;
  apparentTemperature: number;
  relativeHumidity: number;
  precipitation: number;
  windSpeed: number;
  windDirection: number;
  windDirectionCardinal: string;
  weatherCode: number;
  condition: string;
  isDay: boolean;
}

export interface ErrorResponse {
  code: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class WeatherApi {
  private readonly http = inject(HttpClient);

  getCities(name: string): Observable<CityResponse[]> {
    const params = new HttpParams().set('name', name);
    return this.http.get<CityResponse[]>('api/cities', { params });
  }

  getWeather(latitude: number, longitude: number): Observable<WeatherResponse> {
    const params = new HttpParams()
      .set('latitude', latitude.toString())
      .set('longitude', longitude.toString());
    return this.http.get<WeatherResponse>('api/weather', { params });
  }
}
