import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Cidade retornada por GET api/cities. */
export interface City {
  id: number;
  name: string;
  admin1: string | null;
  country: string | null;
  countryCode: string | null;
  latitude: number;
  longitude: number;
}

/** Clima atual retornado por GET api/weather. */
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

/** Corpo de erro padrão do backend. */
export interface ApiError {
  code: string;
  message: string;
}

/**
 * Único ponto que faz HTTP: chama somente o backend, com URLs relativas ao baseHref
 * (o Traefik remove o prefixo /<base>/api antes de chegar ao Spring).
 */
@Injectable({ providedIn: 'root' })
export class WeatherApi {
  private readonly http = inject(HttpClient);

  searchCities(name: string): Observable<City[]> {
    const params = new HttpParams().set('name', name);
    return this.http.get<City[]>('api/cities', { params });
  }

  getWeather(latitude: number, longitude: number): Observable<Weather> {
    const params = new HttpParams()
      .set('latitude', String(latitude))
      .set('longitude', String(longitude));
    return this.http.get<Weather>('api/weather', { params });
  }
}
