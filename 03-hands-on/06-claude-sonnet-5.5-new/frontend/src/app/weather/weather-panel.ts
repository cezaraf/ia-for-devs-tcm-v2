import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  afterNextRender,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, Subject, catchError, switchMap, tap } from 'rxjs';

import { CityList } from './city-list';
import { CitySearchForm } from './city-search-form';
import { ErrorMessage } from './error-message';
import { GeolocationFailure, currentPosition } from './geolocation';
import { WeatherApi } from './weather-api';
import { WeatherCard } from './weather-card';
import { WeatherCardSkeleton } from './weather-card-skeleton';
import {
  CITY_NOT_FOUND,
  City,
  GEOLOCATION_DENIED,
  GEOLOCATION_LABEL,
  GEOLOCATION_UNAVAILABLE,
  MIN_NAME_LENGTH,
  NAME_TOO_SHORT,
  PanelError,
  Weather,
  cityLabel,
  toPanelError,
} from './weather.models';

type PanelState = 'idle' | 'searching' | 'choosing' | 'loading' | 'success' | 'error';

type Action =
  | { kind: 'search'; name: string }
  | { kind: 'weather'; latitude: number; longitude: number; label: string }
  | { kind: 'geolocate'; manual: boolean };

@Component({
  selector: 'app-weather-panel',
  imports: [CitySearchForm, CityList, WeatherCard, WeatherCardSkeleton, ErrorMessage],
  templateUrl: './weather-panel.html',
  styleUrl: './weather-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeatherPanel {
  private readonly api = inject(WeatherApi);
  private readonly destroyRef = inject(DestroyRef);

  /** Toda ação (busca, escolha, geolocalização, retry) passa por aqui: a mais nova cancela a anterior. */
  private readonly actions = new Subject<Action>();
  private readonly retryAction = signal<Action | null>(null);

  protected readonly state = signal<PanelState>('idle');
  protected readonly cities = signal<City[]>([]);
  protected readonly weather = signal<Weather | null>(null);
  protected readonly label = signal('');
  protected readonly error = signal<PanelError | null>(null);
  protected readonly focusRequest = signal(0);

  protected readonly busy = computed(() => {
    const state = this.state();
    return state === 'searching' || state === 'loading';
  });

  /** Dia, exceto quando o último resultado foi noite; erro volta ao dia. */
  protected readonly theme = computed(() =>
    this.state() !== 'error' && this.weather()?.isDay === false ? 'night' : 'day',
  );

  constructor() {
    this.actions
      .pipe(
        switchMap((action) =>
          this.run(action).pipe(
            catchError((error: unknown) => {
              this.handleError(error, action);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    afterNextRender(() => {
      if ('geolocation' in navigator) {
        this.actions.next({ kind: 'geolocate', manual: false });
      }
    });
  }

  protected onSearch(name: string): void {
    this.actions.next({ kind: 'search', name });
  }

  protected onLocate(): void {
    this.actions.next({ kind: 'geolocate', manual: true });
  }

  protected onCitySelected(city: City): void {
    this.actions.next({
      kind: 'weather',
      latitude: city.latitude,
      longitude: city.longitude,
      label: cityLabel(city),
    });
  }

  protected onRetry(): void {
    const action = this.retryAction();
    if (action) {
      this.actions.next(action);
    }
  }

  private run(action: Action): Observable<unknown> {
    switch (action.kind) {
      case 'search':
        return this.search(action);
      case 'weather':
        return this.loadWeather(action);
      case 'geolocate':
        return this.geolocate(action);
    }
  }

  private search(action: Extract<Action, { kind: 'search' }>): Observable<unknown> {
    const name = action.name.trim();
    if (name.length < MIN_NAME_LENGTH) {
      this.fail(NAME_TOO_SHORT);
      return EMPTY;
    }

    this.retryAction.set(action);
    this.start('searching');
    return this.api.searchCities(name).pipe(
      switchMap((cities) => {
        if (cities.length === 0) {
          this.fail(CITY_NOT_FOUND);
          return EMPTY;
        }
        if (cities.length === 1) {
          const [city] = cities;
          return this.loadWeather({
            kind: 'weather',
            latitude: city.latitude,
            longitude: city.longitude,
            label: cityLabel(city),
          });
        }
        this.cities.set(cities);
        this.state.set('choosing');
        return EMPTY;
      }),
    );
  }

  private loadWeather(action: Extract<Action, { kind: 'weather' }>): Observable<unknown> {
    this.retryAction.set(action);
    this.start('loading');
    return this.api.getWeather(action.latitude, action.longitude).pipe(
      tap((weather) => {
        this.weather.set(weather);
        this.label.set(action.label);
        this.state.set('success');
      }),
    );
  }

  private geolocate(action: Extract<Action, { kind: 'geolocate' }>): Observable<unknown> {
    if (action.manual) {
      this.retryAction.set(action);
      this.start('loading');
    }
    return currentPosition().pipe(
      switchMap(({ latitude, longitude }) =>
        this.loadWeather({ kind: 'weather', latitude, longitude, label: GEOLOCATION_LABEL }),
      ),
    );
  }

  private handleError(error: unknown, action: Action): void {
    if (error instanceof GeolocationFailure) {
      // Geolocalização automática que falha não é erro: o painel segue em idle.
      if (action.kind === 'geolocate' && action.manual) {
        this.fail(error.denied ? GEOLOCATION_DENIED : GEOLOCATION_UNAVAILABLE);
      }
      return;
    }
    this.fail(toPanelError(error));
  }

  private start(state: 'searching' | 'loading'): void {
    this.error.set(null);
    this.state.set(state);
  }

  private fail(error: PanelError): void {
    this.error.set(error);
    this.state.set('error');
    if (error.focusInput) {
      this.focusRequest.update((count) => count + 1);
    }
  }
}
