import { Component, ElementRef, afterNextRender, input, output, viewChildren } from '@angular/core';
import { CityResponse } from './weather-api';

@Component({
  selector: 'app-city-list',
  imports: [],
  templateUrl: './city-list.html',
  styleUrl: './city-list.css'
})
export class CityList {
  readonly cities = input.required<CityResponse[]>();
  readonly citySelected = output<CityResponse>();

  readonly buttons = viewChildren<ElementRef<HTMLButtonElement>>('cityBtn');

  constructor() {
    afterNextRender(() => {
      const btns = this.buttons();
      if (btns && btns.length > 0) {
        btns[0].nativeElement.focus();
      }
    });
  }

  formatCity(city: CityResponse): string {
    const locParts = [city.admin1, city.country].filter(Boolean).join(', ');
    return locParts ? `[+] ${city.name} — ${locParts}` : `[+] ${city.name}`;
  }

  selectCity(city: CityResponse): void {
    this.citySelected.emit(city);
  }
}
