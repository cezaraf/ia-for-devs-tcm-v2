import { TestBed } from '@angular/core/testing';
import { StatusMessage } from './status-message';

describe('StatusMessage', () => {
  async function setup(kind: 'info' | 'error', retryable?: boolean) {
    const fixture = TestBed.createComponent(StatusMessage);
    fixture.componentRef.setInput('message', 'Algo aconteceu.');
    fixture.componentRef.setInput('kind', kind);
    if (retryable !== undefined) {
      fixture.componentRef.setInput('retryable', retryable);
    }
    await fixture.whenStable();
    const retry = vi.fn();
    fixture.componentInstance.retry.subscribe(retry);
    return { el: fixture.nativeElement as HTMLElement, retry };
  }

  it('SCN-029 error tem role alert e marcador [x]', async () => {
    const { el } = await setup('error');
    expect(el.querySelector('[role="alert"]')).not.toBeNull();
    expect(el.querySelector('.marker')?.textContent).toBe('[x]');
    expect(el.querySelector('.marker')?.getAttribute('aria-hidden')).toBe('true');
    expect(el.textContent).toContain('Algo aconteceu.');
  });

  it('SCN-029 info tem [i] e não tem role alert', async () => {
    const { el } = await setup('info');
    expect(el.querySelector('[role="alert"]')).toBeNull();
    expect(el.querySelector('.marker')?.textContent).toBe('[i]');
  });

  it('SCN-029 sem retryable não há botão', async () => {
    const { el } = await setup('error');
    expect(el.querySelector('button')).toBeNull();
  });

  it('SCN-029 retryable mostra "Tentar novamente" e emite retry', async () => {
    const { el, retry } = await setup('error', true);
    const button = el.querySelector('button')!;
    expect(button.textContent?.trim()).toBe('Tentar novamente');
    button.click();
    expect(retry).toHaveBeenCalledTimes(1);
  });
});
