import { TestBed } from '@angular/core/testing';
import { ErrorMessage } from './error-message';

describe('ErrorMessage', () => {
  function create(retryable: boolean) {
    const fixture = TestBed.createComponent(ErrorMessage);
    fixture.componentRef.setInput('message', 'Falhou.');
    fixture.componentRef.setInput('retryable', retryable);
    return fixture;
  }

  it('mostra o marcador [x] e a mensagem com role alert', async () => {
    const fixture = create(false);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('[x] Falhou.');
    expect(el.querySelector('button')).toBeNull();
  });

  it('emite retry ao clicar em "tentar novamente"', async () => {
    const fixture = create(true);
    const spy = vi.fn();
    fixture.componentInstance.retry.subscribe(spy);
    await fixture.whenStable();
    const button = (fixture.nativeElement as HTMLElement).querySelector('button')!;

    expect(button.textContent?.trim()).toBe('tentar novamente');
    button.click();
    expect(spy).toHaveBeenCalledTimes(1);
  });
});
