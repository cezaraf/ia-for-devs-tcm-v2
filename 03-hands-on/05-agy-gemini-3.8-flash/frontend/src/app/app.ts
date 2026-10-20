import { Component, OnInit, inject, signal, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WeatherService } from './services/weather.service';
import { WeatherResponse } from './models/weather.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly weatherService = inject(WeatherService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly isBrowser = isPlatformBrowser(this.platformId);

  // Estados com Signals
  cityName = signal<string>('Salvador');
  weatherData = signal<WeatherResponse | null>(null);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  geolocationLoading = signal<boolean>(false);

  // Cidades rápidas para sugestão
  readonly quickCities = [
    'Salvador',
    'São Paulo',
    'Rio de Janeiro',
    'Brasília',
    'Curitiba',
    'Recife',
    'Lisboa',
    'Nova York'
  ];

  ngOnInit(): void {
    if (this.isBrowser) {
      // Tenta obter localização automaticamente no início se permitido
      this.tryAutomaticGeolocation();
    }
  }

  tryAutomaticGeolocation(): void {
    if ('geolocation' in navigator) {
      this.geolocationLoading.set(true);
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.geolocationLoading.set(false);
          this.fetchWeatherByCoords(position.coords.latitude, position.coords.longitude);
        },
        (error) => {
          this.geolocationLoading.set(false);
          // Se o usuário recusar ou falhar, carrega a cidade padrão
          this.fetchWeatherByCity(this.cityName());
        },
        { timeout: 8000, enableHighAccuracy: false }
      );
    } else {
      this.fetchWeatherByCity(this.cityName());
    }
  }

  onSearch(): void {
    const query = this.cityName().trim();
    if (!query) {
      this.errorMessage.set('Por favor, digite o nome de uma cidade.');
      return;
    }
    this.fetchWeatherByCity(query);
  }

  selectCity(city: string): void {
    this.cityName.set(city);
    this.fetchWeatherByCity(city);
  }

  useCurrentLocation(): void {
    if (!this.isBrowser || !('geolocation' in navigator)) {
      this.errorMessage.set('Geolocalização não é suportada pelo seu navegador.');
      return;
    }

    this.geolocationLoading.set(true);
    this.errorMessage.set(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.geolocationLoading.set(false);
        this.fetchWeatherByCoords(position.coords.latitude, position.coords.longitude);
      },
      (error) => {
        this.geolocationLoading.set(false);
        let msg = 'Não foi possível obter sua localização.';
        if (error.code === error.PERMISSION_DENIED) {
          msg = 'Permissão de localização negada pelo navegador.';
        } else if (error.code === error.POSITION_UNAVAILABLE) {
          msg = 'Informações de localização indisponíveis no momento.';
        } else if (error.code === error.TIMEOUT) {
          msg = 'Tempo limite excedido ao buscar localização.';
        }
        this.errorMessage.set(msg);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  }

  private fetchWeatherByCity(city: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.weatherService.getWeatherByCity(city).subscribe({
      next: (data) => {
        this.weatherData.set(data);
        this.cityName.set(data.location.name);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg = err.error?.message || err.message || 'Erro ao carregar dados do clima. Tente novamente.';
        this.errorMessage.set(msg);
      }
    });
  }

  private fetchWeatherByCoords(lat: number, lon: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.weatherService.getWeatherByCoordinates(lat, lon).subscribe({
      next: (data) => {
        this.weatherData.set(data);
        if (data.location?.name) {
          this.cityName.set(data.location.name);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg = err.error?.message || err.message || 'Erro ao buscar clima para a sua localização.';
        this.errorMessage.set(msg);
      }
    });
  }

  formatDateLabel(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const parts = dateStr.split('-');
      if (parts.length === 3) {
        const date = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        const today = new Date();
        if (date.toDateString() === today.toDateString()) {
          return 'Hoje';
        }
        return date.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' });
      }
    } catch {
      // fallback
    }
    return dateStr;
  }
}
