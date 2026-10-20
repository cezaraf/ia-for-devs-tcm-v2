import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { WeatherResponse } from '../models/weather.model';

@Injectable({
  providedIn: 'root'
})
export class WeatherService {
  private readonly http = inject(HttpClient);

  private getBaseUrl(): string {
    if (typeof window !== 'undefined') {
      const host = window.location.hostname;
      // Se estiver rodando na sandbox (sbx-*.cezar.dev)
      if (host.startsWith('sbx-') && host.endsWith('.cezar.dev')) {
        // Ex: sbx-agygeminiflash.cezar.dev -> sbx-agygeminiflash-api.cezar.dev
        const apiHost = host.replace(/^(sbx-[^.]+?)(?:\.cezar\.dev)$/, '$1-api.cezar.dev');
        return `https://${apiHost}`;
      }
    }
    // Fallback padrão para ambiente local / proxy do dev server
    return '/05/api';
  }

  getWeatherByCity(city: string): Observable<WeatherResponse> {
    const baseUrl = this.getBaseUrl();
    const params = new HttpParams().set('city', city.trim());
    return this.http.get<WeatherResponse>(`${baseUrl}/weather`, { params }).pipe(
      catchError(err => {
        // Se a chamada para apiHost falhou e estávamos usando direct host, tenta /api como fallback
        if (baseUrl !== '/api') {
          return this.http.get<WeatherResponse>('/05/api/weather', { params });
        }
        return throwError(() => err);
      })
    );
  }

  getWeatherByCoordinates(lat: number, lon: number): Observable<WeatherResponse> {
    const baseUrl = this.getBaseUrl();
    const params = new HttpParams()
      .set('lat', lat.toString())
      .set('lon', lon.toString());
    return this.http.get<WeatherResponse>(`${baseUrl}/weather`, { params }).pipe(
      catchError(err => {
        if (baseUrl !== '/api') {
          return this.http.get<WeatherResponse>('/05/api/weather', { params });
        }
        return throwError(() => err);
      })
    );
  }
}
