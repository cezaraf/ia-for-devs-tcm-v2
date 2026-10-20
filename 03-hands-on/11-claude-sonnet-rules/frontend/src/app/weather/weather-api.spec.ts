import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { City, Weather, WeatherApi } from './weather-api';

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

  it('busca cidades em api/cities com o parâmetro name', () => {
    let result: City[] | undefined;
    api.cities('Santa Cruz').subscribe((r) => (result = r));

    const req = http.expectOne((r) => r.url === 'api/cities');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('name')).toBe('Santa Cruz');
    const cities: City[] = [
      {
        id: 1,
        name: 'Santa Cruz',
        admin1: null,
        country: 'Brasil',
        countryCode: 'BR',
        latitude: -1,
        longitude: -2,
      },
    ];
    req.flush(cities);

    expect(result).toEqual(cities);
  });

  it('busca o clima em api/weather com latitude e longitude', () => {
    let result: Weather | undefined;
    api.weather(-25.43, -49.27).subscribe((r) => (result = r));

    const req = http.expectOne((r) => r.url === 'api/weather');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('latitude')).toBe('-25.43');
    expect(req.request.params.get('longitude')).toBe('-49.27');
    const weather = { temperature: 20, isDay: true } as Weather;
    req.flush(weather);

    expect(result).toEqual(weather);
  });

  it('propaga o erro HTTP para quem chamou', () => {
    let status: number | undefined;
    api.cities('x').subscribe({ error: (e) => (status = e.status) });

    http
      .expectOne((r) => r.url === 'api/cities')
      .flush({ code: 'CITY_NOT_FOUND', message: 'm' }, { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });
});
