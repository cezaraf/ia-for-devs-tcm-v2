import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { WeatherResponse } from './weather.model';
import { WeatherService } from './weather.service';

@Component({
  selector: 'app-weather-panel',
  imports: [FormsModule],
  templateUrl: './weather-panel.html',
})
export class WeatherPanel {
  private readonly weatherService = inject(WeatherService);

  protected readonly city = signal('');
  protected readonly weather = signal<WeatherResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly geoAvailable = signal(false);
  protected readonly geoLoading = signal(false);

  // Descarta respostas de buscas antigas quando o usuário dispara outra antes da anterior voltar.
  private requestId = 0;

  constructor() {
    // Só o navegador tem geolocation (a página também é renderizada no servidor).
    if (typeof navigator !== 'undefined' && 'geolocation' in navigator) {
      this.geoAvailable.set(true);
    }
  }

  protected search(): void {
    const city = this.city().trim();
    if (!city) {
      this.error.set('Digite o nome de uma cidade.');
      return;
    }
    this.load(this.weatherService.byCity(city));
  }

  protected useMyLocation(): void {
    this.geoLoading.set(true);
    this.error.set(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.geoLoading.set(false);
        this.load(this.weatherService.byCoordinates(pos.coords.latitude, pos.coords.longitude));
      },
      (err) => {
        this.geoLoading.set(false);
        this.error.set(
          err.code === err.PERMISSION_DENIED
            ? 'Permissão de localização negada. Digite uma cidade.'
            : 'Não foi possível obter sua localização. Digite uma cidade.',
        );
      },
      { timeout: 10_000, maximumAge: 5 * 60_000 },
    );
  }

  private load(request: Observable<WeatherResponse>): void {
    const id = ++this.requestId;
    this.loading.set(true);
    this.error.set(null);
    request.subscribe({
      next: (data) => {
        if (id !== this.requestId) return;
        this.weather.set(data);
        this.loading.set(false);
        // Busca por coordenadas não traz nome; preenche o campo se vier.
        if (data.location.name) this.city.set(data.location.name);
      },
      error: (err: HttpErrorResponse) => {
        if (id !== this.requestId) return;
        this.loading.set(false);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    if (err.status === 404) return 'Cidade não encontrada. Confira o nome e tente de novo.';
    if (err.status === 400) return err.error?.message ?? 'Pedido inválido.';
    if (err.status === 502) return 'O serviço de clima está indisponível no momento. Tente novamente.';
    return 'Não foi possível buscar o clima. Tente novamente.';
  }

  protected placeLabel(w: WeatherResponse): string {
    const { name, region, country } = w.location;
    if (!name) return 'Sua localização';
    return [name, region, country].filter(Boolean).join(', ');
  }

  protected compass(deg: number): string {
    const dirs = ['N', 'NE', 'L', 'SE', 'S', 'SO', 'O', 'NO'];
    return dirs[Math.round(deg / 45) % 8];
  }
}
