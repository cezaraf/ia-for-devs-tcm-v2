import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { City, Weather } from './weather.models';

/** Único ponto de HTTP do front: fala somente com o backend (URLs relativas ao baseHref). */
@Injectable({ providedIn: 'root' })
export class WeatherApi {
  private readonly http = inject(HttpClient);

  searchCities(name: string): Observable<City[]> {
    return this.http.get<City[]>('api/cities', { params: { name } });
  }

  getWeather(latitude: number, longitude: number): Observable<Weather> {
    return this.http.get<Weather>('api/weather', {
      params: { latitude: String(latitude), longitude: String(longitude) },
    });
  }
}
