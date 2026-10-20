import { TestBed } from '@angular/core/testing';
import { CityOptions } from './city-options';
import { City } from './weather-api';

function city(id: number, over: Partial<City> = {}): City {
  return {
    id,
    name: 'São José',
    region: 'Santa Catarina',
    subregion: null,
    country: 'Brasil',
    countryCode: 'BR',
    latitude: -27.6,
    longitude: -48.6,
    timezone: 'America/Sao_Paulo',
    ...over,
  };
}

describe('CityOptions', () => {
  async function setup(cities: City[]) {
    const fixture = TestBed.createComponent(CityOptions);
    document.body.appendChild(fixture.nativeElement);
    fixture.componentRef.setInput('cities', cities);
    await fixture.whenStable();
    const selected = vi.fn();
    fixture.componentInstance.selected.subscribe(selected);
    const el: HTMLElement = fixture.nativeElement;
    return { fixture, el, selected, buttons: Array.from(el.querySelectorAll('button')) };
  }

  afterEach(() => document.body.replaceChildren());

  it('SCN-026 título com N e texto "Nome — Região, País"', async () => {
    const { el, buttons } = await setup([city(1), city(2, { name: 'Recife', region: 'Pernambuco' })]);
    expect(el.querySelector('.title')?.textContent).toBe('Encontramos 2 cidades. Escolha uma:');
    expect(buttons.map((b) => b.textContent?.trim())).toEqual([
      'São José — Santa Catarina, Brasil',
      'Recife — Pernambuco, Brasil',
    ]);
  });

  it('SCN-026 sub-região só aparece em colisão', async () => {
    const { buttons } = await setup([
      city(1, { subregion: 'Florianópolis' }),
      city(2, { subregion: 'Palhoça' }),
      city(3, { name: 'Recife', region: 'Pernambuco', subregion: 'Recife' }),
    ]);
    expect(buttons.map((b) => b.textContent?.trim())).toEqual([
      'São José — Santa Catarina, Brasil (Florianópolis)',
      'São José — Santa Catarina, Brasil (Palhoça)',
      'Recife — Pernambuco, Brasil',
    ]);
  });

  it('SCN-026 colisão com sub-região nula não acrescenta sufixo', async () => {
    const { buttons } = await setup([city(1), city(2, { subregion: 'Palhoça' })]);
    expect(buttons[0].textContent?.trim()).toBe('São José — Santa Catarina, Brasil');
    expect(buttons[1].textContent?.trim()).toBe('São José — Santa Catarina, Brasil (Palhoça)');
  });

  it('SCN-026 clique emite selected com a cidade', async () => {
    const cities = [city(1, { subregion: 'A' }), city(2, { subregion: 'B' })];
    const { buttons, selected } = await setup(cities);
    buttons[1].click();
    expect(selected).toHaveBeenCalledExactlyOnceWith(cities[1]);
  });

  it('SCN-026 foco no 1º botão', async () => {
    const { buttons } = await setup([city(1), city(2)]);
    expect(document.activeElement).toBe(buttons[0]);
  });
});
