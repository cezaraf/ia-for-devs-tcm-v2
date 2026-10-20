# ADR-001 — Dois recursos (`/cities` e `/weather`) com orquestração no frontend

- Status: aceito (auditoria do passo 04, 2026-10-20)
- Data: 2026-10-20
- Incremento: painel-clima
- Relacionados: RF-001, RF-004, RF-006, RF-007, RF-008, BR-002, BR-010, PRM-003, PRM-016, PRM-020

## Contexto

O painel precisa (a) resolver um nome em 0, 1 ou várias cidades, (b) mostrar o clima direto quando há uma
só, (c) mostrar o clima da cidade escolhida numa lista e (d) mostrar o clima de uma coordenada vinda da
geolocalização, que a Open-Meteo não converte em nome de cidade. O contrato entre camadas é novo e interno
(mesma origem, PRM-003).

## Decisão

Expor dois recursos de leitura, sem estado:

- `GET /cities?name=` → `CityResponse[]` (1 a 10) ou `404 CITY_NOT_FOUND`;
- `GET /weather?latitude=&longitude=` → `WeatherResponse` (condições atuais já traduzidas).

O `WeatherPanel` encadeia as chamadas: busca → 1 cidade → clima (no mesmo observable interno, cancelado e
cronometrado junto); 2–10 → lista → clima por coordenada; geolocalização → clima por coordenada
("Sua localização"). A identificação do local exibida vem do frontend (cidade escolhida ou rótulo fixo).

## Alternativas

1. **`GET /weather?city=` único, resposta polimórfica** (clima *ou* lista de candidatas) — um round-trip a
   menos no caso de 1 cidade, mas mistura dois formatos no mesmo `200`, ainda exige outro endpoint para
   coordenadas e para a escolha na lista, e complica os tipos TS e os testes de contrato. Descartada.
2. **`GET /weather?cityId=`** usando `/v1/get?id=` da Geocoding — devolveria o nome junto ao clima, mas
   custa uma chamada externa a mais por consulta e não atende a geolocalização. Descartada.
3. **`POST /weather` com coordenadas no corpo** — tira a coordenada da query string (logs de acesso), mas
   foge do padrão de leitura idempotente das skills; o arredondamento para 2 casas foi considerado
   suficiente para o ambiente de dev. Descartada (reabrir se a privacidade exigir zero coordenada em log).

## Consequências

- Positivas: cada recurso tem um formato estável e testável; `/weather` serve escolha e geolocalização;
  cancelamento e retry ficam numa única camada (frontend).
- Negativas: busca com 1 cidade custa dois round-trips navegador → backend (orçamento de tempo em
  ADR-002); o frontend conhece a regra "1 → direto, 2..10 → lista" (BR-002).
- Contrato: domínio `clima` NOVO no impacto contratual (passo 03); mudanças futuras só aditivas.
