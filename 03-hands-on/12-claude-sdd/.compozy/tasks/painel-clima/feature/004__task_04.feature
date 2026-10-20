# language: pt

@FEAT-002
Funcionalidade: Endpoint GET /weather e tabelas de tradução

  Regra: BR-011 - Unidades fixas °C, km/h e %

    @SCN-014
    Cenário: Coordenada válida devolve condições traduzidas
      Dado que a Forecast responde o bloco current de Recife com is_day 1
      Quando o navegador consulta "api/weather?latitude=-8.05&longitude=-34.88"
      Então recebe 200 com o JSON do contrato, latitude e longitude ecoadas, condition e windDirection em PT-BR e isDay true

    @SCN-015
    Cenário: Campos ausentes viram null
      Dado que a Forecast responde current sem humidity e sem wind_direction_10m e com is_day 2
      Quando o navegador consulta "api/weather"
      Então humidity, windDirectionDegrees, windDirection e isDay vêm como null presentes no JSON

  Regra: BR-007 - Condição e direção do vento traduzidas, com padrão para desconhecido

    @SCN-016
    Esquema do Cenário: Tradução do código WMO
      Dado o weather_code <código>
      Quando o service traduz a condição
      Então condition é "<texto>"

      Exemplos:
        | código | texto                  |
        | 0      | Céu limpo              |
        | 63     | Chuva moderada         |
        | 99     | Trovoada com granizo forte |
        | 4      | Condição desconhecida  |
        | 100    | Condição desconhecida  |
        | null   | Condição desconhecida  |

    @SCN-017
    Esquema do Cenário: Direção do vento em 8 pontos
      Dado o ângulo <graus>
      Quando o service traduz a direção
      Então windDirection é "<direção>"

      Exemplos:
        | graus  | direção  |
        | 0      | Norte    |
        | 22.49  | Norte    |
        | 22.5   | Nordeste |
        | 67.5   | Leste    |
        | 337.49 | Noroeste |
        | 337.5  | Norte    |
        | 360    | Norte    |
        | 720    | Norte    |
        | -45    | Noroeste |
        | NaN    | null     |

  Regra: BR-006 - Erros com código estável e sem detalhes internos

    @SCN-018 @FEAT-005
    Esquema do Cenário: Coordenada inválida não consulta o provedor
      Dado latitude "<latitude>" e longitude "<longitude>"
      Quando o navegador consulta "api/weather"
      Então recebe 400 com code "INVALID_COORDINATES"
      E a Forecast não é chamada

      Exemplos:
        | latitude | longitude |
        | ausente  | 10        |
        | abc      | 10        |
        | NaN      | 10        |
        | -90.0001 | 10        |
        | 10       | 180.0001  |

    @SCN-019 @FEAT-005
    Esquema do Cenário: Falhas do provedor no clima
      Dado que "<falha>"
      Quando o navegador consulta "api/weather"
      Então recebe <status> com code "<code>" sem o reason do provedor

      Exemplos:
        | falha                                  | status | code             |
        | a Forecast responde sem current        | 502    | UPSTREAM_ERROR   |
        | a Forecast responde current sem time   | 502    | UPSTREAM_ERROR   |
        | o client lança UpstreamException       | 502    | UPSTREAM_ERROR   |
        | o client lança UpstreamTimeoutException| 504    | UPSTREAM_TIMEOUT |
