# language: pt

@FEAT-001
Funcionalidade: Endpoint GET /cities

  Regra: BR-002 - Até 10 cidades candidatas; 0 é "não encontrada"

    @SCN-008
    Cenário: Nome válido devolve localidades habitadas na ordem do provedor
      Dado que a Geocoding devolve PPLA, AIRP, PPL e ISL nessa ordem
      Quando o navegador consulta "api/cities?name=Recife"
      Então recebe 200 com apenas as duas localidades PPL*, na ordem original
      E cada item traz region, subregion, country, countryCode e timezone, com null quando ausentes

    @SCN-009
    Cenário: Mais de 10 candidatas são cortadas em 10
      Dado que a Geocoding devolve 12 localidades habitadas
      Quando o navegador consulta "api/cities"
      Então recebe as 10 primeiras

    @SCN-010
    Esquema do Cenário: Nenhuma cidade encontrada
      Dado que a Geocoding devolve "<resultado>"
      Quando o navegador consulta "api/cities"
      Então recebe 404 com code "CITY_NOT_FOUND"

      Exemplos:
        | resultado               |
        | sem a chave results     |
        | results vazio           |
        | só AIRP e ISL           |

  Regra: BR-001 - Nome entre 2 e 100 caracteres após remover espaços das extremidades

    @SCN-011
    Esquema do Cenário: Normalização e limites do nome
      Dado o nome "<nome>"
      Quando o navegador consulta "api/cities"
      Então o resultado é "<resultado>"
      E a Geocoding só é chamada quando o nome é válido, com o valor normalizado

      Exemplos:
        | nome                         | resultado                 |
        | ausente                      | 400 INVALID_CITY_NAME     |
        | vazio                        | 400 INVALID_CITY_NAME     |
        | dois espaços                 | 400 INVALID_CITY_NAME     |
        | " a "                        | 400 INVALID_CITY_NAME     |
        | NBSP + "a" + NBSP            | 400 INVALID_CITY_NAME     |
        | 101 code points              | 400 INVALID_CITY_NAME     |
        | 2 code points                | válido                    |
        | 100 code points              | válido                    |
        | nome com emoji               | válido, contado por code point |
        | "\uFEFFab\u2007"             | válido, enviado como "ab" |

  Regra: BR-006 - Falhas do provedor nunca vazam ao cliente

    @SCN-012 @FEAT-005
    Cenário: Falha do provedor ou item incompleto
      Dado que o client lança UpstreamException ou devolve um item sem name
      Quando o navegador consulta "api/cities"
      Então recebe 502 com code "UPSTREAM_ERROR" e corpo só com code e message

    @SCN-013 @FEAT-005
    Cenário: Timeout do provedor
      Dado que o client lança UpstreamTimeoutException
      Quando o navegador consulta "api/cities"
      Então recebe 504 com code "UPSTREAM_TIMEOUT" e a mensagem PT-BR exata
