import {
  Component,
  DestroyRef,
  PLATFORM_ID,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, Subject, TimeoutError, catchError, defer, map, of, switchMap, tap, timeout } from 'rxjs';
import { BrowserLocation } from './browser-location';
import { CityOptions } from './city-options';
import { CitySearchForm } from './city-search-form';
import { StatusMessage } from './status-message';
import { City, Weather, WeatherApi } from './weather-api';
import { WeatherCard } from './weather-card';
import { WeatherSkeleton } from './weather-skeleton';
import { normalizeCityName, isValidCityName } from './city-name';
import { Failure, LOCATION_LABEL, MESSAGES, toFailure } from './weather-messages';

type PanelState =
  | 'idle'
  | 'loading'
  | 'success'
  | 'choosing'
  | 'invalid'
  | 'not-found'
  | 'unavailable'
  | 'timeout'
  | 'geo-denied'
  | 'geo-unavailable';
type Theme = 'day' | 'night';
type Operation =
  | { kind: 'search'; name: string }
  | { kind: 'select'; city: City }
  | { kind: 'locate' }
  | { kind: 'cancel' };

type Outcome =
  | { type: 'weather'; weather: Weather; label: string }
  | { type: 'cities'; cities: City[] }
  | { type: 'failure'; failure: Failure; op: Operation };

const TIMEOUT_MS = 15_000;

function cityLabel(city: City): string {
  return [city.name, city.region, city.country].filter((part) => part).join(', ');
}

function round(value: number): number {
  return Math.round(value * 100) / 100;
}

@Component({
  selector: 'app-weather-panel',
  imports: [CitySearchForm, CityOptions, WeatherCard, WeatherSkeleton, StatusMessage],
  templateUrl: './weather-panel.html',
  styleUrl: './weather-panel.css',
})
export class WeatherPanel {
  private readonly api = inject(WeatherApi);
  private readonly location = inject(BrowserLocation);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly form = viewChild(CitySearchForm);
  private readonly operations = new Subject<Operation>();

  protected readonly state = signal<PanelState>('idle');
  protected readonly weather = signal<Weather | null>(null);
  protected readonly locationLabel = signal<string | null>(null);
  protected readonly cities = signal<City[]>([]);
  protected readonly theme = signal<Theme>('day');
  protected readonly geolocationAvailable = signal(false);
  protected readonly lastFailed = signal<Operation | null>(null);

  protected readonly idleMessage = MESSAGES.idle;
  protected readonly busy = computed(() => this.state() === 'loading');
  protected readonly failure = computed(() => {
    const state = this.state();
    if (state === 'idle' || !(state in MESSAGES)) {
      return null;
    }
    const message = MESSAGES[state as keyof typeof MESSAGES];
    return {
      kind: message.kind,
      text: message.text,
      retryable: message.retryable && this.lastFailed() !== null,
    };
  });
  protected readonly invalidMessage = computed(() =>
    this.state() === 'invalid' ? MESSAGES.invalid.text : null,
  );

  constructor() {
    this.operations
      .pipe(
        switchMap((op) => this.execute(op)),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((outcome) => this.apply(outcome));

    afterNextRender(() => this.geolocationAvailable.set(this.location.isAvailable()));
  }

  protected onSearch(raw: string): void {
    if (!isValidCityName(raw)) {
      this.operations.next({ kind: 'cancel' });
      this.state.set('invalid');
      this.lastFailed.set(null);
      this.focusInput();
      return;
    }
    this.start({ kind: 'search', name: normalizeCityName(raw) });
  }

  protected onSelect(city: City): void {
    this.start({ kind: 'select', city });
  }

  protected onUseLocation(): void {
    this.start({ kind: 'locate' });
  }

  protected onRetry(): void {
    const op = this.lastFailed();
    if (op) {
      this.start(op);
    }
  }

  private start(op: Operation): void {
    this.lastFailed.set(null);
    this.weather.set(null);
    this.cities.set([]);
    this.locationLabel.set(null);
    this.state.set('loading');
    this.operations.next(op);
  }

  private execute(op: Operation): Observable<Outcome> {
    return defer(() => {
      let positionReceived = false;
      return this.run(op, () => (positionReceived = true)).pipe(
        timeout(TIMEOUT_MS),
        catchError((error: unknown) => {
          const beforePosition =
            op.kind === 'locate' && !positionReceived && error instanceof TimeoutError;
          const failure = beforePosition ? toFailure({ code: 3 }) : toFailure(error);
          return of<Outcome>({ type: 'failure', failure, op });
        }),
      );
    });
  }

  private run(op: Operation, onPosition: () => void): Observable<Outcome> {
    switch (op.kind) {
      case 'cancel':
        return EMPTY;
      case 'select':
        return this.fetchWeather(op.city.latitude, op.city.longitude, cityLabel(op.city));
      case 'locate':
        return this.location.getPosition().pipe(
          tap(onPosition),
          switchMap((position) =>
            this.fetchWeather(round(position.latitude), round(position.longitude), LOCATION_LABEL),
          ),
        );
      case 'search':
        return this.api.searchCities(op.name).pipe(
          switchMap((found): Observable<Outcome> => {
            if (found.length === 0) {
              const failure: Failure = { state: 'not-found', ...MESSAGES['not-found'] };
              return of({ type: 'failure', failure, op });
            }
            if (found.length === 1) {
              const [city] = found;
              return this.fetchWeather(city.latitude, city.longitude, cityLabel(city));
            }
            return of({ type: 'cities', cities: found });
          }),
        );
    }
  }

  private fetchWeather(latitude: number, longitude: number, label: string): Observable<Outcome> {
    return this.api
      .getWeather(latitude, longitude)
      .pipe(map((weather): Outcome => ({ type: 'weather', weather, label })));
  }

  private apply(outcome: Outcome): void {
    switch (outcome.type) {
      case 'weather':
        this.weather.set(outcome.weather);
        this.locationLabel.set(outcome.label);
        this.theme.set(outcome.weather.isDay === false ? 'night' : 'day');
        this.state.set('success');
        return;
      case 'cities':
        this.cities.set(outcome.cities);
        this.state.set('choosing');
        return;
      case 'failure': {
        const { state, retryable } = outcome.failure;
        this.lastFailed.set(retryable ? outcome.op : null);
        this.state.set(state);
        if (state === 'invalid') {
          this.focusInput();
        }
      }
    }
  }

  private focusInput(): void {
    if (this.isBrowser) {
      this.form()?.focusInput();
    }
  }
}
