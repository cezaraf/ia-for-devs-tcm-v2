# language: pt

@FEAT-005
Funcionalidade: Formato de erro único e não regressão do backend

  Regra: RNF-010 - GET /healthcheck continua respondendo como hoje

    @SCN-001
    Cenário: Healthcheck preservado
      Dado o backend em execução
      Quando o navegador consulta "api/healthcheck"
      Então o backend responde 200 com o corpo "OK"

  Regra: BR-006 - Mensagens de erro em PT-BR, sem detalhes internos

    @SCN-002
    Esquema do Cenário: Exceção própria vira ErrorResponse exato
      Dado que o processamento lança "<exceção>"
      Quando o ApiExceptionHandler trata a falha
      Então a resposta tem status <status> e corpo apenas com code "<code>" e a mensagem PT-BR da TechSpec
      E o corpo não contém nome de classe Java, stack trace nem o texto da causa

      Exemplos:
        | exceção                                       | status | code                |
        | InvalidRequestException(INVALID_CITY_NAME)    | 400    | INVALID_CITY_NAME   |
        | InvalidRequestException(INVALID_COORDINATES)  | 400    | INVALID_COORDINATES |
        | CityNotFoundException                         | 404    | CITY_NOT_FOUND      |
        | UpstreamException                             | 502    | UPSTREAM_ERROR      |
        | UpstreamTimeoutException                      | 504    | UPSTREAM_TIMEOUT    |
