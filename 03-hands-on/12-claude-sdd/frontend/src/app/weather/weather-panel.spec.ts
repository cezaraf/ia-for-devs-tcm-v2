import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, of, throwError } from 'rxjs';
import { BrowserLocation, Position } from './browser-location';
import { City, Weather, WeatherApi } from './weather-api';
import { WeatherPanel } from './weather-panel';

function city(id: number, name = 'Recife', extra: Partial<City> = {}): City {
  return {
    id,
    name,
    region: 'Pernambuco',
    subregion: null,
    country: 'Brasil',
    countryCode: 'BR',
    latitude: -8.05,
    longitude: -34.9,
    timezone: 'America/Recife',
    ...extra,
  };
}

function weather(isDay: boolean | null = true): Weather {
  return {
    latitude: -8.05,
    longitude: -34.9,
    timezone: 'America/Recife',
    observedAt: '2026-10-20T16:00',
    temperature: 28.6,
    apparentTemperature: 30,
    humidity: 62,
    windSpeed: 12.7,
    windDirectionDegrees: 90,
    windDirection: 'Leste',
    weatherCode: 1,
    condition: 'Predominantemente limpo',
    isDay,
  };
}

function httpError(status: number, code?: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: code ? { code, message: 'x' } : null });
}

describe('WeatherPanel', () => {
  let api: { searchCities: ReturnType<typeof vi.fn>; getWeather: ReturnType<typeof vi.fn> };
  let location: {
    isAvailable: ReturnType<typeof vi.fn>;
    getPosition: ReturnType<typeof vi.fn>;
  };
  let fixture: ComponentFixture<WeatherPanel>;
  let el: HTMLElement;

  beforeEach(() => {
    vi.useFakeTimers();
    api = { searchCities: vi.fn(), getWeather: vi.fn() };
    location = { isAvailable: vi.fn(() => true), getPosition: vi.fn() };
    TestBed.configureTestingModule({
      imports: [WeatherPanel],
      providers: [
        { provide: WeatherApi, useValue: api },
        { provide: BrowserLocation, useValue: location },
      ],
    });
    fixture = TestBed.createComponent(WeatherPanel);
    el = fixture.nativeElement;
    document.body.appendChild(el);
    fixture.detectChanges();
  });

  afterEach(() => {
    el.remove();
    vi.useRealTimers();
  });

  const panel = () => el.querySelector('main') as HTMLElement;
  const section = () => el.querySelector('section') as HTMLElement;
  const text = () => section().textContent ?? '';

  function submit(value: string): void {
    const input = el.querySelector('input') as HTMLInputElement;
    input.value = value;
    el.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  function click(label: string): void {
    const button = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes(label),
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
  }

  function tick(ms: number): void {
    vi.advanceTimersByTime(ms);
    fixture.detectChanges();
  }

  const retryButton = () =>
    Array.from(section().querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Tentar novamente'),
    );

  it('SCN-030 estado inicial idle, tema day, sem chamadas na carga', () => {
    expect(text()).toContain('Digite o nome de uma cidade');
    expect(panel().getAttribute('data-theme')).toBe('day');
    expect(el.querySelector('h1')?.textContent).toBe('Painel de clima');
    expect(api.searchCities).not.toHaveBeenCalled();
    expect(api.getWeather).not.toHaveBeenCalled();
    expect(location.getPosition).not.toHaveBeenCalled();
  });

  it('SCN-032 uma cidade encadeia getWeather e mostra o rótulo', () => {
    api.searchCities.mockReturnValue(of([city(1)]));
    api.getWeather.mockReturnValue(of(weather()));
    submit('  Recife ');
    expect(api.searchCities).toHaveBeenCalledWith('Recife');
    expect(api.getWeather).toHaveBeenCalledWith(-8.05, -34.9);
    expect(text()).toContain('Recife, Pernambuco, Brasil');
    expect(text()).toContain('28,6');
  });

  it('SCN-033 2..10 cidades levam a choosing e a escolha mostra o clima', () => {
    api.searchCities.mockReturnValue(of([city(1), city(2, 'Recife', { region: 'Paraíba' })]));
    api.getWeather.mockReturnValue(of(weather()));
    submit('Recife');
    expect(text()).toContain('Encontramos 2 cidades');
    expect(api.getWeather).not.toHaveBeenCalled();
    const second = section().querySelectorAll<HTMLButtonElement>('.option')[1];
    second.click();
    fixture.detectChanges();
    expect(text()).toContain('Recife, Paraíba, Brasil');
  });

  it('SCN-031 submit inválido: invalid, sem requisição e foco no input', () => {
    submit('a');
    expect(api.searchCities).not.toHaveBeenCalled();
    expect(el.querySelector('#city-name-error')?.textContent).toContain('2 a 100');
    expect(document.activeElement).toBe(el.querySelector('input'));
    expect(section().querySelector('app-status-message')).toBeNull();
  });

  it('SCN-031 submit inválido ignora resposta tardia de operação anterior', () => {
    const slow = new Subject<City[]>();
    api.searchCities.mockReturnValue(slow);
    submit('Recife');
    expect(panel().querySelector('[aria-busy="true"]')).toBeTruthy();
    submit('a');
    slow.next([city(1), city(2)]);
    fixture.detectChanges();
    expect(el.querySelector('#city-name-error')).toBeTruthy();
    expect(text()).not.toContain('Encontramos');
  });

  it('SCN-034 404 → not-found sem retry', () => {
    api.searchCities.mockReturnValue(throwError(() => httpError(404, 'CITY_NOT_FOUND')));
    submit('xyzqwkk');
    expect(text()).toContain('Nenhuma cidade encontrada');
    expect(retryButton()).toBeUndefined();
  });

  it('SCN-034 lista vazia → not-found', () => {
    api.searchCities.mockReturnValue(of([]));
    submit('xyzqwkk');
    expect(text()).toContain('Nenhuma cidade encontrada');
  });

  it.each([
    ['502', () => httpError(502, 'UPSTREAM_ERROR')],
    ['503', () => httpError(503)],
    ['status 0', () => httpError(0)],
  ])('SCN-035 %s → unavailable com retry', (_n, make) => {
    api.searchCities.mockReturnValue(throwError(make));
    submit('Recife');
    expect(text()).toContain('indisponível');
    expect(retryButton()).toBeTruthy();
  });

  it('SCN-036 504 → timeout com retry', () => {
    api.searchCities.mockReturnValue(throwError(() => httpError(504, 'UPSTREAM_TIMEOUT')));
    submit('Recife');
    expect(text()).toContain('demorou demais');
    expect(retryButton()).toBeTruthy();
  });

  it('SCN-037 INVALID_COORDINATES → unavailable sem retry', () => {
    api.searchCities.mockReturnValue(of([city(1)]));
    api.getWeather.mockReturnValue(throwError(() => httpError(400, 'INVALID_COORDINATES')));
    submit('Recife');
    expect(text()).toContain('indisponível');
    expect(retryButton()).toBeUndefined();
  });

  it('SCN-031 HTTP 400 INVALID_CITY_NAME → invalid', () => {
    api.searchCities.mockReturnValue(throwError(() => httpError(400, 'INVALID_CITY_NAME')));
    submit('Recife');
    expect(el.querySelector('#city-name-error')).toBeTruthy();
  });

  it('SCN-035 retry repete a busca', () => {
    api.searchCities.mockReturnValueOnce(throwError(() => httpError(502, 'UPSTREAM_ERROR')));
    submit('Recife');
    api.searchCities.mockReturnValue(of([city(1)]));
    api.getWeather.mockReturnValue(of(weather()));
    click('Tentar novamente');
    expect(api.searchCities).toHaveBeenCalledTimes(2);
    expect(api.searchCities).toHaveBeenLastCalledWith('Recife');
    expect(text()).toContain('Recife, Pernambuco, Brasil');
  });

  it('SCN-035 retry repete a escolha', () => {
    const chosen = city(2, 'Recife', { region: 'Paraíba', latitude: 1, longitude: 2 });
    api.searchCities.mockReturnValue(of([city(1), chosen]));
    api.getWeather.mockReturnValueOnce(throwError(() => httpError(502, 'UPSTREAM_ERROR')));
    submit('Recife');
    section().querySelectorAll<HTMLButtonElement>('.option')[1].click();
    fixture.detectChanges();
    expect(retryButton()).toBeTruthy();
    api.getWeather.mockReturnValue(of(weather()));
    click('Tentar novamente');
    expect(api.getWeather).toHaveBeenLastCalledWith(1, 2);
    expect(text()).toContain('Recife, Paraíba, Brasil');
  });

  it('SCN-035 retry repete a localização', () => {
    location.getPosition.mockReturnValue(throwError(() => ({ code: 2 })));
    click('Usar minha localização');
    expect(retryButton()).toBeTruthy();
    location.getPosition.mockReturnValue(of({ latitude: 1, longitude: 2 }));
    api.getWeather.mockReturnValue(of(weather()));
    click('Tentar novamente');
    expect(location.getPosition).toHaveBeenCalledTimes(2);
    expect(text()).toContain('Sua localização');
  });

  it('SCN-038 resposta antiga é ignorada', () => {
    const first = new Subject<City[]>();
    const second = new Subject<City[]>();
    api.searchCities.mockReturnValueOnce(first).mockReturnValueOnce(second);
    api.getWeather.mockReturnValue(of(weather()));
    submit('Recife');
    submit('Olinda');
    second.next([city(9, 'Olinda')]);
    first.next([city(1), city(2)]);
    fixture.detectChanges();
    expect(text()).toContain('Olinda, Pernambuco, Brasil');
    expect(text()).not.toContain('Encontramos');
  });

  it('SCN-036 teto de 15 s → timeout com retry', () => {
    api.searchCities.mockReturnValue(new Subject<City[]>());
    submit('Recife');
    expect(section().getAttribute('aria-busy')).toBe('true');
    tick(14_999);
    expect(section().getAttribute('aria-busy')).toBe('true');
    tick(1);
    expect(text()).toContain('demorou demais');
    expect(retryButton()).toBeTruthy();
    expect(section().getAttribute('aria-busy')).toBe('false');
  });

  it('SCN-041 locate com posição pendente aos 15 s → geo-unavailable com retry', () => {
    location.getPosition.mockReturnValue(new Subject<Position>());
    click('Usar minha localização');
    tick(15_000);
    expect(text()).toContain('Não foi possível obter sua localização');
    expect(retryButton()).toBeTruthy();
  });

  it('SCN-036 locate com posição obtida e clima pendente aos 15 s → timeout', () => {
    location.getPosition.mockReturnValue(of({ latitude: 1, longitude: 2 }));
    api.getWeather.mockReturnValue(new Subject<Weather>());
    click('Usar minha localização');
    tick(15_000);
    expect(text()).toContain('demorou demais');
  });

  it('SCN-039 localização só após clique, coordenadas arredondadas e rótulo', () => {
    location.getPosition.mockReturnValue(of({ latitude: -8.056789, longitude: -34.9012 }));
    api.getWeather.mockReturnValue(of(weather()));
    expect(location.getPosition).not.toHaveBeenCalled();
    click('Usar minha localização');
    expect(api.getWeather).toHaveBeenCalledWith(-8.06, -34.9);
    expect(text()).toContain('Sua localização');
  });

  it('SCN-040 code 1 → geo-denied sem retry', () => {
    location.getPosition.mockReturnValue(throwError(() => ({ code: 1 })));
    click('Usar minha localização');
    expect(text()).toContain('Permissão de localização negada');
    expect(retryButton()).toBeUndefined();
  });

  it.each([2, 3])('SCN-041 code %i → geo-unavailable com retry', (code) => {
    location.getPosition.mockReturnValue(throwError(() => ({ code })));
    click('Usar minha localização');
    expect(text()).toContain('Não foi possível obter sua localização');
    expect(retryButton()).toBeTruthy();
  });

  it('SCN-030 botão de localização só aparece com geolocalização disponível', () => {
    expect(
      Array.from(el.querySelectorAll('button')).some((b) =>
        b.textContent?.includes('Usar minha localização'),
      ),
    ).toBe(true);
  });

  it('SCN-042 data-theme segue isDay e permanece após erro e na lista', () => {
    api.searchCities.mockReturnValue(of([city(1)]));
    api.getWeather.mockReturnValue(of(weather(false)));
    submit('Recife');
    expect(panel().getAttribute('data-theme')).toBe('night');

    api.searchCities.mockReturnValue(throwError(() => httpError(502, 'UPSTREAM_ERROR')));
    submit('Olinda');
    expect(panel().getAttribute('data-theme')).toBe('night');

    api.searchCities.mockReturnValue(of([city(1), city(2)]));
    submit('Recife');
    expect(panel().getAttribute('data-theme')).toBe('night');

    api.searchCities.mockReturnValue(of([city(1)]));
    api.getWeather.mockReturnValue(of(weather(true)));
    submit('Recife');
    expect(panel().getAttribute('data-theme')).toBe('day');
  });

  it('SCN-042 isDay nulo → day', () => {
    api.searchCities.mockReturnValue(of([city(1)]));
    api.getWeather.mockReturnValue(of(weather(null)));
    submit('Recife');
    expect(panel().getAttribute('data-theme')).toBe('day');
  });

  it('SCN-043 aria-busy e esqueleto durante loading; foco no 1º item em choosing', () => {
    const pending = new Subject<City[]>();
    api.searchCities.mockReturnValue(pending);
    submit('Recife');
    expect(section().getAttribute('aria-busy')).toBe('true');
    expect(section().querySelector('app-weather-skeleton')).toBeTruthy();
    expect(section().getAttribute('aria-live')).toBe('polite');
    pending.next([city(1), city(2)]);
    fixture.detectChanges();
    const first = section().querySelector('.option') as HTMLButtonElement;
    expect(document.activeElement).toBe(first);
    expect(section().getAttribute('aria-busy')).toBe('false');
  });
});
