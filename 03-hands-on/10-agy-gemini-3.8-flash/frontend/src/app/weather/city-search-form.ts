import { Component, ElementRef, PLATFORM_ID, inject, input, output, signal, viewChild } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-city-search-form',
  imports: [],
  templateUrl: './city-search-form.html',
  styleUrl: './city-search-form.css'
})
export class CitySearchForm {
  private readonly platformId = inject(PLATFORM_ID);

  readonly disabled = input<boolean>(false);
  readonly search = output<string>();
  readonly useLocation = output<void>();

  readonly query = signal<string>('');
  readonly hasGeolocation = signal<boolean>(false);

  readonly cityInput = viewChild<ElementRef<HTMLInputElement>>('cityInput');

  constructor() {
    if (isPlatformBrowser(this.platformId) && typeof navigator !== 'undefined' && !!navigator.geolocation) {
      this.hasGeolocation.set(true);
    }
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if (this.disabled()) {
      return;
    }
    this.search.emit(this.query());
  }

  onInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.query.set(val);
  }

  onLocationClick(): void {
    if (this.disabled()) {
      return;
    }
    this.useLocation.emit();
  }

  focusInput(): void {
    this.cityInput()?.nativeElement.focus();
  }
}
