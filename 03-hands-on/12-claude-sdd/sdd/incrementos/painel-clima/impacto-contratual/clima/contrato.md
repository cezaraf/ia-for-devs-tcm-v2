# Impacto contratual — Clima

Incremento: `painel-clima`. Contrato vivo atual: inexistente (`sdd/contratos/` ausente, PRM-002).
Fontes: PRD (`.compozy/tasks/painel-clima/_prd.md`), TechSpec (seções "Backend", "Frontend" e
"Contratos entre camadas") e ADR-001/ADR-002. Valores exatos (mensagens, tabelas, campos) ficam na TechSpec;
este documento fixa o comportamento observável que será consolidado no passo 13.

Comportamento preservado, sem impacto: `GET /healthcheck` responde `200` `text/plain` `OK` (RNF-010).
O placeholder da página inicial do framework é substituído pelo painel; ele não é contrato e não entra
como `REMOVIDO`.

## Comportamentos novos

### Comportamento: Formato único de erro do backend

O sistema deve responder a cada falha prevista de `GET /cities` e `GET /weather` com
`application/json` `{"code": "...", "message": "..."}`, com `code` estável em `UPPER_SNAKE_CASE` e mensagem
curta em PT-BR, sem stack trace, nome de classe, mensagem de exceção Java nem resposta bruta da Open-Meteo.
Códigos: `400 INVALID_CITY_NAME`, `400 INVALID_COORDINATES`, `404 CITY_NOT_FOUND`, `502 UPSTREAM_ERROR`,
`504 UPSTREAM_TIMEOUT`.

#### Cenário: Falha do provedor vira erro genérico

- DADO que a Open-Meteo responde com erro, corpo inválido ou recusa a conexão
- QUANDO o navegador consulta `GET /cities` ou `GET /weather`
- ENTÃO o backend responde `502` com `code` `UPSTREAM_ERROR` e a mensagem PT-BR fixa, sem o `reason` do provedor

#### Cenário: Demora do provedor vira timeout

- DADO que a Open-Meteo não conecta ou não responde dentro do tempo-limite configurado
- QUANDO o navegador consulta `GET /cities` ou `GET /weather`
- ENTÃO o backend responde `504` com `code` `UPSTREAM_TIMEOUT` e a mensagem PT-BR fixa

### Comportamento: Busca de cidades por nome

O sistema deve expor `GET /cities?name=` (no navegador, `api/cities`), que normaliza o nome removendo das
extremidades os espaços Unicode (`White_Space` + U+FEFF), aceita de 2 a 100 code points, consulta a Open-Meteo
Geocoding em português e devolve de 1 a 10 localidades habitadas (`feature_code` `PPL*` ou ausente), na
ordem do provedor, com `id`, `name`, `region`, `subregion`, `country`, `countryCode`, `latitude`,
`longitude` e `timezone` (campos opcionais como `null`, nunca omitidos).

#### Cenário: Nome com várias correspondências

- DADO um nome válido com correspondências habitadas na Open-Meteo
- QUANDO o navegador consulta `api/cities?name=São José`
- ENTÃO recebe `200` com um array de 1 a 10 cidades na ordem do provedor, sem aeroportos nem ilhas

#### Cenário: Nenhuma cidade encontrada

- DADO um nome válido sem localidade habitada correspondente
- QUANDO o navegador consulta `api/cities`
- ENTÃO recebe `404` `CITY_NOT_FOUND` (nunca `200 []`)

#### Cenário: Nome inválido

- DADO um nome ausente, vazio, só com espaços ou fora de 2 a 100 code points após a normalização
- QUANDO o navegador consulta `api/cities`
- ENTÃO recebe `400` `INVALID_CITY_NAME` e nenhuma consulta é feita à Open-Meteo

### Comportamento: Condições atuais por coordenada

O sistema deve expor `GET /weather?latitude=&longitude=` (no navegador, `api/weather`), que valida
latitude em `[-90, 90]` e longitude em `[-180, 180]`, consulta a Open-Meteo Forecast com `timezone=auto`,
°C e km/h, e devolve `latitude`/`longitude` ecoadas da requisição, `timezone`, `observedAt` (hora local sem
offset), `temperature`, `apparentTemperature`, `humidity`, `windSpeed`, `windDirectionDegrees`,
`windDirection`, `weatherCode`, `condition` e `isDay`; campos que o provedor não informar vêm como `null`,
exceto `observedAt` e `condition`, que nunca são nulos.

