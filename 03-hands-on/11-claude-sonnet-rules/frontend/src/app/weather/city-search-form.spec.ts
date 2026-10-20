import { TestBed } from '@angular/core/testing';
import { CitySearchForm } from './city-search-form';

describe('CitySearchForm', () => {
  async function create(inputs: Record<string, unknown> = {}) {
    const fixture = TestBed.createComponent(CitySearchForm);
    for (const [k, v] of Object.entries(inputs)) {
      fixture.componentRef.setInput(k, v);
    }
    await fixture.whenStable();
    return fixture;
  }

  it('tem label visível "cidade" ligada ao input', async () => {
    const el = (await create()).nativeElement as HTMLElement;
    const label = el.querySelector('label')!;

    expect(label.textContent).toBe('cidade');
    expect(label.htmlFor).toBe(el.querySelector('input')!.id);
  });

  it('emite o texto digitado ao enviar o formulário e impede o reload', async () => {
    const fixture = await create();
    const spy = vi.fn();
    fixture.componentInstance.citySearch.subscribe(spy);
    const el = fixture.nativeElement as HTMLElement;
    el.querySelector('input')!.value = 'Curitiba';

    const event = new Event('submit', { cancelable: true });
    el.querySelector('form')!.dispatchEvent(event);

    expect(spy).toHaveBeenCalledWith('Curitiba');
    expect(event.defaultPrevented).toBe(true);
  });

  it('esconde "[usar minha localização]" sem geolocalização e emite locate quando disponível', async () => {
    const hidden = await create();
    expect((hidden.nativeElement as HTMLElement).querySelectorAll('button').length).toBe(1);

    const fixture = await create({ geolocationAvailable: true });
    const spy = vi.fn();
    fixture.componentInstance.locate.subscribe(spy);
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');

    expect(buttons[1].textContent?.trim()).toBe('[usar minha localização]');
    buttons[1].click();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('desabilita os botões (não o input) quando ocupado', async () => {
    const fixture = await create({ geolocationAvailable: true, busy: true });
    const el = fixture.nativeElement as HTMLElement;

    el.querySelectorAll('button').forEach((b) => expect(b.disabled).toBe(true));
    expect(el.querySelector('input')!.disabled).toBe(false);
  });

  it('focusInput foca o input', async () => {
    const fixture = await create();
    document.body.appendChild(fixture.nativeElement);

    fixture.componentInstance.focusInput();

    expect(document.activeElement).toBe(
      (fixture.nativeElement as HTMLElement).querySelector('input'),
    );
    fixture.nativeElement.remove();
  });
});
