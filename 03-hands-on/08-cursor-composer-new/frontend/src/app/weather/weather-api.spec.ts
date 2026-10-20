import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { WeatherApi } from './weather-api';

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

  afterEach(() => {
    http.verify();
  });

  it('searchCities calls api/cities', () => {
    api.searchCities('Curitiba').subscribe();
    const req = http.expectOne((r) => r.url.includes('api/cities') && r.params.get('name') === 'Curitiba');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'Curitiba', admin1: null, country: null, countryCode: null, latitude: 0, longitude: 0 }]);
  });

  it('getWeather calls api/weather', () => {
    api.getWeather(-25, -49).subscribe();
    const req = http.expectOne((r) => r.url.includes('api/weather'));
    expect(req.request.params.get('latitude')).toBe('-25');
    expect(req.request.params.get('longitude')).toBe('-49');
    req.flush({
      latitude: -25,
      longitude: -49,
      timezone: 'America/Sao_Paulo',
      observedAt: '2026-10-13T14:15',
      temperature: 20,
      apparentTemperature: 21,
      relativeHumidity: 50,
      precipitation: 0,
      windSpeed: 10,
      windDirection: 90,
      windDirectionCardinal: 'E',
      weatherCode: 0,
      condition: 'Céu limpo',
      isDay: true,
    });
  });
});
