import { Component, ElementRef, afterNextRender, input, output, viewChildren } from '@angular/core';
import { City } from './weather-api';

@Component({
  selector: 'app-city-list',
  imports: [],
  templateUrl: './city-list.html',
  styleUrl: './city-list.css',
})
export class CityList {
  readonly cities = input.required<City[]>();
  readonly selected = output<City>();

  private readonly items = viewChildren<ElementRef<HTMLButtonElement>>('item');

  constructor() {
    afterNextRender(() => this.items()[0]?.nativeElement.focus());
  }

  protected label(city: City): string {
    const place = [city.admin1, city.country].filter(Boolean).join(', ');
    return place ? `${city.name} — ${place}` : city.name;
  }
}
