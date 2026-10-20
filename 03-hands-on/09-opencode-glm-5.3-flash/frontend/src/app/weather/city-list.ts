import { Component, ElementRef, afterNextRender, input, output, viewChild } from '@angular/core';

import { City } from './weather-api';

/**
 * Lista de cidades homônimas (até 10, 5 visíveis com scroll). Clique ou Enter
 * seleciona; ao entrar em `choosing`, o foco vai para o primeiro item.
 */
@Component({
  selector: 'app-city-list',
  templateUrl: './city-list.html',
  styleUrl: './city-list.css',
})
export class CityList {
  readonly cities = input.required<City[]>();

  readonly select = output<City>();

  private readonly firstItem = viewChild<ElementRef<HTMLButtonElement>>('firstItem');

  constructor() {
    afterNextRender(() => this.firstItem()?.nativeElement.focus());
  }

  protected label(city: City): string {
    const parts = [city.admin1, city.country].filter((part) => part !== null && part !== '');
    return parts.length > 0 ? `${city.name} — ${parts.join(', ')}` : city.name;
  }
}
