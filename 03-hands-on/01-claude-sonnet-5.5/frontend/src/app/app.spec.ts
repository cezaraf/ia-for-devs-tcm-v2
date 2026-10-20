import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { WeatherResponse } from './weather/weather.model';

const sample: WeatherResponse = {
  location: { name: 'Salvador', region: 'Bahia', country: 'Brasil', latitude: -12.97, longitude: -38.5, timezone: 'America/Bahia' },
  current: {
    time: '2026-10-13T15:00', temperature: 27.4, apparentTemperature: 29.1, humidity: 70, precipitation: 0,
    windSpeed: 12.5, windDirection: 90, day: true, weatherCode: 1, description: 'Predominantemente limpo', icon: '🌤️',
  },
  units: { temperature: '°C', humidity: '%', precipitation: 'mm', windSpeed: 'km/h' },
};

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the weather panel title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Painel de clima');
  });

  it('should fetch weather from the backend and display it', async () => {
    const fixture = TestBed.createComponent(App);
    const http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    const input = el.querySelector('input') as HTMLInputElement;
    input.value = 'Salvador';
    input.dispatchEvent(new Event('input'));
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const req = http.expectOne((r) => r.url === 'api/weather' && r.params.get('city') === 'Salvador');
    req.flush(sample);
    await fixture.whenStable();

    expect(el.textContent).toContain('Salvador, Bahia, Brasil');
    expect(el.textContent).toContain('27°C');
    expect(el.textContent).toContain('Predominantemente limpo');
  });

  it('should show a message when the city is not found', async () => {
    const fixture = TestBed.createComponent(App);
    const http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    const input = el.querySelector('input') as HTMLInputElement;
    input.value = 'Xyzzy';
    input.dispatchEvent(new Event('input'));
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    http.expectOne((r) => r.url === 'api/weather').flush({ message: 'x' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')?.textContent).toContain('Cidade não encontrada');
  });
});
