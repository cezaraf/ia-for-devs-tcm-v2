import { Component, ElementRef, input, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-city-search-form',
  imports: [],
  templateUrl: './city-search-form.html',
  styleUrl: './city-search-form.css',
})
export class CitySearchForm {
  readonly geolocationAvailable = input.required<boolean>();
  readonly busy = input.required<boolean>();
  readonly invalidMessage = input.required<string | null>();

  readonly search = output<string>();
  readonly useLocation = output<void>();

  private readonly field = viewChild.required<ElementRef<HTMLInputElement>>('field');

  focusInput(): void {
    this.field().nativeElement.focus();
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.search.emit(this.field().nativeElement.value);
  }
}
