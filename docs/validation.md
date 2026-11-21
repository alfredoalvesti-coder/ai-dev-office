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
