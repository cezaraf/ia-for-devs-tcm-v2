import { HttpErrorResponse } from '@angular/common/http';

export interface City {
  id: number;
  name: string;
  admin1: string | null;
  country: string | null;
  countryCode: string | null;
  latitude: number;
  longitude: number;
}

export interface Weather {
  latitude: number;
  longitude: number;
  timezone: string;
  observedAt: string;
  temperature: number;
  apparentTemperature: number;
  relativeHumidity: number;
  precipitation: number;
  windSpeed: number;
  windDirection: number;
  windDirectionCardinal: string;
  weatherCode: number;
  condition: string;
  isDay: boolean;
}

export interface PanelError {
  message: string;
  retryable: boolean;
  focusInput: boolean;
}

export const GEOLOCATION_LABEL = 'Sua localização';

export const MIN_NAME_LENGTH = 2;

export const NAME_TOO_SHORT: PanelError = {
  message: 'Digite ao menos 2 caracteres.',
  retryable: false,
  focusInput: true,
};

export const CITY_NOT_FOUND: PanelError = {
  message: 'Cidade não encontrada. Verifique o nome e tente novamente.',
  retryable: false,
  focusInput: true,
};

const INVALID_REQUEST: PanelError = {
  message: 'Requisição inválida.',
  retryable: false,
  focusInput: false,
};

const UPSTREAM_ERROR: PanelError = {
  message: 'O serviço de clima está indisponível no momento.',
  retryable: true,
  focusInput: false,
};

const UPSTREAM_TIMEOUT: PanelError = {
  message: 'O serviço de clima demorou para responder.',
  retryable: true,
  focusInput: false,
};

const CONNECTION_ERROR: PanelError = {
  message: 'Não foi possível conectar ao servidor.',
  retryable: true,
  focusInput: false,
};

export const GEOLOCATION_DENIED: PanelError = {
  message: 'Permissão de localização negada. Digite uma cidade.',
  retryable: false,
  focusInput: true,
};

export const GEOLOCATION_UNAVAILABLE: PanelError = {
  message: 'Não foi possível obter sua localização.',
  retryable: true,
  focusInput: false,
};

/**
 * Mapeia pelo `code` do corpo, não só pelo status: quando o backend está fora, o proxy responde
 * 502/503/504 sem JSON e isso deve virar "não foi possível conectar".
 */
export function toPanelError(error: unknown): PanelError {
  if (!(error instanceof HttpErrorResponse)) {
    return CONNECTION_ERROR;
  }
  const code = (error.error as { code?: unknown } | null)?.code;
  if (error.status === 400) {
    return INVALID_REQUEST;
  }
  if (error.status === 404 && code === 'CITY_NOT_FOUND') {
    return CITY_NOT_FOUND;
  }
  if (error.status === 502 && code === 'UPSTREAM_ERROR') {
    return UPSTREAM_ERROR;
  }
  if (error.status === 504 && code === 'UPSTREAM_TIMEOUT') {
    return UPSTREAM_TIMEOUT;
  }
  return CONNECTION_ERROR;
}

/** `<nome>, <estado>, <país>`, omitindo partes nulas. */
export function cityLabel(city: City): string {
  return [city.name, city.admin1, city.country].filter(Boolean).join(', ');
}
