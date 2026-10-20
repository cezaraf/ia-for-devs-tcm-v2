# language: pt

@FEAT-003
Funcionalidade: Orquestrador WeatherPanel e página inicial

  Regra: RNF-006 - Renderização no servidor sem HTTP nem APIs do navegador

    @SCN-030 @FEAT-006
    Cenário: Estado inicial sem efeitos
      Dado o WeatherPanel recém-criado
      Então o estado é "idle", o tema é "day" e a instrução inicial é exibida
      E nenhuma chamada foi feita a WeatherApi nem a getPosition

    @SCN-044
    Cenário: Página inicial é o painel
      Dado o App
      Quando é renderizado
      Então contém app-weather-panel e não contém o placeholder do framework

  Regra: BR-001 - Entrada inválida não dispara consulta

    @SCN-031
    Cenário: Submit inválido durante carregamento
      Dado uma busca válida em andamento
      Quando o usuário confirma o texto "a"
      Então o estado é "invalid", nenhuma nova requisição é feita, o foco vai ao campo e a resposta tardia da busca anterior é ignorada

  Regra: BR-002 - 1 cidade mostra o clima direto; 2 a 10 mostram a lista

    @SCN-032
    Cenário: Cidade única vai direto ao clima
      Dado que searchCities devolve 1 cidade
      Quando o usuário busca "Pindamonhangaba"
      Então getWeather é chamado com a coordenada dela e o estado é "success" com o rótulo "Nome, Região, País"

    @SCN-033
    Cenário: Homônimas, escolha e clima
      Dado que searchCities devolve 2 cidades
      Quando a busca termina
      Então o estado é "choosing" e o foco está no 1º item
      Quando o usuário escolhe a segunda
      Então o estado é "success" com o rótulo da segunda

    @SCN-034
    Cenário: Cidade não encontrada
      Dado que searchCities falha com 404 CITY_NOT_FOUND
      Quando a busca termina
      Então o estado é "not-found" sem "Tentar novamente"

  Regra: BR-004 - Tentar novamente só para falhas repetíveis

    @SCN-035
    Esquema do Cenário: Serviço indisponível e nova tentativa da mesma operação
      Dado que a operação "<operação>" falhou com 502
      Quando o usuário aciona "Tentar novamente"
      Então a mesma operação é refeita sem redigitar e o estado volta a "loading"

      Exemplos:
        | operação     |
        | busca        |
        | escolha      |
        | localização  |

    @SCN-036
    Cenário: Demora excessiva por 504 ou pelo teto de 15 segundos
      Dado uma operação que recebe 504, e outra sem resposta
      Quando a primeira termina e passam 15 segundos simulados na segunda
      Então ambas terminam no estado "timeout" com "Tentar novamente"

    @SCN-037
    Cenário: Coordenada rejeitada não oferece nova tentativa
      Dado que getWeather falha com 400 INVALID_COORDINATES
      Quando a operação termina
      Então o estado é "unavailable" sem botão "Tentar novamente"

  Regra: RF-007 - Nova operação substitui a anterior

    @SCN-038
    Cenário: Resposta antiga ignorada
      Dado a busca "Recife" pendente e a busca "Curitiba" iniciada depois
      Quando "Curitiba" responde e em seguida "Recife" responde
      Então o painel mostra só o resultado de "Curitiba"

  Regra: BR-008 - Localização só após ação explícita

    @SCN-039 @FEAT-004
    Cenário: Permissão concedida
      Dado geolocalização disponível
      Quando o usuário aciona "Usar minha localização" e a posição -8.05123, -34.88765 é obtida
      Então getWeather recebe -8.05 e -34.89 e o estado é "success" com o rótulo "Sua localização"
      E getPosition nunca é chamado sem o clique

    @SCN-040 @FEAT-004
    Cenário: Permissão negada
      Dado que getPosition falha com code 1
      Quando a operação termina
      Então o estado é "geo-denied" sem "Tentar novamente" e a busca por nome continua disponível

    @SCN-041 @FEAT-004
    Esquema do Cenário: Posição indisponível ou demorada
      Dado que "<situação>"
      Quando a operação termina
      Então o estado é "<estado>" com "Tentar novamente"

      Exemplos:
        | situação                                                 | estado          |
        | getPosition falha com code 2                             | geo-unavailable |
        | getPosition falha com code 3                             | geo-unavailable |
        | a posição não chega em 15 segundos                       | geo-unavailable |
        | a posição chega e o clima não responde em 15 segundos    | timeout         |

  Regra: RF-010 - Tema segue o dia ou a noite do local exibido

    @SCN-042 @FEAT-006
    Cenário: Tema noite e permanência após erro
      Dado um sucesso com isDay falso
      Então a raiz do painel tem data-theme "night"
      Quando a busca seguinte falha com 502
      Então data-theme continua "night"
      E um sucesso com isDay nulo aplica "day"

  Regra: RNF-005 - Área de resultado anunciada como região dinâmica

    @SCN-043 @FEAT-006
    Cenário: Carregamento ocupado e esqueleto
      Dado uma busca válida em andamento
      Então a área de resultado tem aria-live "polite" e aria-busy "true" e mostra o esqueleto
      Quando a busca termina
      Então aria-busy deixa de ser "true"
