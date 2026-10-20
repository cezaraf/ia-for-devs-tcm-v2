import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { Weather } from './weather.models';

@Component({
  selector: 'app-weather-card',
  templateUrl: './weather-card.html',
  styleUrl: './weather-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeatherCard {
  readonly weather = input.required<Weather>();
  readonly label = input.required<string>();

  protected readonly badge = computed(() => (this.weather().isDay ? '[ DIA ]' : '[ NOITE ]'));
  protected readonly temperature = computed(() => Math.round(this.weather().temperature));
  /** `observedAt` já é hora local sem offset: só fatiar, sem Date (evita deslocar fuso). */
  protected readonly time = computed(() => this.weather().observedAt.substring(11, 16));

  protected readonly details = computed(() => {
    const w = this.weather();
    return [
      { key: 'apparent', text: `[+] sensação  ${Math.round(w.apparentTemperature)}°C` },
      { key: 'humidity', text: `[+] umidade  ${w.relativeHumidity}%` },
      {
        key: 'wind',
        text: `[+] vento  ${Math.round(w.windSpeed)} km/h ${w.windDirectionCardinal}`,
      },
      { key: 'precipitation', text: `[+] precipitação  ${w.precipitation.toFixed(1)} mm` },
    ];
  });
}
