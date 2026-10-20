import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  inject,
  input,
  output,
} from '@angular/core';

import { City } from './weather.models';

@Component({
  selector: 'app-city-list',
  templateUrl: './city-list.html',
  styleUrl: './city-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CityList {
  readonly cities = input.required<City[]>();
  readonly citySelected = output<City>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    afterNextRender(() => {
      this.host.nativeElement.querySelector('button')?.focus();
    });
  }

  protected describe(city: City): string {
    const place = [city.admin1, city.country].filter(Boolean).join(', ');
    return place ? `[+] ${city.name} — ${place}` : `[+] ${city.name}`;
  }
}
