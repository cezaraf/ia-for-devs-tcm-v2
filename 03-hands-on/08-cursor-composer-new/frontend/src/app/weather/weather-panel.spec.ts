import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WeatherPanel } from './weather-panel';

describe('WeatherPanel', () => {
  let fixture: ComponentFixture<WeatherPanel>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WeatherPanel],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(WeatherPanel);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
  });

  it('loads weather directly when one city is returned', async () => {
    const panel = fixture.componentInstance;
    panel['onSearch']('Curitiba');
    fixture.detectChanges();

    const citiesReq = http.expectOne((r) => r.url.includes('api/cities'));
    citiesReq.flush([
      {
        id: 1,
        name: 'Curitiba',
        admin1: 'Paraná',
        country: 'Brasil',
        countryCode: 'BR',
        latitude: -25.42,
        longitude: -49.27,
      },
    ]);
    fixture.detectChanges();

    const weatherReq = http.expectOne((r) => r.url.includes('api/weather'));
    weatherReq.flush({
      latitude: -25.42,
      longitude: -49.27,
      timezone: 'America/Sao_Paulo',
      observedAt: '2026-10-13T14:15',
      temperature: 22.3,
      apparentTemperature: 24,
      relativeHumidity: 78,
      precipitation: 0,
      windSpeed: 12,
      windDirection: 45,
      windDirectionCardinal: 'NE',
      weatherCode: 0,
      condition: 'Céu limpo',
      isDay: true,
    });
    fixture.detectChanges();
    await fixture.whenStable();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.panel')?.getAttribute('data-theme')).toBe('day');
    expect(root.textContent).toContain('Curitiba, Paraná, Brasil');
    expect(root.textContent).toContain('22°C');
  });

  it('shows city list when multiple cities are returned', () => {
    const panel = fixture.componentInstance;
    panel['onSearch']('Santa Cruz');
    fixture.detectChanges();

    const citiesReq = http.expectOne((r) => r.url.includes('api/cities'));
    citiesReq.flush([
      {
        id: 1,
        name: 'Santa Cruz',
        admin1: 'A',
        country: 'Brasil',
        countryCode: 'BR',
        latitude: 1,
        longitude: 1,
      },
      {
        id: 2,
        name: 'Santa Cruz',
        admin1: 'B',
        country: 'Chile',
        countryCode: 'CL',
        latitude: 2,
        longitude: 2,
      },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.list')).toBeTruthy();
  });

  it('shows retry on upstream error', () => {
    const panel = fixture.componentInstance;
    panel['onSearch']('Curitiba');
    fixture.detectChanges();

    const citiesReq = http.expectOne((r) => r.url.includes('api/cities'));
    citiesReq.flush({ code: 'UPSTREAM_ERROR', message: 'x' }, { status: 502, statusText: 'Bad Gateway' });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('O serviço de clima está indisponível no momento.');
    expect(el.textContent).toContain('tentar novamente');
  });

  it('applies night theme when isDay is false', () => {
    const panel = fixture.componentInstance;
    panel['onSearch']('Curitiba');
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('api/cities')).flush([
      {
        id: 1,
        name: 'Curitiba',
        admin1: null,
        country: null,
        countryCode: null,
        latitude: -25,
        longitude: -49,
      },
    ]);
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('api/weather')).flush({
      latitude: -25,
      longitude: -49,
      timezone: 'America/Sao_Paulo',
      observedAt: '2026-10-13T02:15',
      temperature: 10,
      apparentTemperature: 10,
      relativeHumidity: 80,
      precipitation: 0,
      windSpeed: 5,
      windDirection: 0,
      windDirectionCardinal: 'N',
      weatherCode: 0,
      condition: 'Céu limpo',
      isDay: false,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.panel')?.getAttribute('data-theme')).toBe('night');
    expect(fixture.nativeElement.textContent).toContain('[ NOITE ]');
  });
});
