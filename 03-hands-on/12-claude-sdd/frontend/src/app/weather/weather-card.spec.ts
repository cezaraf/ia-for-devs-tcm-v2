import { TestBed } from '@angular/core/testing';
import { Weather } from './weather-api';
import { WeatherCard } from './weather-card';

const BASE: Weather = {
  latitude: -8.05,
  longitude: -34.88,
  timezone: 'America/Recife',
  observedAt: '2026-10-20T16:00',
  temperature: 28.6,
  apparentTemperature: 31,
  humidity: 62,
  windSpeed: 12.7,
  windDirectionDegrees: 90,
  windDirection: 'Leste',
  weatherCode: 1,
  condition: 'Predominantemente limpo',
  isDay: true,
};

describe('WeatherCard', () => {
  async function render(over: Partial<Weather> = {}) {
    const fixture = TestBed.createComponent(WeatherCard);
    fixture.componentRef.setInput('weather', { ...BASE, ...over });
    fixture.componentRef.setInput('locationLabel', 'Recife');
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    const row = (label: string) =>
      Array.from(el.querySelectorAll('.row'))
        .find((r) => r.querySelector('dt')?.textContent === label)
        ?.querySelector('dd')?.textContent;
    return {
      el,
      row,
      temperature: el.querySelector('.temperature')?.textContent,
      observed: el.querySelector('.observed')?.textContent,
    };
  }

  it('SCN-027 formata valores em PT-BR', async () => {
    const { el, row, temperature } = await render();
    expect(el.querySelector('h2')?.textContent).toBe('Recife');
    expect(temperature).toBe('28,6 °C');
    expect(row('Sensação')).toBe('31 °C');
    expect(row('Umidade')).toBe('62 %');
    expect(row('Vento')).toBe('12,7 km/h · Leste');
    expect(el.querySelector('.condition')?.textContent).toBe('Predominantemente limpo');
  });

  it('SCN-027 mostra "—" em campos nulos', async () => {
    const { row, temperature } = await render({
      humidity: null,
      apparentTemperature: null,
      temperature: null,
    });
    expect(row('Umidade')).toBe('—');
    expect(row('Sensação')).toBe('—');
    expect(temperature).toBe('—');
  });

  it('SCN-027 direção nula mostra só a velocidade; ambos nulos mostram "—"', async () => {
    expect((await render({ windDirection: null })).row('Vento')).toBe('12,7 km/h');
    expect((await render({ windSpeed: null, windDirection: null })).row('Vento')).toBe('—');
  });

  it('SCN-027 horário vem da string, sem conversão de fuso', async () => {
    expect((await render()).observed).toBe('Medido às 16:00 (horário local)');
  });

  it('SCN-027 badge [ DIA ], [ NOITE ] e omitida', async () => {
    expect((await render({ isDay: true })).el.querySelector('.badge')?.textContent).toBe('[ DIA ]');
    expect((await render({ isDay: false })).el.querySelector('.badge')?.textContent).toBe('[ NOITE ]');
    expect((await render({ isDay: null })).el.querySelector('.badge')).toBeNull();
  });
});