#### Cenário: Coordenada válida

- DADO uma coordenada válida e a Open-Meteo respondendo com o bloco `current`
- QUANDO o navegador consulta `api/weather`
- ENTÃO recebe `200` com as condições atuais já traduzidas para PT-BR

#### Cenário: Coordenada inválida

- DADO latitude ou longitude ausente, não numérica, não finita ou fora da faixa
- QUANDO o navegador consulta `api/weather`
- ENTÃO recebe `400` `INVALID_COORDINATES` e nenhuma consulta é feita à Open-Meteo

#### Cenário: Resposta do provedor sem medição

- DADO que a Open-Meteo responde sem `current` ou sem `current.time`
- QUANDO o navegador consulta `api/weather`
- ENTÃO recebe `502` `UPSTREAM_ERROR`

### Comportamento: Tradução da condição do tempo

O sistema deve traduzir o código WMO `weather_code` para texto PT-BR conforme a tabela de 29 entradas da
TechSpec, e devolver "Condição desconhecida" para código ausente ou fora da tabela.

#### Cenário: Código conhecido e desconhecido

- DADO o código `63` e, em outra consulta, o código `4`
- QUANDO o backend monta a resposta de `GET /weather`
- ENTÃO `condition` é "Chuva moderada" e, para `4`, "Condição desconhecida"

### Comportamento: Direção do vento por extenso

O sistema deve converter o ângulo do vento em um de 8 pontos em PT-BR (Norte, Nordeste, Leste, Sudeste, Sul,
Sudoeste, Oeste, Noroeste), em faixas de 45° centradas no ponto, com início inclusivo, cobrindo o círculo
inteiro (360° ≡ 0°, negativos e valores acima de 360° normalizados) e `null` para ângulo ausente ou não finito.

#### Cenário: Limites de faixa

- DADO os ângulos 22.49, 22.5, 337.5 e 360
- QUANDO o backend traduz a direção
- ENTÃO obtém Norte, Nordeste, Norte e Norte

### Comportamento: Busca por nome no painel

O sistema deve exibir na página inicial o painel de clima com um campo rotulado "Cidade" e o botão "Buscar";
a busca dispara só por confirmação (botão ou Enter), valida o texto no navegador com a mesma regra do backend
antes de qualquer requisição e, conforme o resultado, mostra o clima direto (1 cidade), a lista de escolha
(2 a 10) ou a mensagem de cidade não encontrada.

#### Cenário: Cidade única

- DADO o painel no estado inicial
- QUANDO o usuário digita um nome com uma única correspondência e pressiona Enter
- ENTÃO o painel mostra o carregamento e, em seguida, o clima atual da cidade, sem lista

#### Cenário: Texto inválido

- DADO o painel em qualquer estado
- QUANDO o usuário confirma um texto vazio ou com menos de 2 caracteres
- ENTÃO nenhuma requisição é feita, a mensagem de validação aparece junto ao campo e o foco volta ao campo

#### Cenário: Cidade não encontrada

- DADO que o backend responde `404 CITY_NOT_FOUND`
- QUANDO a busca termina
- ENTÃO o painel informa que nenhuma cidade foi encontrada, sem "Tentar novamente"

### Comportamento: Escolha entre cidades homônimas

O sistema deve, quando a busca devolver de 2 a 10 cidades, listar as opções como botões
"Nome — Região, País" (acrescentando a sub-região quando duas opções coincidirem em nome, região e país),
mover o foco para a primeira e, ao escolher uma (clique ou Enter), exibir o clima daquela cidade.

#### Cenário: Escolha na lista

- DADO uma busca que devolveu duas cidades "São José — Santa Catarina, Brasil"
- QUANDO a lista aparece
- ENTÃO as opções são distinguidas pela sub-região, o foco está na primeira e escolher uma exibe o clima dela

### Comportamento: Exibição das condições atuais

O sistema deve mostrar, no sucesso, a identificação do local, temperatura (°C), condição, sensação térmica
(°C), umidade (%), vento (km/h e direção), o marcador `[ DIA ]`/`[ NOITE ]` e o horário da medição no horário
local da cidade (lido da string, sem conversão de fuso), com números no formato PT-BR e "—" para campos nulos.

#### Cenário: Campos ausentes

- DADO uma resposta com `humidity` e `windDirection` nulos e `isDay` nulo
- QUANDO o cartão é exibido
- ENTÃO a umidade aparece como "—", o vento mostra só a velocidade e o marcador de dia/noite é omitido

