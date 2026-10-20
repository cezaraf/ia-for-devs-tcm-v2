# language: pt

@FEAT-006
Funcionalidade: Base da página, tokens e HttpClient

  Regra: RNF-008 - Aparência só com os tokens de design-system.md

    @SCN-020
    Cenário: Página pt-BR com fonte única e tokens dos dois temas
      Dado o build do frontend com SSR
      Quando a página inicial é pré-renderizada
      Então o HTML tem lang "pt-BR", título "Painel de clima" e o link da JetBrains Mono como único recurso externo
      E styles.css define os tokens de cor para data-theme day e night, sem @theme, @apply nem prefers-color-scheme
