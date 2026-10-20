import { Component, ElementRef, afterNextRender, computed, input, output, viewChildren } from '@angular/core';
import { City } from './weather-api';
import { choosingTitle } from './weather-messages';

@Component({
  selector: 'app-city-options',
  imports: [],
  templateUrl: './city-options.html',
  styleUrl: './city-options.css',
})
export class CityOptions {
  readonly cities = input.required<City[]>();
  readonly selected = output<City>();

  private readonly buttons = viewChildren<ElementRef<HTMLButtonElement>>('option');

  protected readonly title = computed(() => choosingTitle(this.cities().length));

  protected readonly options = computed(() => {
    const cities = this.cities();
    const base = (c: City) => [c.name, c.region, c.country].join('|');
    return cities.map((city) => {
      const collides = cities.some((o) => o !== city && base(o) === base(city));
      const place = [city.region, city.country].filter(Boolean).join(', ');
      let label = place ? `${city.name} — ${place}` : city.name;
      if (collides && city.subregion) {
        label += ` (${city.subregion})`;
      }
      return { city, label };
    });
  });

  constructor() {
    afterNextRender(() => this.buttons()[0]?.nativeElement.focus());
  }
}
