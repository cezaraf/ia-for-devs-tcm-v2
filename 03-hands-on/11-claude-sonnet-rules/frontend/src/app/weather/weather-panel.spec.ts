import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { City, Weather, WeatherApi } from './weather-api';
import { WeatherPanel } from './weather-panel';

const curitiba: City = {
  id: 1,
  name: 'Curitiba',
  admin1: 'Paraná',
  country: 'Brasil',
  countryCode: 'BR',
  latitude: -25.43,
  longitude: -49.27,
};
const santaCruzBA: City = { ...curitiba, id: 2, name: 'Santa Cruz', admin1: 'Bahia' };
const santaCruzBO: City = {
  ...curitiba,
  id: 3,
  name: 'Santa Cruz',
  admin1: null,
  country: 'Bolívia',
  latitude: -17.8,
  longitude: -63.18,
};

const dayWeather: Weather = {
  latitude: -25.43,
  longitude: -49.27,
  timezone: 'America/Sao_Paulo',
  observedAt: '2026-10-13T14:15',
  temperature: 23.6,
  apparentTemperature: 24.4,
  relativeHumidity: 78,
  precipitation: 0,
  windSpeed: 12.4,
  windDirection: 45,
  windDirectionCardinal: 'NE',
  weatherCode: 3,
  condition: 'Nublado',
  isDay: true,
};
const nightWeather: Weather = { ...dayWeather, isDay: false, observedAt: '2026-10-13T23:05' };

const httpError = (status: number, code?: string) =>
  new HttpErrorResponse({ status, error: code ? { code, message: 'x' } : null });

