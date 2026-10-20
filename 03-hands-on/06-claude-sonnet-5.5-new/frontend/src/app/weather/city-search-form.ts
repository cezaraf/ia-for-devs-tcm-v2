import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  effect,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';

@Component({
  selector: 'app-city-search-form',
  templateUrl: './city-search-form.html',
  styleUrl: './city-search-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitySearchForm {
  readonly disabled = input(false);
  /** Contador: cada incremento pede o foco no input. */
  readonly focusRequest = input(0);

  readonly searchSubmitted = output<string>();
  readonly locateRequested = output<void>();

  protected readonly name = signal('');
  protected readonly geolocationSupported = signal(false);

  private readonly nameInput = viewChild.required<ElementRef<HTMLInputElement>>('nameInput');

  constructor() {
    afterNextRender(() => {
      this.geolocationSupported.set('geolocation' in navigator);
    });

    effect(() => {
      if (this.focusRequest() > 0) {
        this.nameInput().nativeElement.focus();
      }
    });
  }

  protected onInput(event: Event): void {
    this.name.set((event.target as HTMLInputElement).value);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.searchSubmitted.emit(this.name());
  }
}
