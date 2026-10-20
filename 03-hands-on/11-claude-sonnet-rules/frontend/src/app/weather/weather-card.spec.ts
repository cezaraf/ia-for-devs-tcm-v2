import { TestBed } from '@angular/core/testing';
import { Weather } from './weather-api';
import { WeatherCard } from './weather-card';

const weather: Weather = {
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

describe('WeatherCard', () => {
  async function render(w: Weather, label: string) {
    const fixture = TestBed.createComponent(WeatherCard);
    fixture.componentRef.setInput('weather', w);
    fixture.componentRef.setInput('label', label);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const text = (el: HTMLElement, selector: string) =>
    el.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();

  it('mostra rótulo, badge, temperatura inteira, condição, detalhes e rodapé', async () => {
    const el = await render(weather, 'Curitiba, Paraná, Brasil');

    expect(text(el, '.label')).toBe('Curitiba, Paraná, Brasil');
    expect(text(el, '.badge')).toBe('[ DIA ]');
    expect(text(el, '.temperature')).toBe('24°C');
    expect(text(el, '.condition')).toBe('Nublado');
    const details = Array.from(el.querySelectorAll('.detail')).map((d) =>
      d.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(details).toEqual([
      '[+] sensação 24°C',
      '[+] umidade 78%',
      '[+] vento 12 km/h NE',
      '[+] precipitação 0.0 mm',
    ]);
    expect(text(el, '.footer')).toBe('medido às 14:15 (hora local)');
  });

  it('mostra [ NOITE ] à noite e "Sua localização" como rótulo', async () => {
    const el = await render({ ...weather, isDay: false, precipitation: 1.25 }, 'Sua localização');

    expect(text(el, '.badge')).toBe('[ NOITE ]');
    expect(text(el, '.label')).toBe('Sua localização');
    expect(text(el, '.detail:last-child')).toBe('[+] precipitação 1.3 mm');
  });
});
