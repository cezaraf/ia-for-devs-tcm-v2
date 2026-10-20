import { Component, computed, input } from '@angular/core';
import { WeatherResponse } from './weather-api';

@Component({
  selector: 'app-weather-card',
  imports: [],
  templateUrl: './weather-card.html',
  styleUrl: './weather-card.css'
})
export class WeatherCard {
  readonly weather = input.required<WeatherResponse>();
  readonly locationLabel = input.required<string>();

  readonly temperature = computed(() => `${Math.round(this.weather().temperature)}°C`);
  readonly badgeText = computed(() => (this.weather().isDay ? '[ DIA ]' : '[ NOITE ]'));

  readonly apparentTempLine = computed(() => `[+] sensação  ${Math.round(this.weather().apparentTemperature)}°C`);
  readonly humidityLine = computed(() => `[+] umidade  ${this.weather().relativeHumidity}%`);
  readonly windLine = computed(
    () => `[+] vento  ${Math.round(this.weather().windSpeed)} km/h ${this.weather().windDirectionCardinal}`
  );
  readonly precipitationLine = computed(() => `[+] precipitação  ${this.weather().precipitation.toFixed(1)} mm`);

  readonly observedTime = computed(() => {
    const raw = this.weather().observedAt;
    if (!raw) return '';
    if (raw.includes('T')) {
      return raw.split('T')[1].substring(0, 5);
    }
    return raw;
  });
}
