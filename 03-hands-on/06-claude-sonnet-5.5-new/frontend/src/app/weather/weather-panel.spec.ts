import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WeatherPanel } from './weather-panel';
import { City, Weather } from './weather.models';

const curitiba: City = {
  id: 1,
  name: 'Curitiba',
  admin1: 'Paraná',
  country: 'Brasil',
  countryCode: 'BR',
  latitude: -25.42,
  longitude: -49.27,
};

const santaCruzRn: City = { ...curitiba, id: 2, name: 'Santa Cruz', admin1: 'Rio Grande do Norte' };
const santaCruzCa: City = {
  ...curitiba,
  id: 3,
  name: 'Santa Cruz',
  admin1: 'Califórnia',
  country: 'Estados Unidos',
};

function weatherOf(overrides: Partial<Weather> = {}): Weather {
  return {
    latitude: -25.5,
    longitude: -49.25,
    timezone: 'America/Sao_Paulo',
    observedAt: '2026-10-13T14:15',
    temperature: 23.6,
    apparentTemperature: 24.1,
    relativeHumidity: 78,
    precipitation: 0,
    windSpeed: 12.4,
    windDirection: 50,
    windDirectionCardinal: 'NE',
    weatherCode: 3,
    condition: 'Nublado',
    isDay: true,
    ...overrides,
  };
}

