import { Component, ElementRef, afterNextRender, input, output, signal, viewChild } from '@angular/core';

/** Formulário de busca: label "cidade", input, botão "buscar" e "[usar minha localização]". */
@Component({
  selector: 'app-city-search-form',
  templateUrl: './city-search-form.html',
  styleUrl: './city-search-form.css',
})
export class CitySearchForm {
  /** Bloqueia os botões enquanto `searching`/`loading`. */
  readonly busy = input(false);

  readonly search = output<string>();
  readonly locate = output<void>();

  protected readonly city = signal('');
  protected readonly canLocate = signal(false);

  private readonly cityInput = viewChild.required<ElementRef<HTMLInputElement>>('cityInput');

  constructor() {
    // navigator.geolocation só pode ser verificado no navegador (nunca no SSR).
    afterNextRender(() => {
      this.canLocate.set(typeof navigator !== 'undefined' && 'geolocation' in navigator);
    });
  }

  protected onInput(event: Event): void {
    this.city.set((event.target as HTMLInputElement).value);
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.search.emit(this.city().trim());
  }

  protected onLocate(): void {
    this.locate.emit();
  }

  /** Devolve o foco ao input (usado pelo painel em erros sem retry). */
  focusInput(): void {
    this.cityInput().nativeElement.focus();
  }
}
