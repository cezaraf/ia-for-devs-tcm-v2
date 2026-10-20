import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError } from 'rxjs';

export type FailureState =
  | 'invalid'
  | 'not-found'
  | 'unavailable'
  | 'timeout'
  | 'geo-denied'
  | 'geo-unavailable';

export interface PanelMessage {
  kind: 'info' | 'error';
  text: string;
  retryable: boolean;
}

export const MESSAGES = {
  idle: {
    kind: 'info',
    text: 'Digite o nome de uma cidade e pressione Enter para ver o clima atual.',
    retryable: false,
  },
  invalid: {
    kind: 'error',
    text: 'Informe o nome de uma cidade com 2 a 100 caracteres.',
    retryable: false,
  },
  'not-found': {
    kind: 'error',
    text: 'Nenhuma cidade encontrada com esse nome. Confira a grafia e busque novamente.',
    retryable: false,
  },
  unavailable: {
    kind: 'error',
    text: 'O serviço de clima está indisponível no momento.',
    retryable: true,
  },
  timeout: {
    kind: 'error',
    text: 'O serviço de clima demorou demais para responder.',
    retryable: true,
  },
  'geo-denied': {
    kind: 'info',
    text: 'Permissão de localização negada. Busque pelo nome da cidade.',
    retryable: false,
  },
  'geo-unavailable': {
    kind: 'info',
    text: 'Não foi possível obter sua localização. Tente novamente ou busque pelo nome da cidade.',
    retryable: true,
  },
} as const satisfies Record<string, PanelMessage>;

export const LOADING_TEXT = 'Carregando o clima…';
export const LOCATION_LABEL = 'Sua localização';
export const RETRY_LABEL = 'Tentar novamente';

export function choosingTitle(count: number): string {
  return `Encontramos ${count} cidades. Escolha uma:`;
}

export interface Failure extends PanelMessage {
  state: FailureState;
}

function failure(state: FailureState, retryable?: boolean): Failure {
  const message: PanelMessage = MESSAGES[state];
  return { state, ...message, retryable: retryable ?? message.retryable };
}

// Nunca usa error.message do backend: só o código/status escolhe o texto de MESSAGES.
export function toFailure(error: unknown): Failure {
  if (error instanceof TimeoutError) {
    return failure('timeout');
  }
  if (error instanceof HttpErrorResponse) {
    const code = (error.error as { code?: unknown } | null)?.code;
    switch (code) {
      case 'CITY_NOT_FOUND':
        return failure('not-found');
      case 'INVALID_CITY_NAME':
        return failure('invalid');
      case 'INVALID_COORDINATES':
        return failure('unavailable', false);
      case 'UPSTREAM_TIMEOUT':
        return failure('timeout');
      case 'UPSTREAM_ERROR':
        return failure('unavailable');
    }
    return failure('unavailable');
  }
  const geoCode = (error as { code?: unknown } | null)?.code;
  if (geoCode === 1) {
    return failure('geo-denied');
  }
  if (geoCode === 2 || geoCode === 3) {
    return failure('geo-unavailable');
  }
  return failure('unavailable');
}
