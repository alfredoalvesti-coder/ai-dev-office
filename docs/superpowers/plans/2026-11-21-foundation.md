# Plano de implementação da fundação

> Execução nesta sessão pelo implementador, conforme escopo detalhado do usuário. Conferir arquitetura e testes a cada entrega.

**Objetivo** — Entregar somente a Fase 1 executável e documentada.

**Arquitetura** — Backend monólito modular Spring Boot, frontend Angular e PostgreSQL. Saúde representa estado real do backend. Domínios futuros são especificados na documentação.

**Stack** — Java 21, Spring Boot 3.5.16, Angular 21 LTS, PostgreSQL 17, Maven 3.9.11.

**Design** — `docs/superpowers/specs/2026-11-21-foundation-design.md`.

## Restrições

- Fase 1, sem agentes de IA e escritório virtual.
- Não publicar, fazer push, instalar globalmente ou executar comandos destrutivos.
- Licença pendente. Configuração por ambiente, sem segredos versionados.

## Foco da revisão

- Backend indisponível deve produzir mensagem de erro no frontend.
- Banco indisponível deve afetar readiness.
- Health público não pode expor detalhes de infraestrutura.
- Migrações devem ser reexecutáveis sem duplicar alterações.
- Testes sem Docker precisam registrar que PostgreSQL não foi exercitado.

## Tarefas

- [x] Inspecionar workspace, ferramentas e fontes oficiais.
- [x] Escrever design e registrar versões.
- [x] Configurar Maven, migração, saúde, segurança e OpenAPI. Testar com `mvnw verify`.
- [x] Criar página Angular e serviço HTTP. Testar sucesso, falha, timeout e nova tentativa com `npm test -- --watch=false`.
- [x] Criar Compose e teste Testcontainers. Executar quando Docker estiver disponível.
- [x] Documentar arquitetura, agentes futuros, roadmap, aprendizado e contribuição.
- [x] Preparar templates e CI. Revisar diff, executar builds e registrar evidências e limites.

## Evidências finais

Backend com quatro testes aprovados e uma integração PostgreSQL ignorada sem Docker. Frontend com cinco testes aprovados, instalação via lockfile e build de produção. Compose, PostgreSQL real, revisão visual e CI remoto pendentes. Detalhes em docs/validation.md.

