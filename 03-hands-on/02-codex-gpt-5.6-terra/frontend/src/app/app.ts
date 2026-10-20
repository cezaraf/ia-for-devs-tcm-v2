import { DecimalPipe, NgIf } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface WeatherResult {
  location: { name: string; admin1?: string; country: string };
  observedAt: string;
  weather: { temperature: number; apparentTemperature: number; humidity: number; windSpeed: number; code: number };
}

@Component({ selector: 'app-root', imports: [FormsModule, NgIf, DecimalPipe], templateUrl: './app.html', styleUrl: './app.css' })
export class App {
  city = '';
  protected readonly weather = signal<WeatherResult | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  async search(): Promise<void> {
    const city = this.city.trim();
    if (!city) return;
    this.loading.set(true);
    this.error.set('');
    try {
      const response = await fetch(`api/weather?city=${encodeURIComponent(city)}`);
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Não foi possível obter o clima agora.');
      this.weather.set(data as WeatherResult);
    } catch (error) {
      this.weather.set(null);
      this.error.set(error instanceof Error ? error.message : 'Não foi possível obter o clima agora.');
    } finally { this.loading.set(false); }
  }

  protected iconFor(code: number): string {
    if ([0, 1].includes(code)) return '☀️';
    if ([2, 3].includes(code)) return '⛅';
    if ([45, 48].includes(code)) return '🌫️';
    if ([51, 53, 55, 56, 57, 61, 63, 65, 80, 81, 82].includes(code)) return '🌧️';
    if ([71, 73, 75, 77, 85, 86].includes(code)) return '❄️';
    return '⛈️';
  }

  protected descriptionFor(code: number): string {
    const descriptions: Record<number, string> = { 0: 'Céu limpo', 1: 'Predominantemente limpo', 2: 'Parcialmente nublado', 3: 'Nublado', 45: 'Neblina', 48: 'Neblina com geada', 51: 'Garoa fraca', 53: 'Garoa moderada', 55: 'Garoa intensa', 61: 'Chuva fraca', 63: 'Chuva moderada', 65: 'Chuva forte', 71: 'Neve fraca', 73: 'Neve moderada', 75: 'Neve forte', 80: 'Pancadas fracas', 81: 'Pancadas moderadas', 82: 'Pancadas fortes', 95: 'Trovoadas' };
    return descriptions[code] || 'Condições variáveis';
  }
}
