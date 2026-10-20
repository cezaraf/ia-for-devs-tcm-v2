import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { WeatherResponse } from './weather.model';

@Injectable({ providedIn: 'root' })
export class WeatherService {
  private readonly http = inject(HttpClient);

  // Relativo ao <base href>: atrás do Traefik vira /01/api/weather.
  private readonly url = 'api/weather';

  byCity(city: string): Observable<WeatherResponse> {
    return this.http.get<WeatherResponse>(this.url, { params: new HttpParams().set('city', city) });
  }

  byCoordinates(lat: number, lon: number): Observable<WeatherResponse> {
    const params = new HttpParams().set('lat', lat).set('lon', lon);
    return this.http.get<WeatherResponse>(this.url, { params });
  }
}
