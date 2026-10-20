# Execução e ambiente

- Esta máquina é a VPS de desenvolvimento: para subir, mostrar no browser, reproduzir bug ou depurar, use a
  skill global **`sandbox`** (`~/.local/bin/sandbox`), conforme o `~/.claude/CLAUDE.md` global.
- Teste unitário e build pontual (`./mvnw test`, `npm test`, `npm run build`) podem rodar no host.
- Proibido para expor o projeto: `npm run dev`/`npm start`/`ng serve` no host, `./mvnw spring-boot:run` no host,
  `docker run -p`, `ports:` no compose, serviço instalado no host.

## Como o `compose.yaml` publica o projeto

- Projeto `airquality-template-${VERSION}`; serviços `backend` (Maven, porta 8080) e `frontend`
  (`ng serve -c template`, porta 4200), ambos na rede `edge`, com `mem_limit`, sem `ports:`.
- Traefik (labels no compose):
  - `https://test.cezar.dev/<VERSION>/` → frontend;
  - `https://test.cezar.dev/<VERSION>/api/...` → backend, com o prefixo `/<VERSION>/api` removido
    (ex.: `/01/api/healthcheck` chega ao Spring como `/healthcheck`).
- O `baseHref` da configuração `template` do `angular.json` é reescrito para `/<VERSION>/` quando o container
  sobe. Por isso o código nunca conhece o prefixo: URLs relativas (`api/...`) resolvidas pelo `baseHref`.
- O backend roda com `server.forward-headers-strategy=framework` e `-Duser.timezone=America/Bahia`.
- Código montado por bind mount: editar no host recarrega no container.

## Não fazer

- Arquivos de infraestrutura intocáveis: veja `restricoes.md`.
- Não derrubar o ambiente nem apagar volumes sem pedido explícito do dev.
