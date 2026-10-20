---
name: validar-entrega
description: Valida uma entrega antes de declará-la pronta — roda ./mvnw test, npm test e npm run build, e varre o código atrás de violações das regras do projeto (prefixo de rota fixo, chamada direta a API externa, cor hardcoded, Tailwind utilitário, box-shadow, dependência nova, CORS, compose alterado). Use ao terminar qualquer tarefa de código neste repositório.
---

# Validar entrega

Execute tudo a partir da raiz do projeto. Relate o resultado real de cada passo (saída de falha incluída).

## 1. Testes e build (no host)

```bash
(cd backend && ./mvnw -q test)
(cd frontend && npm test -- --watch=false)
(cd frontend && npm run build)
```

## 2. Varredura de regras

```bash
# Prefixo de rota fixo no código (arquitetura.md / execucao.md)
grep -rnE "['\"\`]/(template|[0-9]{2})/" frontend/src backend/src/main || echo OK

# URL de backend com barra inicial ou /api no Spring
grep -rnE "['\"\`]/api/" frontend/src || echo OK
grep -rnE '@(Get|Post|Request)Mapping\("/api' backend/src/main || echo OK

# Frontend chamando host externo (só o Google Fonts é permitido, no index.html)
grep -rnE "https?://" frontend/src/app --include='*.ts' || echo OK

# HttpClient injetado fora do serviço de API
grep -rlnE "inject\(HttpClient\)|HttpClient\)" frontend/src/app --include='*.ts' | grep -vE -- '-api\.ts$' || echo OK

# Cor / sombra / gradiente hardcoded nos componentes (tokens só em styles.css)
grep -rnE "#[0-9a-fA-F]{3,8}\b|rgba?\(|box-shadow|gradient|blur\(" frontend/src/app --include='*.css' || echo OK

# Classes utilitárias Tailwind / @theme
grep -rnE "@theme|@apply" frontend/src || echo OK

# CORS
grep -rniE "CrossOrigin|addCorsMappings|CorsConfiguration" backend/src || echo OK

# Acesso ao navegador sem guarda (revisar cada ocorrência)
grep -rnE "\b(navigator|window|document)\." frontend/src/app --include='*.ts' | grep -v spec

# Sem itálico / outline: none sem substituto (revisar)
grep -rnE "italic|outline:\s*(none|0)" frontend/src || echo OK
```

## 3. Arquivos que não podem mudar

Compare com o original (git, se houver; senão, a cópia de referência fornecida pelo dev):
`compose.yaml`, `frontend/angular.json`, `pom.xml` (dependências), `frontend/package.json` (dependências),
ausência de `Dockerfile`.

## 4. Conferência manual

- Cada critério de aceite do prompt da tarefa tem um teste ou foi verificado; liste os não verificados.
- Para ver no browser, use a skill global `sandbox` (não subir `ng serve`/`spring-boot:run` no host).
- `GET /healthcheck` continua respondendo `OK`.

Só declare a tarefa pronta com os passos 1–3 limpos; caso contrário, relate o que falhou.
