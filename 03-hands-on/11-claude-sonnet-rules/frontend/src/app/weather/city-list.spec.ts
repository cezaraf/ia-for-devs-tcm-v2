import { TestBed } from '@angular/core/testing';
import { CityList } from './city-list';
import { City } from './weather-api';

const city = (id: number, name: string, admin1: string | null, country: string | null): City => ({
  id,
  name,
  admin1,
  country,
  countryCode: null,
  latitude: 0,
  longitude: 0,
});

describe('CityList', () => {
  it('renderiza uma linha por cidade omitindo partes nulas e foca o primeiro item', async () => {
    const fixture = TestBed.createComponent(CityList);
    fixture.componentRef.setInput('cities', [
      city(1, 'Santa Cruz', 'Bahia', 'Brasil'),
      city(2, 'Santa Cruz', null, 'Bolívia'),
      city(3, 'Santa Cruz', 'Cabo Verde', null),
      city(4, 'Santa Cruz', null, null),
    ]);
    document.body.appendChild(fixture.nativeElement);
    await fixture.whenStable();
    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));

    expect(buttons.map((b) => b.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
      '[+] Santa Cruz — Bahia, Brasil',
      '[+] Santa Cruz — Bolívia',
      '[+] Santa Cruz — Cabo Verde',
      '[+] Santa Cruz',
    ]);
    expect(document.activeElement).toBe(buttons[0]);
    fixture.nativeElement.remove();
  });

  it('emite a cidade escolhida ao clicar', async () => {
    const fixture = TestBed.createComponent(CityList);
    const cities = [city(1, 'A', null, null), city(2, 'B', null, null)];
    fixture.componentRef.setInput('cities', cities);
    const spy = vi.fn();
    fixture.componentInstance.selected.subscribe(spy);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement).querySelectorAll('button')[1].click();

    expect(spy).toHaveBeenCalledWith(cities[1]);
  });
});
