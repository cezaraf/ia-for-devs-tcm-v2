# language: pt

@FEAT-003
Funcionalidade: Camada de dados do painel

  Regra: BR-003 - O navegador só fala com o backend da própria aplicação

    @SCN-021
    Cenário: URLs relativas ao backend
      Dado o WeatherApi com HttpClientTesting
      Quando busca "São José" e depois o clima de -27.61 e -48.63
      Então as requisições são GET "api/cities" com params name e GET "api/weather" com params latitude e longitude, sem barra inicial nem host

  Regra: BR-001 - Nome entre 2 e 100 caracteres após remover espaços das extremidades

    @SCN-022
    Esquema do Cenário: Validação do nome idêntica à do backend
      Dado o texto "<texto>"
      Quando o navegador normaliza e valida
      Então o resultado é "<resultado>"

      Exemplos:
        | texto              | resultado       |
        | vazio              | inválido        |
        | " a "              | inválido        |
        | NBSP + "a" + NBSP  | inválido        |
        | "\uFEFFab\u2007"   | válido como ab  |
        | 100 code points    | válido          |
        | 101 code points    | inválido        |

  Regra: BR-004 - Tentar novamente só para falhas repetíveis

    @SCN-023
    Esquema do Cenário: Erro traduzido para estado e mensagem do painel
      Dado o erro "<erro>"
      Quando toFailure é aplicado
      Então o estado é "<estado>" e retry é <retry>

      Exemplos:
        | erro                      | estado          | retry |
        | 404 CITY_NOT_FOUND        | not-found       | false |
        | 400 INVALID_CITY_NAME     | invalid         | false |
        | 400 INVALID_COORDINATES   | unavailable     | false |
        | 502 UPSTREAM_ERROR        | unavailable     | true  |
        | status 0                  | unavailable     | true  |
        | 504 UPSTREAM_TIMEOUT      | timeout         | true  |
        | TimeoutError              | timeout         | true  |
        | geolocalização code 1     | geo-denied      | false |
        | geolocalização code 2     | geo-unavailable | true  |
        | geolocalização code 3     | geo-unavailable | true  |

  Regra: BR-008 - Localização só no navegador e só por ação do usuário

    @SCN-024 @FEAT-004
    Cenário: BrowserLocation seguro no servidor e tolerante a callback tardio
      Dado o BrowserLocation com PLATFORM_ID "server"
      Então isAvailable é falso e navigator não é acessado
      Dado o BrowserLocation no navegador com geolocalização simulada
      Quando getPosition é assinado
      Então emite a posição no sucesso, erro com code na falha e ignora callback após unsubscribe
