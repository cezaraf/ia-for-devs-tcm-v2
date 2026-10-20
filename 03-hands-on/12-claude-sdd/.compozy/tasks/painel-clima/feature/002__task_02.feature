# language: pt

@FEAT-001 @FEAT-002
Funcionalidade: Clients Open-Meteo (Geocoding e Forecast)

  Regra: BR-003 - Dados de clima e cidades vêm da Open-Meteo apenas pelo backend

    @SCN-003 @FEAT-001
    Cenário: Requisição de geocoding com nome codificado uma única vez
      Dado o GeocodingApiClient ligado ao MockRestServiceServer
      Quando busca o nome "São José"
      Então a requisição é GET "{base}/search" com name codificado uma vez, count=10, language=pt e format=json

    @SCN-004 @FEAT-002
    Cenário: Requisição de forecast com campos atuais e unidades fixas
      Dado o ForecastApiClient ligado ao MockRestServiceServer
      Quando consulta a latitude -8.05 e a longitude -34.88
      Então a requisição é GET "{base}/forecast" com os sete campos de current, timezone=auto, temperature_unit=celsius e wind_speed_unit=kmh

    @SCN-007
    Cenário: Corpo do provedor lido de forma tolerante
      Dado que a Geocoding responde 200 com {"generationtime_ms":0.1} e campos desconhecidos
      Quando o client desserializa a resposta
      Então results é nulo e nenhum erro de desserialização ocorre

  Regra: BR-006 - Falhas do provedor nunca vazam ao cliente

    @SCN-005 @FEAT-005
    Esquema do Cenário: Falha do provedor vira UpstreamException
      Dado que a Open-Meteo responde com "<falha>"
      Quando o client faz a chamada
      Então lança UpstreamException

      Exemplos:
        | falha                                   |
        | erro 500                                |
        | 400 {"error":true,"reason":"..."}       |
        | corpo "not json"                        |
        | ConnectException                        |

    @SCN-006 @FEAT-005
    Cenário: Timeout do provedor vira UpstreamTimeoutException
      Dado que a Open-Meteo lança SocketTimeoutException
      Quando o client faz a chamada
      Então lança UpstreamTimeoutException
