import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { City, Weather, WeatherApi } from './weather-api';

const city: City = {
  id: 3448744,
  name: 'São José',
  region: 'Santa Catarina',
  subregion: 'São José',
  country: 'Brasil',
  countryCode: 'BR',
  latitude: -27.61528,
  longitude: -48.6275,
  timezone: 'America/Sao_Paulo',
};

const weather: Weather = {
  latitude: -8.05,
  longitude: -34.88,
  timezone: 'America/Recife',
  observedAt: '2026-10-20T16:00',
  temperature: 28.6,
  apparentTemperature: 30.8,
  humidity: 62,
  windSpeed: 12.7,
  windDirectionDegrees: 111.0,
  windDirection: 'Leste',
  weatherCode: 1,
  condition: 'Predominantemente limpo',
  isDay: true,
};

describe('WeatherApi', () => {
  let api: WeatherApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(WeatherApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('TST-011 searchCities faz GET relativo api/cities com params name', () => {
    let result: City[] | undefined;
    api.searchCities('São José').subscribe((r) => (result = r));

    const req = httpMock.expectOne((r) => r.url === 'api/cities');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual(['name']);
    expect(req.request.params.get('name')).toBe('São José');
    req.flush([city]);

    expect(result).toEqual([city]);
  });

  it('TST-011 getWeather faz GET relativo api/weather com params latitude e longitude', () => {
    let result: Weather | undefined;
    api.getWeather(-8.05, -34.88).subscribe((r) => (result = r));

    const req = httpMock.expectOne((r) => r.url === 'api/weather');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().sort()).toEqual(['latitude', 'longitude']);
    expect(req.request.params.get('latitude')).toBe('-8.05');
    expect(req.request.params.get('longitude')).toBe('-34.88');
    req.flush(weather);

    expect(result).toEqual(weather);
  });

  it('TST-011 as URLs não têm barra inicial nem host', () => {
    api.searchCities('Recife').subscribe();
    api.getWeather(1, 2).subscribe();

    const reqs = [
      httpMock.expectOne((r) => r.url === 'api/cities'),
      httpMock.expectOne((r) => r.url === 'api/weather'),
    ];
    for (const req of reqs) {
      expect(req.request.url).not.toMatch(/^\/|^https?:/);
      req.flush(null);
    }
  });
});
