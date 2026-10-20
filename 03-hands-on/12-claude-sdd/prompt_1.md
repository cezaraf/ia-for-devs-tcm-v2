Implemente um painel de clima no frontend e backend existente.

O usuário deve poder digitar uma cidade e ver o clima atual.

Para obter os dados, utilize a API Open-Meteo (gratuita, sem necessidade de API key):

- Geocoding API: https://geocoding-api.open-meteo.com/v1/search (converter cidade em coordenadas)
- Weather API: https://api.open-meteo.com/v1/forecast (obter dados do clima)

O frontend deve buscar os dados somente do backend. Opcionalmente, o frontend pode tentar obter a localização do usuário pelo navegador (geolocation) e sugerir a cidade automaticamente. 

Crie um endpoint no backend para o frontend consumir e exiba os dados no painel.
## Lacunas a definir na TechSpec

Este prompt não fixa os valores que as skills do projeto (`.claude/skills/`) esperam receber "do prompt da
tarefa". Na criação da TechSpec (passo 02), defina explicitamente e use como fonte única:

- Endpoint(s) do backend: caminho (sem `/api`), parâmetros, limites de validação (`trim`, tamanho mínimo/máximo)
  e campos da resposta com tipos e nulabilidade (camelCase).
- Tabela de erros: status (400/404/502/504), `code` em `UPPER_SNAKE_CASE` e mensagem em PT-BR de cada caso.
- Chaves de `application.properties` das duas APIs (`base-url`, `connect-timeout`, `read-timeout`) e seus valores.
- Nomes de classes do backend (controller, service, client(s), DTOs, exceções).
- Tabelas de tradução com valor padrão: `weather_code` (WMO) → descrição em PT-BR e graus → direção do vento,
  com os limites de faixa (ex.: 0°, 22.5°, 360°).
- Tratamento de cidades homônimas (primeira, lista para escolher ou refinamento).
- Tema `day`/`night` por `data-theme` (ex.: a partir de `is_day`) e marcadores ASCII (`[ DIA ]`, `[ NOITE ]`).
- Nomes de arquivos, classes e seletores do frontend (serviço de API, orquestrador e componentes de apresentação).
- Estados da UI, mensagens exibidas, quais erros permitem "tentar novamente" e o comportamento da geolocalização
  (permissão negada, indisponível, como a coordenada vira cidade sugerida).

## Ambiente de execução

Para subir e testar no browser, use o `compose.yaml` do projeto (`https://test.cezar.dev/<VERSION>/`), e não a
skill global `sandbox`.
