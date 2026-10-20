import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface City {
  id: number;
  name: string;
  region: string | null;
  subregion: string | null;
  country: string | null;
  countryCode: string | null;
  latitude: number;
  longitude: number;
  timezone: string | null;
}

export interface Weather {
  latitude: number;
  longitude: number;
  timezone: string | null;
  observedAt: string;
  temperature: number | null;
  apparentTemperature: number | null;
  humidity: number | null;
  windSpeed: number | null;
  windDirectionDegrees: number | null;
  windDirection: string | null;
  weatherCode: number | null;
  condition: string;
  isDay: boolean | null;
}

export interface ApiError {
  code: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class WeatherApi {
  private readonly http = inject(HttpClient);

  searchCities(name: string): Observable<City[]> {
    return this.http.get<City[]>('api/cities', { params: { name } });
  }

  getWeather(latitude: number, longitude: number): Observable<Weather> {
    return this.http.get<Weather>('api/weather', { params: { latitude, longitude } });
  }
}
