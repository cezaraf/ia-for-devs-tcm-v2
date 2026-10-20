import { provideHttpClient, withFetch } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { City, Weather, WeatherApi } from './weather-api';

describe('WeatherApi', () => {
  let api: WeatherApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withFetch()), provideHttpClientTesting()],
    });
    api = TestBed.inject(WeatherApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('deve buscar cidades em api/cities com o parâmetro name', () => {
    const cidades: City[] = [
      {
        id: 3454561,
        name: 'Curitiba',
        admin1: 'Paraná',
        country: 'Brasil',
        countryCode: 'BR',
        latitude: -25.43,
        longitude: -49.27,
      },
    ];
    let resultado: City[] | undefined;

    api.searchCities('Curitiba').subscribe((valor) => (resultado = valor));

    const req = http.expectOne('api/cities?name=Curitiba');
    expect(req.request.method).toBe('GET');
    req.flush(cidades);
    expect(resultado).toEqual(cidades);
  });

  it('deve buscar o clima em api/weather com latitude e longitude', () => {
    const clima: Weather = {
      latitude: -25.43,
      longitude: -49.27,
      timezone: 'America/Sao_Paulo',
      observedAt: '2026-10-13T14:15',
      temperature: 21.4,
      apparentTemperature: 24.1,
      relativeHumidity: 78,
      precipitation: 0,
      windSpeed: 12.3,
      windDirection: 45,
      windDirectionCardinal: 'NE',
      weatherCode: 2,
      condition: 'Parcialmente nublado',
      isDay: true,
    };
    let resultado: Weather | undefined;

    api.getWeather(-25.43, -49.27).subscribe((valor) => (resultado = valor));

    const req = http.expectOne('api/weather?latitude=-25.43&longitude=-49.27');
    expect(req.request.method).toBe('GET');
    req.flush(clima);
    expect(resultado).toEqual(clima);
  });

  it('deve propagar o erro do backend com o corpo { code, message }', () => {
    let erro: { code: string; message: string } | undefined;

    api.searchCities('Xyzabcqwe').subscribe({ error: (valor) => (erro = valor.error) });

    const req = http.expectOne('api/cities?name=Xyzabcqwe');
    req.flush({ code: 'CITY_NOT_FOUND', message: 'Cidade não encontrada.' }, { status: 404, statusText: 'Not Found' });
    expect(erro?.code).toBe('CITY_NOT_FOUND');
    expect(erro?.message).toBe('Cidade não encontrada.');
  });
});
