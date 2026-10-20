import { TestBed } from '@angular/core/testing';
import { WeatherSkeleton } from './weather-skeleton';

describe('WeatherSkeleton', () => {
  it('SCN-028 blocos com aria-hidden e texto oculto "Carregando o clima…"', async () => {
    const fixture = TestBed.createComponent(WeatherSkeleton);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.card')?.getAttribute('aria-hidden')).toBe('true');
    const hidden = el.querySelector('.visually-hidden');
    expect(hidden?.textContent).toBe('Carregando o clima…');
    expect(hidden?.closest('[aria-hidden="true"]')).toBeNull();
  });
});
