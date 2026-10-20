import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CityResponse, WeatherApi, WeatherResponse } from './weather-api';

describe('WeatherApi', () => {
  let service: WeatherApi;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        WeatherApi
      ]
    });
    service = TestBed.inject(WeatherApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call api/cities with query param name', () => {
    const mockCities: CityResponse[] = [
      {
        id: 1,
        name: 'Curitiba',
        admin1: 'Paraná',
        country: 'Brasil',
        countryCode: 'BR',
        latitude: -25.4284,
        longitude: -49.2733
      }
    ];

    service.getCities('Curitiba').subscribe((cities) => {
      expect(cities).toEqual(mockCities);
    });

    const req = httpTesting.expectOne((request) => {
      return request.url === 'api/cities' && request.params.get('name') === 'Curitiba';
    });
    expect(req.request.method).toBe('GET');
    req.flush(mockCities);
  });

  it('should call api/weather with query params latitude and longitude', () => {
    const mockWeather: WeatherResponse = {
      latitude: -25.4284,
      longitude: -49.2733,
      timezone: 'America/Sao_Paulo',
      observedAt: '2026-10-14T12:00',
      temperature: 21.4,
      apparentTemperature: 21.0,
      relativeHumidity: 65,
      precipitation: 0.0,
      windSpeed: 12.0,
      windDirection: 45,
      windDirectionCardinal: 'NE',
      weatherCode: 0,
      condition: 'Céu limpo',
      isDay: true
    };

    service.getWeather(-25.4284, -49.2733).subscribe((weather) => {
      expect(weather).toEqual(mockWeather);
    });

    const req = httpTesting.expectOne((request) => {
      return (
        request.url === 'api/weather' &&
        request.params.get('latitude') === '-25.4284' &&
        request.params.get('longitude') === '-49.2733'
      );
    });
    expect(req.request.method).toBe('GET');
    req.flush(mockWeather);
  });
});
