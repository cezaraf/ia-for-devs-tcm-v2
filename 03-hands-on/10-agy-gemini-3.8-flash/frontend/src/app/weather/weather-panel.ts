import {
  Component,
  PLATFORM_ID,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { CityResponse, WeatherApi, WeatherResponse } from './weather-api';
import { CitySearchForm } from './city-search-form';
import { CityList } from './city-list';
import { WeatherCard } from './weather-card';
import { WeatherCardSkeleton } from './weather-card-skeleton';
import { ErrorMessage } from './error-message';

export type WeatherPanelState = 'idle' | 'searching' | 'choosing' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-weather-panel',
  imports: [
    CitySearchForm,
    CityList,
    WeatherCard,
    WeatherCardSkeleton,
    ErrorMessage
  ],
  templateUrl: './weather-panel.html',
  styleUrl: './weather-panel.css'
})
export class WeatherPanel {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly weatherApi = inject(WeatherApi);

  readonly state = signal<WeatherPanelState>('idle');
  readonly cityResults = signal<CityResponse[]>([]);
  readonly currentWeather = signal<WeatherResponse | null>(null);
  readonly currentLocationLabel = signal<string>('');
  readonly errorMessage = signal<string>('');
  readonly canRetry = signal<boolean>(false);

  readonly searchForm = viewChild(CitySearchForm);

  readonly isBusy = computed(() => this.state() === 'searching' || this.state() === 'loading');

  readonly theme = computed<'day' | 'night'>(() => {
    if (this.state() === 'success' && this.currentWeather() && !this.currentWeather()!.isDay) {
      return 'night';
    }
    return 'day';
  });

  private currentRequestId = 0;
  private lastAction: (() => void) | null = null;

  constructor() {
    afterNextRender(() => {
      this.initAutoGeolocation();
    });
  }

  private initAutoGeolocation(): void {
    if (!isPlatformBrowser(this.platformId) || typeof navigator === 'undefined' || !navigator.geolocation) {
      return;
    }

    const reqId = ++this.currentRequestId;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (reqId !== this.currentRequestId) return;
        this.fetchWeatherForCoords(pos.coords.latitude, pos.coords.longitude, 'Sua localização', reqId);
      },
      () => {
        // "Se negada, indisponível ou expirada, ficar em idle sem mostrar erro."
      },
      { timeout: 10000, maximumAge: 600000 }
    );
  }

  onSearch(query: string): void {
    const trimmed = (query || '').trim();
    if (trimmed.length < 2) {
      this.setError('Digite ao menos 2 caracteres.', false);
      this.focusSearchInput();
      return;
    }

    const reqId = ++this.currentRequestId;
    this.lastAction = () => this.executeSearch(trimmed, reqId);
    this.executeSearch(trimmed, reqId);
  }

  private executeSearch(name: string, reqId: number): void {
    this.state.set('searching');
    this.weatherApi.getCities(name).subscribe({
      next: (cities) => {
        if (reqId !== this.currentRequestId) return;

        if (!cities || cities.length === 0) {
          this.setError('Cidade não encontrada. Verifique o nome e tente novamente.', false);
          this.focusSearchInput();
        } else if (cities.length === 1) {
          const city = cities[0];
          const label = this.formatCityLabel(city);
          this.executeWeather(city.latitude, city.longitude, label, reqId);
        } else {
          this.cityResults.set(cities.slice(0, 10));
          this.state.set('choosing');
        }
      },
      error: (err: HttpErrorResponse) => {
        if (reqId !== this.currentRequestId) return;
        this.handleHttpError(err);
      }
    });
  }

  onSelectCity(city: CityResponse): void {
    const reqId = ++this.currentRequestId;
    const label = this.formatCityLabel(city);
    this.lastAction = () => this.executeWeather(city.latitude, city.longitude, label, reqId);
    this.executeWeather(city.latitude, city.longitude, label, reqId);
  }

  onGeolocationClick(): void {
    if (!isPlatformBrowser(this.platformId) || typeof navigator === 'undefined' || !navigator.geolocation) {
      return;
    }

    const reqId = ++this.currentRequestId;
    this.lastAction = () => this.executeGeolocation(reqId);
    this.executeGeolocation(reqId);
  }

  private executeGeolocation(reqId: number): void {
    this.state.set('loading');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (reqId !== this.currentRequestId) return;
        this.fetchWeatherForCoords(pos.coords.latitude, pos.coords.longitude, 'Sua localização', reqId);
      },
      (err) => {
        if (reqId !== this.currentRequestId) return;
        if (err.code === 1 /* PERMISSION_DENIED */) {
          this.setError('Permissão de localização negada. Digite uma cidade.', false);
          this.focusSearchInput();
        } else {
          this.setError('Não foi possível obter sua localização.', true);
        }
      },
      { timeout: 10000, maximumAge: 600000 }
    );
  }

  private fetchWeatherForCoords(lat: number, lon: number, label: string, reqId: number): void {
    this.executeWeather(lat, lon, label, reqId);
  }

  private executeWeather(lat: number, lon: number, label: string, reqId: number): void {
    this.state.set('loading');
    this.weatherApi.getWeather(lat, lon).subscribe({
      next: (weather) => {
        if (reqId !== this.currentRequestId) return;
        this.currentWeather.set(weather);
        this.currentLocationLabel.set(label);
        this.state.set('success');
      },
      error: (err: HttpErrorResponse) => {
        if (reqId !== this.currentRequestId) return;
        this.handleHttpError(err);
      }
    });
  }

  onRetry(): void {
    if (this.lastAction) {
      this.lastAction();
    }
  }

  private handleHttpError(err: HttpErrorResponse): void {
    if (err.status === 404) {
      this.setError('Cidade não encontrada. Verifique o nome e tente novamente.', false);
      this.focusSearchInput();
    } else if (err.status === 400) {
      this.setError('Requisição inválida.', false);
    } else if (err.status === 502) {
      this.setError('O serviço de clima está indisponível no momento.', true);
    } else if (err.status === 504) {
      this.setError('O serviço de clima demorou para responder.', true);
    } else {
      this.setError('Não foi possível conectar ao servidor.', true);
    }
  }

  private setError(msg: string, retryable: boolean): void {
    this.errorMessage.set(msg);
    this.canRetry.set(retryable);
    this.state.set('error');
  }

  private focusSearchInput(): void {
    setTimeout(() => {
      this.searchForm()?.focusInput();
    }, 0);
  }

  private formatCityLabel(city: CityResponse): string {
    const parts = [city.name, city.admin1, city.country].filter(Boolean);
    return parts.join(', ');
  }
}
