import { Component, computed, input } from '@angular/core';
import { Weather } from './weather-api';

@Component({
  selector: 'app-weather-card',
  imports: [],
  templateUrl: './weather-card.html',
  styleUrl: './weather-card.css',
})
export class WeatherCard {
  readonly weather = input.required<Weather>();
  readonly label = input.required<string>();

  protected readonly temperature = computed(() => Math.round(this.weather().temperature));
  protected readonly apparent = computed(() => Math.round(this.weather().apparentTemperature));
  protected readonly precipitation = computed(() => this.weather().precipitation.toFixed(1));
  protected readonly wind = computed(() => {
    const w = this.weather();
    return `${Math.round(w.windSpeed)} km/h ${w.windDirectionCardinal}`;
  });
  // observedAt já vem na hora local do local consultado (sem offset): não passar por Date.
  protected readonly time = computed(() => this.weather().observedAt.slice(11, 16));
}
