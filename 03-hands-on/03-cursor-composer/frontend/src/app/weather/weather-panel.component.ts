import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { WeatherData } from './weather.models';
import { WeatherService } from './weather.service';

@Component({
  selector: 'app-weather-panel',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './weather-panel.component.html',
  styleUrl: './weather-panel.component.css',
})
export class WeatherPanelComponent implements OnInit {
  private readonly weatherService = inject(WeatherService);

  protected cityQuery = '';
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly weather = signal<WeatherData | null>(null);
  protected readonly geoHint = signal<string | null>(null);

  ngOnInit(): void {
    this.trySuggestCityFromGeolocation();
  }

  protected search(): void {
    const city = this.cityQuery.trim();
    if (!city) {
      this.error.set('Digite o nome de uma cidade.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.weatherService.getByCity(city).subscribe({
      next: (data) => {
        this.weather.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.weather.set(null);
        this.loading.set(false);
        this.error.set(this.messageFromError(err));
      },
    });
  }

  protected useMyLocation(): void {
    if (!navigator.geolocation) {
      this.error.set('Geolocalização não disponível neste navegador.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.geoHint.set(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;
        this.weatherService.getByCoordinates(latitude, longitude).subscribe({
          next: (data) => {
            this.weather.set(data);
            this.cityQuery = data.city;
            this.geoHint.set('Cidade sugerida pela sua localização.');
            this.loading.set(false);
          },
          error: (err) => {
            this.loading.set(false);
            this.error.set(this.messageFromError(err));
          },
        });
      },
      () => {
        this.loading.set(false);
        this.error.set('Não foi possível obter sua localização.');
      },
      { enableHighAccuracy: false, timeout: 10000 },
    );
  }

  private trySuggestCityFromGeolocation(): void {
    if (!navigator.geolocation) {
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.weatherService
          .getByCoordinates(position.coords.latitude, position.coords.longitude)
          .subscribe({
            next: (data) => {
              if (!this.cityQuery.trim()) {
                this.cityQuery = data.city;
                this.geoHint.set('Sugerimos a cidade mais próxima da sua localização.');
              }
            },
            error: () => {
              /* sugestão opcional — falha silenciosa */
            },
          });
      },
      () => {
        /* permissão negada ou indisponível */
      },
      { enableHighAccuracy: false, timeout: 8000 },
    );
  }

  private messageFromError(err: { error?: { message?: string }; message?: string; status?: number }): string {
    const body = err.error;
    if (body && typeof body.message === 'string') {
      return body.message;
    }
    if (err.status === 404) {
      return 'Cidade não encontrada.';
    }
    return 'Não foi possível carregar o clima. Tente novamente.';
  }
}
