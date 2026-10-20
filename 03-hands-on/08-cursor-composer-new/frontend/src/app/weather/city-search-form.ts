import { Component, ElementRef, input, output, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-city-search-form',
  imports: [FormsModule],
  templateUrl: './city-search-form.html',
  styleUrl: './city-search-form.css',
})
export class CitySearchForm {
  readonly searching = input(false);
  readonly loading = input(false);
  readonly geolocationAvailable = input(false);

  readonly search = output<string>();
  readonly useLocation = output<void>();

  private readonly cityInput = viewChild<ElementRef<HTMLInputElement>>('cityInput');

  protected cityName = '';

  focusInput(): void {
    this.cityInput()?.nativeElement.focus();
  }

  protected disabled(): boolean {
    return this.searching() || this.loading();
  }

  protected onSubmit(): void {
    if (this.disabled()) {
      return;
    }
    this.search.emit(this.cityName.trim());
  }

  protected onUseLocation(): void {
    if (this.disabled()) {
      return;
    }
    this.useLocation.emit();
  }
}
