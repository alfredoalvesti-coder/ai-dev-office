# ADR 0010 — Implementação aprovada em workspace

**Estado** — Aceito para a V1 da Fase 7.

## Contexto

A equipe multiagente gera planos e tarefas. O desenvolvedor do fluxo sequencial não escrevia arquivos. O executor da Fase 6 executa JavaScript efêmero sem montar um produto em disco. A nova intenção do portfólio é colocar a equipe para trabalhar: após o plano aprovado, propor e aplicar código sob revisão humana.

## Decisão

- Persistir `implementation_runs` vinculados a um `team_run` em estado `APPROVED`.
- Gerar proposta JSON com resumo e lista de arquivos (`path`, `content`, `rationale`) via Ollama.
- Exigir aprovação do hash SHA-256 da proposta antes de qualquer escrita.
- Aplicar somente em `app.implementation.workspace-root/{projectId}/`, com allowlist de extensões e rejeição de path traversal.
- Publicar eventos `IMPLEMENTATION_*` no canal já usado pelo escritório.

## Consequências

- Há um caminho demonstrável de demanda → plano → arquivos no workspace.
- A escrita não toca o monorepo do AI Dev Office.
- Qualidade do código depende do modelo e da revisão humana.
- Verificação automática (build/teste) e ciclo de correção ficam para iterações seguintes.
