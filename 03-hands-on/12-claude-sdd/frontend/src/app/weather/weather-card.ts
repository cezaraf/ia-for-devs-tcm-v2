import { Component, computed, input } from '@angular/core';
import { Weather } from './weather-api';

const NUMBER = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
const DASH = '—';

function format(value: number | null, unit: string): string {
  return value === null ? DASH : `${NUMBER.format(value)} ${unit}`;
}

@Component({
  selector: 'app-weather-card',
  imports: [],
  templateUrl: './weather-card.html',
  styleUrl: './weather-card.css',
})
export class WeatherCard {
  readonly weather = input.required<Weather>();
  readonly locationLabel = input.required<string>();

  protected readonly badge = computed(() => {
    const isDay = this.weather().isDay;
    return isDay === null ? null : isDay ? '[ DIA ]' : '[ NOITE ]';
  });
  protected readonly temperature = computed(() => format(this.weather().temperature, '°C'));
  protected readonly apparent = computed(() => format(this.weather().apparentTemperature, '°C'));
  protected readonly humidity = computed(() => format(this.weather().humidity, '%'));
  protected readonly wind = computed(() => {
    const { windSpeed, windDirection } = this.weather();
    if (windSpeed === null) {
      return DASH;
    }
    const speed = format(windSpeed, 'km/h');
    return windDirection ? `${speed} · ${windDirection}` : speed;
  });
  protected readonly observedTime = computed(() => this.weather().observedAt.slice(11, 16));
}
