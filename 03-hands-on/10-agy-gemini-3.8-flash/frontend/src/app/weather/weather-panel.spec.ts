import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WeatherPanel } from './weather-panel';
import { CityResponse, WeatherResponse } from './weather-api';

describe('WeatherPanel', () => {
  let fixture: ComponentFixture<WeatherPanel>;
  let component: WeatherPanel;
  let httpTesting: HttpTestingController;

  const mockCityCuritiba: CityResponse = {
    id: 1,
    name: 'Curitiba',
    admin1: 'Paraná',
    country: 'Brasil',
    countryCode: 'BR',
    latitude: -25.4284,
    longitude: -49.2733
  };

  const mockCitySantaCruz1: CityResponse = {
    id: 10,
    name: 'Santa Cruz do Sul',
    admin1: 'Rio Grande do Sul',
    country: 'Brasil',
    countryCode: 'BR',
    latitude: -29.7139,
    longitude: -52.4258
  };

  const mockCitySantaCruz2: CityResponse = {
    id: 11,
    name: 'Santa Cruz',
    admin1: 'California',
    country: 'Estados Unidos',
    countryCode: 'US',
    latitude: 36.9741,
    longitude: -122.0308
  };

  const mockCityLondon: CityResponse = {
    id: 99,
    name: 'London',
    admin1: 'England',
    country: 'Reino Unido',
    countryCode: 'GB',
    latitude: 51.5074,
    longitude: -0.1278
  };

  const mockDayWeather: WeatherResponse = {
    latitude: -25.4284,
    longitude: -49.2733,
    timezone: 'America/Sao_Paulo',
    observedAt: '2026-10-14T14:30',
    temperature: 24.0,
    apparentTemperature: 24.2,
    relativeHumidity: 78,
    precipitation: 0.0,
    windSpeed: 12.0,
    windDirection: 45,
    windDirectionCardinal: 'NE',
    weatherCode: 0,
    condition: 'Céu limpo',
    isDay: true
  };

  const mockNightWeather: WeatherResponse = {
    latitude: -25.4284,
    longitude: -49.2733,
    timezone: 'America/Sao_Paulo',
    observedAt: '2026-10-14T22:30',
    temperature: 18.0,
    apparentTemperature: 18.0,
    relativeHumidity: 85,
    precipitation: 1.2,
    windSpeed: 8.0,
    windDirection: 180,
    windDirectionCardinal: 'S',
    weatherCode: 61,
    condition: 'Chuva fraca',
    isDay: false
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WeatherPanel],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    fixture = TestBed.createComponent(WeatherPanel);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.resetTestingModule();
  });

  it('should initialize in idle state with day theme', () => {
    expect(component.state()).toBe('idle');
    expect(component.theme()).toBe('day');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.panel-title')?.textContent).toContain('[clima]');
    expect(compiled.querySelector('.idle-message')?.textContent).toContain('digite uma cidade para ver o clima atual');
  });

  it('should validate query with less than 2 characters and not call backend', () => {
    component.onSearch('a');
    fixture.detectChanges();

    expect(component.state()).toBe('error');
    expect(component.errorMessage()).toBe('Digite ao menos 2 caracteres.');
    expect(component.canRetry()).toBe(false);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.error-text')?.textContent).toContain('Digite ao menos 2 caracteres.');
    httpTesting.expectNone('api/cities');
  });

  it('should handle flow with 1 result: load weather directly and display WeatherCard', async () => {
    component.onSearch('Curitiba');
    fixture.detectChanges();

    expect(component.state()).toBe('searching');

    const cityReq = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'Curitiba');
    cityReq.flush([mockCityCuritiba]);
    fixture.detectChanges();

    expect(component.state()).toBe('loading');

    const weatherReq = httpTesting.expectOne(
      (r) => r.url === 'api/weather' && r.params.get('latitude') === '-25.4284' && r.params.get('longitude') === '-49.2733'
    );
    weatherReq.flush(mockDayWeather);
    fixture.detectChanges();

    expect(component.state()).toBe('success');
    expect(component.theme()).toBe('day');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.location-label')?.textContent).toContain('Curitiba, Paraná, Brasil');
    expect(compiled.querySelector('.temperature')?.textContent).toContain('24°C');
    expect(compiled.querySelector('.condition')?.textContent).toContain('Céu limpo');
    expect(compiled.querySelector('.badge')?.textContent).toContain('[ DIA ]');
    expect(compiled.querySelector('.details-grid')?.textContent).toContain('[+] sensação  24°C');
    expect(compiled.querySelector('.details-grid')?.textContent).toContain('[+] umidade  78%');
    expect(compiled.querySelector('.details-grid')?.textContent).toContain('[+] vento  12 km/h NE');
    expect(compiled.querySelector('.details-grid')?.textContent).toContain('[+] precipitação  0.0 mm');
    expect(compiled.querySelector('.card-footer')?.textContent).toContain('medido às 14:30 (hora local)');
  });

  it('should handle flow with multiple results: show CityList and allow user to pick', async () => {
    component.onSearch('Santa Cruz');
    fixture.detectChanges();

    expect(component.state()).toBe('searching');

    const cityReq = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'Santa Cruz');
    cityReq.flush([mockCitySantaCruz1, mockCitySantaCruz2]);
    fixture.detectChanges();

    expect(component.state()).toBe('choosing');
    expect(component.cityResults().length).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    const cityButtons = compiled.querySelectorAll('.city-item');
    expect(cityButtons.length).toBe(2);
    expect(cityButtons[0].textContent).toContain('Santa Cruz do Sul — Rio Grande do Sul, Brasil');
    expect(cityButtons[1].textContent).toContain('Santa Cruz — California, Estados Unidos');

    // Select the second city
    (cityButtons[1] as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(component.state()).toBe('loading');

    const weatherReq = httpTesting.expectOne(
      (r) => r.url === 'api/weather' && r.params.get('latitude') === '36.9741' && r.params.get('longitude') === '-122.0308'
    );
    weatherReq.flush(mockDayWeather);
    fixture.detectChanges();

    expect(component.state()).toBe('success');
    expect(compiled.querySelector('.location-label')?.textContent).toContain('Santa Cruz, California, Estados Unidos');
  });

  it('should handle 404 city not found without retry button', () => {
    component.onSearch('CidadeInexistente');
    fixture.detectChanges();

    const cityReq = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'CidadeInexistente');
    cityReq.flush({ code: 'CITY_NOT_FOUND', message: 'Cidade não encontrada.' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(component.state()).toBe('error');
    expect(component.canRetry()).toBe(false);
    expect(component.errorMessage()).toBe('Cidade não encontrada. Verifique o nome e tente novamente.');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.error-text')?.textContent).toContain('Cidade não encontrada. Verifique o nome e tente novamente.');
    expect(compiled.querySelector('.btn-secondary')).toBeNull();
  });

  it('should handle 502 error with retry repeating the action', () => {
    component.onSearch('Curitiba');
    fixture.detectChanges();

    const cityReq = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'Curitiba');
    cityReq.flush({ code: 'UPSTREAM_ERROR', message: 'Upstream error' }, { status: 502, statusText: 'Bad Gateway' });
    fixture.detectChanges();

    expect(component.state()).toBe('error');
    expect(component.canRetry()).toBe(true);
    expect(component.errorMessage()).toBe('O serviço de clima está indisponível no momento.');

    const compiled = fixture.nativeElement as HTMLElement;
    const retryBtn = compiled.querySelector('.btn-secondary') as HTMLButtonElement;
    expect(retryBtn).not.toBeNull();
    expect(retryBtn.textContent).toContain('tentar novamente');

    // Click retry
    retryBtn.click();
    fixture.detectChanges();

    // Verifies that api/cities?name=Curitiba was requested again
    const retryReq = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'Curitiba');
    retryReq.flush([mockCityCuritiba]);
    fixture.detectChanges();

    const weatherReq = httpTesting.expectOne(
      (r) => r.url === 'api/weather' && r.params.get('latitude') === '-25.4284' && r.params.get('longitude') === '-49.2733'
    );
    weatherReq.flush(mockDayWeather);
    fixture.detectChanges();

    expect(component.state()).toBe('success');
  });

  it('should switch between day and night theme based on weather isDay', () => {
    // 1. Night weather
    component.onSearch('Curitiba');
    fixture.detectChanges();

    const cityReq = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'Curitiba');
    cityReq.flush([mockCityCuritiba]);
    fixture.detectChanges();

    const weatherReq = httpTesting.expectOne(
      (r) => r.url === 'api/weather' && r.params.get('latitude') === '-25.4284' && r.params.get('longitude') === '-49.2733'
    );
    weatherReq.flush(mockNightWeather);
    fixture.detectChanges();

    expect(component.state()).toBe('success');
    expect(component.theme()).toBe('night');

    const compiled = fixture.nativeElement as HTMLElement;
    const rootEl = compiled.querySelector('.weather-panel-root');
    expect(rootEl?.getAttribute('data-theme')).toBe('night');
    expect(compiled.querySelector('.badge')?.textContent).toContain('[ NOITE ]');

    // 2. Query again with Day weather
    component.onSearch('Curitiba');
    fixture.detectChanges();

    const cityReq2 = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'Curitiba');
    cityReq2.flush([mockCityCuritiba]);
    fixture.detectChanges();

    const weatherReq2 = httpTesting.expectOne(
      (r) => r.url === 'api/weather' && r.params.get('latitude') === '-25.4284' && r.params.get('longitude') === '-49.2733'
    );
    weatherReq2.flush(mockDayWeather);
    fixture.detectChanges();

    expect(component.state()).toBe('success');
    expect(component.theme()).toBe('day');
    expect(rootEl?.getAttribute('data-theme')).toBe('day');
    expect(compiled.querySelector('.badge')?.textContent).toContain('[ DIA ]');
  });

  it('should discard delayed response from older search when new search arrives', () => {
    // Search 1: Curitiba
    component.onSearch('Curitiba');
    fixture.detectChanges();

    const req1 = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'Curitiba');

    // Search 2: London
    component.onSearch('London');
    fixture.detectChanges();

    const req2 = httpTesting.expectOne((r) => r.url === 'api/cities' && r.params.get('name') === 'London');

    // Response 2 arrives first
    req2.flush([mockCityLondon]);
    fixture.detectChanges();

    const weatherReq2 = httpTesting.expectOne(
      (r) => r.url === 'api/weather' && r.params.get('latitude') === '51.5074' && r.params.get('longitude') === '-0.1278'
    );
    weatherReq2.flush(mockDayWeather);
    fixture.detectChanges();

    expect(component.state()).toBe('success');
    expect(component.currentLocationLabel()).toBe('London, England, Reino Unido');

    // Delayed response 1 arrives afterwards
    req1.flush([mockCityCuritiba]);
    fixture.detectChanges();

    // Should NOT have triggered weather for Curitiba and location remains London!
    httpTesting.expectNone((r) => r.url === 'api/weather' && r.params.get('latitude') === '-25.4284');
    expect(component.currentLocationLabel()).toBe('London, England, Reino Unido');
  });

  it('should handle geolocation permission denied error', () => {
    const originalGeo = navigator.geolocation;
    const mockGeolocation = {
      getCurrentPosition: (success: Function, error: Function) => {
        error({ code: 1, message: 'User denied geolocation' });
      }
    };
    Object.defineProperty(navigator, 'geolocation', {
      value: mockGeolocation,
      configurable: true
    });

    component.onGeolocationClick();
    fixture.detectChanges();

    expect(component.state()).toBe('error');
    expect(component.errorMessage()).toBe('Permissão de localização negada. Digite uma cidade.');
    expect(component.canRetry()).toBe(false);

    Object.defineProperty(navigator, 'geolocation', {
      value: originalGeo,
      configurable: true
    });
  });

  it('should handle geolocation position unavailable error with retry', () => {
    const originalGeo = navigator.geolocation;
    let callCount = 0;
    const mockGeolocation = {
      getCurrentPosition: (success: Function, error: Function) => {
        callCount++;
        error({ code: 2, message: 'Position unavailable' });
      }
    };
    Object.defineProperty(navigator, 'geolocation', {
      value: mockGeolocation,
      configurable: true
    });

    component.onGeolocationClick();
    fixture.detectChanges();

    expect(component.state()).toBe('error');
    expect(component.errorMessage()).toBe('Não foi possível obter sua localização.');
    expect(component.canRetry()).toBe(true);

    component.onRetry();
    fixture.detectChanges();

    expect(callCount).toBe(2);

    Object.defineProperty(navigator, 'geolocation', {
      value: originalGeo,
      configurable: true
    });
  });
});
