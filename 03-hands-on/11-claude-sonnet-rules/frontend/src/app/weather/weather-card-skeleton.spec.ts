import { TestBed } from '@angular/core/testing';
import { WeatherCardSkeleton } from './weather-card-skeleton';

describe('WeatherCardSkeleton', () => {
  it('renderiza a grade de 4 detalhes escondida de leitores de tela', async () => {
    const fixture = TestBed.createComponent(WeatherCardSkeleton);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelectorAll('.detail').length).toBe(4);
    expect(el.querySelector('.card')?.getAttribute('aria-hidden')).toBe('true');
  });
});
