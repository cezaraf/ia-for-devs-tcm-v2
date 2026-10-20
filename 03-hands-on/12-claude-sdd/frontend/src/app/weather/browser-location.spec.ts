import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { BrowserLocation } from './browser-location';

type Ok = (p: { coords: { latitude: number; longitude: number } }) => void;
type Err = (e: { code: number }) => void;

describe('BrowserLocation', () => {
  let getCurrentPosition: ReturnType<typeof vi.fn>;
  let ok: Ok;
  let err: Err;

  beforeEach(() => {
    getCurrentPosition = vi.fn((success: Ok, error: Err) => {
      ok = success;
      err = error;
    });
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } });
  });

  afterEach(() => vi.unstubAllGlobals());

  function create(platform: 'browser' | 'server'): BrowserLocation {
    TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: platform }] });
    return TestBed.inject(BrowserLocation);
  }

  it('TST-014 SCN-024 isAvailable é false no servidor', () => {
    expect(create('server').isAvailable()).toBe(false);
  });

  it('TST-014 isAvailable é true no navegador com geolocation', () => {
    expect(create('browser').isAvailable()).toBe(true);
  });

  it('TST-014 isAvailable é false no navegador sem geolocation', () => {
    vi.stubGlobal('navigator', {});
    expect(create('browser').isAvailable()).toBe(false);
  });

  it('TST-014 SCN-021 emite a posição, completa e usa as opções esperadas', () => {
    const values: unknown[] = [];
    let completed = false;
    create('browser')
      .getPosition()
      .subscribe({ next: (v) => values.push(v), complete: () => (completed = true) });

    expect(getCurrentPosition.mock.calls[0][2]).toEqual({
      enableHighAccuracy: false,
      timeout: 8000,
      maximumAge: 300000,
    });
    ok({ coords: { latitude: -8.05, longitude: -34.88 } });

    expect(values).toEqual([{ latitude: -8.05, longitude: -34.88 }]);
    expect(completed).toBe(true);
  });

  it('TST-014 SCN-023 erro emite { code }', () => {
    let error: unknown;
    create('browser')
      .getPosition()
      .subscribe({ error: (e) => (error = e) });

    err({ code: 1 });

    expect(error).toEqual({ code: 1 });
  });

  it('TST-014 não chama a API antes de assinar', () => {
    create('browser').getPosition();
    expect(getCurrentPosition).not.toHaveBeenCalled();
  });

  it('TST-014 callback tardio após unsubscribe é ignorado', () => {
    const next = vi.fn();
    const error = vi.fn();
    const sub = create('browser').getPosition().subscribe({ next, error });

    sub.unsubscribe();
    ok({ coords: { latitude: 1, longitude: 2 } });
    err({ code: 3 });

    expect(next).not.toHaveBeenCalled();
    expect(error).not.toHaveBeenCalled();
  });
});
