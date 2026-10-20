import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface WeatherPlace {
  name: string | null;
  admin1: string | null;
  country: string | null;
  countryCode: string | null;
  latitude: number;
  longitude: number;
  timezone: string | null;
}

export interface WeatherCurrent {
  observedAt: string;
  temperature: number;
  apparentTemperature: number;
  humidity: number;
  isDay: boolean;
  precipitation: number;
  weatherCode: number;
  cloudCover: number;
  windSpeed: number;
  windDirection: number;
}

export interface DailyForecast {
  date: string;
  tempMax: number;
  tempMin: number;
  precipitationProbability: number | null;
  weatherCode: number;
}

export interface WeatherUnits {
  temperature: string;
  windSpeed: string;
  precipitation: string;
}

export interface WeatherResponse {
  place: WeatherPlace;
  current: WeatherCurrent;
  daily: DailyForecast[];
  units: WeatherUnits;
}

/**
 * Acesso ao backend. O painel só fala com a nossa API (`api/weather`), nunca
 * direto com o Open-Meteo — quem resolve a cidade em coordenadas e busca o
 * clima é o backend.
 */
@Injectable({ providedIn: 'root' })
export class WeatherService {
  private readonly http = inject(HttpClient);

  /** Clima da cidade informada (o backend resolve as coordenadas). */
  byCity(city: string) {
    return this.http.get<WeatherResponse>('api/weather', { params: { city } });
  }

  /** Clima pelas coordenadas (usado com a geolocation do navegador). */
  byCoordinates(latitude: number, longitude: number) {
    return this.http.get<WeatherResponse>('api/weather', { params: { latitude, longitude } });
  }
}
