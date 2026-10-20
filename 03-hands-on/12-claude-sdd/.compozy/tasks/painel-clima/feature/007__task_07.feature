# language: pt

@FEAT-003
Funcionalidade: Componentes de apresentação do painel

  Regra: BR-005 - Busca só por confirmação explícita

    @SCN-025
    Cenário: Formulário emite só no submit e é acessível
      Dado o CitySearchForm com geolocationAvailable falso
      Quando o usuário digita "Recife" sem confirmar
      Então nada é emitido
      Quando pressiona Enter
      Então search emite o texto bruto
      E o botão "Usar minha localização" não existe
      E com invalidMessage a mensagem tem role "alert" e o campo tem aria-invalid e aria-describedby

  Regra: BR-002 - De 2 a 10 cidades, o usuário escolhe na lista

    @SCN-026
    Cenário: Lista de homônimas distinguíveis
      Dado o CityOptions com duas cidades "São José — Santa Catarina, Brasil" de sub-regiões diferentes
      Quando a lista é renderizada
      Então o título é "Encontramos 2 cidades. Escolha uma:", cada linha mostra a sub-região entre parênteses e o foco está no 1º botão
      Quando o usuário clica na segunda
      Então selected emite a segunda cidade

  Regra: BR-012 - Horário da medição no fuso local da cidade

    @SCN-027
    Cenário: Cartão com valores formatados e campos ausentes
      Dado o WeatherCard com observedAt "2026-10-20T16:00", temperature 28.6, humidity null e isDay false
      Quando o cartão é renderizado
      Então mostra "28,6 °C", umidade "—", "Medido às 16:00 (horário local)" e o marcador "[ NOITE ]"

  Regra: RNF-005 - Carregamento e erros anunciados ao leitor de tela

    @SCN-028 @FEAT-006
    Cenário: Esqueleto oculto do leitor de tela com texto alternativo
      Dado o WeatherSkeleton
      Quando é renderizado
      Então os blocos têm aria-hidden "true" e existe o texto oculto "Carregando o clima…"

    @SCN-029 @FEAT-006
    Cenário: Mensagem de erro com nova tentativa
      Dado o StatusMessage com kind "error" e retryable verdadeiro
      Quando é renderizado
      Então tem role "alert", o marcador "[x]" e o botão "Tentar novamente", que emite retry ao ser clicado
      E com kind "info" mostra "[i]" sem role "alert"
