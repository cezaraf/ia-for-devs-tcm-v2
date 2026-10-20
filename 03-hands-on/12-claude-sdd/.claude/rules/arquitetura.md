# Arquitetura

- Fluxo de dados único: **Navegador → Backend (Spring) → API externa**. O frontend nunca chama uma API externa
  diretamente; só fala com o backend.
- Frontend e backend estão na **mesma origem** (Traefik). Não existe e não deve existir configuração de CORS.
- O Traefik recebe `/<prefixo>/api/...`, remove o prefixo `/<prefixo>/api` e entrega ao Spring. Por isso:
  - endpoints Spring são declarados **sem** `/api` (ex.: `@GetMapping("/cities")`, não `/api/cities`);
  - no frontend, URLs do backend são **relativas e sem barra inicial** (`api/<recurso>`), resolvidas contra o
    `baseHref`;
  - nunca fixar `/template/`, `/01/` ou qualquer outro prefixo de rota no código.
- Responsabilidades no backend:
  - `controllers/`: recebem a requisição HTTP e delegam; nenhuma regra de negócio nem chamada externa.
  - `services/`: validação de regra, mapeamento para DTOs de resposta, traduções (códigos → texto, unidades etc.).
  - `clients/`: **única** camada que faz HTTP para fora; uma classe por API externa.
- Responsabilidades no frontend:
  - um serviço `@Injectable({ providedIn: 'root' })` por funcionalidade é o **único** ponto que faz HTTP;
  - um componente orquestrador por funcionalidade guarda o estado e chama o serviço;
  - os demais componentes são de apresentação: só recebem `input()` e emitem `output()`.
- `GET /healthcheck` (`HealthCheckController`) deve continuar funcionando.
