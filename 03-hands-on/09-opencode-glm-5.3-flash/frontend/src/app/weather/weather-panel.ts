import { HttpErrorResponse } from '@angular/common/http';
import { Component, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

import { CitySearchForm } from './city-search-form';
import { CityList } from './city-list';
import { ErrorMessage } from './error-message';
import { WeatherCard } from './weather-card';
import { WeatherCardSkeleton } from './weather-card-skeleton';
import { City, Weather, WeatherApi } from './weather-api';

type PanelState = 'idle' | 'searching' | 'choosing' | 'loading' | 'success' | 'error';

interface PanelError {
  message: string;
  retryable: boolean;
}

const GEOLOCATION_OPTIONS: PositionOptions = {
  timeout: 10000,
  maximumAge: 600000,
};

/**
 * Página inteira: título [clima], formulário e área de resultado. Orquestra o estado
 * (idle | searching | choosing | loading | success | error), aplica data-theme no
 * elemento raiz conforme o isDay do local consultado e cancela buscas anteriores.
 */
@Component({
  selector: 'app-weather-panel',
  imports: [CitySearchForm, CityList, WeatherCard, WeatherCardSkeleton, ErrorMessage],
  templateUrl: './weather-panel.html',
  styleUrl: './weather-panel.css',
  host: {
    '[attr.data-theme]': 'theme()',
  },
})
export class WeatherPanel {
  private readonly weatherApi = inject(WeatherApi);
  private readonly searchForm = viewChild(CitySearchForm);

  protected readonly state = signal<PanelState>('idle');
  protected readonly cities = signal<City[]>([]);
  protected readonly weather = signal<Weather | null>(null);
  protected readonly placeLabel = signal('');
  protected readonly error = signal<PanelError | null>(null);

  protected readonly busy = computed(() => this.state() === 'searching' || this.state() === 'loading');

  protected readonly theme = computed(() => {
    if (this.state() === 'error') {
      return 'day';
    }
    const current = this.weather();
    return current === null || current.isDay ? 'day' : 'night';
  });

  private request?: Subscription;
  private lastAction?: () => void;

  constructor() {
    // Geolocalização automática e qualquer chamada HTTP só acontecem no navegador.
    afterNextRender(() => this.requestGeolocation(false));
  }

  protected onSearch(rawName: string): void {
    const name = rawName.trim();
    if (name.length < 2) {
      this.fail({ message: 'Digite ao menos 2 caracteres.', retryable: false });
      this.focusSearchInput();
      return;
    }
    this.lastAction = () => this.searchByName(name);
    this.searchByName(name);
  }

  protected onSelect(city: City): void {
    const label = cityLabel(city);
    this.lastAction = () => this.loadWeather(city.latitude, city.longitude, label);
    this.loadWeather(city.latitude, city.longitude, label);
  }

  protected onLocate(): void {
    this.lastAction = () => this.requestGeolocation(true);
    this.requestGeolocation(true);
  }

  protected onRetry(): void {
    this.lastAction?.();
  }

  private searchByName(name: string): void {
    this.state.set('searching');
    this.error.set(null);
    this.runRequest(this.weatherApi.searchCities(name), (results) => {
      if (results.length === 1) {
        const [city] = results;
        this.loadWeather(city.latitude, city.longitude, cityLabel(city));
        return;
      }
      this.cities.set(results);
      this.state.set('choosing');
    });
  }

  private loadWeather(latitude: number, longitude: number, label: string): void {
    this.state.set('loading');
    this.error.set(null);
    this.runRequest(this.weatherApi.getWeather(latitude, longitude), (weather) => {
      this.placeLabel.set(label);
      this.weather.set(weather);
      this.state.set('success');
    });
  }

  /** `fromUser=false`: negada, indisponível ou expirada apenas volta ao idle, sem erro. */
  private requestGeolocation(fromUser: boolean): void {
    if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
      if (fromUser) {
        this.fail({ message: 'Não foi possível obter sua localização.', retryable: true });
      }
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (!fromUser && this.state() !== 'idle') {
          return;
        }
        const { latitude, longitude } = position.coords;
        this.lastAction = () => this.loadWeather(latitude, longitude, 'Sua localização');
        this.loadWeather(latitude, longitude, 'Sua localização');
      },
      (geolocationError) => {
        if (!fromUser) {
          return;
        }
        if (geolocationError.code === GeolocationPositionError.PERMISSION_DENIED) {
          this.fail({ message: 'Permissão de localização negada. Digite uma cidade.', retryable: false });
          this.focusSearchInput();
          return;
        }
        this.fail({ message: 'Não foi possível obter sua localização.', retryable: true });
      },
      GEOLOCATION_OPTIONS,
    );
  }

  /** Cancela a requisição anterior: respostas atrasadas nunca sobrescrevem o estado. */
  private runRequest<T>(source: Observable<T>, onSuccess: (value: T) => void): void {
    this.request?.unsubscribe();
    this.request = source.subscribe({
      next: onSuccess,
      error: (cause: unknown) => {
        this.fail(describeError(cause));
        if (cause instanceof HttpErrorResponse && cause.status === 404) {
          this.focusSearchInput();
        }
      },
    });
  }

  private fail(panelError: PanelError): void {
    this.error.set(panelError);
    this.state.set('error');
  }

  private focusSearchInput(): void {
    this.searchForm()?.focusInput();
  }
}

function cityLabel(city: City): string {
  const parts = [city.name, city.admin1, city.country].filter((part) => part !== null && part !== '');
  return parts.join(', ');
}

function describeError(cause: unknown): PanelError {
  if (cause instanceof HttpErrorResponse) {
    switch (cause.status) {
      case 400:
        return { message: 'Requisição inválida.', retryable: false };
      case 404:
        return { message: 'Cidade não encontrada. Verifique o nome e tente novamente.', retryable: false };
      case 502:
        return { message: 'O serviço de clima está indisponível no momento.', retryable: true };
      case 504:
        return { message: 'O serviço de clima demorou para responder.', retryable: true };
      default:
        break;
    }
  }
  return { message: 'Não foi possível conectar ao servidor.', retryable: true };
}
