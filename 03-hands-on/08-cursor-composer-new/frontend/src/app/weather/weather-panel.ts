import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, switchMap, catchError, EMPTY } from 'rxjs';
import { City, Weather, WeatherApi } from './weather-api';
import { CitySearchForm } from './city-search-form';
import { CityList } from './city-list';
import { WeatherCard } from './weather-card';
import { WeatherCardSkeleton } from './weather-card-skeleton';
import { ErrorMessage } from './error-message';

export type PanelState = 'idle' | 'searching' | 'choosing' | 'loading' | 'success' | 'error';

type LastAction =
  | { type: 'search'; name: string }
  | { type: 'weather'; latitude: number; longitude: number; city: City | null; useMyLocation: boolean }
  | { type: 'geolocation'; fromButton: boolean };

@Component({
  selector: 'app-weather-panel',
  imports: [
    CitySearchForm,
    CityList,
    WeatherCard,
    WeatherCardSkeleton,
    ErrorMessage,
  ],
  templateUrl: './weather-panel.html',
  styleUrl: './weather-panel.css',
})
export class WeatherPanel {
  private readonly api = inject(WeatherApi);
  private readonly destroyRef = inject(DestroyRef);

  private readonly searchForm = viewChild(CitySearchForm);

  protected readonly state = signal<PanelState>('idle');
  protected readonly cities = signal<City[]>([]);
  protected readonly weather = signal<Weather | null>(null);
  protected readonly selectedCity = signal<City | null>(null);
  protected readonly useMyLocationLabel = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly showRetry = signal(false);

  protected readonly geolocationAvailable = signal(false);

  private lastAction: LastAction | null = null;

  private readonly searchTrigger$ = new Subject<string>();
  private readonly weatherTrigger$ = new Subject<{
    latitude: number;
    longitude: number;
    city: City | null;
    useMyLocation: boolean;
  }>();

  protected readonly theme = computed(() => {
    const w = this.weather();
    if (w && this.state() === 'success') {
      return w.isDay ? 'day' : 'night';
    }
    return 'day';
  });

  protected readonly isBusy = computed(
    () => this.state() === 'searching' || this.state() === 'loading',
  );

  constructor() {
    afterNextRender(() => {
      this.geolocationAvailable.set(typeof navigator !== 'undefined' && !!navigator.geolocation);
      this.tryAutoGeolocation();
    });

    this.searchTrigger$
      .pipe(
        switchMap((name) => {
          this.state.set('searching');
          this.errorMessage.set('');
          this.showRetry.set(false);
          return this.api.searchCities(name).pipe(
            catchError((err) => {
              this.handleHttpError(err, { type: 'search', name });
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((results) => this.handleCityResults(results));

    this.weatherTrigger$
      .pipe(
        switchMap(({ latitude, longitude, city, useMyLocation }) => {
          this.state.set('loading');
          this.errorMessage.set('');
          this.showRetry.set(false);
          return this.api.getWeather(latitude, longitude).pipe(
            catchError((err) => {
              this.handleHttpError(err, {
                type: 'weather',
                latitude,
                longitude,
                city,
                useMyLocation,
              });
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => {
        this.weather.set(data);
        this.useMyLocationLabel.set(this.lastAction?.type === 'weather' && this.lastAction.useMyLocation);
        this.state.set('success');
      });
  }

  protected onSearch(name: string): void {
    const trimmed = name.trim();
    if (trimmed.length < 2) {
      this.state.set('error');
      this.errorMessage.set('Digite ao menos 2 caracteres.');
      this.showRetry.set(false);
      this.lastAction = null;
      this.focusInput();
      return;
    }
    this.lastAction = { type: 'search', name: trimmed };
    this.selectedCity.set(null);
    this.useMyLocationLabel.set(false);
    this.searchTrigger$.next(trimmed);
  }

  protected onSelectCity(city: City): void {
    this.selectedCity.set(city);
    this.loadWeather(city.latitude, city.longitude, city, false);
  }

  protected onUseLocation(): void {
    this.requestGeolocation(true);
  }

  protected onRetry(): void {
    const action = this.lastAction;
    if (!action) {
      return;
    }
    if (action.type === 'search') {
      this.onSearch(action.name);
    } else if (action.type === 'weather') {
      this.loadWeather(action.latitude, action.longitude, action.city, action.useMyLocation);
    } else if (action.type === 'geolocation') {
      this.requestGeolocation(action.fromButton);
    }
  }

  private handleCityResults(results: City[]): void {
    if (results.length === 1) {
      this.onSelectCity(results[0]);
      return;
    }
    this.cities.set(results);
    this.state.set('choosing');
  }

  private loadWeather(
    latitude: number,
    longitude: number,
    city: City | null,
    useMyLocation: boolean,
  ): void {
    this.lastAction = { type: 'weather', latitude, longitude, city, useMyLocation };
    if (city) {
      this.selectedCity.set(city);
    }
    this.useMyLocationLabel.set(useMyLocation);
    this.weatherTrigger$.next({ latitude, longitude, city, useMyLocation });
  }

  private tryAutoGeolocation(): void {
    this.requestGeolocation(false);
  }

  private requestGeolocation(fromButton: boolean): void {
    if (!navigator.geolocation) {
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.selectedCity.set(null);
        this.loadWeather(position.coords.latitude, position.coords.longitude, null, true);
      },
      (error) => {
        if (fromButton) {
          this.state.set('error');
          this.showRetry.set(error.code === error.POSITION_UNAVAILABLE || error.code === error.TIMEOUT);
          if (error.code === error.PERMISSION_DENIED) {
            this.errorMessage.set('Permissão de localização negada. Digite uma cidade.');
            this.showRetry.set(false);
            this.focusInput();
          } else {
            this.errorMessage.set('Não foi possível obter sua localização.');
          }
          this.lastAction = { type: 'geolocation', fromButton: true };
        }
      },
      { timeout: 10000, maximumAge: 600000 },
    );
  }

  private handleHttpError(error: unknown, action: LastAction): void {
    this.lastAction = action;
    this.state.set('error');
    this.showRetry.set(false);

    if (error instanceof HttpErrorResponse) {
      const code = error.error?.code as string | undefined;
      if (error.status === 404 && code === 'CITY_NOT_FOUND') {
        this.errorMessage.set('Cidade não encontrada. Verifique o nome e tente novamente.');
        this.focusInput();
        return;
      }
      if (error.status === 400) {
        this.errorMessage.set('Requisição inválida.');
        return;
      }
      if (error.status === 502 && code === 'UPSTREAM_ERROR') {
        this.errorMessage.set('O serviço de clima está indisponível no momento.');
        this.showRetry.set(true);
        return;
      }
      if (error.status === 504 && code === 'UPSTREAM_TIMEOUT') {
        this.errorMessage.set('O serviço de clima demorou para responder.');
        this.showRetry.set(true);
        return;
      }
    }

    this.errorMessage.set('Não foi possível conectar ao servidor.');
    this.showRetry.set(true);
  }

  private focusInput(): void {
    queueMicrotask(() => this.searchForm()?.focusInput());
  }
}
