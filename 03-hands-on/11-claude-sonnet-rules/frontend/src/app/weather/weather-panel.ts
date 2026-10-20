import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, Subject, catchError, defer, switchMap, tap } from 'rxjs';
import { CityList } from './city-list';
import { CitySearchForm } from './city-search-form';
import { ErrorMessage } from './error-message';
import { City, Weather, WeatherApi } from './weather-api';
import { WeatherCard } from './weather-card';
import { WeatherCardSkeleton } from './weather-card-skeleton';

type State = 'idle' | 'searching' | 'choosing' | 'loading' | 'success' | 'error';

interface PanelError {
  message: string;
  retryable: boolean;
}

const MY_LOCATION = 'Sua localização';
const PERMISSION_DENIED = 1;

@Component({
  selector: 'app-weather-panel',
  imports: [CitySearchForm, CityList, WeatherCard, WeatherCardSkeleton, ErrorMessage],
  templateUrl: './weather-panel.html',
  styleUrl: './weather-panel.css',
})
export class WeatherPanel {
  private readonly api = inject(WeatherApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly form = viewChild.required(CitySearchForm);

  protected readonly state = signal<State>('idle');
  protected readonly cities = signal<City[]>([]);
  protected readonly weather = signal<Weather | null>(null);
  protected readonly locationLabel = signal('');
  protected readonly error = signal<PanelError | null>(null);
  protected readonly geolocationAvailable = signal(false);

  protected readonly busy = computed(
    () => this.state() === 'searching' || this.state() === 'loading',
  );
  // Sem resultado anterior (primeira consulta, erro) o tema é "day".
  protected readonly theme = computed(() => (this.weather()?.isDay === false ? 'night' : 'day'));

  // Toda ação entra neste Subject: uma nova ação cancela a anterior (inclusive as locais).
  private readonly actions = new Subject<() => Observable<unknown>>();
  private lastAction: (() => void) | null = null;

  constructor() {
    this.actions
      .pipe(
        switchMap((run) => run()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    afterNextRender(() => {
      const available = typeof navigator !== 'undefined' && !!navigator.geolocation;
      this.geolocationAvailable.set(available);
      if (available) {
        this.locate(true);
      }
    });
  }

  protected onSearch(raw: string): void {
    const name = raw.trim();
    if (name.length < 2) {
      this.lastAction = null;
      this.actions.next(() =>
        defer(() => {
          this.fail({ message: 'Digite ao menos 2 caracteres.', retryable: false }, true);
          return EMPTY;
        }),
      );
      return;
    }
    this.lastAction = () => this.onSearch(name);
    this.actions.next(() =>
      defer(() => {
        this.state.set('searching');
        return this.api.cities(name).pipe(
          switchMap((found) => {
            if (found.length === 1) {
              return this.weatherFlow(
                found[0].latitude,
                found[0].longitude,
                this.cityLabel(found[0]),
              );
            }
            this.cities.set(found);
            this.state.set('choosing');
            return EMPTY;
          }),
          catchError((e) => this.handleError(e)),
        );
      }),
    );
  }

  protected onSelect(city: City): void {
    this.loadWeather(city.latitude, city.longitude, this.cityLabel(city));
  }

  protected locate(automatic = false): void {
    this.lastAction = automatic ? null : () => this.locate();
    this.actions.next(() =>
      defer(() => {
        if (!automatic) {
          this.state.set('loading');
        }
        return this.currentPosition().pipe(
          switchMap((pos) =>
            this.weatherFlow(pos.coords.latitude, pos.coords.longitude, MY_LOCATION),
          ),
          catchError((e) => this.handleError(e, automatic)),
        );
      }),
    );
  }

  protected retry(): void {
    this.lastAction?.();
  }

  private loadWeather(latitude: number, longitude: number, label: string): void {
    this.actions.next(() =>
      this.weatherFlow(latitude, longitude, label).pipe(catchError((e) => this.handleError(e))),
    );
  }

  private weatherFlow(latitude: number, longitude: number, label: string): Observable<unknown> {
    this.lastAction = () => this.loadWeather(latitude, longitude, label);
    return defer(() => {
      this.state.set('loading');
      return this.api.weather(latitude, longitude).pipe(
        tap((weather) => {
          this.weather.set(weather);
          this.locationLabel.set(label);
          this.state.set('success');
        }),
      );
    });
  }

  private currentPosition(): Observable<GeolocationPosition> {
    return new Observable<GeolocationPosition>((subscriber) => {
      let cancelled = false;
      navigator.geolocation.getCurrentPosition(
        (position) => {
          if (!cancelled) {
            subscriber.next(position);
            subscriber.complete();
          }
        },
        (err) => {
          if (!cancelled) {
            subscriber.error(err);
          }
        },
        { timeout: 10000, maximumAge: 600000 },
      );
      return () => {
        cancelled = true;
      };
    });
  }

  private cityLabel(city: City): string {
    return [city.name, city.admin1, city.country].filter(Boolean).join(', ');
  }

  private handleError(e: unknown, silentGeolocation = false): Observable<never> {
    if (e instanceof HttpErrorResponse) {
      this.fail(this.httpError(e), e.status === 404 && e.error?.code === 'CITY_NOT_FOUND');
    } else if (silentGeolocation) {
      this.state.set('idle');
    } else if ((e as { code?: number } | null)?.code === PERMISSION_DENIED) {
      this.fail(
        { message: 'Permissão de localização negada. Digite uma cidade.', retryable: false },
        true,
      );
    } else {
      this.fail({ message: 'Não foi possível obter sua localização.', retryable: true }, false);
    }
    return EMPTY;
  }

  private httpError(e: HttpErrorResponse): PanelError {
    if (e.status === 404 && e.error?.code === 'CITY_NOT_FOUND') {
      return {
        message: 'Cidade não encontrada. Verifique o nome e tente novamente.',
        retryable: false,
      };
    }
    if (e.status === 400) {
      return { message: 'Requisição inválida.', retryable: false };
    }
    if (e.status === 502) {
      return { message: 'O serviço de clima está indisponível no momento.', retryable: true };
    }
    if (e.status === 504) {
      return { message: 'O serviço de clima demorou para responder.', retryable: true };
    }
    return { message: 'Não foi possível conectar ao servidor.', retryable: true };
  }

  private fail(error: PanelError, focusInput: boolean): void {
    this.weather.set(null);
    this.error.set(error);
    this.state.set('error');
    if (focusInput) {
      this.form().focusInput();
    }
  }
}
