# Validação da Fase 1

Registro local em 21/11/2026, Windows 11. Este arquivo registra evidências desta entrega. O workflow configurado ainda depende de publicação autorizada para executar no GitHub.

## Ambiente inspecionado

O workspace inicial estava vazio e sem repositório Git. O projeto foi criado em `ai-dev-office/`, com Git inicializado localmente e sem remote configurado.

| Ferramenta | Resultado |
| --- | --- |
| Java | Microsoft OpenJDK 21.0.13.0.2 LTS disponível |
| Node | 24.22.1 disponível no runtime do aplicativo |
| Git | 2.56.0.windows.4 disponível |
| Maven | Ausente do PATH. Distribuição oficial 3.9.11 baixada em `.tools` no workspace pai. Wrapper 3.3.4 gerado e validado |
| npm | Ausente do PATH. npm oficial 11.6.2 baixado em `.tools/package` no workspace pai |
| Angular CLI | Sem instalação global. CLI 21.2.30 executado temporariamente e incluído como dependência local |
| Docker | Ausente do PATH e do caminho padrão do Docker Desktop. Nenhum serviço com.docker.service encontrado |

Nenhuma ferramenta foi instalada globalmente. `.tools` é auxiliar de validação e fica fora do repositório do projeto.

## Resultado dos testes

| Verificação | Resultado |
| --- | --- |
| Maven Wrapper, `verify` | BUILD SUCCESS e JAR gerado |
| FoundationTest | 4 testes aprovados, zero falhas e erros |
| PostgresIntegrationTest | 1 teste ignorado por ausência de Docker |
| Angular/Vitest | 5 testes aprovados |
| Angular, build de produção | Aprovado. Bundle inicial aproximado de 292 kB, dentro do orçamento configurado |
| npm audit, produção | Zero vulnerabilidades reportadas |
| npm audit, completo | 16 entradas moderadas na cadeia de desenvolvimento, relacionadas a minimatch. Pendência em docs/versions.md |
| Regras de ignore | `.env`, target, node_modules e dist ignorados pelo Git |

O backend cobre saúde pública sem detalhes, readiness saudável, negação de rota futura e OpenAPI. Usa Spring real, Security, Flyway e H2 no perfil test. O teste PostgreSQL adicional compila e está preparado para migração, validação, reexecução sem alterações e queda de readiness com banco indisponível, preservando liveness. Esse comportamento PostgreSQL ainda precisa ser executado.

O frontend cobre carregamento, resposta UP, resposta DOWN, HTTP 503 com nova tentativa e timeout de oito segundos. Usa o componente e o HttpClient reais com respostas controladas por HttpTestingController.

## Comandos usados neste notebook

Backend, dentro de `backend/`, com o cache de Maven mantido no workspace pai.

```powershell
$env:MAVEN_USER_HOME=(Resolve-Path '../../.tools').Path
.\mvnw.cmd -B '-Dmaven.repo.local=../../.tools/m2' verify
```

Frontend, dentro de `frontend/`, usando npm local.

```powershell
node ../../.tools/package/bin/npm-cli.js ci --offline
node ../../.tools/package/bin/npm-cli.js test -- --watch=false
node ../../.tools/package/bin/npm-cli.js run build
node ../../.tools/package/bin/npm-cli.js audit --omit=dev --json
```

## Verificações pendentes

- Validar Compose com `docker compose --env-file .env -f infra/compose.yml config --quiet`.
- Iniciar PostgreSQL e executar o teste Testcontainers em ambiente Docker.
- Executar aplicação completa com PostgreSQL e frontend, incluindo revisão visual em navegador e exercício de queda do banco.
- Executar o workflow no GitHub após autorização de publicação.
- Revisar atualização compatível da cadeia Angular CLI para tratar os avisos de desenvolvimento.
- Definir licença antes da publicação pública.

Não há evidência local de execução PostgreSQL, de aprovação do workflow remoto ou de revisão visual da página. Os testes H2 e os builds não substituem essas verificações.

## Investigação posterior da instalação no CI

Após publicar `f5ba1f4`, o workflow Fundação concluiu backend e Compose com sucesso. O job frontend falhou em `npm ci`, com código EUSAGE e ausência de `@emnapi/core@1.11.3` e `@emnapi/runtime@1.11.3` no lockfile. Testes, build e auditoria do frontend remoto foram ignorados após essa falha.

O lockfile registra `@napi-rs/wasm-runtime@1.2.7`, dependência do binding WASM do Rolldown, com os dois pacotes em `peerDependencies`. As entradas desses peers estavam ausentes. O CI usou npm 11.19.1 no Linux, enquanto a validação original usou npm 11.6.2 no Windows. A reprodução isolada com npm 11.19.1 e resolução Linux x64 glibc apresentou as mesmas duas ausências.

A correção local declara os dois peers explicitamente em `devDependencies`, fixados em 1.11.3, e atualiza o lockfile. A resolução Linux em modo dry-run passou após essa mudança. Os comandos de reprodução são os seguintes, usando uma cópia temporária dos manifests e a instalação local da versão do npm do CI.

```powershell
node ../npm-ci-version/package/bin/npm-cli.js ci --dry-run --ignore-scripts --os=linux --cpu=x64 --libc=glibc --no-audit --no-fund
```

O dry-run verifica resolução e consistência do lockfile. A execução real dos scripts no Linux exige uma nova execução do CI. O notebook não possui Docker ou WSL configurados para essa validação.

A instalação real no Windows com npm 11.19.1 passou após a correção. Os cinco testes Angular e o build de produção também passaram. A resolução Linux em dry-run foi aprovada tanto com npm 11.19.1 quanto com npm 11.6.2. A comparação do lockfile confirmou duas entradas novas e nenhuma alteração de versão nos pacotes existentes. A revisão independente não identificou achados no ajuste. A correção foi preparada na branch `fix/frontend-ci-peer-dependencies`. A validação remota depende da publicação e de uma nova execução do CI.

[Workflow original](https://github.com/alfredoalvesti-coder/ai-dev-office/actions/runs/44931987909) · [Relato e solução indicada pelo mantenedor](https://github.com/napi-rs/napi-rs/issues/3174) · [Contrato do npm ci](https://docs.npmjs.com/cli/v11/commands/npm-ci/)
