import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WeatherApi } from './weather-api';
import { City, Weather } from './weather.models';

const city: City = {
  id: 1,
  name: 'Curitiba',
  admin1: 'Paraná',
  country: 'Brasil',
  countryCode: 'BR',
  latitude: -25.42,
  longitude: -49.27,
};

const weather: Weather = {
  latitude: -25.5,
  longitude: -49.25,
  timezone: 'America/Sao_Paulo',
  observedAt: '2026-10-13T14:15',
  temperature: 23.6,
  apparentTemperature: 24.1,
  relativeHumidity: 78,
  precipitation: 0,
  windSpeed: 12.4,
  windDirection: 50,
  windDirectionCardinal: 'NE',
  weatherCode: 3,
  condition: 'Nublado',
  isDay: true,
};

describe('WeatherApi', () => {
  let api: WeatherApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(WeatherApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches cities through the relative api/cities url', () => {
    let result: City[] | undefined;
    api.searchCities('Curitiba').subscribe((cities) => (result = cities));

    const req = http.expectOne((r) => r.url === 'api/cities');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('name')).toBe('Curitiba');
    req.flush([city]);

    expect(result).toEqual([city]);
  });

  it('gets the weather through the relative api/weather url', () => {
    let result: Weather | undefined;
    api.getWeather(-25.5, -49.25).subscribe((w) => (result = w));

    const req = http.expectOne((r) => r.url === 'api/weather');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('latitude')).toBe('-25.5');
    expect(req.request.params.get('longitude')).toBe('-49.25');
    req.flush(weather);

    expect(result).toEqual(weather);
  });

  it('propagates backend errors', () => {
    let status: number | undefined;
    api.searchCities('Xyzabcqwe').subscribe({ error: (e) => (status = e.status) });

    http
      .expectOne((r) => r.url === 'api/cities')
      .flush({ code: 'CITY_NOT_FOUND', message: 'x' }, { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });
});
