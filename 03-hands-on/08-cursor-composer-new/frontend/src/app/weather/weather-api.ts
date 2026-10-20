import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface City {
  id: number;
  name: string;
  admin1: string | null;
  country: string | null;
  countryCode: string | null;
  latitude: number;
  longitude: number;
}

export interface Weather {
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

export interface ApiErrorBody {
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
    return this.http.get<Weather>('api/weather', {
      params: { latitude, longitude },
    });
  }
}
