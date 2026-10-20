import { TestBed } from '@angular/core/testing';
import { CitySearchForm } from './city-search-form';

describe('CitySearchForm', () => {
  async function setup(inputs: { geo?: boolean; busy?: boolean; invalid?: string | null } = {}) {
    const fixture = TestBed.createComponent(CitySearchForm);
    fixture.componentRef.setInput('geolocationAvailable', inputs.geo ?? false);
    fixture.componentRef.setInput('busy', inputs.busy ?? false);
    fixture.componentRef.setInput('invalidMessage', inputs.invalid ?? null);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    const search = vi.fn();
    const useLocation = vi.fn();
    fixture.componentInstance.search.subscribe(search);
    fixture.componentInstance.useLocation.subscribe(useLocation);
    return { fixture, el, search, useLocation, input: el.querySelector('input')! };
  }

  it('SCN-025 digitar não emite e o submit emite o texto bruto', async () => {
    const { el, input, search } = await setup();
    input.value = '  Recife ';
    input.dispatchEvent(new Event('input'));
    expect(search).not.toHaveBeenCalled();
    el.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    expect(search).toHaveBeenCalledExactlyOnceWith('  Recife ');
  });

  it('SCN-025 clicar em Buscar emite o texto bruto', async () => {
    const { el, input, search } = await setup();
    input.value = 'Recife';
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    expect(search).toHaveBeenCalledExactlyOnceWith('Recife');
  });

  it('SCN-025 label, input sem maxlength e sem botão de localização sem geolocalização', async () => {
    const { el, input } = await setup({ geo: false });
    expect(el.querySelector('label[for="city-name"]')?.textContent).toBe('Cidade');
    expect(input.id).toBe('city-name');
    expect(input.hasAttribute('maxlength')).toBe(false);
    expect(el.textContent).not.toContain('Usar minha localização');
  });

  it('SCN-025 botão de localização aparece e emite useLocation', async () => {
    const { el, useLocation } = await setup({ geo: true });
    const button = el.querySelector<HTMLButtonElement>('button[type="button"]')!;
    expect(button.textContent?.trim()).toBe('Usar minha localização');
    button.click();
    expect(useLocation).toHaveBeenCalledTimes(1);
  });

  it('SCN-025 mensagem inválida tem role alert, aria-invalid e aria-describedby', async () => {
    const { el, input } = await setup({ invalid: 'Informe o nome.' });
    const alert = el.querySelector('[role="alert"]')!;
    expect(alert.textContent).toBe('Informe o nome.');
    expect(alert.id).toBe('city-name-error');
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe('city-name-error');
  });

  it('SCN-025 sem mensagem não há alert nem atributos de erro', async () => {
    const { el, input } = await setup();
    expect(el.querySelector('[role="alert"]')).toBeNull();
    expect(input.hasAttribute('aria-invalid')).toBe(false);
    expect(input.hasAttribute('aria-describedby')).toBe(false);
  });

  it('SCN-025 focusInput foca o campo', async () => {
    const { fixture, input } = await setup();
    document.body.appendChild(fixture.nativeElement);
    fixture.componentInstance.focusInput();
    expect(document.activeElement).toBe(input);
    fixture.nativeElement.remove();
  });

  it('SCN-025 botões continuam habilitados com busy=true', async () => {
    const { el } = await setup({ geo: true, busy: true });
    const buttons = Array.from(el.querySelectorAll('button'));
    expect(buttons).toHaveLength(2);
    expect(buttons.every((b) => !b.disabled)).toBe(true);
  });
});