describe('WeatherPanel', () => {
  let api: { cities: ReturnType<typeof vi.fn>; weather: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<WeatherPanel>;
  let el: HTMLElement;

  async function create() {
    fixture = TestBed.createComponent(WeatherPanel);
    el = fixture.nativeElement;
    document.body.appendChild(el);
    await fixture.whenStable();
  }

  async function search(text: string) {
    el.querySelector<HTMLInputElement>('input')!.value = text;
    el.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
  }

  const click = async (selector: string) => {
    el.querySelector<HTMLElement>(selector)!.click();
    await fixture.whenStable();
  };
  const theme = () => el.querySelector('.page')!.getAttribute('data-theme');
  const resultText = () => el.querySelector('.result')!.textContent!.replace(/\s+/g, ' ').trim();
  const busy = () => el.querySelector('.result')!.getAttribute('aria-busy');

  beforeEach(() => {
    api = { cities: vi.fn(), weather: vi.fn() };
    TestBed.configureTestingModule({ providers: [{ provide: WeatherApi, useValue: api }] });
  });

  afterEach(() => {
    el?.remove();
    Reflect.deleteProperty(navigator, 'geolocation');
  });

  function mockGeolocation(impl: (ok: PositionCallback, err: PositionErrorCallback) => void) {
    const getCurrentPosition = vi.fn(impl);
    Object.defineProperty(navigator, 'geolocation', {
      value: { getCurrentPosition },
      configurable: true,
    });
    return getCurrentPosition;
  }
  const position = (latitude: number, longitude: number) =>
    ({ coords: { latitude, longitude } }) as GeolocationPosition;

  it('começa em idle com tema day, título e texto de ajuda', async () => {
    await create();

    expect(el.querySelector('h1')!.textContent).toBe('[clima]');
    expect(resultText()).toBe('digite uma cidade para ver o clima atual');
    expect(theme()).toBe('day');
    expect(busy()).toBe('false');
    expect(el.querySelectorAll('button').length).toBe(1);
  });

  it('1 resultado: carrega o clima direto, sem lista', async () => {
    api.cities.mockReturnValue(of([curitiba]));
    api.weather.mockReturnValue(of(dayWeather));
    await create();

    await search('  Curitiba ');

    expect(api.cities).toHaveBeenCalledWith('Curitiba');
    expect(api.weather).toHaveBeenCalledWith(-25.43, -49.27);
    expect(el.querySelector('app-city-list')).toBeNull();
    expect(el.querySelector('app-weather-card .label')!.textContent).toBe(
      'Curitiba, Paraná, Brasil',
    );
    expect(el.querySelector('app-weather-card .temperature')!.textContent).toBe('24°C');
    expect(el.querySelector('app-weather-card .badge')!.textContent).toBe('[ DIA ]');
    expect(theme()).toBe('day');
  });

  it('vários resultados: mostra a lista, foca o primeiro item e carrega a cidade escolhida', async () => {
    api.cities.mockReturnValue(of([santaCruzBA, santaCruzBO]));
    api.weather.mockReturnValue(of(dayWeather));
    await create();

    await search('Santa Cruz');

    const items = el.querySelectorAll<HTMLButtonElement>('app-city-list button');
    expect(items.length).toBe(2);
    expect(api.weather).not.toHaveBeenCalled();
    expect(document.activeElement).toBe(items[0]);

    items[1].click();
    await fixture.whenStable();

    expect(api.weather).toHaveBeenCalledWith(-17.8, -63.18);
    expect(el.querySelector('app-weather-card .label')!.textContent).toBe('Santa Cruz, Bolívia');
  });

  it('mostra skeleton com aria-busy durante a busca e o carregamento', async () => {
    const cities$ = new Subject<City[]>();
    const weather$ = new Subject<Weather>();
    api.cities.mockReturnValue(cities$);
    api.weather.mockReturnValue(weather$);
    await create();

    await search('Curitiba');
    expect(el.querySelector('app-weather-card-skeleton')).not.toBeNull();
    expect(busy()).toBe('true');
    el.querySelectorAll('button').forEach((b) => expect(b.disabled).toBe(true));

    cities$.next([curitiba]);
    await fixture.whenStable();
    expect(el.querySelector('app-weather-card-skeleton')).not.toBeNull();
    expect(busy()).toBe('true');

    weather$.next(dayWeather);
    await fixture.whenStable();
    expect(el.querySelector('app-weather-card-skeleton')).toBeNull();
    expect(busy()).toBe('false');
  });

  it('tema acompanha isDay: noite → night e [ NOITE ]; nova consulta de dia volta a day', async () => {
    api.cities.mockReturnValue(of([curitiba]));
    api.weather.mockReturnValue(of(nightWeather));
    await create();

    await search('Curitiba');
    expect(theme()).toBe('night');
    expect(el.querySelector('app-weather-card .badge')!.textContent).toBe('[ NOITE ]');

    const pending$ = new Subject<Weather>();
    api.weather.mockReturnValue(pending$);
    await search('Curitiba');
    expect(theme()).toBe('night');

    pending$.next(dayWeather);
    await fixture.whenStable();
    expect(theme()).toBe('day');
    expect(el.querySelector('app-weather-card .badge')!.textContent).toBe('[ DIA ]');
  });

  it('erro volta o tema para day', async () => {
    api.cities.mockReturnValue(of([curitiba]));
    api.weather.mockReturnValueOnce(of(nightWeather));
    await create();
    await search('Curitiba');
    expect(theme()).toBe('night');

    api.weather.mockReturnValue(throwError(() => httpError(502, 'UPSTREAM_ERROR')));
    await search('Curitiba');

    expect(theme()).toBe('day');
  });

  it('validação: menos de 2 caracteres não chama o backend, mostra erro sem retry e foca o input', async () => {
    await create();

    await search(' a ');

    expect(api.cities).not.toHaveBeenCalled();
    expect(resultText()).toBe('[x] Digite ao menos 2 caracteres.');
    expect(el.querySelector('app-error-message button')).toBeNull();
    expect(document.activeElement).toBe(el.querySelector('input'));

    await search('');
    expect(api.cities).not.toHaveBeenCalled();
  });

  it('404 CITY_NOT_FOUND: mensagem sem retry e foco no input', async () => {
    api.cities.mockReturnValue(throwError(() => httpError(404, 'CITY_NOT_FOUND')));
    await create();

    await search('Xyzabcqwe');

    expect(resultText()).toBe('[x] Cidade não encontrada. Verifique o nome e tente novamente.');
    expect(el.querySelector('app-error-message button')).toBeNull();
    expect(document.activeElement).toBe(el.querySelector('input'));
  });

  it('400: "Requisição inválida." sem retry', async () => {
    api.cities.mockReturnValue(throwError(() => httpError(400, 'INVALID_CITY_NAME')));
    await create();

    await search('Curitiba');

    expect(resultText()).toBe('[x] Requisição inválida.');
    expect(el.querySelector('app-error-message button')).toBeNull();
  });

  it.each([
    [502, 'UPSTREAM_ERROR', 'O serviço de clima está indisponível no momento.'],
    [504, 'UPSTREAM_TIMEOUT', 'O serviço de clima demorou para responder.'],
    [0, undefined, 'Não foi possível conectar ao servidor.'],
    [500, undefined, 'Não foi possível conectar ao servidor.'],
  ])(
    'status %i: mensagem com retry que repete a busca de cidades',
    async (status, code, message) => {
      api.cities.mockReturnValueOnce(throwError(() => httpError(status, code)));
      await create();

      await search('Curitiba');
      expect(resultText()).toContain(`[x] ${message}`);
      const retry = el.querySelector('app-error-message button')!;
      expect(retry.textContent?.trim()).toBe('tentar novamente');

      api.cities.mockReturnValue(of([curitiba]));
      api.weather.mockReturnValue(of(dayWeather));
      await click('app-error-message button');

      expect(api.cities).toHaveBeenCalledTimes(2);
      expect(api.cities).toHaveBeenLastCalledWith('Curitiba');
      expect(el.querySelector('app-weather-card')).not.toBeNull();
    },
  );

  it('retry após falha do clima repete a busca do clima (sem refazer a busca de cidades)', async () => {
    api.cities.mockReturnValue(of([santaCruzBA, santaCruzBO]));
    api.weather.mockReturnValueOnce(throwError(() => httpError(504, 'UPSTREAM_TIMEOUT')));
    await create();
    await search('Santa Cruz');

    await click('app-city-list button');
    expect(resultText()).toContain('[x] O serviço de clima demorou para responder.');

    api.weather.mockReturnValue(of(dayWeather));
    await click('app-error-message button');

    expect(api.cities).toHaveBeenCalledTimes(1);
    expect(api.weather).toHaveBeenCalledTimes(2);
    expect(api.weather).toHaveBeenLastCalledWith(-25.43, -49.27);
    expect(el.querySelector('app-weather-card')).not.toBeNull();
  });

  it('resposta atrasada da primeira busca não sobrescreve a segunda', async () => {
    const first$ = new Subject<City[]>();
    const second$ = new Subject<City[]>();
    api.cities.mockReturnValueOnce(first$).mockReturnValueOnce(second$);
    api.weather.mockReturnValue(of(dayWeather));
    await create();

    await search('Curitiba');
    await search('Santa Cruz');
    second$.next([santaCruzBA, santaCruzBO]);
    await fixture.whenStable();
    first$.next([curitiba]);
    await fixture.whenStable();

    expect(api.weather).not.toHaveBeenCalled();
    expect(el.querySelectorAll('app-city-list button').length).toBe(2);
  });

  it('clima atrasado de uma cidade antiga é ignorado', async () => {
    const slow$ = new Subject<Weather>();
    api.cities.mockReturnValue(of([curitiba]));
    api.weather.mockReturnValueOnce(slow$).mockReturnValueOnce(of(nightWeather));
    await create();

    await search('Curitiba');
    await search('Curitiba');
    slow$.next(dayWeather);
    await fixture.whenStable();

    expect(theme()).toBe('night');
    expect(el.querySelector('app-weather-card .badge')!.textContent).toBe('[ NOITE ]');
  });

  describe('geolocalização', () => {
    it('sem navigator.geolocation: fica em idle e não mostra o botão', async () => {
      await create();

      expect(el.textContent).not.toContain('[usar minha localização]');
      expect(resultText()).toBe('digite uma cidade para ver o clima atual');
    });

    it('ao abrir, com permissão: carrega o clima rotulado "Sua localização"', async () => {
      const getCurrentPosition = mockGeolocation((ok) => ok(position(-10, -20)));
      api.weather.mockReturnValue(of(dayWeather));

      await create();

      expect(getCurrentPosition).toHaveBeenCalledWith(expect.any(Function), expect.any(Function), {
        timeout: 10000,
        maximumAge: 600000,
      });
      expect(api.weather).toHaveBeenCalledWith(-10, -20);
      expect(el.querySelector('app-weather-card .label')!.textContent).toBe('Sua localização');
      expect(api.cities).not.toHaveBeenCalled();
    });

    it('ao abrir, negada ou expirada: fica em idle sem erro', async () => {
      mockGeolocation((_ok, err) => err({ code: 1 } as GeolocationPositionError));
      await create();

      expect(resultText()).toBe('digite uma cidade para ver o clima atual');
      expect(el.querySelector('[role="alert"]')).toBeNull();
      expect(el.textContent).toContain('[usar minha localização]');
    });

    it('botão: negada mostra mensagem sem retry e foca o input', async () => {
      const g = mockGeolocation((_ok, err) => err({ code: 1 } as GeolocationPositionError));
      await create();
      g.mockClear();

      await click('button.secondary');

      expect(g).toHaveBeenCalledTimes(1);
      expect(resultText()).toBe('[x] Permissão de localização negada. Digite uma cidade.');
      expect(el.querySelector('app-error-message button')).toBeNull();
      expect(document.activeElement).toBe(el.querySelector('input'));
    });

    it('botão: indisponível/expirada mostra erro com retry que tenta de novo', async () => {
      let attempt = 0;
      mockGeolocation((ok, err) => {
        attempt++;
        if (attempt < 3) {
          err({ code: 3 } as GeolocationPositionError);
        } else {
          ok(position(1, 2));
        }
      });
      api.weather.mockReturnValue(of(dayWeather));
      await create();

      await click('button.secondary');
      expect(resultText()).toContain('[x] Não foi possível obter sua localização.');

      await click('app-error-message button');

      expect(api.weather).toHaveBeenCalledWith(1, 2);
      expect(el.querySelector('app-weather-card .label')!.textContent).toBe('Sua localização');
    });

    it('busca digitada durante a geolocalização automática cancela a posição pendente', async () => {
      let ok!: PositionCallback;
      mockGeolocation((success) => (ok = success));
      api.cities.mockReturnValue(of([curitiba]));
      api.weather.mockReturnValue(of(nightWeather));
      await create();

      await search('Curitiba');
      ok(position(5, 5));
      await fixture.whenStable();

      expect(api.weather).toHaveBeenCalledTimes(1);
      expect(api.weather).toHaveBeenCalledWith(-25.43, -49.27);
      expect(el.querySelector('app-weather-card .label')!.textContent).toBe(
        'Curitiba, Paraná, Brasil',
      );
    });
  });
});