### Comportamento: Carregamento, falhas e Tentar novamente

O sistema deve mostrar, durante cada operação, um esqueleto com a mesma grade do cartão; ao falhar, a
mensagem PT-BR do estado (serviço indisponível, demora excessiva, localização indisponível) com
"Tentar novamente", que repete exatamente a última operação do usuário; nenhuma operação fica em
carregamento por mais de 15 s. Entrada inválida, cidade não encontrada, permissão negada e
`INVALID_COORDINATES` não oferecem "Tentar novamente".

#### Cenário: Serviço indisponível e nova tentativa

- DADO uma busca que falhou com `502`
- QUANDO o usuário aciona "Tentar novamente"
- ENTÃO a mesma busca é refeita sem redigitar e o painel volta ao carregamento

#### Cenário: Teto de 15 segundos

- DADO uma operação sem resposta
- QUANDO passam 15 segundos
- ENTÃO o painel mostra "O serviço de clima demorou demais para responder." com "Tentar novamente"

### Comportamento: Operação mais recente prevalece

O sistema deve refletir apenas a operação mais recente do usuário (busca, escolha, localização ou envio de
texto inválido); o resultado de uma operação anterior, mesmo que chegue depois, nunca altera o estado.

#### Cenário: Resposta antiga chega depois

- DADO uma busca "Recife" em andamento
- QUANDO o usuário busca "Curitiba" e a resposta de "Recife" chega depois da de "Curitiba"
- ENTÃO o painel mostra apenas o resultado de "Curitiba"

### Comportamento: Clima pela localização do navegador

O sistema deve oferecer "Usar minha localização" somente quando o navegador tem geolocalização; pedir a
posição só após o clique; arredondar a coordenada para 2 casas antes de enviá-la ao backend; exibir o clima
com o rótulo "Sua localização"; nunca guardar nem registrar a coordenada. Permissão negada mostra mensagem
informativa sem nova tentativa; posição indisponível ou demorada mostra mensagem informativa com
"Tentar novamente"; a busca por nome segue disponível em qualquer caso.

#### Cenário: Permissão concedida

- DADO um navegador com geolocalização e o painel carregado
- QUANDO o usuário aciona "Usar minha localização" e concede a permissão
- ENTÃO o painel consulta o clima da coordenada arredondada e mostra "Sua localização"

#### Cenário: Permissão negada

- DADO o pedido de permissão aberto
- QUANDO o usuário nega
- ENTÃO o painel mostra "Permissão de localização negada. Busque pelo nome da cidade." e o campo segue utilizável

### Comportamento: Tema dia/noite

O sistema deve aplicar `data-theme="day"` ou `data-theme="night"` na raiz do painel conforme `isDay` do
último resultado com sucesso (`day` antes do primeiro resultado e quando `isDay` for nulo), mantendo o tema
em erros e na lista, sem animação e sem seguir a preferência de cor do sistema operacional.

#### Cenário: Noite no local

- DADO um resultado com `isDay` falso
- QUANDO o cartão é exibido
- ENTÃO a raiz do painel tem `data-theme="night"` e o marcador `[ NOITE ]`

### Comportamento: Acessibilidade do painel

O sistema deve publicar a página em `pt-BR`, com rótulo visível no campo, foco visível em cada elemento
interativo, área de resultado com `aria-live="polite"` e `aria-busy="true"` durante o carregamento, erros
com `role="alert"`, lista navegável por Tab e Enter, foco movido para o primeiro item da lista ou para o campo
em erro de validação e animação do esqueleto desativada com `prefers-reduced-motion: reduce`.

#### Cenário: Carregamento anunciado

- DADO uma busca válida confirmada
- QUANDO a requisição está em andamento
- ENTÃO a área de resultado tem `aria-busy="true"` e o esqueleto é ocultado do leitor de tela com texto alternativo

### Comportamento: Renderização no servidor sem efeitos do navegador

O sistema deve pré-renderizar a página no servidor no estado inicial, com tema `day`, sem requisição ao
backend e sem acessar APIs do navegador; o botão de localização só aparece após a hidratação no navegador.
Todas as URLs do backend no frontend são relativas (`api/...`), sem prefixo de publicação fixo.

#### Cenário: Prerender

- DADO o build com SSR
- QUANDO a página inicial é pré-renderizada
- ENTÃO o HTML contém o painel no estado inicial, sem resultado de clima e sem o botão de localização
