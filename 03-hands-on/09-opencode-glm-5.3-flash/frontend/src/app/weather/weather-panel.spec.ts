import { provideHttpClient, withFetch } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { City, Weather } from './weather-api';
import { WeatherPanel } from './weather-panel';

const CURITIBA: City = {
  id: 3454561,
  name: 'Curitiba',
  admin1: 'Paraná',
  country: 'Brasil',
  countryCode: 'BR',
  latitude: -25.43,
  longitude: -49.27,
};

const SANTA_CRUZ: City[] = [
  {
    id: 1,
    name: 'Santa Cruz do Sul',
    admin1: 'Rio Grande do Sul',
    country: 'Brasil',
    countryCode: 'BR',
    latitude: -29.72,
    longitude: -52.43,
  },
  {
    id: 2,
    name: 'Santa Cruz de Tenerife',
    admin1: 'Ilhas Canárias',
    country: 'Espanha',
    countryCode: 'ES',
    latitude: 28.47,
    longitude: -16.25,
  },
];

const CLIMA_DIA: Weather = {
  latitude: -25.43,
  longitude: -49.27,
  timezone: 'America/Sao_Paulo',
  observedAt: '2026-10-13T14:15',
  temperature: 21.4,
  apparentTemperature: 24.1,
  relativeHumidity: 78,
  precipitation: 0,
  windSpeed: 12.3,
  windDirection: 45,
  windDirectionCardinal: 'NE',
  weatherCode: 2,
  condition: 'Parcialmente nublado',
  isDay: true,
};

const CLIMA_NOITE: Weather = { ...CLIMA_DIA, observedAt: '2026-10-13T22:00', isDay: false };

const URL_CURITIBA = 'api/cities?name=Curitiba';
const URL_CLIMA_CURITIBA = 'api/weather?latitude=-25.43&longitude=-49.27';