describe('WeatherPanel', () => {
  let fixture: ComponentFixture<WeatherPanel>;
  let http: HttpTestingController;
  let root: HTMLElement;

  const query = <T extends HTMLElement>(selector: string) => root.querySelector<T>(selector);
  const text = () => root.textContent ?? '';
  const theme = () => query('.panel')?.getAttribute('data-theme');
  const cities = (req: TestRequest = http.expectOne((r) => r.url === 'api/cities')) => req;
  const weatherRequest = () => http.expectOne((r) => r.url === 'api/weather');
  const errorResponse = (code: string, status: number) => ({
    body: { code, message: 'x' },
    opts: { status, statusText: 'err' },
  });

  async function settle(): Promise<void> {
    await fixture.whenStable();
  }

  async function search(name: string): Promise<void> {
    const input = query<HTMLInputElement>('input')!;
    input.value = name;
    input.dispatchEvent(new Event('input'));
    query<HTMLFormElement>('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await settle();
  }

  async function create(): Promise<void> {
    fixture = TestBed.createComponent(WeatherPanel);
    root = fixture.nativeElement as HTMLElement;
    await settle();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WeatherPanel],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    Reflect.deleteProperty(navigator, 'geolocation');
  });

  it('starts idle with the day theme and no request', async () => {
    await create();

    expect(text()).toContain('digite uma cidade para ver o clima atual');
    expect(theme()).toBe('day');
    expect(query('[aria-live="polite"]')?.getAttribute('aria-busy')).toBe('false');
    expect(query('h1')?.textContent).toBe('[clima]');
  });

  it('loads the weather straight away when the search returns a single city', async () => {
    await create();
    await search('Curitiba');

    expect(query('app-weather-card-skeleton')).not.toBeNull();
    expect(query('[aria-live="polite"]')?.getAttribute('aria-busy')).toBe('true');

    const citiesReq = cities();
    expect(citiesReq.request.params.get('name')).toBe('Curitiba');
    citiesReq.flush([curitiba]);
    await settle();

    const req = weatherRequest();
    expect(req.request.params.get('latitude')).toBe('-25.42');
    expect(req.request.params.get('longitude')).toBe('-49.27');
    req.flush(weatherOf());
    await settle();

    expect(query('app-city-list')).toBeNull();
    expect(query('app-weather-card h2')?.textContent).toBe('Curitiba, Paraná, Brasil');
    expect(query('.temperature')?.textContent).toBe('24°C');
    expect(query('.condition')?.textContent).toBe('Nublado');
    expect(text()).toContain('[+] sensação  24°C');
    expect(text()).toContain('[+] umidade  78%');
    expect(text()).toContain('[+] vento  12 km/h NE');
    expect(text()).toContain('[+] precipitação  0.0 mm');
    expect(text()).toContain('medido às 14:15 (hora local)');
    expect(query('.badge')?.textContent).toBe('[ DIA ]');
    expect(query('[aria-live="polite"]')?.getAttribute('aria-busy')).toBe('false');
  });

  it('lists homonyms, focuses the first one and loads the chosen city', async () => {
    await create();
    await search('Santa Cruz');
    cities().flush([santaCruzRn, santaCruzCa]);
    await settle();

    const options = Array.from(root.querySelectorAll<HTMLButtonElement>('app-city-list button'));
    expect(options.map((o) => o.textContent?.trim())).toEqual([
      '[+] Santa Cruz — Rio Grande do Norte, Brasil',
      '[+] Santa Cruz — Califórnia, Estados Unidos',
    ]);
    expect(document.activeElement).toBe(options[0]);

    options[1].click();
    await settle();
    const req = weatherRequest();
    req.flush(weatherOf());
    await settle();

    expect(query('app-weather-card h2')?.textContent).toBe(
      'Santa Cruz, Califórnia, Estados Unidos',
    );
    expect(query('app-city-list')).toBeNull();
  });

  it('omits null parts from list rows', async () => {
    await create();
    await search('Santa Cruz');
    cities().flush([{ ...santaCruzRn, admin1: null, country: null }, santaCruzCa]);
    await settle();

    expect(query('app-city-list button')?.textContent?.trim()).toBe('[+] Santa Cruz');
  });

  it('shows the error, retries the same city search and then recovers', async () => {
    await create();
    await search('Curitiba');
    const failed = errorResponse('UPSTREAM_ERROR', 502);
    cities().flush(failed.body, failed.opts);
    await settle();

    expect(query('app-error-message')).not.toBeNull();
    expect(query('[role="alert"]')?.textContent).toContain('[x]');
    expect(text()).toContain('O serviço de clima está indisponível no momento.');
    expect(theme()).toBe('day');

    query<HTMLButtonElement>('app-error-message button')!.click();
    await settle();
    expect(query('app-weather-card-skeleton')).not.toBeNull();
    const retried = cities();
    expect(retried.request.params.get('name')).toBe('Curitiba');
    retried.flush([curitiba]);
    await settle();
    weatherRequest().flush(weatherOf());
    await settle();

    expect(query('app-error-message')).toBeNull();
    expect(query('app-weather-card')).not.toBeNull();
  });

  it('retries only the weather request when the weather lookup failed', async () => {
    await create();
    await search('Curitiba');
    cities().flush([curitiba]);
    await settle();
    const timeout = errorResponse('UPSTREAM_TIMEOUT', 504);
    weatherRequest().flush(timeout.body, timeout.opts);
    await settle();

    expect(text()).toContain('O serviço de clima demorou para responder.');

    query<HTMLButtonElement>('app-error-message button')!.click();
    await settle();
    weatherRequest().flush(weatherOf());
    await settle();

    expect(query('app-weather-card')).not.toBeNull();
  });

  it('shows the connection message, with retry, when the backend is unreachable', async () => {
    await create();
    await search('Curitiba');
    cities().error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
    await settle();

    expect(text()).toContain('Não foi possível conectar ao servidor.');
    expect(query('app-error-message button')?.textContent?.trim()).toBe('tentar novamente');
  });

  it('does not mistake a proxy 502 without payload for an upstream error', async () => {
    await create();
    await search('Curitiba');
    cities().flush('Bad Gateway', { status: 502, statusText: 'Bad Gateway' });
    await settle();

    expect(text()).toContain('Não foi possível conectar ao servidor.');
  });

  it('shows "cidade não encontrada" without retry and refocuses the input', async () => {
    await create();
    await search('Xyzabcqwe');
    const notFound = errorResponse('CITY_NOT_FOUND', 404);
    cities().flush(notFound.body, notFound.opts);
    await settle();

    expect(text()).toContain('Cidade não encontrada. Verifique o nome e tente novamente.');
    expect(query('app-error-message button')).toBeNull();
    expect(document.activeElement).toBe(query('input'));
  });

  it('shows "requisição inválida" without retry on a 400', async () => {
    await create();
    await search('Curitiba');
    const invalid = errorResponse('INVALID_CITY_NAME', 400);
    cities().flush(invalid.body, invalid.opts);
    await settle();

    expect(text()).toContain('Requisição inválida.');
    expect(query('app-error-message button')).toBeNull();
  });

  it.each(['', ' ', 'a', ' a '])('rejects %j without calling the backend', async (name) => {
    await create();
    await search(name);

    http.expectNone((r) => r.url === 'api/cities');
    expect(text()).toContain('Digite ao menos 2 caracteres.');
    expect(query('app-error-message button')).toBeNull();
    expect(document.activeElement).toBe(query('input'));
  });

  it('switches between night and day themes following isDay', async () => {
    await create();
    await search('Curitiba');
    cities().flush([curitiba]);
    await settle();
    weatherRequest().flush(weatherOf({ isDay: false }));
    await settle();

    expect(theme()).toBe('night');
    expect(query('.badge')?.textContent).toBe('[ NOITE ]');

    await search('Curitiba');
    expect(theme()).toBe('night');
    cities().flush([curitiba]);
    await settle();
    weatherRequest().flush(weatherOf({ isDay: true }));
    await settle();

    expect(theme()).toBe('day');
    expect(query('.badge')?.textContent).toBe('[ DIA ]');
  });

  it('goes back to the day theme on error', async () => {
    await create();
    await search('Curitiba');
    cities().flush([curitiba]);
    await settle();
    weatherRequest().flush(weatherOf({ isDay: false }));
    await settle();
    expect(theme()).toBe('night');

    await search('Curitiba');
    const failed = errorResponse('UPSTREAM_ERROR', 502);
    cities().flush(failed.body, failed.opts);
    await settle();

    expect(theme()).toBe('day');
  });

  it('ignores the late response of an older search', async () => {
    await create();
    await search('Curitiba');
    const first = cities();
    await search('Santa Cruz');
    const second = cities();

    expect(first.cancelled).toBe(true);
    second.flush([santaCruzRn, santaCruzCa]);
    await settle();

    expect(query('app-city-list')).not.toBeNull();
    expect(query('app-weather-card')).toBeNull();
  });

  describe('geolocation', () => {
    function stubGeolocation(
      behaviour: (success: PositionCallback, failure: PositionErrorCallback) => void,
    ) {
      const getCurrentPosition = vi.fn(
        (success: PositionCallback, failure: PositionErrorCallback, _options?: PositionOptions) =>
          behaviour(success, failure),
      );
      Object.defineProperty(navigator, 'geolocation', {
        configurable: true,
        value: { getCurrentPosition },
      });
      return getCurrentPosition;
    }

    const position = { coords: { latitude: -23.5, longitude: -46.6 } } as GeolocationPosition;
    const denied = { code: 1 } as GeolocationPositionError;
    const timeout = { code: 3 } as GeolocationPositionError;

    it('has no location button when the browser lacks geolocation', async () => {
      await create();

      expect(Array.from(root.querySelectorAll('button')).map((b) => b.textContent?.trim())).toEqual(
        ['buscar'],
      );
    });

    it('loads the weather automatically as "Sua localização"', async () => {
      const getCurrentPosition = stubGeolocation((success) => success(position));
      await create();

      expect(getCurrentPosition.mock.calls[0][2]).toEqual({ timeout: 10000, maximumAge: 600000 });
      const req = weatherRequest();
      expect(req.request.params.get('latitude')).toBe('-23.5');
      expect(req.request.params.get('longitude')).toBe('-46.6');
      req.flush(weatherOf());
      await settle();

      expect(query('app-weather-card h2')?.textContent).toBe('Sua localização');
      http.expectNone((r) => r.url === 'api/cities');
    });

    it('stays idle, without error, when the automatic request is denied', async () => {
      stubGeolocation((_success, failure) => failure(denied));
      await create();

      expect(query('app-error-message')).toBeNull();
      expect(text()).toContain('digite uma cidade para ver o clima atual');
    });

    it('explains a denial from the button and focuses the input', async () => {
      let calls = 0;
      stubGeolocation((success, failure) => (calls++ < 2 ? failure(denied) : success(position)));
      await create();

      const button = Array.from(root.querySelectorAll('button')).find((b) =>
        b.textContent?.includes('[usar minha localização]'),
      )!;
      button.click();
      await settle();

      expect(text()).toContain('Permissão de localização negada. Digite uma cidade.');
      expect(query('app-error-message button')).toBeNull();
      expect(document.activeElement).toBe(query('input'));
    });

    it('offers retry when the position is unavailable or expired', async () => {
      let calls = 0;
      stubGeolocation((success, failure) => (calls++ < 2 ? failure(timeout) : success(position)));
      await create();

      Array.from(root.querySelectorAll('button'))
        .find((b) => b.textContent?.includes('[usar minha localização]'))!
        .click();
      await settle();
      expect(text()).toContain('Não foi possível obter sua localização.');

      query<HTMLButtonElement>('app-error-message button')!.click();
      await settle();
      weatherRequest().flush(weatherOf());
      await settle();

      expect(query('app-weather-card h2')?.textContent).toBe('Sua localização');
    });
  });
});
