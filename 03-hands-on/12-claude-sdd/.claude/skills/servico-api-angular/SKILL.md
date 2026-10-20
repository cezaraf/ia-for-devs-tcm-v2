---
name: servico-api-angular
description: Cria a camada de dados de uma funcionalidade no frontend Angular 21 com SSR — o serviço único de HTTP (HttpClient + withFetch, URLs relativas api/...) e o componente orquestrador com máquina de estados em signal, cancelamento de requisições antigas (switchMap), retry, tema e chamadas só no navegador. Use ao ligar o frontend a um endpoint do backend.
---

# Serviço de API + orquestrador (frontend)

Regras aplicáveis: `.claude/rules/arquitetura.md`, `frontend.md`, `ssr.md`, `testes.md`.

## 1. Providers

Em `src/app/app.config.ts`, adicione (mantendo os existentes):
```ts
import { provideHttpClient, withFetch } from '@angular/common/http';
// providers: [..., provideHttpClient(withFetch())]
```

## 2. Serviço de API — `src/app/<feature>/<feature>-api.ts`

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Foo { id: number; name: string; region: string | null; }

@Injectable({ providedIn: 'root' })
export class FooApi {
  private readonly http = inject(HttpClient);

  search(name: string): Observable<Foo[]> {
    return this.http.get<Foo[]>('api/foos', { params: { name } }); // relativo, sem "/" inicial
  }
}
```
- Tipos espelham exatamente o contrato do backend (`| null` onde o contrato diz nullable).
- Nada de tratamento de UI aqui; erros sobem como `HttpErrorResponse`.

## 3. Orquestrador — máquina de estados

```ts
type State = 'idle' | 'loading' | 'success' | 'error';

export class FooPanel {
  private readonly api = inject(FooApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<State>('idle');
  protected readonly result = signal<Foo | null>(null);
  protected readonly error = signal<{ message: string; retryable: boolean } | null>(null);

  private readonly requests = new Subject<() => Observable<unknown>>();
  private lastAction: (() => void) | null = null;

  constructor() {
    this.requests
      .pipe(switchMap((run) => run()), takeUntilDestroyed(this.destroyRef))
      .subscribe();

    afterNextRender(() => {
      // disparos automáticos (ex. geolocalização) só no navegador
    });
  }

  protected search(name: string): void {
    this.lastAction = () => this.search(name);
    this.state.set('loading');
    this.requests.next(() =>
      this.api.search(name).pipe(
        tap((r) => { this.result.set(r); this.state.set('success'); }),
        catchError((e: HttpErrorResponse) => { this.showError(e); return EMPTY; }),
      ),
    );
  }

  protected retry(): void { this.lastAction?.(); }
}
```

Pontos obrigatórios:
- `switchMap` garante que uma nova ação cancela a anterior; o `catchError` fica **dentro** do observable
  interno para o fluxo principal não morrer.
- Encadeamentos (ex. buscar lista → se 1 item, buscar detalhe) ficam dentro do mesmo observable interno
  (`switchMap` aninhado), para serem cancelados juntos.
- APIs de callback do navegador (ex. `navigator.geolocation.getCurrentPosition`) são embrulhadas em
  `new Observable(...)` e entram no mesmo `Subject`, para também serem canceláveis.
- Mapeie o erro HTTP por `status` + `error?.code` para a mensagem e o `retryable` definidos no prompt;
  `status === 0` = falha de rede.
- Estado derivado (tema, `aria-busy`, qual filho exibir) em `computed`.
- Tema da página: `[attr.data-theme]="theme()"` no elemento raiz do orquestrador (os tokens de cor são
  definidos por `[data-theme]` em `styles.css`); garanta que o fundo da página toda acompanhe (ex. o raiz
  ocupa `min-height: 100dvh` com `background: var(--color-bg)`).
- Foco programático (input, primeiro item da lista) via `viewChild` + `afterNextRender`/`setTimeout`
  só no navegador.

## 4. Testes

`<feature>-api.spec.ts`:
```ts
TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
const api = TestBed.inject(FooApi);
const http = TestBed.inject(HttpTestingController);
api.search('Rio').subscribe((r) => (result = r));
const req = http.expectOne((r) => r.url === 'api/foos' && r.params.get('name') === 'Rio');
expect(req.request.method).toBe('GET');
req.flush([...]);
http.verify();
```

`<feature>-panel.spec.ts`: forneça `{ provide: FooApi, useValue: { search: vi.fn() } }`; cubra cada
transição de estado, retry repetindo a última ação, resposta atrasada ignorada (dois `Subject`s, emitir o
primeiro depois do segundo) e o `data-theme` resultante.

## Checklist

- [ ] Só o serviço injeta `HttpClient`; só chama `api/...` relativo.
- [ ] Nenhuma chamada HTTP/`navigator` no SSR.
- [ ] Nova ação cancela a anterior; retry repete exatamente a última ação.
