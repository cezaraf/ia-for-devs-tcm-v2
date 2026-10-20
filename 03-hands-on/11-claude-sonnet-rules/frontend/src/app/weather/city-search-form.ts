import { Component, ElementRef, input, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-city-search-form',
  imports: [],
  templateUrl: './city-search-form.html',
  styleUrl: './city-search-form.css',
})
export class CitySearchForm {
  readonly busy = input(false);
  readonly geolocationAvailable = input(false);
  readonly citySearch = output<string>();
  readonly locate = output<void>();

  private readonly field = viewChild.required<ElementRef<HTMLInputElement>>('field');

  focusInput(): void {
    this.field().nativeElement.focus();
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.citySearch.emit(this.field().nativeElement.value);
  }
}
