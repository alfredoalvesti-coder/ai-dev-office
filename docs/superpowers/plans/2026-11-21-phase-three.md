# Fase 3 — Plano de Implementação

**Goal:** Gerar rascunhos de requisitos e tarefas com Ollama local, registrar execuções e exigir aprovação humana para persistir tarefas.

**Architecture:** Spring AI 1.1.x e Ollama ficam atrás de uma porta de geração. O módulo `agents` mantém a execução, tentativas, eventos e aprovações persistidos. A UI consulta e aprova execuções sem chamar o modelo diretamente.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring AI 1.1.x, Ollama, PostgreSQL/Flyway, H2, Angular 21, Signals, RxJS e Material.

**Spec:** `docs/superpowers/specs/2026-11-21-phase-three-design.md`

## Global Constraints

- Não adicionar OpenAI API, credenciais pagas, tool calling ou execução de código.
- Nunca aprovar resultado do modelo automaticamente.
- Limitar demanda a 5.000 caracteres, saída a 1.200 tokens, tentativas a 3, fila concorrente a 1 e uma etapa de modelo por tentativa.
- Aprovação vale somente para SHA-256 do rascunho armazenado e expira em 24 horas.
- Não auto-baixar nem executar Ollama/modelos na inicialização.
- Criar apenas V3 e preservar V1/V2.

## Arquivos

- `backend/pom.xml`, `backend/src/main/resources/application.yml`: BOM/starter Spring AI e configuração Ollama.
- `backend/src/main/resources/db/migration/V3__create_analysis_runs.sql`: execuções, tentativas, eventos e decisões.
- `backend/src/main/java/dev/aidevoffice/agents/**`: modelos de domínio, DTOs, controllers, coordenação, fila e porta de análise.
- `backend/src/main/java/dev/aidevoffice/agents/ai/**`: adaptador Spring AI/Ollama e prompt versionado.
- `backend/src/main/java/dev/aidevoffice/tasks/**`: criação transacional de tarefas aprovadas via serviço existente.
- `frontend/src/app/**`: serviço Angular e painel de execuções/aprovação dentro do projeto selecionado.
- `docs/adr/0005-local-ai-provider.md`, `docs/agents.md`, `docs/architecture.md`, `docs/learning/phase-three.md`, `README.md`, `docs/validation.md`: decisão, limites, execução e evidências.

## Entregas

- [ ] Fixar versão Spring AI compatível e configurar Ollama local sem pull automático.
- [ ] Persistir criação idempotente, estado, tentativas e eventos; cancelar e repetir sem duplicar tentativa.
- [ ] Validar saída estruturada, orçamento e timeout; guardar hash e metadados.
- [ ] Aprovar/rejeitar exatamente o hash pendente, aplicar expiração e criar tarefas atomicamente.
- [ ] Integrar painel Angular a criação, consulta, revisão, cancelamento, retentativa e aprovação.
- [ ] Atualizar ADRs/documentação, compilar backend e compilar/build frontend.

## Revisão de escopo

F4 (equipe multiagente) e F6 (sandbox de execução) continuam futuras. A saída aprovada deste analista pode criar tarefas descritivas; nunca executa comandos nem patches.
