import { DecimalPipe } from '@angular/common';
import { Component, afterNextRender, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { WeatherResponse, WeatherService } from './weather-service';

/** Textos pt-BR do WMO weather code retornado pelo Open-Meteo. */
const WEATHER_DESCRIPTIONS: Record<number, string> = {
  0: 'Céu limpo',
  1: 'Predominantemente limpo',
  2: 'Parcialmente nublado',
  3: 'Encoberto',
  45: 'Neblina',
  48: 'Neblina com geada',
  51: 'Garoa fraca',
  53: 'Garoa moderada',
  55: 'Garoa intensa',
  56: 'Garoa congelante fraca',
  57: 'Garoa congelante intensa',
  61: 'Chuva fraca',
  63: 'Chuva moderada',
  65: 'Chuva forte',
  66: 'Chuva congelante fraca',
  67: 'Chuva congelante forte',
  71: 'Neve fraca',
  73: 'Neve moderada',
  75: 'Neve forte',
  77: 'Grãos de neve',
  80: 'Pancadas de chuva fracas',
  81: 'Pancadas de chuva moderadas',
  82: 'Pancadas de chuva fortes',
  85: 'Pancadas de neve fracas',
  86: 'Pancadas de neve fortes',
  95: 'Trovoada',
  96: 'Trovoada com granizo',
  99: 'Trovoada forte com granizo',
};

const DEFAULT_CITY = 'São Paulo';

@Component({
  selector: 'app-weather-panel',
  imports: [DecimalPipe],
  templateUrl: './weather-panel.html',
  styleUrl: './weather-panel.css',
})
export class WeatherPanel {
  private readonly weatherApi = inject(WeatherService);

  protected readonly city = signal('');
  protected readonly weather = signal<WeatherResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly locating = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly fromGeolocation = signal(false);

  private readonly weekdayFormatter = new Intl.DateTimeFormat('pt-BR', {
    weekday: 'short',
    timeZone: 'UTC',
  });

  constructor() {
    // A geolocation e a primeira busca só existem no navegador; o SSR renderiza
    // o estado inicial sem dados.
    afterNextRender(() => this.suggestCurrentLocation());
  }

  protected readonly placeLabel = computed(() => {
    const place = this.weather()?.place;
    if (!place) return '';
    return place.name ?? 'Minha localização';
  });

  protected readonly placeDetail = computed(() => {
    const place = this.weather()?.place;
    if (!place) return '';
    const region = [place.admin1, place.country].filter(Boolean).join(' · ');
    return region || `${place.latitude.toFixed(4)}, ${place.longitude.toFixed(4)}`;
  });

  protected onCityInput(event: Event): void {
    this.city.set((event.target as HTMLInputElement).value);
  }

  protected search(): void {
    const city = this.city().trim();
    if (!city || this.loading()) return;
    this.notice.set('');
    this.load(() => this.weatherApi.byCity(city), false);
  }

  protected useMyLocation(): void {
    if (this.locating()) return;
    this.locate();
  }

  /** Tenta a geolocation do navegador para sugerir o clima da cidade atual. */
  private suggestCurrentLocation(): void {
    if (!('geolocation' in navigator)) {
      this.searchDefaultCity('Navegador sem geolocation — mostrando um exemplo.');
      return;
    }
    this.locate();
  }

  private locate(): void {
    this.locating.set(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.locating.set(false);
        this.notice.set('');
        this.load(
          () => this.weatherApi.byCoordinates(position.coords.latitude, position.coords.longitude),
          true,
        );
      },
      () => {
        this.locating.set(false);
        this.searchDefaultCity('Não foi possível obter sua localização — mostrando um exemplo.');
      },
      { timeout: 10_000, maximumAge: 10 * 60 * 1000 },
    );
  }

  private searchDefaultCity(notice: string): void {
    this.city.set(DEFAULT_CITY);
    this.notice.set(notice);
    this.load(() => this.weatherApi.byCity(DEFAULT_CITY), false);
  }

  private load(request: () => Observable<WeatherResponse>, fromGeolocation: boolean): void {
    this.loading.set(true);
    this.error.set('');
    request().subscribe({
      next: (data) => {
        this.weather.set(data);
        this.fromGeolocation.set(fromGeolocation);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const body = (err as { error?: { message?: string } } | null)?.error?.message;
        this.weather.set(null);
        this.error.set(body || 'Não foi possível obter o clima agora. Tente novamente.');
        this.loading.set(false);
      },
    });
  }

  protected descriptionFor(code: number): string {
    return WEATHER_DESCRIPTIONS[code] ?? 'Condições variáveis';
  }

  protected iconFor(code: number, isDay = true): string {
    if (code <= 1) return isDay ? '☀️' : '🌙';
    if (code === 2) return isDay ? '⛅' : '☁️';
    if (code === 3) return '☁️';
    if (code === 45 || code === 48) return '🌫️';
    if ([51, 53, 55, 56, 57, 80, 81, 82].includes(code)) return '🌦️';
    if ([61, 63, 65, 66, 67].includes(code)) return '🌧️';
    if ([71, 73, 75, 77, 85, 86].includes(code)) return '🌨️';
    return '⛈️';
  }

  protected weekdayFor(date: string): string {
    // Meio-dia em UTC evita o dia "virar" por causa do fuso na formatação.
    return this.weekdayFormatter.format(new Date(`${date}T12:00:00Z`));
  }

  protected compassFor(degrees: number): string {
    const directions = ['N', 'NE', 'L', 'SE', 'S', 'SO', 'O', 'NO'];
    return directions[Math.round(degrees / 45) % 8];
  }
}
