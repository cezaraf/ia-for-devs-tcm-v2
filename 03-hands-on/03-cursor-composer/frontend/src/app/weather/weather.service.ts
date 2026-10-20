import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { WeatherData } from './weather.models';

const API_BASE = '/03/api';

@Injectable({ providedIn: 'root' })
export class WeatherService {
  private readonly http = inject(HttpClient);

  getByCity(city: string): Observable<WeatherData> {
    const params = new HttpParams().set('city', city.trim());
    return this.http.get<WeatherData>(`${API_BASE}/weather`, { params });
  }

  getByCoordinates(latitude: number, longitude: number): Observable<WeatherData> {
    const params = new HttpParams()
      .set('latitude', latitude.toString())
      .set('longitude', longitude.toString());
    return this.http.get<WeatherData>(`${API_BASE}/weather`, { params });
  }
}