describe('WeatherPanel', () => {
  let fixture: ComponentFixture<WeatherPanel>;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [WeatherPanel],
      providers: [provideHttpClient(withFetch()), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(WeatherPanel);
    await fixture.whenStable();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  function texto(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function buscarCidade(nome: string): void {
    const elemento = fixture.nativeElement as HTMLElement;
    const input = elemento.querySelector<HTMLInputElement>('input')!;
    input.value = nome;
    input.dispatchEvent(new Event('input'));
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
  }

  async function carregarClimaDireto(clima: Weather): Promise<void> {
    buscarCidade('Curitiba');
    http.expectOne(URL_CURITIBA).flush([CURITIBA]);
    http.expectOne(URL_CLIMA_CURITIBA).flush(clima);
    await fixture.whenStable();
  }

  it('começa em idle com a dica para digitar uma cidade', () => {
    expect(texto()).toContain('digite uma cidade para ver o clima atual');
  });

  it('carrega o clima direto quando a busca retorna exatamente uma cidade', async () => {
    await carregarClimaDireto(CLIMA_DIA);

    expect(texto()).toContain('Curitiba, Paraná, Brasil');
    expect(texto()).toContain('21°C');
    expect(texto()).toContain('Parcialmente nublado');
    expect(texto()).toContain('[+] sensação  24°C');
    expect(texto()).toContain('[+] umidade  78%');
    expect(texto()).toContain('[+] vento  12 km/h NE');
    expect(texto()).toContain('[+] precipitação  0.0 mm');
    expect(texto()).toContain('medido às 14:15 (hora local)');
    expect(texto()).toContain('[ DIA ]');
  });

  it('mostra a lista de homônimos e carrega o clima da cidade escolhida', async () => {
    buscarCidade('Santa Cruz');
    http.expectOne('api/cities?name=Santa%20Cruz').flush(SANTA_CRUZ);
    await fixture.whenStable();

    const botoes = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      'app-city-list button',
    );
    expect(botoes.length).toBe(2);
    expect(botoes[0].textContent).toContain('[+] Santa Cruz do Sul — Rio Grande do Sul, Brasil');
    expect(botoes[1].textContent).toContain('[+] Santa Cruz de Tenerife — Ilhas Canárias, Espanha');
    expect(document.activeElement).toBe(botoes[0]);

    botoes[1].click();
    http.expectOne('api/weather?latitude=28.47&longitude=-16.25').flush({
      ...CLIMA_DIA,
      temperature: 26.5,
    });
    await fixture.whenStable();

    expect(texto()).toContain('Santa Cruz de Tenerife, Ilhas Canárias, Espanha');
    expect(texto()).toContain('27°C');
  });

  it('aplica data-theme night à noite e day quando volta a ser dia', async () => {
    await carregarClimaDireto(CLIMA_NOITE);
    expect(fixture.nativeElement.getAttribute('data-theme')).toBe('night');
    expect(texto()).toContain('[ NOITE ]');

    await carregarClimaDireto(CLIMA_DIA);
    expect(fixture.nativeElement.getAttribute('data-theme')).toBe('day');
    expect(texto()).toContain('[ DIA ]');
  });

  it('valida o mínimo de caracteres sem chamar o backend', async () => {
    buscarCidade('a');
    await fixture.whenStable();

    expect(texto()).toContain('Digite ao menos 2 caracteres.');
  });

  it('mostra erro de cidade não encontrada sem botão de retry', async () => {
    buscarCidade('Xyzabcqwe');
    http
      .expectOne('api/cities?name=Xyzabcqwe')
      .flush({ code: 'CITY_NOT_FOUND', message: 'Cidade não encontrada.' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(texto()).toContain('Cidade não encontrada. Verifique o nome e tente novamente.');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-error-message button')).toBeNull();
  });

  it('oferece tentar novamente após erro do serviço de clima e repete a última ação', async () => {
    buscarCidade('Curitiba');
    http
      .expectOne(URL_CURITIBA)
      .flush({ code: 'UPSTREAM_ERROR', message: 'erro' }, { status: 502, statusText: 'Bad Gateway' });
    await fixture.whenStable();

    expect(texto()).toContain('O serviço de clima está indisponível no momento.');

    const retry = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      'app-error-message button',
    )!;
    expect(retry.textContent).toContain('tentar novamente');
    retry.click();

    http.expectOne(URL_CURITIBA).flush([CURITIBA]);
    http.expectOne(URL_CLIMA_CURITIBA).flush(CLIMA_DIA);
    await fixture.whenStable();

    expect(texto()).toContain('Curitiba, Paraná, Brasil');
  });

  it('só exibe o resultado da segunda busca quando a primeira responde depois', async () => {
    buscarCidade('Curitiba');
    const primeira = http.expectOne(URL_CURITIBA);

    buscarCidade('Curitiba');
    const segunda = http.expectOne(URL_CURITIBA);

    segunda.flush([CURITIBA]);
    http.expectOne(URL_CLIMA_CURITIBA).flush(CLIMA_DIA);
    await fixture.whenStable();

    expect(texto()).toContain('Curitiba, Paraná, Brasil');
    // A primeira busca foi cancelada: uma resposta atrasada nunca sobrescreveria o estado.
    expect(primeira.cancelled).toBe(true);
  });

  it('marca a área de resultado como ocupada durante o carregamento', async () => {
    buscarCidade('Curitiba');
    fixture.detectChanges();

    const area = (fixture.nativeElement as HTMLElement).querySelector('.result');
    expect(area?.getAttribute('aria-busy')).toBe('true');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-weather-card-skeleton')).toBeTruthy();

    http.expectOne(URL_CURITIBA).flush([CURITIBA]);
    http.expectOne(URL_CLIMA_CURITIBA).flush(CLIMA_DIA);
    await fixture.whenStable();

    expect(area?.getAttribute('aria-busy')).toBeNull();
  });
});
