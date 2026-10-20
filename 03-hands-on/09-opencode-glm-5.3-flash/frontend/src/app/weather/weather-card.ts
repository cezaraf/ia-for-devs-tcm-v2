import { Component, computed, input } from '@angular/core';

import { Weather } from './weather-api';

/** Painel do clima atual: rótulo do local, badge dia/noite, temperatura e detalhes. */
@Component({
  selector: 'app-weather-card',
  templateUrl: './weather-card.html',
  styleUrl: './weather-card.css',
})
export class WeatherCard {
  readonly weather = input.required<Weather>();
  readonly label = input.required<string>();

  protected readonly badge = computed(() => (this.weather().isDay ? '[ DIA ]' : '[ NOITE ]'));

  protected readonly temperature = computed(() => `${Math.round(this.weather().temperature)}°C`);

  protected readonly details = computed(() => {
    const weather = this.weather();
    return [
      `[+] sensação  ${Math.round(weather.apparentTemperature)}°C`,
      `[+] umidade  ${Math.round(weather.relativeHumidity)}%`,
      `[+] vento  ${Math.round(weather.windSpeed)} km/h ${weather.windDirectionCardinal}`,
      `[+] precipitação  ${weather.precipitation.toFixed(1)} mm`,
    ];
  });

  /** "2026-10-13T14:15" → "14:15" (data-hora local informada pelo backend). */
  protected readonly measuredAt = computed(() => `medido às ${this.weather().observedAt.slice(11, 16)} (hora local)`);
}
