import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { WeatherService } from './services/weather.service';
import { of } from 'rxjs';
import { WeatherResponse } from './models/weather.model';

describe('App', () => {
  const mockWeatherData: WeatherResponse = {
    location: {
      name: 'Salvador',
      state: 'Bahia',
      country: 'Brasil',
      latitude: -12.97,
      longitude: -38.51,
      timezone: 'America/Bahia'
    },
    current: {
      temperature: 28,
      apparentTemperature: 30,
      humidity: 75,
      precipitation: 0,
      windSpeed: 15,
      windDirection: 90,
      cloudCover: 20,
      weatherCode: 0,
      condition: 'Céu limpo',
      icon: '☀️',
      isDay: true,
      observedAt: '2026-10-13T12:00'
    },
    dailyForecast: [
      {
        date: '2026-10-13',
        maxTemp: 29,
        minTemp: 24,
        weatherCode: 0,
        condition: 'Céu limpo',
        icon: '☀️'
      }
    ]
  };

  const weatherServiceMock = {
    getWeatherByCity: vi.fn().mockReturnValue(of(mockWeatherData)),
    getWeatherByCoordinates: vi.fn().mockReturnValue(of(mockWeatherData))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: WeatherService, useValue: weatherServiceMock }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render header title "Painel de Clima"', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Painel de Clima');
  });

  it('should display quick city chips', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const app = fixture.componentInstance;
    expect(app.quickCities.length).toBeGreaterThan(0);
  });

  it('should search for city on button click', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const app = fixture.componentInstance;
    app.cityName.set('São Paulo');
    app.onSearch();
    expect(weatherServiceMock.getWeatherByCity).toHaveBeenCalledWith('São Paulo');
  });
});
