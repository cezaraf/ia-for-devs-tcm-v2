import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError } from 'rxjs';
import { MESSAGES, toFailure } from './weather-messages';

function http(status: number, body: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: body });
}

describe('weather-messages', () => {
  it('TST-013 MESSAGES tem os textos exatos', () => {
    expect(MESSAGES.idle.text).toBe(
      'Digite o nome de uma cidade e pressione Enter para ver o clima atual.',
    );
    expect(MESSAGES.invalid.text).toBe('Informe o nome de uma cidade com 2 a 100 caracteres.');
    expect(MESSAGES['not-found'].text).toBe(
      'Nenhuma cidade encontrada com esse nome. Confira a grafia e busque novamente.',
    );
    expect(MESSAGES.unavailable.text).toBe('O serviço de clima está indisponível no momento.');
    expect(MESSAGES.timeout.text).toBe('O serviço de clima demorou demais para responder.');
    expect(MESSAGES['geo-denied'].text).toBe(
      'Permissão de localização negada. Busque pelo nome da cidade.',
    );
    expect(MESSAGES['geo-unavailable'].text).toBe(
      'Não foi possível obter sua localização. Tente novamente ou busque pelo nome da cidade.',
    );
  });

  it('TST-013 SCN-022 CITY_NOT_FOUND → not-found sem retry', () => {
    const f = toFailure(http(404, { code: 'CITY_NOT_FOUND', message: 'x' }));
    expect(f).toMatchObject({ state: 'not-found', kind: 'error', retryable: false });
  });

  it('TST-013 INVALID_CITY_NAME → invalid sem retry', () => {
    const f = toFailure(http(400, { code: 'INVALID_CITY_NAME', message: 'x' }));
    expect(f).toMatchObject({ state: 'invalid', retryable: false });
  });

  it('TST-013 INVALID_COORDINATES → unavailable sem retry (BR-004)', () => {
    const f = toFailure(http(400, { code: 'INVALID_COORDINATES', message: 'x' }));
    expect(f).toMatchObject({ state: 'unavailable', retryable: false });
  });

  it('TST-013 UPSTREAM_ERROR → unavailable com retry', () => {
    const f = toFailure(http(502, { code: 'UPSTREAM_ERROR', message: 'x' }));
    expect(f).toMatchObject({ state: 'unavailable', retryable: true });
  });

  it('TST-013 UPSTREAM_TIMEOUT → timeout com retry', () => {
    const f = toFailure(http(504, { code: 'UPSTREAM_TIMEOUT', message: 'x' }));
    expect(f).toMatchObject({ state: 'timeout', kind: 'error', retryable: true });
  });

  it('TST-013 status 0 → unavailable com retry', () => {
    expect(toFailure(http(0, new ProgressEvent('error')))).toMatchObject({
      state: 'unavailable',
      retryable: true,
    });
  });

  it('TST-013 5xx sem corpo → unavailable com retry', () => {
    expect(toFailure(http(500, null))).toMatchObject({ state: 'unavailable', retryable: true });
    expect(toFailure(http(503, ''))).toMatchObject({ state: 'unavailable', retryable: true });
  });

  it('TST-013 TimeoutError do rxjs → timeout com retry', () => {
    expect(toFailure(new TimeoutError())).toMatchObject({ state: 'timeout', retryable: true });
  });

  it('TST-013 SCN-023 geolocalização code 1 → geo-denied sem retry', () => {
    expect(toFailure({ code: 1 })).toMatchObject({
      state: 'geo-denied',
      kind: 'info',
      retryable: false,
    });
  });

  it.each([2, 3])('TST-013 SCN-023 geolocalização code %i → geo-unavailable com retry', (code) => {
    expect(toFailure({ code })).toMatchObject({
      state: 'geo-unavailable',
      kind: 'info',
      retryable: true,
    });
  });

  it('TST-013 erro desconhecido → unavailable', () => {
    expect(toFailure(new Error('boom'))).toMatchObject({ state: 'unavailable', retryable: true });
  });

  it('TST-013 nunca usa error.message do backend', () => {
    const secret = 'java.lang.NullPointerException at Foo.bar';
    const f = toFailure(http(502, { code: 'UPSTREAM_ERROR', message: secret }));
    expect(JSON.stringify(f)).not.toContain(secret);
    expect(f.text).toBe(MESSAGES.unavailable.text);
  });
});
