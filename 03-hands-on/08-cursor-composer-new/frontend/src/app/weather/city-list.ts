import { AfterViewInit, Component, ElementRef, input, output, viewChild } from '@angular/core';
import { City } from './weather-api';

@Component({
  selector: 'app-city-list',
  templateUrl: './city-list.html',
  styleUrl: './city-list.css',
})
export class CityList implements AfterViewInit {
  readonly cities = input.required<City[]>();
  readonly selectCity = output<City>();

  private readonly listRef = viewChild<ElementRef<HTMLElement>>('list');

  ngAfterViewInit(): void {
    const first = this.listRef()?.nativeElement.querySelector('button');
    first?.focus();
  }

  protected formatCity(city: City): string {
    const suffix = [city.admin1, city.country].filter(Boolean).join(', ');
    if (suffix) {
      return `[+] ${city.name} — ${suffix}`;
    }
    return `[+] ${city.name}`;
  }

  protected onSelect(city: City): void {
    this.selectCity.emit(city);
  }
}
