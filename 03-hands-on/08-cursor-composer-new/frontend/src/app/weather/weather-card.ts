import { Component, computed, input } from '@angular/core';
import { City, Weather } from './weather-api';

@Component({
  selector: 'app-weather-card',
  templateUrl: './weather-card.html',
  styleUrl: './weather-card.css',
})
export class WeatherCard {
  readonly weather = input.required<Weather>();
  readonly city = input<City | null>(null);
  readonly useMyLocationLabel = input(false);

  protected readonly locationLabel = computed(() => {
    if (this.useMyLocationLabel()) {
      return 'Sua localização';
    }
    const city = this.city();
    if (!city) {
      return 'Sua localização';
    }
    const parts = [city.name];
    if (city.admin1) {
      parts.push(city.admin1);
    }
    if (city.country) {
      parts.push(city.country);
    }
    return parts.join(', ');
  });

  protected readonly temperature = computed(() => Math.round(this.weather().temperature));

  protected readonly apparent = computed(() => Math.round(this.weather().apparentTemperature));

  protected readonly precipitation = computed(() => this.weather().precipitation.toFixed(1));

  protected readonly observedTime = computed(() => {
    const raw = this.weather().observedAt;
    const timePart = raw.includes('T') ? raw.split('T')[1] : raw;
    return timePart.slice(0, 5);
  });

  protected readonly dayBadge = computed(() => (this.weather().isDay ? '[ DIA ]' : '[ NOITE ]'));
}
